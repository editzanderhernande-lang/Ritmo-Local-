package com.example.ui.components

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AudioClipTrimmer
import com.example.data.MusicBackupManager
import com.example.model.LocalTrack
import com.example.model.formatDurationMs
import kotlinx.coroutines.delay

data class TrimmerSource(
    val uri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long
)

@Composable
fun AudioTrimmerDialog(
    localTracks: List<LocalTrack>,
    externalSourceUri: Uri?,
    isBusyTrimming: Boolean,
    onPickExternalFile: () -> Unit,
    onConfirmTrim: (uri: Uri, title: String, artist: String, startMs: Long, endMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary

    var selectedSource by remember {
        mutableStateOf<TrimmerSource?>(
            localTracks.firstOrNull()?.let {
                TrimmerSource(Uri.parse(it.contentUri), it.title, it.artist, it.durationMs)
            }
        )
    }

    LaunchedEffect(externalSourceUri) {
        if (externalSourceUri != null) {
            val (name, _) = MusicBackupManager.resolveFileSizeAndName(context, externalSourceUri)
            val duration = resolveAudioDurationMs(context, externalSourceUri)
            selectedSource = TrimmerSource(
                uri = externalSourceUri,
                title = name.substringBeforeLast('.').ifBlank { "Audio" },
                artist = "",
                durationMs = duration.coerceAtLeast(5_000L)
            )
        }
    }

    val totalDurationMs = (selectedSource?.durationMs ?: 60_000L).coerceAtLeast(5_000L)
    var rangeStartMs by remember(selectedSource?.uri) { mutableStateOf(0L) }
    var rangeEndMs by remember(selectedSource?.uri) {
        mutableStateOf(minOf(totalDurationMs, 60_000L))
    }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                previewPlayer?.stop()
                previewPlayer?.release()
            } catch (_: Exception) {
            }
            previewPlayer = null
        }
    }

    LaunchedEffect(isPreviewPlaying, rangeStartMs, rangeEndMs) {
        while (isPreviewPlaying) {
            val mp = previewPlayer
            if (mp == null || !mp.isPlaying) {
                isPreviewPlaying = false
                break
            }
            if (mp.currentPosition >= rangeEndMs) {
                mp.seekTo(rangeStartMs.toInt())
            }
            delay(180L)
        }
    }

    fun togglePreview() {
        val src = selectedSource ?: return
        if (isPreviewPlaying) {
            try { previewPlayer?.pause() } catch (_: Exception) {}
            isPreviewPlaying = false
            return
        }
        try {
            previewPlayer?.release()
            val mp = MediaPlayer().apply {
                setDataSource(context, src.uri)
                prepare()
                seekTo(rangeStartMs.toInt())
                start()
            }
            previewPlayer = mp
            isPreviewPlaying = true
        } catch (_: Exception) {
            isPreviewPlaying = false
        }
    }

    val clipDurationMs = (rangeEndMs - rangeStartMs).coerceAtLeast(1_000L)
    val estKb = AudioClipTrimmer.estimateSizeKb(clipDurationMs)

    AlertDialog(
        onDismissRequest = {
            if (!isBusyTrimming) onDismiss()
        },
        title = { Text("Recortar audio para el Chat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickExternalFile,
                        enabled = !isBusyTrimming,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Otro archivo")
                    }
                }

                if (localTracks.isNotEmpty()) {
                    Text(
                        text = "O elige una canción de tu biblioteca:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 130.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(localTracks.take(40), key = { it.trackKey }) { track ->
                            val isSelected = selectedSource?.uri?.toString() == track.contentUri
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isBusyTrimming) {
                                        try { previewPlayer?.stop() } catch (_: Exception) {}
                                        isPreviewPlaying = false
                                        selectedSource = TrimmerSource(
                                            uri = Uri.parse(track.contentUri),
                                            title = track.title,
                                            artist = track.artist,
                                            durationMs = track.durationMs.coerceAtLeast(5_000L)
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${track.title} · ${track.artist}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatDurationMs(track.durationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                selectedSource?.let { src ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = src.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${formatDurationMs(rangeStartMs)} → ${formatDurationMs(rangeEndMs)} (${formatDurationMs(clipDurationMs)})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "~${AudioClipTrimmer.formatSizeReadable(estKb * 1024L)} / 5.7 MB · Máx 5m",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            RangeSlider(
                                value = rangeStartMs.toFloat()..rangeEndMs.toFloat(),
                                onValueChange = { range ->
                                    var s = range.start.toLong().coerceIn(0L, (totalDurationMs - 2_000L).coerceAtLeast(0L))
                                    var e = range.endInclusive.toLong().coerceIn(s + 2_000L, totalDurationMs)
                                    if (e - s > AudioClipTrimmer.MAX_TRIM_WINDOW_MS) {
                                        e = (s + AudioClipTrimmer.MAX_TRIM_WINDOW_MS).coerceAtMost(totalDurationMs)
                                        if (e - s < 2_000L) s = (e - 2_000L).coerceAtLeast(0L)
                                    }
                                    rangeStartMs = s
                                    rangeEndMs = e
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = primary,
                                    activeTrackColor = primary
                                ),
                                modifier = Modifier.testTag("trimmer_range_slider")
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf(30, 60, 120, 180, 300)) { sec ->
                                    val targetMs = sec * 1000L
                                    if (targetMs <= totalDurationMs) {
                                        val label = if (sec >= 60) "${sec / 60}m" else "${sec}s"
                                        FilterChip(
                                            selected = kotlin.math.abs(clipDurationMs - targetMs) < 1500L,
                                            onClick = {
                                                val maxStart = (totalDurationMs - targetMs).coerceAtLeast(0L)
                                                val newStart = rangeStartMs.coerceAtMost(maxStart)
                                                rangeStartMs = newStart
                                                rangeEndMs = (newStart + targetMs).coerceAtMost(totalDurationMs)
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = ::togglePreview,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPreviewPlaying) "Pausar vista previa" else "Escuchar fragmento")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val src = selectedSource ?: return@Button
                    try { previewPlayer?.stop() } catch (_: Exception) {}
                    isPreviewPlaying = false
                    onConfirmTrim(src.uri, src.title, src.artist, rangeStartMs, rangeEndMs)
                },
                enabled = selectedSource != null && !isBusyTrimming,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_trim_audio_button")
            ) {
                if (isBusyTrimming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recortando…")
                } else {
                    Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Recortar y adjuntar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusyTrimming) {
                Text("Cancelar")
            }
        }
    )
}

private fun resolveAudioDurationMs(context: android.content.Context, uri: Uri): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 60_000L
    } catch (_: Exception) {
        60_000L
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}
