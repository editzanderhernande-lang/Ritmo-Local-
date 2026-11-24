package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.diagnostics.AppDiagnosticsManager
import com.example.diagnostics.DiagnosticModule
import com.example.diagnostics.DiagnosticSeverity
import com.example.diagnostics.RepairActionId
import com.example.model.LocalTrack
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentTrack: LocalTrack? = null,
    val queue: List<LocalTrack> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val isControllerReady: Boolean = false
)

class PlaybackControllerManager(
    context: Context,
    private val scope: CoroutineScope,
    private val onListeningTick: (track: LocalTrack, deltaSeconds: Long, positionMs: Long) -> Unit,
    private val onTrackStarted: (track: LocalTrack) -> Unit,
    private val onPausedOrEnded: (track: LocalTrack?, positionMs: Long) -> Unit,
    private val onModesChanged: (shuffleEnabled: Boolean, repeatMode: Int) -> Unit = { _, _ -> },
    private val onQueueSnapshotChanged: (queue: List<LocalTrack>, currentIndex: Int, positionMs: Long) -> Unit = { _, _, _ -> }
) {
    private val appContext = context.applicationContext
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var tickerJob: Job? = null
    private var lastTrackedMediaId: String? = null
    private var trackByKey: Map<String, LocalTrack> = emptyMap()
    private var includeArtworkInNotification: Boolean = true
    private var pendingPlayRequest: Triple<List<LocalTrack>, Int, Long>? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val controller = mediaController
            if (controller != null) {
                syncStateFromController(controller, countAsNewStart = false)
            } else {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }
            if (isPlaying) {
                startTicker()
            } else {
                stopTicker()
                val curTrack = controller?.let(::resolveCurrentTrack) ?: _state.value.currentTrack
                val curPos = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
                AppDiagnosticsManager.updateProtectedPlaybackSnapshot(curTrack?.trackKey, curTrack?.title, curPos)
                onPausedOrEnded(curTrack, curPos)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val controller = mediaController ?: return
            syncStateFromController(controller, countAsNewStart = true)
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val controller = mediaController ?: return
            syncStateFromController(controller, countAsNewStart = false)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val controller = mediaController ?: return
            syncStateFromController(controller, countAsNewStart = false)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val controller = mediaController ?: return
            syncStateFromController(controller, countAsNewStart = false)
            if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                stopTicker()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val controller = mediaController ?: return
            _state.value = _state.value.copy(
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                durationMs = controller.duration.takeIf { it > 0 } ?: _state.value.durationMs
            )
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.value = _state.value.copy(shuffleEnabled = shuffleModeEnabled)
            onModesChanged(shuffleModeEnabled, _state.value.repeatMode)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.value = _state.value.copy(repeatMode = repeatMode)
            onModesChanged(_state.value.shuffleEnabled, repeatMode)
        }
    }

    fun setNotificationArtworkEnabled(enabled: Boolean) {
        includeArtworkInNotification = enabled
    }

    fun applyPersistedModes(shuffleEnabled: Boolean, repeatMode: Int) {
        _state.value = _state.value.copy(shuffleEnabled = shuffleEnabled, repeatMode = repeatMode)
        mediaController?.let { controller ->
            if (controller.shuffleModeEnabled != shuffleEnabled) {
                controller.shuffleModeEnabled = shuffleEnabled
            }
            if (controller.repeatMode != repeatMode) {
                controller.repeatMode = repeatMode
            }
        }
    }

    fun ensureConnected(initialShuffle: Boolean = false, initialRepeat: Int = Player.REPEAT_MODE_OFF) {
        val activeController = mediaController
        if (activeController != null) {
            syncStateFromController(activeController, countAsNewStart = false)
            if (activeController.isPlaying) {
                startTicker()
            }
            return
        }
        connect(initialShuffle, initialRepeat)
    }

    fun connect(initialShuffle: Boolean = false, initialRepeat: Int = Player.REPEAT_MODE_OFF) {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, sessionToken)
            .setListener(
                object : MediaController.Listener {
                    override fun onDisconnected(controller: MediaController) {
                        stopTicker()
                        mediaController = null
                        controllerFuture = null
                        _state.value = _state.value.copy(isControllerReady = false)
                        AppDiagnosticsManager.logTech(
                            level = "WARN",
                            module = DiagnosticModule.PLAYBACK_SERVICE,
                            message = "MediaController desconectado temporalmente; estado protegido para reconexión."
                        )
                    }
                }
            )
            .buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    controller.addListener(playerListener)
                    SleepTimerController.attachFallbackStopCallback {
                        mediaController?.pause()
                    }

                    val pending = pendingPlayRequest
                    pendingPlayRequest = null
                    if (pending != null) {
                        playQueue(pending.first, pending.second, pending.third)
                    } else {
                        if (controller.mediaItemCount == 0) {
                            controller.shuffleModeEnabled = initialShuffle
                            controller.repeatMode = initialRepeat
                        }
                        // Recover live service state (track, artwork, queue, position, isPlaying) without restarting or duplicating!
                        syncStateFromController(controller, countAsNewStart = false)
                        if (controller.isPlaying) {
                            startTicker()
                        }
                    }
                } catch (e: Exception) {
                    controllerFuture = null
                    _state.value = _state.value.copy(isControllerReady = false)
                    AppDiagnosticsManager.recordException(
                        module = DiagnosticModule.PLAYBACK_SERVICE,
                        title = "Error al enlazar con PlaybackService",
                        throwable = e,
                        severity = DiagnosticSeverity.HIGH,
                        plainExplanation = "No se pudo establecer la conexión inicial con el servicio de música en segundo plano.",
                        proposedSolution = "Usa la reparación automática para reconectar el servicio.",
                        repairAction = RepairActionId.RECONNECT_PLAYER
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun syncLibraryTracks(tracks: List<LocalTrack>) {
        if (tracks.isEmpty()) return
        val incoming = tracks.associateBy { it.trackKey }
        trackByKey = trackByKey + incoming
        val current = _state.value.currentTrack
        val enrichedCurrent = current?.let { cur ->
            incoming[cur.trackKey] ?: cur
        }
        val enrichedQueue = _state.value.queue.map { q -> incoming[q.trackKey] ?: q }
        _state.value = _state.value.copy(
            currentTrack = enrichedCurrent,
            queue = enrichedQueue
        )
    }

    fun playQueue(
        tracks: List<LocalTrack>,
        startIndex: Int = 0,
        startPositionMs: Long = 0L
    ) {
        if (tracks.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        trackByKey = tracks.associateBy { it.trackKey }
        val controller = mediaController
        val selectedTrack = tracks[safeIndex]

        _state.value = _state.value.copy(
            currentTrack = selectedTrack,
            queue = tracks,
            currentQueueIndex = safeIndex,
            positionMs = startPositionMs.coerceAtLeast(0L),
            durationMs = selectedTrack.durationMs
        )

        if (controller == null) {
            pendingPlayRequest = Triple(tracks, safeIndex, startPositionMs)
            connect(_state.value.shuffleEnabled, _state.value.repeatMode)
            return
        }

        val mediaItems = tracks.map { buildMediaItem(it) }
        lastTrackedMediaId = selectedTrack.trackKey
        controller.setMediaItems(mediaItems, safeIndex, startPositionMs.coerceAtLeast(0L))
        controller.prepare()
        controller.play()
        onTrackStarted(selectedTrack)
        onQueueSnapshotChanged(tracks, safeIndex, startPositionMs.coerceAtLeast(0L))
    }

    /**
     * Restores saved queue metadata into state ONLY when controller has no active media yet
     * so ongoing background playback is never interrupted or duplicated.
     */
    fun restorePersistedQueueIfIdle(
        tracks: List<LocalTrack>,
        startIndex: Int,
        positionMs: Long
    ) {
        if (tracks.isEmpty()) return
        val controller = mediaController
        if (controller != null && (controller.mediaItemCount > 0 || controller.isPlaying)) return
        if (_state.value.currentTrack != null || _state.value.queue.isNotEmpty()) return
        val safeIdx = startIndex.coerceIn(0, tracks.lastIndex)
        trackByKey = trackByKey + tracks.associateBy { it.trackKey }
        val target = tracks[safeIdx]
        _state.value = _state.value.copy(
            currentTrack = target,
            queue = tracks,
            currentQueueIndex = safeIdx,
            positionMs = positionMs.coerceAtLeast(0L),
            durationMs = target.durationMs
        )
    }

    fun playNext(track: LocalTrack) {
        val controller = mediaController
        val currentQueue = _state.value.queue
        if (controller == null || controller.mediaItemCount == 0 || currentQueue.isEmpty()) {
            playQueue(listOf(track), 0, 0L)
            return
        }

        val currentIdx = controller.currentMediaItemIndex.coerceAtLeast(0)
        val insertIndex = (currentIdx + 1).coerceAtMost(controller.mediaItemCount)
        trackByKey = trackByKey + (track.trackKey to track)
        val updatedNext = currentQueue.toMutableList().apply {
            val listInsertIdx = insertIndex.coerceIn(0, size)
            add(listInsertIdx, track)
        }
        _state.value = _state.value.copy(queue = updatedNext)
        controller.addMediaItem(insertIndex, buildMediaItem(track))
        onQueueSnapshotChanged(updatedNext, currentIdx, _state.value.positionMs)
    }

    fun appendToQueue(track: LocalTrack) {
        val controller = mediaController
        val currentQueue = _state.value.queue
        if (controller == null || controller.mediaItemCount == 0 || currentQueue.isEmpty()) {
            playQueue(listOf(track), 0, 0L)
            return
        }
        trackByKey = trackByKey + (track.trackKey to track)
        val appended = currentQueue + track
        _state.value = _state.value.copy(queue = appended)
        controller.addMediaItem(buildMediaItem(track))
        onQueueSnapshotChanged(appended, _state.value.currentQueueIndex, _state.value.positionMs)
    }

    fun jumpToQueueIndex(index: Int) {
        val queue = _state.value.queue
        if (index !in queue.indices) return
        val controller = mediaController
        if (controller != null && index < controller.mediaItemCount) {
            controller.seekTo(index, 0L)
            if (!controller.isPlaying) {
                controller.prepare()
                controller.play()
            }
            _state.value = _state.value.copy(
                currentQueueIndex = index,
                currentTrack = queue[index],
                positionMs = 0L,
                durationMs = queue[index].durationMs
            )
        } else {
            playQueue(queue, index, 0L)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val queue = _state.value.queue
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return
        val reordered = queue.toMutableList().apply {
            val moved = removeAt(fromIndex)
            add(toIndex, moved)
        }
        val oldActiveIdx = _state.value.currentQueueIndex
        val newActiveIdx = when {
            oldActiveIdx == fromIndex -> toIndex
            fromIndex < oldActiveIdx && toIndex >= oldActiveIdx -> oldActiveIdx - 1
            fromIndex > oldActiveIdx && toIndex <= oldActiveIdx -> oldActiveIdx + 1
            else -> oldActiveIdx
        }.coerceIn(0, reordered.lastIndex)

        _state.value = _state.value.copy(
            queue = reordered,
            currentQueueIndex = newActiveIdx,
            currentTrack = reordered.getOrNull(newActiveIdx)
        )
        mediaController?.let { controller ->
            if (fromIndex < controller.mediaItemCount && toIndex < controller.mediaItemCount) {
                controller.moveMediaItem(fromIndex, toIndex)
            }
        }
        onQueueSnapshotChanged(reordered, newActiveIdx, _state.value.positionMs)
    }

    fun removeQueueItem(index: Int) {
        val queue = _state.value.queue
        if (index !in queue.indices || queue.size <= 1) return
        val updated = queue.toMutableList().apply { removeAt(index) }
        val oldActiveIdx = _state.value.currentQueueIndex
        val newActiveIdx = when {
            index < oldActiveIdx -> (oldActiveIdx - 1).coerceAtLeast(0)
            index == oldActiveIdx -> oldActiveIdx.coerceAtMost(updated.lastIndex)
            else -> oldActiveIdx
        }
        _state.value = _state.value.copy(
            queue = updated,
            currentQueueIndex = newActiveIdx,
            currentTrack = updated.getOrNull(newActiveIdx)
        )
        mediaController?.let { controller ->
            if (index < controller.mediaItemCount) {
                controller.removeMediaItem(index)
            }
        }
        onQueueSnapshotChanged(updated, newActiveIdx, _state.value.positionMs)
    }

    fun smartShuffleQueue(avoidRepeats: Boolean) {
        val queue = _state.value.queue
        if (queue.size <= 2) return
        val activeIdx = _state.value.currentQueueIndex.coerceIn(0, queue.lastIndex)
        val smartQueue = PlaybackEngineBus.buildSmartQueue(
            queue = queue,
            currentIndex = activeIdx,
            avoidRepeats = avoidRepeats
        )
        applyUpcomingReorderedQueue(smartQueue, activeIdx)
    }

    fun deduplicateQueue(): Int {
        val queue = _state.value.queue
        if (queue.size <= 1) return 0
        val activeIdx = _state.value.currentQueueIndex.coerceIn(0, queue.lastIndex)
        val (deduped, newActiveIdx) = PlaybackEngineBus.deduplicateQueuePreservingCurrent(queue, activeIdx)
        val removed = queue.size - deduped.size
        if (removed <= 0) return 0
        val controller = mediaController
        if (controller != null && controller.mediaItemCount == queue.size) {
            val seen = HashSet<String>()
            val dropIndices = mutableListOf<Int>()
            queue.forEachIndexed { idx, t ->
                if (idx != activeIdx && !seen.add(t.trackKey)) {
                    dropIndices.add(idx)
                } else if (idx == activeIdx) {
                    seen.add(t.trackKey)
                }
            }
            dropIndices.asReversed().forEach { idx ->
                if (idx < controller.mediaItemCount && idx != controller.currentMediaItemIndex) {
                    controller.removeMediaItem(idx)
                }
            }
        }
        _state.value = _state.value.copy(
            queue = deduped,
            currentQueueIndex = newActiveIdx,
            currentTrack = deduped.getOrNull(newActiveIdx)
        )
        onQueueSnapshotChanged(deduped, newActiveIdx, _state.value.positionMs)
        return removed
    }

    private fun applyUpcomingReorderedQueue(smartQueue: List<LocalTrack>, activeIdx: Int) {
        val controller = mediaController
        if (controller != null && controller.mediaItemCount > activeIdx + 1) {
            // Preserve currently playing MediaItem undisturbed; replace upcoming tail cleanly
            val curCount = controller.mediaItemCount
            if (curCount > activeIdx + 1) {
                controller.removeMediaItems(activeIdx + 1, curCount)
            }
            val newTail = smartQueue.drop(activeIdx + 1).map(::buildMediaItem)
            if (newTail.isNotEmpty()) {
                controller.addMediaItems(newTail)
            }
        }
        _state.value = _state.value.copy(
            queue = smartQueue,
            currentQueueIndex = activeIdx,
            currentTrack = smartQueue.getOrNull(activeIdx)
        )
        onQueueSnapshotChanged(smartQueue, activeIdx, _state.value.positionMs)
    }

    fun refreshTrackMetadata(updatedTrack: LocalTrack) {
        trackByKey = trackByKey + (updatedTrack.trackKey to updatedTrack)
        val current = _state.value.currentTrack
        if (current?.trackKey == updatedTrack.trackKey) {
            _state.value = _state.value.copy(currentTrack = updatedTrack)
        }
        val controller = mediaController ?: return
        val count = controller.mediaItemCount
        for (i in 0 until count) {
            if (controller.getMediaItemAt(i).mediaId == updatedTrack.trackKey) {
                controller.replaceMediaItem(i, buildMediaItem(updatedTrack))
            }
        }
    }

    private fun buildMediaItem(track: LocalTrack): MediaItem {
        val extras = Bundle().apply {
            putLong(EXTRA_ID, track.id)
            putString(EXTRA_TRACK_KEY, track.trackKey)
            putLong(EXTRA_ALBUM_ID, track.albumId)
            putLong(EXTRA_DURATION_MS, track.durationMs)
            putString(EXTRA_CONTENT_URI, track.contentUri)
            track.albumArtUri?.let { putString(EXTRA_ALBUM_ART_URI, it) }
            putLong(EXTRA_DATE_ADDED, track.dateAdded)
            putString(EXTRA_FORMAT_LABEL, track.formatLabel)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setExtras(extras)
            .apply {
                if (includeArtworkInNotification) {
                    track.albumArtUri?.let { setArtworkUri(Uri.parse(it)) }
                }
            }
            .build()

        return MediaItem.Builder()
            .setMediaId(track.trackKey)
            .setUri(Uri.parse(track.contentUri))
            .setMediaMetadata(metadata)
            .build()
    }

    fun togglePlayPause() {
        val controller = mediaController
        if (controller == null) {
            connect(_state.value.shuffleEnabled, _state.value.repeatMode)
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.mediaItemCount == 0 && _state.value.queue.isNotEmpty()) {
                val idx = _state.value.queue.indexOfFirst { it.trackKey == _state.value.currentTrack?.trackKey }.coerceAtLeast(0)
                playQueue(_state.value.queue, idx, _state.value.positionMs)
            } else {
                if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                    if (controller.playbackState == Player.STATE_ENDED) {
                        controller.seekTo(0L)
                    }
                    controller.prepare()
                }
                controller.play()
            }
        }
    }

    fun pauseForVoice(): Boolean {
        val controller = mediaController ?: return false
        if (controller.isPlaying || controller.playWhenReady) {
            controller.pause()
            _state.value = _state.value.copy(isPlaying = false)
            return true
        }
        return false
    }

    fun resumeFromVoice() {
        val controller = mediaController
        if (controller == null) {
            connect(_state.value.shuffleEnabled, _state.value.repeatMode)
            return
        }
        if (controller.mediaItemCount == 0 && _state.value.queue.isNotEmpty()) {
            val idx = _state.value.queue.indexOfFirst { it.trackKey == _state.value.currentTrack?.trackKey }.coerceAtLeast(0)
            playQueue(_state.value.queue, idx, _state.value.positionMs)
            return
        }
        if (!controller.isPlaying) {
            if (controller.playbackState == Player.STATE_IDLE || controller.playbackState == Player.STATE_ENDED) {
                if (controller.playbackState == Player.STATE_ENDED) {
                    controller.seekTo(0L)
                }
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        mediaController?.seekTo(clamped)
        _state.value = _state.value.copy(positionMs = clamped)
    }

    fun skipNext() {
        mediaController?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
            } else if (it.mediaItemCount > 1) {
                it.seekTo(0, 0L)
            }
        }
    }

    fun skipPrevious() {
        mediaController?.let {
            if (it.currentPosition > 3000L) {
                it.seekTo(0L)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
            } else {
                it.seekTo(0L)
            }
        }
    }

    fun toggleShuffle() {
        mediaController?.let {
            val next = !it.shuffleModeEnabled
            it.shuffleModeEnabled = next
            onModesChanged(next, it.repeatMode)
        }
    }

    fun cycleRepeatMode() {
        mediaController?.let {
            val next = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = next
            onModesChanged(it.shuffleModeEnabled, next)
        }
    }

    fun stopAndRelease() {
        SleepTimerController.detachFallbackStopCallback()
        pendingPlayRequest = null
        stopTicker()
        mediaController?.removeListener(playerListener)
        mediaController = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun syncStateFromController(controller: MediaController, countAsNewStart: Boolean) {
        val count = controller.mediaItemCount
        val rebuiltQueue = if (count > 0) {
            (0 until count).mapNotNull { idx ->
                mediaItemToLocalTrack(controller.getMediaItemAt(idx), trackByKey)
            }
        } else {
            emptyList()
        }
        if (rebuiltQueue.isNotEmpty()) {
            // Preserve richer library entries where available while keeping any reconstructed items
            val rebuiltMap = rebuiltQueue.associateBy { it.trackKey }
            trackByKey = rebuiltMap + trackByKey
        }

        val current = resolveCurrentTrack(controller)
        val resolvedDuration = controller.duration.takeIf { it > 0 }
            ?: current?.durationMs?.takeIf { it > 0 }
            ?: _state.value.durationMs

        val activeQueue = if (rebuiltQueue.isNotEmpty()) rebuiltQueue else _state.value.queue
        val activeIdx = if (count > 0) {
            controller.currentMediaItemIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
        } else {
            _state.value.currentQueueIndex.coerceIn(0, (activeQueue.size - 1).coerceAtLeast(0))
        }

        _state.value = _state.value.copy(
            isControllerReady = true,
            isPlaying = controller.isPlaying,
            currentTrack = current ?: if (count == 0) _state.value.currentTrack else null,
            queue = activeQueue,
            currentQueueIndex = activeIdx,
            positionMs = controller.currentPosition.coerceAtLeast(0L),
            durationMs = resolvedDuration,
            shuffleEnabled = controller.shuffleModeEnabled,
            repeatMode = controller.repeatMode
        )

        if (current != null) {
            if (countAsNewStart && current.trackKey != lastTrackedMediaId) {
                lastTrackedMediaId = current.trackKey
                onTrackStarted(current)
            } else if (!countAsNewStart && lastTrackedMediaId == null) {
                // Reconnecting to already-playing background service: bind ID without re-counting play
                lastTrackedMediaId = current.trackKey
            }
        }
    }

    private fun resolveCurrentTrack(controller: MediaController?): LocalTrack? {
        val item = controller?.currentMediaItem ?: return null
        return mediaItemToLocalTrack(item, trackByKey)
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch(Dispatchers.Main) {
            var accumulatedMs = 0L
            while (isActive) {
                delay(500L)
                val controller = mediaController ?: continue
                if (controller.isPlaying) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0 } ?: (_state.value.currentTrack?.durationMs ?: 0L)
                    val track = resolveCurrentTrack(controller) ?: _state.value.currentTrack
                    val idx = controller.currentMediaItemIndex.coerceAtLeast(0)
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        positionMs = pos,
                        durationMs = dur,
                        currentTrack = track,
                        currentQueueIndex = idx
                    )
                    accumulatedMs += 500L
                    if (accumulatedMs >= 1000L && track != null) {
                        val deltaSec = accumulatedMs / 1000L
                        accumulatedMs %= 1000L
                        onListeningTick(track, deltaSec, pos)
                    }
                } else {
                    accumulatedMs = 0L
                }
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    companion object {
        const val EXTRA_ID = "ritmo_id"
        const val EXTRA_TRACK_KEY = "ritmo_track_key"
        const val EXTRA_ALBUM_ID = "ritmo_album_id"
        const val EXTRA_DURATION_MS = "ritmo_duration_ms"
        const val EXTRA_CONTENT_URI = "ritmo_content_uri"
        const val EXTRA_ALBUM_ART_URI = "ritmo_album_art_uri"
        const val EXTRA_DATE_ADDED = "ritmo_date_added"
        const val EXTRA_FORMAT_LABEL = "ritmo_format_label"

        fun mediaItemToLocalTrack(
            item: MediaItem,
            knownTracksByKey: Map<String, LocalTrack> = emptyMap()
        ): LocalTrack? {
            val extras = item.mediaMetadata.extras
            val trackKey = extras?.getString(EXTRA_TRACK_KEY)?.takeIf { it.isNotBlank() }
                ?: item.mediaId.takeIf { it.isNotBlank() }
                ?: ""

            if (trackKey.isNotBlank()) {
                knownTracksByKey[trackKey]?.let { return it }
            }

            val meta = item.mediaMetadata
            val title = meta.title?.toString()?.trim().orEmpty()
            val contentUri = extras?.getString(EXTRA_CONTENT_URI)?.takeIf { it.isNotBlank() }
                ?: item.localConfiguration?.uri?.toString().orEmpty()

            if (trackKey.isBlank() && title.isBlank() && contentUri.isBlank()) {
                return null
            }

            val resolvedKey = trackKey.ifBlank { contentUri.ifBlank { title } }
            val resolvedArt = extras?.getString(EXTRA_ALBUM_ART_URI)?.takeIf { it.isNotBlank() }
                ?: meta.artworkUri?.toString()
            val resolvedDuration = extras?.getLong(EXTRA_DURATION_MS, 0L)?.takeIf { it > 0L } ?: 0L

            return LocalTrack(
                id = extras?.getLong(EXTRA_ID, 0L)?.takeIf { it != 0L } ?: resolvedKey.hashCode().toLong(),
                trackKey = resolvedKey,
                title = title.ifBlank { "Pista" },
                artist = meta.artist?.toString()?.trim().orEmpty().ifBlank { "Artista desconocido" },
                album = meta.albumTitle?.toString()?.trim().orEmpty().ifBlank { "Álbum" },
                albumId = extras?.getLong(EXTRA_ALBUM_ID, 0L) ?: 0L,
                durationMs = resolvedDuration,
                contentUri = contentUri,
                albumArtUri = resolvedArt,
                dateAdded = extras?.getLong(EXTRA_DATE_ADDED, 0L) ?: 0L,
                formatLabel = extras?.getString(EXTRA_FORMAT_LABEL)?.takeIf { it.isNotBlank() } ?: "AUDIO"
            )
        }
    }
}
