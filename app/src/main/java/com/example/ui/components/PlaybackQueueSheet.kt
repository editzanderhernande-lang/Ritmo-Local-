package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.model.LocalTrack
import com.example.model.formatDurationMs

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import com.example.model.SavedQueuePreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueueSheet(
    queue: List<LocalTrack>,
    currentQueueIndex: Int,
    isPlaying: Boolean,
    libraryTracks: List<LocalTrack>,
    savedQueuePresets: List<SavedQueuePreset> = emptyList(),
    onJumpToIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onPlayNext: (LocalTrack) -> Unit,
    onAppendToQueue: (LocalTrack) -> Unit,
    onSmartShuffleQueue: () -> Unit = {},
    onDeduplicateQueue: () -> Unit = {},
    onSaveQueuePreset: (String) -> Unit = {},
    onLoadQueuePreset: (SavedQueuePreset) -> Unit = {},
    onDeleteQueuePreset: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    var showAddPicker by remember { mutableStateOf(false) }
    var showSavedQueues by remember { mutableStateOf(false) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }
    var queueNameInput by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }

    val safeCurrentIdx = currentQueueIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
    val activeTrack = queue.getOrNull(safeCurrentIdx)

    val filteredLibrary = remember(libraryTracks, searchText) {
        val q = searchText.trim().lowercase()
        val list = if (q.isEmpty()) libraryTracks else libraryTracks.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
        list.take(30)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
                .testTag("playback_queue_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cola de reproducción",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (queue.isEmpty()) "Vacía" else "${queue.size} canciones en lista",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showAddPicker = !showAddPicker },
                    modifier = Modifier.testTag("queue_add_songs_button")
                ) {
                    Icon(
                        imageVector = if (showAddPicker) Icons.Rounded.Close else Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showAddPicker) "Ver cola" else "Agregar")
                }
            }

            if (activeTrack != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = primary.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtwork(
                            albumArtUri = activeTrack.albumArtUri,
                            contentDescription = "Portada de ${activeTrack.title}",
                            modifier = Modifier.size(48.dp),
                            cornerRadius = 10.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "SONANDO AHORA" else "EN PAUSA",
                                style = MaterialTheme.typography.labelSmall,
                                color = primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = activeTrack.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeTrack.artist} · ${activeTrack.album}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = primary
                        )
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    AssistChip(
                        onClick = onSmartShuffleQueue,
                        label = { Text("Mezcla inteligente") },
                        leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("queue_smart_shuffle_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = onDeduplicateQueue,
                        label = { Text("Evitar repetidas") },
                        leadingIcon = { Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("queue_dedupe_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            queueNameInput = ""
                            showSaveQueueDialog = true
                        },
                        label = { Text("Guardar cola") },
                        leadingIcon = { Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("queue_save_preset_chip")
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            showSavedQueues = !showSavedQueues
                            if (showSavedQueues) showAddPicker = false
                        },
                        label = { Text(if (showSavedQueues) "Ver cola actual" else "Colas (${savedQueuePresets.size})") },
                        leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("queue_saved_list_chip")
                    )
                }
            }

            if (showSavedQueues) {
                if (savedQueuePresets.isEmpty()) {
                    Text(
                        text = "Aún no has guardado ninguna cola. Toca 'Guardar cola' para conservar este orden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp, max = 280.dp)
                            .testTag("queue_saved_presets_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedQueuePresets, key = { it.id }) { preset ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(preset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${preset.trackKeys.size} canciones guardadas",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onLoadQueuePreset(preset)
                                            showSavedQueues = false
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize().testTag("queue_load_preset_${preset.id}")
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Cargar cola ${preset.name}", tint = primary)
                                    }
                                    IconButton(
                                        onClick = { onDeleteQueuePreset(preset.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize().testTag("queue_delete_preset_${preset.id}")
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar cola ${preset.name}", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (showAddPicker) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Buscar en biblioteca para añadir...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("queue_search_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 320.dp)
                        .testTag("queue_library_picker_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLibrary, key = { "lib_${it.trackKey}" }) { libTrack ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtwork(
                                    albumArtUri = libTrack.albumArtUri,
                                    contentDescription = libTrack.title,
                                    modifier = Modifier.size(42.dp),
                                    cornerRadius = 8.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = libTrack.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${libTrack.artist} · ${formatDurationMs(libTrack.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = { onPlayNext(libTrack) },
                                    modifier = Modifier.testTag("queue_add_next_${libTrack.id}")
                                ) {
                                    Text("Siguiente")
                                }
                                IconButton(
                                    onClick = { onAppendToQueue(libTrack) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("queue_append_${libTrack.id}")
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "Agregar ${libTrack.title} al final de la cola",
                                        tint = primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text(
                    text = "Lista de reproducción (${queue.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 320.dp)
                        .testTag("playback_queue_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queue, key = { idx, t -> "${t.trackKey}_$idx" }) { index, item ->
                        val isCurrent = index == safeCurrentIdx
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJumpToIndex(index) }
                                .testTag("queue_item_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtwork(
                                    albumArtUri = item.albumArtUri,
                                    contentDescription = "Portada de ${item.title}",
                                    modifier = Modifier.size(40.dp),
                                    cornerRadius = 9.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${index + 1}. ${item.title}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCurrent) primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.artist} · ${formatDurationMs(item.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { if (index > 0) onMoveItem(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("queue_move_up_$index")
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowUpward,
                                        contentDescription = "Subir en cola",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { if (index < queue.lastIndex) onMoveItem(index, index + 1) },
                                    enabled = index < queue.lastIndex,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("queue_move_down_$index")
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowDownward,
                                        contentDescription = "Bajar en cola",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onRemoveItem(index) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("queue_remove_$index")
                                ) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "Eliminar de cola",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveQueueDialog) {
        AlertDialog(
            onDismissRequest = { showSaveQueueDialog = false },
            title = { Text("Guardar cola actual") },
            text = {
                OutlinedTextField(
                    value = queueNameInput,
                    onValueChange = { queueNameInput = it },
                    placeholder = { Text("Ej: Viaje nocturno, Cardio…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("queue_save_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (queueNameInput.isNotBlank()) {
                            onSaveQueuePreset(queueNameInput.trim())
                            showSaveQueueDialog = false
                        }
                    },
                    modifier = Modifier.testTag("queue_save_confirm_btn")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveQueueDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
