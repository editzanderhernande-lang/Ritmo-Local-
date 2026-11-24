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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.player.SleepTimerUiState
import com.example.player.formatSleepTimerCountdown
import kotlin.math.roundToInt

import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.VolumeDown

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.BookmarkAdd

private val PRESET_SONGS = listOf(1, 2, 3, 5)
private val FADE_OUT_DURATION_OPTIONS = listOf(2, 3, 6, 10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    sleepTimerState: SleepTimerUiState,
    customPresetMinutes: List<Int> = listOf(5, 10, 15, 30, 45, 60),
    fadeOutDurationSec: Int = sleepTimerState.fadeOutDurationSec,
    onStartPreset: (Int) -> Unit,
    onStartSongCount: (Int) -> Unit = {},
    onToggleFadeOut: (Boolean) -> Unit = {},
    onChangeFadeOutDuration: (Int) -> Unit = {},
    onSaveCustomPreset: (Int) -> Unit = {},
    onExtendFiveMinutes: () -> Unit,
    onToggleStopAfterCurrentTrack: () -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    var customMinutes by remember { mutableFloatStateOf(20f) }
    val selectedCustomMin = customMinutes.roundToInt().coerceIn(1, 180)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag("sleep_timer_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Temporizador de apagado",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "La reproducción se detendrá automáticamente al terminar la cuenta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(visible = sleepTimerState.isActive) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(primary.copy(alpha = 0.7f), secondary.copy(alpha = 0.5f))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(primary.copy(alpha = 0.14f), secondary.copy(alpha = 0.08f))
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when {
                                        sleepTimerState.remainingSongsToStop > 1 ->
                                            "Detener tras ${sleepTimerState.remainingSongsToStop} canciones"
                                        sleepTimerState.stopAfterCurrentTrack || sleepTimerState.remainingSongsToStop == 1 ->
                                            "Fin de la canción actual"
                                        else -> "Cuenta regresiva activa"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = primary
                                )
                            }
                            when {
                                sleepTimerState.remainingSongsToStop > 0 -> {
                                    Text(
                                        text = "${sleepTimerState.remainingSongsToStop} ${if (sleepTimerState.remainingSongsToStop == 1) "pista" else "pistas"}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("sleep_timer_song_countdown_text")
                                    )
                                }
                                !sleepTimerState.stopAfterCurrentTrack -> {
                                    Text(
                                        text = formatSleepTimerCountdown(sleepTimerState.remainingSeconds),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("sleep_timer_countdown_text")
                                    )
                                }
                            }
                        }

                        if (sleepTimerState.progressFraction > 0f) {
                            LinearProgressIndicator(
                                progress = { sleepTimerState.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!sleepTimerState.stopAfterCurrentTrack && sleepTimerState.remainingSongsToStop <= 0) {
                                FilledTonalButton(
                                    onClick = onExtendFiveMinutes,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("sleep_timer_add_5_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("5 min", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            OutlinedButton(
                                onClick = onCancelTimer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("sleep_timer_cancel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Cancelar",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = "Duración rápida",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                customPresetMinutes.chunked(3).forEach { rowPresets ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowPresets.forEach { minutes ->
                            val isSelected = sleepTimerState.isActive &&
                                !sleepTimerState.stopAfterCurrentTrack &&
                                sleepTimerState.totalSeconds == minutes * 60L
                            val labelText = if (minutes >= 60) {
                                val h = minutes / 60
                                val m = minutes % 60
                                if (m == 0) "${h} h" else "${h}h ${m}m"
                            } else "$minutes min"
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) {
                                    primary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) {
                                            primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onStartPreset(minutes) }
                                    .testTag("sleep_timer_preset_$minutes")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val customFormatted = if (selectedCustomMin >= 60) {
                        val h = selectedCustomMin / 60
                        val m = selectedCustomMin % 60
                        if (m == 0) "${h} h" else "${h}h ${m}m"
                    } else "$selectedCustomMin min"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tiempo personalizado",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { onSaveCustomPreset(selectedCustomMin) },
                                modifier = Modifier.testTag("sleep_timer_save_preset_btn")
                            ) {
                                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar")
                            }
                            Text(
                                text = customFormatted,
                                style = MaterialTheme.typography.labelLarge,
                                color = primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Slider(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        valueRange = 1f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = primary,
                            activeTrackColor = primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_timer_custom_slider")
                    )
                    Button(
                        onClick = { onStartPreset(selectedCustomMin) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sleep_timer_start_custom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Iniciar ($customFormatted)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Al terminar la canción actual",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Detiene la música justo al concluir esta pista",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = sleepTimerState.isActive && (sleepTimerState.stopAfterCurrentTrack || sleepTimerState.remainingSongsToStop == 1),
                        onCheckedChange = { onToggleStopAfterCurrentTrack() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primary
                        ),
                        modifier = Modifier.testTag("sleep_timer_end_of_track_switch")
                    )
                }
            }

            // Detener tras cierta cantidad de canciones
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Detener tras cantidad de canciones",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_SONGS.forEach { count ->
                            val isSelected = sleepTimerState.isActive && sleepTimerState.remainingSongsToStop == count
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primary.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onStartSongCount(count) }
                                    .testTag("sleep_timer_songs_$count")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (count == 1) "1 canción" else "$count pistas",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    if (sleepTimerState.isActive && sleepTimerState.remainingSongsToStop > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ajustar canciones: ${sleepTimerState.remainingSongsToStop}",
                                style = MaterialTheme.typography.labelMedium,
                                color = primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onStartSongCount((sleepTimerState.remainingSongsToStop - 1).coerceAtLeast(1)) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = "Menos canciones", modifier = Modifier.size(16.dp))
                                }
                                OutlinedButton(
                                    onClick = { onStartSongCount((sleepTimerState.remainingSongsToStop + 1).coerceAtMost(30)) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Más canciones", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Fundido gradual de volumen
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeDown,
                                contentDescription = null,
                                tint = primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Fundido gradual de volumen",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Disminuye suavemente el sonido antes de pausar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = sleepTimerState.fadeOutEnabled,
                            onCheckedChange = onToggleFadeOut,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = primary
                            ),
                            modifier = Modifier.testTag("sleep_timer_fade_out_switch")
                        )
                    }
                    if (sleepTimerState.fadeOutEnabled) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("sleep_fade_sec_row")
                        ) {
                            items(FADE_OUT_DURATION_OPTIONS, key = { it }) { sec ->
                                FilterChip(
                                    selected = fadeOutDurationSec == sec,
                                    onClick = { onChangeFadeOutDuration(sec) },
                                    label = { Text("${sec}s fundido") },
                                    modifier = Modifier.testTag("sleep_fade_sec_$sec")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
