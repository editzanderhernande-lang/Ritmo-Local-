package com.example.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerUiState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val stopAfterCurrentTrack: Boolean = false,
    val remainingSongsToStop: Int = 0,
    val targetSongsTotal: Int = 0,
    val fadeOutEnabled: Boolean = true,
    val fadeOutDurationSec: Int = 3,
    val justExpiredAtMs: Long? = null
) {
    val progressFraction: Float
        get() = when {
            isActive && !stopAfterCurrentTrack && remainingSongsToStop <= 0 && totalSeconds > 0L ->
                (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
            isActive && targetSongsTotal > 0 ->
                (remainingSongsToStop.toFloat() / targetSongsTotal.toFloat()).coerceIn(0f, 1f)
            else -> 0f
        }
}

fun formatSleepTimerCountdown(totalSeconds: Long): String {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val hours = clamped / 3600L
    val minutes = (clamped % 3600L) / 60L
    val seconds = clamped % 60L
    return if (hours > 0L) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

class SleepTimerEngine(
    private val scope: CoroutineScope,
    private val elapsedRealtimeMs: () -> Long,
    private val nowWallClockMs: () -> Long = { System.currentTimeMillis() },
    private val tickIntervalMs: Long = 250L,
    private val onExpired: () -> Unit
) {
    private var countdownJob: Job? = null
    private var endElapsedMs: Long = 0L

    private val _state = MutableStateFlow(SleepTimerUiState())
    val state: StateFlow<SleepTimerUiState> = _state.asStateFlow()

    fun setFadeOutEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(fadeOutEnabled = enabled)
    }

    fun setFadeOutDurationSeconds(seconds: Int) {
        _state.value = _state.value.copy(fadeOutDurationSec = seconds.coerceIn(1, 15))
    }

    fun startCountdown(durationSeconds: Long) {
        val safeSeconds = durationSeconds.coerceAtLeast(1L)
        val fade = _state.value.fadeOutEnabled
        countdownJob?.cancel()
        val now = elapsedRealtimeMs()
        endElapsedMs = now + safeSeconds * 1000L
        _state.value = SleepTimerUiState(
            isActive = true,
            remainingSeconds = safeSeconds,
            totalSeconds = safeSeconds,
            stopAfterCurrentTrack = false,
            remainingSongsToStop = 0,
            targetSongsTotal = 0,
            fadeOutEnabled = fade,
            justExpiredAtMs = null
        )
        countdownJob = scope.launch {
            while (isActive) {
                delay(tickIntervalMs)
                val remainingMs = endElapsedMs - elapsedRealtimeMs()
                if (remainingMs <= 0L) {
                    _state.value = SleepTimerUiState(
                        isActive = false,
                        remainingSeconds = 0L,
                        totalSeconds = 0L,
                        stopAfterCurrentTrack = false,
                        remainingSongsToStop = 0,
                        targetSongsTotal = 0,
                        fadeOutEnabled = _state.value.fadeOutEnabled,
                        justExpiredAtMs = nowWallClockMs()
                    )
                    onExpired()
                    break
                } else {
                    val remSec = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)
                    val cur = _state.value
                    if (cur.remainingSeconds != remSec) {
                        _state.value = cur.copy(remainingSeconds = remSec)
                    }
                }
            }
        }
    }

    fun startSongCountdown(songCount: Int) {
        val safeCount = songCount.coerceIn(1, 30)
        val fade = _state.value.fadeOutEnabled
        countdownJob?.cancel()
        countdownJob = null
        endElapsedMs = 0L
        _state.value = SleepTimerUiState(
            isActive = true,
            remainingSeconds = 0L,
            totalSeconds = 0L,
            stopAfterCurrentTrack = safeCount == 1,
            remainingSongsToStop = safeCount,
            targetSongsTotal = safeCount,
            fadeOutEnabled = fade,
            justExpiredAtMs = null
        )
    }

    fun addSeconds(additionalSeconds: Long) {
        val safeAdd = additionalSeconds.coerceAtLeast(1L)
        val cur = _state.value
        if (cur.isActive && !cur.stopAfterCurrentTrack && cur.remainingSongsToStop <= 0) {
            endElapsedMs += safeAdd * 1000L
            val remainingMs = (endElapsedMs - elapsedRealtimeMs()).coerceAtLeast(1000L)
            val newRemSec = ((remainingMs + 999L) / 1000L).coerceAtLeast(1L)
            val newTotal = maxOf(cur.totalSeconds + safeAdd, newRemSec)
            _state.value = cur.copy(
                remainingSeconds = newRemSec,
                totalSeconds = newTotal
            )
        } else {
            startCountdown(safeAdd)
        }
    }

    fun setStopAfterCurrentTrack(enabled: Boolean) {
        if (!enabled) {
            cancel()
            return
        }
        startSongCountdown(1)
    }

    fun consumeStopAfterCurrentTrack(): Boolean {
        val cur = _state.value
        if (!cur.isActive) return false
        val remaining = when {
            cur.remainingSongsToStop > 0 -> cur.remainingSongsToStop
            cur.stopAfterCurrentTrack -> 1
            else -> return false
        }
        val next = remaining - 1
        return if (next <= 0) {
            _state.value = SleepTimerUiState(
                isActive = false,
                remainingSeconds = 0L,
                totalSeconds = 0L,
                stopAfterCurrentTrack = false,
                remainingSongsToStop = 0,
                targetSongsTotal = 0,
                fadeOutEnabled = cur.fadeOutEnabled,
                justExpiredAtMs = nowWallClockMs()
            )
            true
        } else {
            _state.value = cur.copy(
                remainingSongsToStop = next,
                stopAfterCurrentTrack = next == 1
            )
            false
        }
    }

    fun cancel() {
        val cur = _state.value
        countdownJob?.cancel()
        countdownJob = null
        endElapsedMs = 0L
        _state.value = SleepTimerUiState(
            fadeOutEnabled = cur.fadeOutEnabled,
            fadeOutDurationSec = cur.fadeOutDurationSec
        )
    }

    fun clearExpiredFlag() {
        if (_state.value.justExpiredAtMs != null) {
            _state.value = _state.value.copy(justExpiredAtMs = null)
        }
    }
}

object SleepTimerController {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var stopPlaybackCallback: (() -> Unit)? = null

    @Volatile
    private var fallbackStopCallback: (() -> Unit)? = null

    private val engine = SleepTimerEngine(
        scope = serviceScope,
        elapsedRealtimeMs = { SystemClock.elapsedRealtime() },
        onExpired = {
            val primary = stopPlaybackCallback
            if (primary != null) {
                primary.invoke()
            } else {
                fallbackStopCallback?.invoke()
            }
        }
    )

    val state: StateFlow<SleepTimerUiState> = engine.state

    fun attachServiceStopCallback(callback: () -> Unit) {
        stopPlaybackCallback = callback
    }

    fun detachServiceStopCallback() {
        stopPlaybackCallback = null
    }

    fun attachFallbackStopCallback(callback: () -> Unit) {
        fallbackStopCallback = callback
    }

    fun detachFallbackStopCallback() {
        fallbackStopCallback = null
    }

    fun startCountdown(durationSeconds: Long) = engine.startCountdown(durationSeconds)

    fun startSongCountdown(songCount: Int) = engine.startSongCountdown(songCount)

    fun setFadeOutEnabled(enabled: Boolean) = engine.setFadeOutEnabled(enabled)

    fun setFadeOutDurationSeconds(seconds: Int) = engine.setFadeOutDurationSeconds(seconds)

    fun addSeconds(additionalSeconds: Long) = engine.addSeconds(additionalSeconds)

    fun toggleStopAfterCurrentTrack() {
        val currentlyEnabled = engine.state.value.let { it.isActive && (it.stopAfterCurrentTrack || it.remainingSongsToStop == 1) }
        engine.setStopAfterCurrentTrack(!currentlyEnabled)
    }

    fun consumeStopAfterCurrentTrack(): Boolean = engine.consumeStopAfterCurrentTrack()

    fun cancel() = engine.cancel()

    fun clearExpiredFlag() = engine.clearExpiredFlag()
}
