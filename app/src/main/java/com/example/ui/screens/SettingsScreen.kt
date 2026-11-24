package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Tune
import com.example.BuildConfig
import com.example.model.AppFontStyle
import com.example.model.AppLanguage
import com.example.model.AppSettingsPrefs
import com.example.model.UiStyleMode
import com.example.model.UserAppearancePrefs
import com.example.ui.components.UserProfileAvatar
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.extraStringsFor
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.EmeraldSync
import com.example.ui.theme.LocalUiStyleMode
import com.example.ui.theme.displayFamilyFor

@Composable
fun SettingsScreen(
    settings: AppSettingsPrefs,
    appearancePrefs: UserAppearancePrefs = UserAppearancePrefs(),
    isSignedIn: Boolean,
    userDisplayName: String?,
    userEmail: String?,
    userPhotoUrl: String? = null,
    backupStatusMessage: String?,
    isBusyBackup: Boolean,
    onTogglePlayWithOtherApps: (Boolean) -> Unit,
    onUpdateNotificationOptions: (showArt: Boolean, showFav: Boolean, showMode: Boolean) -> Unit,
    onToggleScheduledReminders: (Boolean) -> Unit,
    onSendTestReminder: () -> Unit,
    onToggleChatPushNotifications: (Boolean) -> Unit = {},
    onToggleChatPushOnlyActiveRegion: (Boolean) -> Unit = {},
    onSendTestChatPush: () -> Unit = {},
    onToggleHideShortAudio: (Boolean) -> Unit,
    onToggleAnimations: (Boolean) -> Unit,
    onToggleAutoArtwork: (Boolean) -> Unit,
    onDownloadMissingArtworkNow: () -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectVoiceLanguage: (AppLanguage) -> Unit = {},
    onToggleHandsFreeVoice: (Boolean) -> Unit = {},
    onToggleScreenOffVoice: (Boolean) -> Unit = {},
    onSelectUiStyleMode: (UiStyleMode) -> Unit = {},
    onSelectFontStyle: (AppFontStyle) -> Unit = {},
    onPickChatAvatar: () -> Unit = {},
    onOpenVoiceControl: () -> Unit = {},
    onExportBackupToDrive: () -> Unit,
    onRestoreBackupFromDrive: () -> Unit,
    onOpenCastSheet: () -> Unit,
    onOpenThemeCustomization: () -> Unit,
    onOpenExtremeVisualEditor: () -> Unit = {},
    onOpenPlayerDesigner: () -> Unit = {},
    onOpenHomeCustomization: () -> Unit = {},
    onOpenRecapAndWrapped: () -> Unit = {},
    onOpenMusicalHistory: () -> Unit = {},
    onOpenMusicalProfile: () -> Unit = {},
    onOpenLibraryOrganizer: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenTimeCloud: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val extra = extraStringsFor(settings.appLanguage)
    val isNeon = LocalUiStyleMode.current == UiStyleMode.NEON_MODERN
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    var showSettingsSearch by remember { mutableStateOf(false) }
    var settingsFilterQuery by remember { mutableStateOf("") }
    // Keep Personalización expanded so users can switch UI style back to Original/Minimalista in 1 tap
    var expandedSection by remember { mutableStateOf<String?>("personalization") }
    val activeVoiceLang = remember(settings.voiceLanguageCode, settings.appLanguage) {
        if (settings.voiceLanguageCode.isNotBlank()) AppLanguage.fromCode(settings.voiceLanguageCode) else settings.appLanguage
    }

    val isNotifGranted = remember(context) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val isBatteryUnrestricted = remember(context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = if (isNeon) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (!isNeon) {
                        Text(
                            text = strings.settingsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isNeon) {
                        IconButton(
                            onClick = {
                                showSettingsSearch = !showSettingsSearch
                                if (!showSettingsSearch) settingsFilterQuery = ""
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Buscar en ajustes", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    IconButton(
                        onClick = onOpenThemeCustomization,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = "Personalizar colores", tint = primary)
                    }
                }
            }
            if (isNeon && showSettingsSearch) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = settingsFilterQuery,
                    onValueChange = { settingsFilterQuery = it },
                    placeholder = { Text("Buscar ajuste (ecualizador, voz, tema, drive)…") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Top User Profile Header Card (matches Mockup "Zander / Oyente" + editable Chat avatar)
        item {
            val activeProfileName = settings.customChatUsername.trim().ifBlank { userDisplayName ?: "Oyente" }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .clickable(onClick = onPickChatAvatar)
                    .testTag("settings_profile_header")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    UserProfileAvatar(
                        avatarUri = settings.chatAvatarUri,
                        avatarBase64 = settings.chatAvatarBase64,
                        fallbackPhotoUrl = userPhotoUrl,
                        displayName = activeProfileName,
                        size = 56.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeProfileName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${strings.creditZander} · v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = primary
                        )
                    }
                    FilledTonalButton(onClick = onPickChatAvatar) {
                        Text("Foto")
                    }
                }
            }
        }

        // UI Style, Settings Menu Items
        if (isNeon) {
            val q = settingsFilterQuery.trim().lowercase()
            val matches: (String) -> Boolean = { label -> q.isBlank() || label.lowercase().contains(q) }

            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        if (matches("Reproducción en segundo plano notificaciones audio")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.Layers,
                                iconColor = Color(0xFF0EA5E9),
                                title = "Reproducción en segundo plano",
                                subtitle = "Permitir reproducción sin restricciones",
                                isExpanded = expandedSection == "background",
                                onClick = { expandedSection = if (expandedSection == "background") null else "background" }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Permiso de notificaciones", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = if (isNotifGranted) "Concedido (2º plano activo)" else "Pendiente para multimedia",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isNotifGranted) EmeraldSync else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (!isNotifGranted) onRequestNotificationPermission() else openAppDetailsSettings(context)
                                        },
                                        modifier = Modifier.testTag("settings_bg_permission_btn")
                                    ) {
                                        Text(if (isNotifGranted) "Ajustes" else "Permitir")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                SettingsSwitchRow(
                                    title = strings.playWithOtherAppsTitle,
                                    subtitle = "No pausar con otras apps de audio",
                                    checked = settings.playAlongOtherApps,
                                    onCheckedChange = onTogglePlayWithOtherApps,
                                    testTag = "settings_play_along_switch"
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Desactivar optimización de batería")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.BatteryChargingFull,
                                iconColor = Color(0xFF22C55E),
                                title = "Desactivar optimización de batería",
                                subtitle = if (isBatteryUnrestricted) "Sin restricciones (Activo)" else "Toca para configurar sin restricciones",
                                showChevron = true,
                                onClick = { openBatteryOptimizationSettings(context) },
                                rowModifier = Modifier.testTag("settings_battery_opt_btn")
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Ecualizador y Dolby Atmos sonido 32-bit")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.Equalizer,
                                iconColor = Color(0xFF6366F1),
                                title = "Ecualizador y Dolby Atmos",
                                subtitle = "Motor 32-bit Hi-Res, 5 bandas y Sonido 3D",
                                showChevron = true,
                                onClick = onOpenEqualizer
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Tiempo en la nube sincronización reproducción")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.AccessTimeFilled,
                                iconColor = Color(0xFF3B82F6),
                                title = "Tiempo y Nube",
                                subtitle = "Historial conectado, temporizador y sincronización",
                                showChevron = true,
                                onClick = onOpenTimeCloud
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Centro de diagnóstico errores reparar analizar salud memoria")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.HealthAndSafety,
                                iconColor = EmeraldSync,
                                title = "Centro de diagnóstico de Ritmo Local",
                                subtitle = "Analizar app, reparar fallos, logs técnicos y salud",
                                showChevron = true,
                                onClick = onOpenDiagnostics,
                                rowModifier = Modifier.testTag("settings_open_diagnostics_btn")
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Personalización tema interfaz original minimalista fondo")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.AutoAwesomeMosaic,
                                iconColor = primary,
                                title = "Personalización",
                                subtitle = "Diseño (${appearancePrefs.uiStyleMode.displayName}), temas y fondo de pantalla",
                                isExpanded = expandedSection == "personalization",
                                onClick = { expandedSection = if (expandedSection == "personalization") null else "personalization" }
                            ) {
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
                                            modifier = Modifier.weight(1f).testTag("settings_ui_style_${mode.id}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tipo de fuente",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AppFontStyle.entries, key = { "neon_font_${it.id}" }) { style ->
                                        val selected = appearancePrefs.fontStyle == style
                                        val family = displayFamilyFor(style)
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onSelectFontStyle(style) },
                                            label = {
                                                Text(
                                                    text = "Aa · ${style.displayName.substringBefore(" (")}",
                                                    fontFamily = family
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = primary.copy(alpha = 0.24f),
                                                selectedLabelColor = primary
                                            ),
                                            modifier = Modifier.testTag("settings_font_chip_${style.id}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = onOpenThemeCustomization,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_wallpaper_themes_btn")
                                ) {
                                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (!appearancePrefs.wallpaperUri.isNullOrBlank()) "Temas y fondo personalizado (Activo)"
                                        else "Cambiar temas, fuentes y fondo"
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = onOpenExtremeVisualEditor,
                                        modifier = Modifier.weight(1f).testTag("settings_extreme_editor_btn")
                                    ) {
                                        Text("Editor 2.0", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = onOpenPlayerDesigner,
                                        modifier = Modifier.weight(1f).testTag("settings_player_designer_btn")
                                    ) {
                                        Text("Reproductor", maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = onOpenHomeCustomization,
                                        modifier = Modifier.weight(1f).testTag("settings_home_custom_btn")
                                    ) {
                                        Text("Inicio", maxLines = 1)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Ritmo Local 2.0 Recap Wrapped Historia Perfil Logros Organizador")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.AutoGraph,
                                iconColor = secondary,
                                title = "Ritmo 2.0 · Recap, Historia y Biblioteca",
                                subtitle = "Recap/Wrapped, Perfil y Logros, Mi Historia y Organizador",
                                isExpanded = expandedSection == "ritmo2",
                                onClick = { expandedSection = if (expandedSection == "ritmo2") null else "ritmo2" }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onOpenRecapAndWrapped,
                                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                                        modifier = Modifier.weight(1f).testTag("settings_open_recap_btn")
                                    ) {
                                        Icon(Icons.Rounded.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Recap / Wrapped", maxLines = 1)
                                    }
                                    FilledTonalButton(
                                        onClick = onOpenMusicalHistory,
                                        modifier = Modifier.weight(1f).testTag("settings_open_history_btn")
                                    ) {
                                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mi Historia", maxLines = 1)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = onOpenMusicalProfile,
                                        modifier = Modifier.weight(1f).testTag("settings_open_profile_btn")
                                    ) {
                                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Perfil y Logros", maxLines = 1)
                                    }
                                    FilledTonalButton(
                                        onClick = onOpenLibraryOrganizer,
                                        modifier = Modifier.weight(1f).testTag("settings_open_organizer_btn")
                                    ) {
                                        Icon(Icons.Rounded.LibraryAddCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Organizador", maxLines = 1)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Control por voz sin internet ridmo pantalla apagada")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.Mic,
                                iconColor = Color(0xFF8B5CF6),
                                title = "Control por voz (Sin internet)",
                                subtitle = "Di \"Ridmo\" incluso con pantalla apagada (${activeVoiceLang.flagEmoji})",
                                isExpanded = expandedSection == "voice",
                                onClick = { expandedSection = if (expandedSection == "voice") null else "voice" }
                            ) {
                                SettingsSwitchRow(
                                    title = extra.handsFreeWakeTitle,
                                    subtitle = extra.handsFreeWakeSubtitle,
                                    checked = settings.handsFreeVoiceEnabled,
                                    onCheckedChange = onToggleHandsFreeVoice,
                                    testTag = "settings_hands_free_voice_switch"
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                SettingsSwitchRow(
                                    title = extra.screenOffVoiceTitle,
                                    subtitle = extra.screenOffVoiceSubtitle,
                                    checked = settings.screenOffVoiceEnabled,
                                    onCheckedChange = onToggleScreenOffVoice,
                                    testTag = "settings_screen_off_voice_switch"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AppLanguage.entries, key = { "voice_${it.code}" }) { lang ->
                                        val selected = lang == activeVoiceLang
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onSelectVoiceLanguage(lang) },
                                            label = { Text("${lang.flagEmoji} ${lang.displayName}") },
                                            modifier = Modifier.testTag("settings_voice_lang_${lang.code}")
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onOpenVoiceControl,
                                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                                    modifier = Modifier.fillMaxWidth().testTag("settings_open_voice_btn")
                                ) {
                                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abrir control por voz (${activeVoiceLang.flagEmoji})", fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Idioma Language español english")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.Language,
                                iconColor = Color(0xFF06B6D4),
                                title = "Idioma / Language",
                                subtitle = "(${settings.appLanguage.displayName})",
                                isExpanded = expandedSection == "language",
                                onClick = { expandedSection = if (expandedSection == "language") null else "language" }
                            ) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(AppLanguage.entries, key = { it.code }) { lang ->
                                        FilterChip(
                                            selected = lang == settings.appLanguage,
                                            onClick = { onSelectLanguage(lang) },
                                            label = { Text("${lang.flagEmoji} ${lang.displayName}") },
                                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Notificaciones carátula horario")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.NotificationsActive,
                                iconColor = Color(0xFFF59E0B),
                                title = "Notificaciones",
                                subtitle = "Controles multimedia y saludos por horario",
                                isExpanded = expandedSection == "notifications",
                                onClick = { expandedSection = if (expandedSection == "notifications") null else "notifications" }
                            ) {
                                SettingsSwitchRow(
                                    title = "Mostrar carátula de la canción",
                                    subtitle = "Portada en la notificación multimedia",
                                    checked = settings.notifShowArtwork,
                                    onCheckedChange = { onUpdateNotificationOptions(it, settings.notifShowFavorite, settings.notifShowModeToggle) },
                                    testTag = "settings_notif_artwork_switch"
                                )
                                SettingsSwitchRow(
                                    title = "Botón Agregar a favoritas",
                                    subtitle = "Corazón directo en la barra",
                                    checked = settings.notifShowFavorite,
                                    onCheckedChange = { onUpdateNotificationOptions(settings.notifShowArtwork, it, settings.notifShowModeToggle) },
                                    testTag = "settings_notif_fav_switch"
                                )
                                SettingsSwitchRow(
                                    title = "Botón Aleatorio / Repetir",
                                    subtitle = "Alterna modo de reproducción",
                                    checked = settings.notifShowModeToggle,
                                    onCheckedChange = { onUpdateNotificationOptions(settings.notifShowArtwork, settings.notifShowFavorite, it) },
                                    testTag = "settings_notif_mode_switch"
                                )
                                SettingsSwitchRow(
                                    title = strings.scheduledMixNotifTitle,
                                    subtitle = "Saludo en Mañana, Tarde y Noche",
                                    checked = settings.scheduledMixRemindersEnabled,
                                    onCheckedChange = onToggleScheduledReminders,
                                    testTag = "settings_mix_reminders_switch"
                                )
                                SettingsSwitchRow(
                                    title = "Alertas push del Chat Global (FCM)",
                                    subtitle = if (settings.chatPushNotificationsEnabled) {
                                        if (settings.chatPushOnlyActiveRegion) "Activas para sala: ${settings.preferredChatRegion.displayName}"
                                        else "Activas para todas las salas regionales"
                                    } else "Silenciadas por el usuario",
                                    checked = settings.chatPushNotificationsEnabled,
                                    onCheckedChange = onToggleChatPushNotifications,
                                    testTag = "settings_chat_push_switch"
                                )
                                if (settings.chatPushNotificationsEnabled) {
                                    SettingsSwitchRow(
                                        title = "Solo mi sala regional activa",
                                        subtitle = "Recibir push únicamente de ${settings.preferredChatRegion.displayName}",
                                        checked = settings.chatPushOnlyActiveRegion,
                                        onCheckedChange = onToggleChatPushOnlyActiveRegion,
                                        testTag = "settings_chat_push_only_region_switch"
                                    )
                                }
                                OutlinedButton(
                                    onClick = onSendTestReminder,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_test_reminder_btn")
                                ) {
                                    Icon(Icons.Rounded.NotificationsPaused, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar notificación de horario ahora")
                                }
                                FilledTonalButton(
                                    onClick = onSendTestChatPush,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_test_chat_push_btn")
                                ) {
                                    Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Probar alerta push del Chat Global")
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Copia de seguridad Google Drive TV Cast")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.CloudUpload,
                                iconColor = Color(0xFF10B981),
                                title = "Copia de seguridad",
                                subtitle = "(Google Drive) · Smart TV Cast",
                                isExpanded = expandedSection == "backup",
                                onClick = { expandedSection = if (expandedSection == "backup") null else "backup" }
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onExportBackupToDrive,
                                        enabled = !isBusyBackup,
                                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                                        modifier = Modifier.weight(1f).testTag("settings_backup_export_btn")
                                    ) {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Subir a Drive", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = onRestoreBackupFromDrive,
                                        enabled = !isBusyBackup,
                                        modifier = Modifier.weight(1f).testTag("settings_backup_restore_btn")
                                    ) {
                                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restaurar")
                                    }
                                }
                                backupStatusMessage?.let { msg ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = EmeraldSync)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                FilledTonalButton(
                                    onClick = onOpenCastSheet,
                                    modifier = Modifier.fillMaxWidth().testTag("settings_cast_tv_btn")
                                ) {
                                    Icon(Icons.Rounded.Cast, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.castToTv)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Compartir música Biblioteca filtros animaciones carátulas")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.FilterAlt,
                                iconColor = secondary,
                                title = "Biblioteca y Carátulas",
                                subtitle = "Filtros WhatsApp, portadas y animaciones",
                                isExpanded = expandedSection == "library",
                                onClick = { expandedSection = if (expandedSection == "library") null else "library" }
                            ) {
                                SettingsSwitchRow(
                                    title = strings.hideShortAudioTitle,
                                    subtitle = "Oculta notas < 30s",
                                    checked = settings.hideShortWhatsAppAudio,
                                    onCheckedChange = onToggleHideShortAudio,
                                    testTag = "settings_hide_whatsapp_switch"
                                )
                                SettingsSwitchRow(
                                    title = strings.autoArtworkTitle,
                                    subtitle = "Descarga portadas faltantes",
                                    checked = settings.autoDownloadArtwork,
                                    onCheckedChange = onToggleAutoArtwork,
                                    testTag = "settings_auto_art_switch"
                                )
                                if (settings.autoDownloadArtwork) {
                                    FilledTonalButton(onClick = onDownloadMissingArtworkNow, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Rounded.ImageSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Buscar carátulas faltantes ahora")
                                    }
                                }
                                SettingsSwitchRow(
                                    title = strings.animationsTitle,
                                    subtitle = "Efectos visuales y transiciones",
                                    checked = settings.animationsEnabled,
                                    onCheckedChange = onToggleAnimations,
                                    testTag = "settings_animations_switch"
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        }

                        if (matches("Cuenta Google offline sesión")) {
                            NeonSettingsRow(
                                icon = Icons.Rounded.Settings,
                                iconColor = Color(0xFF64748B),
                                title = "Cuenta y Modo Offline",
                                subtitle = if (isSignedIn) "${userDisplayName ?: "Usuario"} (${userEmail ?: "Google"})" else "Modo Offline activo (opcional)",
                                isExpanded = expandedSection == "account",
                                onClick = { expandedSection = if (expandedSection == "account") null else "account" }
                            ) {
                                if (isSignedIn) {
                                    OutlinedButton(onClick = onSignOutClick, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.signOut)
                                    }
                                } else {
                                    Button(
                                        onClick = onSignInClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                                        modifier = Modifier.fillMaxWidth().testTag("settings_sign_in_btn")
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.signInGoogle, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Classic Original / Minimalist Cards
            item {
                SettingsCard(
                    icon = Icons.Rounded.AutoAwesomeMosaic,
                    title = "${extra.uiStyleTitle} y ${extra.wallpaperTitle}",
                    subtitle = "Elige Moderno Neón (como el rediseño), Minimalista o vuelve a la interfaz Original en cualquier momento."
                ) {
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
                                .testTag("settings_ui_style_${mode.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TextFields, contentDescription = null, tint = primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tipo de fuente", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppFontStyle.entries, key = { "classic_font_${it.id}" }) { style ->
                        val selected = appearancePrefs.fontStyle == style
                        val family = displayFamilyFor(style)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFontStyle(style) },
                            label = {
                                Text(
                                    text = "Aa · ${style.displayName.substringBefore(" (")}",
                                    fontFamily = family
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primary.copy(alpha = 0.24f),
                                selectedLabelColor = primary
                            ),
                            modifier = Modifier.testTag("settings_font_chip_${style.id}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onOpenThemeCustomization,
                    modifier = Modifier.fillMaxWidth().testTag("settings_wallpaper_themes_btn")
                ) {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (!appearancePrefs.wallpaperUri.isNullOrBlank()) {
                            "Temas y fondo personalizado (Activo)"
                        } else {
                            "Cambiar temas, fuentes y fondo"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenExtremeVisualEditor,
                        modifier = Modifier.weight(1f).testTag("settings_extreme_editor_btn")
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editor 2.0", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenPlayerDesigner,
                        modifier = Modifier.weight(1f).testTag("settings_player_designer_btn")
                    ) {
                        Text("Reproductor", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onOpenHomeCustomization,
                        modifier = Modifier.weight(1f).testTag("settings_home_custom_btn")
                    ) {
                        Icon(Icons.Rounded.DashboardCustomize, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inicio", maxLines = 1)
                    }
                }
            }
        }

        // Ritmo Local 2.0 Experience Card (Classic / Minimalist)
        item {
            SettingsCard(
                icon = Icons.Rounded.AutoGraph,
                title = "Ritmo Local 2.0 · Recap, Historia y Biblioteca",
                subtitle = "Recap mensual, Wrapped anual, Mi Historia, Logros y Organizador Inteligente."
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenRecapAndWrapped,
                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier.weight(1f).testTag("settings_open_recap_btn")
                    ) {
                        Icon(Icons.Rounded.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recap / Wrapped", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onOpenMusicalHistory,
                        modifier = Modifier.weight(1f).testTag("settings_open_history_btn")
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mi Historia", maxLines = 1)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onOpenMusicalProfile,
                        modifier = Modifier.weight(1f).testTag("settings_open_profile_btn")
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Perfil y Logros", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onOpenLibraryOrganizer,
                        modifier = Modifier.weight(1f).testTag("settings_open_organizer_btn")
                    ) {
                        Icon(Icons.Rounded.LibraryAddCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Organizador", maxLines = 1)
                    }
                }
            }
        }

        // Offline Multi-Language Voice Control ("Ritmo ...")
        item {
            val activeVoiceLang = remember(settings.voiceLanguageCode, settings.appLanguage) {
                if (settings.voiceLanguageCode.isNotBlank()) AppLanguage.fromCode(settings.voiceLanguageCode) else settings.appLanguage
            }
            SettingsCard(
                icon = Icons.Rounded.Mic,
                title = extra.voiceControlTitle,
                subtitle = "Di \"Ridmo siguiente canción\", \"Ritmo ver letra\" o \"Ritmo reproducir [canción] de [artista]\" sin internet."
            ) {
                SettingsSwitchRow(
                    title = extra.handsFreeWakeTitle,
                    subtitle = extra.handsFreeWakeSubtitle,
                    checked = settings.handsFreeVoiceEnabled,
                    onCheckedChange = onToggleHandsFreeVoice,
                    testTag = "settings_hands_free_voice_switch"
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSwitchRow(
                    title = extra.screenOffVoiceTitle,
                    subtitle = extra.screenOffVoiceSubtitle,
                    checked = settings.screenOffVoiceEnabled,
                    onCheckedChange = onToggleScreenOffVoice,
                    testTag = "settings_screen_off_voice_switch"
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppLanguage.entries, key = { "voice_${it.code}" }) { lang ->
                        val selected = lang == activeVoiceLang
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectVoiceLanguage(lang) },
                            label = { Text("${lang.flagEmoji} ${lang.displayName}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primary.copy(alpha = 0.22f),
                                selectedLabelColor = primary
                            ),
                            modifier = Modifier.testTag("settings_voice_lang_${lang.code}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenVoiceControl,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("settings_open_voice_btn")
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir control por voz (${activeVoiceLang.flagEmoji})", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Background playback & Battery optimization & Play with other apps
        item {
            SettingsCard(
                icon = Icons.Rounded.Layers,
                title = strings.backgroundPlaybackTitle,
                subtitle = "Permisos de notificación, segundo plano, audio simultáneo y batería."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permiso de notificaciones", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (isNotifGranted) "Concedido (controles activos en 2º plano)" else "Pendiente para controles multimedia",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isNotifGranted) EmeraldSync else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            if (!isNotifGranted) {
                                onRequestNotificationPermission()
                            } else {
                                openAppDetailsSettings(context)
                            }
                        },
                        modifier = Modifier.testTag("settings_bg_permission_btn")
                    ) {
                        Text(if (isNotifGranted) "Ajustes" else "Permitir")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                SettingsSwitchRow(
                    title = strings.playWithOtherAppsTitle,
                    subtitle = "No pausar RitmoLocal cuando otras apps o videos reproducen audio",
                    checked = settings.playAlongOtherApps,
                    onCheckedChange = onTogglePlayWithOtherApps,
                    testTag = "settings_play_along_switch"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.batteryOptTitle, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            text = if (isBatteryUnrestricted) {
                                "Sin restricciones (recomendado para no cerrarse)"
                            } else {
                                "Optimizada por el sistema — toca para configurar en 'Sin restricciones'"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isBatteryUnrestricted) EmeraldSync else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { openBatteryOptimizationSettings(context) },
                        modifier = Modifier.testTag("settings_battery_opt_btn")
                    ) {
                        Text("Configurar")
                    }
                }
            }
        }

        // Player Notification Customization & Scheduled Time-of-Day Reminders
        item {
            SettingsCard(
                icon = Icons.Rounded.NotificationsActive,
                title = strings.notifConfigTitle,
                subtitle = "Elige qué mostrar en la barra de notificación multimedia."
            ) {
                SettingsSwitchRow(
                    title = "Mostrar carátula de la canción",
                    subtitle = "Incluye la portada local, personalizada o descargada en la notificación",
                    checked = settings.notifShowArtwork,
                    onCheckedChange = {
                        onUpdateNotificationOptions(it, settings.notifShowFavorite, settings.notifShowModeToggle)
                    },
                    testTag = "settings_notif_artwork_switch"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSwitchRow(
                    title = "Botón Agregar a favoritas",
                    subtitle = "Marca o desmarca favoritas directo desde la notificación",
                    checked = settings.notifShowFavorite,
                    onCheckedChange = {
                        onUpdateNotificationOptions(settings.notifShowArtwork, it, settings.notifShowModeToggle)
                    },
                    testTag = "settings_notif_fav_switch"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSwitchRow(
                    title = "Botón Aleatorio / Repetir / Continuo",
                    subtitle = "Toca en la notificación para alternar entre aleatorio, continua y repetir",
                    checked = settings.notifShowModeToggle,
                    onCheckedChange = {
                        onUpdateNotificationOptions(settings.notifShowArtwork, settings.notifShowFavorite, it)
                    },
                    testTag = "settings_notif_mode_switch"
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsSwitchRow(
                    title = strings.scheduledMixNotifTitle,
                    subtitle = "Te saluda en Mañana, Tarde y Noche para abrir tu playlist del horario",
                    checked = settings.scheduledMixRemindersEnabled,
                    onCheckedChange = onToggleScheduledReminders,
                    testTag = "settings_mix_reminders_switch"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSwitchRow(
                    title = "Alertas push del Chat Global (FCM)",
                    subtitle = if (settings.chatPushNotificationsEnabled) {
                        if (settings.chatPushOnlyActiveRegion) "Activas para tu sala (${settings.preferredChatRegion.displayName})"
                        else "Activas para todas las salas del chat"
                    } else "Desactivadas — no recibirás avisos de nuevos mensajes",
                    checked = settings.chatPushNotificationsEnabled,
                    onCheckedChange = onToggleChatPushNotifications,
                    testTag = "settings_chat_push_switch"
                )
                if (settings.chatPushNotificationsEnabled) {
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingsSwitchRow(
                        title = "Solo mi sala regional activa",
                        subtitle = "Filtrar notificaciones push exclusivamente a ${settings.preferredChatRegion.displayName}",
                        checked = settings.chatPushOnlyActiveRegion,
                        onCheckedChange = onToggleChatPushOnlyActiveRegion,
                        testTag = "settings_chat_push_only_region_switch"
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSendTestReminder,
                    modifier = Modifier.fillMaxWidth().testTag("settings_test_reminder_btn")
                ) {
                    Icon(Icons.Rounded.NotificationsPaused, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar notificación de horario ahora")
                }
                Spacer(modifier = Modifier.height(6.dp))
                FilledTonalButton(
                    onClick = onSendTestChatPush,
                    modifier = Modifier.fillMaxWidth().testTag("settings_test_chat_push_btn")
                ) {
                    Icon(Icons.Rounded.Forum, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar alerta push del Chat Global")
                }
            }
        }

        // Library & Cover Art & Performance Animations
        item {
            SettingsCard(
                icon = Icons.Rounded.FilterAlt,
                title = "Biblioteca, Carátulas y Rendimiento",
                subtitle = "Filtros de audio, descarga de portadas y fluidez visual."
            ) {
                SettingsSwitchRow(
                    title = strings.hideShortAudioTitle,
                    subtitle = "Oculta notas de voz de WhatsApp y grabaciones menores a 30s",
                    checked = settings.hideShortWhatsAppAudio,
                    onCheckedChange = onToggleHideShortAudio,
                    testTag = "settings_hide_whatsapp_switch"
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSwitchRow(
                    title = strings.autoArtworkTitle,
                    subtitle = "Descarga carátulas faltantes por internet cuando está activado",
                    checked = settings.autoDownloadArtwork,
                    onCheckedChange = onToggleAutoArtwork,
                    testTag = "settings_auto_art_switch"
                )
                if (settings.autoDownloadArtwork) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FilledTonalButton(
                        onClick = onDownloadMissingArtworkNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.ImageSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buscar carátulas faltantes ahora")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSwitchRow(
                    title = strings.animationsTitle,
                    subtitle = "Desactívalas si tu teléfono va lento para ahorrar batería y RAM",
                    checked = settings.animationsEnabled,
                    onCheckedChange = onToggleAnimations,
                    testTag = "settings_animations_switch"
                )
            }
        }

        // Centro de diagnóstico de Ritmo Local (Classic mode)
        item {
            SettingsCard(
                icon = Icons.Rounded.HealthAndSafety,
                title = "Centro de diagnóstico de Ritmo Local",
                subtitle = "Supervisa reproductor, servicio, voz, ecualizador, permisos, nube y memoria con reparación segura."
            ) {
                Button(
                    onClick = onOpenDiagnostics,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSync,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("settings_open_diagnostics_btn")
                ) {
                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir Centro de diagnóstico", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Multi-language Selector
        item {
            SettingsCard(
                icon = Icons.Rounded.Language,
                title = strings.languageTitle,
                subtitle = "Cambia el idioma de menús, botones y recordatorios."
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AppLanguage.entries, key = { it.code }) { lang ->
                        val selected = lang == settings.appLanguage
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text("${lang.flagEmoji} ${lang.displayName}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primary.copy(alpha = 0.22f),
                                selectedLabelColor = primary
                            ),
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }
            }
        }

        // Google Drive Backup & Restore + Smart TV Cast + Multi-Window
        item {
            SettingsCard(
                icon = Icons.Rounded.CloudUpload,
                title = strings.driveBackupTitle,
                subtitle = "Guarda o restaura un archivo .zip con tu música, carátulas y playlists directo en Google Drive."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportBackupToDrive,
                        enabled = !isBusyBackup,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f).testTag("settings_backup_export_btn")
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subir a Drive", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onRestoreBackupFromDrive,
                        enabled = !isBusyBackup,
                        modifier = Modifier.weight(1f).testTag("settings_backup_restore_btn")
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar")
                    }
                }
                backupStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = msg, style = MaterialTheme.typography.bodySmall, color = EmeraldSync)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenCastSheet,
                        modifier = Modifier.weight(1f).testTag("settings_cast_tv_btn")
                    ) {
                        Icon(Icons.Rounded.Cast, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.castToTv)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Splitscreen, contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${strings.splitScreenTitle}: habilitado (usa Recientes -> Dividir o Ventana flotante).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Account status (Sign in is optional!)
        item {
            SettingsCard(
                icon = Icons.Rounded.Settings,
                title = "Cuenta y Modo Offline",
                subtitle = if (isSignedIn) {
                    "${userDisplayName ?: "Usuario"} (${userEmail ?: "Google"})"
                } else {
                    "Modo Offline activo: no es obligatorio iniciar sesión."
                }
            ) {
                if (isSignedIn) {
                    OutlinedButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.signOut)
                    }
                } else {
                    Button(
                        onClick = onSignInClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("settings_sign_in_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.signInGoogle, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        } // end else classic Settings cards

        // Bottom Credit to Zander + Version Beta Badge (Mockup Panel 15 inspired in Neon)
        item {
            if (isNeon) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ritmo Local",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Gracias por usar nuestra app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "- ${strings.creditZander} -",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("credit_zander_text")
                    )
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = CoralPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = primary.copy(alpha = 0.16f),
                        modifier = Modifier.border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "v${BuildConfig.VERSION_NAME} Beta",
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("version_beta_badge")
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = strings.creditZander,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("credit_zander_text")
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = primary.copy(alpha = 0.16f),
                            modifier = Modifier.border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(50))
                        ) {
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} Beta",
                                style = MaterialTheme.typography.labelSmall,
                                color = primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .testTag("version_beta_badge")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NeonSettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isExpanded: Boolean = false,
    showChevron: Boolean = false,
    onClick: () -> Unit,
    rowModifier: Modifier = Modifier,
    expandedContent: (@Composable () -> Unit)? = null
) {
    Column(modifier = rowModifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expandedContent != null || showChevron) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (isExpanded && expandedContent != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                expandedContent()
            }
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val isMinimal = LocalUiStyleMode.current == UiStyleMode.MINIMALIST
    val primary = MaterialTheme.colorScheme.primary
    val cardShape = RoundedCornerShape(if (isMinimal) 18.dp else 22.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMinimal) Modifier.border(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), cardShape) else Modifier
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(primary.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun openAppDetailsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppDetailsSettings(context)
    }
}
