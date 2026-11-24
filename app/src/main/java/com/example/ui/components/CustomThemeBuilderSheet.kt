package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.CUSTOM_THEME_BG_DARK_SWATCHES
import com.example.model.CUSTOM_THEME_BG_LIGHT_SWATCHES
import com.example.model.CUSTOM_THEME_PRIMARY_SWATCHES
import com.example.model.CustomThemeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomThemeBuilderSheet(
    savedThemes: List<CustomThemeConfig>,
    activeCustomThemeId: String?,
    onSaveTheme: (CustomThemeConfig) -> Unit,
    onApplyTheme: (CustomThemeConfig) -> Unit,
    onDeleteTheme: (String) -> Unit,
    onImportThemeJson: (String) -> Boolean,
    onShowMessage: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    val primary = MaterialTheme.colorScheme.primary

    var themeName by remember { mutableStateOf("") }
    var isDark by remember { mutableStateOf(true) }
    var primaryArgb by remember { mutableLongStateOf(CUSTOM_THEME_PRIMARY_SWATCHES.first()) }
    var secondaryArgb by remember { mutableLongStateOf(0xFFD946EF) }
    var backgroundArgb by remember { mutableLongStateOf(CUSTOM_THEME_BG_DARK_SWATCHES.first()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }

    val surfaceArgb = if (isDark) 0xFF161326 else 0xFFFFFFFF
    val bgChoices = if (isDark) CUSTOM_THEME_BG_DARK_SWATCHES else CUSTOM_THEME_BG_LIGHT_SWATCHES

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
                .testTag("custom_theme_builder_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(primaryArgb).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ColorLens, contentDescription = null, tint = Color(primaryArgb))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Creador de Temas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Crea, guarda, exporta e importa combinaciones de color",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Preview Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(backgroundArgb),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(primaryArgb), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(primaryArgb).copy(alpha = 0.18f), Color(secondaryArgb).copy(alpha = 0.12f))
                            )
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = themeName.ifBlank { "Vista previa del tema" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF14111F)
                        )
                        Text(
                            text = if (isDark) "Fondo oscuro personalizado" else "Fondo claro personalizado",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(primaryArgb), Color(secondaryArgb))))
                    )
                }
            }

            OutlinedTextField(
                value = themeName,
                onValueChange = { themeName = it.take(36) },
                label = { Text("Nombre del tema") },
                placeholder = { Text("Ej: Noche Cibernética") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_theme_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Base oscura (desactívalo para claro)", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = isDark,
                    onCheckedChange = { nextDark ->
                        isDark = nextDark
                        backgroundArgb = if (nextDark) CUSTOM_THEME_BG_DARK_SWATCHES.first() else CUSTOM_THEME_BG_LIGHT_SWATCHES.first()
                    },
                    modifier = Modifier.testTag("custom_theme_dark_switch")
                )
            }

            SwatchRowSelector(
                title = "Color principal",
                swatches = CUSTOM_THEME_PRIMARY_SWATCHES,
                selectedArgb = primaryArgb,
                tagPrefix = "custom_theme_pri",
                onSelect = { primaryArgb = it }
            )

            SwatchRowSelector(
                title = "Color secundario",
                swatches = CUSTOM_THEME_PRIMARY_SWATCHES.reversed(),
                selectedArgb = secondaryArgb,
                tagPrefix = "custom_theme_sec",
                onSelect = { secondaryArgb = it }
            )

            SwatchRowSelector(
                title = "Color de fondo",
                swatches = bgChoices,
                selectedArgb = backgroundArgb,
                tagPrefix = "custom_theme_bg",
                onSelect = { backgroundArgb = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val draft = CustomThemeConfig(
                            id = "theme_${System.currentTimeMillis()}",
                            name = themeName.trim().ifBlank { "Mi Tema ${savedThemes.size + 1}" },
                            isDark = isDark,
                            primaryArgb = primaryArgb,
                            secondaryArgb = secondaryArgb,
                            backgroundArgb = backgroundArgb,
                            surfaceArgb = surfaceArgb,
                            buttonArgb = primaryArgb
                        )
                        onSaveTheme(draft)
                        themeName = ""
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("custom_theme_save_btn")
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Guardar y aplicar", maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        importJsonText = ""
                        importError = null
                        showImportDialog = true
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("custom_theme_open_import_btn")
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Importar")
                }
            }

            if (savedThemes.isNotEmpty()) {
                Text("Mis temas guardados (${savedThemes.size})", style = MaterialTheme.typography.titleSmall)
                savedThemes.forEach { saved ->
                    val isSelected = saved.id == activeCustomThemeId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onApplyTheme(saved) }
                            .testTag("saved_custom_theme_${saved.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(saved.primaryArgb), Color(saved.secondaryArgb))))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(saved.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    if (saved.isDark) "Oscuro personalizado" else "Claro personalizado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(saved.toJsonString()))
                                    onShowMessage("JSON del tema '${saved.name}' copiado al portapapeles")
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("export_theme_${saved.id}")
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Exportar JSON de ${saved.name}", tint = primary)
                            }
                            IconButton(
                                onClick = { onDeleteTheme(saved.id) },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("delete_theme_${saved.id}")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar tema ${saved.name}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importar tema (JSON)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importError = null
                        },
                        placeholder = { Text("Pega aquí el JSON exportado de Ritmo Local…") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_theme_import_input")
                    )
                    if (importError != null) {
                        Text(importError!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onImportThemeJson(importJsonText)) {
                            showImportDialog = false
                        } else {
                            importError = "El JSON no es válido o le falta el nombre del tema."
                        }
                    },
                    modifier = Modifier.testTag("custom_theme_import_confirm_btn")
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SwatchRowSelector(
    title: String,
    swatches: List<Long>,
    selectedArgb: Long,
    tagPrefix: String,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(swatches, key = { "${tagPrefix}_$it" }) { argb ->
                val selected = argb == selectedArgb
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(argb) }
                        .testTag("${tagPrefix}_${argb.toString(16)}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = "Seleccionado", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
