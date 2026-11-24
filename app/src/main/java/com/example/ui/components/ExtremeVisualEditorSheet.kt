package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.example.model.CUSTOM_THEME_PRIMARY_SWATCHES
import com.example.model.ExtremeVisualStyleConfig
import com.example.model.IconVisualMode
import com.example.model.MiniPlayerBarStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtremeVisualEditorSheet(
    currentStyle: ExtremeVisualStyleConfig,
    onSaveStyle: (ExtremeVisualStyleConfig) -> Unit,
    onOpenCustomThemeBuilder: () -> Unit,
    onOpenPlayerDesigner: () -> Unit,
    onRestoreOriginalDesign: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    var draft by remember(currentStyle) { mutableStateOf(currentStyle.normalized()) }

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
                .testTag("extreme_visual_editor_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = primary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Editor Visual Extremo 2.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Bordes, transparencias, sombras, degradados, botones, barra de reproducción y velocidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live interactive preview
            val previewShape = RoundedCornerShape(draft.cardCornerDp.dp)
            val previewBrush = if (draft.customGradientEnabled) {
                Brush.linearGradient(listOf(Color(draft.gradientStartArgb).copy(alpha = 0.45f), Color(draft.gradientEndArgb).copy(alpha = 0.55f)))
            } else {
                Brush.linearGradient(listOf(primary.copy(alpha = 0.16f), MaterialTheme.colorScheme.surfaceContainer))
            }
            Surface(
                shape = previewShape,
                shadowElevation = draft.shadowElevationDp.dp,
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = draft.surfaceTransparencyAlpha),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(draft.cardBorderWidthDp.dp, primary.copy(alpha = 0.55f), previewShape)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(previewBrush)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Vista previa en tiempo real", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Radio ${draft.cardCornerDp.roundToInt()}dp · Borde ${String.format("%.1f", draft.cardBorderWidthDp)}dp · Vel ${(draft.animationSpeedScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(percent = draft.buttonCornerPercent),
                        color = primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Botón", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onOpenCustomThemeBuilder,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("extreme_open_themes_btn")
                ) {
                    Icon(Icons.Rounded.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Temas JSON", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onOpenPlayerDesigner,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("extreme_open_player_designer_btn")
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Diseñador Repr.", maxLines = 1)
                }
            }

            // Custom Gradient
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Degradado de fondo personalizado", style = MaterialTheme.typography.titleSmall)
                            Text("Aplica colores y ángulo propios al lienzo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = draft.customGradientEnabled,
                            onCheckedChange = { draft = draft.copy(customGradientEnabled = it) },
                            modifier = Modifier.testTag("extreme_gradient_switch")
                        )
                    }
                    if (draft.customGradientEnabled) {
                        Text("Color inicial", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CUSTOM_THEME_PRIMARY_SWATCHES.take(6).forEach { argb ->
                                val selected = draft.gradientStartArgb == argb
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                                        .clickable { draft = draft.copy(gradientStartArgb = argb) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Text("Color final", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0xFF0A0814L, 0xFF1E1B4BL, 0xFF0F172AL, 0xFF064E3BL, 0xFF4C1D95L, 0xFF3F1D2BL).forEach { argb ->
                                val selected = draft.gradientEndArgb == argb
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb))
                                        .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                                        .clickable { draft = draft.copy(gradientEndArgb = argb) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        LabeledSlider(
                            label = "Ángulo del degradado (${draft.gradientAngleDeg.roundToInt()}°)",
                            value = draft.gradientAngleDeg,
                            range = 0f..360f,
                            onValueChange = { draft = draft.copy(gradientAngleDeg = it) }
                        )
                    }
                }
            }

            // Sliders for Corners, Borders, Transparency, Shadow, Button Shape, Animation Speed
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LabeledSlider(
                        label = "Redondeo de tarjetas (${draft.cardCornerDp.roundToInt()} dp)",
                        value = draft.cardCornerDp,
                        range = 6f..30f,
                        onValueChange = { draft = draft.copy(cardCornerDp = it) }
                    )
                    LabeledSlider(
                        label = "Grosor de bordes (${String.format("%.1f", draft.cardBorderWidthDp)} dp)",
                        value = draft.cardBorderWidthDp,
                        range = 0f..3f,
                        onValueChange = { draft = draft.copy(cardBorderWidthDp = it) }
                    )
                    LabeledSlider(
                        label = "Opacidad de superficies (${(draft.surfaceTransparencyAlpha * 100).roundToInt()}%)",
                        value = draft.surfaceTransparencyAlpha,
                        range = 0.60f..1.0f,
                        onValueChange = { draft = draft.copy(surfaceTransparencyAlpha = it) }
                    )
                    LabeledSlider(
                        label = "Profundidad de sombras (${draft.shadowElevationDp.roundToInt()} dp)",
                        value = draft.shadowElevationDp,
                        range = 0f..14f,
                        onValueChange = { draft = draft.copy(shadowElevationDp = it) }
                    )
                    LabeledSlider(
                        label = "Forma de botones (${draft.buttonCornerPercent}%)",
                        value = draft.buttonCornerPercent.toFloat(),
                        range = 8f..50f,
                        onValueChange = { draft = draft.copy(buttonCornerPercent = it.roundToInt()) }
                    )
                    LabeledSlider(
                        label = "Velocidad de animaciones (${(draft.animationSpeedScale * 100).roundToInt()}%)",
                        value = draft.animationSpeedScale,
                        range = 0.5f..1.8f,
                        onValueChange = { draft = draft.copy(animationSpeedScale = it) }
                    )
                }
            }

            Text("Estilo de barra de reproducción", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniPlayerBarStyle.entries.forEach { barStyle ->
                    FilterChip(
                        selected = draft.miniPlayerBarStyle == barStyle,
                        onClick = { draft = draft.copy(miniPlayerBarStyle = barStyle) },
                        label = { Text(barStyle.displayName) },
                        modifier = Modifier.testTag("extreme_minibar_${barStyle.id}")
                    )
                }
            }

            Text("Estilo de iconos", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconVisualMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.iconStyleMode == mode,
                        onClick = { draft = draft.copy(iconStyleMode = mode) },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        draft = ExtremeVisualStyleConfig.defaultConfig()
                        onRestoreOriginalDesign()
                    },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("extreme_restore_btn")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar original", maxLines = 1)
                }
                Button(
                    onClick = {
                        onSaveStyle(draft)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("extreme_save_btn")
                ) {
                    Text("Aplicar estilo", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onValueChange, valueRange = range)
    }
}
