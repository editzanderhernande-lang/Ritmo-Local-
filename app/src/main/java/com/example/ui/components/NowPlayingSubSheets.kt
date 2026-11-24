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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.PLAYER_COLOR_SWATCHES
import com.example.model.PlayerBackgroundMode
import com.example.model.PlayerControlStyle
import com.example.model.PlayerLayoutMode
import com.example.model.PlayerVisualPrefs
import com.example.model.PlayerVisualStyle
import com.example.model.SavedCustomEqProfile
import com.example.player.EqualizerUiState
import androidx.compose.material.icons.rounded.RestartAlt
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InPlayerEqualizerSheet(
    eqState: EqualizerUiState,
    customProfiles: List<SavedCustomEqProfile>,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onUpdateBand: (Int, Int) -> Unit,
    onUpdateBassBoost: (Int) -> Unit,
    onUpdateTrebleBoost: (Int) -> Unit = {},
    onToggleSpatial3d: (Boolean) -> Unit,
    onSaveCustomProfile: (String) -> Unit,
    onApplyCustomProfile: (SavedCustomEqProfile) -> Unit,
    onDeleteCustomProfile: (String) -> Unit,
    onOpenFullEqualizer: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileNameInput by remember { mutableStateOf("") }

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
                .testTag("in_player_eq_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = primary)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Ecualizador rápido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (eqState.enabled) {
                                "${eqState.studioAudioMode.displayName} · ${eqState.activeDeviceProfileType.shortLabel}"
                            } else {
                                "Ecualizador desactivado"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = eqState.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = primary),
                    modifier = Modifier.testTag("player_eq_enable_switch")
                )
            }

            // Built-in presets
            Text("Perfiles de sonido", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
                modifier = Modifier.testTag("player_eq_presets_row")
            ) {
                items(eqState.presets, key = { "preset_${it.index}" }) { preset ->
                    val selected = eqState.selectedPresetIndex == preset.index
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset.index) },
                        label = { Text(preset.name) },
                        modifier = Modifier.testTag("player_eq_preset_${preset.index}")
                    )
                }
            }

            // Saved custom EQ profiles
            if (customProfiles.isNotEmpty()) {
                Text("Mis perfiles guardados", style = MaterialTheme.typography.titleSmall)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(customProfiles, key = { it.id }) { profile ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .clickable { onApplyCustomProfile(profile) }
                                .testTag("player_eq_custom_profile_${profile.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(profile.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                IconButton(
                                    onClick = { onDeleteCustomProfile(profile.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Eliminar perfil ${profile.name}", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Frequency bands
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Frecuencias", style = MaterialTheme.typography.labelLarge, color = primary)
                    eqState.bands.forEach { band ->
                        val labelHz = if (band.centerFreqHz >= 1000) "${band.centerFreqHz / 1000} kHz" else "${band.centerFreqHz} Hz"
                        val dbVal = band.levelMb / 100f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = labelHz,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.width(60.dp)
                            )
                            Slider(
                                value = band.levelMb.toFloat(),
                                onValueChange = { onUpdateBand(band.index, it.roundToInt()) },
                                valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                enabled = eqState.enabled,
                                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("player_eq_band_${band.index}")
                            )
                            Text(
                                text = String.format("%+.1fdB", dbVal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(52.dp)
                            )
                        }
                    }
                }
            }

            // Bass boost + Spatial 3D
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Refuerzo de graves", style = MaterialTheme.typography.titleSmall)
                        Text("${(eqState.bassBoostStrength / 10)}%", style = MaterialTheme.typography.labelLarge, color = primary)
                    }
                    Slider(
                        value = eqState.bassBoostStrength.toFloat(),
                        onValueChange = { onUpdateBassBoost(it.roundToInt()) },
                        valueRange = 0f..1000f,
                        enabled = eqState.enabled,
                        colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                        modifier = Modifier.testTag("player_eq_bass_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Agudos (Treble)", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = String.format(java.util.Locale.US, "%+.1f dB", eqState.trebleBoostMb / 100f),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = eqState.trebleBoostMb.toFloat(),
                        onValueChange = { onUpdateTrebleBoost(it.roundToInt()) },
                        valueRange = -900f..900f,
                        enabled = eqState.enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("player_eq_treble_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dolby Atmos 3D Envolvente", style = MaterialTheme.typography.titleSmall)
                        }
                        Switch(
                            checked = eqState.spatial3dEnabled,
                            onCheckedChange = onToggleSpatial3d,
                            enabled = eqState.enabled,
                            modifier = Modifier.testTag("player_eq_spatial_switch")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        profileNameInput = ""
                        showSaveDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_eq_save_profile_btn")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar perfil", maxLines = 1)
                }
                Button(
                    onClick = {
                        onDismiss()
                        onOpenFullEqualizer()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_eq_full_screen_btn")
                ) {
                    Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Avanzado", maxLines = 1)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar perfil de ecualizador") },
            text = {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    placeholder = { Text("Ej: Bajo nocturno, Podcast…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("eq_profile_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (profileNameInput.isNotBlank()) {
                            onSaveCustomProfile(profileNameInput)
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("eq_profile_confirm_save_btn")
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCustomizationSheet(
    visualPrefs: PlayerVisualPrefs,
    isDarkTheme: Boolean,
    animationsEnabled: Boolean,
    onSelectStyle: (PlayerVisualStyle) -> Unit,
    onSelectBackgroundMode: (PlayerBackgroundMode) -> Unit,
    onSelectLayoutMode: (PlayerLayoutMode) -> Unit = {},
    onSelectControlStyle: (PlayerControlStyle) -> Unit = {},
    onUpdateBlurIntensity: (Float) -> Unit = {},
    onSelectPrimaryColor: (Long) -> Unit,
    onSelectButtonColor: (Long) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleVisualizer: (Boolean) -> Unit,
    onToggleInlineLyrics: (Boolean) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onRestoreDefaults: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary

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
                .testTag("player_customization_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Personalizar reproductor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Estilo visual, fondo dinámico, colores y animaciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Visual styles
            Text("Estilo del reproductor", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerVisualStyle.entries.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { style ->
                            val selected = visualPrefs.style == style
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .border(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectStyle(style) }
                                    .testTag("player_style_chip_${style.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = style.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = style.description,
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

            // Background mode
            Text("Fondo del reproductor", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlayerBackgroundMode.entries, key = { it.id }) { mode ->
                    FilterChip(
                        selected = visualPrefs.backgroundMode == mode,
                        onClick = { onSelectBackgroundMode(mode) },
                        label = { Text(mode.displayName) },
                        modifier = Modifier.testTag("player_bg_chip_${mode.id}")
                    )
                }
            }

            if (visualPrefs.backgroundMode == PlayerBackgroundMode.DYNAMIC_COVER) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Desenfoque de carátula de fondo", style = MaterialTheme.typography.labelMedium)
                        Text("${(visualPrefs.blurIntensity * 100f).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = primary)
                    }
                    Slider(
                        value = visualPrefs.blurIntensity,
                        onValueChange = onUpdateBlurIntensity,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                        modifier = Modifier.testTag("player_blur_slider")
                    )
                }
            }

            // Player Layout Mode
            Text("Modo de pantalla del reproductor", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("player_layout_modes_row")
            ) {
                items(PlayerLayoutMode.entries, key = { it.id }) { layoutMode ->
                    FilterChip(
                        selected = visualPrefs.layoutMode == layoutMode,
                        onClick = { onSelectLayoutMode(layoutMode) },
                        label = { Text(layoutMode.displayName) },
                        modifier = Modifier.testTag("player_layout_chip_${layoutMode.id}")
                    )
                }
            }

            // Player Control Style
            Text("Estilo de controles", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("player_control_styles_row")
            ) {
                items(PlayerControlStyle.entries, key = { it.id }) { controlStyle ->
                    FilterChip(
                        selected = visualPrefs.controlStyle == controlStyle,
                        onClick = { onSelectControlStyle(controlStyle) },
                        label = { Text(controlStyle.displayName) },
                        modifier = Modifier.testTag("player_control_chip_${controlStyle.id}")
                    )
                }
            }

            // Primary Color Swatches
            Text("Color principal del reproductor", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(PLAYER_COLOR_SWATCHES, key = { "pri_${it.id}" }) { swatch ->
                    val selected = visualPrefs.customPrimaryColorArgb == swatch.colorArgb
                    val circleColor = if (swatch.colorArgb == 0L) primary else Color(swatch.colorArgb)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectPrimaryColor(swatch.colorArgb) }
                            .padding(4.dp)
                            .testTag("player_primary_color_${swatch.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(swatch.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Button Color Swatches
            Text("Color de botones y controles", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(PLAYER_COLOR_SWATCHES, key = { "btn_${it.id}" }) { swatch ->
                    val selected = visualPrefs.customButtonColorArgb == swatch.colorArgb
                    val circleColor = if (swatch.colorArgb == 0L) primary else Color(swatch.colorArgb)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectButtonColor(swatch.colorArgb) }
                            .padding(4.dp)
                            .testTag("player_button_color_${swatch.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(circleColor)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(swatch.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Toggles
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CustomToggleRow(
                        icon = Icons.Rounded.DarkMode,
                        title = "Modo oscuro",
                        subtitle = "Cambia entre tema oscuro o claro",
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        tag = "player_dark_theme_switch"
                    )
                    CustomToggleRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Visualizador de frecuencias",
                        subtitle = "Barras reactivas al ritmo de la música",
                        checked = visualPrefs.showVisualizer,
                        onCheckedChange = onToggleVisualizer,
                        tag = "player_visualizer_switch"
                    )
                    CustomToggleRow(
                        icon = Icons.Rounded.Lyrics,
                        title = "Barra de letra en vivo",
                        subtitle = "Muestra el verso actual debajo del título",
                        checked = visualPrefs.showInlineLyricsBar,
                        onCheckedChange = onToggleInlineLyrics,
                        tag = "player_inline_lyrics_switch"
                    )
                    CustomToggleRow(
                        icon = Icons.Rounded.SlowMotionVideo,
                        title = "Animaciones musicales",
                        subtitle = "Desactívalo para ahorrar batería",
                        checked = animationsEnabled,
                        onCheckedChange = onToggleAnimations,
                        tag = "player_animations_switch"
                    )
                }
            }

            OutlinedButton(
                onClick = onRestoreDefaults,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("player_restore_defaults_btn")
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restaurar diseño original del reproductor")
            }
        }
    }
}

@Composable
private fun CustomToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = primary),
            modifier = Modifier.testTag(tag)
        )
    }
}
