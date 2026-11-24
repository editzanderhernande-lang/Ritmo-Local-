package com.example

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.example.data.AudioClipTrimmer
import com.example.data.LocalMusicRepository
import com.example.data.LyricsRepository
import com.example.model.AppAccentPalette
import com.example.model.AppLanguage
import com.example.model.AudioChannelMode
import com.example.model.LocalTrack
import com.example.model.RegionalChatMessage
import com.example.model.StudioAudioMode
import com.example.model.UiStyleMode
import com.example.player.MonoAndBalanceAudioProcessor
import com.example.player.VoiceAudioFocusCoordinator
import com.example.ui.i18n.extraStringsFor
import com.example.ui.i18n.stringsFor
import com.example.voice.ParsedVoiceCommand
import com.example.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RitmoLocalUnitTest {

    @Test
    fun shortAndWhatsAppAudioFilter_identifiesVoiceNotesAndShortClips() {
        assertTrue(LocalMusicRepository.isShortOrVoiceNoteAudio(durationMs = 18_000L, title = "Intro"))
        assertTrue(
            LocalMusicRepository.isShortOrVoiceNoteAudio(
                durationMs = 120_000L,
                title = "PTT-20250412-WA0008",
                displayName = "PTT-20250412-WA0008.opus"
            )
        )
        assertTrue(
            LocalMusicRepository.isShortOrVoiceNoteAudio(
                durationMs = 95_000L,
                title = "Audio",
                filePath = "/storage/emulated/0/WhatsApp/Media/WhatsApp Audio/AUD-1.opus"
            )
        )
        assertFalse(
            LocalMusicRepository.isShortOrVoiceNoteAudio(
                durationMs = 215_000L,
                title = "Noche Estrellada",
                displayName = "noche_estrellada.flac",
                filePath = "/storage/emulated/0/Music/noche_estrellada.flac",
                album = "Horizontes"
            )
        )
    }

    @Test
    fun timeOfDaySlot_coversAll24HoursCleanly() {
        (0..23).forEach { hour ->
            val slot = com.example.model.TimeOfDaySlot.fromHour(hour)
            assertTrue(slot in com.example.model.TimeOfDaySlot.entries)
        }
    }

    @Test
    fun allEightLanguages_provideNonBlankLocalizedStringsAndZanderCredit() {
        assertEquals(8, AppLanguage.entries.size)
        AppLanguage.entries.forEach { lang ->
            val s = stringsFor(lang)
            assertTrue("Missing credit for $lang", s.creditZander.contains("Zander"))
            assertTrue("Blank settingsTitle for $lang", s.settingsTitle.isNotBlank())
            assertTrue("Blank morningGreetingBody for $lang", s.morningGreetingBody.isNotBlank())
            assertTrue("Blank regionalChatTitle for $lang", s.regionalChatTitle.isNotBlank())
        }
    }

    @Test
    fun monoAndBalanceAudioProcessor_downmixesMonoAndAppliesBalance() {
        val processor = MonoAndBalanceAudioProcessor()
        val format = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        // Frame: L = 1000, R = 0
        val input = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).apply {
            putShort(1000.toShort())
            putShort(0.toShort())
            flip()
        }

        // 1) Mono mode -> both L and R become average (500, 500)
        processor.setChannelConfig(AudioChannelMode.MONO, 0f)
        processor.queueInput(input)
        val monoOut = processor.output
        assertEquals(4, monoOut.remaining())
        assertEquals(500.toShort(), monoOut.short)
        assertEquals(500.toShort(), monoOut.short)

        // 2) Full Right balance (+1.0) in Stereo -> Left attenuated to 0, Right untouched
        processor.setChannelConfig(AudioChannelMode.BALANCED, 1.0f)
        val input2 = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).apply {
            putShort(1200.toShort())
            putShort(800.toShort())
            flip()
        }
        processor.queueInput(input2)
        val rightOut = processor.output
        assertEquals(0.toShort(), rightOut.short)
        assertEquals(800.toShort(), rightOut.short)
    }

    @Test
    fun offlineVoiceCommandParser_parsesMultiLanguageCommandsAndArtistQueries() {
        assertEquals(ParsedVoiceCommand.NextTrack, VoiceCommandParser.parse("ridmo siguiente canción"))
        assertEquals(ParsedVoiceCommand.PreviousTrack, VoiceCommandParser.parse("Ritmo anterior canción"))
        assertEquals(ParsedVoiceCommand.NextTrack, VoiceCommandParser.parse("Ritmo next track"))
        assertEquals(ParsedVoiceCommand.PreviousTrack, VoiceCommandParser.parse("rhythm previous song"))
        assertEquals(ParsedVoiceCommand.NextTrack, VoiceCommandParser.parse("リズム 次の曲"))
        assertEquals(ParsedVoiceCommand.PreviousTrack, VoiceCommandParser.parse("节奏 上一首"))
        assertEquals(ParsedVoiceCommand.NextTrack, VoiceCommandParser.parse("ritmo sonraki şarkı"))
        assertEquals(ParsedVoiceCommand.NextTrack, VoiceCommandParser.parse("ritmo lagu berikutnya"))
        assertEquals(ParsedVoiceCommand.Pause, VoiceCommandParser.parse("ritmo pausar"))
        assertEquals(ParsedVoiceCommand.ShowLyrics, VoiceCommandParser.parse("ritmo ver letra"))

        val esQuery = VoiceCommandParser.parse("ridmo reproducir música ligera de soda stereo")
        assertTrue(esQuery is ParsedVoiceCommand.PlayQuery)
        esQuery as ParsedVoiceCommand.PlayQuery
        assertEquals("musica ligera", esQuery.songOrArtistQuery)
        assertEquals("soda stereo", esQuery.artistHint)

        val enQuery = VoiceCommandParser.parse("Ritmo play Believer by Imagine Dragons")
        assertTrue(enQuery is ParsedVoiceCommand.PlayQuery)
        enQuery as ParsedVoiceCommand.PlayQuery
        assertEquals("believer", enQuery.songOrArtistQuery)
        assertEquals("imagine dragons", enQuery.artistHint)
    }

    @Test
    fun handsFreeWakeWord_requiresWakeWordWhenEnabledAndSupportsWakeOnly() {
        assertEquals(
            ParsedVoiceCommand.WakeOnly,
            VoiceCommandParser.parse("ridmo", requireWakeWord = true)
        )
        assertEquals(
            ParsedVoiceCommand.WakeOnly,
            VoiceCommandParser.parse("Oye Ritmo", requireWakeWord = true)
        )
        assertEquals(
            ParsedVoiceCommand.NextTrack,
            VoiceCommandParser.parse("oye ridmo siguiente canción", requireWakeWord = true)
        )
        val backgroundChat = VoiceCommandParser.parse("hola que tal como estas", requireWakeWord = true)
        assertTrue(backgroundChat is ParsedVoiceCommand.Unknown)
        assertFalse(VoiceCommandParser.containsWakeWord("siguiente canción"))
        assertTrue(VoiceCommandParser.containsWakeWord("por favor ridmo"))
    }

    @Test
    fun lyricsRepository_parsesSyncedLrcAndFindsActiveLine() {
        val rawLrc = """
            [ar:Soda Stereo]
            [ti:De Música Ligera]
            [00:12.50][01:02.100] Ella durmió al calor de las masas
            [00:18.00] Y yo desperté queriendo soñarla
        """.trimIndent()

        val parsed = LyricsRepository.parseLyricsText("t1", rawLrc, "Test")
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(12_500L, parsed.lines[0].timestampMs)
        assertEquals(18_000L, parsed.lines[1].timestampMs)
        assertEquals(62_100L, parsed.lines[2].timestampMs)
        assertEquals("Ella durmió al calor de las masas", parsed.lines[0].text)

        assertEquals(-1, LyricsRepository.findActiveLineIndex(parsed.lines, 4_000L))
        assertEquals(0, LyricsRepository.findActiveLineIndex(parsed.lines, 13_000L))
        assertEquals(1, LyricsRepository.findActiveLineIndex(parsed.lines, 25_000L))
        assertEquals(2, LyricsRepository.findActiveLineIndex(parsed.lines, 65_000L))

        val plain = LyricsRepository.parseLyricsText("t2", "Primera línea\nSegunda línea", "Plain")
        assertFalse(plain.isSynced)
        assertEquals(2, plain.lines.size)
        assertEquals(-1, LyricsRepository.findActiveLineIndex(plain.lines, 10_000L))
    }

    @Test
    fun offlineVoiceMatcher_prioritizesSongAndArtistMatches() {
        val sampleLibrary = listOf(
            LocalTrack(
                id = 1L, trackKey = "t1", title = "De Música Ligera", artist = "Soda Stereo",
                album = "Canción Animal", albumId = 1L, durationMs = 210_000L, contentUri = "content://1",
                albumArtUri = null, dateAdded = 0L
            ),
            LocalTrack(
                id = 2L, trackKey = "t2", title = "Persiana Americana", artist = "Soda Stereo",
                album = "Signos", albumId = 2L, durationMs = 290_000L, contentUri = "content://2",
                albumArtUri = null, dateAdded = 0L
            ),
            LocalTrack(
                id = 3L, trackKey = "t3", title = "Believer", artist = "Imagine Dragons",
                album = "Evolve", albumId = 3L, durationMs = 204_000L, contentUri = "content://3",
                albumArtUri = null, dateAdded = 0L
            )
        )

        val exactSongArtist = VoiceCommandParser.findBestTrackQueue(
            tracks = sampleLibrary,
            songOrArtistQuery = "musica ligera",
            artistHint = "soda stereo"
        )
        assertEquals("t1", exactSongArtist.first().trackKey)

        val artistOnly = VoiceCommandParser.findBestTrackQueue(
            tracks = sampleLibrary,
            songOrArtistQuery = "soda stereo"
        )
        assertEquals(2, artistOnly.size)
        assertTrue(artistOnly.all { it.artist == "Soda Stereo" })
    }

    @Test
    fun audioClipTrimmer_keepsFiveMinuteClipWithinSixMbBudget() {
        assertEquals(300_000L, AudioClipTrimmer.MAX_TRIM_WINDOW_MS)
        assertTrue(AudioClipTrimmer.MAX_RAW_CLIP_BYTES >= 5_000_000)

        val dur15sKb = AudioClipTrimmer.estimateSizeKb(15_000L)
        val dur5MinKb = AudioClipTrimmer.estimateSizeKb(300_000L)
        val maxAllowedKb = AudioClipTrimmer.MAX_RAW_CLIP_BYTES / 1024
        assertTrue("15s clip estimate should fit: $dur15sKb KB", dur15sKb in 1..maxAllowedKb)
        assertTrue("5-min clip estimate should fit: $dur5MinKb KB", dur5MinKb in 1..maxAllowedKb)

        val msgInline = RegionalChatMessage(audioClipBase64 = "abc")
        val msgChunked = RegionalChatMessage(audioChunkCount = 5, audioSizeBytes = 3_200_000L)
        val msgTextOnly = RegionalChatMessage()
        assertTrue(msgInline.hasDownloadableAudio())
        assertTrue(msgChunked.hasDownloadableAudio())
        assertFalse(msgTextOnly.hasDownloadableAudio())
    }

    @Test
    fun voiceAudioFocusCoordinator_tracksActiveMicAndTransientDuckingIndependently() {
        VoiceAudioFocusCoordinator.setVoiceMicActive(true)
        assertTrue(VoiceAudioFocusCoordinator.isVoiceMicActive.value)
        assertFalse(VoiceAudioFocusCoordinator.isVoiceDucking.value)

        VoiceAudioFocusCoordinator.setTransientVoiceDucking(true)
        assertTrue(VoiceAudioFocusCoordinator.isVoiceDucking.value)
        assertTrue(VoiceAudioFocusCoordinator.isVoiceMicActive.value)

        VoiceAudioFocusCoordinator.setTransientVoiceDucking(false)
        assertFalse(VoiceAudioFocusCoordinator.isVoiceDucking.value)
        assertTrue(VoiceAudioFocusCoordinator.isVoiceMicActive.value)

        VoiceAudioFocusCoordinator.setVoiceMicActive(false)
        assertFalse(VoiceAudioFocusCoordinator.isVoiceMicActive.value)
        assertFalse(VoiceAudioFocusCoordinator.isVoiceDucking.value)
    }

    @Test
    fun offlineVoiceCommandParser_parsesVolumeUpVolumeDownAndResumeOverMusic() {
        assertEquals(ParsedVoiceCommand.VolumeUp, VoiceCommandParser.parse("Ritmo subir volumen"))
        assertEquals(ParsedVoiceCommand.VolumeUp, VoiceCommandParser.parse("oye ridmo sube el volumen", requireWakeWord = true))
        assertEquals(ParsedVoiceCommand.VolumeUp, VoiceCommandParser.parse("ritmo turn up the volume"))
        assertEquals(ParsedVoiceCommand.VolumeDown, VoiceCommandParser.parse("Ritmo bajar volumen"))
        assertEquals(ParsedVoiceCommand.VolumeDown, VoiceCommandParser.parse("ridmo baja la música", requireWakeWord = true))
        assertEquals(ParsedVoiceCommand.VolumeDown, VoiceCommandParser.parse("rhythm volume down"))
        assertEquals(ParsedVoiceCommand.Resume, VoiceCommandParser.parse("ritmo reanudar música", requireWakeWord = true))
    }

    @Test
    fun extraStrings_coverAllEightLanguages() {
        AppLanguage.entries.forEach { lang ->
            val extra = extraStringsFor(lang)
            assertTrue(extra.minimalistLabel.isNotBlank())
            assertTrue(extra.neonModernLabel.isNotBlank())
            assertTrue(extra.originalLabel.isNotBlank())
            assertTrue(extra.trimAudioButton.isNotBlank())
            assertTrue(extra.voiceControlTitle.isNotBlank())
            assertTrue(extra.screenOffVoiceTitle.isNotBlank())
            assertTrue(extra.studioMasterTitle.isNotBlank())
            assertTrue(extra.lyricsLabel.isNotBlank())
            assertTrue(extra.handsFreeWakeTitle.isNotBlank())
            assertTrue(extra.handsFreeActivePill.isNotBlank())
        }
    }

    @Test
    fun monoAndBalanceAudioProcessor_processesFloat32BitPcmCleanly() {
        val processor = MonoAndBalanceAudioProcessor()
        val format = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)
        processor.configure(format)
        processor.flush()

        // Stereo frame: L = 0.8f, R = 0.2f (Mono avg = 0.5f)
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).apply {
            putFloat(0.8f)
            putFloat(0.2f)
            flip()
        }
        processor.setChannelConfig(AudioChannelMode.MONO, 0f)
        processor.queueInput(input)
        val monoOut = processor.output
        assertEquals(8, monoOut.remaining())
        assertEquals(0.5f, monoOut.float, 0.001f)
        assertEquals(0.5f, monoOut.float, 0.001f)

        // Balanced right (+1.0): Left silenced, Right kept at 0.6f
        val input2 = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder()).apply {
            putFloat(0.9f)
            putFloat(0.6f)
            flip()
        }
        processor.setChannelConfig(AudioChannelMode.BALANCED, 1.0f)
        processor.queueInput(input2)
        val rightOut = processor.output
        assertEquals(0.0f, rightOut.float, 0.001f)
        assertEquals(0.6f, rightOut.float, 0.001f)
    }

    @Test
    fun v22Models_preserveOriginalStyleAndStudioModesAndAvatar() {
        assertEquals(UiStyleMode.NEON_MODERN, UiStyleMode.fromId("neon_modern"))
        assertEquals(UiStyleMode.ORIGINAL, UiStyleMode.fromId("original"))
        assertEquals(UiStyleMode.NEON_MODERN, UiStyleMode.fromId("unknown"))
        assertEquals(AppAccentPalette.NEON_VIOLET, AppAccentPalette.fromId(null))
        assertEquals(StudioAudioMode.HIFI_MASTER, StudioAudioMode.fromId(null))
        assertEquals(StudioAudioMode.ULTRA_CLARITY, StudioAudioMode.fromId("ultra_clarity"))

        val chatWithAvatar = RegionalChatMessage(senderName = "Zander", senderAvatarBase64 = "abc123")
        assertEquals("abc123", chatWithAvatar.senderAvatarBase64)
    }

    @Test
    fun appFontStyles_resolveAllSixTypographiesAndFallback() {
        assertEquals(6, com.example.model.AppFontStyle.entries.size)
        com.example.model.AppFontStyle.entries.forEach { style ->
            assertEquals(style, com.example.model.AppFontStyle.fromId(style.id))
            assertTrue(style.displayName.isNotBlank())
        }
        assertEquals(
            com.example.model.AppFontStyle.SPACE_JAKARTA,
            com.example.model.AppFontStyle.fromId("nonexistent")
        )
    }

    @Test
    fun chatPushNotifications_strictlyRespectUserPreferencesAndSelfSuppression() {
        // 1) Disabled globally by user -> suppressed
        assertFalse(
            com.example.notifications.ChatNotificationHelper.shouldDisplayChatPushNotification(
                chatPushEnabled = false,
                onlyActiveRegion = false,
                preferredRegionId = "LATAM",
                incomingRegionId = "LATAM",
                incomingSenderId = "other_user",
                currentUserId = "me_user"
            )
        )

        // 2) Message sent by self -> suppressed
        assertFalse(
            com.example.notifications.ChatNotificationHelper.shouldDisplayChatPushNotification(
                chatPushEnabled = true,
                onlyActiveRegion = false,
                preferredRegionId = "LATAM",
                incomingRegionId = "LATAM",
                incomingSenderId = "me_user",
                currentUserId = "me_user"
            )
        )

        // 3) Only active region enabled, different region arrives -> suppressed
        assertFalse(
            com.example.notifications.ChatNotificationHelper.shouldDisplayChatPushNotification(
                chatPushEnabled = true,
                onlyActiveRegion = true,
                preferredRegionId = "LATAM",
                incomingRegionId = "ESPANA",
                incomingSenderId = "other_user",
                currentUserId = "me_user"
            )
        )

        // 4) Only active region enabled, matching region -> shown
        assertTrue(
            com.example.notifications.ChatNotificationHelper.shouldDisplayChatPushNotification(
                chatPushEnabled = true,
                onlyActiveRegion = true,
                preferredRegionId = "LATAM",
                incomingRegionId = "LATAM",
                incomingSenderId = "other_user",
                currentUserId = "me_user"
            )
        )

        // 5) All regions allowed -> shown regardless of room
        assertTrue(
            com.example.notifications.ChatNotificationHelper.shouldDisplayChatPushNotification(
                chatPushEnabled = true,
                onlyActiveRegion = false,
                preferredRegionId = "LATAM",
                incomingRegionId = "ASIA",
                incomingSenderId = "other_user",
                currentUserId = "me_user"
            )
        )

        // Topic subscription mapping & stable doc id format
        assertTrue(
            com.example.notifications.ChatNotificationHelper.desiredTopicsForPrefs(
                enabled = false,
                onlyActiveRegion = true,
                activeRegion = com.example.model.ChatRegion.LATAM
            ).isEmpty()
        )
        assertEquals(
            setOf("ritmo_chat_latam"),
            com.example.notifications.ChatNotificationHelper.desiredTopicsForPrefs(
                enabled = true,
                onlyActiveRegion = true,
                activeRegion = com.example.model.ChatRegion.LATAM
            )
        )
        assertTrue(
            com.example.notifications.ChatNotificationHelper.desiredTopicsForPrefs(
                enabled = true,
                onlyActiveRegion = false,
                activeRegion = com.example.model.ChatRegion.LATAM
            ).contains(com.example.notifications.ChatNotificationHelper.TOPIC_ALL_CHAT)
        )
        val tokenId = com.example.notifications.ChatNotificationHelper.stableFcmTokenId("sample-fcm-token")
        assertTrue(tokenId.matches(Regex("^[a-zA-Z0-9_\\-]+$")))
    }

    @Test
    fun voiceAudioRouteDetector_distinguishesSpeakerVsBluetoothOutputs() {
        assertEquals(
            com.example.voice.VoiceAudioRoute.PHONE_SPEAKER,
            com.example.voice.VoiceAudioRouteDetector.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
                legacyBluetoothOn = false
            )
        )
        assertEquals(
            com.example.voice.VoiceAudioRoute.BLUETOOTH_HEADSET,
            com.example.voice.VoiceAudioRouteDetector.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP),
                legacyBluetoothOn = false
            )
        )
        assertEquals(
            com.example.voice.VoiceAudioRoute.BLUETOOTH_HEADSET,
            com.example.voice.VoiceAudioRouteDetector.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO),
                legacyBluetoothOn = false
            )
        )
        assertEquals(
            com.example.voice.VoiceAudioRoute.BLUETOOTH_HEADSET,
            com.example.voice.VoiceAudioRouteDetector.detectFromDeviceTypes(
                intArrayOf(),
                legacyBluetoothOn = true
            )
        )
    }

    @Test
    fun voiceCaptureEngine_highPassFilterAttenuatesLowRumbleAndPreservesVoiceBand() {
        // Generate constant DC + low rumble vs alternating 2 kHz speech-band PCM
        val rumble = ByteArray(128)
        for (i in 0 until 64) {
            val s = 4000.toShort()
            rumble[i * 2] = (s.toInt() and 0xFF).toByte()
            rumble[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }
        com.example.voice.VoiceAudioCaptureEngine.applySpeakerBassHighPass(rumble, rumble.size)
        val lastSample = ((rumble[rumble.size - 1].toInt() shl 8) or (rumble[rumble.size - 2].toInt() and 0xFF)).toShort()
        assertTrue("DC/low-frequency bass rumble should decay strongly (was $lastSample)", kotlin.math.abs(lastSample.toInt()) < 450)

        // Rapid alternating sign signal (~8 kHz) passes cleanly with minimal loss
        val speech = ByteArray(64)
        for (i in 0 until 32) {
            val s = if (i % 2 == 0) 3000.toShort() else (-3000).toShort()
            speech[i * 2] = (s.toInt() and 0xFF).toByte()
            speech[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
        }
        com.example.voice.VoiceAudioCaptureEngine.applySpeakerBassHighPass(speech, speech.size)
        val peakPreserved = ((speech[21].toInt() shl 8) or (speech[20].toInt() and 0xFF)).toShort()
        assertTrue("Speech-band transitions should be preserved (was $peakPreserved)", kotlin.math.abs(peakPreserved.toInt()) > 2500)
    }

    @Test
    fun sleepTimerEngine_supportsMultiSongCountdownAndGradualFadeToggle() {
        var expiredCalls = 0
        var fakeClockMs = 1000L
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined)
        val engine = com.example.player.SleepTimerEngine(
            scope = scope,
            elapsedRealtimeMs = { fakeClockMs },
            nowWallClockMs = { fakeClockMs },
            onExpired = { expiredCalls++ }
        )

        assertTrue(engine.state.value.fadeOutEnabled)
        engine.setFadeOutEnabled(false)
        assertFalse(engine.state.value.fadeOutEnabled)

        engine.startSongCountdown(3)
        assertTrue(engine.state.value.isActive)
        assertEquals(3, engine.state.value.remainingSongsToStop)
        assertEquals(3, engine.state.value.targetSongsTotal)
        assertFalse(engine.state.value.stopAfterCurrentTrack)
        assertFalse(engine.state.value.fadeOutEnabled)

        // First track finishes -> 2 remaining
        assertFalse(engine.consumeStopAfterCurrentTrack())
        assertEquals(2, engine.state.value.remainingSongsToStop)
        assertTrue(engine.state.value.isActive)

        // Second track finishes -> 1 remaining (stopAfterCurrentTrack = true)
        assertFalse(engine.consumeStopAfterCurrentTrack())
        assertEquals(1, engine.state.value.remainingSongsToStop)
        assertTrue(engine.state.value.stopAfterCurrentTrack)

        // Third track finishes -> expires now!
        assertTrue(engine.consumeStopAfterCurrentTrack())
        assertFalse(engine.state.value.isActive)
        assertEquals(0, engine.state.value.remainingSongsToStop)
        assertFalse(engine.state.value.fadeOutEnabled)
    }

    @Test
    fun audioEffectsController_appliesCustomBandsAndBoostForSavedProfiles() {
        val stored = com.example.player.AudioEffectsController.applyCustomBandsAndBoost(
            bandLevelsMb = listOf(450, 200, -100, 300, 500),
            bassBoostStrength = 640,
            spatial3dEnabled = true
        )
        assertTrue(stored.enabled)
        assertEquals(listOf(450, 200, -100, 300, 500), stored.bandLevelsMb)
        assertEquals(640, stored.bassBoostStrength)
        assertTrue(stored.spatial3dEnabled)
    }

    @Test
    fun crossfadeCurves_computeEqualPowerSmoothSAndLinearBoundsCorrectly() {
        val (outStartEq, inStartEq) = com.example.player.computeCrossfadeGains(0f, com.example.model.CrossfadeCurve.EQUAL_POWER)
        assertEquals(1f, outStartEq, 0.01f)
        assertEquals(0f, inStartEq, 0.01f)

        val (outMidEq, inMidEq) = com.example.player.computeCrossfadeGains(0.5f, com.example.model.CrossfadeCurve.EQUAL_POWER)
        // Equal power at 0.5 satisfies out^2 + in^2 ~= 1
        assertEquals(1f, outMidEq * outMidEq + inMidEq * inMidEq, 0.02f)

        val (outEndLin, inEndLin) = com.example.player.computeCrossfadeGains(1f, com.example.model.CrossfadeCurve.LINEAR)
        assertEquals(0f, outEndLin, 0.01f)
        assertEquals(1f, inEndLin, 0.01f)

        val (outMidS, inMidS) = com.example.player.computeCrossfadeGains(0.5f, com.example.model.CrossfadeCurve.SMOOTH_S)
        assertEquals(0.5f, outMidS, 0.01f)
        assertEquals(0.5f, inMidS, 0.01f)
    }

    @Test
    fun smartDjAnalysis_computesSafeOutroTriggerRespectingSkipSilenceAndFloor() {
        val analysis = com.example.player.AudioTrackAnalysis(
            trackKey = "song1|artist|200",
            durationMs = 200_000L,
            introSilenceEndMs = 1_500L,
            outroSilenceStartMs = 192_000L,
            estimatedBpm = 122,
            energyScore = 0.78f,
            recommendedCrossfadeMs = 6_000L
        )
        // With skipSilence = true, triggers crossfade 6s before outroSilenceStartMs (186_000ms)
        assertEquals(186_000L, analysis.safeOutroTriggerPositionMs(6_000L, skipSilence = true))
        // With skipSilence = false, triggers crossfade 6s before total duration (194_000ms)
        assertEquals(194_000L, analysis.safeOutroTriggerPositionMs(6_000L, skipSilence = false))
    }

    @Test
    fun smartQueueBuilder_disperseArtistsAndDeduplicatesPreservingCurrentTrack() {
        val t0 = LocalTrack(1L, "k1", "A1", "ArtistOne", "Alb", 1L, 180_000L, "u1", null, 0L)
        val t1 = LocalTrack(2L, "k2", "A2", "ArtistOne", "Alb", 1L, 180_000L, "u2", null, 0L)
        val t2 = LocalTrack(3L, "k3", "B1", "ArtistTwo", "Alb", 2L, 180_000L, "u3", null, 0L)
        val t3 = LocalTrack(4L, "k4", "C1", "ArtistThree", "Alb", 3L, 180_000L, "u4", null, 0L)
        val tDup = LocalTrack(5L, "k2", "A2", "ArtistOne", "Alb", 1L, 180_000L, "u2", null, 0L)

        val smart = com.example.player.PlaybackEngineBus.buildSmartQueue(
            queue = listOf(t0, t1, t2, t3, tDup),
            currentIndex = 0,
            avoidRepeats = true,
            seed = 42L
        )
        // Current track preserved at 0, duplicate k2 stripped, first upcoming is not ArtistOne if alternatives exist
        assertEquals("k1", smart.first().trackKey)
        assertEquals(4, smart.size)
        assertFalse(smart[1].artist.equals("ArtistOne", ignoreCase = true))

        val (deduped, newActiveIdx) = com.example.player.PlaybackEngineBus.deduplicateQueuePreservingCurrent(
            queue = listOf(t0, tDup, t1, t2),
            currentIndex = 2
        )
        assertEquals(3, deduped.size)
        assertEquals("k2", deduped[newActiveIdx].trackKey)
    }

    @Test
    fun realtimeAudioAnalysisProcessor_computesSpectralBandsFromSineWave() {
        val count = 128
        val samples = FloatArray(count) { i ->
            kotlin.math.sin(2.0 * kotlin.math.PI * 3.0 * i / count).toFloat() * 0.8f
        }
        val smoothed = FloatArray(com.example.player.RealtimeAudioAnalysisProcessor.BAND_COUNT) { 0.08f }
        val result = com.example.player.RealtimeAudioAnalysisProcessor.computeSpectralSnapshot(
            samples = samples,
            count = count,
            smoothed = smoothed,
            prevBassAvg = 0.10f,
            lpSeed = 0f,
            nowMs = 5000L
        )
        assertTrue(result.snapshot.isLiveAudio)
        assertEquals(20, result.snapshot.bands.size)
        assertTrue(result.snapshot.overallRms > 0.3f)
        assertTrue(result.snapshot.bassEnergy > 0.1f)
    }

    @Test
    fun audioEffectsController_includesElevenPresetsTrebleToneAndDeviceProfileMapping() {
        assertEquals(11, com.example.player.DEFAULT_EQ_PRESETS.size)
        assertEquals("Hip-Hop", com.example.player.DEFAULT_EQ_PRESETS[8].name)
        assertEquals("Acústico", com.example.player.DEFAULT_EQ_PRESETS[9].name)
        assertEquals("Latino", com.example.player.DEFAULT_EQ_PRESETS[10].name)

        val latino = com.example.player.AudioEffectsController.applyPreset(10)
        assertEquals(10, latino.presetIndex)
        assertEquals(450, latino.bassBoostStrength)
        assertEquals(250, latino.trebleBoostMb)

        val trebleBoosted = com.example.player.AudioEffectsController.updateTrebleBoost(600)
        assertEquals(600, trebleBoosted.trebleBoostMb)
        assertEquals(0, com.example.player.AudioEffectsController.trebleOffsetForBand(0, 5, 600))
        assertEquals(270, com.example.player.AudioEffectsController.trebleOffsetForBand(3, 5, 600))
        assertEquals(600, com.example.player.AudioEffectsController.trebleOffsetForBand(4, 5, 600))

        val btProfile = com.example.player.AudioEffectsController.buildDeviceSoundProfile(
            com.example.model.AudioDeviceProfileType.BLUETOOTH
        )
        assertEquals(com.example.model.AudioDeviceProfileType.BLUETOOTH, btProfile.deviceType)
        assertEquals(600, btProfile.trebleBoostMb)
    }

    @Test
    fun deviceAudioProfileManager_classifiesAllAudioOutputRoutesAccurately() {
        assertEquals(
            com.example.model.AudioDeviceProfileType.PHONE_SPEAKER,
            com.example.player.DeviceAudioProfileManager.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
            )
        )
        assertEquals(
            com.example.model.AudioDeviceProfileType.WIRED_HEADSET,
            com.example.player.DeviceAudioProfileManager.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
            )
        )
        assertEquals(
            com.example.model.AudioDeviceProfileType.BLUETOOTH,
            com.example.player.DeviceAudioProfileManager.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET, android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
            )
        )
        assertEquals(
            com.example.model.AudioDeviceProfileType.OTHER_OUTPUT,
            com.example.player.DeviceAudioProfileManager.detectFromDeviceTypes(
                intArrayOf(android.media.AudioDeviceInfo.TYPE_HDMI)
            )
        )
    }

    @Test
    fun homeLayoutConfig_movesSectionsTogglesVisibilityAndShortcutsSafely() {
        var config = com.example.model.HomeLayoutConfig.defaultConfig()
        assertEquals(com.example.model.HomeSectionId.defaultOrder, config.orderedSections)

        // Move TRACK_LIST up by 1 (from index 5 to index 4 with 6 default sections including LIVE_STATS)
        config = config.moveSection(com.example.model.HomeSectionId.TRACK_LIST, -1)
        assertEquals(com.example.model.HomeSectionId.TRACK_LIST, config.orderedSections[4])

        // Boundary move does nothing
        val same = config.moveSection(com.example.model.HomeSectionId.QUICK_TABS, -1)
        assertEquals(config.orderedSections, same.orderedSections)

        // Toggle visibility: never hides every single section
        var hideTest = com.example.model.HomeLayoutConfig.defaultConfig()
        com.example.model.HomeSectionId.entries.forEach { sec ->
            if (sec !in hideTest.hiddenSections) {
                hideTest = hideTest.toggleSectionVisibility(sec)
            }
        }
        assertTrue(
            "At least one section must remain visible",
            com.example.model.HomeSectionId.entries.any { it !in hideTest.hiddenSections }
        )

        // Toggle pinned shortcut & v2.0 LIVE_STATS flags
        val toggled = config.togglePinnedShortcut(com.example.model.HomeShortcutType.STATS)
        assertFalse(com.example.model.HomeShortcutType.STATS in toggled.pinnedShortcuts)
        val liveAdjusted = config.copy(showTodayTimeStat = false, showRecapBannerCard = false)
        assertFalse(liveAdjusted.showTodayTimeStat)
        assertFalse(liveAdjusted.showRecapBannerCard)
        assertTrue(liveAdjusted.showTodayPlaysStat)
    }

    @Test
    fun typographyFor_appliesClampedFontSizeScaleAndResolvesPlayerStyleEnums() {
        val base = com.example.ui.theme.typographyFor(com.example.model.AppFontStyle.SPACE_JAKARTA, 1.0f)
        val scaledUp = com.example.ui.theme.typographyFor(com.example.model.AppFontStyle.SPACE_JAKARTA, 2.0f) // clamped to 1.25f
        val scaledDown = com.example.ui.theme.typographyFor(com.example.model.AppFontStyle.SPACE_JAKARTA, 0.2f) // clamped to 0.85f

        assertEquals(base.bodyLarge.fontSize.value * 1.25f, scaledUp.bodyLarge.fontSize.value, 0.05f)
        assertEquals(base.bodyLarge.fontSize.value * 0.85f, scaledDown.bodyLarge.fontSize.value, 0.05f)

        assertEquals(com.example.model.PlayerControlStyle.CLASSIC_STUDIO, com.example.model.PlayerControlStyle.fromId("classic_studio"))
        assertEquals(com.example.model.PlayerControlStyle.PILL, com.example.model.PlayerControlStyle.fromId("invalid"))
        assertEquals(com.example.model.PlayerLayoutMode.IMMERSIVE, com.example.model.PlayerLayoutMode.fromId("immersive"))
        assertEquals(com.example.model.PlayerLayoutMode.STANDARD, com.example.model.PlayerLayoutMode.fromId("invalid"))
    }

    @Test
    fun statsAnalyticsEngine_qualifiedPlayRulesAndRealTimeSummaryAreAccurate() {
        val engine = com.example.data.stats.StatsAnalyticsEngine
        assertEquals(15L, engine.requiredSecondsForQualifiedPlay(0L))
        assertEquals(8L, engine.requiredSecondsForQualifiedPlay(15_000L))
        assertEquals(30L, engine.requiredSecondsForQualifiedPlay(240_000L))

        val nowMs = 1_775_044_800_000L // fixed timestamp
        val todayKey = engine.buildDateKey(nowMs)
        val monthKey = engine.buildMonthKey(nowMs)
        val year = engine.yearFromTimestamp(nowMs)
        val events = listOf(
            com.example.data.stats.ListeningEventEntity(
                trackKey = "k1", trackTitle = "Luz", artistName = "Soda", albumName = "Signos", albumId = 1L,
                genre = "Rock", durationMs = 200_000L, listenedSeconds = 180L, isCompletedPlay = true,
                timestampMs = nowMs, dateKey = todayKey, monthKey = monthKey, year = year, hourOfDay = 21, timeSlot = "NIGHT"
            ),
            com.example.data.stats.ListeningEventEntity(
                trackKey = "k2", trackTitle = "Brisa", artistName = "Nadia", albumName = "Alba", albumId = 2L,
                genre = "Indie", durationMs = 190_000L, listenedSeconds = 3L, isCompletedPlay = false,
                timestampMs = nowMs - 2_000L, dateKey = todayKey, monthKey = monthKey, year = year, hourOfDay = 21, timeSlot = "NIGHT"
            )
        )
        val realTime = engine.computeRealTimeSummary(events, nowMs)
        assertEquals(183L, realTime.todaySeconds)
        assertEquals(1L, realTime.todayPlays)
        assertEquals(2, realTime.todayUniqueArtists)
        assertEquals(7, realTime.weeklyDailyBars.size)
        assertEquals(183L, realTime.allTimeSeconds)
    }

    @Test
    fun statsAnalyticsEngine_computesRecapWrappedHistoryAndHealthyAchievements() {
        val engine = com.example.data.stats.StatsAnalyticsEngine
        val tsFeb = 1_739_534_400_000L // 2025-02-14
        val tsMar = 1_742_040_000_000L // 2025-03-15
        val febMk = engine.buildMonthKey(tsFeb)
        val marMk = engine.buildMonthKey(tsMar)
        val events = listOf(
            com.example.data.stats.ListeningEventEntity(
                trackKey = "k1", trackTitle = "Aurora", artistName = "Viento", albumName = "Norte", albumId = 10L,
                genre = "Synthwave", durationMs = 220_000L, listenedSeconds = 100L, isCompletedPlay = true,
                timestampMs = tsFeb, dateKey = engine.buildDateKey(tsFeb), monthKey = febMk,
                year = 2025, hourOfDay = 10, timeSlot = "MORNING"
            ),
            com.example.data.stats.ListeningEventEntity(
                trackKey = "k2", trackTitle = "Eclipse", artistName = "Sol", albumName = "Cielo", albumId = 20L,
                genre = "Rock", durationMs = 210_000L, listenedSeconds = 200L, isCompletedPlay = true,
                timestampMs = tsMar, dateKey = engine.buildDateKey(tsMar), monthKey = marMk,
                year = 2025, hourOfDay = 22, timeSlot = "NIGHT"
            )
        )
        val emptyRecap = engine.computeRecapReport(emptyList(), marMk)
        assertFalse(emptyRecap.hasEnoughData)

        val marRecap = engine.computeRecapReport(events, marMk)
        assertTrue(marRecap.hasEnoughData)
        assertEquals("Eclipse", marRecap.topSong?.trackTitle)
        assertEquals("Sol", marRecap.topArtist?.artistName)
        assertTrue("Sol should be detected as new discovery in March", "Sol" in marRecap.newlyDiscoveredArtists)
        assertTrue(marRecap.comparisonWithPreviousMonth.hasPriorPeriodData)
        assertEquals(100, marRecap.comparisonWithPreviousMonth.deltaPercentSeconds)

        val wrapped = engine.computeWrappedReport(events, 2025)
        assertTrue(wrapped.hasEnoughData)
        assertEquals(12, wrapped.monthlyEvolution12.size)
        assertEquals(marMk, wrapped.peakMonthKey)
        assertEquals(300L, wrapped.totalSeconds)

        val forgottenThresholdNow = tsFeb + 30L * 24L * 60L * 60L * 1000L
        val trackStats = listOf(
            com.example.data.stats.TrackStatsEntity(
                trackKey = "k1", trackTitle = "Aurora", artistName = "Viento", albumName = "Norte", albumId = 10L,
                genre = "Synthwave", firstPlayedAtMs = tsFeb, lastPlayedAtMs = tsFeb, totalSeconds = 250L, qualifiedPlays = 3L
            )
        )
        val history = engine.computeHistoryReport(events, trackStats, marMk, 2025, nowMs = forgottenThresholdNow)
        assertTrue(history.hasEnoughHistory)
        assertTrue(history.forgottenTracks.any { it.trackKey == "k1" && it.daysSinceLastPlay >= 21 })
        assertTrue(history.milestones.isNotEmpty())
        assertTrue(history.loyalArtists.isNotEmpty())

        val achievements = engine.computeAchievements(events, trackStats, favoriteCount = 3, customPlaylistCount = 1)
        assertEquals(8, achievements.size)
        assertTrue(achievements.first { it.id == "first_note" }.isUnlocked)
        assertTrue(achievements.all { it.currentValue <= it.targetValue })
    }

    @Test
    fun libraryOrganizerEngine_findsDuplicatesMissingCoversGenresAndFiltersCorrectly() {
        val tracks = listOf(
            LocalTrack(1L, "k1", "Música Ligera", "Soda Stereo", "Canción Animal", 1L, 210_000L, "u1", "art://1", 100L, formatLabel = "FLAC", genre = "Rock"),
            LocalTrack(2L, "k2", "Musica Ligera (Live)", "Soda Stereo", "El Último Concierto", 2L, 230_000L, "u2", null, 200L, formatLabel = "MP3", genre = "Rock"),
            LocalTrack(3L, "k3", "Piano Nocturno", "Clara", "Estudios", 3L, 180_000L, "u3", null, 300L, formatLabel = "WAV", genre = "Clásica")
        )
        val duplicates = com.example.library.LibraryOrganizerEngine.findDuplicateGroups(tracks)
        assertEquals(1, duplicates.size)
        assertEquals(2, duplicates.first().tracks.size)

        val missingCovers = com.example.library.LibraryOrganizerEngine.findTracksMissingCover(tracks)
        assertEquals(2, missingCovers.size)

        val genres = com.example.library.LibraryOrganizerEngine.buildGenreGroups(tracks)
        assertEquals("Rock", genres.first().genreName)
        assertEquals(2, genres.first().tracks.size)

        val filtered = com.example.library.LibraryOrganizerEngine.filterAndSort(
            tracks = tracks,
            criteria = com.example.library.LibraryFilterCriteria(
                selectedGenre = "Clásica",
                onlyMissingArtwork = true,
                sortOption = com.example.library.LibrarySortOption.DURATION_DESC
            )
        )
        assertEquals(1, filtered.size)
        assertEquals("k3", filtered.first().trackKey)
    }

    @Test
    fun ambientMixGenerator_producesAllTenSmartCategoriesAndDispersesArtists() {
        val tracks = listOf(
            LocalTrack(1L, "k1", "Morning Sun", "ArtA", "Alb1", 1L, 180_000L, "u1", null, 500L, genre = "Pop"),
            LocalTrack(2L, "k2", "Midnight Jazz", "ArtA", "Alb2", 2L, 260_000L, "u2", null, 400L, genre = "Jazz"),
            LocalTrack(3L, "k3", "Gym Power", "ArtB", "Alb3", 3L, 160_000L, "u3", null, 300L, genre = "Rock"),
            LocalTrack(4L, "k4", "Soft Piano Focus", "ArtC", "Alb4", 4L, 240_000L, "u4", null, 200L, genre = "Ambient")
        )
        val allMixes = com.example.model.AmbientMixGenerator.generateAllCategories(
            tracks = tracks,
            favoriteTrackKeys = setOf("k1", "k3"),
            mostPlayedKeys = listOf("k2"),
            seed = 77L
        )
        assertEquals(10, allMixes.size)
        allMixes.values.forEach { mix ->
            assertTrue("Mix ${mix.category.name} should not be empty", mix.tracks.isNotEmpty())
        }
        val favMix = allMixes[com.example.model.AmbientMoodCategory.FAVORITES_SMART]!!
        assertTrue(favMix.tracks.all { it.trackKey in setOf("k1", "k3") })
    }

    @Test
    fun extremePersonalizationModels_clampNormalizeAndProtectCoreControls() {
        val normalizedExtreme = com.example.model.ExtremeVisualStyleConfig(
            cardCornerDp = 99f,
            cardBorderWidthDp = -5f,
            surfaceTransparencyAlpha = 0.1f,
            shadowElevationDp = 40f,
            customGradientEnabled = true,
            gradientAngleDeg = 450f,
            animationSpeedScale = 3.0f,
            buttonCornerPercent = 2,
            iconStyleMode = com.example.model.IconVisualMode.OUTLINED,
            miniPlayerBarStyle = com.example.model.MiniPlayerBarStyle.DOCKED_GLASS
        ).normalized()
        assertEquals(30f, normalizedExtreme.cardCornerDp, 0.01f)
        assertEquals(0f, normalizedExtreme.cardBorderWidthDp, 0.01f)
        assertEquals(0.60f, normalizedExtreme.surfaceTransparencyAlpha, 0.01f)
        assertEquals(14f, normalizedExtreme.shadowElevationDp, 0.01f)
        assertEquals(1.8f, normalizedExtreme.animationSpeedScale, 0.01f)
        assertEquals(8, normalizedExtreme.buttonCornerPercent)
        assertEquals(com.example.model.IconVisualMode.OUTLINED, normalizedExtreme.iconStyleMode)
        assertEquals(com.example.model.MiniPlayerBarStyle.DOCKED_GLASS, normalizedExtreme.miniPlayerBarStyle)

        var designer = com.example.model.PlayerDesignerConfig.defaultConfig()
        designer = designer.toggleActionVisibility(com.example.model.PlayerQuickAction.PLAY_PAUSE)
        assertFalse(
            "PLAY_PAUSE must never be hidden",
            com.example.model.PlayerQuickAction.PLAY_PAUSE in designer.hiddenActions
        )
        designer = designer.toggleActionVisibility(com.example.model.PlayerQuickAction.SHUFFLE)
            .movePrimaryControl(com.example.model.PlayerQuickAction.NEXT, -1)
            .copy(
                artworkPosition = com.example.model.ArtworkPositionMode.COMPACT_LEFT,
                progressBarStyle = com.example.model.ProgressBarVisualStyle.WAVEFORM_BAR,
                immersiveEffectsEnabled = false
            ).normalized()
        assertEquals(com.example.model.ArtworkPositionMode.COMPACT_LEFT, designer.artworkPosition)
        assertEquals(com.example.model.ProgressBarVisualStyle.WAVEFORM_BAR, designer.progressBarStyle)
        assertTrue(com.example.model.PlayerQuickAction.SHUFFLE in designer.hiddenActions)
        assertFalse(designer.immersiveEffectsEnabled)

        val profile = com.example.model.MusicalProfileConfig.defaultConfig()
        assertTrue(profile.privateByDefault)
        assertFalse(profile.statsCloudSyncEnabled)
    }

    @Test
    fun songRecognitionSecurity_rejectsClientSecretsAndRequiresHttpsProxyUrl() {
        val buildConfigFields = BuildConfig::class.java.declaredFields.map { it.name }.toSet()
        assertFalse("AUDD_API_KEY must never be compiled into BuildConfig", "AUDD_API_KEY" in buildConfigFields)
        assertFalse("GEMINI_API_KEY must never be compiled into BuildConfig", "GEMINI_API_KEY" in buildConfigFields)
        assertTrue("SONG_RECOGNITION_PROXY_URL should be present as public config", "SONG_RECOGNITION_PROXY_URL" in buildConfigFields)

        val state = com.example.recognition.SongRecognitionUiState()
        assertFalse(state.acousticProxyConfigured)

        assertTrue(
            com.example.recognition.SongRecognitionUiState.isValidHttpsProxyUrl(
                "https://us-central1-ritmo-local.cloudfunctions.net/recognizeSongProxy"
            )
        )
        assertFalse(com.example.recognition.SongRecognitionUiState.isValidHttpsProxyUrl(""))
        assertFalse(com.example.recognition.SongRecognitionUiState.isValidHttpsProxyUrl("   "))
        assertFalse(
            com.example.recognition.SongRecognitionUiState.isValidHttpsProxyUrl(
                "http://insecure.example.com/proxy"
            )
        )
        assertFalse(
            com.example.recognition.SongRecognitionUiState.isValidHttpsProxyUrl(
                "https://invalid host with spaces.example.com"
            )
        )
    }
}
