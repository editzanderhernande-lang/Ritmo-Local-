package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.model.AudioChannelMode
import java.nio.ByteBuffer
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
class MonoAndBalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var channelMode: AudioChannelMode = AudioChannelMode.STEREO

    @Volatile
    private var balanceLR: Float = 0f // -1.0 (Left) .. +1.0 (Right)

    fun setChannelConfig(mode: AudioChannelMode, balance: Float) {
        channelMode = mode
        balanceLR = balance.coerceIn(-1f, 1f)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val mode = channelMode
        val bal = balanceLR
        val isIdentity = mode == AudioChannelMode.STEREO && kotlin.math.abs(bal) < 0.01f

        val output = replaceOutputBuffer(remaining)
        val encoding = inputAudioFormat.encoding
        if (isIdentity ||
            inputAudioFormat.channelCount != 2 ||
            (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT)
        ) {
            output.put(inputBuffer)
            output.flip()
            return
        }

        val leftGain = if (bal > 0f) (1f - bal).coerceIn(0f, 1f) else 1f
        val rightGain = if (bal < 0f) (1f + bal).coerceIn(0f, 1f) else 1f

        if (encoding == C.ENCODING_PCM_FLOAT) {
            while (inputBuffer.remaining() >= 8) {
                val l = inputBuffer.float
                val r = inputBuffer.float
                val (outL, outR) = if (mode == AudioChannelMode.MONO) {
                    val mono = (l + r) * 0.5f
                    (mono * leftGain).coerceIn(-1f, 1f) to (mono * rightGain).coerceIn(-1f, 1f)
                } else {
                    (l * leftGain).coerceIn(-1f, 1f) to (r * rightGain).coerceIn(-1f, 1f)
                }
                output.putFloat(outL)
                output.putFloat(outR)
            }
        } else {
            while (inputBuffer.remaining() >= 4) {
                val l = inputBuffer.short.toInt()
                val r = inputBuffer.short.toInt()
                val (outL, outR) = if (mode == AudioChannelMode.MONO) {
                    val mono = ((l + r) * 0.5f)
                    (mono * leftGain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()) to
                        (mono * rightGain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                } else {
                    (l * leftGain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()) to
                        (r * rightGain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                }
                output.putShort(outL.toShort())
                output.putShort(outR.toShort())
            }
        }
        while (inputBuffer.hasRemaining()) {
            output.put(inputBuffer.get())
        }
        output.flip()
    }
}
