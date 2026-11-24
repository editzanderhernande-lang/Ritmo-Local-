package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class AudioAnalysisSnapshot(
    val bands: List<Float> = List(20) { 0.08f },
    val bassEnergy: Float = 0f,
    val midEnergy: Float = 0f,
    val trebleEnergy: Float = 0f,
    val overallRms: Float = 0f,
    val isBeatPulse: Boolean = false,
    val isLiveAudio: Boolean = false,
    val updatedAtMs: Long = 0L
)

object AudioAnalysisBus {
    private val collectorCount = AtomicInteger(0)
    private val _snapshot = MutableStateFlow(AudioAnalysisSnapshot())
    val snapshot: StateFlow<AudioAnalysisSnapshot> = _snapshot.asStateFlow()

    fun registerCollector() {
        collectorCount.incrementAndGet()
    }

    fun unregisterCollector() {
        val next = collectorCount.decrementAndGet()
        if (next <= 0) {
            collectorCount.set(0)
            _snapshot.value = AudioAnalysisSnapshot()
        }
    }

    fun hasActiveCollectors(): Boolean = collectorCount.get() > 0

    internal fun publish(snapshot: AudioAnalysisSnapshot) {
        _snapshot.value = snapshot
    }

    fun resetToIdle() {
        _snapshot.value = AudioAnalysisSnapshot()
    }
}

/**
 * Battery-friendly real-time PCM audio analyzer placed in PlaybackService's DefaultAudioSink.
 * When no visualizer UI is collecting, passes bytes through with zero DSP allocation overhead.
 * Computes 20 logarithmic frequency-weighted bands, bass/mid/treble energy, and beat pulse
 * without requiring RECORD_AUDIO or touching Android's hardware Visualizer session.
 */
@OptIn(UnstableApi::class)
class RealtimeAudioAnalysisProcessor : BaseAudioProcessor() {

    private var lastEmitMs = 0L
    private var runningBassAvg = 0.12f
    private var lpState = 0f
    private val smoothedBands = FloatArray(BAND_COUNT) { 0.08f }
    private val monoScratch = FloatArray(ANALYSIS_WINDOW)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val nowMs = System.currentTimeMillis()
        val shouldAnalyze = AudioAnalysisBus.hasActiveCollectors() &&
            (nowMs - lastEmitMs) >= EMIT_INTERVAL_MS &&
            (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT || inputAudioFormat.encoding == C.ENCODING_PCM_16BIT)

        if (shouldAnalyze) {
            val dup = inputBuffer.asReadOnlyBuffer().order(inputBuffer.order())
            val sampleCount = extractMonoSamples(
                buffer = dup,
                encoding = inputAudioFormat.encoding,
                channels = inputAudioFormat.channelCount.coerceAtLeast(1),
                outSamples = monoScratch
            )
            if (sampleCount >= 32) {
                lastEmitMs = nowMs
                val computed = computeSpectralSnapshot(
                    samples = monoScratch,
                    count = sampleCount,
                    smoothed = smoothedBands,
                    prevBassAvg = runningBassAvg,
                    lpSeed = lpState,
                    nowMs = nowMs
                )
                runningBassAvg = computed.nextBassAvg
                lpState = computed.nextLpState
                AudioAnalysisBus.publish(computed.snapshot)
            }
        }

        val output = replaceOutputBuffer(remaining)
        output.put(inputBuffer)
        output.flip()
    }

    internal data class ComputedResult(
        val snapshot: AudioAnalysisSnapshot,
        val nextBassAvg: Float,
        val nextLpState: Float
    )

    companion object {
        const val BAND_COUNT = 20
        private const val ANALYSIS_WINDOW = 128
        private const val EMIT_INTERVAL_MS = 45L

        internal fun extractMonoSamples(
            buffer: ByteBuffer,
            encoding: Int,
            channels: Int,
            outSamples: FloatArray
        ): Int {
            var idx = 0
            val maxSamples = outSamples.size
            if (encoding == C.ENCODING_PCM_FLOAT) {
                val stepBytes = 4 * channels
                while (buffer.remaining() >= stepBytes && idx < maxSamples) {
                    var sum = 0f
                    for (c in 0 until channels) {
                        sum += buffer.float
                    }
                    outSamples[idx++] = (sum / channels).coerceIn(-1f, 1f)
                }
            } else if (encoding == C.ENCODING_PCM_16BIT) {
                val stepBytes = 2 * channels
                while (buffer.remaining() >= stepBytes && idx < maxSamples) {
                    var sum = 0f
                    for (c in 0 until channels) {
                        sum += buffer.short / 32768f
                    }
                    outSamples[idx++] = (sum / channels).coerceIn(-1f, 1f)
                }
            }
            return idx
        }

        internal fun computeSpectralSnapshot(
            samples: FloatArray,
            count: Int,
            smoothed: FloatArray,
            prevBassAvg: Float,
            lpSeed: Float,
            nowMs: Long
        ): ComputedResult {
            val n = min(count, samples.size).coerceAtLeast(1)
            var sumSq = 0f
            var lp = lpSeed
            var bassAcc = 0f
            var trebleAcc = 0f
            var prevSample = samples[0]

            for (i in 0 until n) {
                val s = samples[i]
                sumSq += s * s
                lp += 0.14f * (s - lp)
                bassAcc += lp * lp
                val diff = s - prevSample
                trebleAcc += diff * diff
                prevSample = s
            }

            val rms = sqrt(sumSq / n).coerceIn(0f, 1f)
            val bassEnergy = (sqrt(bassAcc / n) * 3.1f).coerceIn(0f, 1f)
            val trebleEnergy = (sqrt(trebleAcc / n) * 2.8f).coerceIn(0f, 1f)
            val midEnergy = (rms * 2.6f * 0.65f + (bassEnergy + trebleEnergy) * 0.18f).coerceIn(0f, 1f)

            // Goertzel-style multi-bin spectral energy across 20 perceptual bands
            val bandFreqNorms = floatArrayOf(
                0.015f, 0.025f, 0.038f, 0.052f, 0.070f,
                0.095f, 0.125f, 0.160f, 0.200f, 0.245f,
                0.295f, 0.350f, 0.410f, 0.475f, 0.545f,
                0.620f, 0.700f, 0.785f, 0.875f, 0.960f
            )

            for (b in 0 until BAND_COUNT) {
                val k = (bandFreqNorms[b] * (n / 2f)).coerceAtLeast(0.6f)
                val omega = (2.0 * PI * k / n.toDouble())
                var real = 0.0
                var imag = 0.0
                for (i in 0 until n) {
                    val win = 0.54 - 0.46 * cos(2.0 * PI * i / (n - 1).coerceAtLeast(1))
                    val x = samples[i] * win
                    real += x * cos(omega * i)
                    imag -= x * sin(omega * i)
                }
                val mag = sqrt(real * real + imag * imag).toFloat() / (n * 0.28f)
                val freqBias = when {
                    b < 5 -> bassEnergy * 0.35f
                    b < 13 -> midEnergy * 0.25f
                    else -> trebleEnergy * 0.35f
                }
                val target = (0.06f + mag * 1.15f + freqBias).coerceIn(0.06f, 1f)
                smoothed[b] = smoothed[b] * 0.48f + target * 0.52f
            }

            val nextBassAvg = prevBassAvg * 0.86f + bassEnergy * 0.14f
            val isBeat = bassEnergy > 0.24f && bassEnergy > prevBassAvg * 1.30f

            return ComputedResult(
                snapshot = AudioAnalysisSnapshot(
                    bands = smoothed.toList(),
                    bassEnergy = bassEnergy,
                    midEnergy = midEnergy,
                    trebleEnergy = trebleEnergy,
                    overallRms = rms,
                    isBeatPulse = isBeat,
                    isLiveAudio = rms > 0.004f || abs(lp) > 0.002f,
                    updatedAtMs = nowMs
                ),
                nextBassAvg = nextBassAvg.coerceIn(0.05f, 0.95f),
                nextLpState = lp
            )
        }
    }
}
