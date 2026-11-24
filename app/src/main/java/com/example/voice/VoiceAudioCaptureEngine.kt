package com.example.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import kotlin.concurrent.thread

data class AudioCaptureCapabilities(
    val aecAvailable: Boolean = false,
    val aecActive: Boolean = false,
    val nsAvailable: Boolean = false,
    val nsActive: Boolean = false,
    val agcAvailable: Boolean = false,
    val agcActive: Boolean = false,
    val audioSourceLabel: String = "Standard"
)

/**
 * Configures an Android AudioRecord pipeline with VOICE_COMMUNICATION (fallback VOICE_RECOGNITION)
 * plus hardware AcousticEchoCanceler (AEC), NoiseSuppressor (NS), and AutomaticGainControl (AGC).
 *
 * Importantly, AudioManager.mode is left untouched (MODE_NORMAL) so music playback remains on full
 * stereo loudspeakers instead of dropping into low-bandwidth earpiece/call routing.
 *
 * On Android 13+, can also stream the filtered 16 kHz mono 16-bit PCM frames via a
 * ParcelFileDescriptor pipe directly into RecognizerIntent.EXTRA_AUDIO_SOURCE.
 */
class VoiceAudioCaptureEngine(private val context: Context) {

    @Volatile
    private var captureThread: Thread? = null

    @Volatile
    private var isCapturing: Boolean = false

    private var audioRecord: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var autoGainControl: AutomaticGainControl? = null
    private var writePfd: ParcelFileDescriptor? = null

    fun queryDeviceCapabilities(): AudioCaptureCapabilities {
        val aecAvail = runCatching { AcousticEchoCanceler.isAvailable() }.getOrDefault(false)
        val nsAvail = runCatching { NoiseSuppressor.isAvailable() }.getOrDefault(false)
        val agcAvail = runCatching { AutomaticGainControl.isAvailable() }.getOrDefault(false)
        return AudioCaptureCapabilities(
            aecAvailable = aecAvail,
            aecActive = aecAvail,
            nsAvailable = nsAvail,
            nsActive = nsAvail,
            agcAvailable = agcAvail,
            agcActive = agcAvail,
            audioSourceLabel = "VOICE_COMMUNICATION"
        )
    }

    /**
     * Starts an AEC + NS + AGC audio capture session.
     *
     * @param createOutputPipe when true (Android 13+), returns a read-end ParcelFileDescriptor
     * containing live filtered 16kHz 16-bit mono PCM bytes for RecognizerIntent.EXTRA_AUDIO_SOURCE.
     * When false, runs in hardware-DSP conditioning mode so system SpeechRecognizer inherits
     * active platform echo cancellation cleanly.
     */
    fun start(createOutputPipe: Boolean): Pair<AudioCaptureCapabilities, ParcelFileDescriptor?> {
        stop()

        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm) {
            return queryDeviceCapabilities().copy(aecActive = false, nsActive = false, agcActive = false) to null
        }

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(DEFAULT_FRAME_BYTES * 4)

        val candidateSources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION to "VOICE_COMMUNICATION",
            MediaRecorder.AudioSource.VOICE_RECOGNITION to "VOICE_RECOGNITION",
            MediaRecorder.AudioSource.MIC to "MIC"
        )

        var selectedRecord: AudioRecord? = null
        var selectedLabel = "Standard"
        for ((src, label) in candidateSources) {
            val attempt = try {
                AudioRecord(
                    src,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
            } catch (e: SecurityException) {
                Log.w(TAG, "AudioRecord permission error for $label: ${e.message}")
                null
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord init failed for $label: ${e.message}")
                null
            }
            if (attempt != null && attempt.state == AudioRecord.STATE_INITIALIZED) {
                selectedRecord = attempt
                selectedLabel = label
                break
            } else {
                runCatching { attempt?.release() }
            }
        }

        if (selectedRecord == null) {
            return queryDeviceCapabilities().copy(
                aecActive = false,
                nsActive = false,
                agcActive = false,
                audioSourceLabel = "Fallback"
            ) to null
        }

        audioRecord = selectedRecord
        val sessionId = selectedRecord.audioSessionId

        val aecAvail = runCatching { AcousticEchoCanceler.isAvailable() }.getOrDefault(false)
        val nsAvail = runCatching { NoiseSuppressor.isAvailable() }.getOrDefault(false)
        val agcAvail = runCatching { AutomaticGainControl.isAvailable() }.getOrDefault(false)

        var aecOn = false
        if (aecAvail) {
            runCatching {
                AcousticEchoCanceler.create(sessionId)?.let { effect ->
                    effect.enabled = true
                    echoCanceler = effect
                    aecOn = effect.enabled
                }
            }
        }

        var nsOn = false
        if (nsAvail) {
            runCatching {
                NoiseSuppressor.create(sessionId)?.let { effect ->
                    effect.enabled = true
                    noiseSuppressor = effect
                    nsOn = effect.enabled
                }
            }
        }

        var agcOn = false
        if (agcAvail) {
            runCatching {
                AutomaticGainControl.create(sessionId)?.let { effect ->
                    effect.enabled = true
                    autoGainControl = effect
                    agcOn = effect.enabled
                }
            }
        }

        val pipePair = if (createOutputPipe) {
            runCatching { ParcelFileDescriptor.createPipe() }.getOrNull()
        } else {
            null
        }
        val readSide = pipePair?.get(0)
        val writeSide = pipePair?.get(1)
        writePfd = writeSide

        try {
            selectedRecord.startRecording()
            isCapturing = true
            captureThread = thread(start = true, isDaemon = true, name = "RitmoVoiceAecCapture") {
                val outStream = writeSide?.let { ParcelFileDescriptor.AutoCloseOutputStream(it) }
                val buffer = ByteArray(DEFAULT_FRAME_BYTES)
                var prevIn = 0f
                var prevOut = 0f
                try {
                    while (isCapturing) {
                        val read = selectedRecord.read(buffer, 0, buffer.size)
                        if (read > 0 && outStream != null) {
                            val statePair = applySpeakerBassHighPass(buffer, read, prevIn, prevOut)
                            prevIn = statePair.first
                            prevOut = statePair.second
                            outStream.write(buffer, 0, read)
                        } else if (read < 0) {
                            break
                        }
                    }
                } catch (_: IOException) {
                    // Closed cleanly on consumer stop
                } catch (e: Exception) {
                    Log.d(TAG, "AEC capture loop ended: ${e.message}")
                } finally {
                    runCatching { outStream?.flush() }
                    runCatching { outStream?.close() }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed starting AEC capture: ${e.message}")
            runCatching { readSide?.close() }
            stop()
            return AudioCaptureCapabilities(
                aecAvailable = aecAvail,
                aecActive = false,
                nsAvailable = nsAvail,
                nsActive = false,
                agcAvailable = agcAvail,
                agcActive = false,
                audioSourceLabel = selectedLabel
            ) to null
        }

        val caps = AudioCaptureCapabilities(
            aecAvailable = aecAvail,
            aecActive = aecOn,
            nsAvailable = nsAvail,
            nsActive = nsOn,
            agcAvailable = agcAvail,
            agcActive = agcOn,
            audioSourceLabel = selectedLabel
        )
        return caps to readSide
    }

    fun stop() {
        isCapturing = false
        val worker = captureThread
        captureThread = null

        runCatching { audioRecord?.stop() }
        runCatching { writePfd?.close() }
        writePfd = null

        if (worker != null && worker !== Thread.currentThread()) {
            runCatching { worker.join(180L) }
        }

        runCatching { echoCanceler?.release() }
        echoCanceler = null
        runCatching { noiseSuppressor?.release() }
        noiseSuppressor = null
        runCatching { autoGainControl?.release() }
        autoGainControl = null
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    companion object {
        private const val TAG = "VoiceAudioCaptureEngine"
        const val SAMPLE_RATE_HZ = 16_000
        private const val DEFAULT_FRAME_BYTES = 1024
        private const val HIGH_PASS_ALPHA = 0.963f // ~95 Hz cutoff at 16 kHz to strip loudspeaker bass bleed

        internal fun applySpeakerBassHighPass(
            pcmLittleEndian: ByteArray,
            byteCount: Int,
            initialPrevIn: Float = 0f,
            initialPrevOut: Float = 0f
        ): Pair<Float, Float> {
            var prevIn = initialPrevIn
            var prevOut = initialPrevOut
            var idx = 0
            while (idx + 1 < byteCount) {
                val low = pcmLittleEndian[idx].toInt() and 0xFF
                val high = pcmLittleEndian[idx + 1].toInt()
                val sample = ((high shl 8) or low).toShort().toFloat()
                val filtered = HIGH_PASS_ALPHA * (prevOut + sample - prevIn)
                prevIn = sample
                prevOut = filtered
                val clamped = filtered.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcmLittleEndian[idx] = (clamped and 0xFF).toByte()
                pcmLittleEndian[idx + 1] = ((clamped shr 8) and 0xFF).toByte()
                idx += 2
            }
            return prevIn to prevOut
        }
    }
}
