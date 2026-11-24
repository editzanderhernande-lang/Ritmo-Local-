package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.CloudSyncRepository
import com.example.data.CoverArtRepository
import com.example.data.LocalMusicRepository
import com.example.data.MusicBackupManager
import com.example.data.Ritmo2PreferencesRepository
import com.example.data.UserPreferencesRepository
import com.example.model.AlbumPlaylist
import com.example.model.AppSettingsPrefs
import com.example.model.ChatRegion
import com.example.model.CustomThemeConfig
import com.example.model.ExtremeVisualStyleConfig
import com.example.model.HomeShortcutType
import com.example.model.LocalTrack
import com.example.model.TimeOfDaySlot
import com.example.model.UiState
import com.example.model.UiStyleMode
import com.example.model.UserAppearancePrefs
import com.example.model.currentMonthKey
import com.example.notifications.ChatNotificationHelper
import com.example.notifications.PlaylistReminderScheduler
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutFromApp
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CustomThemeBuilderSheet
import com.example.ui.components.ExtremeVisualEditorSheet
import com.example.ui.components.HomeCustomizationSheet
import com.example.ui.components.ImmersiveVisualOverlay
import com.example.ui.components.InPlayerEqualizerSheet
import com.example.ui.components.LyricsSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PlaybackProControlsSheet
import com.example.ui.components.PlaybackQueueSheet
import com.example.ui.components.PlayerCustomizationSheet
import com.example.ui.components.PlayerDesignerSheet
import com.example.ui.components.SleepTimerSheet
import com.example.ui.components.SmartTvCastSheet
import com.example.ui.components.ThemeCustomizationSheet
import com.example.ui.components.VoiceCommandSheet
import com.example.ui.i18n.LocalExtraStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.extraStringsFor
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.AdvancedNowPlayingScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryOrganizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.MixesScreen
import com.example.ui.screens.MonthlyStatsScreen
import com.example.ui.screens.MusicalHistoryScreen
import com.example.ui.screens.MusicalProfileScreen
import com.example.ui.screens.RecapAndWrappedScreen
import com.example.ui.screens.RegionalChatScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimeCloudScreen
import com.example.ui.screens.TrackActionsCallbacks
import com.example.ui.theme.AppWallpaperBackdrop
import com.example.ui.theme.RitmoLocalTheme
import com.example.ui.viewmodel.MusicViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

const val ROUTE_LIBRARY = "library"
const val ROUTE_MIXES = "mixes"
const val ROUTE_DIAGNOSTICS = "diagnostics"
const val ROUTE_EQUALIZER = "equalizer"
const val ROUTE_TIME_CLOUD = "time_cloud"
const val ROUTE_MONTHLY_STATS = "monthly_stats"
const val ROUTE_RECAP_WRAPPED = "recap_wrapped"
const val ROUTE_MUSICAL_HISTORY = "musical_history"
const val ROUTE_MUSICAL_PROFILE = "musical_profile"
const val ROUTE_LIBRARY_ORGANIZER = "library_organizer"
const val ROUTE_CHAT = "chat"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    private val launchMixSlotFlow = MutableStateFlow<String?>(null)
    private val launchChatRegionFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchMixSlotFlow.value = intent?.getStringExtra(PlaylistReminderScheduler.EXTRA_OPEN_MIX_SLOT)
        launchChatRegionFlow.value = intent?.getStringExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_REGION)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefsRepo = remember(context) { UserPreferencesRepository(context) }
            val ritmo2Prefs = remember(context) { Ritmo2PreferencesRepository(context) }
            LaunchedEffect(Unit) {
                prefsRepo.ensureV22DefaultDesignIfNeeded()
            }
            val appearancePrefs by prefsRepo.appearancePrefs.collectAsStateWithLifecycle(UserAppearancePrefs())
            val extremeVisualStyle by ritmo2Prefs.extremeVisualStyle.collectAsStateWithLifecycle(ExtremeVisualStyleConfig.defaultConfig())
            val customThemes by prefsRepo.customThemes.collectAsStateWithLifecycle(emptyList())
            val activeCustomTheme = remember(appearancePrefs.activeCustomThemeId, customThemes) {
                appearancePrefs.activeCustomThemeId?.let { id -> customThemes.firstOrNull { it.id == id } }
            }
            val appSettings by prefsRepo.appSettingsPrefs.collectAsStateWithLifecycle(AppSettingsPrefs())
            val launchSlot by launchMixSlotFlow.collectAsStateWithLifecycle()
            val launchChatRegion by launchChatRegionFlow.collectAsStateWithLifecycle()

            val hasWallpaper = !appearancePrefs.wallpaperUri.isNullOrBlank()
            CompositionLocalProvider(
                LocalStrings provides stringsFor(appSettings.appLanguage),
                LocalExtraStrings provides extraStringsFor(appSettings.appLanguage)
            ) {
                RitmoLocalTheme(
                    darkTheme = appearancePrefs.isDarkTheme,
                    accentPalette = appearancePrefs.accentPalette,
                    uiStyleMode = appearancePrefs.uiStyleMode,
                    fontStyle = appearancePrefs.fontStyle,
                    fontSizeScale = appearancePrefs.fontSizeScale,
                    hasWallpaper = hasWallpaper,
                    customTheme = activeCustomTheme,
                    extremeStyle = extremeVisualStyle,
                    useDynamicMaterialYou = appearancePrefs.useDynamicMaterialYou
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppWallpaperBackdrop(
                            wallpaperUri = appearancePrefs.wallpaperUri,
                            dimAlpha = appearancePrefs.wallpaperDimAlpha,
                            darkTheme = appearancePrefs.isDarkTheme,
                            uiStyleMode = appearancePrefs.uiStyleMode,
                            accentPalette = appearancePrefs.accentPalette,
                            blurRadiusDp = appearancePrefs.wallpaperBlurDp,
                            customTheme = activeCustomTheme,
                            extremeStyle = extremeVisualStyle
                        )
                        AppNavigation(
                            preferencesRepository = prefsRepo,
                            appearancePrefs = appearancePrefs,
                            launchMixSlotWire = launchSlot,
                            onConsumeLaunchMixSlot = { launchMixSlotFlow.value = null },
                            launchChatRegionId = launchChatRegion,
                            onConsumeLaunchChatRegion = { launchChatRegionFlow.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(PlaylistReminderScheduler.EXTRA_OPEN_MIX_SLOT)?.let {
            launchMixSlotFlow.value = it
        }
        intent.getStringExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_REGION)?.let {
            launchChatRegionFlow.value = it
        }
    }
}

@Composable
fun AppNavigation(
    preferencesRepository: UserPreferencesRepository,
    appearancePrefs: UserAppearancePrefs,
    launchMixSlotWire: String? = null,
    onConsumeLaunchMixSlot: () -> Unit = {},
    launchChatRegionId: String? = null,
    onConsumeLaunchChatRegion: () -> Unit = {},
    auth: FirebaseAuth = Firebase.auth
) {
    val scope = rememberCoroutineScope()
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val appSettings by preferencesRepository.appSettingsPrefs.collectAsStateWithLifecycle(AppSettingsPrefs())
    var showSignInOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) showSignInOverlay = false
    }

    val user = currentUser
    val mustShowAuth = (user == null && !appSettings.guestModeEnabled) || (user == null && showSignInOverlay)
    if (mustShowAuth) {
        if (user == null && showSignInOverlay) {
            BackHandler { showSignInOverlay = false }
        }
        AuthScreen(
            onAuthSuccess = { showSignInOverlay = false },
            onContinueOffline = {
                showSignInOverlay = false
                scope.launch { preferencesRepository.setGuestModeEnabled(true) }
            }
        )
    } else {
        key(user?.uid ?: "offline") {
            RitmoLocalMainShell(
                currentUserId = user?.uid,
                userEmail = user?.email,
                userDisplayName = user?.displayName,
                userPhotoUrl = user?.photoUrl?.toString(),
                preferencesRepository = preferencesRepository,
                appearancePrefs = appearancePrefs,
                launchMixSlotWire = launchMixSlotWire,
                onConsumeLaunchMixSlot = onConsumeLaunchMixSlot,
                launchChatRegionId = launchChatRegionId,
                onConsumeLaunchChatRegion = onConsumeLaunchChatRegion,
                onRequestSignIn = { showSignInOverlay = true }
            )
        }
    }
}

@Composable
private fun RitmoLocalMainShell(
    currentUserId: String?,
    userEmail: String?,
    userDisplayName: String?,
    userPhotoUrl: String?,
    preferencesRepository: UserPreferencesRepository,
    appearancePrefs: UserAppearancePrefs,
    launchMixSlotWire: String?,
    onConsumeLaunchMixSlot: () -> Unit,
    launchChatRegionId: String?,
    onConsumeLaunchChatRegion: () -> Unit,
    onRequestSignIn: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: MusicViewModel = viewModel(
        key = currentUserId ?: "offline",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                MusicViewModel(
                    currentUserId = currentUserId,
                    localMusicRepository = LocalMusicRepository(app.applicationContext),
                    cloudSyncRepository = CloudSyncRepository(app.applicationContext),
                    preferencesRepository = preferencesRepository,
                    coverArtRepository = CoverArtRepository(app.applicationContext),
                    appContext = app.applicationContext
                )
            }
        }
    )

    val audioPermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }
    var pendingTrackForCover by remember { mutableStateOf<LocalTrack?>(null) }
    var pendingAlbumForCover by remember { mutableStateOf<AlbumPlaylist?>(null) }
    var pendingTrimExternalUri by remember { mutableStateOf<Uri?>(null) }
    var pendingEnableHandsFreeOnPermission by remember { mutableStateOf(false) }
    var trackForPlaylistDialog by remember { mutableStateOf<LocalTrack?>(null) }
    var lyricsSheetTrack by remember { mutableStateOf<LocalTrack?>(null) }
    var preselectedChatTrack by remember { mutableStateOf<LocalTrack?>(null) }
    var showCastSheet by remember { mutableStateOf(false) }
    var castTargetTitle by remember { mutableStateOf<String?>(null) }
    var showNowPlayingSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showInPlayerEqSheet by remember { mutableStateOf(false) }
    var showPlayerCustomizeSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showCustomThemeBuilder by remember { mutableStateOf(false) }
    var showHomeCustomizeSheet by remember { mutableStateOf(false) }
    var showExtremeVisualEditor by remember { mutableStateOf(false) }
    var showPlayerDesignerSheet by remember { mutableStateOf(false) }
    var showImmersiveVisualOverlay by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showProControlsSheet by remember { mutableStateOf(false) }
    var mixesSectionJump by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppLifecycleForeground(true)
                Lifecycle.Event.ON_STOP -> viewModel.onAppLifecycleForeground(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasAudioPermission = grants[audioPermission] == true ||
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        viewModel.refreshLocalLibrary()
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (pendingEnableHandsFreeOnPermission) {
                pendingEnableHandsFreeOnPermission = false
                viewModel.setHandsFreeVoiceEnabled(true)
            } else {
                viewModel.startVoiceRecognition()
            }
        } else {
            pendingEnableHandsFreeOnPermission = false
            viewModel.setHandsFreeVoiceEnabled(false)
        }
    }
    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importAudioUris(uris)
    }
    val chatAudioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::attachAudioForChat)
    }
    val trimExternalAudioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingTrimExternalUri = uri
    }
    val lyricsImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = lyricsSheetTrack
        if (uri != null && target != null) {
            viewModel.importLyricsFromUri(target, uri)
        }
    }
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::applyWallpaperFromUri)
    }
    val avatarPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::updateChatAvatarFromUri)
    }
    val coverPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingTrackForCover?.let { viewModel.applyCustomTrackCover(it, uri) }
            pendingAlbumForCover?.let { viewModel.applyCustomAlbumCover(it, uri) }
        }
        pendingTrackForCover = null
        pendingAlbumForCover = null
    }
    val backupExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportBackupZip)
    }
    val backupRestoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restoreBackupZip)
    }
    val statsJsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportStatsJsonToUri)
    }

    val startVoiceWithPermission = {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.startVoiceRecognition()
        } else {
            pendingEnableHandsFreeOnPermission = false
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    val toggleHandsFreeWithPermission: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            viewModel.setHandsFreeVoiceEnabled(false)
        } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.setHandsFreeVoiceEnabled(true)
            } else {
                pendingEnableHandsFreeOnPermission = true
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val requestAudioPermissions = {
        val perms = buildList {
            add(audioPermission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        permissionLauncher.launch(perms)
    }

    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val favoriteTrackKeys by viewModel.favoriteTrackKeys.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val albumPlaylists by viewModel.albumPlaylists.collectAsStateWithLifecycle()
    val artistPlaylists by viewModel.artistPlaylists.collectAsStateWithLifecycle()
    val customPlaylists by viewModel.customPlaylists.collectAsStateWithLifecycle()
    val mostPlayedTracks by viewModel.mostPlayedTracks.collectAsStateWithLifecycle()
    val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
    val deviceSoundProfiles by viewModel.deviceSoundProfiles.collectAsStateWithLifecycle()
    val activeAudioDeviceType by viewModel.activeAudioDeviceType.collectAsStateWithLifecycle()
    val activeAudioSessionId by viewModel.activeAudioSessionId.collectAsStateWithLifecycle()
    val playerVisualPrefs by viewModel.playerVisualPrefs.collectAsStateWithLifecycle()
    val customThemes by viewModel.customThemes.collectAsStateWithLifecycle()
    val homeLayoutConfig by viewModel.homeLayoutConfig.collectAsStateWithLifecycle()
    val homeLayoutPresets by viewModel.homeLayoutPresets.collectAsStateWithLifecycle()
    val savedCustomEqProfiles by viewModel.savedCustomEqProfiles.collectAsStateWithLifecycle()
    val sleepTimerState by viewModel.sleepTimerState.collectAsStateWithLifecycle()
    val playbackEnginePrefs by viewModel.playbackEnginePrefs.collectAsStateWithLifecycle()
    val djRuntimeState by viewModel.djRuntimeState.collectAsStateWithLifecycle()
    val savedQueuePresets by viewModel.savedQueuePresets.collectAsStateWithLifecycle()
    val sleepTimerCustomPresets by viewModel.sleepTimerCustomPresets.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanningLibrary.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedSlot by viewModel.selectedSlot.collectAsStateWithLifecycle()
    val playlistsBySlot by viewModel.playlistsBySlot.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val cloudProgressState by viewModel.cloudPlaybackProgress.collectAsStateWithLifecycle()
    val liveTotalSeconds by viewModel.liveTotalPlaybackSeconds.collectAsStateWithLifecycle()
    val sessionSeconds by viewModel.sessionSeconds.collectAsStateWithLifecycle()
    val cloudStatus by viewModel.cloudStatus.collectAsStateWithLifecycle()
    val monthlyReportState by viewModel.monthlyReportState.collectAsStateWithLifecycle()
    val selectedChatRegion by viewModel.selectedChatRegion.collectAsStateWithLifecycle()
    val regionalChatMessages by viewModel.regionalChatMessages.collectAsStateWithLifecycle()
    val pendingChatAttachment by viewModel.pendingChatAttachment.collectAsStateWithLifecycle()
    val chatStatusMessage by viewModel.chatStatusMessage.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()
    val isBusyBackup by viewModel.isBusyBackup.collectAsStateWithLifecycle()
    val isTrimmingChatAudio by viewModel.isTrimmingChatAudio.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val lyricsByTrackKey by viewModel.lyricsByTrackKey.collectAsStateWithLifecycle()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val lyricsStatusMessage by viewModel.lyricsStatusMessage.collectAsStateWithLifecycle()
    val openLyricsTrackEvent by viewModel.openLyricsTrackEvent.collectAsStateWithLifecycle()
    val actionToast by viewModel.actionToastMessage.collectAsStateWithLifecycle()
    val diagnosticsState by viewModel.diagnosticsState.collectAsStateWithLifecycle()
    val extremeVisualStyle by viewModel.extremeVisualStyle.collectAsStateWithLifecycle()
    val playerDesignerConfig by viewModel.playerDesignerConfig.collectAsStateWithLifecycle()
    val musicalProfileConfig by viewModel.musicalProfileConfig.collectAsStateWithLifecycle()
    val allTrackStatsMap by viewModel.allTrackStatsMap.collectAsStateWithLifecycle()
    val metadataOverrides by viewModel.metadataOverrides.collectAsStateWithLifecycle()
    val savedWrappedYears by viewModel.savedWrappedYears.collectAsStateWithLifecycle()
    val realTimeStatsSummary by viewModel.realTimeStatsSummary.collectAsStateWithLifecycle()
    val ritmoRecapReport by viewModel.ritmoRecapReport.collectAsStateWithLifecycle()
    val ritmoWrappedReport by viewModel.ritmoWrappedReport.collectAsStateWithLifecycle()
    val musicalHistoryReport by viewModel.musicalHistoryReport.collectAsStateWithLifecycle()
    val musicalAchievements by viewModel.musicalAchievements.collectAsStateWithLifecycle()
    val smartAmbientMixes by viewModel.smartAmbientMixes.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_LIBRARY
    val primary = MaterialTheme.colorScheme.primary
    val extraStrings = LocalExtraStrings.current

    val navigateToRoute: (String) -> Unit = { route ->
        viewModel.noteRouteNavigationProtected(route)
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(launchMixSlotWire) {
        if (!launchMixSlotWire.isNullOrBlank()) {
            viewModel.selectTimeOfDaySlot(TimeOfDaySlot.fromWire(launchMixSlotWire))
            navigateToRoute(ROUTE_MIXES)
            onConsumeLaunchMixSlot()
        }
    }
    LaunchedEffect(launchChatRegionId) {
        if (!launchChatRegionId.isNullOrBlank()) {
            viewModel.selectChatRegion(ChatRegion.fromId(launchChatRegionId))
            navigateToRoute(ROUTE_CHAT)
            onConsumeLaunchChatRegion()
        }
    }
    LaunchedEffect(sleepTimerState.justExpiredAtMs) {
        if (sleepTimerState.justExpiredAtMs != null) {
            snackbarHostState.showSnackbar("Temporizador finalizado: reproducción detenida.")
            viewModel.clearSleepTimerExpiredBanner()
        }
    }
    LaunchedEffect(actionToast) {
        actionToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionToast()
        }
    }
    LaunchedEffect(openLyricsTrackEvent) {
        openLyricsTrackEvent?.let { track ->
            lyricsSheetTrack = track
            viewModel.consumeOpenLyricsEvent()
        }
    }

    val shareTrackFn: (LocalTrack) -> Unit = { track ->
        val shareIntent = MusicBackupManager.buildShareAudioIntent(context, track)
        if (shareIntent != null) {
            context.startActivity(shareIntent)
        } else {
            scope.launch { snackbarHostState.showSnackbar("No se pudo preparar '${track.title}' para compartir.") }
        }
    }

    val trackActions = remember(viewModel, navigateToRoute) {
        TrackActionsCallbacks(
            onPlayNext = viewModel::playNext,
            onAddToPlaylist = { trackForPlaylistDialog = it },
            onChangeCoverFromGallery = {
                pendingAlbumForCover = null
                pendingTrackForCover = it
                coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onFetchCoverOnline = viewModel::fetchTrackCoverOnline,
            onShareTrack = shareTrackFn,
            onCastToTv = {
                castTargetTitle = "${it.title} — ${it.artist}"
                showCastSheet = true
            },
            onRecommendInChat = {
                preselectedChatTrack = it
                navigateToRoute(ROUTE_CHAT)
            },
            onOpenLyrics = { track ->
                viewModel.loadLyricsForTrack(track)
                lyricsSheetTrack = track
            }
        )
    }

    val isNeon = appearancePrefs.uiStyleMode == UiStyleMode.NEON_MODERN
    val bottomDestinations = if (isNeon) {
        listOf(
            NavDestinationItem(ROUTE_LIBRARY, "Inicio", Icons.Rounded.Home, Icons.Outlined.Home),
            NavDestinationItem(ROUTE_MIXES, "Explorar", Icons.Rounded.Search, Icons.Outlined.Search),
            NavDestinationItem(ROUTE_MONTHLY_STATS, "Estadísticas", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
            NavDestinationItem(ROUTE_CHAT, "Chat", Icons.Rounded.Forum, Icons.Outlined.Forum),
            NavDestinationItem(ROUTE_SETTINGS, "Más", Icons.Rounded.MoreHoriz, Icons.Outlined.MoreHoriz)
        )
    } else {
        listOf(
            NavDestinationItem(ROUTE_LIBRARY, strings.navHome, Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
            NavDestinationItem(ROUTE_MIXES, strings.navPlaylists, Icons.Rounded.QueueMusic, Icons.Outlined.QueueMusic),
            NavDestinationItem(ROUTE_EQUALIZER, strings.navEq, Icons.Rounded.Equalizer, Icons.Outlined.Equalizer),
            NavDestinationItem(ROUTE_TIME_CLOUD, strings.navTime, Icons.Rounded.AccessTimeFilled, Icons.Outlined.AccessTime),
            NavDestinationItem(ROUTE_CHAT, strings.navChat, Icons.Rounded.Forum, Icons.Outlined.Forum)
        )
    }
    val railDestinations = if (isNeon) {
        bottomDestinations + listOf(
            NavDestinationItem(ROUTE_EQUALIZER, strings.navEq, Icons.Rounded.Equalizer, Icons.Outlined.Equalizer),
            NavDestinationItem(ROUTE_TIME_CLOUD, strings.navTime, Icons.Rounded.AccessTimeFilled, Icons.Outlined.AccessTime),
            NavDestinationItem(ROUTE_DIAGNOSTICS, "Diagnóstico", Icons.Rounded.HealthAndSafety, Icons.Outlined.HealthAndSafety)
        )
    } else {
        bottomDestinations + listOf(
            NavDestinationItem(ROUTE_MONTHLY_STATS, strings.navMonthly, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
            NavDestinationItem(ROUTE_SETTINGS, strings.navSettings, Icons.Rounded.Settings, Icons.Outlined.Settings),
            NavDestinationItem(ROUTE_DIAGNOSTICS, "Diagnóstico", Icons.Rounded.HealthAndSafety, Icons.Outlined.HealthAndSafety)
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    Column {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            activeSlot = selectedSlot,
                            isFavorite = playbackState.currentTrack?.let { favoriteTrackKeys.contains(it.trackKey) } == true,
                            sleepTimerState = sleepTimerState,
                            animationsEnabled = appSettings.animationsEnabled,
                            handsFreeVoiceActive = appSettings.handsFreeVoiceEnabled,
                            onOpenSleepTimer = { showSleepTimerSheet = true },
                            onOpenVoiceControl = { showVoiceSheet = true },
                            onToggleFavorite = { playbackState.currentTrack?.let { viewModel.toggleFavorite(it) } },
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext,
                            onExpand = { showNowPlayingSheet = true }
                        )
                        NavigationBar(
                            containerColor = if (isNeon) Color(0xFF0A0814) else MaterialTheme.colorScheme.surface
                        ) {
                            bottomDestinations.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToRoute(dest.route) },
                                    icon = { Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label) },
                                    label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = primary,
                                        selectedTextColor = primary,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = primary.copy(alpha = 0.16f)
                                    ),
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        railDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToRoute(dest.route) },
                                icon = { Icon(if (selected) dest.selectedIcon else dest.unselectedIcon, contentDescription = dest.label) },
                                label = { Text(dest.label, maxLines = 1) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = primary,
                                    selectedTextColor = primary,
                                    indicatorColor = primary.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
                        composable(ROUTE_LIBRARY) {
                            LibraryScreen(
                                tracks = localTracks,
                                favoriteTrackKeys = favoriteTrackKeys,
                                searchQuery = searchQuery,
                                isScanning = isScanning,
                                hasAudioPermission = hasAudioPermission,
                                currentPlayingTrackKey = playbackState.currentTrack?.trackKey,
                                totalPlaybackSeconds = liveTotalSeconds,
                                cloudProgressState = cloudProgressState,
                                sleepTimerState = sleepTimerState,
                                trackActions = trackActions,
                                homeLayoutConfig = homeLayoutConfig,
                                realTimeSummary = realTimeStatsSummary,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onRequestAudioPermission = requestAudioPermissions,
                                onImportAudioFiles = {
                                    audioPickerLauncher.launch(arrayOf("audio/*", "application/ogg", "application/x-flac"))
                                },
                                onRefreshLibrary = viewModel::refreshLocalLibrary,
                                onOpenThemeCustomization = { showThemeSheet = true },
                                onOpenHomeCustomization = { showHomeCustomizeSheet = true },
                                onOpenRecapAndWrapped = { navigateToRoute(ROUTE_RECAP_WRAPPED) },
                                onOpenLibraryOrganizer = { navigateToRoute(ROUTE_LIBRARY_ORGANIZER) },
                                onOpenExtremeVisualEditor = { showExtremeVisualEditor = true },
                                onHomeShortcutClick = { shortcut ->
                                    when (shortcut) {
                                        HomeShortcutType.FAVORITES -> {
                                            mixesSectionJump = "FAVORITES"
                                            navigateToRoute(ROUTE_MIXES)
                                        }
                                        HomeShortcutType.MIXES -> navigateToRoute(ROUTE_MIXES)
                                        HomeShortcutType.EQUALIZER -> navigateToRoute(ROUTE_EQUALIZER)
                                        HomeShortcutType.STATS -> navigateToRoute(ROUTE_RECAP_WRAPPED)
                                        HomeShortcutType.PROFILE -> navigateToRoute(ROUTE_MUSICAL_PROFILE)
                                        HomeShortcutType.HISTORY -> navigateToRoute(ROUTE_MUSICAL_HISTORY)
                                        HomeShortcutType.ORGANIZER -> navigateToRoute(ROUTE_LIBRARY_ORGANIZER)
                                        HomeShortcutType.DIAGNOSTICS -> navigateToRoute(ROUTE_DIAGNOSTICS)
                                    }
                                },
                                onOpenSleepTimer = { showSleepTimerSheet = true },
                                onOpenCastSheet = {
                                    castTargetTitle = playbackState.currentTrack?.let { "${it.title} — ${it.artist}" }
                                    showCastSheet = true
                                },
                                onOpenVoiceControl = { showVoiceSheet = true },
                                onOpenSettings = { navigateToRoute(ROUTE_SETTINGS) },
                                onTrackClick = viewModel::playTrackFromLibrary,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onResumeCloudClick = viewModel::resumeFromCloudProgress,
                                onPlayTimeSlot = { slot -> viewModel.playGeneratedPlaylist(slot, 0) },
                                onNavigateToMixesSection = { sectionKey ->
                                    mixesSectionJump = sectionKey
                                    navigateToRoute(ROUTE_MIXES)
                                }
                            )
                        }
                        composable(ROUTE_DIAGNOSTICS) {
                            LaunchedEffect(Unit) {
                                if (diagnosticsState.latestReport == null) {
                                    viewModel.runDiagnosticAnalysis()
                                }
                            }
                            DiagnosticsScreen(
                                uiState = diagnosticsState,
                                onAnalyzeApp = viewModel::runDiagnosticAnalysis,
                                onRunSelfTest = viewModel::runDiagnosticSelfTest,
                                onToggleDiagnosticMode = viewModel::setDiagnosticModeEnabled,
                                onExecuteRepair = { issue ->
                                    viewModel.executeDiagnosticRepair(
                                        issue = issue,
                                        onRequestPermission = { perm ->
                                            when (perm) {
                                                Manifest.permission.POST_NOTIFICATIONS -> notifPermissionLauncher.launch(perm)
                                                Manifest.permission.RECORD_AUDIO -> recordAudioPermissionLauncher.launch(perm)
                                                else -> requestAudioPermissions()
                                            }
                                        },
                                        onOpenBatterySettings = {
                                            runCatching {
                                                val i = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(i)
                                            }
                                        }
                                    )
                                },
                                onDismissIssue = viewModel::dismissDiagnosticIssue,
                                onClearHistory = viewModel::clearDiagnosticHistory,
                                onShowMessage = viewModel::showToastMessage,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_MIXES) {
                            MixesScreen(
                                selectedSlot = selectedSlot,
                                currentRealTimeSlot = TimeOfDaySlot.current(),
                                playlistsBySlot = playlistsBySlot,
                                allTracks = localTracks,
                                customPlaylists = customPlaylists,
                                favoriteTracks = favoriteTracks,
                                favoriteTrackKeys = favoriteTrackKeys,
                                mostPlayedTracks = mostPlayedTracks,
                                albumPlaylists = albumPlaylists,
                                artistPlaylists = artistPlaylists,
                                currentPlayingTrackKey = playbackState.currentTrack?.trackKey,
                                animationsEnabled = appSettings.animationsEnabled,
                                trackActions = trackActions,
                                onSelectSlot = viewModel::selectTimeOfDaySlot,
                                onRegeneratePlaylist = viewModel::regeneratePlaylist,
                                onPlayTimeSlotPlaylist = viewModel::playGeneratedPlaylist,
                                onPlayTracksQueue = viewModel::playTracksQueue,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onCreateCustomPlaylist = viewModel::createCustomPlaylist,
                                onRenameCustomPlaylist = viewModel::renameCustomPlaylist,
                                onDeleteCustomPlaylist = viewModel::deleteCustomPlaylist,
                                onRemoveTrackFromCustomPlaylist = viewModel::removeTrackFromCustomPlaylist,
                                onChangeAlbumCover = { album ->
                                    pendingTrackForCover = null
                                    pendingAlbumForCover = album
                                    coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                ambientMixesByCategory = smartAmbientMixes,
                                onRegenerateAmbientMix = viewModel::regenerateAmbientMix,
                                onSaveAmbientMixAsPlaylist = viewModel::saveAmbientMixAsCustomPlaylist,
                                initialSectionKey = mixesSectionJump,
                                onConsumeInitialSectionKey = { mixesSectionJump = null }
                            )
                        }
                        composable(ROUTE_EQUALIZER) {
                            EqualizerScreen(
                                eqState = equalizerState,
                                onToggleEnabled = viewModel::setEqualizerEnabled,
                                onSelectPreset = viewModel::applyEqualizerPreset,
                                onUpdateBand = viewModel::updateEqualizerBand,
                                onUpdateBassBoost = viewModel::updateBassBoost,
                                onToggleSpatial3d = viewModel::setSpatial3dEnabled,
                                onSelectSpatial3dPreset = viewModel::applySpatial3dPreset,
                                onUpdateSpatial3dStrength = viewModel::updateSpatial3dStrength,
                                onUpdateClarityBoost = viewModel::updateClarityBoost,
                                onSelectHearingProfile = viewModel::selectHearingProfile,
                                onSelectAudioChannelMode = viewModel::setAudioChannelMode,
                                onUpdateBalanceLR = viewModel::setBalanceLR,
                                onToggleHiResMastering = viewModel::setHiResMasteringEnabled,
                                onSelectStudioAudioMode = viewModel::selectStudioAudioMode,
                                onUpdateDynamicPunch = viewModel::updateDynamicPunch,
                                onToggleAntiClipLimiter = viewModel::setAntiClipLimiterEnabled,
                                deviceSoundProfiles = deviceSoundProfiles,
                                detectedDeviceType = activeAudioDeviceType,
                                onUpdateTrebleBoost = viewModel::updateTrebleBoost,
                                onToggleAutoSwitchDeviceProfile = viewModel::setAutoSwitchDeviceProfile,
                                onSelectDeviceProfile = viewModel::selectAndApplyDeviceProfile,
                                onSaveCurrentDeviceProfile = viewModel::saveCurrentEqToDeviceProfile,
                                onResetDeviceProfile = viewModel::deleteDeviceSoundProfile,
                                onOpenThemeCustomization = { showThemeSheet = true }
                            )
                        }
                        composable(ROUTE_TIME_CLOUD) {
                            val currentMonthSummary = (monthlyReportState as? UiState.Success)?.data?.let { report ->
                                if (report.selectedMonthKey == currentMonthKey()) report.summary else null
                            }
                            TimeCloudScreen(
                                isSignedIn = currentUserId != null,
                                userEmail = userEmail,
                                userDisplayName = userDisplayName,
                                liveTotalPlaybackSeconds = liveTotalSeconds,
                                sessionSeconds = sessionSeconds,
                                playbackState = playbackState,
                                cloudProgressState = cloudProgressState,
                                currentMonthSummary = currentMonthSummary,
                                cloudStatus = cloudStatus,
                                sleepTimerState = sleepTimerState,
                                onSaveNow = viewModel::saveProgressNow,
                                onResumeFromCloud = viewModel::resumeFromCloudProgress,
                                onOpenSleepTimer = { showSleepTimerSheet = true },
                                onExtendSleepTimer = { viewModel.extendSleepTimer(5) },
                                onCancelSleepTimer = viewModel::cancelSleepTimer,
                                onOpenThemeCustomization = { showThemeSheet = true },
                                onOpenMonthlyStats = { navigateToRoute(ROUTE_MONTHLY_STATS) },
                                onOpenSettings = { navigateToRoute(ROUTE_SETTINGS) },
                                onSignIn = onRequestSignIn,
                                onSignOut = {
                                    signOutFromApp(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        beforeSignOut = { viewModel.flushBeforeSignOut() }
                                    )
                                }
                            )
                        }
                        composable(ROUTE_MONTHLY_STATS) {
                            MonthlyStatsScreen(
                                reportState = monthlyReportState,
                                onSelectMonth = viewModel::selectMonth,
                                isSignedIn = currentUserId != null,
                                onRequestSignIn = onRequestSignIn,
                                onOpenRecapAndWrapped = { navigateToRoute(ROUTE_RECAP_WRAPPED) },
                                onOpenMusicalHistory = { navigateToRoute(ROUTE_MUSICAL_HISTORY) },
                                onOpenMusicalProfile = { navigateToRoute(ROUTE_MUSICAL_PROFILE) },
                                localeTag = appSettings.appLanguage.code
                            )
                        }
                        composable(ROUTE_RECAP_WRAPPED) {
                            RecapAndWrappedScreen(
                                recapReport = ritmoRecapReport,
                                wrappedReport = ritmoWrappedReport,
                                realTimeSummary = realTimeStatsSummary,
                                savedWrappedYears = savedWrappedYears,
                                cloudSyncEnabled = musicalProfileConfig.statsCloudSyncEnabled,
                                isSignedIn = currentUserId != null,
                                localeTag = appSettings.appLanguage.code,
                                onSelectRecapMonth = viewModel::selectMonth,
                                onSelectWrappedYear = viewModel::selectWrappedYear,
                                onSaveWrappedSnapshot = viewModel::saveWrappedSnapshot,
                                onToggleCloudSync = { enabled ->
                                    viewModel.saveMusicalProfileConfig(
                                        musicalProfileConfig.copy(statsCloudSyncEnabled = enabled)
                                    )
                                },
                                onExportStatsJson = {
                                    statsJsonExportLauncher.launch("RitmoLocal_Estadisticas_${currentMonthKey()}.json")
                                },
                                onClearAllStatsConfirmed = viewModel::clearAllStatsConfirmed,
                                onOpenHistory = { navigateToRoute(ROUTE_MUSICAL_HISTORY) },
                                onOpenProfile = { navigateToRoute(ROUTE_MUSICAL_PROFILE) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_MUSICAL_HISTORY) {
                            MusicalHistoryScreen(
                                report = musicalHistoryReport,
                                localeTag = appSettings.appLanguage.code,
                                onSelectMonth = viewModel::selectMonth,
                                onSelectYear = viewModel::selectWrappedYear,
                                onPlayTrackByKey = viewModel::playTrackByKey,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_MUSICAL_PROFILE) {
                            MusicalProfileScreen(
                                profileConfig = musicalProfileConfig,
                                achievements = musicalAchievements,
                                recapReport = ritmoRecapReport,
                                realTimeSummary = realTimeStatsSummary,
                                favoriteTracks = favoriteTracks,
                                onSaveProfileConfig = viewModel::saveMusicalProfileConfig,
                                onOpenRecap = { navigateToRoute(ROUTE_RECAP_WRAPPED) },
                                onPlayTrack = viewModel::playTrackFromLibrary,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_LIBRARY_ORGANIZER) {
                            LibraryOrganizerScreen(
                                allTracks = localTracks,
                                trackStatsByKey = allTrackStatsMap,
                                metadataOverrides = metadataOverrides,
                                onPlayTracksQueue = viewModel::playTracksQueue,
                                onFetchCoverOnline = viewModel::fetchTrackCoverOnline,
                                onPickCoverFromGallery = { track ->
                                    pendingAlbumForCover = null
                                    pendingTrackForCover = track
                                    coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onSaveMetadataOverride = viewModel::saveTrackMetadataOverride,
                                onClearMetadataOverride = viewModel::clearTrackMetadataOverride,
                                onHideDuplicateTrack = viewModel::hideDuplicateTrack,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(ROUTE_CHAT) {
                            val defaultName = userDisplayName ?: userEmail?.substringBefore("@") ?: "Oyente"
                            RegionalChatScreen(
                                isSignedIn = currentUserId != null,
                                currentUserId = currentUserId,
                                selectedRegion = selectedChatRegion,
                                messagesState = regionalChatMessages,
                                localTracks = localTracks,
                                preselectedTrackForChat = preselectedChatTrack,
                                pendingAudioAttachmentName = pendingChatAttachment?.fileName,
                                externalTrimmerUri = pendingTrimExternalUri,
                                isTrimmingAudio = isTrimmingChatAudio,
                                chatStatusMessage = chatStatusMessage,
                                customUsername = appSettings.customChatUsername,
                                defaultAccountName = defaultName,
                                userAvatarUri = appSettings.chatAvatarUri,
                                userAvatarBase64 = appSettings.chatAvatarBase64,
                                userPhotoUrl = userPhotoUrl,
                                chatPushEnabled = appSettings.chatPushNotificationsEnabled,
                                onToggleChatPush = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.setChatPushNotificationsEnabled(enabled)
                                },
                                onUpdateCustomUsername = viewModel::setCustomChatUsername,
                                onPickChatAvatar = {
                                    avatarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onClearChatAvatar = viewModel::clearChatAvatar,
                                onSelectRegion = viewModel::selectChatRegion,
                                onRequestSignIn = onRequestSignIn,
                                onPickAudioAttachment = { chatAudioPickerLauncher.launch(arrayOf("audio/*")) },
                                onPickExternalAudioForTrim = { trimExternalAudioLauncher.launch(arrayOf("audio/*")) },
                                onTrimAndAttachAudio = { uri, title, artist, startMs, endMs, onDone ->
                                    viewModel.trimAndAttachAudioForChat(uri, title, artist, startMs, endMs) {
                                        pendingTrimExternalUri = null
                                        onDone()
                                    }
                                },
                                onClearAudioAttachment = viewModel::clearPendingChatAttachment,
                                onSendMessage = { msgText, recTitle, recArtist ->
                                    val sender = appSettings.customChatUsername.ifBlank { defaultName }
                                    viewModel.sendRegionalChatMessage(sender, msgText, recTitle, recArtist)
                                },
                                onDownloadAttachedAudio = viewModel::downloadChatAudio,
                                onDeleteMessage = viewModel::deleteRegionalChatMessage,
                                onClearPreselectedTrack = { preselectedChatTrack = null }
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                settings = appSettings,
                                appearancePrefs = appearancePrefs,
                                isSignedIn = currentUserId != null,
                                userDisplayName = userDisplayName,
                                userEmail = userEmail,
                                userPhotoUrl = userPhotoUrl,
                                backupStatusMessage = backupStatus,
                                isBusyBackup = isBusyBackup,
                                onTogglePlayWithOtherApps = viewModel::setPlayAlongOtherApps,
                                onUpdateNotificationOptions = viewModel::updateNotificationOptions,
                                onToggleScheduledReminders = viewModel::setScheduledMixReminders,
                                onSendTestReminder = viewModel::sendTestMixReminder,
                                onToggleChatPushNotifications = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.setChatPushNotificationsEnabled(enabled)
                                },
                                onToggleChatPushOnlyActiveRegion = viewModel::setChatPushOnlyActiveRegion,
                                onSendTestChatPush = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.sendTestChatPushNotification()
                                    }
                                },
                                onToggleHideShortAudio = viewModel::setHideShortWhatsAppAudio,
                                onToggleAnimations = viewModel::setAnimationsEnabled,
                                onToggleAutoArtwork = viewModel::setAutoDownloadArtwork,
                                onDownloadMissingArtworkNow = viewModel::downloadMissingArtworkNow,
                                onSelectLanguage = viewModel::setAppLanguage,
                                onSelectVoiceLanguage = viewModel::setVoiceLanguage,
                                onToggleHandsFreeVoice = toggleHandsFreeWithPermission,
                                onToggleScreenOffVoice = viewModel::setScreenOffVoiceEnabled,
                                onSelectUiStyleMode = viewModel::setUiStyleMode,
                                onSelectFontStyle = viewModel::setFontStyle,
                                onPickChatAvatar = {
                                    avatarPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                onOpenVoiceControl = { showVoiceSheet = true },
                                onExportBackupToDrive = {
                                    backupExportLauncher.launch("RitmoLocal_Respaldo_${System.currentTimeMillis() / 1000}.zip")
                                },
                                onRestoreBackupFromDrive = {
                                    backupRestoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                },
                                onOpenCastSheet = {
                                    castTargetTitle = playbackState.currentTrack?.let { "${it.title} — ${it.artist}" }
                                    showCastSheet = true
                                },
                                onOpenThemeCustomization = { showThemeSheet = true },
                                onOpenExtremeVisualEditor = { showExtremeVisualEditor = true },
                                onOpenPlayerDesigner = { showPlayerDesignerSheet = true },
                                onOpenHomeCustomization = { showHomeCustomizeSheet = true },
                                onOpenRecapAndWrapped = { navigateToRoute(ROUTE_RECAP_WRAPPED) },
                                onOpenMusicalHistory = { navigateToRoute(ROUTE_MUSICAL_HISTORY) },
                                onOpenMusicalProfile = { navigateToRoute(ROUTE_MUSICAL_PROFILE) },
                                onOpenLibraryOrganizer = { navigateToRoute(ROUTE_LIBRARY_ORGANIZER) },
                                onOpenEqualizer = { navigateToRoute(ROUTE_EQUALIZER) },
                                onOpenTimeCloud = { navigateToRoute(ROUTE_TIME_CLOUD) },
                                onOpenDiagnostics = { navigateToRoute(ROUTE_DIAGNOSTICS) },
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onSignInClick = onRequestSignIn,
                                onSignOutClick = {
                                    signOutFromApp(
                                        credentialManager = credentialManager,
                                        scope = scope,
                                        beforeSignOut = { viewModel.flushBeforeSignOut() }
                                    )
                                }
                            )
                        }
                    }

                    if (useRail) {
                        MiniPlayerBar(
                            playbackState = playbackState,
                            activeSlot = selectedSlot,
                            isFavorite = playbackState.currentTrack?.let { favoriteTrackKeys.contains(it.trackKey) } == true,
                            sleepTimerState = sleepTimerState,
                            animationsEnabled = appSettings.animationsEnabled,
                            handsFreeVoiceActive = appSettings.handsFreeVoiceEnabled,
                            onOpenSleepTimer = { showSleepTimerSheet = true },
                            onOpenVoiceControl = { showVoiceSheet = true },
                            onToggleFavorite = { playbackState.currentTrack?.let { viewModel.toggleFavorite(it) } },
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onSkipNext = viewModel::skipNext,
                            onExpand = { showNowPlayingSheet = true },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        )
                    }

                    if (appSettings.handsFreeVoiceEnabled) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 6.dp)
                                .border(
                                    width = 1.dp,
                                    color = if (voiceState.awaitingCommandAfterWake) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable { showVoiceSheet = true }
                                .testTag("hands_free_driving_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (voiceState.awaitingCommandAfterWake) primary else Color(0xFF10B981),
                                            CircleShape
                                        )
                                )
                                Icon(Icons.Rounded.Mic, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = when {
                                        voiceState.awaitingCommandAfterWake -> "¡Te escucho! Di: siguiente, anterior, subir volumen…"
                                        voiceState.partialTranscript.isNotBlank() -> voiceState.partialTranscript
                                        else -> extraStrings.handsFreeActivePill
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val currentTrack = playbackState.currentTrack
    val openFullPlayer = showNowPlayingSheet && currentTrack != null
    val renderFullNowPlaying: @Composable () -> Unit = {
        currentTrack?.let { track ->
            AdvancedNowPlayingScreen(
                track = track,
                playbackState = playbackState,
                activeSlot = selectedSlot,
                isFavorite = favoriteTrackKeys.contains(track.trackKey),
                sleepTimerState = sleepTimerState,
                eqState = equalizerState,
                audioSessionId = activeAudioSessionId,
                visualPrefs = playerVisualPrefs,
                designerConfig = playerDesignerConfig,
                isDarkTheme = appearancePrefs.isDarkTheme,
                animationsEnabled = appSettings.animationsEnabled,
                isSignedIn = currentUserId != null,
                currentLyrics = lyricsByTrackKey[track.trackKey],
                isLoadingLyrics = isLoadingLyrics,
                handsFreeVoiceActive = appSettings.handsFreeVoiceEnabled,
                crossfadeOrDjActive = playbackEnginePrefs.crossfadeEnabled || playbackEnginePrefs.smartDjEnabled,
                onOpenProControls = { showProControlsSheet = true },
                onOpenPlayerDesigner = { showPlayerDesignerSheet = true },
                onOpenImmersiveVisual = { showImmersiveVisualOverlay = true },
                onDismiss = { showNowPlayingSheet = false },
                onToggleFavorite = { viewModel.toggleFavorite(track) },
                onOpenQueue = { showQueueSheet = true },
                onOpenEqualizerSheet = { showInPlayerEqSheet = true },
                onOpenCustomizeSheet = { showPlayerCustomizeSheet = true },
                onOpenSleepTimer = { showSleepTimerSheet = true },
                onOpenCastSheet = {
                    castTargetTitle = "${track.title} — ${track.artist}"
                    showCastSheet = true
                },
                onOpenVoiceControl = { showVoiceSheet = true },
                onOpenFullLyricsEditor = {
                    viewModel.loadLyricsForTrack(track)
                    lyricsSheetTrack = track
                },
                onSearchOnlineLyrics = { viewModel.searchOnlineLyrics(track) },
                onShareTrack = { shareTrackFn(track) },
                onChangeCover = {
                    pendingAlbumForCover = null
                    pendingTrackForCover = track
                    coverPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onTogglePlayPause = viewModel::togglePlayPause,
                onSeekTo = viewModel::seekTo,
                onSkipPrevious = viewModel::skipPrevious,
                onSkipNext = viewModel::skipNext,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onSaveToCloud = viewModel::saveProgressNow
            )
        }
    }

    if (appSettings.animationsEnabled) {
        AnimatedVisibility(
            visible = openFullPlayer,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 5 })
        ) {
            renderFullNowPlaying()
        }
    } else if (openFullPlayer) {
        renderFullNowPlaying()
    }

    if (showQueueSheet) {
        PlaybackQueueSheet(
            queue = playbackState.queue,
            currentQueueIndex = playbackState.currentQueueIndex,
            isPlaying = playbackState.isPlaying,
            libraryTracks = localTracks,
            savedQueuePresets = savedQueuePresets,
            onJumpToIndex = viewModel::jumpToQueueIndex,
            onMoveItem = viewModel::moveQueueItem,
            onRemoveItem = viewModel::removeQueueItem,
            onPlayNext = viewModel::playNext,
            onAppendToQueue = viewModel::appendToQueue,
            onSmartShuffleQueue = viewModel::smartShuffleQueue,
            onDeduplicateQueue = viewModel::deduplicateQueue,
            onSaveQueuePreset = viewModel::saveCurrentQueueAsPreset,
            onLoadQueuePreset = viewModel::loadSavedQueuePreset,
            onDeleteQueuePreset = viewModel::deleteSavedQueuePreset,
            onDismiss = { showQueueSheet = false }
        )
    }

    if (showProControlsSheet) {
        PlaybackProControlsSheet(
            enginePrefs = playbackEnginePrefs,
            djRuntime = djRuntimeState,
            visualizerStyle = playerVisualPrefs.visualizerStyle,
            visualizerIntensity = playerVisualPrefs.visualizerIntensity,
            canMixNow = playbackState.queue.size > 1,
            onUpdateEnginePrefs = viewModel::updatePlaybackEnginePrefs,
            onTriggerManualDjMix = viewModel::triggerManualDjMix,
            onSelectVisualizerStyle = viewModel::setVisualizerStyle,
            onUpdateVisualizerIntensity = viewModel::setVisualizerIntensity,
            onDismiss = { showProControlsSheet = false }
        )
    }

    if (showInPlayerEqSheet) {
        InPlayerEqualizerSheet(
            eqState = equalizerState,
            customProfiles = savedCustomEqProfiles,
            onToggleEnabled = viewModel::setEqualizerEnabled,
            onSelectPreset = viewModel::applyEqualizerPreset,
            onUpdateBand = viewModel::updateEqualizerBand,
            onUpdateBassBoost = viewModel::updateBassBoost,
            onUpdateTrebleBoost = viewModel::updateTrebleBoost,
            onToggleSpatial3d = viewModel::setSpatial3dEnabled,
            onSaveCustomProfile = viewModel::saveCurrentAsCustomEqProfile,
            onApplyCustomProfile = viewModel::applySavedCustomEqProfile,
            onDeleteCustomProfile = viewModel::deleteSavedCustomEqProfile,
            onOpenFullEqualizer = {
                showNowPlayingSheet = false
                navigateToRoute(ROUTE_EQUALIZER)
            },
            onDismiss = { showInPlayerEqSheet = false }
        )
    }

    if (showPlayerCustomizeSheet) {
        PlayerCustomizationSheet(
            visualPrefs = playerVisualPrefs,
            isDarkTheme = appearancePrefs.isDarkTheme,
            animationsEnabled = appSettings.animationsEnabled,
            onSelectStyle = viewModel::setPlayerStyle,
            onSelectBackgroundMode = viewModel::setPlayerBackgroundMode,
            onSelectLayoutMode = viewModel::setPlayerLayoutMode,
            onSelectControlStyle = viewModel::setPlayerControlStyle,
            onUpdateBlurIntensity = viewModel::setPlayerBlurIntensity,
            onSelectPrimaryColor = viewModel::setPlayerPrimaryColor,
            onSelectButtonColor = viewModel::setPlayerButtonColor,
            onToggleDarkTheme = { isDark -> scope.launch { preferencesRepository.setDarkTheme(isDark) } },
            onToggleVisualizer = viewModel::setPlayerShowVisualizer,
            onToggleInlineLyrics = viewModel::setPlayerShowInlineLyrics,
            onToggleAnimations = viewModel::setAnimationsEnabled,
            onRestoreDefaults = viewModel::restoreOriginalPlayerVisuals,
            onDismiss = { showPlayerCustomizeSheet = false }
        )
    }

    lyricsSheetTrack?.let { targetTrack ->
        LyricsSheet(
            track = targetTrack,
            lyrics = lyricsByTrackKey[targetTrack.trackKey],
            positionMs = playbackState.positionMs,
            isCurrentPlayingTrack = currentTrack?.trackKey == targetTrack.trackKey,
            isLoading = isLoadingLyrics,
            statusMessage = lyricsStatusMessage,
            onSearchOnline = { viewModel.searchOnlineLyrics(targetTrack) },
            onImportLrcFile = {
                lyricsImportLauncher.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
            },
            onSaveEditedLyrics = { rawText -> viewModel.saveCustomLyrics(targetTrack, rawText) },
            onDeleteLyrics = { viewModel.clearSavedLyrics(targetTrack) },
            onSeekToMs = { tsMs -> viewModel.seekTo(tsMs) },
            onDismiss = { lyricsSheetTrack = null }
        )
    }

    trackForPlaylistDialog?.let { targetTrack ->
        AddToPlaylistDialog(
            track = targetTrack,
            playlists = customPlaylists,
            onAddToExisting = viewModel::addTrackToCustomPlaylist,
            onCreateAndAdd = viewModel::createCustomPlaylistAndAdd,
            onDismiss = { trackForPlaylistDialog = null }
        )
    }
    if (showCastSheet) {
        SmartTvCastSheet(currentTrackTitle = castTargetTitle, onDismiss = { showCastSheet = false })
    }
    if (showSleepTimerSheet) {
        SleepTimerSheet(
            sleepTimerState = sleepTimerState,
            customPresetMinutes = sleepTimerCustomPresets,
            fadeOutDurationSec = appSettings.sleepTimerFadeOutSeconds,
            onStartPreset = viewModel::startSleepTimer,
            onStartSongCount = viewModel::startSongSleepTimer,
            onToggleFadeOut = viewModel::setSleepTimerFadeOut,
            onChangeFadeOutDuration = viewModel::setSleepTimerFadeOutDuration,
            onSaveCustomPreset = viewModel::saveSleepTimerPreset,
            onExtendFiveMinutes = { viewModel.extendSleepTimer(5) },
            onToggleStopAfterCurrentTrack = viewModel::toggleStopAfterCurrentTrack,
            onCancelTimer = viewModel::cancelSleepTimer,
            onDismiss = { showSleepTimerSheet = false }
        )
    }
    if (showThemeSheet) {
        ThemeCustomizationSheet(
            appearancePrefs = appearancePrefs,
            appLanguage = appSettings.appLanguage,
            onToggleDarkTheme = { isDark ->
                scope.launch {
                    preferencesRepository.setActiveCustomThemeId(null)
                    preferencesRepository.setDarkTheme(isDark)
                }
            },
            onSelectPalette = { palette ->
                scope.launch {
                    preferencesRepository.setActiveCustomThemeId(null)
                    preferencesRepository.setUseDynamicMaterialYou(false)
                    preferencesRepository.setAccentPalette(palette)
                }
            },
            onSelectUiStyleMode = viewModel::setUiStyleMode,
            onSelectFontStyle = viewModel::setFontStyle,
            onFontSizeScaleChange = viewModel::setFontSizeScale,
            onPickWallpaper = {
                wallpaperPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onClearWallpaper = viewModel::clearWallpaper,
            onWallpaperDimChange = viewModel::setWallpaperDimAlpha,
            onWallpaperBlurChange = viewModel::setWallpaperBlurDp,
            onToggleMaterialYou = viewModel::setUseDynamicMaterialYou,
            onOpenCustomThemeBuilder = {
                showThemeSheet = false
                showCustomThemeBuilder = true
            },
            onOpenExtremeVisualEditor = {
                showThemeSheet = false
                showExtremeVisualEditor = true
            },
            onRestoreDefaultAppearance = viewModel::restoreOriginalAppearance,
            onDismiss = { showThemeSheet = false }
        )
    }
    if (showExtremeVisualEditor) {
        ExtremeVisualEditorSheet(
            currentStyle = extremeVisualStyle,
            onSaveStyle = viewModel::saveExtremeVisualStyle,
            onOpenCustomThemeBuilder = {
                showExtremeVisualEditor = false
                showCustomThemeBuilder = true
            },
            onOpenPlayerDesigner = {
                showExtremeVisualEditor = false
                showPlayerDesignerSheet = true
            },
            onRestoreOriginalDesign = {
                viewModel.restoreExtremeVisualDefaults()
                viewModel.restoreOriginalAppearance()
            },
            onDismiss = { showExtremeVisualEditor = false }
        )
    }
    if (showPlayerDesignerSheet) {
        PlayerDesignerSheet(
            designerConfig = playerDesignerConfig,
            visualPrefs = playerVisualPrefs,
            onSaveDesigner = viewModel::savePlayerDesignerConfig,
            onSelectControlStyle = viewModel::setPlayerControlStyle,
            onSelectLayoutMode = viewModel::setPlayerLayoutMode,
            onSelectVisualizerStyle = viewModel::setVisualizerStyle,
            onRestoreDefaults = viewModel::restorePlayerDesignerDefaults,
            onDismiss = { showPlayerDesignerSheet = false }
        )
    }
    if (showCustomThemeBuilder) {
        CustomThemeBuilderSheet(
            savedThemes = customThemes,
            activeCustomThemeId = appearancePrefs.activeCustomThemeId,
            onSaveTheme = viewModel::saveAndApplyCustomTheme,
            onApplyTheme = viewModel::applyCustomTheme,
            onDeleteTheme = viewModel::deleteCustomTheme,
            onImportThemeJson = viewModel::importCustomThemeJson,
            onShowMessage = viewModel::showToastMessage,
            onDismiss = { showCustomThemeBuilder = false }
        )
    }
    if (showHomeCustomizeSheet) {
        HomeCustomizationSheet(
            config = homeLayoutConfig,
            savedPresets = homeLayoutPresets,
            onUpdateConfig = viewModel::updateHomeLayoutConfig,
            onSavePreset = viewModel::saveHomeLayoutPreset,
            onApplyPreset = viewModel::applyHomeLayoutPreset,
            onDeletePreset = viewModel::deleteHomeLayoutPreset,
            onResetDefault = viewModel::resetHomeLayoutDefault,
            onDismiss = { showHomeCustomizeSheet = false }
        )
    }
    if (showVoiceSheet) {
        VoiceCommandSheet(
            voiceState = voiceState,
            activeLanguage = appSettings.appLanguage,
            selectedVoiceLang = viewModel.activeVoiceLanguage(),
            handsFreeEnabled = appSettings.handsFreeVoiceEnabled,
            screenOffEnabled = appSettings.screenOffVoiceEnabled,
            onToggleHandsFree = toggleHandsFreeWithPermission,
            onToggleScreenOff = viewModel::setScreenOffVoiceEnabled,
            onSelectVoiceLanguage = viewModel::setVoiceLanguage,
            onStartListening = startVoiceWithPermission,
            onStopListening = viewModel::stopVoiceRecognition,
            onSubmitTextCommand = viewModel::executeVoiceCommandText,
            onDismiss = {
                if (!appSettings.handsFreeVoiceEnabled) {
                    viewModel.stopVoiceRecognition()
                }
                showVoiceSheet = false
            }
        )
    }
    if (showImmersiveVisualOverlay && currentTrack != null) {
        ImmersiveVisualOverlay(
            track = currentTrack,
            playbackState = playbackState,
            audioSessionId = activeAudioSessionId,
            initialVisualizerStyle = playerVisualPrefs.visualizerStyle,
            effectsEnabled = playerDesignerConfig.immersiveEffectsEnabled,
            onToggleEffects = { enabled ->
                viewModel.savePlayerDesignerConfig(
                    playerDesignerConfig.copy(immersiveEffectsEnabled = enabled)
                )
            },
            onSelectVisualizerStyle = viewModel::setVisualizerStyle,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSkipPrevious = viewModel::skipPrevious,
            onSkipNext = viewModel::skipNext,
            onDismiss = { showImmersiveVisualOverlay = false }
        )
    }
}
