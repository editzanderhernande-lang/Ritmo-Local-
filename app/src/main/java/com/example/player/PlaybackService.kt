package com.example.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.MainActivity
import com.example.R
import com.example.data.CloudSyncRepository
import com.example.data.UserPreferencesRepository
import com.example.model.AppSettingsPrefs
import com.example.model.LocalTrack
import com.example.model.PlaybackEnginePrefs
import com.example.widget.RitmoWidgetUpdater
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@UnstableApi
private class RitmoArtworkBitmapLoader(
    private val context: Context
) : BitmapLoader {
    private val executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())
    private val delegate = DataSourceBitmapLoader(executor, DefaultDataSource.Factory(context))

    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        return delegate.decodeBitmap(data)
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        return executor.submit<Bitmap> {
            decodeLocalOrRemoteUri(uri) ?: delegate.loadBitmap(uri).get()
        }
    }

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        val uri = metadata.artworkUri
        if (uri != null) {
            return loadBitmap(uri)
        }
        val bytes = metadata.artworkData
        if (bytes != null && bytes.isNotEmpty()) {
            return decodeBitmap(bytes)
        }
        val fallbackContentUri = metadata.extras
            ?.getString(PlaybackControllerManager.EXTRA_CONTENT_URI)
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
        if (fallbackContentUri != null) {
            return executor.submit<Bitmap> {
                extractEmbeddedPicture(fallbackContentUri)
                    ?: decodeFallbackPlaceholderBitmap()
            }
        }
        return executor.submit<Bitmap> { decodeFallbackPlaceholderBitmap() }
    }

    private fun decodeLocalOrRemoteUri(uri: Uri): Bitmap? {
        return try {
            when (uri.scheme) {
                "file" -> uri.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath, boundedOptions())
                    } else null
                }
                "content" -> context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundedOptions())
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractEmbeddedPicture(audioUri: Uri): Bitmap? {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, audioUri)
            val pic = mmr.embeddedPicture ?: return null
            BitmapFactory.decodeByteArray(pic, 0, pic.size, boundedOptions())
        } catch (_: Exception) {
            null
        } finally {
            runCatching { mmr.release() }
        }
    }

    private fun decodeFallbackPlaceholderBitmap(): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.img_album_placeholder)
    }

    private fun boundedOptions(): BitmapFactory.Options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
}

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var crossfadeTailPlayer: ExoPlayer? = null
    private val realtimeAudioAnalyzer = RealtimeAudioAnalysisProcessor()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeOutJob: Job? = null
    private var fadeInJob: Job? = null
    private var crossfadeJob: Job? = null
    private var crossfadeMonitorJob: Job? = null
    private lateinit var prefsRepo: UserPreferencesRepository

    private var currentSettings = AppSettingsPrefs()
    private var currentEnginePrefs = PlaybackEnginePrefs()
    private var currentFavoriteKeys: Set<String> = emptySet()
    private var isSleepTimerFadingOut: Boolean = false
    private var isCrossfadingNow: Boolean = false
    private var lastAnalyzedTrackKey: String? = null
    private var currentTrackAnalysis: AudioTrackAnalysis? = null
    private var nextTrackAnalysis: AudioTrackAnalysis? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        prefsRepo = UserPreferencesRepository(this)

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(MEDIA_NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.media_notification_channel_name)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_notif_ritmo)
        setMediaNotificationProvider(notificationProvider)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setConstantBitrateSeekingAlwaysEnabled(true)

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(true)
                    .setEnableAudioTrackPlaybackParams(true)
                    .setAudioProcessors(arrayOf(realtimeAudioAnalyzer, AudioEffectsController.channelProcessor))
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .setAudioAttributes(audioAttributes, !VoiceAudioFocusCoordinator.isVoiceMicActive.value)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Secondary internal tail-deck inside the same single PlaybackService for seamless Crossfade / DJ transitions
        crossfadeTailPlayer = ExoPlayer.Builder(this, DefaultRenderersFactory(this))
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .setAudioAttributes(audioAttributes, false)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioEffectsController.attachToAudioSession(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    SleepTimerController.consumeStopAfterCurrentTrack()
                ) {
                    stopPlayerWithOptionalFade(player)
                }
                refreshSmartDjAnalysisFor(player)
                refreshCustomNotificationLayout(player)
                pushWidgetSnapshot(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (currentEnginePrefs.fadeInEnabled && !isCrossfadingNow && !isSleepTimerFadingOut) {
                        runFadeInEnvelope(player)
                    }
                    ensureCrossfadeMonitorRunning(player)
                } else {
                    fadeInJob?.cancel()
                    if (!isCrossfadingNow) {
                        stopCrossfadeTailImmediately()
                    }
                    AudioAnalysisBus.resetToIdle()
                }
                pushWidgetSnapshot(player)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                serviceScope.launch {
                    prefsRepo.savePlaybackModes(shuffleModeEnabled, player.repeatMode)
                }
                refreshSmartDjAnalysisFor(player)
                refreshCustomNotificationLayout(player)
                pushWidgetSnapshot(player)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                serviceScope.launch {
                    prefsRepo.savePlaybackModes(player.shuffleModeEnabled, repeatMode)
                }
                refreshSmartDjAnalysisFor(player)
                refreshCustomNotificationLayout(player)
                pushWidgetSnapshot(player)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    SleepTimerController.consumeStopAfterCurrentTrack()
                    AudioAnalysisBus.resetToIdle()
                }
                pushWidgetSnapshot(player)
            }
        })
        AudioEffectsController.attachToAudioSession(player.audioSessionId)

        SleepTimerController.attachServiceStopCallback {
            stopPlayerWithOptionalFade(player)
        }

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val base = super.onConnect(session, controller)
                val sessionCommands = base.availableSessionCommands.buildUpon()
                    .add(SessionCommand(CMD_TOGGLE_FAVORITE, Bundle.EMPTY))
                    .add(SessionCommand(CMD_CYCLE_PLAY_MODE, Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(sessionCommands)
                    .setAvailablePlayerCommands(base.availablePlayerCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                when (customCommand.customAction) {
                    CMD_TOGGLE_FAVORITE -> toggleCurrentTrackFavorite(player)
                    CMD_CYCLE_PLAY_MODE -> cyclePlaybackMode(player)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            9001,
            sessionActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setBitmapLoader(RitmoArtworkBitmapLoader(applicationContext))
            .setCallback(sessionCallback)
            .build()

        // Restore persisted playback modes, DSP settings, and observe live preferences
        serviceScope.launch {
            val initialEq = prefsRepo.equalizerPrefs.first()
            AudioEffectsController.loadStoredPrefs(initialEq)

            val initialSettings = prefsRepo.appSettingsPrefs.first()
            currentSettings = initialSettings
            player.shuffleModeEnabled = initialSettings.savedShuffleEnabled
            player.repeatMode = initialSettings.savedRepeatMode
            val initialHandleFocus = !initialSettings.playAlongOtherApps &&
                !initialSettings.handsFreeVoiceEnabled &&
                !VoiceAudioFocusCoordinator.isVoiceMicActive.value
            player.setAudioAttributes(audioAttributes, initialHandleFocus)
            refreshCustomNotificationLayout(player)
        }

        serviceScope.launch {
            combine(
                prefsRepo.appSettingsPrefs,
                VoiceAudioFocusCoordinator.isVoiceMicActive
            ) { settings, micActive -> settings to micActive }
                .collect { (settings, micActive) ->
                    currentSettings = settings
                    val handleFocus = !settings.playAlongOtherApps &&
                        !settings.handsFreeVoiceEnabled &&
                        !micActive
                    player.setAudioAttributes(audioAttributes, handleFocus)
                    refreshCustomNotificationLayout(player)
                }
        }

        // Transiently soften volume while user is actively speaking a command ("Ridmo" -> command)
        // without EVER pausing music playback.
        serviceScope.launch {
            VoiceAudioFocusCoordinator.isVoiceDucking.collect { ducking ->
                if (!isSleepTimerFadingOut && !isCrossfadingNow) {
                    player.volume = if (ducking) VOICE_DUCK_VOLUME else 1.0f
                }
            }
        }

        serviceScope.launch {
            prefsRepo.playbackEnginePrefs.collect { enginePrefs ->
                currentEnginePrefs = enginePrefs
                PlaybackEngineBus.updateConfig(enginePrefs)
                if (enginePrefs.smartDjEnabled) {
                    refreshSmartDjAnalysisFor(player)
                }
                if (player.isPlaying) {
                    ensureCrossfadeMonitorRunning(player)
                }
            }
        }

        serviceScope.launch {
            PlaybackEngineBus.manualMixRequests.collect {
                triggerDjTransitionToNext(player, isManual = true)
            }
        }

        serviceScope.launch {
            prefsRepo.offlineFavoriteKeys.collect { offlineKeys ->
                if (Firebase.auth.currentUser == null) {
                    currentFavoriteKeys = offlineKeys
                    refreshCustomNotificationLayout(player)
                    pushWidgetSnapshot(player)
                }
            }
        }

        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            serviceScope.launch {
                try {
                    CloudSyncRepository(this@PlaybackService).observeFavorites(uid).collect { favs ->
                        currentFavoriteKeys = favs.map { it.trackKey }.toSet()
                        refreshCustomNotificationLayout(player)
                        pushWidgetSnapshot(player)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = mediaSession?.player
        when (intent?.action) {
            ACTION_WIDGET_PLAY_PAUSE -> if (player != null && player.mediaItemCount > 0) {
                if (player.isPlaying) player.pause() else player.play()
            }
            ACTION_WIDGET_NEXT -> if (player != null && player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            }
            ACTION_WIDGET_PREV -> if (player != null && player.mediaItemCount > 0) {
                if (player.currentPosition > 3000L) player.seekTo(0L) else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
            }
            ACTION_WIDGET_FAV -> if (player != null) {
                toggleCurrentTrackFavorite(player)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    @OptIn(UnstableApi::class)
    private fun refreshCustomNotificationLayout(player: Player) {
        val session = mediaSession ?: return
        val buttons = mutableListOf<CommandButton>()
        val mediaId = player.currentMediaItem?.mediaId
        val isFav = mediaId != null && currentFavoriteKeys.contains(mediaId)

        if (currentSettings.notifShowFavorite) {
            val favIcon = if (isFav) R.drawable.ic_notif_favorite_filled else R.drawable.ic_notif_favorite_border
            buttons.add(
                CommandButton.Builder()
                    .setDisplayName(if (isFav) "Quitar de favoritas" else "Agregar a favoritas")
                    .setIconResId(favIcon)
                    .setSessionCommand(SessionCommand(CMD_TOGGLE_FAVORITE, Bundle.EMPTY))
                    .build()
            )
        }

        if (currentSettings.notifShowModeToggle) {
            val (modeLabel, modeIcon) = when {
                player.shuffleModeEnabled -> "Aleatorio" to R.drawable.ic_notif_shuffle
                player.repeatMode == Player.REPEAT_MODE_ONE -> "Repetir 1" to R.drawable.ic_notif_repeat_one
                player.repeatMode == Player.REPEAT_MODE_ALL -> "Repetir todo" to R.drawable.ic_notif_repeat
                else -> "Continuo" to R.drawable.ic_notif_continuous
            }
            buttons.add(
                CommandButton.Builder()
                    .setDisplayName(modeLabel)
                    .setIconResId(modeIcon)
                    .setSessionCommand(SessionCommand(CMD_CYCLE_PLAY_MODE, Bundle.EMPTY))
                    .build()
            )
        }

        session.setCustomLayout(ImmutableList.copyOf(buttons))
    }

    private fun cyclePlaybackMode(player: Player) {
        when {
            !player.shuffleModeEnabled && player.repeatMode == Player.REPEAT_MODE_OFF -> {
                player.shuffleModeEnabled = true
                player.repeatMode = Player.REPEAT_MODE_ALL
            }
            player.shuffleModeEnabled -> {
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_ALL
            }
            player.repeatMode == Player.REPEAT_MODE_ALL -> {
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_ONE
            }
            else -> {
                player.shuffleModeEnabled = false
                player.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    private fun toggleCurrentTrackFavorite(player: Player) {
        val item = player.currentMediaItem ?: return
        val trackKey = item.mediaId.ifBlank { return }
        val meta = item.mediaMetadata
        val isFav = currentFavoriteKeys.contains(trackKey)
        serviceScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null) {
                val track = LocalTrack(
                    id = trackKey.hashCode().toLong(),
                    trackKey = trackKey,
                    title = meta.title?.toString().orEmpty().ifBlank { "Pista" },
                    artist = meta.artist?.toString().orEmpty().ifBlank { "Artista" },
                    album = meta.albumTitle?.toString().orEmpty().ifBlank { "Álbum" },
                    albumId = 0L,
                    durationMs = player.duration.coerceAtLeast(0L),
                    contentUri = item.localConfiguration?.uri?.toString().orEmpty(),
                    albumArtUri = meta.artworkUri?.toString(),
                    dateAdded = 0L
                )
                CloudSyncRepository(this@PlaybackService).toggleFavorite(user.uid, track, isFav)
            } else {
                prefsRepo.toggleOfflineFavorite(trackKey)
            }
        }
    }

    private fun pushWidgetSnapshot(player: Player) {
        val item = player.currentMediaItem
        val meta = item?.mediaMetadata
        val mediaId = item?.mediaId
        val isFav = mediaId != null && currentFavoriteKeys.contains(mediaId)
        val artUri = item?.let {
            PlaybackControllerManager.mediaItemToLocalTrack(it)?.albumArtUri
        } ?: meta?.artworkUri?.toString()
        RitmoWidgetUpdater.updateAllWidgets(
            context = this,
            title = meta?.title?.toString().orEmpty(),
            artist = meta?.artist?.toString().orEmpty(),
            isPlaying = player.isPlaying,
            isFavorite = isFav,
            artUriString = if (currentSettings.notifShowArtwork) artUri else null
        )
    }

    private fun stopPlayerWithOptionalFade(player: Player) {
        fadeOutJob?.cancel()
        fadeInJob?.cancel()
        stopCrossfadeTailImmediately()
        val sleepState = SleepTimerController.state.value
        val fadeEnabled = sleepState.fadeOutEnabled
        val fadeDurationMs = (sleepState.fadeOutDurationSec.coerceIn(1, 15) * 1000L)
        if (!fadeEnabled) {
            isSleepTimerFadingOut = false
            if (player.isPlaying || player.playWhenReady) {
                player.pause()
            }
            player.volume = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1f
            return
        }
        fadeOutJob = serviceScope.launch {
            isSleepTimerFadingOut = true
            try {
                if (player.isPlaying || player.playWhenReady) {
                    val steps = 16
                    val stepDelayMs = (fadeDurationMs / steps).coerceAtLeast(60L)
                    val startVol = player.volume.coerceIn(0.05f, 1f)
                    for (i in 1..steps) {
                        player.volume = (startVol * (1f - i.toFloat() / steps.toFloat())).coerceIn(0f, 1f)
                        delay(stepDelayMs)
                    }
                    player.pause()
                }
            } finally {
                isSleepTimerFadingOut = false
                player.volume = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1f
            }
        }
    }

    private fun runFadeInEnvelope(player: Player) {
        fadeInJob?.cancel()
        val targetVol = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1.0f
        val durationMs = currentEnginePrefs.fadeDurationMs.coerceIn(200, 2000).toLong()
        fadeInJob = serviceScope.launch {
            val steps = 10
            val stepDelay = (durationMs / steps).coerceAtLeast(25L)
            for (i in 1..steps) {
                if (!player.isPlaying || isSleepTimerFadingOut || isCrossfadingNow) break
                val t = i.toFloat() / steps.toFloat()
                player.volume = (targetVol * (0.25f + 0.75f * t)).coerceIn(0f, 1f)
                delay(stepDelay)
            }
            if (!isSleepTimerFadingOut && !isCrossfadingNow) {
                player.volume = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1.0f
            }
        }
    }

    private fun refreshSmartDjAnalysisFor(player: Player) {
        val item = player.currentMediaItem ?: return
        val track = PlaybackControllerManager.mediaItemToLocalTrack(item) ?: return
        if (lastAnalyzedTrackKey == track.trackKey && currentTrackAnalysis != null) {
            analyzeNextInBackground(player)
            return
        }
        lastAnalyzedTrackKey = track.trackKey
        serviceScope.launch {
            val crossMs = (currentEnginePrefs.crossfadeDurationSec.coerceIn(1, 12) * 1000L)
            val analysis = SmartDjAnalyzer.analyzeTrackReadOnly(
                context = applicationContext,
                track = track,
                djStyle = currentEnginePrefs.djTransitionStyle,
                configuredCrossfadeMs = crossMs
            )
            currentTrackAnalysis = analysis
            PlaybackEngineBus.updateDjRuntime { it.copy(currentAnalysis = analysis) }
            analyzeNextInBackground(player)
        }
    }

    private fun analyzeNextInBackground(player: Player) {
        val nextIdx = resolveNextQueueIndex(player)
        if (nextIdx == C.INDEX_UNSET || nextIdx !in 0 until player.mediaItemCount) {
            nextTrackAnalysis = null
            PlaybackEngineBus.updateDjRuntime { it.copy(nextAnalysis = null) }
            return
        }
        val nextItem = player.getMediaItemAt(nextIdx)
        val nextTrack = PlaybackControllerManager.mediaItemToLocalTrack(nextItem) ?: return
        serviceScope.launch {
            val crossMs = (currentEnginePrefs.crossfadeDurationSec.coerceIn(1, 12) * 1000L)
            val analysis = SmartDjAnalyzer.analyzeTrackReadOnly(
                context = applicationContext,
                track = nextTrack,
                djStyle = currentEnginePrefs.djTransitionStyle,
                configuredCrossfadeMs = crossMs
            )
            nextTrackAnalysis = analysis
            PlaybackEngineBus.updateDjRuntime { it.copy(nextAnalysis = analysis) }
        }
    }

    private fun ensureCrossfadeMonitorRunning(player: Player) {
        if (crossfadeMonitorJob?.isActive == true) return
        crossfadeMonitorJob = serviceScope.launch {
            while (isActive && player.isPlaying) {
                val cfg = currentEnginePrefs
                if ((cfg.crossfadeEnabled || cfg.smartDjEnabled) && !isCrossfadingNow && !isSleepTimerFadingOut) {
                    val sleepState = SleepTimerController.state.value
                    val stoppingAfterCurrent = sleepState.isActive &&
                        (sleepState.stopAfterCurrentTrack || sleepState.remainingSongsToStop == 1)
                    if (!stoppingAfterCurrent) {
                        val duration = player.duration
                        val pos = player.currentPosition
                        val nextIdx = resolveNextQueueIndex(player)
                        if (duration > 12_000L && nextIdx != C.INDEX_UNSET && pos > 4_000L) {
                            val crossfadeMs = if (cfg.smartDjEnabled) {
                                currentTrackAnalysis?.recommendedCrossfadeMs
                                    ?: (cfg.crossfadeDurationSec.coerceIn(1, 12) * 1000L)
                            } else {
                                (cfg.crossfadeDurationSec.coerceIn(1, 12) * 1000L)
                            }
                            val triggerPosMs = if (cfg.smartDjEnabled && currentTrackAnalysis != null) {
                                currentTrackAnalysis!!.safeOutroTriggerPositionMs(
                                    configuredCrossfadeMs = crossfadeMs,
                                    skipSilence = cfg.djSkipSilenceEnabled
                                )
                            } else {
                                (duration - crossfadeMs).coerceAtLeast((duration * 0.65f).toLong())
                            }
                            if (pos >= triggerPosMs && (duration - pos) > 350L) {
                                triggerDjTransitionToNext(player, isManual = false)
                            }
                        }
                    }
                }
                delay(280L)
            }
        }
    }

    private fun resolveNextQueueIndex(player: Player): Int {
        if (player.mediaItemCount <= 1) return C.INDEX_UNSET
        val directNext = player.nextMediaItemIndex
        if (directNext != C.INDEX_UNSET && directNext != player.currentMediaItemIndex) {
            return directNext
        }
        val cur = player.currentMediaItemIndex
        return if (cur + 1 < player.mediaItemCount) cur + 1 else if (player.repeatMode == Player.REPEAT_MODE_ALL) 0 else C.INDEX_UNSET
    }

    private fun triggerDjTransitionToNext(player: Player, isManual: Boolean) {
        if (isCrossfadingNow || isSleepTimerFadingOut) return
        val nextIdx = resolveNextQueueIndex(player)
        if (nextIdx == C.INDEX_UNSET || nextIdx !in 0 until player.mediaItemCount) return
        val outgoingItem = player.currentMediaItem ?: return
        val outgoingPosMs = player.currentPosition.coerceAtLeast(0L)
        val tail = crossfadeTailPlayer ?: return

        val cfg = currentEnginePrefs
        val crossfadeMs = when {
            isManual -> (cfg.crossfadeDurationSec.coerceIn(2, 8) * 1000L)
            cfg.smartDjEnabled -> currentTrackAnalysis?.recommendedCrossfadeMs ?: (cfg.crossfadeDurationSec.coerceIn(2, 12) * 1000L)
            else -> (cfg.crossfadeDurationSec.coerceIn(1, 12) * 1000L)
        }
        val incomingStartPosMs = if (cfg.smartDjEnabled && cfg.djSkipSilenceEnabled) {
            nextTrackAnalysis?.introSilenceEndMs ?: 0L
        } else 0L

        val incomingGainCap = if (cfg.smartDjEnabled && cfg.djVolumeNormalization &&
            currentTrackAnalysis != null && nextTrackAnalysis != null
        ) {
            val ratio = (currentTrackAnalysis!!.energyScore / nextTrackAnalysis!!.energyScore.coerceAtLeast(0.25f))
            ratio.coerceIn(0.84f, 1.0f)
        } else 1.0f

        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            isCrossfadingNow = true
            val nextTitle = player.getMediaItemAt(nextIdx).mediaMetadata.title?.toString().orEmpty().ifBlank { "Siguiente pista" }
            PlaybackEngineBus.updateDjRuntime {
                it.copy(
                    isCrossfading = true,
                    crossfadeProgress = 0f,
                    lastTransitionNote = if (isManual) "Mezcla DJ → $nextTitle" else "Transición suave → $nextTitle"
                )
            }
            try {
                val duckMultiplier = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1.0f
                val outgoingInitialVol = player.volume.coerceIn(0.1f, 1f)

                tail.stop()
                tail.clearMediaItems()
                tail.setMediaItem(outgoingItem, outgoingPosMs)
                tail.volume = outgoingInitialVol * duckMultiplier
                tail.prepare()
                tail.play()

                player.volume = 0f
                player.seekTo(nextIdx, incomingStartPosMs)
                if (!player.isPlaying) {
                    player.prepare()
                    player.play()
                }

                val steps = (crossfadeMs / 45L).toInt().coerceIn(12, 180)
                val stepDelay = (crossfadeMs / steps).coerceAtLeast(30L)
                for (step in 1..steps) {
                    if (!player.isPlaying && step > 3) break
                    val progress = step.toFloat() / steps.toFloat()
                    val (outGain, inGain) = computeCrossfadeGains(progress, cfg.crossfadeCurve)
                    val voiceMul = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1.0f
                    tail.volume = (outGain * outgoingInitialVol * voiceMul).coerceIn(0f, 1f)
                    player.volume = (inGain * incomingGainCap * voiceMul).coerceIn(0f, 1f)
                    PlaybackEngineBus.updateDjRuntime { it.copy(crossfadeProgress = progress) }
                    delay(stepDelay)
                }
            } finally {
                stopCrossfadeTailImmediately()
                if (!isSleepTimerFadingOut) {
                    val voiceMul = if (VoiceAudioFocusCoordinator.isVoiceDucking.value) VOICE_DUCK_VOLUME else 1.0f
                    player.volume = voiceMul
                }
                isCrossfadingNow = false
                PlaybackEngineBus.updateDjRuntime {
                    it.copy(isCrossfading = false, crossfadeProgress = 0f)
                }
            }
        }
    }

    private fun stopCrossfadeTailImmediately() {
        crossfadeJob?.cancel()
        crossfadeJob = null
        crossfadeTailPlayer?.let { tail ->
            runCatching {
                tail.stop()
                tail.clearMediaItems()
            }
        }
        isCrossfadingNow = false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        // Only stop service if nothing is actively playing; if music is playing, keep PlaybackService running!
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        SleepTimerController.detachServiceStopCallback()
        fadeOutJob?.cancel()
        fadeInJob?.cancel()
        crossfadeMonitorJob?.cancel()
        stopCrossfadeTailImmediately()
        crossfadeTailPlayer?.release()
        crossfadeTailPlayer = null
        AudioAnalysisBus.resetToIdle()
        serviceScope.cancel()
        AudioEffectsController.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    companion object {
        const val MEDIA_NOTIFICATION_CHANNEL_ID = "ritmolocal_media_playback"
        private const val VOICE_DUCK_VOLUME = 0.28f

        const val CMD_TOGGLE_FAVORITE = "com.example.ritmolocal.TOGGLE_FAVORITE"
        const val CMD_CYCLE_PLAY_MODE = "com.example.ritmolocal.CYCLE_PLAY_MODE"
        const val ACTION_WIDGET_PLAY_PAUSE = "com.example.ritmolocal.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.example.ritmolocal.WIDGET_NEXT"
        const val ACTION_WIDGET_PREV = "com.example.ritmolocal.WIDGET_PREV"
        const val ACTION_WIDGET_FAV = "com.example.ritmolocal.WIDGET_FAV"
    }
}
