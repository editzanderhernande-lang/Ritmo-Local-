package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AlbumPlaylist
import com.example.model.AmbientMoodCategory
import com.example.model.ArtistPlaylist
import com.example.model.CustomPlaylist
import com.example.model.GeneratedAmbientMix
import com.example.model.GeneratedPlaylist
import com.example.model.LocalTrack
import com.example.model.TimeOfDaySlot
import com.example.model.UiStyleMode
import com.example.model.formatDurationMs
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.AlbumPlaylistCard
import com.example.ui.components.ArtistPlaylistCard
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.CustomPlaylistCard
import com.example.ui.components.EmptyPlaylistHint
import com.example.ui.components.PlaylistSummaryBanner
import com.example.ui.components.slotAccentColor
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.MostPlayedItem

private enum class PlaylistHubSection(val label: String, val icon: ImageVector) {
    TIME_OF_DAY("Por Hora", Icons.Rounded.AutoAwesome),
    SMART_MIXES("Mezclas Inteligentes", Icons.Rounded.Shuffle),
    CUSTOM("Mis Playlists", Icons.Rounded.QueueMusic),
    ARTISTS("Artistas", Icons.Rounded.Person),
    ALBUMS("Álbumes", Icons.Rounded.Album),
    FAVORITES("Favoritas", Icons.Rounded.Favorite),
    MOST_PLAYED("Más Reproducidas", Icons.Rounded.LocalFireDepartment)
}

@Composable
fun MixesScreen(
    selectedSlot: TimeOfDaySlot,
    currentRealTimeSlot: TimeOfDaySlot,
    playlistsBySlot: Map<TimeOfDaySlot, GeneratedPlaylist>,
    allTracks: List<LocalTrack>,
    customPlaylists: List<CustomPlaylist>,
    favoriteTracks: List<LocalTrack>,
    favoriteTrackKeys: Set<String>,
    mostPlayedTracks: List<MostPlayedItem>,
    albumPlaylists: List<AlbumPlaylist>,
    artistPlaylists: List<ArtistPlaylist>,
    currentPlayingTrackKey: String?,
    animationsEnabled: Boolean,
    trackActions: TrackActionsCallbacks,
    onSelectSlot: (TimeOfDaySlot) -> Unit,
    onRegeneratePlaylist: (TimeOfDaySlot) -> Unit,
    onPlayTimeSlotPlaylist: (TimeOfDaySlot, Int) -> Unit,
    onPlayTracksQueue: (List<LocalTrack>, Int) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit,
    onCreateCustomPlaylist: (String) -> Unit,
    onRenameCustomPlaylist: (String, String) -> Unit,
    onDeleteCustomPlaylist: (String) -> Unit,
    onRemoveTrackFromCustomPlaylist: (String, LocalTrack) -> Unit,
    onChangeAlbumCover: (AlbumPlaylist) -> Unit,
    ambientMixesByCategory: Map<AmbientMoodCategory, GeneratedAmbientMix> = emptyMap(),
    onRegenerateAmbientMix: (AmbientMoodCategory) -> Unit = {},
    onSaveAmbientMixAsPlaylist: (GeneratedAmbientMix) -> Unit = {},
    initialSectionKey: String? = null,
    onConsumeInitialSectionKey: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isNeon = LocalUiStyleMode.current == UiStyleMode.NEON_MODERN
    var activeSection by remember { mutableStateOf(PlaylistHubSection.TIME_OF_DAY) }
    var selectedSmartMood by remember { mutableStateOf(AmbientMoodCategory.FAVORITES_SMART) }
    LaunchedEffect(initialSectionKey) {
        if (!initialSectionKey.isNullOrBlank()) {
            PlaylistHubSection.entries.firstOrNull { it.name == initialSectionKey }?.let { activeSection = it }
            onConsumeInitialSectionKey()
        }
    }
    var expandedAlbumKey by remember { mutableStateOf<String?>(null) }
    var expandedArtistKey by remember { mutableStateOf<String?>(null) }
    var expandedCustomId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<CustomPlaylist?>(null) }

    val trackByKey = remember(allTracks) { allTracks.associateBy { it.trackKey } }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (isNeon) {
                val neonTitle = when (activeSection) {
                    PlaylistHubSection.ARTISTS -> "Artistas"
                    PlaylistHubSection.ALBUMS -> "Álbumes"
                    PlaylistHubSection.CUSTOM -> "Mis Listas"
                    PlaylistHubSection.FAVORITES -> "Favoritas"
                    PlaylistHubSection.MOST_PLAYED -> "Más escuchadas"
                    PlaylistHubSection.SMART_MIXES -> "Mezclas Inteligentes"
                    PlaylistHubSection.TIME_OF_DAY -> "Explorar Mixes"
                }
                Text(
                    text = neonTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Column {
                    Text(
                        text = strings.mixesTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Mezclas por horario, tus playlists, artistas con sus álbumes, favoritas y más escuchadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlaylistHubSection.entries, key = { it.name }) { section ->
                    val selected = section == activeSection
                    val chipLabel = if (section == PlaylistHubSection.CUSTOM) {
                        if (isNeon) "Mis Listas" else strings.customPlaylistsTab
                    } else section.label
                    if (isNeon) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .border(
                                    width = if (selected) 0.dp else 0.7.dp,
                                    color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clip(RoundedCornerShape(50))
                                .clickable { activeSection = section }
                                .testTag("playlist_hub_${section.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        FilterChip(
                            selected = selected,
                            onClick = { activeSection = section },
                            label = { Text(chipLabel) },
                            leadingIcon = { Icon(section.icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primary.copy(alpha = 0.22f),
                                selectedLabelColor = primary,
                                selectedLeadingIconColor = primary
                            ),
                            modifier = Modifier.testTag("playlist_hub_${section.name.lowercase()}")
                        )
                    }
                }
            }
        }

        when (activeSection) {
            PlaylistHubSection.TIME_OF_DAY -> {
                val activePlaylist = playlistsBySlot[selectedSlot]
                val tracks = activePlaylist?.tracks.orEmpty()
                val accent = slotAccentColor(selectedSlot)

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeOfDaySlot.entries.forEach { slot ->
                            val isSelected = slot == selectedSlot
                            val isNow = slot == currentRealTimeSlot
                            val slotColor = slotAccentColor(slot)
                            val icon = when (slot) {
                                TimeOfDaySlot.MORNING -> Icons.Rounded.WbSunny
                                TimeOfDaySlot.AFTERNOON -> Icons.Rounded.WbTwilight
                                TimeOfDaySlot.NIGHT -> Icons.Rounded.NightsStay
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSlot(slot) },
                                label = { Text(if (isNow) "${slot.displayName} · Ahora" else slot.displayName, maxLines = 1) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = slotColor.copy(alpha = 0.22f),
                                    selectedLabelColor = slotColor,
                                    selectedLeadingIconColor = slotColor
                                ),
                                modifier = Modifier.weight(1f).testTag("slot_chip_${slot.wireValue.lowercase()}")
                            )
                        }
                    }
                }

                item {
                    val bannerRes = when (selectedSlot) {
                        TimeOfDaySlot.MORNING -> R.drawable.img_morning_mix
                        TimeOfDaySlot.AFTERNOON -> R.drawable.img_afternoon_mix
                        TimeOfDaySlot.NIGHT -> R.drawable.img_night_mix
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(205.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = bannerRes),
                            contentDescription = "Arte de mezcla ${selectedSlot.displayName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.85f))))
                        )
                        Column(
                            modifier = Modifier.fillMaxSize().padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(shape = RoundedCornerShape(50), color = accent.copy(alpha = 0.24f)) {
                                    Text(
                                        text = selectedSlot.timeRangeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                                if (tracks.isNotEmpty()) {
                                    Text(
                                        text = "${tracks.size} pistas · ${formatDurationMs(activePlaylist?.totalDurationMs ?: 0L)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = activePlaylist?.title ?: "Mix de ${selectedSlot.displayName}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(selectedSlot.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onPlayTimeSlotPlaylist(selectedSlot, 0) },
                                        enabled = tracks.isNotEmpty(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBg),
                                        modifier = Modifier.testTag("play_mix_button")
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reproducir", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onRegeneratePlaylist(selectedSlot) },
                                        enabled = tracks.isNotEmpty(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        modifier = Modifier.testTag("regenerate_mix_button")
                                    ) {
                                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nueva mezcla")
                                    }
                                }
                            }
                        }
                    }
                }

                if (tracks.isEmpty()) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Sin canciones para generar mezcla",
                            subtitle = "Agrega música local en Inicio para crear tus listas de Mañana, Tarde y Noche."
                        )
                    }
                } else {
                    itemsIndexed(tracks, key = { index, track -> "${selectedSlot.wireValue}_${track.trackKey}_$index" }) { index, track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.trackKey == currentPlayingTrackKey,
                            isFavorite = favoriteTrackKeys.contains(track.trackKey),
                            onClick = { onPlayTimeSlotPlaylist(selectedSlot, index) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            actions = trackActions
                        )
                    }
                }
            }

            PlaylistHubSection.SMART_MIXES -> {
                val selectedMix = ambientMixesByCategory[selectedSmartMood]
                val mixTracks = selectedMix?.tracks.orEmpty()
                val moodAccent = Color(selectedSmartMood.accentArgb)

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("smart_moods_row")
                    ) {
                        items(AmbientMoodCategory.entries, key = { it.id }) { mood ->
                            val selected = mood == selectedSmartMood
                            val chipAccent = Color(mood.accentArgb)
                            FilterChip(
                                selected = selected,
                                onClick = { selectedSmartMood = mood },
                                label = { Text(mood.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipAccent.copy(alpha = 0.22f),
                                    selectedLabelColor = chipAccent
                                ),
                                modifier = Modifier.testTag("smart_mix_chip_${mood.id}")
                            )
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, moodAccent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = selectedMix?.title ?: "Mezcla · ${selectedSmartMood.displayName}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = moodAccent
                            )
                            Text(
                                text = selectedSmartMood.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${mixTracks.size} canciones · ${formatDurationMs(selectedMix?.totalDurationMs ?: 0L)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { if (mixTracks.isNotEmpty()) onPlayTracksQueue(mixTracks, 0) },
                                    enabled = mixTracks.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = moodAccent, contentColor = ObsidianBg),
                                    modifier = Modifier.testTag("smart_mix_play_btn")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reproducir", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onRegenerateAmbientMix(selectedSmartMood) },
                                    enabled = allTracks.isNotEmpty(),
                                    modifier = Modifier.testTag("smart_mix_regen_btn")
                                ) {
                                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Variar")
                                }
                                if (selectedMix != null && mixTracks.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { onSaveAmbientMixAsPlaylist(selectedMix) },
                                        modifier = Modifier.testTag("smart_mix_save_playlist_btn")
                                    ) {
                                        Text("Guardar")
                                    }
                                }
                            }
                        }
                    }
                }

                if (mixTracks.isEmpty()) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Sin canciones para esta mezcla inteligente",
                            subtitle = "Escucha o agrega canciones en Inicio para alimentar tus mezclas personalizadas."
                        )
                    }
                } else {
                    itemsIndexed(mixTracks, key = { idx, t -> "smart_${selectedSmartMood.id}_${t.trackKey}_$idx" }) { idx, track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.trackKey == currentPlayingTrackKey,
                            isFavorite = favoriteTrackKeys.contains(track.trackKey),
                            onClick = { onPlayTracksQueue(mixTracks, idx) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            actions = trackActions
                        )
                    }
                }
            }

            PlaylistHubSection.CUSTOM -> {
                if (isNeon) {
                    val morningCount = playlistsBySlot[TimeOfDaySlot.MORNING]?.tracks?.size ?: 0
                    val afternoonCount = playlistsBySlot[TimeOfDaySlot.AFTERNOON]?.tracks?.size ?: 0
                    val nightCount = playlistsBySlot[TimeOfDaySlot.NIGHT]?.tracks?.size ?: 0

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MisListasQuickRow(
                                icon = Icons.Rounded.Favorite,
                                iconGradient = listOf(Color(0xFFFF4081), Color(0xFFF50057)),
                                title = "Favoritas",
                                subtitle = "(${favoriteTracks.size} canciones)",
                                onClick = { activeSection = PlaylistHubSection.FAVORITES }
                            )
                            MisListasQuickRow(
                                icon = Icons.Rounded.WbSunny,
                                iconGradient = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                                title = "Mañana ☀️",
                                subtitle = "($morningCount canciones)",
                                onClick = {
                                    onSelectSlot(TimeOfDaySlot.MORNING)
                                    activeSection = PlaylistHubSection.TIME_OF_DAY
                                }
                            )
                            MisListasQuickRow(
                                icon = Icons.Rounded.WbTwilight,
                                iconGradient = listOf(Color(0xFFFB923C), Color(0xFFEA580C)),
                                title = "Tarde 🌅",
                                subtitle = "($afternoonCount canciones)",
                                onClick = {
                                    onSelectSlot(TimeOfDaySlot.AFTERNOON)
                                    activeSection = PlaylistHubSection.TIME_OF_DAY
                                }
                            )
                            MisListasQuickRow(
                                icon = Icons.Rounded.NightsStay,
                                iconGradient = listOf(Color(0xFF6366F1), Color(0xFF312E81)),
                                title = "Noche 🌙",
                                subtitle = "($nightCount canciones)",
                                onClick = {
                                    onSelectSlot(TimeOfDaySlot.NIGHT)
                                    activeSection = PlaylistHubSection.TIME_OF_DAY
                                }
                            )
                            MisListasQuickRow(
                                icon = Icons.Rounded.Shuffle,
                                iconGradient = listOf(primary, secondary),
                                title = "Mix Aleatorio",
                                subtitle = "(Reproducción inteligente)",
                                onClick = {
                                    if (allTracks.isNotEmpty()) {
                                        onPlayTracksQueue(allTracks.shuffled(), 0)
                                    }
                                }
                            )
                        }
                    }
                }

                if (customPlaylists.isEmpty() && !isNeon) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.QueueMusic,
                            title = "Aún no creas playlists",
                            subtitle = "Crea tu propia playlist o usa el menú ⋮ de cualquier canción para agregar temas."
                        )
                    }
                } else if (customPlaylists.isNotEmpty()) {
                    items(customPlaylists, key = { it.id }) { pl ->
                        val resolved = pl.trackKeys.mapNotNull { trackByKey[it] }
                        CustomPlaylistCard(
                            playlist = pl,
                            resolvedTracks = resolved,
                            isExpanded = expandedCustomId == pl.id,
                            currentPlayingTrackKey = currentPlayingTrackKey,
                            favoriteTrackKeys = favoriteTrackKeys,
                            trackActions = trackActions,
                            onToggleExpand = {
                                expandedCustomId = if (expandedCustomId == pl.id) null else pl.id
                            },
                            onPlayPlaylist = { onPlayTracksQueue(resolved, 0) },
                            onRenamePlaylist = { renamingPlaylist = pl },
                            onDeletePlaylist = { onDeleteCustomPlaylist(pl.id) },
                            onRemoveTrack = { track -> onRemoveTrackFromCustomPlaylist(pl.id, track) },
                            onPlayTrackAt = { idx -> onPlayTracksQueue(resolved, idx) },
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }

                item {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(if (isNeon) 50 else 14),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_custom_playlist_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNeon) "+ Crear Playlist" else strings.createPlaylist, fontWeight = FontWeight.Bold)
                    }
                }
            }

            PlaylistHubSection.ARTISTS -> {
                if (artistPlaylists.isEmpty()) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.Person,
                            title = "Sin artistas locales",
                            subtitle = "Escanea o importa música local para explorar artistas, sus álbumes y canciones."
                        )
                    }
                } else {
                    items(artistPlaylists, key = { it.artistName }) { artist ->
                        ArtistPlaylistCard(
                            artist = artist,
                            isExpanded = expandedArtistKey == artist.artistName,
                            animationsEnabled = animationsEnabled,
                            currentPlayingTrackKey = currentPlayingTrackKey,
                            favoriteTrackKeys = favoriteTrackKeys,
                            trackActions = trackActions,
                            onToggleExpand = {
                                expandedArtistKey = if (expandedArtistKey == artist.artistName) null else artist.artistName
                            },
                            onPlayArtistAll = { onPlayTracksQueue(artist.tracks, 0) },
                            onPlayAlbum = { album -> onPlayTracksQueue(album.tracks, 0) },
                            onPlayTrackAt = { idx -> onPlayTracksQueue(artist.tracks, idx) },
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }

            PlaylistHubSection.ALBUMS -> {
                if (albumPlaylists.isEmpty()) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.Album,
                            title = "Sin álbumes locales",
                            subtitle = "Escanea o importa música local para ver tus álbumes organizados."
                        )
                    }
                } else if (isNeon) {
                    val rows = albumPlaylists.chunked(3)
                    itemsIndexed(rows, key = { idx, _ -> "album_row_$idx" }) { _, rowAlbums ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowAlbums.forEach { album ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onPlayTracksQueue(album.tracks, 0) }
                                ) {
                                    AlbumArtwork(
                                        albumArtUri = album.albumArtUri,
                                        contentDescription = album.albumName,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                        cornerRadius = 14.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = album.albumName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "(${album.artistName})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                            repeat(3 - rowAlbums.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(albumPlaylists, key = { "${it.albumName}|${it.artistName}" }) { album ->
                        val albumKey = "${album.albumName}|${album.artistName}"
                        AlbumPlaylistCard(
                            album = album,
                            isExpanded = expandedAlbumKey == albumKey,
                            animationsEnabled = animationsEnabled,
                            currentPlayingTrackKey = currentPlayingTrackKey,
                            favoriteTrackKeys = favoriteTrackKeys,
                            trackActions = trackActions,
                            onToggleExpand = {
                                expandedAlbumKey = if (expandedAlbumKey == albumKey) null else albumKey
                            },
                            onPlayAlbum = { onPlayTracksQueue(album.tracks, 0) },
                            onChangeAlbumCover = { onChangeAlbumCover(album) },
                            onPlayTrackAt = { idx -> onPlayTracksQueue(album.tracks, idx) },
                            onToggleFavorite = onToggleFavorite
                        )
                    }
                }
            }

            PlaylistHubSection.FAVORITES -> {
                item {
                    PlaylistSummaryBanner(
                        title = "Tus Canciones Favoritas",
                        subtitle = "${favoriteTracks.size} canciones marcadas como favoritas",
                        accent = secondary,
                        hasTracks = favoriteTracks.isNotEmpty(),
                        onPlayAll = { onPlayTracksQueue(favoriteTracks, 0) },
                        onShufflePlay = { onPlayTracksQueue(favoriteTracks.shuffled(), 0) }
                    )
                }
                if (favoriteTracks.isEmpty()) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.Favorite,
                            title = "Aún no tienes favoritas",
                            subtitle = "Toca el ícono de corazón en cualquier canción para agregarla a favoritas."
                        )
                    }
                } else {
                    itemsIndexed(favoriteTracks, key = { _, t -> "fav_${t.trackKey}" }) { index, track ->
                        TrackRowItem(
                            track = track,
                            isCurrent = track.trackKey == currentPlayingTrackKey,
                            isFavorite = true,
                            onClick = { onPlayTracksQueue(favoriteTracks, index) },
                            onToggleFavorite = { onToggleFavorite(track) },
                            actions = trackActions
                        )
                    }
                }
            }

            PlaylistHubSection.MOST_PLAYED -> {
                val tracks = mostPlayedTracks.map { it.track }
                item {
                    PlaylistSummaryBanner(
                        title = "Más Reproducidas del Mes",
                        subtitle = "${mostPlayedTracks.size} éxitos personales según tu historial",
                        accent = primary,
                        hasTracks = tracks.isNotEmpty(),
                        onPlayAll = { onPlayTracksQueue(tracks, 0) },
                        onShufflePlay = { onPlayTracksQueue(tracks.shuffled(), 0) }
                    )
                }
                if (mostPlayedTracks.isEmpty()) {
                    item {
                        EmptyPlaylistHint(
                            icon = Icons.Rounded.LocalFireDepartment,
                            title = "Sin historial de reproducciones todavía",
                            subtitle = "Escucha tus canciones y aquí se ordenarán automáticamente las más reproducidas."
                        )
                    }
                } else {
                    itemsIndexed(mostPlayedTracks, key = { _, item -> "top_${item.track.trackKey}" }) { index, entry ->
                        TrackRowItem(
                            track = entry.track,
                            isCurrent = entry.track.trackKey == currentPlayingTrackKey,
                            isFavorite = favoriteTrackKeys.contains(entry.track.trackKey),
                            onClick = { onPlayTracksQueue(tracks, index) },
                            onToggleFavorite = { onToggleFavorite(entry.track) },
                            badgeOverride = "#${index + 1} · ${entry.playCount}x",
                            actions = trackActions
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            titleText = "Crear nueva playlist",
            confirmLabel = "Crear",
            onConfirm = onCreateCustomPlaylist,
            onDismiss = { showCreateDialog = false }
        )
    }

    renamingPlaylist?.let { target ->
        CreatePlaylistDialog(
            initialName = target.name,
            titleText = "Renombrar playlist",
            confirmLabel = "Guardar",
            onConfirm = { newName -> onRenameCustomPlaylist(target.id, newName) },
            onDismiss = { renamingPlaylist = null }
        )
    }
}

@Composable
private fun MisListasQuickRow(
    icon: ImageVector,
    iconGradient: List<Color>,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(iconGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
