package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CrossfadeCurve
import com.example.model.DjTransitionStyle
import com.example.model.PlaybackEnginePrefs
import com.example.model.VisualizerStyle
import com.example.player.SmartDjRuntimeState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackProControlsSheet(
    enginePrefs: PlaybackEnginePrefs,
    djRuntime: SmartDjRuntimeState,
    visualizerStyle: VisualizerStyle,
    visualizerIntensity: Float,
    canMixNow: Boolean,
    onUpdateEnginePrefs: (PlaybackEnginePrefs) -> Unit,
    onTriggerManualDjMix: () -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onUpdateVisualizerIntensity: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

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
                .testTag("playback_pro_controls_sheet"),
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
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Motor Pro · DJ y Transiciones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Crossfade continuo, Modo DJ inteligente y visualizador en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Smart DJ Section
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (enginePrefs.smartDjEnabled) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (enginePrefs.smartDjEnabled) primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Modo DJ Inteligente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Analiza BPM, energía y silencios en solo lectura sin alterar tus archivos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = enginePrefs.smartDjEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateEnginePrefs(
                                    enginePrefs.copy(
                                        smartDjEnabled = enabled,
                                        crossfadeEnabled = if (enabled) true else enginePrefs.crossfadeEnabled
                                    )
                                )
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = primary),
                            modifier = Modifier.testTag("pro_smart_dj_switch")
                        )
                    }

                    val analysis = djRuntime.currentAnalysis
                    if (analysis != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Tempo ~${analysis.estimatedBpm} BPM",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Energía ${(analysis.energyScore * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    if (djRuntime.isCrossfading) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = djRuntime.lastTransitionNote ?: "Mezclando pistas…",
                                style = MaterialTheme.typography.labelSmall,
                                color = primary,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = { djRuntime.crossfadeProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = primary
                            )
                        }
                    }

                    Text("Estilo de mezcla DJ", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                        items(DjTransitionStyle.entries, key = { it.id }) { style ->
                            FilterChip(
                                selected = enginePrefs.djTransitionStyle == style,
                                onClick = { onUpdateEnginePrefs(enginePrefs.copy(djTransitionStyle = style)) },
                                label = { Text(style.displayName) },
                                modifier = Modifier.testTag("dj_style_chip_${style.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Saltar silencios al inicio/final", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Evita pausas muertas entre canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = enginePrefs.djSkipSilenceEnabled,
                            onCheckedChange = { onUpdateEnginePrefs(enginePrefs.copy(djSkipSilenceEnabled = it)) },
                            modifier = Modifier.testTag("dj_skip_silence_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nivelar volumen en mezcla", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Evita saltos bruscos de sonoridad", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = enginePrefs.djVolumeNormalization,
                            onCheckedChange = { onUpdateEnginePrefs(enginePrefs.copy(djVolumeNormalization = it)) },
                            modifier = Modifier.testTag("dj_normalize_switch")
                        )
                    }

                    Button(
                        onClick = onTriggerManualDjMix,
                        enabled = canMixNow && !djRuntime.isCrossfading,
                        colors = ButtonDefaults.buttonColors(containerColor = secondary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dj_mix_now_button")
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (djRuntime.isCrossfading) "Mezclando transición…" else "Mezcla DJ ahora → Siguiente",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Crossfade & Fade In/Out
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Crossfade entre canciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Superpone suavemente fin e inicio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = enginePrefs.crossfadeEnabled,
                            onCheckedChange = { onUpdateEnginePrefs(enginePrefs.copy(crossfadeEnabled = it)) },
                            modifier = Modifier.testTag("pro_crossfade_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duración del Crossfade", style = MaterialTheme.typography.bodySmall)
                        Text("${enginePrefs.crossfadeDurationSec} s", style = MaterialTheme.typography.labelLarge, color = primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = enginePrefs.crossfadeDurationSec.toFloat(),
                        onValueChange = {
                            onUpdateEnginePrefs(enginePrefs.copy(crossfadeDurationSec = it.roundToInt().coerceIn(1, 12)))
                        },
                        valueRange = 1f..12f,
                        colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                        modifier = Modifier.testTag("pro_crossfade_slider")
                    )

                    Text("Curva de mezcla", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CrossfadeCurve.entries, key = { it.id }) { curve ->
                            FilterChip(
                                selected = enginePrefs.crossfadeCurve == curve,
                                onClick = { onUpdateEnginePrefs(enginePrefs.copy(crossfadeCurve = curve)) },
                                label = { Text(curve.displayName) },
                                modifier = Modifier.testTag("crossfade_curve_${curve.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fundido de entrada (Fade-In)", style = MaterialTheme.typography.bodyMedium)
                            Text("Arranque suave al reanudar música", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = enginePrefs.fadeInEnabled,
                            onCheckedChange = { onUpdateEnginePrefs(enginePrefs.copy(fadeInEnabled = it)) },
                            modifier = Modifier.testTag("pro_fade_in_switch")
                        )
                    }
                }
            }

            // 3. Visualizer Style & Intensity
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visualizador Dinámico en Tiempo Real", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        VisualizerStyle.entries.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { vStyle ->
                                    val selected = visualizerStyle == vStyle
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (selected) 1.5.dp else 1.dp,
                                                color = if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { onSelectVisualizerStyle(vStyle) }
                                            .testTag("visualizer_style_${vStyle.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = vStyle.displayName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = vStyle.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Intensidad reactiva", style = MaterialTheme.typography.bodySmall)
                        Text("${(visualizerIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = visualizerIntensity,
                        onValueChange = onUpdateVisualizerIntensity,
                        valueRange = 0.35f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                        modifier = Modifier.testTag("visualizer_intensity_slider")
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sincronizado con graves y agudos · Optimizado para bajo consumo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
