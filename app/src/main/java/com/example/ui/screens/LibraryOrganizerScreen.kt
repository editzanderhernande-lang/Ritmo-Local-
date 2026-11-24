package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.stats.TrackMetadataOverrideEntity
import com.example.data.stats.TrackStatsEntity
import com.example.library.LibraryFilterCriteria
import com.example.library.LibraryOrganizerEngine
import com.example.library.LibrarySortOption
import com.example.model.LocalTrack
import com.example.model.formatDurationMs
import com.example.ui.components.AlbumArtwork

private enum class OrganizerTab(val label: String) {
    FILTERED("Explorar y Editar"),
    DUPLICATES("Duplicados"),
    GENRES("Por Género"),
    MISSING_COVERS("Sin Carátula")
}

@Composable
fun LibraryOrganizerScreen(
    allTracks: List<LocalTrack>,
    trackStatsByKey: Map<String, TrackStatsEntity>,
    metadataOverrides: Map<String, TrackMetadataOverrideEntity>,
    onPlayTracksQueue: (List<LocalTrack>, Int) -> Unit,
    onFetchCoverOnline: (LocalTrack) -> Unit,
    onPickCoverFromGallery: (LocalTrack) -> Unit,
    onSaveMetadataOverride: (trackKey: String, title: String, artist: String, album: String, genre: String) -> Unit,
    onClearMetadataOverride: (trackKey: String) -> Unit,
    onHideDuplicateTrack: (LocalTrack, Boolean) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(OrganizerTab.FILTERED) }
    var criteria by remember { mutableStateOf(LibraryFilterCriteria()) }
    var editingTrack by remember { mutableStateOf<LocalTrack?>(null) }
    var duplicateToHide by remember { mutableStateOf<LocalTrack?>(null) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val availableGenres = remember(allTracks) { LibraryOrganizerEngine.availableGenres(allTracks) }
    val availableFormats = remember(allTracks) { LibraryOrganizerEngine.availableFormats(allTracks) }
    val filteredTracks = remember(allTracks, criteria, trackStatsByKey) {
        LibraryOrganizerEngine.filterAndSort(allTracks, criteria, trackStatsByKey)
    }
    val duplicateGroups = remember(allTracks) { LibraryOrganizerEngine.findDuplicateGroups(allTracks) }
    val genreGroups = remember(allTracks) { LibraryOrganizerEngine.buildGenreGroups(allTracks) }
    val missingCovers = remember(allTracks) { LibraryOrganizerEngine.findTracksMissingCover(allTracks) }
    val hiddenDuplicatesCount = remember(metadataOverrides) {
        metadataOverrides.values.count { it.hiddenAsDuplicate }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("organizer_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("organizer_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
                Column {
                    Text("Organizador Inteligente", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Filtros avanzados, edición segura de etiquetas y detección de duplicados sin borrar archivos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(OrganizerTab.entries, key = { it.name }) { tab ->
                    val badge = when (tab) {
                        OrganizerTab.DUPLICATES -> " (${duplicateGroups.size})"
                        OrganizerTab.MISSING_COVERS -> " (${missingCovers.size})"
                        else -> ""
                    }
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text("${tab.label}$badge") },
                        modifier = Modifier.testTag("organizer_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }

        when (activeTab) {
            OrganizerTab.FILTERED -> {
                item {
                    OutlinedTextField(
                        value = criteria.query,
                        onValueChange = { criteria = criteria.copy(query = it) },
                        placeholder = { Text("Filtrar por título, artista, álbum o género...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().testTag("organizer_search_input")
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LibrarySortOption.entries, key = { it.id }) { sortOpt ->
                            FilterChip(
                                selected = criteria.sortOption == sortOpt,
                                onClick = { criteria = criteria.copy(sortOption = sortOpt) },
                                label = { Text(sortOpt.label) },
                                leadingIcon = { Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("organizer_sort_${sortOpt.id}")
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = criteria.onlyMissingArtwork,
                                onClick = { criteria = criteria.copy(onlyMissingArtwork = !criteria.onlyMissingArtwork) },
                                label = { Text("Sin portada") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = criteria.onlyUnplayed,
                                onClick = { criteria = criteria.copy(onlyUnplayed = !criteria.onlyUnplayed) },
                                label = { Text("Sin reproducir") }
                            )
                        }
                        items(availableFormats, key = { "fmt_$it" }) { fmt ->
                            FilterChip(
                                selected = criteria.selectedFormat == fmt,
                                onClick = {
                                    criteria = criteria.copy(
                                        selectedFormat = if (criteria.selectedFormat == fmt) "" else fmt
                                    )
                                },
                                label = { Text(fmt) }
                            )
                        }
                        items(availableGenres, key = { "gen_$it" }) { genre ->
                            FilterChip(
                                selected = criteria.selectedGenre == genre,
                                onClick = {
                                    criteria = criteria.copy(
                                        selectedGenre = if (criteria.selectedGenre == genre) "" else genre
                                    )
                                },
                                label = { Text(genre) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "${filteredTracks.size} canciones coinciden",
                        style = MaterialTheme.typography.labelMedium,
                        color = primary
                    )
                }

                items(filteredTracks, key = { "org_${it.trackKey}" }) { track ->
                    val hasOverride = metadataOverrides.containsKey(track.trackKey)
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val idx = filteredTracks.indexOfFirst { it.trackKey == track.trackKey }.coerceAtLeast(0)
                                    onPlayTracksQueue(filteredTracks, idx)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtwork(
                                albumArtUri = track.albumArtUri,
                                contentDescription = track.title,
                                modifier = Modifier.size(46.dp),
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${track.artist} · ${track.album}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val genreTag = track.genre.ifBlank { "Sin género" }
                                Text(
                                    text = "$genreTag · ${track.formatLabel} · ${formatDurationMs(track.durationMs)}" +
                                        if (hasOverride) " · Editada" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasOverride) secondary else primary
                                )
                            }
                            IconButton(
                                onClick = { editingTrack = track },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("organizer_edit_meta_${track.trackKey}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Editar metadatos de ${track.title}", tint = primary)
                            }
                        }
                    }
                }
            }

            OrganizerTab.DUPLICATES -> {
                if (hiddenDuplicatesCount > 0) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = secondary.copy(alpha = 0.14f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$hiddenDuplicatesCount copia(s) ocultas de la biblioteca (archivos intactos).",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        metadataOverrides.values.filter { it.hiddenAsDuplicate }.forEach { ov ->
                                            onClearMetadataOverride(ov.trackKey)
                                        }
                                    }
                                ) {
                                    Text("Restaurar")
                                }
                            }
                        }
                    }
                }

                if (duplicateGroups.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = primary)
                                Text("No se detectaron duplicados activos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Cuando existan varias pistas con el mismo título y artista, podrás revisarlas aquí y ocultar copias sin borrar ningún archivo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(duplicateGroups, key = { it.normalizedSignature }) { group ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().border(1.dp, primary.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "${group.displayTitle} · ${group.displayArtist} (${group.tracks.size} copias)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                group.tracks.forEach { copy ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(copy.album, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                "${copy.formatLabel} · ${formatDurationMs(copy.durationMs)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { duplicateToHide = copy },
                                            modifier = Modifier.testTag("organizer_hide_dup_${copy.trackKey}")
                                        ) {
                                            Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ocultar copia")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OrganizerTab.GENRES -> {
                items(genreGroups, key = { "grp_${it.genreName}" }) { group ->
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayTracksQueue(group.tracks, 0) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(primary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = primary)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(group.genreName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("${group.tracks.size} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { onPlayTracksQueue(group.tracks, 0) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir ${group.genreName}", tint = primary)
                            }
                        }
                    }
                }
            }

            OrganizerTab.MISSING_COVERS -> {
                if (missingCovers.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = primary)
                                Text("Todas tus canciones tienen carátula", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(missingCovers, key = { "nocov_${it.trackKey}" }) { track ->
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(
                                    onClick = { onFetchCoverOnline(track) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.ImageSearch, contentDescription = "Buscar portada online", tint = primary)
                                }
                                IconButton(
                                    onClick = { onPickCoverFromGallery(track) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Elegir portada de galería", tint = secondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingTrack?.let { target ->
        MetadataOverrideDialog(
            track = target,
            hasExistingOverride = metadataOverrides.containsKey(target.trackKey),
            onDismiss = { editingTrack = null },
            onConfirmSave = { newTitle, newArtist, newAlbum, newGenre ->
                onSaveMetadataOverride(target.trackKey, newTitle, newArtist, newAlbum, newGenre)
                editingTrack = null
            },
            onRevertOriginal = {
                onClearMetadataOverride(target.trackKey)
                editingTrack = null
            }
        )
    }

    duplicateToHide?.let { dup ->
        AlertDialog(
            onDismissRequest = { duplicateToHide = null },
            title = { Text("¿Ocultar copia de la biblioteca?") },
            text = {
                Text("Se ocultará «${dup.title}» (${dup.album} · ${dup.formatLabel}) de tu vista de Ritmo Local. El archivo de audio físico en tu teléfono NO se borrará.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onHideDuplicateTrack(dup, true)
                        duplicateToHide = null
                    }
                ) {
                    Text("Sí, ocultar de la lista")
                }
            },
            dismissButton = { TextButton(onClick = { duplicateToHide = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MetadataOverrideDialog(
    track: LocalTrack,
    hasExistingOverride: Boolean,
    onDismiss: () -> Unit,
    onConfirmSave: (title: String, artist: String, album: String, genre: String) -> Unit,
    onRevertOriginal: () -> Unit
) {
    var title by remember(track.trackKey) { mutableStateOf(track.title) }
    var artist by remember(track.trackKey) { mutableStateOf(track.artist) }
    var album by remember(track.trackKey) { mutableStateOf(track.album) }
    var genre by remember(track.trackKey) { mutableStateOf(track.genre) }
    var awaitingConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (awaitingConfirm) "Confirmar cambios de información" else "Editar información musical") },
        text = {
            if (awaitingConfirm) {
                Text(
                    "Los cambios se guardarán de forma segura y no destructiva dentro de Ritmo Local sin alterar tu archivo original:\n\n" +
                        "Título: $title\nArtista: $artist\nÁlbum: $album\nGénero: ${genre.ifBlank { "Sin definir" }}"
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artista") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Álbum") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Género") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (hasExistingOverride) {
                        OutlinedButton(onClick = onRevertOriginal, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurar etiquetas originales del archivo")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (awaitingConfirm) {
                Button(
                    onClick = {
                        onConfirmSave(
                            title.ifBlank { track.title },
                            artist.ifBlank { track.artist },
                            album.ifBlank { track.album },
                            genre
                        )
                    }
                ) {
                    Text("Confirmar y aplicar")
                }
            } else {
                Button(onClick = { awaitingConfirm = true }) { Text("Revisar") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (awaitingConfirm) awaitingConfirm = false else onDismiss() }) {
                Text(if (awaitingConfirm) "Volver" else "Cancelar")
            }
        }
    )
}
