package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Tune
import com.example.data.stats.RealTimeStatsSummary
import com.example.data.stats.StatsAnalyticsEngine
import com.example.model.HomeLayoutConfig
import com.example.model.HomeSectionId
import com.example.model.HomeShortcutType
import com.example.model.LocalTrack
import com.example.model.PlaybackProgress
import com.example.model.TimeOfDaySlot
import com.example.model.UiState
import com.example.model.UiStyleMode
import com.example.model.formatDurationMs
import com.example.model.formatPlaybackTimeReadable
import com.example.player.SleepTimerUiState
import com.example.player.formatSleepTimerCountdown
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.TrackOverflowMenuButton
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.EmeraldSync
import com.example.ui.theme.LocalUiStyleMode

data class TrackActionsCallbacks(
    val onPlayNext: (LocalTrack) -> Unit = {},
    val onAddToPlaylist: (LocalTrack) -> Unit = {},
    val onChangeCoverFromGallery: (LocalTrack) -> Unit = {},
    val onFetchCoverOnline: (LocalTrack) -> Unit = {},
    val onShareTrack: (LocalTrack) -> Unit = {},
    val onCastToTv: (LocalTrack) -> Unit = {},
    val onRecommendInChat: (LocalTrack) -> Unit = {},
    val onOpenLyrics: (LocalTrack) -> Unit = {}
)

@Composable
fun LibraryScreen(
    tracks: List<LocalTrack>,
    favoriteTrackKeys: Set<String>,
    searchQuery: String,
    isScanning: Boolean,
    hasAudioPermission: Boolean,
    currentPlayingTrackKey: String?,
    totalPlaybackSeconds: Long,
    cloudProgressState: UiState<PlaybackProgress?>,
    sleepTimerState: SleepTimerUiState,
    trackActions: TrackActionsCallbacks,
    homeLayoutConfig: HomeLayoutConfig = HomeLayoutConfig.defaultConfig(),
    realTimeSummary: RealTimeStatsSummary = StatsAnalyticsEngine.computeRealTimeSummary(emptyList()),
    onSearchQueryChange: (String) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onImportAudioFiles: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onOpenThemeCustomization: () -> Unit,
    onOpenHomeCustomization: () -> Unit = {},
    onOpenRecapAndWrapped: () -> Unit = {},
    onOpenLibraryOrganizer: () -> Unit = {},
    onOpenExtremeVisualEditor: () -> Unit = {},
    onHomeShortcutClick: (HomeShortcutType) -> Unit = {},
    onOpenSleepTimer: () -> Unit,
    onOpenCastSheet: () -> Unit,
    onOpenVoiceControl: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onTrackClick: (LocalTrack) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit,
    onResumeCloudClick: (PlaybackProgress) -> Unit,
    onPlayTimeSlot: (TimeOfDaySlot) -> Unit = {},
    onNavigateToMixesSection: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isNeon = LocalUiStyleMode.current == UiStyleMode.NEON_MODERN
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    var showNeonSearch by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val filteredTracks = if (searchQuery.isBlank()) {
        tracks
    } else {
        val q = searchQuery.trim().lowercase()
        tracks.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q) ||
                it.formatLabel.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (isNeon) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ritmo Local",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showNeonSearch = !showNeonSearch },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("neon_search_toggle")
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = "Buscar",
                                tint = if (showNeonSearch) primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(
                            onClick = onOpenVoiceControl,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_voice_button")
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = "Voz sin internet", tint = primary)
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("library_settings_button")
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = strings.settingsTitle, tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Más acciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Importar archivos de audio") },
                                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onImportAudioFiles()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Escanear biblioteca local") },
                                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onRefreshLibrary()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Temas y fondo de pantalla") },
                                    leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenThemeCustomization()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Personalizar inicio") },
                                    leadingIcon = { Icon(Icons.Rounded.DashboardCustomize, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenHomeCustomization()
                                    },
                                    modifier = Modifier.testTag("library_customize_home_btn")
                                )
                                DropdownMenuItem(
                                    text = { Text("Editor Visual Extremo 2.0") },
                                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenExtremeVisualEditor()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Organizador inteligente") },
                                    leadingIcon = { Icon(Icons.Rounded.LibraryAddCheck, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenLibraryOrganizer()
                                    },
                                    modifier = Modifier.testTag("library_open_organizer_btn")
                                )
                                DropdownMenuItem(
                                    text = { Text("Ritmo Recap & Wrapped") },
                                    leadingIcon = { Icon(Icons.Rounded.AutoGraph, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenRecapAndWrapped()
                                    },
                                    modifier = Modifier.testTag("library_open_recap_btn")
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.castToTv) },
                                    leadingIcon = { Icon(Icons.Rounded.Cast, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenCastSheet()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Temporizador de apagado") },
                                    leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenSleepTimer()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.libraryTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${tracks.size} pistas · ${formatPlaybackTimeReadable(totalPlaybackSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onOpenVoiceControl,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_voice_button")
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = "Voz sin internet", tint = primary)
                            }
                            IconButton(
                                onClick = onOpenCastSheet,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_cast_tv_button")
                            ) {
                                Icon(Icons.Rounded.Cast, contentDescription = strings.castToTv, tint = primary)
                            }
                            IconButton(
                                onClick = onOpenSleepTimer,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_sleep_timer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = "Temporizador",
                                    tint = if (sleepTimerState.isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = onOpenHomeCustomization,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_customize_home_btn")
                            ) {
                                Icon(Icons.Rounded.DashboardCustomize, contentDescription = "Personalizar inicio", tint = primary)
                            }
                            IconButton(
                                onClick = onOpenThemeCustomization,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_theme_button")
                            ) {
                                Icon(Icons.Rounded.Palette, contentDescription = "Temas y fondo", tint = primary)
                            }
                            IconButton(
                                onClick = onImportAudioFiles,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("import_audio_button")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = stringResource(id = R.string.import_audio_files), tint = primary)
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("library_settings_button")
                            ) {
                                Icon(Icons.Rounded.Settings, contentDescription = strings.settingsTitle, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        if (sleepTimerState.isActive) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = primary.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenSleepTimer)
                        .testTag("library_active_sleep_timer_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (sleepTimerState.stopAfterCurrentTrack) {
                                    "Temporizador: al terminar la canción actual"
                                } else {
                                    "Temporizador activo: ${formatSleepTimerCountdown(sleepTimerState.remainingSeconds)}"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = primary
                            )
                        }
                        Text("Configurar", style = MaterialTheme.typography.labelMedium, color = primary)
                    }
                }
            }
        }

        val savedProgress = (cloudProgressState as? UiState.Success)?.data
        if (savedProgress != null && savedProgress.trackTitle.isNotBlank()) {
            item {
                CloudResumeBanner(progress = savedProgress, onResume = { onResumeCloudClick(savedProgress) })
            }
        }

        if (!hasAudioPermission) {
            item {
                PermissionCard(onGrantPermission = onRequestAudioPermission, onImportAudio = onImportAudioFiles)
            }
        }

        if (!isNeon || showNeonSearch || searchQuery.isNotBlank()) {
            item {
                AnimatedVisibility(visible = !isNeon || showNeonSearch || searchQuery.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text(strings.searchPlaceholder) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f).testTag("library_search_input")
                        )
                        IconButton(
                            onClick = onRefreshLibrary,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_library_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Escanear música local", tint = primary)
                        }
                    }
                }
            }
        }

        val layout = homeLayoutConfig.normalized()
        layout.orderedSections
            .filter { it !in layout.hiddenSections }
            .forEach { section ->
                when (section) {
                    HomeSectionId.QUICK_TABS -> {
                        item(key = "section_quick_tabs") {
                            val quickTabs = listOf(
                                "Inicio" to "",
                                "Listas" to "CUSTOM",
                                "Artistas" to "ARTISTS",
                                "Álbumes" to "ALBUMS"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().testTag("home_section_quick_tabs"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickTabs.forEach { (label, sectionKey) ->
                                    val selected = sectionKey.isEmpty()
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) primary else MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (selected) 0.dp else 0.8.dp,
                                                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .clip(RoundedCornerShape(50))
                                            .clickable { if (sectionKey.isNotEmpty()) onNavigateToMixesSection(sectionKey) }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.PINNED_SHORTCUTS -> {
                        if (layout.pinnedShortcuts.isNotEmpty()) {
                            item(key = "section_pinned_shortcuts") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("home_section_pinned_shortcuts")
                                ) {
                                    items(layout.pinnedShortcuts, key = { "sc_${it.id}" }) { shortcut ->
                                        val icon = when (shortcut) {
                                            HomeShortcutType.FAVORITES -> Icons.Rounded.Favorite
                                            HomeShortcutType.MIXES -> Icons.Rounded.QueueMusic
                                            HomeShortcutType.EQUALIZER -> Icons.Rounded.Equalizer
                                            HomeShortcutType.STATS -> Icons.Rounded.AutoGraph
                                            HomeShortcutType.PROFILE -> Icons.Rounded.Person
                                            HomeShortcutType.HISTORY -> Icons.Rounded.History
                                            HomeShortcutType.ORGANIZER -> Icons.Rounded.LibraryAddCheck
                                            HomeShortcutType.DIAGNOSTICS -> Icons.Rounded.HealthAndSafety
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier
                                                .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { onHomeShortcutClick(shortcut) }
                                                .testTag("pinned_shortcut_${shortcut.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                                                Text(shortcut.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.LIVE_STATS -> {
                        if (searchQuery.isBlank() && (layout.showTodayTimeStat || layout.showTodayPlaysStat || layout.showTodayArtistsStat || layout.showRecapBannerCard)) {
                            item(key = "section_live_stats") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("home_section_live_stats")
                                ) {
                                    if (layout.showTodayTimeStat || layout.showTodayPlaysStat || layout.showTodayArtistsStat) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (layout.showTodayTimeStat) {
                                                HomeLiveStatChip(
                                                    label = "Hoy",
                                                    value = formatPlaybackTimeReadable(realTimeSummary.todaySeconds),
                                                    accent = primary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (layout.showTodayPlaysStat) {
                                                HomeLiveStatChip(
                                                    label = "Reprods",
                                                    value = "${realTimeSummary.todayPlays}",
                                                    accent = secondary,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            if (layout.showTodayArtistsStat) {
                                                HomeLiveStatChip(
                                                    label = "Artistas",
                                                    value = "${realTimeSummary.todayUniqueArtists}",
                                                    accent = EmeraldSync,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                    if (layout.showRecapBannerCard) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable(onClick = onOpenRecapAndWrapped)
                                                .testTag("home_recap_banner_card")
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text("Ritmo Recap & Wrapped", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            if (realTimeSummary.allTimeSeconds > 0L)
                                                                "Acumulado: ${formatPlaybackTimeReadable(realTimeSummary.allTimeSeconds)} · ${realTimeSummary.allTimePlays} reprods"
                                                            else "Explora tu actividad real por días y meses",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Abrir Recap", tint = primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.MOOD_BANNERS -> {
                        if (searchQuery.isBlank() && !showNeonSearch) {
                            item(key = "section_mood_banners") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.testTag("home_section_mood_banners")
                                ) {
                                    TimeOfDaySlot.entries.forEach { slot ->
                                        val artRes = when (slot) {
                                            TimeOfDaySlot.MORNING -> R.drawable.img_morning_mix
                                            TimeOfDaySlot.AFTERNOON -> R.drawable.img_afternoon_mix
                                            TimeOfDaySlot.NIGHT -> R.drawable.img_night_mix
                                        }
                                        val bannerTitle = when (slot) {
                                            TimeOfDaySlot.MORNING -> "Buenos días ✨"
                                            TimeOfDaySlot.AFTERNOON -> "Tarde 🌅"
                                            TimeOfDaySlot.NIGHT -> "Noche 🌙"
                                        }
                                        val bannerSub = when (slot) {
                                            TimeOfDaySlot.MORNING -> "Listo para tu playlist de la mañana"
                                            TimeOfDaySlot.AFTERNOON -> "Tu música para la tarde"
                                            TimeOfDaySlot.NIGHT -> "Relájate con la mejor música"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(86.dp)
                                                .clip(RoundedCornerShape(18.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                                .clickable { onPlayTimeSlot(slot) }
                                                .testTag("home_slot_banner_${slot.wireValue.lowercase()}")
                                        ) {
                                            Image(
                                                painter = painterResource(id = artRes),
                                                contentDescription = bannerTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(
                                                                Color(0xE0110D22),
                                                                Color(0x99191333),
                                                                Color(0x33000000)
                                                            )
                                                        )
                                                    )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = bannerTitle,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = bannerSub,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White.copy(alpha = 0.88f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                FilledIconButton(
                                                    onClick = { onPlayTimeSlot(slot) },
                                                    shape = CircleShape,
                                                    colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = primary,
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(40.dp)
                                                ) {
                                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir ${slot.displayName}")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HomeSectionId.RECENT_PLAYS -> {
                        if (searchQuery.isBlank() && !showNeonSearch && tracks.isNotEmpty()) {
                            item(key = "section_recent_plays") {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.testTag("home_section_recent_plays")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToMixesSection("MOST_PLAYED") },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reproducción reciente",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(tracks.take(10), key = { "recent_${it.trackKey}" }) { track ->
                                            Column(
                                                modifier = Modifier
                                                    .width(104.dp)
                                                    .clickable { onTrackClick(track) }
                                            ) {
                                                AlbumArtwork(
                                                    albumArtUri = track.albumArtUri,
                                                    contentDescription = track.title,
                                                    modifier = Modifier.size(104.dp),
                                                    cornerRadius = 16.dp
                                                )
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.artist,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = secondary,
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

                    HomeSectionId.TRACK_LIST -> {
                        if (isScanning) {
                            item(key = "section_scanning") {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = primary)
                                }
                            }
                        } else if (filteredTracks.isEmpty()) {
                            item(key = "section_empty_tracks") {
                                EmptyLibraryCard(
                                    hasPermission = hasAudioPermission,
                                    hasSearchFilter = searchQuery.isNotBlank(),
                                    onGrantPermission = onRequestAudioPermission,
                                    onImportAudio = onImportAudioFiles
                                )
                            }
                        } else {
                            if (isNeon) {
                                item(key = "section_tracks_header") {
                                    Text(
                                        text = "Tu música (${filteredTracks.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.testTag("home_section_track_list_header")
                                    )
                                }
                            }
                            items(filteredTracks, key = { it.trackKey }) { track ->
                                TrackRowItem(
                                    track = track,
                                    isCurrent = track.trackKey == currentPlayingTrackKey,
                                    isFavorite = favoriteTrackKeys.contains(track.trackKey),
                                    onClick = { onTrackClick(track) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    showFormatBadge = layout.showFormatBadge,
                                    showAlbumName = layout.showAlbumName,
                                    showDuration = layout.showDuration,
                                    actions = trackActions
                                )
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun HomeLiveStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun CloudResumeBanner(progress: PlaybackProgress, onResume: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Brush.horizontalGradient(listOf(primary.copy(alpha = 0.7f), secondary.copy(alpha = 0.5f))), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(primary.copy(alpha = 0.14f), secondary.copy(alpha = 0.08f))))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = EmeraldSync, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(id = R.string.resume_title), style = MaterialTheme.typography.labelMedium, color = EmeraldSync)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(progress.trackTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${progress.artistName} · ${formatDurationMs(progress.positionMs)} / ${formatDurationMs(progress.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onResume,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                modifier = Modifier.minimumInteractiveComponentSize().testTag("resume_cloud_button")
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reanudar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionCard(onGrantPermission: () -> Unit, onImportAudio: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(stringResource(id = R.string.permission_audio_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(stringResource(id = R.string.permission_audio_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onGrantPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.testTag("grant_audio_permission_button")
                ) {
                    Text(stringResource(id = R.string.grant_permission), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onImportAudio, modifier = Modifier.testTag("import_audio_fallback_button")) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Elegir archivos")
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(
    hasPermission: Boolean,
    hasSearchFilter: Boolean,
    onGrantPermission: () -> Unit,
    onImportAudio: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = primary, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(if (hasSearchFilter) "Sin coincidencias" else "No hay música local detectada", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasSearchFilter) {
                    "Prueba con otro título, artista, álbum o formato."
                } else {
                    "Agrega archivos MP3, FLAC, WAV, OGG, OPUS, M4A o AAC desde tu dispositivo para reproducirlos offline."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasSearchFilter) {
                Spacer(modifier = Modifier.height(16.dp))
                FilledTonalButton(
                    onClick = if (hasPermission) onImportAudio else onGrantPermission,
                    modifier = Modifier.testTag("empty_import_audio_button")
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasPermission) "Importar archivos de audio" else "Permitir lectura de audio")
                }
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: LocalTrack,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    badgeOverride: String? = null,
    showFormatBadge: Boolean = true,
    showAlbumName: Boolean = true,
    showDuration: Boolean = true,
    actions: TrackActionsCallbacks = TrackActionsCallbacks()
) {
    val isMinimal = LocalUiStyleMode.current == UiStyleMode.MINIMALIST
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val rowShape = RoundedCornerShape(if (isMinimal) 14.dp else 16.dp)
    Surface(
        shape = rowShape,
        color = if (isCurrent) primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    isCurrent -> Modifier.border(1.dp, primary.copy(alpha = 0.45f), rowShape)
                    isMinimal -> Modifier.border(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), rowShape)
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag("track_item_${track.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtwork(
                albumArtUri = track.albumArtUri,
                contentDescription = "Portada de ${track.title}",
                modifier = Modifier.size(50.dp),
                cornerRadius = 12.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (showFormatBadge) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text(
                                text = badgeOverride ?: track.formatLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = primary,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = if (showAlbumName) "${track.artist} · ${track.album}" else track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (isCurrent) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = "Reproduciendo", tint = primary, modifier = Modifier.size(18.dp))
            }
            if (showDuration) {
                Text(
                    text = formatDurationMs(track.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("favorite_toggle_${track.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritas" else "Agregar a favoritas",
                    tint = if (isFavorite) secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            TrackOverflowMenuButton(
                track = track,
                onPlayNext = actions.onPlayNext,
                onAddToPlaylist = actions.onAddToPlaylist,
                onChangeCoverFromGallery = actions.onChangeCoverFromGallery,
                onFetchCoverOnline = actions.onFetchCoverOnline,
                onShareTrack = actions.onShareTrack,
                onCastToTv = actions.onCastToTv,
                onRecommendInChat = actions.onRecommendInChat,
                onOpenLyrics = actions.onOpenLyrics
            )
        }
    }
}
