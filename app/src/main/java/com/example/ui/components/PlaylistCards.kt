package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AlbumPlaylist
import com.example.model.ArtistPlaylist
import com.example.model.CustomPlaylist
import com.example.model.LocalTrack
import com.example.model.UiStyleMode
import com.example.model.formatDurationMs
import com.example.ui.screens.TrackActionsCallbacks
import com.example.ui.screens.TrackRowItem
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.LocalUiStyleMode

@Composable
fun PlaylistSummaryBanner(
    title: String,
    subtitle: String,
    accent: Color,
    hasTracks: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent)))
                .padding(18.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onPlayAll,
                    enabled = hasTracks,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reproducir todo", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onShufflePlay, enabled = hasTracks) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aleatorio")
                }
            }
        }
    }
}

@Composable
fun EmptyPlaylistHint(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AlbumPlaylistCard(
    album: AlbumPlaylist,
    isExpanded: Boolean,
    animationsEnabled: Boolean,
    currentPlayingTrackKey: String?,
    favoriteTrackKeys: Set<String>,
    trackActions: TrackActionsCallbacks,
    onToggleExpand: () -> Unit,
    onPlayAlbum: () -> Unit,
    onChangeAlbumCover: () -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
            ) {
                AlbumArtwork(
                    albumArtUri = album.albumArtUri,
                    contentDescription = "Portada del álbum ${album.albumName}",
                    modifier = Modifier.size(56.dp),
                    cornerRadius = 12.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.albumName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${album.artistName} · ${album.tracks.size} canciones · ${formatDurationMs(album.totalDurationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onChangeAlbumCover) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = "Cambiar carátula del álbum ${album.albumName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledIconButton(
                    onClick = onPlayAlbum,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir álbum ${album.albumName}")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Expandir álbum",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val showBody = isExpanded
            if (animationsEnabled) {
                AnimatedVisibility(visible = showBody) {
                    AlbumTrackListContent(album, currentPlayingTrackKey, favoriteTrackKeys, trackActions, onPlayTrackAt, onToggleFavorite)
                }
            } else if (showBody) {
                AlbumTrackListContent(album, currentPlayingTrackKey, favoriteTrackKeys, trackActions, onPlayTrackAt, onToggleFavorite)
            }
        }
    }
}

@Composable
private fun AlbumTrackListContent(
    album: AlbumPlaylist,
    currentPlayingTrackKey: String?,
    favoriteTrackKeys: Set<String>,
    trackActions: TrackActionsCallbacks,
    onPlayTrackAt: (Int) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        album.tracks.forEachIndexed { idx, track ->
            TrackRowItem(
                track = track,
                isCurrent = track.trackKey == currentPlayingTrackKey,
                isFavorite = favoriteTrackKeys.contains(track.trackKey),
                onClick = { onPlayTrackAt(idx) },
                onToggleFavorite = { onToggleFavorite(track) },
                actions = trackActions
            )
        }
    }
}

@Composable
fun ArtistPlaylistCard(
    artist: ArtistPlaylist,
    isExpanded: Boolean,
    animationsEnabled: Boolean,
    currentPlayingTrackKey: String?,
    favoriteTrackKeys: Set<String>,
    trackActions: TrackActionsCallbacks,
    onToggleExpand: () -> Unit,
    onPlayArtistAll: () -> Unit,
    onPlayAlbum: (AlbumPlaylist) -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val isNeon = LocalUiStyleMode.current == UiStyleMode.NEON_MODERN
    val artistArtUri = artist.albums.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
        ?: artist.tracks.firstOrNull { !it.albumArtUri.isNullOrBlank() }?.albumArtUri
    Surface(
        shape = RoundedCornerShape(if (isNeon) 16.dp else 18.dp),
        color = if (isNeon) MaterialTheme.colorScheme.surface.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = if (isNeon) 10.dp else 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
            ) {
                if (isNeon) {
                    AlbumArtwork(
                        albumArtUri = artistArtUri,
                        contentDescription = "Imagen de ${artist.artistName}",
                        modifier = Modifier.size(54.dp),
                        cornerRadius = 12.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artist.artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNeon) FontWeight.SemiBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isNeon) {
                            "${artist.tracks.size} canciones · ${artist.albumCount} álbumes"
                        } else {
                            "${artist.tracks.size} canciones · ${artist.albumCount} álbumes · ${formatDurationMs(artist.totalDurationMs)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isNeon) {
                    FilledIconButton(
                        onClick = onPlayArtistAll,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir artista ${artist.artistName}")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    IconButton(onClick = onPlayArtistAll, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir artista ${artist.artistName}", tint = primary)
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else if (isNeon) Icons.Rounded.ChevronRight else Icons.Rounded.ExpandMore,
                    contentDescription = "Expandir artista",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (animationsEnabled) {
                AnimatedVisibility(visible = isExpanded) {
                    ArtistExpandedContent(
                        artist = artist,
                        secondary = secondary,
                        currentPlayingTrackKey = currentPlayingTrackKey,
                        favoriteTrackKeys = favoriteTrackKeys,
                        trackActions = trackActions,
                        onPlayAlbum = onPlayAlbum,
                        onPlayTrackAt = onPlayTrackAt,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            } else if (isExpanded) {
                ArtistExpandedContent(
                    artist = artist,
                    secondary = secondary,
                    currentPlayingTrackKey = currentPlayingTrackKey,
                    favoriteTrackKeys = favoriteTrackKeys,
                    trackActions = trackActions,
                    onPlayAlbum = onPlayAlbum,
                    onPlayTrackAt = onPlayTrackAt,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }
    }
}

@Composable
private fun ArtistExpandedContent(
    artist: ArtistPlaylist,
    secondary: Color,
    currentPlayingTrackKey: String?,
    favoriteTrackKeys: Set<String>,
    trackActions: TrackActionsCallbacks,
    onPlayAlbum: (AlbumPlaylist) -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (artist.albums.isNotEmpty()) {
            Text(
                text = "Álbumes de ${artist.artistName} (${artist.albums.size})",
                style = MaterialTheme.typography.labelLarge,
                color = secondary
            )
            artist.albums.forEach { album ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayAlbum(album) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtwork(
                            albumArtUri = album.albumArtUri,
                            contentDescription = "Álbum ${album.albumName}",
                            modifier = Modifier.size(42.dp),
                            cornerRadius = 8.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(album.albumName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${album.tracks.size} canciones · ${formatDurationMs(album.totalDurationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Rounded.Album, contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Text(
            text = "Todas las canciones (${artist.tracks.size})",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        artist.tracks.forEachIndexed { idx, track ->
            TrackRowItem(
                track = track,
                isCurrent = track.trackKey == currentPlayingTrackKey,
                isFavorite = favoriteTrackKeys.contains(track.trackKey),
                onClick = { onPlayTrackAt(idx) },
                onToggleFavorite = { onToggleFavorite(track) },
                actions = trackActions
            )
        }
    }
}

@Composable
fun CustomPlaylistCard(
    playlist: CustomPlaylist,
    resolvedTracks: List<LocalTrack>,
    isExpanded: Boolean,
    currentPlayingTrackKey: String?,
    favoriteTrackKeys: Set<String>,
    trackActions: TrackActionsCallbacks,
    onToggleExpand: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onRenamePlaylist: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onRemoveTrack: (LocalTrack) -> Unit,
    onPlayTrackAt: (Int) -> Unit,
    onToggleFavorite: (LocalTrack) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${resolvedTracks.size} canciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRenamePlaylist) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Renombrar playlist", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDeletePlaylist) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar playlist", tint = CoralPulse)
                }
                FilledIconButton(
                    onClick = onPlayPlaylist,
                    enabled = resolvedTracks.isNotEmpty(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir playlist")
                }
            }
            if (isExpanded && resolvedTracks.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    resolvedTracks.forEachIndexed { idx, track ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                TrackRowItem(
                                    track = track,
                                    isCurrent = track.trackKey == currentPlayingTrackKey,
                                    isFavorite = favoriteTrackKeys.contains(track.trackKey),
                                    onClick = { onPlayTrackAt(idx) },
                                    onToggleFavorite = { onToggleFavorite(track) },
                                    actions = trackActions
                                )
                            }
                            IconButton(onClick = { onRemoveTrack(track) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Quitar de playlist", tint = CoralPulse, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
