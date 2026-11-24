package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Stadium
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.example.model.AudioChannelMode
import com.example.model.AudioDeviceProfileType
import com.example.model.DeviceSoundProfile
import com.example.model.HearingProfile
import com.example.model.Spatial3dPreset
import com.example.model.StudioAudioMode
import com.example.model.UiStyleMode
import com.example.player.EqBandUi
import com.example.player.EqualizerUiState
import com.example.ui.i18n.LocalExtraStrings
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.EmeraldSync
import com.example.ui.theme.LocalUiStyleMode
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    eqState: EqualizerUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onUpdateBand: (Int, Int) -> Unit,
    onUpdateBassBoost: (Int) -> Unit,
    onToggleSpatial3d: (Boolean) -> Unit,
    onSelectSpatial3dPreset: (Spatial3dPreset) -> Unit,
    onUpdateSpatial3dStrength: (Int) -> Unit,
    onUpdateClarityBoost: (Int) -> Unit,
    onSelectHearingProfile: (HearingProfile) -> Unit,
    onSelectAudioChannelMode: (AudioChannelMode) -> Unit,
    onUpdateBalanceLR: (Float) -> Unit,
    onToggleHiResMastering: (Boolean) -> Unit = {},
    onSelectStudioAudioMode: (StudioAudioMode) -> Unit = {},
    onUpdateDynamicPunch: (Int) -> Unit = {},
    onToggleAntiClipLimiter: (Boolean) -> Unit = {},
    deviceSoundProfiles: Map<AudioDeviceProfileType, DeviceSoundProfile> = emptyMap(),
    detectedDeviceType: AudioDeviceProfileType = eqState.activeDeviceProfileType,
    onUpdateTrebleBoost: (Int) -> Unit = {},
    onToggleAutoSwitchDeviceProfile: (Boolean) -> Unit = {},
    onSelectDeviceProfile: (AudioDeviceProfileType) -> Unit = {},
    onSaveCurrentDeviceProfile: (AudioDeviceProfileType) -> Unit = {},
    onResetDeviceProfile: (AudioDeviceProfileType) -> Unit = {},
    onOpenThemeCustomization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val extra = LocalExtraStrings.current
    val isNeon = LocalUiStyleMode.current == UiStyleMode.NEON_MODERN
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    var expandAdaptSound by remember { mutableStateOf(false) }
    var expandCustomPresets by remember { mutableStateOf(false) }
    var expandHiResCard by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("equalizer_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.eqTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "MP3, FLAC, WAV, OGG, OPUS, M4A, AAC + Dolby Atmos 3D y Adaptación Auditiva.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onOpenThemeCustomization,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("eq_open_theme_button")
                ) {
                    Icon(Icons.Rounded.Palette, contentDescription = "Personalizar colores", tint = primary)
                }
            }
        }

        if (isNeon) {
            // Neon Mockup Top Dolby Atmos Highlight Card + 4 Square Icon Mode Tiles
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.2.dp, primary.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(primary.copy(alpha = 0.16f), Color.Transparent)))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(primary, secondary))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Dolby Atmos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Sonido envolvente 3D · 32-bit",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = eqState.spatial3dEnabled,
                            onCheckedChange = onToggleSpatial3d,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primary),
                            modifier = Modifier.testTag("eq_dolby3d_switch")
                        )
                    }
                }
            }

            item {
                val quickTiles = listOf(
                    Triple(Spatial3dPreset.MUSIC_3D, "Música", Icons.Rounded.MusicNote),
                    Triple(Spatial3dPreset.CINEMA_3D, "Cine", Icons.Rounded.Movie),
                    Triple(Spatial3dPreset.CRYSTAL_VOICE, "Vocal", Icons.Rounded.RecordVoiceOver),
                    Triple(Spatial3dPreset.STADIUM, "Estadio", Icons.Rounded.Stadium)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    quickTiles.forEach { (preset, shortLabel, tileIcon) ->
                        val active = eqState.spatial3dEnabled && eqState.spatial3dPreset == preset
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (active) primary.copy(alpha = 0.24f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    1.dp,
                                    if (active) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable { onSelectSpatial3dPreset(preset) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tileIcon,
                                        contentDescription = shortLabel,
                                        tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Master Switch & Status
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (eqState.enabled) primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = null,
                            tint = if (eqState.enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (eqState.enabled) "Ecualizador Activo" else "Ecualizador Apagado",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (eqState.isHardwareAttached) {
                                    "Motor DSP conectado (${if (eqState.isDynamicsProcessingAttached) "32-bit Hi-Res + EQ" else "Hardware EQ"})"
                                } else {
                                    "Se aplicará automáticamente al reproducir"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = eqState.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primary
                        ),
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }
        }

        // Per-Device Sound Profiles Card (scoped inside Ritmo Local)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        primary.copy(alpha = if (eqState.autoSwitchDeviceProfile) 0.45f else 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("eq_device_profiles_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Perfiles por dispositivo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Detectado: ${detectedDeviceType.displayName} · Solo dentro de Ritmo Local",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = eqState.autoSwitchDeviceProfile,
                            onCheckedChange = onToggleAutoSwitchDeviceProfile,
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primary),
                            modifier = Modifier.testTag("eq_device_autoswitch_switch")
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("eq_device_chips_row")
                    ) {
                        items(AudioDeviceProfileType.entries, key = { it.id }) { devType ->
                            val selected = eqState.activeDeviceProfileType == devType
                            val hasSaved = deviceSoundProfiles.containsKey(devType)
                            val icon = when (devType) {
                                AudioDeviceProfileType.PHONE_SPEAKER -> Icons.Rounded.PhoneAndroid
                                AudioDeviceProfileType.WIRED_HEADSET -> Icons.Rounded.Headphones
                                AudioDeviceProfileType.BLUETOOTH -> Icons.Rounded.BluetoothAudio
                                AudioDeviceProfileType.OTHER_OUTPUT -> Icons.Rounded.SpeakerGroup
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectDeviceProfile(devType) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (hasSaved && selected) Icons.Rounded.Check else icon,
                                        contentDescription = devType.shortLabel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(if (hasSaved) "${devType.shortLabel} •" else devType.shortLabel)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primary.copy(alpha = 0.22f),
                                    selectedLabelColor = primary
                                ),
                                modifier = Modifier.testTag("eq_device_chip_${devType.id}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSaveCurrentDeviceProfile(eqState.activeDeviceProfileType) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("eq_save_device_profile_btn")
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Guardar en ${eqState.activeDeviceProfileType.shortLabel}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (deviceSoundProfiles.containsKey(eqState.activeDeviceProfileType)) {
                            TextButton(
                                onClick = { onResetDeviceProfile(eqState.activeDeviceProfileType) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("eq_reset_device_profile_btn")
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restablecer")
                            }
                        }
                    }
                }
            }
        }

        // Frequency Curve Canvas
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Curva de Frecuencia",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val activePresetLabel = if (eqState.selectedPresetIndex == -1) {
                            "Personalizado"
                        } else {
                            eqState.presets.getOrNull(eqState.selectedPresetIndex)?.name ?: "Plano"
                        }
                        Surface(shape = RoundedCornerShape(50), color = primary.copy(alpha = 0.16f)) {
                            Text(
                                text = activePresetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    EqualizerCurveCanvas(
                        bands = eqState.bands,
                        enabled = eqState.enabled,
                        primaryColor = primary,
                        secondaryColor = secondary,
                        gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                }
            }
        }

        // Presets Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("eq_presets_row")
            ) {
                items(eqState.presets, key = { it.index }) { preset ->
                    val selected = eqState.selectedPresetIndex == preset.index
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset.index) },
                        label = { Text(preset.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primary.copy(alpha = 0.22f),
                            selectedLabelColor = primary
                        ),
                        modifier = Modifier.testTag("eq_preset_${preset.index}")
                    )
                }
            }
        }

        // 5-Band Sliders (Vertical Neon faders in NEON_MODERN, Horizontal classic sliders in ORIGINAL/MINIMALIST)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Bandas de frecuencia",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isNeon && eqState.bands.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            eqState.bands.forEach { band ->
                                val minF = band.minLevelMb.toFloat()
                                val maxF = band.maxLevelMb.toFloat().coerceAtLeast(minF + 1f)
                                val dbValue = band.levelMb / 100f
                                val shortFreq = when {
                                    band.centerFreqHz >= 10000 -> "${band.centerFreqHz / 1000}k"
                                    band.centerFreqHz >= 1000 -> String.format(Locale.US, "%.1fk", band.centerFreqHz / 1000f)
                                    else -> "${band.centerFreqHz}"
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%+.0f", dbValue),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (eqState.enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    VerticalBandSlider(
                                        value = band.levelMb.toFloat().coerceIn(minF, maxF),
                                        onValueChange = { onUpdateBand(band.index, it.roundToInt()) },
                                        valueRange = minF..maxF,
                                        enabled = eqState.enabled,
                                        accentColor = primary,
                                        testTagName = "eq_band_slider_${band.index}"
                                    )
                                    Text(
                                        text = shortFreq,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        eqState.bands.forEach { band ->
                            val minF = band.minLevelMb.toFloat()
                            val maxF = band.maxLevelMb.toFloat().coerceAtLeast(minF + 1f)
                            val dbValue = band.levelMb / 100f
                            val freqLabel = if (band.centerFreqHz >= 1000) {
                                String.format(Locale.US, "%.1f kHz", band.centerFreqHz / 1000f)
                            } else {
                                "${band.centerFreqHz} Hz"
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(freqLabel, style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        text = String.format(Locale.US, "%+.1f dB", dbValue),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (eqState.enabled) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Slider(
                                    value = band.levelMb.toFloat().coerceIn(minF, maxF),
                                    onValueChange = { onUpdateBand(band.index, it.roundToInt()) },
                                    valueRange = minF..maxF,
                                    enabled = eqState.enabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = primary,
                                        activeTrackColor = primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.testTag("eq_band_slider_${band.index}")
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isNeon) {
            // Mockup Panel 4 stacked sleek settings card: Refuerzo de bajos, Virtualizador 3D, Adaptar sonido >, Preajustes personalizados >
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        // Row 1: Refuerzo de bajos
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Refuerzo de bajos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${eqState.bassBoostStrength / 10}%", style = MaterialTheme.typography.labelSmall, color = primary, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = eqState.bassBoostStrength > 0,
                                    onCheckedChange = { checked -> onUpdateBassBoost(if (checked) 650 else 0) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primary)
                                )
                            }
                        }
                        Slider(
                            value = eqState.bassBoostStrength.toFloat(),
                            onValueChange = { onUpdateBassBoost(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                            modifier = Modifier.testTag("eq_bass_boost_slider")
                        )

                        // Row 1B: Control rápido de Agudos (Treble)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = secondary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Agudos (Treble)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = String.format(Locale.US, "%+.1f dB", eqState.trebleBoostMb / 100f),
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = eqState.trebleBoostMb.toFloat(),
                            onValueChange = { onUpdateTrebleBoost(it.roundToInt()) },
                            valueRange = -900f..900f,
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(thumbColor = secondary, activeTrackColor = secondary),
                            modifier = Modifier.testTag("eq_treble_boost_slider")
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        // Row 2: Virtualizador 3D
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Virtualizador 3D", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${eqState.spatial3dStrength / 10}%", style = MaterialTheme.typography.labelSmall, color = primary, fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = eqState.spatial3dEnabled,
                                    onCheckedChange = onToggleSpatial3d,
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primary)
                                )
                            }
                        }
                        if (eqState.spatial3dEnabled) {
                            Slider(
                                value = eqState.spatial3dStrength.toFloat(),
                                onValueChange = { onUpdateSpatial3dStrength(it.roundToInt()) },
                                valueRange = 0f..1000f,
                                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        // Row 3: Adaptar sonido >
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandAdaptSound = !expandAdaptSound }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Hearing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Adaptar sonido", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Según tu audición · ${eqState.hearingProfile.displayName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                if (expandAdaptSound) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (expandAdaptSound) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                                HearingProfile.entries.forEach { profile ->
                                    val selected = eqState.hearingProfile == profile
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, if (selected) primary else Color.Transparent, RoundedCornerShape(14.dp))
                                            .clickable { onSelectHearingProfile(profile) }
                                            .testTag("hearing_profile_${profile.id}")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(profile.displayName, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                            Text(profile.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Claridad Envolvente y Diálogos", style = MaterialTheme.typography.labelMedium)
                                    Text("${eqState.clarityBoostMb / 10}%", style = MaterialTheme.typography.labelMedium, color = secondary, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = eqState.clarityBoostMb.toFloat(),
                                    onValueChange = { onUpdateClarityBoost(it.roundToInt()) },
                                    valueRange = 0f..1000f,
                                    enabled = eqState.spatial3dEnabled,
                                    colors = SliderDefaults.colors(thumbColor = secondary, activeTrackColor = secondary)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

                        // Row 4: Preajustes personalizados > (Stereo/Mono/Balance)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandCustomPresets = !expandCustomPresets }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Preajustes personalizados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${eqState.audioChannelMode.name} · Balance L/R", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                if (expandCustomPresets) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (expandCustomPresets) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    FilterChip(
                                        selected = eqState.audioChannelMode == AudioChannelMode.STEREO,
                                        onClick = { onSelectAudioChannelMode(AudioChannelMode.STEREO) },
                                        label = { Text(strings.stereoLabel) },
                                        modifier = Modifier.weight(1f).testTag("channel_stereo_chip")
                                    )
                                    FilterChip(
                                        selected = eqState.audioChannelMode == AudioChannelMode.MONO,
                                        onClick = { onSelectAudioChannelMode(AudioChannelMode.MONO) },
                                        label = { Text(strings.monoLabel) },
                                        modifier = Modifier.weight(1f).testTag("channel_mono_chip")
                                    )
                                    FilterChip(
                                        selected = eqState.audioChannelMode == AudioChannelMode.BALANCED,
                                        onClick = { onSelectAudioChannelMode(AudioChannelMode.BALANCED) },
                                        label = { Text("Balance") },
                                        modifier = Modifier.weight(1f).testTag("channel_balanced_chip")
                                    )
                                }
                                Slider(
                                    value = eqState.balanceLR,
                                    onValueChange = { onUpdateBalanceLR(it) },
                                    valueRange = -1f..1f,
                                    colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                                    modifier = Modifier.testTag("eq_balance_slider")
                                )
                            }
                        }
                    }
                }
            }

            // Hi-Res 32-bit Studio Mastering Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (eqState.hiResMasteringEnabled) EmeraldSync.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expandHiResCard = !expandHiResCard },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = EmeraldSync, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(extra.studioMasterTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "32-bit Float · ${eqState.studioAudioMode.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = eqState.hiResMasteringEnabled,
                                onCheckedChange = onToggleHiResMastering,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldSync),
                                modifier = Modifier.testTag("eq_hires_master_switch")
                            )
                        }
                        if (expandHiResCard) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(StudioAudioMode.entries, key = { it.id }) { mode ->
                                    FilterChip(
                                        selected = eqState.hiResMasteringEnabled && eqState.studioAudioMode == mode,
                                        onClick = { onSelectStudioAudioMode(mode) },
                                        label = { Text(mode.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldSync.copy(alpha = 0.2f),
                                            selectedLabelColor = EmeraldSync
                                        ),
                                        modifier = Modifier.testTag("eq_studio_mode_${mode.id}")
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Compresión dinámica", style = MaterialTheme.typography.labelMedium)
                                Text("${eqState.dynamicPunchStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = EmeraldSync, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = eqState.dynamicPunchStrength.toFloat(),
                                onValueChange = { onUpdateDynamicPunch(it.roundToInt()) },
                                valueRange = 0f..1000f,
                                enabled = eqState.hiResMasteringEnabled,
                                colors = SliderDefaults.colors(thumbColor = EmeraldSync, activeTrackColor = EmeraldSync),
                                modifier = Modifier.testTag("eq_dynamic_punch_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Limitador anti-distorsión (-0.8 dB)", style = MaterialTheme.typography.labelLarge)
                                Switch(
                                    checked = eqState.antiClipLimiterEnabled,
                                    onCheckedChange = onToggleAntiClipLimiter,
                                    enabled = eqState.hiResMasteringEnabled,
                                    modifier = Modifier.testTag("eq_anticlip_switch")
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Bass Boost Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refuerzo de Graves (Bass Boost)", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "${(eqState.bassBoostStrength / 10)}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = eqState.bassBoostStrength.toFloat(),
                            onValueChange = { onUpdateBassBoost(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(thumbColor = secondary, activeTrackColor = secondary),
                            modifier = Modifier.testTag("eq_bass_boost_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, tint = primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Agudos (Treble)", style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = String.format(Locale.US, "%+.1f dB", eqState.trebleBoostMb / 100f),
                                style = MaterialTheme.typography.labelMedium,
                                color = primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = eqState.trebleBoostMb.toFloat(),
                            onValueChange = { onUpdateTrebleBoost(it.roundToInt()) },
                            valueRange = -900f..900f,
                            enabled = eqState.enabled,
                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                            modifier = Modifier.testTag("eq_treble_boost_slider")
                        )
                    }
                }
            }

            // Hi-Res 32-bit Studio Mastering Engine (DynamicsProcessing)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (eqState.hiResMasteringEnabled) EmeraldSync.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = EmeraldSync, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(extra.studioMasterTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (eqState.isDynamicsProcessingSupported) {
                                            "PCM Float 32-bit + 10 bandas Pre-EQ + Compresor multibanda + Limitador"
                                        } else {
                                            "Pipeline PCM Float 32-bit activo"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = eqState.hiResMasteringEnabled,
                                onCheckedChange = onToggleHiResMastering,
                                modifier = Modifier.testTag("eq_hires_master_switch")
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(StudioAudioMode.entries, key = { it.id }) { mode ->
                                FilterChip(
                                    selected = eqState.hiResMasteringEnabled && eqState.studioAudioMode == mode,
                                    onClick = { onSelectStudioAudioMode(mode) },
                                    label = { Text(mode.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldSync.copy(alpha = 0.2f),
                                        selectedLabelColor = EmeraldSync
                                    ),
                                    modifier = Modifier.testTag("eq_studio_mode_${mode.id}")
                                )
                            }
                        }

                        Text(
                            text = eqState.studioAudioMode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Compresión dinámica (Rango / Pegada)", style = MaterialTheme.typography.labelMedium)
                            Text("${eqState.dynamicPunchStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = EmeraldSync, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = eqState.dynamicPunchStrength.toFloat(),
                            onValueChange = { onUpdateDynamicPunch(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.hiResMasteringEnabled,
                            colors = SliderDefaults.colors(thumbColor = EmeraldSync, activeTrackColor = EmeraldSync),
                            modifier = Modifier.testTag("eq_dynamic_punch_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Limitador anti-distorsión (-0.8 dB)", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = "Evita saturación al subir graves o ecualizador al máximo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = eqState.antiClipLimiterEnabled,
                                onCheckedChange = onToggleAntiClipLimiter,
                                enabled = eqState.hiResMasteringEnabled,
                                modifier = Modifier.testTag("eq_anticlip_switch")
                            )
                        }
                    }
                }
            }

            // Dolby Atmos / Spatial 3D Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (eqState.spatial3dEnabled) primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(22.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.SurroundSound, contentDescription = null, tint = primary, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.dolby3dTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Virtualizador espacial, amplitud 3D y claridad envolvente",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = eqState.spatial3dEnabled,
                                onCheckedChange = onToggleSpatial3d,
                                modifier = Modifier.testTag("eq_dolby3d_switch")
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(Spatial3dPreset.entries, key = { it.id }) { preset ->
                                FilterChip(
                                    selected = eqState.spatial3dPreset == preset && eqState.spatial3dEnabled,
                                    onClick = { onSelectSpatial3dPreset(preset) },
                                    label = { Text(preset.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primary.copy(alpha = 0.22f),
                                        selectedLabelColor = primary
                                    )
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Profundidad / Amplitud 3D", style = MaterialTheme.typography.labelMedium)
                            Text("${eqState.spatial3dStrength / 10}%", style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = eqState.spatial3dStrength.toFloat(),
                            onValueChange = { onUpdateSpatial3dStrength(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.spatial3dEnabled,
                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Claridad Envolvente y Diálogos", style = MaterialTheme.typography.labelMedium)
                            Text("${eqState.clarityBoostMb / 10}%", style = MaterialTheme.typography.labelMedium, color = secondary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = eqState.clarityBoostMb.toFloat(),
                            onValueChange = { onUpdateClarityBoost(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.spatial3dEnabled,
                            colors = SliderDefaults.colors(thumbColor = secondary, activeTrackColor = secondary)
                        )
                    }
                }
            }

            // Hearing Adaptation Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Hearing, contentDescription = null, tint = secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(strings.hearingAdaptationTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Adapta frecuencias y ganancia según tu perfil de audición",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HearingProfile.entries.forEach { profile ->
                            val selected = eqState.hearingProfile == profile
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) secondary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (selected) secondary.copy(alpha = 0.5f) else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectHearingProfile(profile) }
                                    .testTag("hearing_profile_${profile.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = profile.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) secondary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = profile.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Audio Channel Type (Mono / Stereo / L-R Balance)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SpeakerGroup, contentDescription = null, tint = primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.audioTypeTitle, style = MaterialTheme.typography.titleMedium)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = eqState.audioChannelMode == AudioChannelMode.STEREO,
                                onClick = { onSelectAudioChannelMode(AudioChannelMode.STEREO) },
                                label = { Text(strings.stereoLabel) },
                                modifier = Modifier.weight(1f).testTag("channel_stereo_chip")
                            )
                            FilterChip(
                                selected = eqState.audioChannelMode == AudioChannelMode.MONO,
                                onClick = { onSelectAudioChannelMode(AudioChannelMode.MONO) },
                                label = { Text(strings.monoLabel) },
                                modifier = Modifier.weight(1f).testTag("channel_mono_chip")
                            )
                            FilterChip(
                                selected = eqState.audioChannelMode == AudioChannelMode.BALANCED,
                                onClick = { onSelectAudioChannelMode(AudioChannelMode.BALANCED) },
                                label = { Text("Balance") },
                                modifier = Modifier.weight(1f).testTag("channel_balanced_chip")
                            )
                        }

                        val balPct = (eqState.balanceLR * 100f).roundToInt()
                        val balText = when {
                            balPct < -3 -> "L ${-balPct}%"
                            balPct > 3 -> "R ${balPct}%"
                            else -> "Centro"
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.balanceLabel, style = MaterialTheme.typography.labelMedium)
                            Text(balText, style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = eqState.balanceLR,
                            onValueChange = { onUpdateBalanceLR(it) },
                            valueRange = -1f..1f,
                            colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                            modifier = Modifier.testTag("eq_balance_slider")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerCurveCanvas(
    bands: List<EqBandUi>,
    enabled: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        drawLine(color = gridColor, start = Offset(0f, midY), end = Offset(w, midY), strokeWidth = 1.5f)
        drawLine(color = gridColor.copy(alpha = 0.4f), start = Offset(0f, h * 0.25f), end = Offset(w, h * 0.25f), strokeWidth = 1f)
        drawLine(color = gridColor.copy(alpha = 0.4f), start = Offset(0f, h * 0.75f), end = Offset(w, h * 0.75f), strokeWidth = 1f)

        if (bands.isEmpty()) return@Canvas

        val padX = 18f
        val usableW = (w - padX * 2f).coerceAtLeast(1f)
        val points = bands.mapIndexed { idx, band ->
            val x = if (bands.size == 1) w / 2f else padX + (idx.toFloat() / (bands.size - 1)) * usableW
            val min = band.minLevelMb.toFloat()
            val max = band.maxLevelMb.toFloat().coerceAtLeast(min + 1f)
            val normalized = ((band.levelMb - min) / (max - min)).coerceIn(0f, 1f)
            val y = (1f - normalized) * (h - 24f) + 12f
            Offset(x, y)
        }

        val curvePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        val lineBrush = if (enabled) {
            Brush.horizontalGradient(listOf(primaryColor, secondaryColor))
        } else {
            Brush.horizontalGradient(listOf(gridColor, gridColor))
        }

        drawPath(path = curvePath, brush = lineBrush, style = Stroke(width = 5f, cap = StrokeCap.Round))

        points.forEach { pt ->
            drawCircle(color = if (enabled) primaryColor else gridColor, radius = 8f, center = pt)
        }
    }
}

@Composable
private fun VerticalBandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: Color,
    testTagName: String
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = accentColor,
            activeTrackColor = accentColor,
            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin.Center
            }
            .layout { measurable, constraints ->
                val targetHeightPx = 152.dp.roundToPx()
                val targetWidthPx = 48.dp.roundToPx()
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = targetHeightPx,
                        maxWidth = targetHeightPx,
                        minHeight = targetWidthPx.coerceAtMost(constraints.maxWidth.coerceAtLeast(targetWidthPx)),
                        maxHeight = targetWidthPx
                    )
                )
                layout(placeable.height, placeable.width) {
                    placeable.place(
                        x = (placeable.height - placeable.width) / 2,
                        y = (placeable.width - placeable.height) / 2
                    )
                }
            }
            .testTag(testTagName)
    )
}
