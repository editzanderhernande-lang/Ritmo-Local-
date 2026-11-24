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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import android.os.Build
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import kotlin.math.roundToInt
import com.example.model.AppAccentPalette
import com.example.model.AppFontStyle
import com.example.model.AppLanguage
import com.example.model.UiStyleMode
import com.example.model.UserAppearancePrefs
import com.example.ui.i18n.extraStringsFor
import com.example.ui.theme.displayFamilyFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationSheet(
    appearancePrefs: UserAppearancePrefs,
    appLanguage: AppLanguage = AppLanguage.ES_LATAM,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSelectPalette: (AppAccentPalette) -> Unit,
    onSelectUiStyleMode: (UiStyleMode) -> Unit = {},
    onSelectFontStyle: (AppFontStyle) -> Unit = {},
    onFontSizeScaleChange: (Float) -> Unit = {},
    onPickWallpaper: () -> Unit = {},
    onClearWallpaper: () -> Unit = {},
    onWallpaperDimChange: (Float) -> Unit = {},
    onWallpaperBlurChange: (Float) -> Unit = {},
    onToggleMaterialYou: (Boolean) -> Unit = {},
    onOpenCustomThemeBuilder: () -> Unit = {},
    onOpenExtremeVisualEditor: () -> Unit = {},
    onRestoreDefaultAppearance: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extra = extraStringsFor(appLanguage)
    val primary = MaterialTheme.colorScheme.primary
    val hasWallpaper = !appearancePrefs.wallpaperUri.isNullOrBlank()

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Apariencia, Temas y Fondo",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cambia el estilo visual, colores y fondo de pantalla al instante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // UI Style Switcher (Minimalist vs Original)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesomeMosaic, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(extra.uiStyleTitle, style = MaterialTheme.typography.titleSmall)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UiStyleMode.entries.forEach { mode ->
                            val selected = appearancePrefs.uiStyleMode == mode
                            val label = when (mode) {
                                UiStyleMode.NEON_MODERN -> extra.neonModernLabel
                                UiStyleMode.MINIMALIST -> extra.minimalistLabel
                                UiStyleMode.ORIGINAL -> extra.originalLabel
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectUiStyleMode(mode) },
                                label = { Text(label, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primary.copy(alpha = 0.24f),
                                    selectedLabelColor = primary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ui_style_chip_${mode.id}")
                            )
                        }
                    }
                }
            }

            // Font Style Selector (with live font family preview)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tipo de fuente (${AppFontStyle.entries.size} estilos)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lazyItems(AppFontStyle.entries, key = { it.id }) { style ->
                            val selected = appearancePrefs.fontStyle == style
                            val family = displayFamilyFor(style)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .border(
                                        width = if (selected) 1.5.dp else 0.8.dp,
                                        color = if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectFontStyle(style) }
                                    .testTag("font_style_chip_${style.id}")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                    Text(
                                        text = "Aa · ${style.displayName}",
                                        fontFamily = family,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = style.description,
                                        fontFamily = family,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Font Size Scale Slider (85%..125%)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.FormatSize, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tamaño de letra", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            text = "${(appearancePrefs.fontSizeScale * 100f).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = primary
                        )
                    }
                    Slider(
                        value = appearancePrefs.fontSizeScale,
                        onValueChange = onFontSizeScaleChange,
                        valueRange = 0.85f..1.25f,
                        colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                        modifier = Modifier.testTag("theme_font_scale_slider")
                    )
                }
            }

            // Custom Wallpaper Selector
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wallpaper, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(extra.wallpaperTitle, style = MaterialTheme.typography.titleSmall)
                        }
                        if (hasWallpaper) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(appearancePrefs.wallpaperUri).build(),
                                contentDescription = "Vista previa de fondo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onPickWallpaper,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_pick_wallpaper_button")
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(extra.chooseWallpaper)
                        }
                        if (hasWallpaper) {
                            OutlinedButton(
                                onClick = onClearWallpaper,
                                modifier = Modifier.testTag("theme_clear_wallpaper_button")
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(extra.clearWallpaper)
                            }
                        }
                    }
                    if (hasWallpaper) {
                        Column {
                            Text(
                                text = "${extra.wallpaperDimLabel}: ${(appearancePrefs.wallpaperDimAlpha * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = appearancePrefs.wallpaperDimAlpha,
                                onValueChange = onWallpaperDimChange,
                                valueRange = 0.2f..0.88f,
                                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                                modifier = Modifier.testTag("theme_wallpaper_dim_slider")
                            )
                            Text(
                                text = "Desenfoque de fondo: ${appearancePrefs.wallpaperBlurDp.roundToInt()} dp",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = appearancePrefs.wallpaperBlurDp,
                                onValueChange = onWallpaperBlurChange,
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(thumbColor = primary, activeTrackColor = primary),
                                modifier = Modifier.testTag("theme_wallpaper_blur_slider")
                            )
                        }
                    }
                }
            }

            // Dark / Light Mode
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (appearancePrefs.isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                            contentDescription = null,
                            tint = primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (appearancePrefs.isDarkTheme) "Modo Oscuro Hi-Fi" else "Modo Claro Marfil",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Alternar luminosidad general",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = appearancePrefs.isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = primary
                        ),
                        modifier = Modifier.testTag("theme_dark_mode_switch")
                    )
                }
            }

            // Material You (Android 12+) + Custom Theme Builder & Restore Defaults
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Colores dinámicos (Material You)", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Adapta la paleta automáticamente a tu sistema Android",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = appearancePrefs.useDynamicMaterialYou,
                            onCheckedChange = onToggleMaterialYou,
                            modifier = Modifier.testTag("theme_material_you_switch")
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenCustomThemeBuilder,
                    modifier = Modifier.weight(1f).testTag("theme_open_custom_builder_btn")
                ) {
                    Icon(Icons.Rounded.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Temas propios / JSON", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onRestoreDefaultAppearance,
                    modifier = Modifier.testTag("theme_restore_defaults_btn")
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar")
                }
            }
            FilledTonalButton(
                onClick = onOpenExtremeVisualEditor,
                modifier = Modifier.fillMaxWidth().testTag("theme_open_extreme_editor_btn")
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Editor Visual Extremo 2.0 (bordes, sombras, barra)")
            }

            Text(
                text = "Temas de color (${AppAccentPalette.entries.size} estilos)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(250.dp)
            ) {
                items(AppAccentPalette.entries, key = { it.id }) { palette ->
                    val selected = palette == appearancePrefs.accentPalette
                    val previewPrimary = if (appearancePrefs.isDarkTheme) palette.primaryDark else palette.primaryLight
                    val previewSecondary = if (appearancePrefs.isDarkTheme) palette.secondaryDark else palette.secondaryLight

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) previewPrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) previewPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectPalette(palette) }
                            .testTag("palette_option_${palette.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(previewPrimary, previewSecondary))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Seleccionado",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = palette.displayName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
