package com.example.player

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.LruCache
import com.example.model.CrossfadeCurve
import com.example.model.DjTransitionStyle
import com.example.model.LocalTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AudioTrackAnalysis(
    val trackKey: String,
    val durationMs: Long,
    val introSilenceEndMs: Long,
    val outroSilenceStartMs: Long,
    val estimatedBpm: Int,
    val energyScore: Float, // 0f..1f
    val recommendedCrossfadeMs: Long
) {
    fun safeOutroTriggerPositionMs(
        configuredCrossfadeMs: Long,
        skipSilence: Boolean
    ): Long {
        if (durationMs <= 10_000L) return (durationMs - 1500L).coerceAtLeast(0L)
        val effectiveEnd = if (skipSilence && outroSilenceStartMs in 5_000L..durationMs) {
            outroSilenceStartMs
        } else {
            durationMs
        }
        val minLead = configuredCrossfadeMs.coerceIn(1000L, (durationMs / 3L).coerceAtLeast(1000L))
        // Protect core track body: never trigger transition before 62% of the song has played
        val safeFloor = (durationMs * 0.62f).toLong()
        return (effectiveEnd - minLead).coerceAtLeast(safeFloor).coerceAtMost((durationMs - 1000L).coerceAtLeast(0L))
    }
}

/**
 * Pure gain-curve calculator for outgoing and incoming tracks during crossfade.
 */
fun computeCrossfadeGains(progress: Float, curve: CrossfadeCurve): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    return when (curve) {
        CrossfadeCurve.EQUAL_POWER -> {
            val angle = (p * (PI / 2.0)).toFloat()
            cos(angle).coerceIn(0f, 1f) to sin(angle).coerceIn(0f, 1f)
        }
        CrossfadeCurve.SMOOTH_S -> {
            val s = p * p * (3f - 2f * p)
            (1f - s).coerceIn(0f, 1f) to s.coerceIn(0f, 1f)
        }
        CrossfadeCurve.LINEAR -> {
            (1f - p).coerceIn(0f, 1f) to p.coerceIn(0f, 1f)
        }
    }
}

/**
 * Strictly read-only Smart DJ audio analyzer.
 * Scans only brief header/tail packet envelopes on seekable local files to detect
 * trailing/leading silence, energy density, and estimated tempo without ever modifying files.
 */
object SmartDjAnalyzer {

    private val cache = LruCache<String, AudioTrackAnalysis>(120)

    suspend fun analyzeTrackReadOnly(
        context: Context,
        track: LocalTrack,
        djStyle: DjTransitionStyle,
        configuredCrossfadeMs: Long
    ): AudioTrackAnalysis {
        cache.get(track.trackKey)?.let { cached ->
            return cached.copy(
                recommendedCrossfadeMs = computeRecommendedCrossfadeMs(cached, djStyle, configuredCrossfadeMs)
            )
        }

        val computed = withContext(Dispatchers.IO) {
            val uri = runCatching { Uri.parse(track.contentUri) }.getOrNull()
            val scanResult = if (uri != null) scanContainerEnvelope(context, uri, track.durationMs) else null
            val fallbackDuration = track.durationMs.coerceAtLeast(0L)
            val resolvedDuration = scanResult?.durationMs?.takeIf { it > 0L } ?: fallbackDuration
            val introMs = scanResult?.introSilenceEndMs ?: 0L
            val outroMs = scanResult?.outroSilenceStartMs ?: resolvedDuration
            val energy = scanResult?.energyScore ?: estimateHeuristicEnergy(track)
            val bpm = scanResult?.estimatedBpm ?: estimateHeuristicBpm(track, energy)

            AudioTrackAnalysis(
                trackKey = track.trackKey,
                durationMs = resolvedDuration,
                introSilenceEndMs = introMs.coerceIn(0L, (resolvedDuration / 5L).coerceAtMost(8_000L)),
                outroSilenceStartMs = outroMs.coerceIn((resolvedDuration * 0.72f).toLong(), resolvedDuration),
                estimatedBpm = bpm,
                energyScore = energy,
                recommendedCrossfadeMs = configuredCrossfadeMs
            )
        }

        val withRec = computed.copy(
            recommendedCrossfadeMs = computeRecommendedCrossfadeMs(computed, djStyle, configuredCrossfadeMs)
        )
        cache.put(track.trackKey, withRec)
        return withRec
    }

    fun computeRecommendedCrossfadeMs(
        analysis: AudioTrackAnalysis,
        style: DjTransitionStyle,
        configuredMs: Long
    ): Long {
        val base = configuredMs.coerceIn(1_500L, 12_000L)
        return when (style) {
            DjTransitionStyle.QUICK_CUT -> (base * 0.55f).toLong().coerceIn(1_200L, 4_000L)
            DjTransitionStyle.AMBIENT_FLOW -> (base * 1.35f).toLong().coerceIn(3_500L, 12_000L)
            DjTransitionStyle.SMOOTH_CLUB -> {
                if (analysis.energyScore > 0.65f) base.coerceIn(3_000L, 8_000L)
                else (base * 1.15f).toLong().coerceIn(3_000L, 10_000L)
            }
        }
    }

    private data class EnvelopeScan(
        val durationMs: Long,
        val introSilenceEndMs: Long,
        val outroSilenceStartMs: Long,
        val energyScore: Float,
        val estimatedBpm: Int
    )

    private fun scanContainerEnvelope(context: Context, uri: Uri, knownDurationMs: Long): EnvelopeScan? {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            var audioTrackIdx = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIdx = i
                    format = f
                    break
                }
            }
            if (audioTrackIdx < 0 || format == null) return null
            extractor.selectTrack(audioTrackIdx)

            val formatDurationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else knownDurationMs * 1000L
            val totalDurationMs = (formatDurationUs / 1000L).coerceAtLeast(knownDurationMs)
            if (totalDurationMs <= 3_000L) return null

            val buffer = ByteBuffer.allocate(8192)
            // Sample packet size magnitudes in first 10 seconds to detect leading quiet intro
            val introWindowSamples = mutableListOf<Pair<Long, Int>>()
            extractor.seekTo(0L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            var guard = 0
            while (guard++ < 180) {
                val size = extractor.readSampleData(buffer, 0)
                if (size <= 0) break
                val tsMs = extractor.sampleTime / 1000L
                introWindowSamples.add(tsMs to packetEnergyMetric(buffer, size))
                if (tsMs > 9_000L) break
                if (!extractor.advance()) break
            }

            // Sample middle 6 seconds for rhythm density / energy estimation
            val midStartUs = (totalDurationMs * 500L) // half duration in microseconds
            extractor.seekTo(midStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val midEnergies = mutableListOf<Int>()
            guard = 0
            while (guard++ < 180) {
                val size = extractor.readSampleData(buffer, 0)
                if (size <= 0) break
                midEnergies.add(packetEnergyMetric(buffer, size))
                if ((extractor.sampleTime - midStartUs) > 5_000_000L) break
                if (!extractor.advance()) break
            }

            // Sample last 12 seconds for trailing quiet outro
            val outroSeekUs = ((totalDurationMs - 12_000L).coerceAtLeast(0L)) * 1000L
            extractor.seekTo(outroSeekUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            val outroSamples = mutableListOf<Pair<Long, Int>>()
            guard = 0
            while (guard++ < 260) {
                val size = extractor.readSampleData(buffer, 0)
                if (size <= 0) break
                val tsMs = (extractor.sampleTime / 1000L).coerceIn(0L, totalDurationMs)
                outroSamples.add(tsMs to packetEnergyMetric(buffer, size))
                if (!extractor.advance()) break
            }

            val avgMid = if (midEnergies.isNotEmpty()) midEnergies.average().toFloat() else 60f
            val quietThreshold = (avgMid * 0.28f).coerceIn(10f, 35f)

            val introSilenceEnd = introWindowSamples
                .firstOrNull { it.second > quietThreshold }
                ?.first
                ?.coerceIn(0L, 6_000L)
                ?: 0L

            val lastActiveOutro = outroSamples
                .lastOrNull { it.second > quietThreshold }
                ?.first
                ?.coerceIn((totalDurationMs * 0.72f).toLong(), totalDurationMs)
                ?: totalDurationMs

            val energyNorm = ((avgMid - 20f) / 140f).coerceIn(0.25f, 0.95f)
            val fluxCount = countEnergyPeaks(midEnergies)
            val estimatedBpm = (76 + (fluxCount * 4) + (energyNorm * 32).toInt()).coerceIn(68, 158)

            EnvelopeScan(
                durationMs = totalDurationMs,
                introSilenceEndMs = introSilenceEnd,
                outroSilenceStartMs = lastActiveOutro,
                energyScore = energyNorm,
                estimatedBpm = estimatedBpm
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun packetEnergyMetric(buffer: ByteBuffer, size: Int): Int {
        val len = size.coerceAtMost(256)
        if (len <= 0) return 0
        var sum = 0
        for (i in 0 until len) {
            sum += abs(buffer.get(i).toInt())
        }
        return (sum / len) + (size / 64)
    }

    private fun countEnergyPeaks(values: List<Int>): Int {
        if (values.size < 4) return 4
        val avg = values.average()
        var peaks = 0
        for (i in 1 until values.lastIndex) {
            if (values[i] > avg * 1.12 && values[i] >= values[i - 1] && values[i] >= values[i + 1]) {
                peaks++
            }
        }
        return peaks.coerceIn(2, 20)
    }

    private fun estimateHeuristicEnergy(track: LocalTrack): Float {
        val label = "${track.title} ${track.album} ${track.artist}".lowercase()
        return when {
            label.contains("remix") || label.contains("club") || label.contains("party") || label.contains("gym") -> 0.85f
            label.contains("acoustic") || label.contains("piano") || label.contains("sleep") || label.contains("lofi") -> 0.35f
            track.durationMs in 150_000L..250_000L -> 0.65f
            else -> 0.55f
        }
    }

    private fun estimateHeuristicBpm(track: LocalTrack, energy: Float): Int {
        return (80 + (energy * 50f).toInt() + abs(track.title.hashCode() % 14)).coerceIn(72, 146)
    }
}
