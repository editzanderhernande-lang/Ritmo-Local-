package com.example.voice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.annotation.VisibleForTesting

enum class VoiceAudioRoute(val labelEs: String) {
    PHONE_SPEAKER("Altavoz del teléfono"),
    BLUETOOTH_HEADSET("Auriculares Bluetooth")
}

object VoiceAudioRouteDetector {

    @VisibleForTesting
    @Volatile
    internal var routeOverrideForTesting: VoiceAudioRoute? = null

    private val bluetoothDeviceTypes: Set<Int> by lazy {
        buildSet {
            add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
            add(AudioDeviceInfo.TYPE_HEARING_AID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AudioDeviceInfo.TYPE_BLE_HEADSET)
                add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(AudioDeviceInfo.TYPE_BLE_BROADCAST)
            }
        }
    }

    @Suppress("DEPRECATION")
    fun detect(context: Context): VoiceAudioRoute {
        routeOverrideForTesting?.let { return it }
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return VoiceAudioRoute.PHONE_SPEAKER

        val outputTypes = runCatching {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type }.toIntArray()
        }.getOrElse { IntArray(0) }

        val legacyBt = runCatching { am.isBluetoothA2dpOn || am.isBluetoothScoOn }.getOrDefault(false)
        return detectFromDeviceTypes(outputTypes, legacyBt)
    }

    fun detectFromDeviceTypes(outputDeviceTypes: IntArray, legacyBluetoothOn: Boolean = false): VoiceAudioRoute {
        if (legacyBluetoothOn) return VoiceAudioRoute.BLUETOOTH_HEADSET
        val hasBluetoothOutput = outputDeviceTypes.any { it in bluetoothDeviceTypes }
        return if (hasBluetoothOutput) VoiceAudioRoute.BLUETOOTH_HEADSET else VoiceAudioRoute.PHONE_SPEAKER
    }
}
