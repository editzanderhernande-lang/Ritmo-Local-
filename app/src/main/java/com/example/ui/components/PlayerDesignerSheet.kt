package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ArtworkPositionMode
import com.example.model.PlayerControlStyle
import com.example.model.PlayerDesignerConfig
import com.example.model.PlayerLayoutMode
import com.example.model.PlayerQuickAction
import com.example.model.PlayerVisualPrefs
import com.example.model.ProgressBarVisualStyle
import com.example.model.VisualizerStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlayerDesignerSheet(
    designerConfig: PlayerDesignerConfig,
    visualPrefs: PlayerVisualPrefs,
    onSaveDesigner: (PlayerDesignerConfig) -> Unit,
    onSelectControlStyle: (PlayerControlStyle) -> Unit,
    onSelectLayoutMode: (PlayerLayoutMode) -> Unit,
    onSelectVisualizerStyle: (VisualizerStyle) -> Unit,
    onRestoreDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    var draft by remember(designerConfig) { mutableStateOf(designerConfig.normalized()) }

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
                .testTag("player_designer_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = primary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Diseñador del Reproductor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Mueve botones, cambia su tamaño, oculta controles y mueve la portada con vista previa real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Real-time Preview Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                    .testTag("player_designer_live_preview")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(primary.copy(alpha = 0.2f), Color.Transparent)))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Vista previa en vivo", style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
                    val artSize = (78 * draft.artworkScale).dp
                    when (draft.artworkPosition) {
                        ArtworkPositionMode.COMPACT_LEFT -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(artSize * 0.85f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(primary.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Album, contentDescription = null, tint = primary, modifier = Modifier.size(32.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tu canción sonando", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text("Artista · Diseño ${visualPrefs.layoutMode.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        ArtworkPositionMode.TOP, ArtworkPositionMode.CENTER -> {
                            if (draft.artworkPosition == ArtworkPositionMode.TOP) {
                                Box(
                                    modifier = Modifier
                                        .size(artSize)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(primary.copy(alpha = 0.24f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Album, contentDescription = null, tint = primary, modifier = Modifier.size(38.dp))
                                }
                                Text("Tu canción sonando", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            } else {
                                Text("Tu canción sonando", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(artSize)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(primary.copy(alpha = 0.24f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Album, contentDescription = null, tint = primary, modifier = Modifier.size(38.dp))
                                }
                            }
                        }
                    }

                    // Preview Progress Bar
                    when (draft.progressBarStyle) {
                        ProgressBarVisualStyle.SLIDER_GLOW -> LinearProgressIndicator(
                            progress = { 0.62f },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(4.dp)),
                            color = primary
                        )
                        ProgressBarVisualStyle.WAVEFORM_BAR -> Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(18) { idx ->
                                val h = if (idx % 3 == 0) 14.dp else if (idx % 2 == 0) 10.dp else 6.dp
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (idx < 11) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                )
                            }
                        }
                        ProgressBarVisualStyle.THIN_LINE -> LinearProgressIndicator(
                            progress = { 0.62f },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = secondary
                        )
                    }

                    // Preview Ordered & Scaled Controls
                    val visiblePrimary = draft.primaryControlsOrder.filterNot { it in draft.hiddenActions }
                    val btnDp = (34 * draft.buttonScale).dp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visiblePrimary.forEach { action ->
                            val isMain = action == PlayerQuickAction.PLAY_PAUSE
                            val icon = when (action) {
                                PlayerQuickAction.SHUFFLE -> Icons.Rounded.Shuffle
                                PlayerQuickAction.PREVIOUS -> Icons.Rounded.SkipPrevious
                                PlayerQuickAction.PLAY_PAUSE -> Icons.Rounded.Pause
                                PlayerQuickAction.NEXT -> Icons.Rounded.SkipNext
                                PlayerQuickAction.REPEAT -> Icons.Rounded.Repeat
                                else -> Icons.Rounded.Pause
                            }
                            Box(
                                modifier = Modifier
                                    .size(if (isMain) btnDp * 1.25f else btnDp)
                                    .clip(if (visualPrefs.controlStyle == PlayerControlStyle.CLASSIC_STUDIO) RoundedCornerShape(8.dp) else CircleShape)
                                    .background(if (isMain) primary else primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = if (isMain) Color.White else primary, modifier = Modifier.size(if (isMain) 22.dp else 18.dp))
                            }
                        }
                    }
                }
            }

            Text("Posición y tamaño de portada", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtworkPositionMode.entries.forEach { pos ->
                    FilterChip(
                        selected = draft.artworkPosition == pos,
                        onClick = { draft = draft.copy(artworkPosition = pos) },
                        label = { Text(pos.displayName) },
                        modifier = Modifier.testTag("designer_artwork_pos_${pos.id}")
                    )
                }
            }
            Text("Escala de portada (${(draft.artworkScale * 100).roundToInt()}%)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = draft.artworkScale,
                onValueChange = { draft = draft.copy(artworkScale = it) },
                valueRange = 0.75f..1.18f
            )
            Text("Tamaño de botones (${(draft.buttonScale * 100).roundToInt()}%)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = draft.buttonScale,
                onValueChange = { draft = draft.copy(buttonScale = it) },
                valueRange = 0.80f..1.25f
            )

            Text("Barra de progreso y diseño", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressBarVisualStyle.entries.forEach { pb ->
                    FilterChip(
                        selected = draft.progressBarStyle == pb,
                        onClick = { draft = draft.copy(progressBarStyle = pb) },
                        label = { Text(pb.displayName) }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerLayoutMode.entries.forEach { lm ->
                    FilterChip(
                        selected = visualPrefs.layoutMode == lm,
                        onClick = { onSelectLayoutMode(lm) },
                        label = { Text(lm.displayName) }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerControlStyle.entries.forEach { cs ->
                    FilterChip(
                        selected = visualPrefs.controlStyle == cs,
                        onClick = { onSelectControlStyle(cs) },
                        label = { Text(cs.displayName) }
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VisualizerStyle.entries.forEach { vs ->
                    FilterChip(
                        selected = visualPrefs.visualizerStyle == vs,
                        onClick = { onSelectVisualizerStyle(vs) },
                        label = { Text("Viz: ${vs.displayName}") }
                    )
                }
            }

            Text("Orden y visibilidad de botones principales", style = MaterialTheme.typography.titleSmall)
            draft.primaryControlsOrder.forEachIndexed { idx, action ->
                val isHidden = action in draft.hiddenActions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(action.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { draft = draft.movePrimaryControl(action, -1) },
                            enabled = idx > 0,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("designer_move_up_${action.id}")
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Mover ${action.label} a la izquierda")
                        }
                        IconButton(
                            onClick = { draft = draft.movePrimaryControl(action, 1) },
                            enabled = idx < draft.primaryControlsOrder.lastIndex,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("designer_move_down_${action.id}")
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Mover ${action.label} a la derecha")
                        }
                        if (action != PlayerQuickAction.PLAY_PAUSE) {
                            IconButton(
                                onClick = { draft = draft.toggleActionVisibility(action) },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("designer_toggle_vis_${action.id}")
                            ) {
                                Icon(
                                    imageVector = if (isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (isHidden) "Mostrar ${action.label}" else "Ocultar ${action.label}",
                                    tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else primary
                                )
                            }
                        }
                    }
                }
            }

            Text("Controles secundarios visibles", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerQuickAction.defaultToolsOrder.forEach { tool ->
                    val visible = tool !in draft.hiddenActions
                    FilterChip(
                        selected = visible,
                        onClick = { draft = draft.toggleActionVisibility(tool) },
                        label = { Text(tool.label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Efectos del Modo Visual Inmersivo", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(
                    checked = draft.immersiveEffectsEnabled,
                    onCheckedChange = { draft = draft.copy(immersiveEffectsEnabled = it) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        draft = PlayerDesignerConfig.defaultConfig()
                        onRestoreDefaults()
                    },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("designer_restore_btn")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar original", maxLines = 1)
                }
                Button(
                    onClick = {
                        onSaveDesigner(draft)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("designer_save_btn")
                ) {
                    Text("Guardar diseño", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
