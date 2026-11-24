package com.example.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale

data class TrimmedAudioResult(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val durationMs: Long
)

object AudioClipTrimmer {

    const val MAX_RAW_CLIP_BYTES = 6_000_000 // ~5.72 MB (> 5 MB)
    const val MAX_INLINE_BASE64_CHARS = 650_000
    const val CHUNK_BASE64_CHARS = 680_000
    const val MAX_AUDIO_CHUNKS = 12
    const val MAX_BASE64_CHARS = CHUNK_BASE64_CHARS * MAX_AUDIO_CHUNKS
    const val MAX_TRIM_WINDOW_MS = 300_000L // 5 minutes max

    fun estimateBitrateForDurationMs(durationMs: Long): Int {
        val sec = (durationMs.coerceAtLeast(1_000L) / 1000.0)
        // Reserve 12% overhead for MP4 container; encode up to 128 kbps AAC
        val targetBytes = (MAX_RAW_CLIP_BYTES * 0.88)
        val rawBps = ((targetBytes * 8.0) / sec).toInt()
        return rawBps.coerceIn(64_000, 128_000)
    }

    fun estimateSizeKb(durationMs: Long): Int {
        val bitrate = estimateBitrateForDurationMs(durationMs)
        val bytes = ((bitrate / 8.0) * (durationMs.coerceAtLeast(1000L) / 1000.0) * 1.06).toInt()
        return (bytes / 1024).coerceAtLeast(1)
    }

    fun formatSizeReadable(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L)
        return if (safe >= 1_000_000L) {
            String.format(Locale.US, "%.1f MB", safe / 1_048_576.0)
        } else {
            "${(safe / 1024L).coerceAtLeast(1L)} KB"
        }
    }

    suspend fun trimAudioToM4aClip(
        context: Context,
        sourceUri: Uri,
        titleHint: String,
        startMs: Long,
        endMs: Long
    ): Result<TrimmedAudioResult> = withContext(Dispatchers.IO) {
        val safeStartMs = startMs.coerceAtLeast(0L)
        val safeEndMs = endMs.coerceAtLeast(safeStartMs + 1_000L)
            .coerceAtMost(safeStartMs + MAX_TRIM_WINDOW_MS)
        val targetDurationMs = safeEndMs - safeStartMs
        val targetBitrate = estimateBitrateForDurationMs(targetDurationMs)

        val tempOut = File(context.cacheDir, "trim_${System.currentTimeMillis()}.m4a")
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            setExtractorDataSource(context, extractor, sourceUri)

            val trackIndex = (0 until extractor.trackCount).firstOrNull { idx ->
                extractor.getTrackFormat(idx).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext Result.failure(IllegalStateException("No se encontró pista de audio en el archivo."))

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return@withContext Result.failure(IllegalStateException("Formato de audio no compatible."))

            val startUs = safeStartMs * 1000L
            val endUs = safeEndMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            var muxerTrackIndex = -1
            var muxerStarted = false
            var pcmSampleRate = inputFormat.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, 44100)
            var pcmChannelCount = inputFormat.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, 2).coerceIn(1, 2)

            val decInfo = MediaCodec.BufferInfo()
            val encInfo = MediaCodec.BufferInfo()
            var extractorDone = false
            var decoderDone = false
            var encoderDone = false
            var encoderInitialized = false
            var encoderInputEosSent = false

            fun ensureEncoder(sampleRate: Int, channels: Int) {
                if (encoderInitialized) return
                pcmSampleRate = sampleRate
                pcmChannelCount = channels.coerceIn(1, 2)
                val outFormat = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_AAC,
                    pcmSampleRate,
                    pcmChannelCount
                ).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 65_536)
                }
                encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                    configure(outFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    start()
                }
                muxer = MediaMuxer(tempOut.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                encoderInitialized = true
            }

            fun drainEncoder() {
                val enc = encoder ?: return
                val mx = muxer ?: return
                while (true) {
                    val outIdx = enc.dequeueOutputBuffer(encInfo, 2_000L)
                    when {
                        outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                        outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                muxerTrackIndex = mx.addTrack(enc.outputFormat)
                                mx.start()
                                muxerStarted = true
                            }
                        }
                        outIdx >= 0 -> {
                            val encodedBuf = enc.getOutputBuffer(outIdx)
                            if (encodedBuf != null && encInfo.size > 0 && (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                if (muxerStarted) {
                                    encodedBuf.position(encInfo.offset)
                                    encodedBuf.limit(encInfo.offset + encInfo.size)
                                    mx.writeSampleData(muxerTrackIndex, encodedBuf, encInfo)
                                }
                            }
                            enc.releaseOutputBuffer(outIdx, false)
                            if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                encoderDone = true
                                break
                            }
                        }
                    }
                }
            }

            var safetyLoops = 0
            while (!encoderDone && safetyLoops < 200_000) {
                safetyLoops++
                if (!extractorDone) {
                    val decInIdx = decoder.dequeueInputBuffer(5_000L)
                    if (decInIdx >= 0) {
                        val inBuf = decoder.getInputBuffer(decInIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        val sampleTimeUs = extractor.sampleTime
                        if (sampleSize < 0 || sampleTimeUs > endUs) {
                            decoder.queueInputBuffer(decInIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            extractorDone = true
                        } else {
                            decoder.queueInputBuffer(decInIdx, 0, sampleSize, sampleTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!decoderDone) {
                    val decOutIdx = decoder.dequeueOutputBuffer(decInfo, 5_000L)
                    when {
                        decOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            val outFmt = decoder.outputFormat
                            val sr = outFmt.getIntegerOrDefault(MediaFormat.KEY_SAMPLE_RATE, pcmSampleRate)
                            val ch = outFmt.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, pcmChannelCount)
                            ensureEncoder(sr, ch)
                        }
                        decOutIdx >= 0 -> {
                            ensureEncoder(pcmSampleRate, pcmChannelCount)
                            val pcmBuf = decoder.getOutputBuffer(decOutIdx)
                            val isEos = (decInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                            val inRange = decInfo.presentationTimeUs >= startUs || isEos

                            if (pcmBuf != null && decInfo.size > 0 && inRange) {
                                feedPcmToEncoder(
                                    encoder = encoder!!,
                                    pcmBuffer = pcmBuf,
                                    offset = decInfo.offset,
                                    size = decInfo.size,
                                    ptsUs = (decInfo.presentationTimeUs - startUs).coerceAtLeast(0L),
                                    rawChannels = decoder.outputFormat.getIntegerOrDefault(MediaFormat.KEY_CHANNEL_COUNT, pcmChannelCount),
                                    targetChannels = pcmChannelCount,
                                    drainFn = ::drainEncoder
                                )
                            }
                            decoder.releaseOutputBuffer(decOutIdx, false)
                            if (isEos) {
                                decoderDone = true
                                if (!encoderInputEosSent && encoder != null) {
                                    val encInIdx = encoder!!.dequeueInputBuffer(10_000L)
                                    if (encInIdx >= 0) {
                                        encoder!!.queueInputBuffer(encInIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                        encoderInputEosSent = true
                                    }
                                }
                            }
                        }
                    }
                } else if (!encoderInputEosSent && encoder != null) {
                    val encInIdx = encoder!!.dequeueInputBuffer(10_000L)
                    if (encInIdx >= 0) {
                        encoder!!.queueInputBuffer(encInIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        encoderInputEosSent = true
                    }
                }

                if (encoderInitialized) {
                    drainEncoder()
                } else if (decoderDone) {
                    break
                }
            }

            if (muxerStarted) {
                try { muxer?.stop() } catch (_: Exception) {}
            }

            if (!tempOut.exists() || tempOut.length() <= 128L) {
                return@withContext Result.failure(IllegalStateException("No se pudo generar el clip recortado."))
            }
            val bytes = tempOut.readBytes()
            if (bytes.size > MAX_RAW_CLIP_BYTES) {
                return@withContext Result.failure(
                    IllegalStateException("El recorte pesó ${formatSizeReadable(bytes.size.toLong())} (máximo ${formatSizeReadable(MAX_RAW_CLIP_BYTES.toLong())}). Elige un tramo un poco más corto.")
                )
            }

            val cleanName = titleHint.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .take(36)
                .ifBlank { "clip" }
            val clipLabel = String.format(Locale.US, "%s_%ds.m4a", cleanName, (targetDurationMs / 1000L).coerceAtLeast(1L))
            Result.success(
                TrimmedAudioResult(
                    fileName = clipLabel,
                    mimeType = "audio/mp4",
                    bytes = bytes,
                    durationMs = targetDurationMs
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Error al recortar el audio."))
        } finally {
            try { extractor?.release() } catch (_: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (_: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
            tempOut.delete()
        }
    }

    private fun feedPcmToEncoder(
        encoder: MediaCodec,
        pcmBuffer: ByteBuffer,
        offset: Int,
        size: Int,
        ptsUs: Long,
        rawChannels: Int,
        targetChannels: Int,
        drainFn: () -> Unit
    ) {
        pcmBuffer.position(offset)
        pcmBuffer.limit(offset + size)

        val payload: ByteArray = if (rawChannels > 2 && targetChannels == 2) {
            // Downsample multichannel PCM16 to stereo
            val samples = size / (2 * rawChannels)
            val out = ByteArray(samples * 4)
            var outIdx = 0
            repeat(samples) {
                val l0 = pcmBuffer.get()
                val l1 = pcmBuffer.get()
                val r0 = pcmBuffer.get()
                val r1 = pcmBuffer.get()
                out[outIdx++] = l0
                out[outIdx++] = l1
                out[outIdx++] = r0
                out[outIdx++] = r1
                repeat((rawChannels - 2) * 2) { if (pcmBuffer.hasRemaining()) pcmBuffer.get() }
            }
            out
        } else {
            ByteArray(size).also { pcmBuffer.get(it) }
        }

        var written = 0
        while (written < payload.size) {
            drainFn()
            val encInIdx = encoder.dequeueInputBuffer(10_000L)
            if (encInIdx >= 0) {
                val encBuf = encoder.getInputBuffer(encInIdx) ?: break
                encBuf.clear()
                val chunk = minOf(encBuf.remaining(), payload.size - written)
                encBuf.put(payload, written, chunk)
                encoder.queueInputBuffer(encInIdx, 0, chunk, ptsUs, 0)
                written += chunk
            }
        }
    }

    private fun setExtractorDataSource(context: Context, extractor: MediaExtractor, uri: Uri) {
        if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            extractor.setDataSource(uri.path!!)
        } else {
            extractor.setDataSource(context, uri, null)
        }
    }

    private fun MediaFormat.getIntegerOrDefault(key: String, defaultVal: Int): Int =
        if (containsKey(key)) getInteger(key) else defaultVal
}
