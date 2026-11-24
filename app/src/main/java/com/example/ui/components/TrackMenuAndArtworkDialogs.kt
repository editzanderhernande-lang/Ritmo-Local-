package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CustomPlaylist
import com.example.model.LocalTrack
import com.example.ui.i18n.LocalStrings

@Composable
fun TrackOverflowMenuButton(
    track: LocalTrack,
    onPlayNext: (LocalTrack) -> Unit,
    onAddToPlaylist: (LocalTrack) -> Unit,
    onChangeCoverFromGallery: (LocalTrack) -> Unit,
    onFetchCoverOnline: (LocalTrack) -> Unit,
    onShareTrack: (LocalTrack) -> Unit,
    onCastToTv: (LocalTrack) -> Unit,
    onRecommendInChat: (LocalTrack) -> Unit,
    onOpenLyrics: (LocalTrack) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val strings = LocalStrings.current

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("track_more_button_${track.id}")
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Más opciones de ${track.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.playNext) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPlayNext(track)
                }
            )
            DropdownMenuItem(
                text = { Text(strings.addToPlaylist) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToPlaylist(track)
                }
            )
            DropdownMenuItem(
                text = { Text(strings.changeCover) },
                leadingIcon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                onClick = {
                    expanded = false
                    onChangeCoverFromGallery(track)
                }
            )
            DropdownMenuItem(
                text = { Text(strings.downloadCoverOnline) },
                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                onClick = {
                    expanded = false
                    onFetchCoverOnline(track)
                }
            )
            DropdownMenuItem(
                text = { Text(strings.shareTrack) },
                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShareTrack(track)
                }
            )
            DropdownMenuItem(
                text = { Text(strings.castToTv) },
                leadingIcon = { Icon(Icons.Rounded.Cast, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCastToTv(track)
                }
            )
            DropdownMenuItem(
                text = { Text("Ver / editar letra") },
                leadingIcon = { Icon(Icons.Rounded.Lyrics, contentDescription = null) },
                onClick = {
                    expanded = false
                    onOpenLyrics(track)
                }
            )
            DropdownMenuItem(
                text = { Text(strings.sendRecommendation) },
                leadingIcon = { Icon(Icons.Rounded.Forum, contentDescription = null) },
                onClick = {
                    expanded = false
                    onRecommendInChat(track)
                }
            )
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    track: LocalTrack,
    playlists: List<CustomPlaylist>,
    onAddToExisting: (playlistId: String, track: LocalTrack) -> Unit,
    onCreateAndAdd: (playlistName: String, track: LocalTrack) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar a playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = primary
                )
                if (playlists.isNotEmpty()) {
                    playlists.forEach { pl ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddToExisting(pl.id, track)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pl.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${pl.trackKeys.size} canciones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Nueva playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onCreateAndAdd(newName.trim(), track)
                        onDismiss()
                    }
                },
                enabled = newName.isNotBlank()
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Crear y agregar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    titleText: String = "Nueva playlist",
    confirmLabel: String = "Crear",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim())
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
