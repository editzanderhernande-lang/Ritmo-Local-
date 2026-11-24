package com.example.player

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.model.AudioDeviceProfileType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors active audio output routes (Phone speaker, Wired/USB headphones, Bluetooth, HDMI/Line)
 * purely to apply Ritmo Local's internal DSP/EQ profiles per output device without altering Android
 * global system audio settings.
 */
class DeviceAudioProfileManager(
    context: Context,
    private val onOutputDeviceChanged: (AudioDeviceProfileType) -> Unit
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _activeDevice = MutableStateFlow(detectCurrentOutput(appContext))
    val activeDevice: StateFlow<AudioDeviceProfileType> = _activeDevice.asStateFlow()

    private var registered = false

    private val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                refreshDetectedDevice()
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                refreshDetectedDevice()
            }
        }
    } else null

    fun startMonitoring() {
        if (registered) return
        refreshDetectedDevice()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            runCatching {
                audioManager?.registerAudioDeviceCallback(
                    deviceCallback,
                    Handler(Looper.getMainLooper())
                )
                registered = true
            }
        }
    }

    fun stopMonitoring() {
        if (!registered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            runCatching {
                audioManager?.unregisterAudioDeviceCallback(deviceCallback)
            }
        }
        registered = false
    }

    fun refreshDetectedDevice(): AudioDeviceProfileType {
        val detected = detectCurrentOutput(appContext)
        val previous = _activeDevice.value
        _activeDevice.value = detected
        if (detected != previous) {
            onOutputDeviceChanged(detected)
        }
        return detected
    }

    companion object {
        @Suppress("DEPRECATION")
        fun detectCurrentOutput(context: Context): AudioDeviceProfileType {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return AudioDeviceProfileType.PHONE_SPEAKER
            val deviceTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                runCatching {
                    am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type }.toIntArray()
                }.getOrElse { IntArray(0) }
            } else {
                IntArray(0)
            }
            val legacyBt = runCatching { am.isBluetoothA2dpOn || am.isBluetoothScoOn }.getOrDefault(false)
            val legacyWired = runCatching { am.isWiredHeadsetOn }.getOrDefault(false)
            return detectFromDeviceTypes(deviceTypes, legacyBt, legacyWired)
        }

        fun detectFromDeviceTypes(
            deviceTypes: IntArray,
            legacyBluetoothOn: Boolean = false,
            legacyWiredOn: Boolean = false
        ): AudioDeviceProfileType {
            if (legacyBluetoothOn) return AudioDeviceProfileType.BLUETOOTH
            var hasWired = legacyWiredOn
            var hasOther = false

            for (type in deviceTypes) {
                if (isBluetoothOutputType(type)) {
                    return AudioDeviceProfileType.BLUETOOTH
                }
                if (isWiredOutputType(type)) {
                    hasWired = true
                } else if (isOtherExternalOutputType(type)) {
                    hasOther = true
                }
            }
            return when {
                hasWired -> AudioDeviceProfileType.WIRED_HEADSET
                hasOther -> AudioDeviceProfileType.OTHER_OUTPUT
                else -> AudioDeviceProfileType.PHONE_SPEAKER
            }
        }

        private fun isBluetoothOutputType(type: Int): Boolean {
            return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        type == AudioDeviceInfo.TYPE_BLE_BROADCAST)) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    type == AudioDeviceInfo.TYPE_HEARING_AID)
        }

        private fun isWiredOutputType(type: Int): Boolean {
            return type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        }

        private fun isOtherExternalOutputType(type: Int): Boolean {
            return type == AudioDeviceInfo.TYPE_HDMI ||
                type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                type == AudioDeviceInfo.TYPE_AUX_LINE ||
                type == AudioDeviceInfo.TYPE_DOCK
        }
    }
}
