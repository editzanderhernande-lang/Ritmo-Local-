package com.example.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates audio focus and transient volume ducking between ExoPlayer and voice control:
 * - Keeping [isVoiceMicActive] true ensures ExoPlayer keeps playing while listening for commands.
 * - [isVoiceDucking] softly lowers music volume briefly ONLY during an active command window
 *   (after saying "Ridmo" or tapping Push-to-Talk) so commands are clearly understood over
 *   speakers without ever pausing playback.
 */
object VoiceAudioFocusCoordinator {
    private val _isVoiceMicActive = MutableStateFlow(false)
    val isVoiceMicActive: StateFlow<Boolean> = _isVoiceMicActive.asStateFlow()

    private val _isVoiceDucking = MutableStateFlow(false)
    val isVoiceDucking: StateFlow<Boolean> = _isVoiceDucking.asStateFlow()

    fun setVoiceMicActive(active: Boolean) {
        _isVoiceMicActive.value = active
        if (!active) {
            _isVoiceDucking.value = false
        }
    }

    fun setTransientVoiceDucking(ducking: Boolean) {
        _isVoiceDucking.value = ducking
    }
}
