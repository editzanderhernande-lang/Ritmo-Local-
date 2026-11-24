package com.example.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioClipTrimmer
import com.example.data.CloudSyncRepository
import com.example.data.CoverArtRepository
import com.example.data.LocalMusicRepository
import com.example.data.LyricsRepository
import com.example.data.MusicBackupManager
import com.example.data.Ritmo2PreferencesRepository
import com.example.data.TrackLyrics
import com.example.data.UserPreferencesRepository
import com.example.data.stats.ListeningEventEntity
import com.example.data.stats.MusicalAchievement
import com.example.data.stats.MusicalHistoryReport
import com.example.data.stats.RealTimeStatsSummary
import com.example.data.stats.RitmoRecapReport
import com.example.data.stats.RitmoWrappedReport
import com.example.data.stats.StatsAnalyticsEngine
import com.example.data.stats.StatsRepository
import com.example.data.stats.TrackMetadataOverrideEntity
import com.example.data.stats.TrackStatsEntity
import com.example.diagnostics.AppDiagnosticsManager
import com.example.diagnostics.DiagnosticAuditInput
import com.example.diagnostics.DiagnosticIssue
import com.example.diagnostics.DiagnosticModule
import com.example.diagnostics.DiagnosticRepairCallbacks
import com.example.diagnostics.DiagnosticSeverity
import com.example.diagnostics.DiagnosticsUiState
import com.example.diagnostics.RepairActionId
import com.example.model.AlbumPlaylist
import com.example.model.AlbumStatSummary
import com.example.model.AmbientMixGenerator
import com.example.model.AmbientMoodCategory
import com.example.model.AppFontStyle
import com.example.model.AppLanguage
import com.example.model.AppSettingsPrefs
import com.example.model.ArtistPlaylist
import com.example.model.ArtistStatSummary
import com.example.model.AudioChannelMode
import com.example.model.AudioDeviceProfileType
import com.example.model.ChatRegion
import com.example.model.CustomPlaylist
import com.example.model.CustomThemeConfig
import com.example.model.DeviceSoundProfile
import com.example.model.ExtremeVisualStyleConfig
import com.example.model.FavoriteTrack
import com.example.model.GeneratedAmbientMix
import com.example.model.GeneratedPlaylist
import com.example.model.HearingProfile
import com.example.model.HomeLayoutConfig
import com.example.model.HomeLayoutPreset
import com.example.model.LocalTrack
import com.example.model.MonthlyStat
import com.example.model.MonthlyTrackStat
import com.example.model.MusicalProfileConfig
import com.example.model.PlaybackEnginePrefs
import com.example.model.PlaybackProgress
import com.example.model.PlayerBackgroundMode
import com.example.model.PlayerControlStyle
import com.example.model.PlayerDesignerConfig
import com.example.model.PlayerLayoutMode
import com.example.model.PlayerVisualPrefs
import com.example.model.PlayerVisualStyle
import com.example.model.RegionalChatMessage
import com.example.model.SavedAmbientMix
import com.example.model.SavedCustomEqProfile
import com.example.model.SavedQueuePreset
import com.example.model.Spatial3dPreset
import com.example.model.StudioAudioMode
import com.example.model.TimeOfDaySlot
import com.example.model.UiState
import com.example.model.UiStyleMode
import com.example.model.VisualizerStyle
import com.example.model.currentMonthKey
import com.example.notifications.ChatNotificationHelper
import com.example.notifications.PlaylistReminderScheduler
import com.example.player.AudioEffectsController
import com.example.player.DeviceAudioProfileManager
import com.example.player.EqualizerUiState
import com.example.player.PlaybackControllerManager
import com.example.player.PlaybackEngineBus
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerController
import com.example.player.SleepTimerUiState
import com.example.player.SmartDjRuntimeState
import com.example.voice.OfflineVoiceRecognizer
import com.example.voice.ParsedVoiceCommand
import com.example.voice.VoiceCommandParser
import com.example.voice.VoiceUiState
import com.example.voice.VoiceWakeService
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class MonthlyReportData(
    val selectedMonthKey: String,
    val availableMonths: List<String>,
    val summary: MonthlyStat?,
    val topSongs: List<MonthlyTrackStat>,
    val topArtists: List<ArtistStatSummary>,
    val topAlbums: List<AlbumStatSummary>
)

data class MostPlayedItem(
    val track: LocalTrack,
    val playCount: Long,
    val totalSeconds: Long
)

data class CloudStatusUi(
    val isSyncing: Boolean = false,
    val lastSyncedAtMs: Long? = null,
    val statusMessage: String? = null,
    val isError: Boolean = false
)

data class ChatAttachmentDraft(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatAttachmentDraft) return false
        return fileName == other.fileName && mimeType == other.mimeType && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MusicViewModel(
    private val currentUserId: String?,
    private val localMusicRepository: LocalMusicRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val coverArtRepository: CoverArtRepository,
    private val appContext: Context
) : ViewModel() {

    private val _localTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    val localTracks: StateFlow<List<LocalTrack>> = _localTracks.asStateFlow()

    private val _isScanningLibrary = MutableStateFlow(false)
    val isScanningLibrary: StateFlow<Boolean> = _isScanningLibrary.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSlot = MutableStateFlow(TimeOfDaySlot.current())
    val selectedSlot: StateFlow<TimeOfDaySlot> = _selectedSlot.asStateFlow()

    private val _playlistsBySlot = MutableStateFlow<Map<TimeOfDaySlot, GeneratedPlaylist>>(emptyMap())
    val playlistsBySlot: StateFlow<Map<TimeOfDaySlot, GeneratedPlaylist>> = _playlistsBySlot.asStateFlow()

    private val _selectedMonthKey = MutableStateFlow(currentMonthKey())
    private val _pendingSeconds = MutableStateFlow(0L)
    private val _sessionSeconds = MutableStateFlow(0L)
    val sessionSeconds: StateFlow<Long> = _sessionSeconds.asStateFlow()

    private val _cloudStatus = MutableStateFlow(CloudStatusUi())
    val cloudStatus: StateFlow<CloudStatusUi> = _cloudStatus.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _isBusyBackup = MutableStateFlow(false)
    val isBusyBackup: StateFlow<Boolean> = _isBusyBackup.asStateFlow()

    private val _selectedChatRegion = MutableStateFlow(ChatRegion.LATAM)
    val selectedChatRegion: StateFlow<ChatRegion> = _selectedChatRegion.asStateFlow()

    private val _pendingChatAttachment = MutableStateFlow<ChatAttachmentDraft?>(null)
    val pendingChatAttachment: StateFlow<ChatAttachmentDraft?> = _pendingChatAttachment.asStateFlow()

    private val _chatStatusMessage = MutableStateFlow<String?>(null)
    val chatStatusMessage: StateFlow<String?> = _chatStatusMessage.asStateFlow()

    private val _isTrimmingChatAudio = MutableStateFlow(false)
    val isTrimmingChatAudio: StateFlow<Boolean> = _isTrimmingChatAudio.asStateFlow()

    private val _actionToastMessage = MutableStateFlow<String?>(null)
    val actionToastMessage: StateFlow<String?> = _actionToastMessage.asStateFlow()

    private val lyricsRepository = LyricsRepository(appContext)
    private val _lyricsByTrackKey = MutableStateFlow<Map<String, TrackLyrics>>(emptyMap())
    val lyricsByTrackKey: StateFlow<Map<String, TrackLyrics>> = _lyricsByTrackKey.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    private val _lyricsStatusMessage = MutableStateFlow<String?>(null)
    val lyricsStatusMessage: StateFlow<String?> = _lyricsStatusMessage.asStateFlow()

    private val _openLyricsTrackEvent = MutableStateFlow<LocalTrack?>(null)
    val openLyricsTrackEvent: StateFlow<LocalTrack?> = _openLyricsTrackEvent.asStateFlow()

    val diagnosticsState: StateFlow<DiagnosticsUiState> = AppDiagnosticsManager.uiState

    private val voiceRecognizer = OfflineVoiceRecognizer(
        context = appContext,
        onRequestPauseForVoice = { playbackManager.pauseForVoice() },
        onRequestResumeAfterVoice = { playbackManager.resumeFromVoice() },
        onHypothesesReady = { hypotheses, requireWake ->
            handleVoiceHypotheses(hypotheses, requireWake)
        }
    )
    val voiceState: StateFlow<VoiceUiState> = voiceRecognizer.state

    val equalizerState: StateFlow<EqualizerUiState> = AudioEffectsController.state
    val activeAudioSessionId: StateFlow<Int> = AudioEffectsController.audioSessionId
    val sleepTimerState: StateFlow<SleepTimerUiState> = SleepTimerController.state

    val deviceSoundProfiles: StateFlow<Map<AudioDeviceProfileType, DeviceSoundProfile>> =
        preferencesRepository.deviceSoundProfiles
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val deviceAudioProfileManager = DeviceAudioProfileManager(appContext) { newDevice ->
        handleOutputAudioDeviceChanged(newDevice)
    }
    val activeAudioDeviceType: StateFlow<AudioDeviceProfileType> = deviceAudioProfileManager.activeDevice

    val appSettings: StateFlow<AppSettingsPrefs> = preferencesRepository.appSettingsPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettingsPrefs())

    val playerVisualPrefs: StateFlow<PlayerVisualPrefs> = preferencesRepository.playerVisualPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerVisualPrefs())

    val customThemes: StateFlow<List<CustomThemeConfig>> = preferencesRepository.customThemes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val homeLayoutConfig: StateFlow<HomeLayoutConfig> = preferencesRepository.homeLayoutConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeLayoutConfig.defaultConfig())

    val homeLayoutPresets: StateFlow<List<HomeLayoutPreset>> = preferencesRepository.homeLayoutPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedCustomEqProfiles: StateFlow<List<SavedCustomEqProfile>> = preferencesRepository.savedCustomEqProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customPlaylists: StateFlow<List<CustomPlaylist>> = preferencesRepository.customPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val playbackEnginePrefs: StateFlow<PlaybackEnginePrefs> = preferencesRepository.playbackEnginePrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackEnginePrefs())

    val savedQueuePresets: StateFlow<List<SavedQueuePreset>> = preferencesRepository.savedQueuePresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sleepTimerCustomPresets: StateFlow<List<Int>> = preferencesRepository.sleepTimerCustomPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), listOf(5, 10, 15, 30, 45, 60))

    private val statsRepository = StatsRepository(appContext)
    private val ritmo2PrefsRepo = Ritmo2PreferencesRepository(appContext)

    private val _selectedWrappedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _ambientMixSeed = MutableStateFlow(System.nanoTime())

    val extremeVisualStyle: StateFlow<ExtremeVisualStyleConfig> = ritmo2PrefsRepo.extremeVisualStyle
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExtremeVisualStyleConfig.defaultConfig())

    val playerDesignerConfig: StateFlow<PlayerDesignerConfig> = ritmo2PrefsRepo.playerDesignerConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerDesignerConfig.defaultConfig())

    val musicalProfileConfig: StateFlow<MusicalProfileConfig> = ritmo2PrefsRepo.musicalProfileConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, MusicalProfileConfig.defaultConfig())

    val savedAmbientMixes: StateFlow<List<SavedAmbientMix>> = ritmo2PrefsRepo.savedAmbientMixes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val djRuntimeState: StateFlow<SmartDjRuntimeState> = PlaybackEngineBus.djState

    private var pendingNewPlayTrack: LocalTrack? = null
    private var lastSyncedTotalSeconds: Long = 0L
    private var activeSessionTrackKey: String? = null
    private var activeSessionSeconds: Long = 0L
    private var activeSessionQualifiedRecorded: Boolean = false
    private var lastObservedPosMs: Long = 0L

    private val playbackManager = PlaybackControllerManager(
        context = appContext,
        scope = viewModelScope,
        onListeningTick = { track, deltaSec, positionMs -> handleListeningTick(track, deltaSec, positionMs) },
        onTrackStarted = { track ->
            pendingNewPlayTrack = track
            activeSessionTrackKey = track.trackKey
            activeSessionSeconds = 0L
            activeSessionQualifiedRecorded = false
            lastObservedPosMs = playbackState.value.positionMs
            viewModelScope.launch { recordPlayAndSaveProgress(track, playbackState.value.positionMs, incrementPlay = false) }
        },
        onPausedOrEnded = { track, positionMs ->
            if (track != null) {
                viewModelScope.launch { flushToCloud(track, positionMs, showSuccessBanner = false) }
            }
        },
        onModesChanged = { shuffle, repeat ->
            viewModelScope.launch { preferencesRepository.savePlaybackModes(shuffle, repeat) }
        },
        onQueueSnapshotChanged = { queue, idx, pos ->
            viewModelScope.launch {
                preferencesRepository.saveActiveQueueSnapshot(queue.map { it.trackKey }, idx, pos)
            }
        }
    )

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.state

    val albumPlaylists: StateFlow<List<AlbumPlaylist>> = _localTracks
        .map { localMusicRepository.buildAlbumPlaylists(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val artistPlaylists: StateFlow<List<ArtistPlaylist>> = _localTracks
        .map { localMusicRepository.buildArtistPlaylists(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val cloudFavoritesFlow: StateFlow<List<FavoriteTrack>> =
        if (currentUserId == null) {
            flowOf(emptyList())
        } else {
            cloudSyncRepository.observeFavorites(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Error observing favorites", firestoreError ?: error)
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val favoriteTrackKeys: StateFlow<Set<String>> =
        if (currentUserId == null) {
            preferencesRepository.offlineFavoriteKeys
        } else {
            cloudFavoritesFlow.map { list -> list.map { it.trackKey }.toSet() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val favoriteTracks: StateFlow<List<LocalTrack>> = combine(
        _localTracks,
        favoriteTrackKeys,
        cloudFavoritesFlow
    ) { local, favKeys, cloudFavs ->
        val localByKey = local.associateBy { it.trackKey }
        if (currentUserId == null) {
            favKeys.mapNotNull { localByKey[it] }
        } else {
            cloudFavs.map { fav ->
                localByKey[fav.trackKey] ?: LocalTrack(
                    id = fav.trackUri.hashCode().toLong(),
                    trackKey = fav.trackKey,
                    title = fav.trackTitle,
                    artist = fav.artistName,
                    album = fav.albumName,
                    albumId = fav.albumId,
                    durationMs = fav.durationMs,
                    contentUri = fav.trackUri,
                    albumArtUri = if (fav.albumId > 0L) "content://media/external/audio/albumart/${fav.albumId}" else null,
                    dateAdded = 0L
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cloudPlaybackProgress: StateFlow<UiState<PlaybackProgress?>> =
        if (currentUserId == null) {
            preferencesRepository.offlineLastProgress.map<PlaybackProgress?, UiState<PlaybackProgress?>> { UiState.Success(it) }
        } else {
            cloudSyncRepository.observePlaybackProgress(currentUserId)
                .map<PlaybackProgress?, UiState<PlaybackProgress?>> { progress ->
                    if (progress != null && progress.totalPlaybackSeconds > lastSyncedTotalSeconds) {
                        lastSyncedTotalSeconds = progress.totalPlaybackSeconds
                    }
                    UiState.Success(progress)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "No se pudo sincronizar"))
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val liveTotalPlaybackSeconds: StateFlow<Long> =
        if (currentUserId == null) {
            combine(preferencesRepository.offlineTotalSeconds, _pendingSeconds) { base, pending -> base + pending }
        } else {
            combine(cloudPlaybackProgress, _pendingSeconds) { progressState, pending ->
                val cloudBase = (progressState as? UiState.Success)?.data?.totalPlaybackSeconds ?: lastSyncedTotalSeconds
                cloudBase + pending
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0L)

    private val localAllEventsFlow: StateFlow<List<ListeningEventEntity>> = statsRepository.allEvents
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allTrackStatsMap: StateFlow<Map<String, TrackStatsEntity>> = statsRepository.allTrackStats
        .map { list -> list.associateBy { it.trackKey } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val metadataOverrides: StateFlow<Map<String, TrackMetadataOverrideEntity>> = statsRepository.metadataOverrides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val savedWrappedYears: StateFlow<List<Int>> = statsRepository.savedWrappedYears
        .map { list -> list.map { it.year } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val realTimeStatsSummary: StateFlow<RealTimeStatsSummary> = localAllEventsFlow
        .map { events -> StatsAnalyticsEngine.computeRealTimeSummary(events) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            StatsAnalyticsEngine.computeRealTimeSummary(emptyList())
        )

    private val genreByTrackKeyFlow = _localTracks.map { tracks ->
        tracks.associate { it.trackKey to it.genre }
    }

    val ritmoRecapReport: StateFlow<RitmoRecapReport> = combine(
        localAllEventsFlow,
        _selectedMonthKey,
        genreByTrackKeyFlow
    ) { events, monthKey, genres ->
        StatsAnalyticsEngine.computeRecapReport(events, monthKey, genres)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        StatsAnalyticsEngine.computeRecapReport(emptyList(), currentMonthKey())
    )

    val ritmoWrappedReport: StateFlow<RitmoWrappedReport> = combine(
        localAllEventsFlow,
        _selectedWrappedYear,
        genreByTrackKeyFlow
    ) { events, year, genres ->
        StatsAnalyticsEngine.computeWrappedReport(events, year, genres)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        StatsAnalyticsEngine.computeWrappedReport(emptyList(), Calendar.getInstance().get(Calendar.YEAR))
    )

    val musicalHistoryReport: StateFlow<MusicalHistoryReport> = combine(
        localAllEventsFlow,
        allTrackStatsMap,
        _selectedMonthKey,
        _selectedWrappedYear
    ) { events, statsMap, monthKey, year ->
        StatsAnalyticsEngine.computeHistoryReport(
            events = events,
            trackStats = statsMap.values.toList(),
            selectedMonthKey = monthKey,
            selectedYear = year
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        StatsAnalyticsEngine.computeHistoryReport(
            events = emptyList(),
            trackStats = emptyList(),
            selectedMonthKey = currentMonthKey(),
            selectedYear = Calendar.getInstance().get(Calendar.YEAR)
        )
    )

    val musicalAchievements: StateFlow<List<MusicalAchievement>> = combine(
        localAllEventsFlow,
        allTrackStatsMap,
        favoriteTrackKeys,
        customPlaylists
    ) { events, statsMap, favKeys, playlists ->
        StatsAnalyticsEngine.computeAchievements(
            events = events,
            trackStats = statsMap.values.toList(),
            favoriteCount = favKeys.size,
            customPlaylistCount = playlists.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        StatsAnalyticsEngine.computeAchievements(emptyList(), emptyList(), 0, 0)
    )

    val smartAmbientMixes: StateFlow<Map<AmbientMoodCategory, GeneratedAmbientMix>> = combine(
        _localTracks,
        favoriteTrackKeys,
        allTrackStatsMap,
        _ambientMixSeed
    ) { tracks, favKeys, statsMap, seed ->
        AmbientMoodCategory.entries.mapIndexed { idx, category ->
            category to AmbientMixGenerator.generateMix(
                tracks = tracks,
                category = category,
                favoriteTrackKeys = favKeys,
                trackStatsByKey = statsMap,
                seed = seed + idx * 31L
            )
        }.toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    private val monthlySummariesFlow: StateFlow<UiState<List<MonthlyStat>>> =
        if (currentUserId == null) {
            flowOf(UiState.Success(emptyList()))
        } else {
            cloudSyncRepository.observeMonthlyStats(currentUserId)
                .map<List<MonthlyStat>, UiState<List<MonthlyStat>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Error cargando meses"))
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val currentMonthTrackStatsFlow: StateFlow<List<MonthlyTrackStat>> =
        if (currentUserId == null) {
            flowOf(emptyList())
        } else {
            cloudSyncRepository.observeMonthTrackStats(currentUserId, currentMonthKey())
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(emptyList())
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val mostPlayedTracks: StateFlow<List<MostPlayedItem>> = combine(
        _localTracks,
        currentMonthTrackStatsFlow,
        allTrackStatsMap
    ) { local, cloudStats, localStatsMap ->
        val localByKey = local.associateBy { it.trackKey }
        if (currentUserId == null || cloudStats.isEmpty()) {
            localStatsMap.values
                .filter { it.qualifiedPlays > 0L || it.totalSeconds > 0L }
                .sortedWith(compareByDescending<TrackStatsEntity> { it.qualifiedPlays }.thenByDescending { it.totalSeconds })
                .mapNotNull { stat ->
                    localByKey[stat.trackKey]?.let { MostPlayedItem(it, stat.qualifiedPlays, stat.totalSeconds) }
                }
        } else {
            cloudStats.filter { it.playCount > 0L || it.totalSeconds > 0L }
                .sortedWith(compareByDescending<MonthlyTrackStat> { it.playCount }.thenByDescending { it.totalSeconds })
                .mapNotNull { stat ->
                    localByKey[stat.trackKey]?.let { MostPlayedItem(it, stat.playCount, stat.totalSeconds) }
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val selectedMonthTrackStatsFlow: StateFlow<UiState<List<MonthlyTrackStat>>> =
        if (currentUserId == null) {
            flowOf(UiState.Success(emptyList()))
        } else {
            _selectedMonthKey.flatMapLatest { monthKey ->
                cloudSyncRepository.observeMonthTrackStats(currentUserId, monthKey)
                    .map<List<MonthlyTrackStat>, UiState<List<MonthlyTrackStat>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Error cargando estadísticas"))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val monthlyReportState: StateFlow<UiState<MonthlyReportData>> = combine(
        _selectedMonthKey,
        monthlySummariesFlow,
        selectedMonthTrackStatsFlow,
        ritmoRecapReport
    ) { monthKey, summariesUi, tracksUi, localRecap ->
        if (currentUserId == null || (summariesUi is UiState.Success && summariesUi.data.isEmpty() && localRecap.hasEnoughData)) {
            val summary = if (localRecap.totalSeconds > 0L || localRecap.totalPlays > 0L) {
                MonthlyStat(
                    monthKey = localRecap.monthKey,
                    totalSeconds = localRecap.totalSeconds,
                    totalPlays = localRecap.totalPlays
                )
            } else null
            UiState.Success(
                MonthlyReportData(
                    selectedMonthKey = localRecap.monthKey,
                    availableMonths = localRecap.availableMonths,
                    summary = summary,
                    topSongs = localRecap.top10Songs,
                    topArtists = localRecap.top10Artists,
                    topAlbums = localRecap.topAlbums
                )
            )
        } else {
            when {
                summariesUi is UiState.Error -> UiState.Error(summariesUi.message)
                tracksUi is UiState.Error -> UiState.Error(tracksUi.message)
                summariesUi is UiState.Loading || tracksUi is UiState.Loading -> UiState.Loading
                summariesUi is UiState.Success && tracksUi is UiState.Success -> {
                    val allMonths = (listOf(currentMonthKey()) + summariesUi.data.map { it.monthKey } + localRecap.availableMonths)
                        .distinct().sortedDescending()
                    val summary = summariesUi.data.firstOrNull { it.monthKey == monthKey }
                    val rawTracks = tracksUi.data
                    val topSongs = rawTracks.sortedWith(compareByDescending<MonthlyTrackStat> { it.totalSeconds }.thenByDescending { it.playCount })
                    val topArtists = rawTracks.groupBy { it.artistName }.map { (artist, items) ->
                        ArtistStatSummary(artist, items.sumOf { it.totalSeconds }, items.sumOf { it.playCount }, items.size)
                    }.sortedWith(compareByDescending<ArtistStatSummary> { it.totalSeconds }.thenByDescending { it.totalPlays })
                    val topAlbums = rawTracks.groupBy { it.albumName to it.artistName }.map { (aa, items) ->
                        AlbumStatSummary(aa.first, aa.second, items.firstOrNull { it.albumId > 0L }?.albumId ?: 0L, items.sumOf { it.totalSeconds }, items.sumOf { it.playCount }, items.size)
                    }.sortedWith(compareByDescending<AlbumStatSummary> { it.totalSeconds }.thenByDescending { it.totalPlays })
                    UiState.Success(MonthlyReportData(monthKey, allMonths, summary, topSongs, topArtists, topAlbums))
                }
                else -> UiState.Loading
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val regionalChatMessages: StateFlow<UiState<List<RegionalChatMessage>>> =
        if (currentUserId == null) {
            flowOf(UiState.Success(emptyList()))
        } else {
            _selectedChatRegion.flatMapLatest { region ->
                viewModelScope.launch {
                    cloudSyncRepository.purgeExpiredChatMessages(region)
                }
                cloudSyncRepository.observeRegionalChat(region)
                    .map<List<RegionalChatMessage>, UiState<List<RegionalChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Error en chat regional"))
                    }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        viewModelScope.launch {
            val storedEq = preferencesRepository.equalizerPrefs.first()
            val savedDeviceMap = preferencesRepository.deviceSoundProfiles.first()
            val detectedDevice = deviceAudioProfileManager.refreshDetectedDevice()
            AudioEffectsController.loadStoredPrefs(storedEq)
            AudioEffectsController.setActiveDeviceProfileType(detectedDevice)
            if (storedEq.autoSwitchDeviceProfile) {
                savedDeviceMap[detectedDevice]?.let { devProfile ->
                    val merged = AudioEffectsController.applyDeviceSoundProfile(devProfile)
                    preferencesRepository.saveEqualizerPrefs(merged)
                }
            }
            deviceAudioProfileManager.startMonitoring()
            val initialSettings = preferencesRepository.appSettingsPrefs.first()
            val initialEnginePrefs = preferencesRepository.playbackEnginePrefs.first()
            PlaybackEngineBus.updateConfig(initialEnginePrefs)
            SleepTimerController.setFadeOutDurationSeconds(initialSettings.sleepTimerFadeOutSeconds)
            AppDiagnosticsManager.initialize(appContext, initialSettings.diagnosticModeEnabled)
            _selectedChatRegion.value = initialSettings.preferredChatRegion
            playbackManager.setNotificationArtworkEnabled(initialSettings.notifShowArtwork)
            playbackManager.connect(initialSettings.savedShuffleEnabled, initialSettings.savedRepeatMode)
            refreshLocalLibrary()
        }
        viewModelScope.launch {
            playbackState
                .map { it.currentTrack }
                .distinctUntilChanged { a, b -> a?.trackKey == b?.trackKey }
                .collect { track ->
                    if (track != null) loadLyricsForTrack(track)
                }
        }
        viewModelScope.launch {
            appSettings.collect { settings ->
                val lang = if (settings.voiceLanguageCode.isNotBlank()) settings.voiceLanguageCode else settings.appLanguage.code
                val hasMic = ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (settings.handsFreeVoiceEnabled && hasMic) {
                    if (settings.screenOffVoiceEnabled) {
                        VoiceWakeService.start(appContext)
                    } else {
                        VoiceWakeService.stop(appContext)
                    }
                    voiceRecognizer.configureHandsFree(true, lang, settings.screenOffVoiceEnabled)
                } else {
                    VoiceWakeService.stop(appContext)
                    voiceRecognizer.configureHandsFree(false, lang, settings.screenOffVoiceEnabled)
                }
            }
        }
        viewModelScope.launch {
            appSettings
                .map { Triple(it.chatPushNotificationsEnabled, it.chatPushOnlyActiveRegion, it.preferredChatRegion) }
                .distinctUntilChanged()
                .collect {
                    ChatNotificationHelper.syncTokenAndSubscriptions(
                        context = appContext,
                        cloudSyncRepository = cloudSyncRepository,
                        preferencesRepository = preferencesRepository,
                        userId = currentUserId,
                        settings = appSettings.value
                    )
                }
        }
    }

    fun refreshLocalLibrary() {
        viewModelScope.launch {
            _isScanningLibrary.value = true
            val settings = preferencesRepository.appSettingsPrefs.first()
            val overrides = preferencesRepository.customCoverMap.first()
            val metaOverrides = statsRepository.getAllMetadataOverrides()
            val tracks = localMusicRepository.loadLocalTracks(
                hideShortOrWhatsApp = settings.hideShortWhatsAppAudio,
                coverOverrides = overrides,
                metadataOverrides = metaOverrides
            )
            _localTracks.value = tracks
            playbackManager.syncLibraryTracks(tracks)
            val snapshot = preferencesRepository.activeQueueSnapshot.first()
            if (snapshot != null && tracks.isNotEmpty()) {
                val trackByKey = tracks.associateBy { it.trackKey }
                val restoredQueue = snapshot.first.mapNotNull { trackByKey[it] }
                if (restoredQueue.isNotEmpty()) {
                    playbackManager.restorePersistedQueueIfIdle(restoredQueue, snapshot.second, snapshot.third)
                }
            }
            generateAllSlotPlaylists(tracks)
            _isScanningLibrary.value = false
            if (settings.autoDownloadArtwork) {
                downloadMissingArtworkInternal(tracks)
            }
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            localMusicRepository.saveImportedAudioUris(uris)
            refreshLocalLibrary()
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    fun selectTimeOfDaySlot(slot: TimeOfDaySlot) {
        _selectedSlot.value = slot
        if (!_playlistsBySlot.value.containsKey(slot) && _localTracks.value.isNotEmpty()) {
            regeneratePlaylist(slot)
        }
    }

    fun regeneratePlaylist(slot: TimeOfDaySlot) {
        val generated = localMusicRepository.generatePlaylistForSlot(_localTracks.value, slot, System.nanoTime())
        _playlistsBySlot.value = _playlistsBySlot.value + (slot to generated)
    }

    fun selectMonth(monthKey: String) { _selectedMonthKey.value = monthKey }

    fun playTrackFromLibrary(track: LocalTrack) {
        val tracks = _localTracks.value
        val idx = tracks.indexOfFirst { it.trackKey == track.trackKey }.coerceAtLeast(0)
        playbackManager.playQueue(tracks.ifEmpty { listOf(track) }, idx, 0L)
    }

    fun playGeneratedPlaylist(slot: TimeOfDaySlot, startIndex: Int = 0) {
        _selectedSlot.value = slot
        val playlist = _playlistsBySlot.value[slot] ?: return
        if (playlist.tracks.isEmpty()) return
        playbackManager.playQueue(playlist.tracks, startIndex, 0L)
    }

    fun playTracksQueue(tracks: List<LocalTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        playbackManager.playQueue(tracks, startIndex, 0L)
    }

    fun playNext(track: LocalTrack) {
        playbackManager.playNext(track)
        _actionToastMessage.value = "Se reproducirá a continuación: ${track.title}"
    }

    fun appendToQueue(track: LocalTrack) {
        playbackManager.appendToQueue(track)
        _actionToastMessage.value = "Agregada a la cola: ${track.title}"
    }

    fun jumpToQueueIndex(index: Int) {
        playbackManager.jumpToQueueIndex(index)
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playbackManager.moveQueueItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        playbackManager.removeQueueItem(index)
    }

    fun smartShuffleQueue() {
        if (playbackState.value.queue.size <= 2) {
            _actionToastMessage.value = "Agrega al menos 3 pistas para una mezcla inteligente"
            return
        }
        playbackManager.smartShuffleQueue(playbackEnginePrefs.value.avoidQueueRepeats)
        _actionToastMessage.value = "Cola reorganizada sin repetir artista seguido"
    }

    fun deduplicateQueue() {
        val removed = playbackManager.deduplicateQueue()
        _actionToastMessage.value = if (removed > 0) {
            "Se quitaron $removed canciones repetidas de la cola"
        } else {
            "La cola ya no tiene canciones repetidas"
        }
    }

    fun saveCurrentQueueAsPreset(name: String) {
        val clean = name.trim().take(40)
        val queue = playbackState.value.queue
        if (clean.isBlank() || queue.isEmpty()) return
        val preset = SavedQueuePreset(
            id = "queue_${System.currentTimeMillis()}",
            name = clean,
            trackKeys = queue.map { it.trackKey }
        )
        viewModelScope.launch {
            preferencesRepository.saveQueuePreset(preset)
            _actionToastMessage.value = "Cola guardada: $clean"
        }
    }

    fun loadSavedQueuePreset(preset: SavedQueuePreset) {
        val byKey = _localTracks.value.associateBy { it.trackKey }
        val matched = preset.trackKeys.mapNotNull { byKey[it] }
        if (matched.isEmpty()) {
            _actionToastMessage.value = "Las pistas de '${preset.name}' ya no están disponibles"
            return
        }
        playbackManager.playQueue(matched, 0, 0L)
        _actionToastMessage.value = "Cola cargada: ${preset.name}"
    }

    fun deleteSavedQueuePreset(presetId: String) {
        viewModelScope.launch {
            preferencesRepository.deleteQueuePreset(presetId)
        }
    }

    fun updatePlaybackEnginePrefs(prefs: PlaybackEnginePrefs) {
        PlaybackEngineBus.updateConfig(prefs)
        viewModelScope.launch {
            preferencesRepository.savePlaybackEnginePrefs(prefs)
        }
    }

    fun triggerManualDjMix() {
        if (playbackState.value.queue.size <= 1) {
            _actionToastMessage.value = "Agrega otra canción a la cola para usar Mezcla DJ"
            return
        }
        PlaybackEngineBus.requestManualDjMix()
    }

    fun toggleFavorite(track: LocalTrack) {
        val isFav = favoriteTrackKeys.value.contains(track.trackKey)
        viewModelScope.launch {
            if (currentUserId != null) {
                cloudSyncRepository.toggleFavorite(currentUserId, track, isFav)
            } else {
                preferencesRepository.toggleOfflineFavorite(track.trackKey)
            }
        }
    }

    // Custom Playlists
    fun createCustomPlaylist(name: String) {
        viewModelScope.launch {
            val current = preferencesRepository.customPlaylists.first()
            val newList = current + CustomPlaylist(id = "pl_${System.currentTimeMillis()}", name = name.trim(), trackKeys = emptyList())
            preferencesRepository.saveCustomPlaylists(newList)
        }
    }

    fun createCustomPlaylistAndAdd(name: String, track: LocalTrack) {
        viewModelScope.launch {
            val current = preferencesRepository.customPlaylists.first()
            val newList = current + CustomPlaylist(id = "pl_${System.currentTimeMillis()}", name = name.trim(), trackKeys = listOf(track.trackKey))
            preferencesRepository.saveCustomPlaylists(newList)
            _actionToastMessage.value = "Agregada a '$name'"
        }
    }

    fun addTrackToCustomPlaylist(playlistId: String, track: LocalTrack) {
        viewModelScope.launch {
            val current = preferencesRepository.customPlaylists.first()
            val updated = current.map { pl ->
                if (pl.id == playlistId && !pl.trackKeys.contains(track.trackKey)) {
                    pl.copy(trackKeys = pl.trackKeys + track.trackKey)
                } else pl
            }
            preferencesRepository.saveCustomPlaylists(updated)
            _actionToastMessage.value = "Agregada a playlist"
        }
    }

    fun renameCustomPlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            val current = preferencesRepository.customPlaylists.first()
            preferencesRepository.saveCustomPlaylists(current.map { if (it.id == playlistId) it.copy(name = newName.trim()) else it })
        }
    }

    fun deleteCustomPlaylist(playlistId: String) {
        viewModelScope.launch {
            val current = preferencesRepository.customPlaylists.first()
            preferencesRepository.saveCustomPlaylists(current.filterNot { it.id == playlistId })
        }
    }

    fun removeTrackFromCustomPlaylist(playlistId: String, track: LocalTrack) {
        viewModelScope.launch {
            val current = preferencesRepository.customPlaylists.first()
            preferencesRepository.saveCustomPlaylists(
                current.map { pl -> if (pl.id == playlistId) pl.copy(trackKeys = pl.trackKeys.filterNot { it == track.trackKey }) else pl }
            )
        }
    }

    // Cover Art Customization & Online Fetch
    fun applyCustomTrackCover(track: LocalTrack, imageUri: Uri) {
        viewModelScope.launch {
            val saved = coverArtRepository.saveCustomArtworkFromUri(track.trackKey, imageUri)
            if (saved != null) {
                preferencesRepository.setCustomCoverForKey(track.trackKey, saved)
                playbackManager.refreshTrackMetadata(track.copy(albumArtUri = saved))
                refreshLocalLibrary()
                _actionToastMessage.value = "Carátula actualizada"
            }
        }
    }

    fun applyCustomAlbumCover(album: AlbumPlaylist, imageUri: Uri) {
        viewModelScope.launch {
            val albumKey = CoverArtRepository.albumOverrideKey(album.albumName, album.artistName)
            val saved = coverArtRepository.saveCustomArtworkFromUri(albumKey, imageUri)
            if (saved != null) {
                preferencesRepository.setCustomCoverForKey(albumKey, saved)
                refreshLocalLibrary()
                _actionToastMessage.value = "Carátula del álbum actualizada"
            }
        }
    }

    fun fetchTrackCoverOnline(track: LocalTrack) {
        viewModelScope.launch {
            val url = coverArtRepository.fetchAndCacheOnlineCover(track.trackKey, track.title, track.artist, track.album)
            if (url != null) {
                preferencesRepository.setCustomCoverForKey(track.trackKey, url)
                playbackManager.refreshTrackMetadata(track.copy(albumArtUri = url))
                refreshLocalLibrary()
                _actionToastMessage.value = "Carátula descargada en línea"
            } else {
                _actionToastMessage.value = "No se encontró carátula en línea para ${track.title}"
            }
        }
    }

    fun downloadMissingArtworkNow() {
        viewModelScope.launch { downloadMissingArtworkInternal(_localTracks.value) }
    }

    private suspend fun downloadMissingArtworkInternal(tracks: List<LocalTrack>) {
        val candidates = tracks.filter { it.albumArtUri.isNullOrBlank() }.take(10)
        if (candidates.isEmpty()) return
        var downloaded = 0
        for (track in candidates) {
            val cached = coverArtRepository.fetchAndCacheOnlineCover(track.trackKey, track.title, track.artist, track.album)
            if (cached != null) {
                preferencesRepository.setCustomCoverForKey(track.trackKey, cached)
                downloaded++
            }
        }
        if (downloaded > 0) {
            val overrides = preferencesRepository.customCoverMap.first()
            val metaOverrides = statsRepository.getAllMetadataOverrides()
            val refreshed = localMusicRepository.loadLocalTracks(
                hideShortOrWhatsApp = appSettings.value.hideShortWhatsAppAudio,
                coverOverrides = overrides,
                metadataOverrides = metaOverrides
            )
            _localTracks.value = refreshed
            playbackManager.syncLibraryTracks(refreshed)
        }
    }

    // Equalizer, Dolby Atmos 3D, Hearing Adaptation, Audio Type, Treble & Per-Device Profiles
    private fun persistEqChange(stored: com.example.data.EqualizerStoredPrefs) {
        viewModelScope.launch {
            preferencesRepository.saveEqualizerPrefs(stored)
            if (stored.autoSwitchDeviceProfile) {
                val activeDevice = AudioEffectsController.state.value.activeDeviceProfileType
                preferencesRepository.saveDeviceSoundProfile(AudioEffectsController.buildDeviceSoundProfile(activeDevice))
            }
        }
    }

    private fun handleOutputAudioDeviceChanged(deviceType: AudioDeviceProfileType) {
        AudioEffectsController.setActiveDeviceProfileType(deviceType)
        val eq = equalizerState.value
        if (!eq.autoSwitchDeviceProfile) return
        val saved = deviceSoundProfiles.value[deviceType]
        if (saved != null) {
            val applied = AudioEffectsController.applyDeviceSoundProfile(saved)
            viewModelScope.launch { preferencesRepository.saveEqualizerPrefs(applied) }
            _actionToastMessage.value = "Perfil aplicado para ${deviceType.shortLabel}"
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) { persistEqChange(AudioEffectsController.setEnabled(enabled)) }
    fun applyEqualizerPreset(presetIndex: Int) { persistEqChange(AudioEffectsController.applyPreset(presetIndex)) }
    fun updateEqualizerBand(bandIndex: Int, levelMb: Int) { persistEqChange(AudioEffectsController.updateBandLevel(bandIndex, levelMb)) }
    fun updateBassBoost(strength: Int) { persistEqChange(AudioEffectsController.updateBassBoost(strength)) }
    fun updateTrebleBoost(trebleMb: Int) { persistEqChange(AudioEffectsController.updateTrebleBoost(trebleMb)) }
    fun setSpatial3dEnabled(enabled: Boolean) { persistEqChange(AudioEffectsController.setSpatial3dEnabled(enabled)) }
    fun applySpatial3dPreset(preset: Spatial3dPreset) { persistEqChange(AudioEffectsController.applySpatial3dPreset(preset)) }
    fun updateSpatial3dStrength(strength: Int) { persistEqChange(AudioEffectsController.updateSpatial3dStrength(strength)) }
    fun updateClarityBoost(gainMb: Int) { persistEqChange(AudioEffectsController.updateClarityBoost(gainMb)) }
    fun selectHearingProfile(profile: HearingProfile) { persistEqChange(AudioEffectsController.selectHearingProfile(profile)) }
    fun setAudioChannelMode(mode: AudioChannelMode) { persistEqChange(AudioEffectsController.setAudioChannelMode(mode)) }
    fun setBalanceLR(balance: Float) { persistEqChange(AudioEffectsController.setBalanceLR(balance)) }
    fun setHiResMasteringEnabled(enabled: Boolean) { persistEqChange(AudioEffectsController.setHiResMasteringEnabled(enabled)) }
    fun selectStudioAudioMode(mode: StudioAudioMode) { persistEqChange(AudioEffectsController.selectStudioAudioMode(mode)) }
    fun updateDynamicPunch(strength: Int) { persistEqChange(AudioEffectsController.updateDynamicPunch(strength)) }
    fun setAntiClipLimiterEnabled(enabled: Boolean) { persistEqChange(AudioEffectsController.setAntiClipLimiterEnabled(enabled)) }

    fun setAutoSwitchDeviceProfile(enabled: Boolean) {
        val updated = AudioEffectsController.setAutoSwitchDeviceProfile(enabled)
        viewModelScope.launch { preferencesRepository.saveEqualizerPrefs(updated) }
    }

    fun saveCurrentEqToDeviceProfile(deviceType: AudioDeviceProfileType = equalizerState.value.activeDeviceProfileType) {
        val profile = AudioEffectsController.buildDeviceSoundProfile(deviceType)
        viewModelScope.launch {
            preferencesRepository.saveDeviceSoundProfile(profile)
            _actionToastMessage.value = "Perfil guardado para ${deviceType.shortLabel}"
        }
    }

    fun selectAndApplyDeviceProfile(deviceType: AudioDeviceProfileType) {
        AudioEffectsController.setActiveDeviceProfileType(deviceType)
        val saved = deviceSoundProfiles.value[deviceType]
        if (saved != null) {
            val applied = AudioEffectsController.applyDeviceSoundProfile(saved)
            viewModelScope.launch { preferencesRepository.saveEqualizerPrefs(applied) }
            _actionToastMessage.value = "Cargado perfil de ${deviceType.shortLabel}"
        } else {
            _actionToastMessage.value = "Editando perfil base para ${deviceType.shortLabel} (pulsa Guardar para fijarlo)"
        }
    }

    fun deleteDeviceSoundProfile(deviceType: AudioDeviceProfileType) {
        viewModelScope.launch {
            preferencesRepository.deleteDeviceSoundProfile(deviceType)
            _actionToastMessage.value = "Perfil de ${deviceType.shortLabel} restablecido"
        }
    }

    fun saveCurrentAsCustomEqProfile(name: String) {
        val clean = name.trim().take(36)
        if (clean.isBlank()) return
        val cur = AudioEffectsController.state.value
        val profile = SavedCustomEqProfile(
            id = "eq_${System.currentTimeMillis()}",
            name = clean,
            bandLevelsMb = cur.bands.map { it.levelMb },
            bassBoostStrength = cur.bassBoostStrength,
            spatial3dEnabled = cur.spatial3dEnabled
        )
        viewModelScope.launch {
            preferencesRepository.saveCustomEqProfile(profile)
            _actionToastMessage.value = "Perfil EQ guardado: $clean"
        }
    }

    fun applySavedCustomEqProfile(profile: SavedCustomEqProfile) {
        viewModelScope.launch {
            val stored = AudioEffectsController.applyCustomBandsAndBoost(
                bandLevelsMb = profile.bandLevelsMb,
                bassBoostStrength = profile.bassBoostStrength,
                spatial3dEnabled = profile.spatial3dEnabled
            )
            preferencesRepository.saveEqualizerPrefs(stored)
            _actionToastMessage.value = "Perfil aplicado: ${profile.name}"
        }
    }

    fun deleteSavedCustomEqProfile(profileId: String) {
        viewModelScope.launch {
            preferencesRepository.deleteCustomEqProfile(profileId)
        }
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) { SleepTimerController.startCountdown(minutes.coerceAtLeast(1) * 60L) }
    fun startSongSleepTimer(songCount: Int) { SleepTimerController.startSongCountdown(songCount) }
    fun setSleepTimerFadeOut(enabled: Boolean) { SleepTimerController.setFadeOutEnabled(enabled) }
    fun setSleepTimerFadeOutDuration(seconds: Int) {
        SleepTimerController.setFadeOutDurationSeconds(seconds)
        viewModelScope.launch { preferencesRepository.setSleepTimerFadeOutSeconds(seconds) }
    }
    fun saveSleepTimerPreset(minutes: Int) {
        val cleanMin = minutes.coerceIn(1, 180)
        val updated = (sleepTimerCustomPresets.value + cleanMin).distinct().sorted().take(9)
        viewModelScope.launch {
            preferencesRepository.saveSleepTimerCustomPresets(updated)
            _actionToastMessage.value = "Temporizador guardado: $cleanMin min"
        }
    }
    fun extendSleepTimer(additionalMinutes: Int = 5) { SleepTimerController.addSeconds(additionalMinutes.coerceAtLeast(1) * 60L) }
    fun toggleStopAfterCurrentTrack() { SleepTimerController.toggleStopAfterCurrentTrack() }
    fun cancelSleepTimer() { SleepTimerController.cancel() }
    fun clearSleepTimerExpiredBanner() { SleepTimerController.clearExpiredFlag() }

    // Advanced Now Playing Visual Customization
    fun setPlayerStyle(style: PlayerVisualStyle) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(style = style))
        }
    }

    fun setPlayerBackgroundMode(mode: PlayerBackgroundMode) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(backgroundMode = mode))
        }
    }

    fun setPlayerPrimaryColor(colorArgb: Long) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(customPrimaryColorArgb = colorArgb))
        }
    }

    fun setPlayerButtonColor(colorArgb: Long) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(customButtonColorArgb = colorArgb))
        }
    }

    fun setPlayerShowVisualizer(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(showVisualizer = show))
        }
    }

    fun setPlayerShowInlineLyrics(show: Boolean) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(showInlineLyricsBar = show))
        }
    }

    fun setVisualizerStyle(style: VisualizerStyle) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(visualizerStyle = style))
        }
    }

    fun setVisualizerIntensity(intensity: Float) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(
                playerVisualPrefs.value.copy(visualizerIntensity = intensity.coerceIn(0.35f, 1.5f))
            )
        }
    }

    fun setPlayerLayoutMode(mode: PlayerLayoutMode) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(layoutMode = mode))
        }
    }

    fun setPlayerControlStyle(style: PlayerControlStyle) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(playerVisualPrefs.value.copy(controlStyle = style))
        }
    }

    fun setPlayerBlurIntensity(intensity: Float) {
        viewModelScope.launch {
            preferencesRepository.savePlayerVisualPrefs(
                playerVisualPrefs.value.copy(blurIntensity = intensity.coerceIn(0f, 1f))
            )
        }
    }

    fun restoreOriginalPlayerVisuals() {
        viewModelScope.launch {
            preferencesRepository.restoreOriginalPlayerVisualDefaults()
            _actionToastMessage.value = "Diseño del reproductor restaurado"
        }
    }

    // Settings & Stage 3 Personalization
    fun setAppLanguage(language: AppLanguage) { viewModelScope.launch { preferencesRepository.setAppLanguage(language) } }
    fun setVoiceLanguage(language: AppLanguage) { viewModelScope.launch { preferencesRepository.setVoiceLanguageCode(language.code) } }
    fun setUiStyleMode(mode: UiStyleMode) { viewModelScope.launch { preferencesRepository.setUiStyleMode(mode) } }
    fun setFontStyle(style: AppFontStyle) {
        viewModelScope.launch {
            preferencesRepository.setFontStyle(style)
            _actionToastMessage.value = "Fuente aplicada: ${style.displayName}"
        }
    }
    fun setFontSizeScale(scale: Float) { viewModelScope.launch { preferencesRepository.setFontSizeScale(scale) } }
    fun setWallpaperDimAlpha(alpha: Float) { viewModelScope.launch { preferencesRepository.setWallpaperDimAlpha(alpha) } }
    fun setWallpaperBlurDp(blurDp: Float) { viewModelScope.launch { preferencesRepository.setWallpaperBlurDp(blurDp) } }
    fun setUseDynamicMaterialYou(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setUseDynamicMaterialYou(enabled) } }

    fun saveAndApplyCustomTheme(theme: CustomThemeConfig) {
        viewModelScope.launch {
            preferencesRepository.saveCustomTheme(theme, setAsActive = true)
            _actionToastMessage.value = "Tema aplicado: ${theme.name}"
        }
    }

    fun applyCustomTheme(theme: CustomThemeConfig) {
        viewModelScope.launch {
            preferencesRepository.saveCustomTheme(theme, setAsActive = true)
            _actionToastMessage.value = "Tema activo: ${theme.name}"
        }
    }

    fun deleteCustomTheme(themeId: String) {
        viewModelScope.launch { preferencesRepository.deleteCustomTheme(themeId) }
    }

    fun importCustomThemeJson(rawJson: String): Boolean {
        val parsed = CustomThemeConfig.fromJsonString(rawJson, forceNewId = true) ?: return false
        saveAndApplyCustomTheme(parsed)
        return true
    }

    fun restoreOriginalAppearance() {
        viewModelScope.launch {
            preferencesRepository.restoreOriginalAppearanceDefaults()
            _actionToastMessage.value = "Apariencia original restaurada"
        }
    }

    fun updateHomeLayoutConfig(config: HomeLayoutConfig) {
        viewModelScope.launch { preferencesRepository.saveHomeLayoutConfig(config) }
    }

    fun saveHomeLayoutPreset(name: String) {
        val clean = name.trim().take(36)
        if (clean.isBlank()) return
        val preset = HomeLayoutPreset(
            id = "home_${System.currentTimeMillis()}",
            name = clean,
            config = homeLayoutConfig.value
        )
        viewModelScope.launch {
            preferencesRepository.saveHomeLayoutPreset(preset)
            _actionToastMessage.value = "Diseño de inicio guardado: $clean"
        }
    }

    fun applyHomeLayoutPreset(preset: HomeLayoutPreset) {
        viewModelScope.launch {
            preferencesRepository.saveHomeLayoutConfig(preset.config)
            _actionToastMessage.value = "Diseño aplicado: ${preset.name}"
        }
    }

    fun deleteHomeLayoutPreset(presetId: String) {
        viewModelScope.launch { preferencesRepository.deleteHomeLayoutPreset(presetId) }
    }

    fun resetHomeLayoutDefault() {
        viewModelScope.launch {
            preferencesRepository.saveHomeLayoutConfig(HomeLayoutConfig.defaultConfig())
            _actionToastMessage.value = "Pantalla de Inicio restaurada"
        }
    }

    // Ritmo Local 2.0: Extreme Visual Style, Player Designer, Recap/Wrapped/History, Profile & Smart Organizer
    fun saveExtremeVisualStyle(config: ExtremeVisualStyleConfig) {
        viewModelScope.launch {
            ritmo2PrefsRepo.saveExtremeVisualStyle(config.normalized())
            _actionToastMessage.value = "Estilo visual extremo aplicado"
        }
    }

    fun restoreExtremeVisualDefaults() {
        viewModelScope.launch {
            ritmo2PrefsRepo.restoreExtremeVisualDefaults()
            _actionToastMessage.value = "Estilo visual extremo restaurado"
        }
    }

    fun savePlayerDesignerConfig(config: PlayerDesignerConfig) {
        viewModelScope.launch {
            ritmo2PrefsRepo.savePlayerDesignerConfig(config.normalized())
            _actionToastMessage.value = "Diseño del reproductor guardado"
        }
    }

    fun restorePlayerDesignerDefaults() {
        viewModelScope.launch {
            ritmo2PrefsRepo.restorePlayerDesignerDefaults()
            _actionToastMessage.value = "Diseño del reproductor restaurado"
        }
    }

    fun saveMusicalProfileConfig(config: MusicalProfileConfig) {
        viewModelScope.launch {
            ritmo2PrefsRepo.saveMusicalProfileConfig(config)
            _actionToastMessage.value = "Perfil musical guardado"
        }
    }

    fun selectWrappedYear(year: Int) {
        _selectedWrappedYear.value = year
    }

    fun saveWrappedSnapshot(year: Int) {
        viewModelScope.launch {
            val report = StatsAnalyticsEngine.computeWrappedReport(
                events = localAllEventsFlow.value,
                year = year,
                favoriteGenresByTrackKey = _localTracks.value.associate { it.trackKey to it.genre }
            )
            val summaryJson = JSONObject().apply {
                put("year", report.year)
                put("totalSeconds", report.totalSeconds)
                put("totalPlays", report.totalPlays)
                put("topArtist", report.topArtist?.artistName.orEmpty())
                put("topSong", report.topSong?.trackTitle.orEmpty())
                put("topAlbum", report.topAlbum?.albumName.orEmpty())
            }.toString()
            statsRepository.saveWrappedSnapshot(year, summaryJson)
            _actionToastMessage.value = "Resumen anual Wrapped $year guardado sin internet"
        }
    }

    fun regenerateAmbientMix(category: AmbientMoodCategory) {
        _ambientMixSeed.value = System.nanoTime()
        _actionToastMessage.value = "Mezcla '${category.displayName}' actualizada"
    }

    fun saveAmbientMixAsCustomPlaylist(mix: GeneratedAmbientMix) {
        if (mix.tracks.isEmpty()) return
        viewModelScope.launch {
            val existing = preferencesRepository.customPlaylists.first()
            val cleanName = mix.title.trim().take(40)
            val updated = existing.filterNot { it.name.equals(cleanName, ignoreCase = true) } +
                CustomPlaylist(
                    id = "ambient_${mix.category.id}_${System.currentTimeMillis()}",
                    name = cleanName,
                    trackKeys = mix.tracks.map { it.trackKey }
                )
            preferencesRepository.saveCustomPlaylists(updated)
            ritmo2PrefsRepo.saveAmbientMix(
                SavedAmbientMix(
                    id = "mix_${mix.category.id}",
                    categoryId = mix.category.id,
                    name = cleanName,
                    trackKeys = mix.tracks.map { it.trackKey },
                    isFavorite = true
                )
            )
            _actionToastMessage.value = "Mezcla '$cleanName' guardada en Mis Playlists"
        }
    }

    fun saveTrackMetadataOverride(
        trackKey: String,
        title: String,
        artist: String,
        album: String,
        genre: String
    ) {
        viewModelScope.launch {
            statsRepository.saveMetadataOverride(
                trackKey = trackKey,
                title = title,
                artist = artist,
                album = album,
                genre = genre
            )
            refreshLocalLibrary()
            _actionToastMessage.value = "Metadatos locales actualizados"
        }
    }

    fun clearTrackMetadataOverride(trackKey: String) {
        viewModelScope.launch {
            statsRepository.clearMetadataOverride(trackKey)
            refreshLocalLibrary()
            _actionToastMessage.value = "Metadatos originales restaurados"
        }
    }

    fun hideDuplicateTrack(track: LocalTrack, hide: Boolean) {
        viewModelScope.launch {
            statsRepository.setDuplicateHidden(track, hide)
            refreshLocalLibrary()
            _actionToastMessage.value = if (hide) {
                "Duplicado ocultado de la biblioteca (archivo intacto)"
            } else {
                "Pista restaurada en la biblioteca"
            }
        }
    }

    fun exportStatsJsonToUri(destUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = statsRepository.exportStatsJson()
                appContext.contentResolver.openOutputStream(destUri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el destino para escribir JSON")
            }.fold(
                onSuccess = { _actionToastMessage.value = "Estadísticas exportadas en formato JSON" },
                onFailure = { _actionToastMessage.value = it.localizedMessage ?: "Error al exportar JSON" }
            )
        }
    }

    fun clearAllStatsConfirmed() {
        viewModelScope.launch {
            statsRepository.clearAllListeningHistory()
            _pendingSeconds.value = 0L
            _sessionSeconds.value = 0L
            _actionToastMessage.value = "Historial y métricas locales eliminados"
        }
    }

    fun playTrackByKey(trackKey: String) {
        val tracks = _localTracks.value
        val idx = tracks.indexOfFirst { it.trackKey == trackKey }
        if (idx >= 0) {
            playbackManager.playQueue(tracks, idx, 0L)
        } else {
            _actionToastMessage.value = "Esa pista ya no está en la biblioteca local"
        }
    }

    fun applyWallpaperFromUri(uri: Uri) {
        viewModelScope.launch {
            val saved = coverArtRepository.saveWallpaperFromUri(uri)
            if (saved != null) {
                preferencesRepository.setWallpaperUri(saved)
                _actionToastMessage.value = "Fondo de pantalla actualizado"
            }
        }
    }
    fun clearWallpaper() {
        viewModelScope.launch {
            coverArtRepository.clearWallpaper()
            preferencesRepository.setWallpaperUri(null)
            _actionToastMessage.value = "Fondo personalizado eliminado"
        }
    }
    fun setAnimationsEnabled(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setAnimationsEnabled(enabled) } }
    fun setHideShortWhatsAppAudio(hide: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHideShortWhatsAppAudio(hide)
            refreshLocalLibrary()
        }
    }
    fun setAutoDownloadArtwork(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoDownloadArtwork(enabled)
            if (enabled) downloadMissingArtworkInternal(_localTracks.value)
        }
    }
    fun setPlayAlongOtherApps(enabled: Boolean) { viewModelScope.launch { preferencesRepository.setPlayAlongOtherApps(enabled) } }
    fun updateNotificationOptions(showArt: Boolean, showFav: Boolean, showMode: Boolean) {
        playbackManager.setNotificationArtworkEnabled(showArt)
        viewModelScope.launch { preferencesRepository.setNotificationOptions(showArt, showFav, showMode) }
    }
    fun setScheduledMixReminders(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setScheduledMixReminders(enabled)
            PlaylistReminderScheduler.updateSchedule(appContext, enabled)
        }
    }
    fun sendTestMixReminder() {
        PlaylistReminderScheduler.showReminderNotification(appContext, TimeOfDaySlot.current())
    }

    fun setChatPushNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setChatPushNotificationsEnabled(enabled)
            _actionToastMessage.value = if (enabled) {
                "Alertas push del Chat Global activadas"
            } else {
                "Alertas push del Chat Global silenciadas"
            }
        }
    }

    fun setChatPushOnlyActiveRegion(onlyActive: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setChatPushOnlyActiveRegion(onlyActive)
            _actionToastMessage.value = if (onlyActive) {
                "Notificaciones solo para tu sala activa (${appSettings.value.preferredChatRegion.displayName})"
            } else {
                "Notificaciones activadas para todas las salas del Chat Global"
            }
        }
    }

    fun sendTestChatPushNotification() {
        val currentSettings = appSettings.value
        val allowed = ChatNotificationHelper.shouldDisplayChatPushNotification(
            chatPushEnabled = currentSettings.chatPushNotificationsEnabled,
            onlyActiveRegion = currentSettings.chatPushOnlyActiveRegion,
            preferredRegionId = currentSettings.preferredChatRegion.id,
            incomingRegionId = currentSettings.preferredChatRegion.id,
            incomingSenderId = "test_remote_sender",
            currentUserId = currentUserId
        )
        if (!allowed) {
            _actionToastMessage.value = "Alertas del chat silenciadas según tu configuración."
            return
        }
        val shown = ChatNotificationHelper.showChatMessageNotification(
            context = appContext,
            senderName = "Ritmo Comunidad",
            messageText = "Nuevo mensaje en ${currentSettings.preferredChatRegion.displayName}: ¡escucha este tema recomendado!",
            regionId = currentSettings.preferredChatRegion.id,
            messageId = "test_push_${System.currentTimeMillis()}",
            recommendedTrackTitle = playbackState.value.currentTrack?.title ?: "Mix Hi-Fi",
            hasAudioAttachment = false
        )
        if (!shown) {
            _actionToastMessage.value = "Activa el permiso de notificaciones en el sistema para ver alertas push."
        }
    }

    // Offline Multi-Language Voice Control ("Ritmo / Ridmo ...") + Hands-Free Wake Mode
    fun activeVoiceLanguage(): AppLanguage {
        val code = appSettings.value.voiceLanguageCode
        return if (code.isNotBlank()) AppLanguage.fromCode(code) else appSettings.value.appLanguage
    }

    fun setHandsFreeVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setHandsFreeVoiceEnabled(enabled)
        }
    }

    fun setScreenOffVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setScreenOffVoiceEnabled(enabled)
        }
    }

    fun onVoiceForegroundChanged(inForeground: Boolean) {
        voiceRecognizer.setAppForeground(inForeground)
    }

    fun onAppLifecycleForeground(inForeground: Boolean) {
        onVoiceForegroundChanged(inForeground)
        if (inForeground) {
            val settings = appSettings.value
            playbackManager.setNotificationArtworkEnabled(settings.notifShowArtwork)
            playbackManager.ensureConnected(settings.savedShuffleEnabled, settings.savedRepeatMode)
        }
    }

    fun startVoiceRecognition() {
        voiceRecognizer.startListening(activeVoiceLanguage().code)
    }

    fun stopVoiceRecognition() {
        voiceRecognizer.stopListening()
    }

    fun executeVoiceCommandText(commandText: String) {
        if (commandText.isBlank()) return
        handleVoiceHypotheses(listOf(commandText), requireWake = false)
    }

    private fun handleVoiceHypotheses(hypotheses: List<String>, requireWake: Boolean) {
        val parsed = hypotheses
            .asSequence()
            .map { VoiceCommandParser.parse(it, requireWakeWord = requireWake) }
            .firstOrNull { it !is ParsedVoiceCommand.Unknown }
            ?: VoiceCommandParser.parse(hypotheses.firstOrNull().orEmpty(), requireWakeWord = requireWake)

        when (parsed) {
            ParsedVoiceCommand.WakeOnly -> {
                voiceRecognizer.enterWakeCommandWindow()
            }
            ParsedVoiceCommand.NextTrack -> {
                playbackManager.skipNext()
                voiceRecognizer.markCommandExecuted("Voz: siguiente canción", keepPaused = false)
                _actionToastMessage.value = "Voz: siguiente canción"
            }
            ParsedVoiceCommand.PreviousTrack -> {
                playbackManager.skipPrevious()
                voiceRecognizer.markCommandExecuted("Voz: canción anterior", keepPaused = false)
                _actionToastMessage.value = "Voz: canción anterior"
            }
            ParsedVoiceCommand.Pause -> {
                if (playbackState.value.isPlaying) playbackManager.togglePlayPause()
                voiceRecognizer.markCommandExecuted("Voz: reproducción en pausa", keepPaused = true)
            }
            ParsedVoiceCommand.Resume -> {
                if (!playbackState.value.isPlaying) {
                    if (playbackState.value.currentTrack != null) {
                        playbackManager.resumeFromVoice()
                    } else if (_localTracks.value.isNotEmpty()) {
                        playbackManager.playQueue(_localTracks.value, 0, 0L)
                    }
                }
                voiceRecognizer.markCommandExecuted("Voz: reproduciendo música", keepPaused = true)
            }
            ParsedVoiceCommand.VolumeUp -> {
                adjustMusicVolume(raise = true)
                voiceRecognizer.markCommandExecuted("Voz: subiendo volumen", keepPaused = false)
                _actionToastMessage.value = "Voz: subiendo volumen"
            }
            ParsedVoiceCommand.VolumeDown -> {
                adjustMusicVolume(raise = false)
                voiceRecognizer.markCommandExecuted("Voz: bajando volumen", keepPaused = false)
                _actionToastMessage.value = "Voz: bajando volumen"
            }
            ParsedVoiceCommand.ShowLyrics -> {
                val track = playbackState.value.currentTrack
                if (track != null) {
                    loadLyricsForTrack(track)
                    _openLyricsTrackEvent.value = track
                    voiceRecognizer.markCommandExecuted("Voz: abriendo letra de ${track.title}", keepPaused = false)
                } else {
                    voiceRecognizer.completeUnrecognizedVoiceAttempt("Reproduce una canción primero para ver su letra.")
                }
            }
            is ParsedVoiceCommand.PlayQuery -> {
                val queue = VoiceCommandParser.findBestTrackQueue(
                    tracks = _localTracks.value,
                    songOrArtistQuery = parsed.songOrArtistQuery,
                    artistHint = parsed.artistHint
                )
                if (queue.isNotEmpty()) {
                    playbackManager.playQueue(queue, 0, 0L)
                    val first = queue.first()
                    val feedback = "Voz: reproduciendo '${first.title}' de ${first.artist}"
                    voiceRecognizer.markCommandExecuted(feedback, keepPaused = true)
                    _actionToastMessage.value = feedback
                } else {
                    val hint = listOfNotNull(parsed.songOrArtistQuery, parsed.artistHint).joinToString(" · ")
                    voiceRecognizer.completeUnrecognizedVoiceAttempt("No encontré '$hint' en tu música local.")
                }
            }
            is ParsedVoiceCommand.Unknown -> {
                if (!requireWake) {
                    voiceRecognizer.completeUnrecognizedVoiceAttempt(
                        "Escuché '${parsed.rawText}'. Prueba: \"siguiente canción\", \"pausar\" o \"subir volumen\"."
                    )
                }
            }
        }
    }

    // Intelligent Diagnostic Center & Playback Protection
    fun noteRouteNavigationProtected(route: String) {
        AppDiagnosticsManager.noteNavigationProtected(route, playbackState.value.isPlaying)
    }

    fun showToastMessage(message: String) {
        _actionToastMessage.value = message
    }

    private fun buildDiagnosticAuditInput(): DiagnosticAuditInput = DiagnosticAuditInput(
        playbackState = playbackState.value,
        equalizerState = equalizerState.value,
        sleepTimerState = sleepTimerState.value,
        voiceState = voiceState.value,
        settings = appSettings.value,
        localTracks = _localTracks.value,
        customPlaylists = customPlaylists.value,
        isSignedIn = currentUserId != null,
        cloudSyncError = if (cloudStatus.value.isError) cloudStatus.value.statusMessage else null,
        lastCloudSyncMs = cloudStatus.value.lastSyncedAtMs,
        totalPlaybackSeconds = liveTotalPlaybackSeconds.value,
        playbackEnginePrefs = playbackEnginePrefs.value,
        savedDeviceProfilesCount = deviceSoundProfiles.value.size
    )

    fun runDiagnosticAnalysis() {
        viewModelScope.launch(Dispatchers.Default) {
            AppDiagnosticsManager.runFullAnalysis(appContext, buildDiagnosticAuditInput())
        }
    }

    fun runDiagnosticSelfTest() {
        viewModelScope.launch(Dispatchers.Default) {
            AppDiagnosticsManager.runFunctionalSelfTest(appContext, buildDiagnosticAuditInput())
        }
    }

    fun setDiagnosticModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDiagnosticModeEnabled(enabled)
            AppDiagnosticsManager.setDiagnosticModeEnabled(enabled)
            _actionToastMessage.value = if (enabled) {
                "Modo de diagnóstico técnico activado"
            } else {
                "Modo de diagnóstico técnico desactivado"
            }
        }
    }

    fun executeDiagnosticRepair(
        issue: DiagnosticIssue,
        onRequestPermission: (String) -> Unit,
        onOpenBatterySettings: () -> Unit
    ) {
        viewModelScope.launch {
            val (_, message) = AppDiagnosticsManager.executeSafeRepair(
                context = appContext,
                issue = issue,
                callbacks = DiagnosticRepairCallbacks(
                    reconnectPlayer = {
                        playbackManager.ensureConnected(
                            initialShuffle = appSettings.value.savedShuffleEnabled,
                            initialRepeat = appSettings.value.savedRepeatMode
                        )
                    },
                    restoreLastPlayback = {
                        val snap = AppDiagnosticsManager.uiState.value.playbackProtection
                        val match = _localTracks.value.firstOrNull { it.trackKey == snap.lastProtectedTrackKey }
                        if (match != null) {
                            playbackManager.playQueue(listOf(match), 0, snap.lastProtectedPositionMs)
                            true
                        } else false
                    },
                    reapplyEqualizer = {
                        viewModelScope.launch {
                            val eq = preferencesRepository.equalizerPrefs.first()
                            AudioEffectsController.loadStoredPrefs(eq)
                        }
                    },
                    retryCloudSync = { saveProgressNow() },
                    refreshLibrary = { refreshLocalLibrary() },
                    resetSleepTimer = { cancelSleepTimer() },
                    cleanOrphanPlaylistRefs = {
                        val validKeys = _localTracks.value.map { it.trackKey }.toHashSet()
                        var removed = 0
                        if (validKeys.isNotEmpty()) {
                            val current = preferencesRepository.customPlaylists.first()
                            val cleaned = current.map { pl ->
                                val kept = pl.trackKeys.filter { it in validKeys }
                                removed += (pl.trackKeys.size - kept.size)
                                pl.copy(trackKeys = kept)
                            }
                            if (removed > 0) preferencesRepository.saveCustomPlaylists(cleaned)
                        }
                        removed
                    },
                    requestPermission = onRequestPermission,
                    openBatterySettings = onOpenBatterySettings
                )
            )
            _actionToastMessage.value = message
            AppDiagnosticsManager.runFullAnalysis(appContext, buildDiagnosticAuditInput())
        }
    }

    fun dismissDiagnosticIssue(issueId: String) {
        AppDiagnosticsManager.markIssueDismissed(issueId)
    }

    fun clearDiagnosticHistory() {
        AppDiagnosticsManager.clearHistoryAndLogs()
        _actionToastMessage.value = "Historial de diagnóstico limpiado"
    }

    private fun adjustMusicVolume(raise: Boolean) {
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val direction = if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        runCatching {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)
        }
    }

    // Song Lyrics System (Offline + LRC Synced + Embedded + Online Search)
    fun loadLyricsForTrack(track: LocalTrack) {
        viewModelScope.launch {
            _lyricsStatusMessage.value = null
            if (_lyricsByTrackKey.value.containsKey(track.trackKey)) return@launch
            _isLoadingLyrics.value = true
            val found = lyricsRepository.loadOfflineLyrics(track)
            _isLoadingLyrics.value = false
            if (found != null) {
                _lyricsByTrackKey.value = _lyricsByTrackKey.value + (track.trackKey to found)
            }
        }
    }

    fun searchOnlineLyrics(track: LocalTrack) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            _lyricsStatusMessage.value = "Buscando letra en línea…"
            val result = lyricsRepository.fetchOnlineLyrics(track)
            _isLoadingLyrics.value = false
            result.fold(
                onSuccess = { loaded ->
                    _lyricsByTrackKey.value = _lyricsByTrackKey.value + (track.trackKey to loaded)
                    _lyricsStatusMessage.value = "Letra descargada y guardada para usar sin internet."
                },
                onFailure = { err ->
                    _lyricsStatusMessage.value = err.localizedMessage ?: "No se pudo encontrar la letra."
                }
            )
        }
    }

    fun saveCustomLyrics(track: LocalTrack, rawText: String) {
        viewModelScope.launch {
            val saved = lyricsRepository.saveCustomLyrics(track, rawText)
            if (saved != null) {
                _lyricsByTrackKey.value = _lyricsByTrackKey.value + (track.trackKey to saved)
                _lyricsStatusMessage.value = "Letra guardada sin internet."
            } else {
                _lyricsByTrackKey.value = _lyricsByTrackKey.value - track.trackKey
                _lyricsStatusMessage.value = "Letra eliminada."
            }
        }
    }

    fun importLyricsFromUri(track: LocalTrack, uri: Uri) {
        viewModelScope.launch {
            _isLoadingLyrics.value = true
            val result = lyricsRepository.importLyricsFromUri(track, uri)
            _isLoadingLyrics.value = false
            result.fold(
                onSuccess = { loaded ->
                    _lyricsByTrackKey.value = _lyricsByTrackKey.value + (track.trackKey to loaded)
                    _lyricsStatusMessage.value = "Letra importada y guardada offline."
                },
                onFailure = { err ->
                    _lyricsStatusMessage.value = err.localizedMessage ?: "No se pudo leer el archivo."
                }
            )
        }
    }

    fun clearSavedLyrics(track: LocalTrack) {
        viewModelScope.launch {
            lyricsRepository.clearLyrics(track.trackKey)
            _lyricsByTrackKey.value = _lyricsByTrackKey.value - track.trackKey
            _lyricsStatusMessage.value = "Letra eliminada."
        }
    }

    fun consumeOpenLyricsEvent() {
        _openLyricsTrackEvent.value = null
    }

    // Google Drive Backup / Restore
    fun exportBackupZip(destUri: Uri) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val result = MusicBackupManager.exportBackupZip(
                context = appContext,
                destUri = destUri,
                tracks = _localTracks.value,
                customPlaylists = customPlaylists.value,
                favoriteTrackKeys = favoriteTrackKeys.value
            )
            _isBusyBackup.value = false
            _backupStatus.value = result.fold(
                onSuccess = { "Respaldo listo: ${it.exportedTracks} canciones, ${it.exportedPlaylists} playlists y ${it.exportedCovers} carátulas." },
                onFailure = { it.localizedMessage ?: "No se pudo exportar la copia." }
            )
        }
    }

    fun restoreBackupZip(sourceUri: Uri) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val result = MusicBackupManager.restoreBackupZip(appContext, sourceUri, preferencesRepository)
            _isBusyBackup.value = false
            _backupStatus.value = result.fold(
                onSuccess = {
                    refreshLocalLibrary()
                    "Restaurado: ${it.restoredTracks} pistas y ${it.restoredPlaylists} playlists."
                },
                onFailure = { it.localizedMessage ?: "No se pudo restaurar el respaldo." }
            )
        }
    }

    // Regional Global Chat
    fun selectChatRegion(region: ChatRegion) {
        _selectedChatRegion.value = region
        viewModelScope.launch { preferencesRepository.setPreferredChatRegion(region) }
    }

    fun setCustomChatUsername(username: String) {
        val cleaned = username.trim().take(40)
        viewModelScope.launch {
            preferencesRepository.setCustomChatUsername(cleaned)
            _chatStatusMessage.value = if (cleaned.isNotBlank()) {
                "Tu nombre en el chat ahora es: $cleaned"
            } else {
                "Se restauró el nombre de tu cuenta en el chat."
            }
        }
    }

    fun updateChatAvatarFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = coverArtRepository.saveProfileAvatarFromUri(uri)
            if (saved != null) {
                val (localPath, base64) = saved
                preferencesRepository.setChatAvatar(localPath, base64)
                _chatStatusMessage.value = "Foto de perfil actualizada."
                _actionToastMessage.value = "Foto de perfil actualizada"
            } else {
                _chatStatusMessage.value = "No se pudo procesar la foto seleccionada."
            }
        }
    }

    fun clearChatAvatar() {
        viewModelScope.launch(Dispatchers.IO) {
            coverArtRepository.clearProfileAvatar()
            preferencesRepository.setChatAvatar(null, "")
            _chatStatusMessage.value = "Foto de perfil eliminada."
        }
    }

    fun attachAudioForChat(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val (name, reportedSize) = MusicBackupManager.resolveFileSizeAndName(appContext, uri)
            val maxReadable = AudioClipTrimmer.formatSizeReadable(AudioClipTrimmer.MAX_RAW_CLIP_BYTES.toLong())
            if (reportedSize > AudioClipTrimmer.MAX_RAW_CLIP_BYTES.toLong()) {
                _chatStatusMessage.value = "El archivo supera $maxReadable; usa 'Recortar' para compartir hasta 5 minutos."
                return@launch
            }
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                _chatStatusMessage.value = "No se pudo leer el archivo seleccionado."
                return@launch
            }
            if (bytes.size > AudioClipTrimmer.MAX_RAW_CLIP_BYTES) {
                _chatStatusMessage.value = "El archivo pesa ${AudioClipTrimmer.formatSizeReadable(bytes.size.toLong())} (máximo $maxReadable). Usa 'Recortar'."
                return@launch
            }
            val mime = appContext.contentResolver.getType(uri) ?: "audio/mpeg"
            _pendingChatAttachment.value = ChatAttachmentDraft(name, mime, bytes)
            _chatStatusMessage.value = "Audio adjunto listo (${AudioClipTrimmer.formatSizeReadable(bytes.size.toLong())})."
        }
    }

    fun trimAndAttachAudioForChat(
        sourceUri: Uri,
        title: String,
        artist: String,
        startMs: Long,
        endMs: Long,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isTrimmingChatAudio.value = true
            val result = AudioClipTrimmer.trimAudioToM4aClip(
                context = appContext,
                sourceUri = sourceUri,
                titleHint = title,
                startMs = startMs,
                endMs = endMs
            )
            _isTrimmingChatAudio.value = false
            result.fold(
                onSuccess = { trimmed ->
                    _pendingChatAttachment.value = ChatAttachmentDraft(
                        fileName = trimmed.fileName,
                        mimeType = trimmed.mimeType,
                        bytes = trimmed.bytes
                    )
                    _chatStatusMessage.value = "Audio recortado (${trimmed.durationMs / 1000}s · ${AudioClipTrimmer.formatSizeReadable(trimmed.bytes.size.toLong())}) listo para enviar."
                    onComplete()
                },
                onFailure = { err ->
                    _chatStatusMessage.value = err.localizedMessage ?: "No se pudo recortar el audio."
                }
            )
        }
    }

    fun clearPendingChatAttachment() { _pendingChatAttachment.value = null }

    fun sendRegionalChatMessage(senderName: String, text: String, recTitle: String, recArtist: String) {
        val region = _selectedChatRegion.value
        val draft = _pendingChatAttachment.value
        val effectiveSender = appSettings.value.customChatUsername.trim().ifBlank { senderName }
        viewModelScope.launch {
            if (draft != null && draft.bytes.size > 400_000) {
                _chatStatusMessage.value = "Subiendo audio (${AudioClipTrimmer.formatSizeReadable(draft.bytes.size.toLong())})…"
            }
            val res = cloudSyncRepository.sendRegionalChatMessage(
                region = region,
                senderName = effectiveSender,
                messageText = text,
                recommendedTrackTitle = recTitle,
                recommendedArtist = recArtist,
                audioFileName = draft?.fileName.orEmpty(),
                audioMimeType = draft?.mimeType.orEmpty(),
                audioBytes = draft?.bytes ?: ByteArray(0),
                senderAvatarBase64 = appSettings.value.chatAvatarBase64
            )
            res.fold(
                onSuccess = {
                    _pendingChatAttachment.value = null
                    _chatStatusMessage.value = null
                },
                onFailure = {
                    _chatStatusMessage.value = it.localizedMessage ?: "No se pudo enviar el mensaje."
                }
            )
        }
    }

    fun downloadChatAudio(message: RegionalChatMessage) {
        if (!message.hasDownloadableAudio()) return
        viewModelScope.launch(Dispatchers.IO) {
            _chatStatusMessage.value = "Descargando audio compartido…"
            val bytesResult = cloudSyncRepository.fetchAudioBytesForMessage(_selectedChatRegion.value, message)
            bytesResult.fold(
                onSuccess = { decoded ->
                    try {
                        val dir = LocalMusicRepository.getSharedDownloadsDir(appContext)
                        val safeName = message.audioFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                            .ifBlank { "shared_${System.currentTimeMillis()}.m4a" }
                        val destFile = File(dir, safeName)
                        FileOutputStream(destFile).use { it.write(decoded) }
                        refreshLocalLibrary()
                        _chatStatusMessage.value = "Guardado para siempre en tu biblioteca offline: $safeName"
                    } catch (e: Exception) {
                        _chatStatusMessage.value = "Error al guardar audio: ${e.localizedMessage}"
                    }
                },
                onFailure = { err ->
                    _chatStatusMessage.value = err.localizedMessage ?: "Error al descargar audio."
                }
            )
        }
    }

    fun deleteRegionalChatMessage(message: RegionalChatMessage) {
        viewModelScope.launch {
            cloudSyncRepository.deleteRegionalChatMessage(_selectedChatRegion.value, message.id)
        }
    }

    fun clearActionToast() { _actionToastMessage.value = null }

    fun resumeFromCloudProgress(progress: PlaybackProgress) {
        val tracks = _localTracks.value
        val matchIndex = tracks.indexOfFirst { it.trackKey == progress.trackKey || it.contentUri == progress.trackUri }
        if (matchIndex >= 0) {
            _selectedSlot.value = TimeOfDaySlot.fromWire(progress.timeOfDaySlot)
            playbackManager.playQueue(tracks, matchIndex, progress.positionMs)
        } else if (progress.trackUri.isNotBlank()) {
            val fallbackTrack = LocalTrack(
                id = progress.trackUri.hashCode().toLong(),
                trackKey = progress.trackKey,
                title = progress.trackTitle,
                artist = progress.artistName,
                album = progress.albumName,
                albumId = progress.albumId,
                durationMs = progress.durationMs,
                contentUri = progress.trackUri,
                albumArtUri = if (progress.albumId > 0L) "content://media/external/audio/albumart/${progress.albumId}" else null,
                dateAdded = 0L
            )
            _selectedSlot.value = TimeOfDaySlot.fromWire(progress.timeOfDaySlot)
            playbackManager.playQueue(listOf(fallbackTrack), 0, progress.positionMs)
        } else {
            _cloudStatus.value = CloudStatusUi(statusMessage = "No se encontró el archivo '${progress.trackTitle}'.", isError = true)
        }
    }

    fun togglePlayPause() = playbackManager.togglePlayPause()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun skipNext() = playbackManager.skipNext()
    fun skipPrevious() = playbackManager.skipPrevious()
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()

    fun saveProgressNow() {
        val track = playbackState.value.currentTrack
        val pos = playbackState.value.positionMs
        if (track == null) {
            _cloudStatus.value = CloudStatusUi(statusMessage = "Reproduce una canción primero para guardar tu progreso.")
            return
        }
        viewModelScope.launch { flushToCloud(track, pos, showSuccessBanner = true) }
    }

    suspend fun flushBeforeSignOut() {
        withContext(NonCancellable) {
            val current = playbackState.value.currentTrack
            if (current != null) {
                flushToCloud(current, playbackState.value.positionMs, showSuccessBanner = false)
            }
            if (currentUserId != null) {
                val cachedToken = preferencesRepository.cachedFcmToken.first()
                if (cachedToken.isNotBlank()) {
                    cloudSyncRepository.removeFcmToken(currentUserId, cachedToken)
                }
            }
            playbackManager.stopAndRelease()
        }
    }

    private fun generateAllSlotPlaylists(tracks: List<LocalTrack>) {
        val now = System.nanoTime()
        _playlistsBySlot.value = mapOf(
            TimeOfDaySlot.MORNING to localMusicRepository.generatePlaylistForSlot(tracks, TimeOfDaySlot.MORNING, now + 1),
            TimeOfDaySlot.AFTERNOON to localMusicRepository.generatePlaylistForSlot(tracks, TimeOfDaySlot.AFTERNOON, now + 2),
            TimeOfDaySlot.NIGHT to localMusicRepository.generatePlaylistForSlot(tracks, TimeOfDaySlot.NIGHT, now + 3)
        )
    }

    private fun handleListeningTick(track: LocalTrack, deltaSec: Long, positionMs: Long) {
        val restartedSameTrack = track.trackKey == activeSessionTrackKey &&
            lastObservedPosMs > 15_000L &&
            positionMs < 3_000L
        if (track.trackKey != activeSessionTrackKey || restartedSameTrack) {
            activeSessionTrackKey = track.trackKey
            activeSessionSeconds = 0L
            activeSessionQualifiedRecorded = false
        }
        lastObservedPosMs = positionMs
        activeSessionSeconds += deltaSec
        val qualifiedThreshold = StatsAnalyticsEngine.requiredSecondsForQualifiedPlay(track.durationMs)
        val justQualified = !activeSessionQualifiedRecorded && activeSessionSeconds >= qualifiedThreshold
        if (justQualified) {
            activeSessionQualifiedRecorded = true
        }
        val activeSlot = _selectedSlot.value
        viewModelScope.launch {
            statsRepository.recordListening(
                track = track,
                deltaSeconds = deltaSec,
                qualifiedPlay = justQualified,
                slot = activeSlot
            )
            if (justQualified && currentUserId != null && musicalProfileConfig.value.statsCloudSyncEnabled) {
                cloudSyncRepository.recordListeningTime(
                    userId = currentUserId,
                    monthKey = currentMonthKey(),
                    track = track,
                    deltaSeconds = 0L,
                    incrementPlayCount = true,
                    slot = activeSlot
                )
            }
        }

        _sessionSeconds.value += deltaSec
        _pendingSeconds.value += deltaSec
        if (_pendingSeconds.value >= AUTOSAVE_INTERVAL_SECONDS) {
            viewModelScope.launch { flushToCloud(track, positionMs, showSuccessBanner = false) }
        }
    }

    private suspend fun recordPlayAndSaveProgress(track: LocalTrack, positionMs: Long, incrementPlay: Boolean) {
        val delta = _pendingSeconds.value
        _pendingSeconds.value = 0L
        val slot = _selectedSlot.value

        if (currentUserId == null) {
            preferencesRepository.recordOfflineListening(track, positionMs, delta, slot)
            pendingNewPlayTrack = null
            return
        }

        val cloudBase = (cloudPlaybackProgress.value as? UiState.Success)?.data?.totalPlaybackSeconds ?: lastSyncedTotalSeconds
        val newTotal = cloudBase + delta
        lastSyncedTotalSeconds = newTotal
        val monthKey = currentMonthKey()

        if (musicalProfileConfig.value.statsCloudSyncEnabled && (delta > 0L || incrementPlay)) {
            cloudSyncRepository.recordListeningTime(currentUserId, monthKey, track, delta, incrementPlay, slot)
        }
        cloudSyncRepository.savePlaybackProgress(currentUserId, track, positionMs, newTotal, slot)
        pendingNewPlayTrack = null
    }

    private suspend fun flushToCloud(track: LocalTrack, positionMs: Long, showSuccessBanner: Boolean) {
        val delta = _pendingSeconds.value
        _pendingSeconds.value = 0L
        val slot = _selectedSlot.value
        pendingNewPlayTrack = null

        if (currentUserId == null) {
            preferencesRepository.recordOfflineListening(track, positionMs, delta, slot)
            if (showSuccessBanner) {
                _cloudStatus.value = CloudStatusUi(statusMessage = "Progreso guardado localmente (Modo Offline).")
            }
            return
        }

        _cloudStatus.value = _cloudStatus.value.copy(isSyncing = true)
        val cloudBase = (cloudPlaybackProgress.value as? UiState.Success)?.data?.totalPlaybackSeconds ?: lastSyncedTotalSeconds
        val newTotal = cloudBase + delta
        lastSyncedTotalSeconds = newTotal
        val monthKey = currentMonthKey()

        val statsResult = if (musicalProfileConfig.value.statsCloudSyncEnabled && delta > 0L) {
            cloudSyncRepository.recordListeningTime(currentUserId, monthKey, track, delta, false, slot)
        } else Result.success(Unit)

        val progressResult = cloudSyncRepository.savePlaybackProgress(currentUserId, track, positionMs, newTotal, slot)
        val failure = progressResult.exceptionOrNull() ?: statsResult.exceptionOrNull()
        if (failure != null) {
            _pendingSeconds.value += delta
            _cloudStatus.value = CloudStatusUi(
                isSyncing = false,
                lastSyncedAtMs = _cloudStatus.value.lastSyncedAtMs,
                statusMessage = "Guardado localmente; se sincronizará al volver la conexión.",
                isError = true
            )
            AppDiagnosticsManager.recordException(
                module = DiagnosticModule.CLOUD_SYNC,
                title = "Sincronización en la nube pendiente",
                throwable = failure,
                severity = DiagnosticSeverity.LOW,
                plainExplanation = "Los datos de reproducción se guardaron localmente porque no se pudo sincronizar con la nube en este momento.",
                proposedSolution = "Reintenta cuando tengas conexión estable desde el Centro de diagnóstico.",
                repairAction = RepairActionId.RETRY_CLOUD_SYNC
            )
        } else {
            _cloudStatus.value = CloudStatusUi(
                isSyncing = false,
                lastSyncedAtMs = System.currentTimeMillis(),
                statusMessage = if (showSuccessBanner) "Progreso y tiempo guardados en la nube." else null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        deviceAudioProfileManager.stopMonitoring()
        voiceRecognizer.release()
        playbackManager.stopAndRelease()
    }

    private companion object {
        const val TAG = "MusicViewModel"
        const val AUTOSAVE_INTERVAL_SECONDS = 15L
    }
}
