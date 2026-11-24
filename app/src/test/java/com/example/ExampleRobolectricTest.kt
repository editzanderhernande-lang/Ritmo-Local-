package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.example.data.LocalMusicRepository
import com.example.data.LyricLine
import com.example.data.TrackLyrics
import com.example.data.UserPreferencesRepository
import com.example.diagnostics.AppDiagnosticsManager
import com.example.diagnostics.DiagnosticAuditInput
import com.example.diagnostics.DiagnosticModule
import com.example.diagnostics.DiagnosticRepairCallbacks
import com.example.diagnostics.DiagnosticSeverity
import com.example.diagnostics.RepairActionId
import com.example.model.AppSettingsPrefs
import com.example.model.CustomPlaylist
import com.example.model.LocalTrack
import com.example.model.PlayerBackgroundMode
import com.example.model.PlayerVisualPrefs
import com.example.model.PlayerVisualStyle
import com.example.model.SavedCustomEqProfile
import com.example.model.TimeOfDaySlot
import com.example.notifications.ChatNotificationHelper
import com.example.player.EqualizerUiState
import com.example.player.PlaybackControllerManager
import com.example.player.PlaybackService
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerUiState
import com.example.ui.components.InPlayerEqualizerSheet
import com.example.ui.components.PlaybackQueueSheet
import com.example.ui.components.PlayerCustomizationSheet
import com.example.ui.components.SleepTimerSheet
import com.example.ui.screens.AdvancedNowPlayingScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.theme.RitmoLocalTheme
import com.example.voice.VoiceAudioCaptureEngine
import com.example.voice.VoiceUiState
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("RitmoLocal", appName)
  }

  @Test
  fun `custom playlist json serialization round trips cleanly`() {
    val input = listOf(
      CustomPlaylist(
        id = "pl_1",
        name = "Gym Nocturno",
        trackKeys = listOf("trk1|art1|200", "trk2|art2|180"),
        createdAtMs = 123456789L
      )
    )
    val encoded = UserPreferencesRepository.encodeCustomPlaylists(input)
    val decoded = UserPreferencesRepository.decodeCustomPlaylists(encoded)
    assertEquals(1, decoded.size)
    assertEquals("pl_1", decoded[0].id)
    assertEquals("Gym Nocturno", decoded[0].name)
    assertEquals(listOf("trk1|art1|200", "trk2|art2|180"), decoded[0].trackKeys)
  }

  @Test
  fun `artist playlists group nested albums accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = LocalMusicRepository(context)
    val tracks = listOf(
      sampleTrack(1L, "Luz", "Soda Stereo", "Signos"),
      sampleTrack(2L, "Prófugos", "Soda Stereo", "Signos"),
      sampleTrack(3L, "De Música Ligera", "Soda Stereo", "Canción Animal"),
      sampleTrack(4L, "Entre Caníbales", "Soda Stereo", "Canción Animal"),
      sampleTrack(5L, "Flaca", "Andrés Calamaro", "Alta Suciedad")
    )

    val artists = repo.buildArtistPlaylists(tracks)
    assertEquals(2, artists.size)

    val soda = artists.first { it.artistName == "Soda Stereo" }
    assertEquals(2, soda.albumCount)
    assertEquals(4, soda.tracks.size)
    assertEquals(listOf("Canción Animal", "Signos"), soda.albums.map { it.albumName })
    assertEquals(2, soda.albums.first { it.albumName == "Signos" }.tracks.size)
  }

  @Test
  fun `app launcher adaptive icons and density png mipmaps are properly configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
    assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
    assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_notif_ritmo))

    val densities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")
    val resRoot = File("src/main/res").takeIf { it.exists() } ?: File("app/src/main/res")
    assertTrue("Expected res root to exist: ${resRoot.absolutePath}", resRoot.exists())
    densities.forEach { density ->
      listOf("ic_launcher.png", "ic_launcher_round.png").forEach { fileName ->
        val file = File(resRoot, "mipmap-$density/$fileName")
        assertTrue("Missing mipmap asset: ${file.path}", file.exists() && file.length() > 100L)
        val header = file.inputStream().use { stream -> ByteArray(4).also { stream.read(it) } }
        assertEquals(0x89.toByte(), header[0])
        assertEquals('P'.code.toByte(), header[1])
        assertEquals('N'.code.toByte(), header[2])
        assertEquals('G'.code.toByte(), header[3])
      }
    }
  }

  @Test
  fun `mini player reconstructs active track artwork title and artist from live MediaItem after cold reopen`() {
    val artUri = "file:///storage/emulated/0/covers/we_are.jpg"
    val contentUri = "content://media/external/audio/media/901"
    val extras = Bundle().apply {
      putLong(PlaybackControllerManager.EXTRA_ID, 901L)
      putString(PlaybackControllerManager.EXTRA_TRACK_KEY, "we are|one piece|244")
      putLong(PlaybackControllerManager.EXTRA_ALBUM_ID, 77L)
      putLong(PlaybackControllerManager.EXTRA_DURATION_MS, 244_000L)
      putString(PlaybackControllerManager.EXTRA_CONTENT_URI, contentUri)
      putString(PlaybackControllerManager.EXTRA_ALBUM_ART_URI, artUri)
      putLong(PlaybackControllerManager.EXTRA_DATE_ADDED, 1700000L)
      putString(PlaybackControllerManager.EXTRA_FORMAT_LABEL, "FLAC 24-bit")
    }
    val item = MediaItem.Builder()
      .setMediaId("we are|one piece|244")
      .setUri(Uri.parse(contentUri))
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle("We Are!")
          .setArtist("Hiroshi Kitadani")
          .setAlbumTitle("One Piece OST")
          .setArtworkUri(Uri.parse(artUri))
          .setExtras(extras)
          .build()
      )
      .build()

    // Simulate cold Activity/ViewModel reopen where in-memory library map is initially empty
    val recovered = PlaybackControllerManager.mediaItemToLocalTrack(item, emptyMap())
    assertNotNull(recovered)
    recovered!!
    assertEquals(901L, recovered.id)
    assertEquals("we are|one piece|244", recovered.trackKey)
    assertEquals("We Are!", recovered.title)
    assertEquals("Hiroshi Kitadani", recovered.artist)
    assertEquals("One Piece OST", recovered.album)
    assertEquals(244_000L, recovered.durationMs)
    assertEquals(contentUri, recovered.contentUri)
    assertEquals(artUri, recovered.albumArtUri)
    assertEquals("FLAC 24-bit", recovered.formatLabel)
  }

  @Test
  fun `notifications use monochrome ritmo icon accent color and manifest metadata`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    assertNotNull(ContextCompat.getDrawable(app, R.drawable.ic_notif_ritmo))
    assertNotNull(ContextCompat.getDrawable(app, R.drawable.ic_notif_favorite_filled))
    assertNotNull(ContextCompat.getDrawable(app, R.drawable.ic_notif_favorite_border))
    val expectedAccent = ContextCompat.getColor(app, R.color.ritmo_notif_accent)
    assertTrue(app.getString(R.string.media_notification_channel_name).isNotBlank())
    assertTrue(PlaybackService.MEDIA_NOTIFICATION_CHANNEL_ID.isNotBlank())

    val appInfo = app.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA)
    val meta = appInfo.metaData
    assertNotNull(meta)
    assertEquals(R.drawable.ic_notif_ritmo, meta.getInt("com.google.firebase.messaging.default_notification_icon"))
    assertEquals(R.color.ritmo_notif_accent, meta.getInt("com.google.firebase.messaging.default_notification_color"))

    val posted = ChatNotificationHelper.showChatMessageNotification(
      context = app,
      senderName = "Zander",
      messageText = "Escuchando mix Hi-Fi",
      regionId = "LATAM",
      messageId = "msg_test_icon_1",
      recommendedTrackTitle = "We Are!",
      hasAudioAttachment = false
    )
    assertTrue(posted)

    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val active = nm.activeNotifications
    assertTrue(active.isNotEmpty())
    val postedNotif = active.first().notification
    assertEquals(R.drawable.ic_notif_ritmo, postedNotif.smallIcon.resId)
    assertEquals(expectedAccent, postedNotif.color)
  }

  @Test
  fun `voice capture engine queries capabilities cleanly without changing audio mode`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val initialMode = audioManager.mode

    val engine = VoiceAudioCaptureEngine(context)
    val caps = engine.queryDeviceCapabilities()
    assertEquals("VOICE_COMMUNICATION", caps.audioSourceLabel)
    assertEquals(initialMode, audioManager.mode)
  }

  @Test
  fun `voice recognition pauses on phone speaker and resumes on timeout while bluetooth never pauses`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var pauseCalls = 0
    var resumeCalls = 0

    try {
      // 1) Phone speaker route: startListening pauses playback; stopListening & unrecognized attempt resume playback
      com.example.voice.VoiceAudioRouteDetector.routeOverrideForTesting = com.example.voice.VoiceAudioRoute.PHONE_SPEAKER
      val speakerRecognizer = com.example.voice.OfflineVoiceRecognizer(
        context = context,
        onRequestPauseForVoice = {
          pauseCalls++
          true
        },
        onRequestResumeAfterVoice = { resumeCalls++ },
        onHypothesesReady = { _, _ -> }
      )

      speakerRecognizer.startListening("es-419")
      assertEquals(1, pauseCalls)
      assertTrue(speakerRecognizer.state.value.pausedPlaybackForListening)

      // "Pausar" command keeps music paused (keepPaused = true)
      speakerRecognizer.markCommandExecuted("Voz: reproducción en pausa", keepPaused = true)
      assertEquals(0, resumeCalls)

      // Next speaker session where no command is recognized resumes automatically
      speakerRecognizer.startListening("es-419")
      assertEquals(2, pauseCalls)
      speakerRecognizer.completeUnrecognizedVoiceAttempt("No entendí el comando")
      assertEquals(1, resumeCalls)

      // 2) Bluetooth route: NEVER pauses music on startListening
      pauseCalls = 0
      resumeCalls = 0
      com.example.voice.VoiceAudioRouteDetector.routeOverrideForTesting = com.example.voice.VoiceAudioRoute.BLUETOOTH_HEADSET
      val btRecognizer = com.example.voice.OfflineVoiceRecognizer(
        context = context,
        onRequestPauseForVoice = {
          pauseCalls++
          true
        },
        onRequestResumeAfterVoice = { resumeCalls++ },
        onHypothesesReady = { _, _ -> }
      )
      btRecognizer.startListening("es-419")
      assertEquals(0, pauseCalls)
      assertEquals(com.example.voice.VoiceAudioRoute.BLUETOOTH_HEADSET, btRecognizer.state.value.audioRoute)
    } finally {
      com.example.voice.VoiceAudioRouteDetector.routeOverrideForTesting = null
    }
  }

  @Test
  fun `diagnostic center detects repeated errors escalates severity and scrubs sensitive data`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    AppDiagnosticsManager.resetForTesting(app)
    AppDiagnosticsManager.initialize(app, diagnosticModeEnabled = true)

    val sensitiveRaw = "Bearer abc123SECRETtoken email=user@example.com token=my_secret_99"
    repeat(3) {
      AppDiagnosticsManager.recordError(
        module = DiagnosticModule.CLOUD_SYNC,
        title = "Fallo de conexión temporal",
        technicalMessage = sensitiveRaw,
        severity = DiagnosticSeverity.MEDIUM,
        plainExplanation = "No se pudo sincronizar.",
        proposedSolution = "Reintenta más tarde.",
        repairAction = RepairActionId.RETRY_CLOUD_SYNC
      )
    }

    val state = AppDiagnosticsManager.uiState.value
    val recorded = state.activeIssues.first { it.module == DiagnosticModule.CLOUD_SYNC }
    assertEquals(3, recorded.occurrenceCount)
    assertEquals(DiagnosticSeverity.HIGH, recorded.severity)
    assertFalse("Bearer token must be redacted", recorded.technicalMessage.contains("abc123SECRETtoken"))
    assertFalse("Email must be redacted", recorded.technicalMessage.contains("user@example.com"))
    assertFalse("Token param must be redacted", recorded.technicalMessage.contains("my_secret_99"))
    assertTrue(recorded.technicalMessage.contains("[REDACTED]"))

    // Verify playback protection isolates non-player errors
    val safeFallback = AppDiagnosticsManager.runGuarded(
      module = DiagnosticModule.CLOUD_SYNC,
      operationName = "syncTest",
      fallback = "respaldo"
    ) {
      error("Simulated exception without touching player")
    }
    assertEquals("respaldo", safeFallback)
    assertEquals(1, AppDiagnosticsManager.uiState.value.playbackProtection.guardedErrorsCount)
  }

  @Test
  fun `diagnostic full analysis audits 15 modules detects conflicts and safely cleans temp cache only`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      AppDiagnosticsManager.resetForTesting(app)
      AppDiagnosticsManager.initialize(app, diagnosticModeEnabled = false)

      // Create dummy temp cache file AND protected local music file
      val tempCache = File(app.cacheDir, "stale_probe.tmp").apply { writeBytes(ByteArray(2048)) }
      val protectedDir = File(app.filesDir, "RitmoLocalUserMusic").apply { mkdirs() }
      val protectedSong = File(protectedDir, "mi_cancion.mp3").apply { writeBytes(ByteArray(512)) }

      val sample = sampleTrack(1L, "Luz", "Viento", "Horizonte")
      val input = DiagnosticAuditInput(
        playbackState = PlaybackUiState(currentTrack = sample, queue = listOf(sample), isPlaying = true, isControllerReady = true),
        equalizerState = EqualizerUiState(enabled = true, isHardwareAttached = false),
        sleepTimerState = SleepTimerUiState(),
        voiceState = VoiceUiState(),
        settings = AppSettingsPrefs(handsFreeVoiceEnabled = true),
        localTracks = listOf(sample),
        customPlaylists = listOf(CustomPlaylist(id = "pl1", name = "Mis Favoritas", trackKeys = listOf(sample.trackKey, "missing|key"))),
        isSignedIn = false,
        cloudSyncError = null,
        lastCloudSyncMs = null,
        totalPlaybackSeconds = 3600L
      )

      val report = AppDiagnosticsManager.runFullAnalysis(app, input)
      assertEquals(15, report.componentChecks.size)
      assertTrue(report.conflicts.any { it.id == "conf_voice_mic" })
      assertTrue(report.activeIssues.any { it.module == DiagnosticModule.EQUALIZER })
      assertTrue(report.activeIssues.any { it.module == DiagnosticModule.PLAYLISTS })

      // Safe temp cache repair must delete tempCache ONLY, preserving protectedSong
      val cacheIssue = com.example.diagnostics.DiagnosticIssue(
        id = "manual_cache",
        module = DiagnosticModule.PERFORMANCE_MEMORY,
        title = "Caché temporal",
        plainExplanation = "Temp cache",
        technicalMessage = "",
        proposedSolution = "Clean",
        severity = DiagnosticSeverity.LOW,
        firstOccurredAtMs = 1L,
        lastOccurredAtMs = 1L,
        repairAction = RepairActionId.CLEAR_TEMP_CACHE
      )
      val (ok, _) = AppDiagnosticsManager.executeSafeRepair(app, cacheIssue, DiagnosticRepairCallbacks())
      assertTrue(ok)
      assertFalse("Temporary cache file should be cleaned", tempCache.exists())
      assertTrue("User music file must NEVER be deleted by repair", protectedSong.exists())

      protectedSong.delete()
      protectedDir.delete()
    }
  }

  @Test
  fun `diagnostics screen CUJ runs analysis self test confirmation repair and exports report`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    AppDiagnosticsManager.resetForTesting(app)
    AppDiagnosticsManager.initialize(app, diagnosticModeEnabled = false)

    val cacheIssue = com.example.diagnostics.DiagnosticIssue(
      id = "confirm_cache_1",
      module = DiagnosticModule.PERFORMANCE_MEMORY,
      title = "Caché temporal acumulada",
      plainExplanation = "Puedes liberar memoria temporal de portadas.",
      technicalMessage = "cache=40MB",
      proposedSolution = "Limpiar caché temporal",
      severity = DiagnosticSeverity.MEDIUM,
      firstOccurredAtMs = 1000L,
      lastOccurredAtMs = 1000L,
      repairAction = RepairActionId.CLEAR_TEMP_CACHE
    )

    var analyzedClicks = 0
    var selfTestClicks = 0
    var modeToggled: Boolean? = null
    var repairedId: String? = null
    var lastToast: String? = null
    val sampleState = com.example.diagnostics.DiagnosticsUiState(
      activeIssues = listOf(cacheIssue),
      diagnosticModeEnabled = false
    )

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        DiagnosticsScreen(
          uiState = sampleState,
          onAnalyzeApp = { analyzedClicks++ },
          onRunSelfTest = { selfTestClicks++ },
          onToggleDiagnosticMode = { modeToggled = it },
          onExecuteRepair = { repairedId = it.id },
          onDismissIssue = {},
          onClearHistory = {},
          onShowMessage = { lastToast = it },
          onBack = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diag_analyze_button").performClick()
    assertEquals(1, analyzedClicks)

    composeTestRule.onNodeWithTag("diag_self_test_button").performClick()
    assertEquals(1, selfTestClicks)

    composeTestRule.onNodeWithTag("diag_mode_switch").performClick()
    assertEquals(true, modeToggled)

    // Repair requiring confirmation opens confirmation dialog before executing
    composeTestRule.onNodeWithTag("diagnostics_screen")
      .performScrollToNode(hasTestTag("diag_repair_confirm_cache_1"))
    composeTestRule.onNodeWithTag("diag_repair_confirm_cache_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("diag_confirm_repair_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals("confirm_cache_1", repairedId)

    composeTestRule.onNodeWithTag("diagnostics_screen")
      .performScrollToNode(hasTestTag("diag_copy_report_btn"))
    composeTestRule.onNodeWithTag("diag_copy_report_btn").performClick()
    assertTrue(lastToast?.contains("copiado", ignoreCase = true) == true)
  }

  @Test
  fun `player visual preferences and custom eq profiles persist in DataStore`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val prefs = UserPreferencesRepository(app)

      val expectedVisual = PlayerVisualPrefs(
        style = PlayerVisualStyle.VINYL_STUDIO,
        backgroundMode = PlayerBackgroundMode.GRADIENT_CUSTOM,
        customPrimaryColorArgb = 0xFF22D3EE,
        customButtonColorArgb = 0xFFFB7185,
        showVisualizer = false,
        showInlineLyricsBar = true
      )
      prefs.savePlayerVisualPrefs(expectedVisual)
      val loadedVisual = prefs.playerVisualPrefs.first()
      assertEquals(PlayerVisualStyle.VINYL_STUDIO, loadedVisual.style)
      assertEquals(PlayerBackgroundMode.GRADIENT_CUSTOM, loadedVisual.backgroundMode)
      assertEquals(0xFF22D3EE, loadedVisual.customPrimaryColorArgb)
      assertEquals(0xFFFB7185, loadedVisual.customButtonColorArgb)
      assertFalse(loadedVisual.showVisualizer)

      val profile = SavedCustomEqProfile(
        id = "eq_test_1",
        name = "Studio Nocturno",
        bandLevelsMb = listOf(400, 150, -50, 250, 600),
        bassBoostStrength = 550,
        spatial3dEnabled = true
      )
      prefs.saveCustomEqProfile(profile)
      val savedProfiles = prefs.savedCustomEqProfiles.first()
      assertTrue(savedProfiles.any { it.id == "eq_test_1" && it.name == "Studio Nocturno" })

      prefs.deleteCustomEqProfile("eq_test_1")
      assertFalse(prefs.savedCustomEqProfiles.first().any { it.id == "eq_test_1" })
    }
  }

  @Test
  fun `advanced now playing screen displays artwork info times controls and preserves playback on close`() {
    val track1 = sampleTrack(1L, "We Are!", "Hiroshi Kitadani", "One Piece Best").copy(durationMs = 240_000L)
    val track2 = sampleTrack(2L, "Believer", "Imagine Dragons", "Evolve").copy(durationMs = 200_000L)
    var isPlayingState = true
    var currentIndex = 0
    var isFav = false
    var shuffle = false
    var repeat = Player.REPEAT_MODE_OFF
    var queueOpened = false
    var eqOpened = false
    var dismissed = false

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        val activeTrack = if (currentIndex == 0) track1 else track2
        AdvancedNowPlayingScreen(
          track = activeTrack,
          playbackState = PlaybackUiState(
            currentTrack = activeTrack,
            queue = listOf(track1, track2),
            currentQueueIndex = currentIndex,
            isPlaying = isPlayingState,
            positionMs = 60_000L,
            durationMs = activeTrack.durationMs,
            shuffleEnabled = shuffle,
            repeatMode = repeat
          ),
          activeSlot = TimeOfDaySlot.NIGHT,
          isFavorite = isFav,
          sleepTimerState = SleepTimerUiState(isActive = true, remainingSongsToStop = 2, targetSongsTotal = 2),
          eqState = EqualizerUiState(enabled = true),
          audioSessionId = 0,
          visualPrefs = PlayerVisualPrefs(style = PlayerVisualStyle.GLASS_AURORA),
          isDarkTheme = true,
          animationsEnabled = false,
          isSignedIn = false,
          currentLyrics = TrackLyrics(
            trackKey = track1.trackKey,
            lines = listOf(
              LyricLine(10_000L, "Arittake no yume wo kakiatsume"),
              LyricLine(55_000L, "Sagashi mono wo sagashi ni yuku no sa")
            ),
            isSynced = true,
            sourceLabel = "LRC",
            rawText = "[00:10.00] Arittake no yume wo kakiatsume\n[00:55.00] Sagashi mono wo sagashi ni yuku no sa"
          ),
          isLoadingLyrics = false,
          handsFreeVoiceActive = false,
          onDismiss = { dismissed = true },
          onToggleFavorite = { isFav = !isFav },
          onOpenQueue = { queueOpened = true },
          onOpenEqualizerSheet = { eqOpened = true },
          onOpenCustomizeSheet = {},
          onOpenSleepTimer = {},
          onOpenCastSheet = {},
          onOpenVoiceControl = {},
          onOpenFullLyricsEditor = {},
          onSearchOnlineLyrics = {},
          onShareTrack = {},
          onChangeCover = {},
          onTogglePlayPause = { isPlayingState = !isPlayingState },
          onSeekTo = {},
          onSkipPrevious = { currentIndex = 0 },
          onSkipNext = { currentIndex = 1 },
          onToggleShuffle = { shuffle = !shuffle },
          onCycleRepeat = { repeat = Player.REPEAT_MODE_ONE },
          onSaveToCloud = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("advanced_now_playing_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("player_track_title").assertTextContains("We Are!")
    composeTestRule.onNodeWithTag("player_track_artist").assertTextContains("Hiroshi Kitadani")
    composeTestRule.onNodeWithTag("player_track_album").assertTextContains("One Piece Best")
    composeTestRule.onNodeWithTag("player_elapsed_time").assertTextContains("01:00")
    composeTestRule.onNodeWithTag("player_remaining_time").assertTextContains("-03:00")
    composeTestRule.onNodeWithTag("player_live_lyric_preview").assertIsDisplayed()

    // Toggle lyrics panel inline
    composeTestRule.onNodeWithTag("player_lyrics_button").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("player_inline_lyrics_panel").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("inline_lyrics_close_btn").performScrollTo().performClick()

    // Toggle controls: Favorite, Play/Pause, Shuffle, Repeat, Next track, Queue, EQ, Collapse
    composeTestRule.onNodeWithTag("player_favorite_button").performScrollTo().performClick()
    assertTrue(isFav)

    composeTestRule.onNodeWithTag("player_shuffle_button").performScrollTo().performClick()
    assertTrue(shuffle)

    composeTestRule.onNodeWithTag("player_repeat_button").performScrollTo().performClick()
    assertEquals(Player.REPEAT_MODE_ONE, repeat)

    composeTestRule.onNodeWithTag("player_next_button").performScrollTo().performClick()
    assertEquals(1, currentIndex)

    composeTestRule.onNodeWithTag("player_queue_button").performScrollTo().performClick()
    assertTrue(queueOpened)

    composeTestRule.onNodeWithTag("player_open_eq_button").performScrollTo().performClick()
    assertTrue(eqOpened)

    // Closing the Now Playing UI must keep playback active (isPlayingState stays true)
    composeTestRule.onNodeWithTag("player_collapse_button").performClick()
    assertTrue(dismissed)
    assertTrue(isPlayingState)
  }

  @Test
  fun `playback queue eq customization and sleep timer sheets support full user actions`() {
    val t1 = sampleTrack(1L, "Track Uno", "Artista A", "Album 1")
    val t2 = sampleTrack(2L, "Track Dos", "Artista B", "Album 2")
    var movedPair: Pair<Int, Int>? = null
    var removedIdx: Int? = null
    var appendedTrack: LocalTrack? = null
    var savedProfileName: String? = null
    var selectedPlayerStyle: PlayerVisualStyle? = null
    var selectedSongCount: Int? = null
    var fadeOutVal = true
    var activeSheet by androidx.compose.runtime.mutableStateOf("queue")

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        when (activeSheet) {
          "queue" -> PlaybackQueueSheet(
            queue = listOf(t1, t2),
            currentQueueIndex = 0,
            isPlaying = true,
            libraryTracks = listOf(t1, t2),
            onJumpToIndex = {},
            onMoveItem = { from, to -> movedPair = from to to },
            onRemoveItem = { removedIdx = it },
            onPlayNext = {},
            onAppendToQueue = { appendedTrack = it },
            onDismiss = {}
          )
          "eq" -> InPlayerEqualizerSheet(
            eqState = EqualizerUiState(enabled = true),
            customProfiles = emptyList(),
            onToggleEnabled = {},
            onSelectPreset = {},
            onUpdateBand = { _, _ -> },
            onUpdateBassBoost = {},
            onToggleSpatial3d = {},
            onSaveCustomProfile = { savedProfileName = it },
            onApplyCustomProfile = {},
            onDeleteCustomProfile = {},
            onOpenFullEqualizer = {},
            onDismiss = {}
          )
          "custom" -> PlayerCustomizationSheet(
            visualPrefs = PlayerVisualPrefs(),
            isDarkTheme = true,
            animationsEnabled = true,
            onSelectStyle = { selectedPlayerStyle = it },
            onSelectBackgroundMode = {},
            onSelectPrimaryColor = {},
            onSelectButtonColor = {},
            onToggleDarkTheme = {},
            onToggleVisualizer = {},
            onToggleInlineLyrics = {},
            onToggleAnimations = {},
            onDismiss = {}
          )
          "sleep" -> SleepTimerSheet(
            sleepTimerState = SleepTimerUiState(isActive = true, remainingSongsToStop = 3, targetSongsTotal = 3, fadeOutEnabled = fadeOutVal),
            onStartPreset = {},
            onStartSongCount = { selectedSongCount = it },
            onToggleFadeOut = { fadeOutVal = it },
            onExtendFiveMinutes = {},
            onToggleStopAfterCurrentTrack = {},
            onCancelTimer = {},
            onDismiss = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("playback_queue_list").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("queue_move_up_1").performClick()
    assertEquals(1 to 0, movedPair)

    composeTestRule.onNodeWithTag("queue_remove_1").performClick()
    assertEquals(1, removedIdx)

    composeTestRule.onNodeWithTag("queue_add_songs_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("queue_library_picker_list").performScrollToIndex(1)
    composeTestRule.onNodeWithTag("queue_append_2").performClick()
    assertEquals("Track Dos", appendedTrack?.title)

    activeSheet = "eq"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_eq_save_profile_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("eq_profile_name_input").performTextInput("Mi Perfil Rock")
    composeTestRule.onNodeWithTag("eq_profile_confirm_save_btn").performClick()
    assertEquals("Mi Perfil Rock", savedProfileName)

    activeSheet = "custom"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_style_chip_neon_pulse").performScrollTo().performClick()
    assertEquals(PlayerVisualStyle.NEON_PULSE, selectedPlayerStyle)

    activeSheet = "sleep"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sleep_timer_songs_5").performScrollTo().performClick()
    assertEquals(5, selectedSongCount)

    composeTestRule.onNodeWithTag("sleep_timer_fade_out_switch").performScrollTo().performClick()
    assertFalse(fadeOutVal)
  }

  @Test
  fun `stage 1 playback engine prefs queue presets and sleep timer fade persist cleanly`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val prefs = UserPreferencesRepository(app)

      val engine = com.example.model.PlaybackEnginePrefs(
        crossfadeEnabled = true,
        crossfadeDurationSec = 8,
        crossfadeCurve = com.example.model.CrossfadeCurve.SMOOTH_S,
        fadeInEnabled = true,
        smartDjEnabled = true,
        djTransitionStyle = com.example.model.DjTransitionStyle.QUICK_CUT,
        djSkipSilenceEnabled = true,
        djVolumeNormalization = false,
        avoidQueueRepeats = true
      )
      prefs.savePlaybackEnginePrefs(engine)
      val loadedEngine = prefs.playbackEnginePrefs.first()
      assertTrue(loadedEngine.crossfadeEnabled)
      assertEquals(8, loadedEngine.crossfadeDurationSec)
      assertEquals(com.example.model.CrossfadeCurve.SMOOTH_S, loadedEngine.crossfadeCurve)
      assertTrue(loadedEngine.smartDjEnabled)
      assertEquals(com.example.model.DjTransitionStyle.QUICK_CUT, loadedEngine.djTransitionStyle)
      assertFalse(loadedEngine.djVolumeNormalization)

      val preset = com.example.model.SavedQueuePreset(
        id = "q_night",
        name = "Chill Nocturno",
        trackKeys = listOf("a|b|180", "c|d|200")
      )
      prefs.saveQueuePreset(preset)
      assertTrue(prefs.savedQueuePresets.first().any { it.id == "q_night" && it.name == "Chill Nocturno" })

      prefs.setSleepTimerFadeOutSeconds(10)
      prefs.saveSleepTimerCustomPresets(listOf(10, 45, 120))
      assertEquals(10, prefs.appSettingsPrefs.first().sleepTimerFadeOutSeconds)
      assertEquals(listOf(10, 45, 120), prefs.sleepTimerCustomPresets.first())
    }
  }

  @Test
  fun `stage 1 pro controls sheet queue presets and sleep fade options work via Compose CUJs`() {
    var updatedEngine = com.example.model.PlaybackEnginePrefs()
    var mixTriggered = 0
    var selectedVisualizer = com.example.model.VisualizerStyle.BARS
    var smartShuffleCalls = 0
    var dedupCalls = 0
    var savedQueueName = ""
    var selectedFadeSec = 3
    var savedTimerPresetMin = 0
    var activeView by androidx.compose.runtime.mutableStateOf("pro")
    val t1 = sampleTrack(1L, "Uno", "ArtA", "Alb1")
    val t2 = sampleTrack(2L, "Dos", "ArtB", "Alb2")

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        when (activeView) {
          "pro" -> com.example.ui.components.PlaybackProControlsSheet(
            enginePrefs = updatedEngine,
            djRuntime = com.example.player.SmartDjRuntimeState(
              currentAnalysis = com.example.player.AudioTrackAnalysis("k1", 200_000L, 1000L, 195_000L, 124, 0.8f, 5000L)
            ),
            visualizerStyle = selectedVisualizer,
            visualizerIntensity = 1.0f,
            canMixNow = true,
            onUpdateEnginePrefs = { updatedEngine = it },
            onTriggerManualDjMix = { mixTriggered++ },
            onSelectVisualizerStyle = { selectedVisualizer = it },
            onUpdateVisualizerIntensity = {},
            onDismiss = {}
          )
          "queue" -> PlaybackQueueSheet(
            queue = listOf(t1, t2),
            currentQueueIndex = 0,
            isPlaying = true,
            libraryTracks = listOf(t1, t2),
            onJumpToIndex = {},
            onMoveItem = { _, _ -> },
            onRemoveItem = {},
            onPlayNext = {},
            onAppendToQueue = {},
            onSmartShuffleQueue = { smartShuffleCalls++ },
            onDeduplicateQueue = { dedupCalls++ },
            onSaveQueuePreset = { savedQueueName = it },
            onDismiss = {}
          )
          "sleep" -> SleepTimerSheet(
            sleepTimerState = SleepTimerUiState(fadeOutEnabled = true, fadeOutDurationSec = selectedFadeSec),
            fadeOutDurationSec = selectedFadeSec,
            onStartPreset = {},
            onChangeFadeOutDuration = { selectedFadeSec = it },
            onSaveCustomPreset = { savedTimerPresetMin = it },
            onExtendFiveMinutes = {},
            onToggleStopAfterCurrentTrack = {},
            onCancelTimer = {},
            onDismiss = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("pro_smart_dj_switch").performScrollTo().performClick()
    assertTrue(updatedEngine.smartDjEnabled)
    assertTrue(updatedEngine.crossfadeEnabled)

    composeTestRule.onNodeWithTag("dj_mix_now_button").performScrollTo().performClick()
    assertEquals(1, mixTriggered)

    composeTestRule.onNodeWithTag("visualizer_style_spectrum").performScrollTo().performClick()
    assertEquals(com.example.model.VisualizerStyle.SPECTRUM, selectedVisualizer)

    activeView = "queue"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("queue_smart_shuffle_chip").performClick()
    assertEquals(1, smartShuffleCalls)
    composeTestRule.onNodeWithTag("queue_dedupe_chip").performClick()
    assertEquals(1, dedupCalls)
    composeTestRule.onNodeWithTag("queue_save_preset_chip").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("queue_save_name_input").performTextInput("Viaje")
    composeTestRule.onNodeWithTag("queue_save_confirm_btn").performClick()
    assertEquals("Viaje", savedQueueName)

    activeView = "sleep"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sleep_fade_sec_row")
      .performScrollTo()
      .performScrollToNode(hasTestTag("sleep_fade_sec_10"))
    composeTestRule.onNodeWithTag("sleep_fade_sec_10").performClick()
    assertEquals(10, selectedFadeSec)
    composeTestRule.onNodeWithTag("sleep_timer_save_preset_btn").performScrollTo().performClick()
    assertEquals(20, savedTimerPresetMin)
  }

  @Test
  fun `stage 2 per-device sound profiles and treble tone persist cleanly in DataStore`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val prefs = UserPreferencesRepository(app)

      val stored = com.example.data.EqualizerStoredPrefs(
        enabled = true,
        presetIndex = 9,
        bandLevelsMb = listOf(350, 150, 200, 450, 400),
        bassBoostStrength = 300,
        trebleBoostMb = 450,
        autoSwitchDeviceProfile = true
      )
      prefs.saveEqualizerPrefs(stored)
      val loadedEq = prefs.equalizerPrefs.first()
      assertEquals(450, loadedEq.trebleBoostMb)
      assertTrue(loadedEq.autoSwitchDeviceProfile)

      val btProfile = com.example.model.DeviceSoundProfile(
        deviceType = com.example.model.AudioDeviceProfileType.BLUETOOTH,
        presetIndex = 10,
        bandLevelsMb = listOf(550, 200, 300, 250, 500),
        bassBoostStrength = 450,
        trebleBoostMb = 250,
        spatial3dEnabled = true,
        balanceLR = 0.15f
      )
      prefs.saveDeviceSoundProfile(btProfile)
      val savedMap = prefs.deviceSoundProfiles.first()
      val loadedBt = savedMap[com.example.model.AudioDeviceProfileType.BLUETOOTH]
      assertEquals(10, loadedBt?.presetIndex)
      assertEquals(250, loadedBt?.trebleBoostMb)
      assertEquals(true, loadedBt?.spatial3dEnabled)
    }
  }

  @Test
  fun `stage 2 equalizer screen per-device profile switch save and 11 presets work via Compose`() {
    var autoSwitch by androidx.compose.runtime.mutableStateOf(true)
    var selectedDevice by androidx.compose.runtime.mutableStateOf(com.example.model.AudioDeviceProfileType.PHONE_SPEAKER)
    var savedDevice: com.example.model.AudioDeviceProfileType? = null
    var selectedPreset by androidx.compose.runtime.mutableStateOf(0)
    val profiles = mapOf(
      com.example.model.AudioDeviceProfileType.BLUETOOTH to com.example.model.DeviceSoundProfile(
        deviceType = com.example.model.AudioDeviceProfileType.BLUETOOTH,
        presetIndex = 8,
        trebleBoostMb = 150
      )
    )

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        com.example.ui.screens.EqualizerScreen(
          eqState = com.example.player.EqualizerUiState(
            selectedPresetIndex = selectedPreset,
            autoSwitchDeviceProfile = autoSwitch,
            activeDeviceProfileType = selectedDevice,
            trebleBoostMb = 200
          ),
          onToggleEnabled = {},
          onSelectPreset = { selectedPreset = it },
          onUpdateBand = { _, _ -> },
          onUpdateBassBoost = {},
          onToggleSpatial3d = {},
          onSelectSpatial3dPreset = {},
          onUpdateSpatial3dStrength = {},
          onUpdateClarityBoost = {},
          onSelectHearingProfile = {},
          onSelectAudioChannelMode = {},
          onUpdateBalanceLR = {},
          deviceSoundProfiles = profiles,
          detectedDeviceType = com.example.model.AudioDeviceProfileType.BLUETOOTH,
          onUpdateTrebleBoost = {},
          onToggleAutoSwitchDeviceProfile = { autoSwitch = it },
          onSelectDeviceProfile = { selectedDevice = it },
          onSaveCurrentDeviceProfile = { savedDevice = it },
          onResetDeviceProfile = {},
          onOpenThemeCustomization = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("eq_device_autoswitch_switch")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    assertFalse(autoSwitch)

    composeTestRule.onNodeWithTag("eq_device_chips_row")
      .performScrollTo()
      .performScrollToNode(hasTestTag("eq_device_chip_bluetooth"))
    composeTestRule.onNodeWithTag("eq_device_chip_bluetooth").performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.model.AudioDeviceProfileType.BLUETOOTH, selectedDevice)

    composeTestRule.onNodeWithTag("eq_save_device_profile_btn")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.model.AudioDeviceProfileType.BLUETOOTH, savedDevice)

    composeTestRule.onNodeWithTag("equalizer_list")
      .performScrollToNode(hasTestTag("eq_presets_row"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("eq_presets_row")
      .performScrollToNode(hasTestTag("eq_preset_10"))
    composeTestRule.onNodeWithTag("eq_preset_10")
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(10, selectedPreset)
  }

  @Test
  fun `stage 3 custom theme json import export and home layout persistence work in DataStore`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val prefs = UserPreferencesRepository(app)

      val created = com.example.model.CustomThemeConfig(
        id = "theme_cyber_1",
        name = "Cyber Violet",
        isDark = true,
        primaryArgb = 0xFF22D3EE,
        secondaryArgb = 0xFFEC4899,
        backgroundArgb = 0xFF07090F,
        surfaceArgb = 0xFF151226,
        buttonArgb = 0xFF22D3EE
      )
      val exportedJson = created.toJsonString()
      val imported = com.example.model.CustomThemeConfig.fromJsonString(exportedJson, forceNewId = true)
      assertNotNull(imported)
      assertEquals("Cyber Violet", imported?.name)
      assertEquals(0xFF22D3EE, imported?.primaryArgb)
      assertTrue(imported?.id != created.id)
      assertNull(com.example.model.CustomThemeConfig.fromJsonString("{invalid json"))

      prefs.saveCustomTheme(created, setAsActive = true)
      prefs.setFontSizeScale(1.18f)
      prefs.setWallpaperBlurDp(12f)

      val loadedThemes = prefs.customThemes.first()
      assertTrue(loadedThemes.any { it.id == "theme_cyber_1" })
      val loadedAppearance = prefs.appearancePrefs.first()
      assertEquals("theme_cyber_1", loadedAppearance.activeCustomThemeId)
      assertEquals(1.18f, loadedAppearance.fontSizeScale, 0.01f)
      assertEquals(12f, loadedAppearance.wallpaperBlurDp, 0.1f)

      val homeConfig = com.example.model.HomeLayoutConfig.defaultConfig()
        .moveSection(com.example.model.HomeSectionId.RECENT_PLAYS, -1)
        .copy(showAlbumName = false, showDuration = false)
      val decodedHome = com.example.model.HomeLayoutConfig.decode(com.example.model.HomeLayoutConfig.encode(homeConfig))
      assertEquals(homeConfig.orderedSections, decodedHome.orderedSections)
      assertFalse(decodedHome.showAlbumName)

      prefs.saveHomeLayoutConfig(homeConfig)
      prefs.saveHomeLayoutPreset(
        com.example.model.HomeLayoutPreset(id = "hp_1", name = "Limpio", config = homeConfig)
      )
      val storedHome = prefs.homeLayoutConfig.first()
      assertFalse(storedHome.showAlbumName)
      assertTrue(prefs.homeLayoutPresets.first().any { it.name == "Limpio" })

      prefs.restoreOriginalAppearanceDefaults()
      val restoredAppearance = prefs.appearancePrefs.first()
      assertEquals(1.0f, restoredAppearance.fontSizeScale, 0.01f)
      assertEquals(0f, restoredAppearance.wallpaperBlurDp, 0.01f)
      assertNull(restoredAppearance.activeCustomThemeId)
    }
  }

  @Test
  fun `stage 3 custom theme builder, home customization, and player layout sheets interact via Compose`() {
    var savedTheme: com.example.model.CustomThemeConfig? = null
    var homeConfig = com.example.model.HomeLayoutConfig.defaultConfig()
    var resetHomeCalled = false
    var visualPrefs = com.example.model.PlayerVisualPrefs()
    var sheetMode by androidx.compose.runtime.mutableStateOf("theme")

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        when (sheetMode) {
          "theme" -> com.example.ui.components.CustomThemeBuilderSheet(
            savedThemes = emptyList(),
            activeCustomThemeId = null,
            onSaveTheme = { savedTheme = it },
            onApplyTheme = {},
            onDeleteTheme = {},
            onImportThemeJson = { true },
            onDismiss = {}
          )
          "home" -> com.example.ui.components.HomeCustomizationSheet(
            config = homeConfig,
            savedPresets = emptyList(),
            onUpdateConfig = { homeConfig = it },
            onSavePreset = {},
            onApplyPreset = {},
            onDeletePreset = {},
            onResetDefault = {
              resetHomeCalled = true
              homeConfig = com.example.model.HomeLayoutConfig.defaultConfig()
            },
            onDismiss = {}
          )
          "player" -> com.example.ui.components.PlayerCustomizationSheet(
            visualPrefs = visualPrefs,
            isDarkTheme = true,
            animationsEnabled = true,
            onSelectStyle = { visualPrefs = visualPrefs.copy(style = it) },
            onSelectBackgroundMode = { visualPrefs = visualPrefs.copy(backgroundMode = it) },
            onUpdateBlurIntensity = { visualPrefs = visualPrefs.copy(blurIntensity = it) },
            onSelectLayoutMode = { visualPrefs = visualPrefs.copy(layoutMode = it) },
            onSelectControlStyle = { visualPrefs = visualPrefs.copy(controlStyle = it) },
            onSelectPrimaryColor = {},
            onSelectButtonColor = {},
            onToggleDarkTheme = {},
            onToggleVisualizer = {},
            onToggleInlineLyrics = {},
            onToggleAnimations = {},
            onRestoreDefaults = { visualPrefs = com.example.model.PlayerVisualPrefs() },
            onDismiss = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("custom_theme_name_input").performScrollTo().performTextInput("Aurora Neon")
    composeTestRule.onNodeWithTag("custom_theme_dark_switch").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("custom_theme_save_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals("Aurora Neon", savedTheme?.name)
    assertEquals(false, savedTheme?.isDark)

    sheetMode = "home"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("home_show_album_switch").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertFalse(homeConfig.showAlbumName)
    composeTestRule.onNodeWithTag("home_reset_default_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(resetHomeCalled)
    assertTrue(homeConfig.showAlbumName)

    sheetMode = "player"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("player_layout_modes_row")
      .performScrollTo()
      .performScrollToNode(hasTestTag("player_layout_chip_immersive"))
    composeTestRule.onNodeWithTag("player_layout_chip_immersive").performClick()
    composeTestRule.onNodeWithTag("player_control_styles_row")
      .performScrollTo()
      .performScrollToNode(hasTestTag("player_control_chip_classic_studio"))
    composeTestRule.onNodeWithTag("player_control_chip_classic_studio").performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.model.PlayerLayoutMode.IMMERSIVE, visualPrefs.layoutMode)
    assertEquals(com.example.model.PlayerControlStyle.CLASSIC_STUDIO, visualPrefs.controlStyle)
  }

  @Test
  fun `ritmo 2 room database records qualified plays exports json and preserves overrides on clear`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val statsRepo = com.example.data.stats.StatsRepository(app)
      statsRepo.clearAllListeningHistory()

      val t1 = sampleTrack(11L, "Luz de Neón", "Soda Stereo", "Signos").copy(genre = "Rock", durationMs = 210_000L)
      val t2 = sampleTrack(12L, "Tema Corto", "Otro", "EP").copy(genre = "Indie", durationMs = 150_000L)

      statsRepo.recordListening(t1, deltaSeconds = 45L, qualifiedPlay = true, slot = TimeOfDaySlot.NIGHT)
      statsRepo.recordListening(t2, deltaSeconds = 5L, qualifiedPlay = false, slot = TimeOfDaySlot.MORNING)
      statsRepo.saveMetadataOverride(
        trackKey = t1.trackKey,
        title = "Luz de Neón (Remaster)",
        artist = "Soda Stereo",
        album = "Signos",
        genre = "Rock Latino",
        hiddenAsDuplicate = false
      )
      statsRepo.saveWrappedSnapshot(2025, """{"year":2025}""")

      val events = statsRepo.allEvents.first()
      assertEquals(2, events.size)
      val trackStats = statsRepo.allTrackStats.first()
      assertEquals(1L, trackStats.first { it.trackKey == t1.trackKey }.qualifiedPlays)
      assertEquals(0L, trackStats.first { it.trackKey == t2.trackKey }.qualifiedPlays)
      assertTrue(statsRepo.savedWrappedYears.first().any { it.year == 2025 })

      val exported = statsRepo.exportStatsJson()
      assertTrue(exported.contains("Luz de Neón"))
      assertTrue(exported.contains("ritmo_local_stats_export_v1"))

      // Clearing listening history wipes events/stats/wrapped but preserves user metadata overrides
      statsRepo.clearAllListeningHistory()
      assertTrue(statsRepo.allEvents.first().isEmpty())
      assertTrue(statsRepo.allTrackStats.first().isEmpty())
      assertEquals(
        "Luz de Neón (Remaster)",
        statsRepo.getAllMetadataOverrides()[t1.trackKey]?.title
      )
      statsRepo.clearMetadataOverride(t1.trackKey)
    }
  }

  @Test
  fun `ritmo 2 datastore persists extreme visual designer profile and ambient mixes`() {
    kotlinx.coroutines.runBlocking {
      val app = ApplicationProvider.getApplicationContext<Application>()
      val v2Repo = com.example.data.Ritmo2PreferencesRepository(app)

      val style = com.example.model.ExtremeVisualStyleConfig(
        cardCornerDp = 24f,
        cardBorderWidthDp = 2.0f,
        surfaceTransparencyAlpha = 0.82f,
        customGradientEnabled = true,
        miniPlayerBarStyle = com.example.model.MiniPlayerBarStyle.DOCKED_GLASS
      )
      v2Repo.saveExtremeVisualStyle(style)
      assertEquals(24f, v2Repo.extremeVisualStyle.first().cardCornerDp, 0.01f)
      assertEquals(com.example.model.MiniPlayerBarStyle.DOCKED_GLASS, v2Repo.extremeVisualStyle.first().miniPlayerBarStyle)

      val designer = com.example.model.PlayerDesignerConfig(
        artworkPosition = com.example.model.ArtworkPositionMode.TOP,
        progressBarStyle = com.example.model.ProgressBarVisualStyle.WAVEFORM_BAR,
        hiddenActions = setOf(com.example.model.PlayerQuickAction.VOICE),
        immersiveEffectsEnabled = false
      )
      v2Repo.savePlayerDesignerConfig(designer)
      val loadedDesigner = v2Repo.playerDesignerConfig.first()
      assertEquals(com.example.model.ArtworkPositionMode.TOP, loadedDesigner.artworkPosition)
      assertEquals(com.example.model.ProgressBarVisualStyle.WAVEFORM_BAR, loadedDesigner.progressBarStyle)
      assertTrue(com.example.model.PlayerQuickAction.VOICE in loadedDesigner.hiddenActions)
      assertFalse(loadedDesigner.immersiveEffectsEnabled)

      val profile = com.example.model.MusicalProfileConfig(
        displayName = "Oyente Pro",
        bio = "Sin cloud por defecto",
        statsCloudSyncEnabled = false
      )
      v2Repo.saveMusicalProfileConfig(profile)
      assertEquals("Oyente Pro", v2Repo.musicalProfileConfig.first().displayName)

      val mix = com.example.model.SavedAmbientMix(
        id = "mix_focus_1",
        categoryId = com.example.model.AmbientMoodCategory.FOCUS.id,
        name = "Focus 2.0",
        trackKeys = listOf("a|b|180")
      )
      v2Repo.saveAmbientMix(mix)
      assertTrue(v2Repo.savedAmbientMixes.first().any { it.id == "mix_focus_1" })
      v2Repo.deleteAmbientMix("mix_focus_1")
      assertFalse(v2Repo.savedAmbientMixes.first().any { it.id == "mix_focus_1" })

      v2Repo.restoreExtremeVisualDefaults()
      v2Repo.restorePlayerDesignerDefaults()

      val homeDecoded = com.example.model.HomeLayoutConfig.decode(
        com.example.model.HomeLayoutConfig.encode(
          com.example.model.HomeLayoutConfig.defaultConfig().copy(showTodayTimeStat = false, showRecapBannerCard = false)
        )
      )
      assertFalse(homeDecoded.showTodayTimeStat)
      assertFalse(homeDecoded.showRecapBannerCard)
    }
  }

  @Test
  fun `ritmo 2 recap history and profile screens render stats switch tabs and confirm history clear`() {
    val engine = com.example.data.stats.StatsAnalyticsEngine
    val tsFeb = 1_739_534_400_000L // 2025-02-14
    val tsMar = 1_742_040_000_000L // 2025-03-15
    val marMk = engine.buildMonthKey(tsMar)
    val events = listOf(
      com.example.data.stats.ListeningEventEntity(
        trackKey = "k1", trackTitle = "Aurora", artistName = "Viento", albumName = "Norte", albumId = 10L,
        genre = "Rock", durationMs = 200_000L, listenedSeconds = 120L, isCompletedPlay = true,
        timestampMs = tsFeb, dateKey = engine.buildDateKey(tsFeb), monthKey = engine.buildMonthKey(tsFeb),
        year = 2025, hourOfDay = 10, timeSlot = "MORNING"
      ),
      com.example.data.stats.ListeningEventEntity(
        trackKey = "k2", trackTitle = "Eclipse", artistName = "Sol", albumName = "Cielo", albumId = 20L,
        genre = "Indie", durationMs = 210_000L, listenedSeconds = 210L, isCompletedPlay = true,
        timestampMs = tsMar, dateKey = engine.buildDateKey(tsMar), monthKey = marMk,
        year = 2025, hourOfDay = 22, timeSlot = "NIGHT"
      )
    )
    val trackStats = listOf(
      com.example.data.stats.TrackStatsEntity(
        trackKey = "k1", trackTitle = "Aurora", artistName = "Viento", albumName = "Norte", albumId = 10L,
        genre = "Rock", firstPlayedAtMs = tsFeb, lastPlayedAtMs = tsFeb, totalSeconds = 200L, qualifiedPlays = 2L
      )
    )
    val recap = engine.computeRecapReport(events, marMk)
    val wrapped = engine.computeWrappedReport(events, 2025)
    val realtime = engine.computeRealTimeSummary(events, tsMar)
    val history = engine.computeHistoryReport(events, trackStats, marMk, 2025, nowMs = tsFeb + 30L * 24 * 60 * 60 * 1000L)
    val achievements = engine.computeAchievements(events, trackStats, favoriteCount = 2, customPlaylistCount = 1)

    var clearedConfirmed = false
    var exportedClicks = 0
    var replayedForgottenKey: String? = null
    var savedProfile = com.example.model.MusicalProfileConfig()
    var screenMode by androidx.compose.runtime.mutableStateOf("recap")

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        when (screenMode) {
          "recap" -> com.example.ui.screens.RecapAndWrappedScreen(
            recapReport = recap,
            wrappedReport = wrapped,
            realTimeSummary = realtime,
            savedWrappedYears = listOf(2025),
            cloudSyncEnabled = false,
            isSignedIn = false,
            onSelectRecapMonth = {},
            onSelectWrappedYear = {},
            onSaveWrappedSnapshot = {},
            onToggleCloudSync = {},
            onExportStatsJson = { exportedClicks++ },
            onClearAllStatsConfirmed = { clearedConfirmed = true },
            onOpenHistory = {},
            onOpenProfile = {}
          )
          "history" -> com.example.ui.screens.MusicalHistoryScreen(
            report = history,
            onSelectMonth = {},
            onSelectYear = {},
            onPlayTrackByKey = { replayedForgottenKey = it }
          )
          "profile" -> com.example.ui.screens.MusicalProfileScreen(
            profileConfig = savedProfile,
            achievements = achievements,
            recapReport = recap,
            realTimeSummary = realtime,
            favoriteTracks = listOf(sampleTrack(1L, "Favorita", "Sol", "Cielo")),
            onSaveProfileConfig = { savedProfile = it },
            onOpenRecap = {},
            onPlayTrack = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("recap_tab_wrapped").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Tu año 2025").assertIsDisplayed()

    composeTestRule.onNodeWithTag("recap_tab_realtime").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Estadísticas en tiempo real").assertIsDisplayed()

    composeTestRule.onNodeWithTag("recap_list")
      .performScrollToNode(hasTestTag("recap_export_json_btn"))
    composeTestRule.onNodeWithTag("recap_export_json_btn").performClick()
    assertEquals(1, exportedClicks)

    composeTestRule.onNodeWithTag("recap_list")
      .performScrollToNode(hasTestTag("recap_clear_history_btn"))
    composeTestRule.onNodeWithTag("recap_clear_history_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("¿Borrar todo el historial de escucha?").assertIsDisplayed()
    composeTestRule.onAllNodesWithText("Borrar historial")[1].performClick()
    composeTestRule.waitForIdle()
    assertTrue(clearedConfirmed)

    screenMode = "history"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("history_compare_halves").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("history_list")
      .performScrollToNode(hasTestTag("history_play_forgotten_k1"))
    composeTestRule.onNodeWithTag("history_play_forgotten_k1").performClick()
    assertEquals("k1", replayedForgottenKey)

    screenMode = "profile"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Privado por defecto").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_edit_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Guardar").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Oyente Ritmo Local", savedProfile.displayName)
  }

  @Test
  fun `ritmo 2 organizer extreme editor player designer immersive overlay and smart mixes CUJs`() {
    val t1 = sampleTrack(1L, "Música Ligera", "Soda Stereo", "Canción Animal").copy(genre = "Rock", albumArtUri = "art://1")
    val t2 = sampleTrack(2L, "Musica Ligera (Live)", "Soda Stereo", "Concierto").copy(genre = "Rock", albumArtUri = null)
    var savedMetaTitle = ""
    var hiddenDupTrack: LocalTrack? = null
    var savedExtreme = com.example.model.ExtremeVisualStyleConfig()
    var savedDesigner = com.example.model.PlayerDesignerConfig()
    var immersiveEffects = true
    var smartPlayCount = 0
    var activeUi by androidx.compose.runtime.mutableStateOf("organizer")

    composeTestRule.setContent {
      RitmoLocalTheme(darkTheme = true) {
        when (activeUi) {
          "organizer" -> com.example.ui.screens.LibraryOrganizerScreen(
            allTracks = listOf(t1, t2),
            trackStatsByKey = emptyMap(),
            metadataOverrides = emptyMap(),
            onPlayTracksQueue = { _, _ -> },
            onFetchCoverOnline = {},
            onPickCoverFromGallery = {},
            onSaveMetadataOverride = { _, title, _, _, _ -> savedMetaTitle = title },
            onClearMetadataOverride = {},
            onHideDuplicateTrack = { tr, _ -> hiddenDupTrack = tr }
          )
          "extreme" -> com.example.ui.components.ExtremeVisualEditorSheet(
            currentStyle = savedExtreme,
            onSaveStyle = { savedExtreme = it },
            onOpenCustomThemeBuilder = {},
            onOpenPlayerDesigner = {},
            onRestoreOriginalDesign = {},
            onDismiss = {}
          )
          "designer" -> com.example.ui.components.PlayerDesignerSheet(
            designerConfig = savedDesigner,
            visualPrefs = PlayerVisualPrefs(),
            onSaveDesigner = { savedDesigner = it },
            onSelectControlStyle = {},
            onSelectLayoutMode = {},
            onSelectVisualizerStyle = {},
            onRestoreDefaults = {},
            onDismiss = {}
          )
          "immersive" -> com.example.ui.components.ImmersiveVisualOverlay(
            track = t1,
            playbackState = PlaybackUiState(currentTrack = t1, isPlaying = true, positionMs = 15_000L, durationMs = 200_000L),
            audioSessionId = 0,
            initialVisualizerStyle = com.example.model.VisualizerStyle.BARS,
            effectsEnabled = immersiveEffects,
            onToggleEffects = { immersiveEffects = it },
            onSelectVisualizerStyle = {},
            onTogglePlayPause = {},
            onSkipPrevious = {},
            onSkipNext = {},
            onDismiss = {}
          )
          "mixes" -> com.example.ui.screens.MixesScreen(
            selectedSlot = TimeOfDaySlot.MORNING,
            currentRealTimeSlot = TimeOfDaySlot.MORNING,
            playlistsBySlot = emptyMap(),
            allTracks = listOf(t1, t2),
            customPlaylists = emptyList(),
            favoriteTracks = listOf(t1),
            favoriteTrackKeys = setOf(t1.trackKey),
            mostPlayedTracks = emptyList(),
            albumPlaylists = emptyList(),
            artistPlaylists = emptyList(),
            currentPlayingTrackKey = null,
            animationsEnabled = false,
            trackActions = com.example.ui.screens.TrackActionsCallbacks(),
            onSelectSlot = {},
            onRegeneratePlaylist = {},
            onPlayTimeSlotPlaylist = { _, _ -> },
            onPlayTracksQueue = { _, _ -> smartPlayCount++ },
            onToggleFavorite = {},
            onCreateCustomPlaylist = {},
            onRenameCustomPlaylist = { _, _ -> },
            onDeleteCustomPlaylist = {},
            onRemoveTrackFromCustomPlaylist = { _, _ -> },
            onChangeAlbumCover = {},
            ambientMixesByCategory = com.example.model.AmbientMixGenerator.generateAllCategories(listOf(t1, t2)),
            onRegenerateAmbientMix = {},
            onSaveAmbientMixAsPlaylist = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    // Non-destructive metadata confirmation flow
    composeTestRule.onNodeWithTag("organizer_list")
      .performScrollToNode(hasTestTag("organizer_edit_meta_${t1.trackKey}"))
    composeTestRule.onNodeWithTag("organizer_edit_meta_${t1.trackKey}").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Revisar").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Confirmar y aplicar").performClick()
    composeTestRule.waitForIdle()
    assertEquals(t1.title, savedMetaTitle)

    // Confirmed duplicate hide without file deletion
    composeTestRule.onNodeWithTag("organizer_list").performScrollToIndex(1)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("organizer_tab_duplicates").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("organizer_list")
      .performScrollToNode(hasTestTag("organizer_hide_dup_${t2.trackKey}"))
    composeTestRule.onNodeWithTag("organizer_hide_dup_${t2.trackKey}").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Sí, ocultar de la lista").performClick()
    composeTestRule.waitForIdle()
    assertEquals(t2.trackKey, hiddenDupTrack?.trackKey)

    // Extreme visual editor
    activeUi = "extreme"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("extreme_gradient_switch").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("extreme_minibar_docked_glass").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("extreme_save_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertTrue(savedExtreme.customGradientEnabled)
    assertEquals(com.example.model.MiniPlayerBarStyle.DOCKED_GLASS, savedExtreme.miniPlayerBarStyle)

    // Player designer
    activeUi = "designer"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("designer_artwork_pos_compact_left").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("designer_toggle_vis_shuffle").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("designer_save_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(com.example.model.ArtworkPositionMode.COMPACT_LEFT, savedDesigner.artworkPosition)
    assertTrue(com.example.model.PlayerQuickAction.SHUFFLE in savedDesigner.hiddenActions)

    // Immersive overlay battery toggle
    activeUi = "immersive"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("immersive_battery_toggle_btn").performClick()
    composeTestRule.waitForIdle()
    assertFalse(immersiveEffects)

    // Smart Mixes hub tab
    activeUi = "mixes"
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("playlist_hub_smart_mixes").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("smart_moods_row")
      .performScrollToNode(hasTestTag("smart_mix_chip_workout"))
    composeTestRule.onNodeWithTag("smart_mix_chip_workout").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("smart_mix_play_btn").performClick()
    assertEquals(1, smartPlayCount)
  }

  private fun sampleTrack(id: Long, title: String, artist: String, album: String) = LocalTrack(
    id = id,
    trackKey = "$title|$artist|180",
    title = title,
    artist = artist,
    album = album,
    albumId = id,
    durationMs = 180_000L,
    contentUri = "content://media/external/audio/media/$id",
    albumArtUri = null,
    dateAdded = 1000L
  )
}

