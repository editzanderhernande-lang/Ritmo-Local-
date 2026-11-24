package com.example.player

import com.example.model.LocalTrack
import com.example.model.PlaybackEnginePrefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class SmartDjRuntimeState(
    val isCrossfading: Boolean = false,
    val crossfadeProgress: Float = 0f,
    val currentAnalysis: AudioTrackAnalysis? = null,
    val nextAnalysis: AudioTrackAnalysis? = null,
    val lastTransitionNote: String? = null
)

/**
 * In-process bridge synchronizing PlaybackService's crossfade/DJ engine with UI controls.
 */
object PlaybackEngineBus {

    private val _config = MutableStateFlow(PlaybackEnginePrefs())
    val config: StateFlow<PlaybackEnginePrefs> = _config.asStateFlow()

    private val _djState = MutableStateFlow(SmartDjRuntimeState())
    val djState: StateFlow<SmartDjRuntimeState> = _djState.asStateFlow()

    private val _manualMixRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val manualMixRequests: SharedFlow<Unit> = _manualMixRequests.asSharedFlow()

    fun updateConfig(prefs: PlaybackEnginePrefs) {
        _config.value = prefs
    }

    fun requestManualDjMix() {
        _manualMixRequests.tryEmit(Unit)
    }

    internal fun updateDjRuntime(transform: (SmartDjRuntimeState) -> SmartDjRuntimeState) {
        _djState.value = transform(_djState.value)
    }

    /**
     * Reorders upcoming items to reduce back-to-back artist repetition and deduplicate tracks
     * while preserving the currently playing track index untouched.
     */
    fun buildSmartQueue(
        queue: List<LocalTrack>,
        currentIndex: Int,
        avoidRepeats: Boolean,
        seed: Long = System.nanoTime()
    ): List<LocalTrack> {
        if (queue.size <= 2) return queue
        val safeCurrent = currentIndex.coerceIn(0, queue.lastIndex)
        val currentTrack = queue[safeCurrent]
        val playedPrefix = queue.take(safeCurrent + 1)
        val upcomingRaw = queue.drop(safeCurrent + 1)
        if (upcomingRaw.isEmpty()) return queue

        val filtered = if (avoidRepeats) {
            val seenKeys = playedPrefix.map { it.trackKey }.toHashSet()
            upcomingRaw.filter { track ->
                seenKeys.add(track.trackKey)
            }.ifEmpty { upcomingRaw.distinctBy { it.trackKey } }
        } else {
            upcomingRaw
        }

        val rnd = kotlin.random.Random(seed)
        val pool = filtered.shuffled(rnd).toMutableList()
        val smartOrdered = mutableListOf<LocalTrack>()
        var lastArtist = currentTrack.artist.lowercase()

        while (pool.isNotEmpty()) {
            val nextIdx = pool.indexOfFirst { !it.artist.equals(lastArtist, ignoreCase = true) }
            val chosen = if (nextIdx >= 0) pool.removeAt(nextIdx) else pool.removeAt(0)
            smartOrdered.add(chosen)
            lastArtist = chosen.artist.lowercase()
        }

        return playedPrefix + smartOrdered
    }

    fun deduplicateQueuePreservingCurrent(
        queue: List<LocalTrack>,
        currentIndex: Int
    ): Pair<List<LocalTrack>, Int> {
        if (queue.size <= 1) return queue to currentIndex
        val safeIdx = currentIndex.coerceIn(0, queue.lastIndex)
        val activeKey = queue[safeIdx].trackKey
        val seen = HashSet<String>(queue.size).apply { add(activeKey) }
        val result = ArrayList<LocalTrack>(queue.size)
        var newCurrentIdx = 0
        queue.forEachIndexed { idx, track ->
            if (idx == safeIdx || seen.add(track.trackKey)) {
                if (idx == safeIdx) newCurrentIdx = result.size
                result.add(track)
            }
        }
        if (result.getOrNull(newCurrentIdx)?.trackKey != activeKey) {
            newCurrentIdx = result.indexOfFirst { it.trackKey == activeKey }.coerceAtLeast(0)
        }
        return result to newCurrentIdx
    }
}
