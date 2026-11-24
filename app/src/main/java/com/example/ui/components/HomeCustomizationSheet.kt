package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.HomeLayoutConfig
import com.example.model.HomeLayoutPreset
import com.example.model.HomeShortcutType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeCustomizationSheet(
    config: HomeLayoutConfig,
    savedPresets: List<HomeLayoutPreset>,
    onUpdateConfig: (HomeLayoutConfig) -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (HomeLayoutPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val primary = MaterialTheme.colorScheme.primary
    val normalized = remember(config) { config.normalized() }
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }

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
                .testTag("home_customization_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, tint = primary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Personalizar pantalla de Inicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Reordena secciones, fija accesos directos y guarda configuraciones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Orden y visibilidad de secciones", style = MaterialTheme.typography.titleSmall)
            normalized.orderedSections.forEachIndexed { idx, section ->
                val isVisible = section !in normalized.hiddenSections
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_section_row_${section.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(section.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text(section.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { onUpdateConfig(normalized.moveSection(section, -1)) },
                            enabled = idx > 0,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("home_move_up_${section.id}")
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir ${section.displayName}")
                        }
                        IconButton(
                            onClick = { onUpdateConfig(normalized.moveSection(section, 1)) },
                            enabled = idx < normalized.orderedSections.lastIndex,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("home_move_down_${section.id}")
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar ${section.displayName}")
                        }
                        IconButton(
                            onClick = { onUpdateConfig(normalized.toggleSectionVisibility(section)) },
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("home_toggle_vis_${section.id}")
                        ) {
                            Icon(
                                imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = if (isVisible) "Ocultar ${section.displayName}" else "Mostrar ${section.displayName}",
                                tint = if (isVisible) primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Text("Accesos directos fijados", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HomeShortcutType.entries.forEach { shortcut ->
                    val active = shortcut in normalized.pinnedShortcuts
                    FilterChip(
                        selected = active,
                        onClick = { onUpdateConfig(normalized.togglePinnedShortcut(shortcut)) },
                        label = { Text(shortcut.label) },
                        modifier = Modifier.testTag("home_shortcut_chip_${shortcut.id}")
                    )
                }
            }

            Text("Elementos visibles en cada canción", style = MaterialTheme.typography.titleSmall)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    HomeInfoToggleRow(
                        label = "Mostrar etiqueta de formato (FLAC, MP3…)",
                        checked = normalized.showFormatBadge,
                        tag = "home_show_format_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showFormatBadge = it)) }
                    )
                    HomeInfoToggleRow(
                        label = "Mostrar nombre del álbum",
                        checked = normalized.showAlbumName,
                        tag = "home_show_album_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showAlbumName = it)) }
                    )
                    HomeInfoToggleRow(
                        label = "Mostrar duración",
                        checked = normalized.showDuration,
                        tag = "home_show_duration_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showDuration = it)) }
                    )
                }
            }

            Text("Estadísticas y tarjetas visibles en Inicio", style = MaterialTheme.typography.titleSmall)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    HomeInfoToggleRow(
                        label = "Mostrar tiempo escuchado hoy",
                        checked = normalized.showTodayTimeStat,
                        tag = "home_show_today_time_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showTodayTimeStat = it)) }
                    )
                    HomeInfoToggleRow(
                        label = "Mostrar canciones reproducidas hoy",
                        checked = normalized.showTodayPlaysStat,
                        tag = "home_show_today_plays_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showTodayPlaysStat = it)) }
                    )
                    HomeInfoToggleRow(
                        label = "Mostrar artistas escuchados hoy",
                        checked = normalized.showTodayArtistsStat,
                        tag = "home_show_today_artists_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showTodayArtistsStat = it)) }
                    )
                    HomeInfoToggleRow(
                        label = "Mostrar tarjeta Ritmo Recap y Perfil",
                        checked = normalized.showRecapBannerCard,
                        tag = "home_show_recap_banner_switch",
                        onCheckedChange = { onUpdateConfig(normalized.copy(showRecapBannerCard = it)) }
                    )
                }
            }

            if (savedPresets.isNotEmpty()) {
                Text("Configuraciones guardadas", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedPresets, key = { it.id }) { preset ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { onApplyPreset(preset) }
                                .testTag("home_preset_chip_${preset.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(preset.name, style = MaterialTheme.typography.labelLarge)
                                IconButton(
                                    onClick = { onDeletePreset(preset.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Eliminar ${preset.name}", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        presetName = ""
                        showSaveDialog = true
                    },
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_save_preset_btn")
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar diseño", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onResetDefault,
                    modifier = Modifier.weight(1f).minimumInteractiveComponentSize().testTag("home_reset_default_btn")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restaurar original", maxLines = 1)
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar diseño de Inicio") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it.take(36) },
                    placeholder = { Text("Ej: Minimalista nocturno, Todo a mano…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("home_preset_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetName.isNotBlank()) {
                            onSavePreset(presetName)
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("home_preset_confirm_save_btn")
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun HomeInfoToggleRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
