package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.stats.AchievementCategory
import com.example.data.stats.MusicalAchievement
import com.example.data.stats.RealTimeStatsSummary
import com.example.data.stats.RitmoRecapReport
import com.example.model.LocalTrack
import com.example.model.MusicalProfileConfig
import com.example.model.formatPlaybackTimeReadable
import com.example.ui.components.AlbumArtwork

@Composable
fun MusicalProfileScreen(
    profileConfig: MusicalProfileConfig,
    achievements: List<MusicalAchievement>,
    recapReport: RitmoRecapReport,
    realTimeSummary: RealTimeStatsSummary,
    favoriteTracks: List<LocalTrack>,
    onSaveProfileConfig: (MusicalProfileConfig) -> Unit,
    onOpenRecap: () -> Unit,
    onPlayTrack: (LocalTrack) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bannerStart = Color(profileConfig.bannerPrimaryArgb)
    val bannerEnd = Color(profileConfig.bannerSecondaryArgb)

    val filteredAchievements = remember(achievements, selectedCategory) {
        if (selectedCategory == null) achievements else achievements.filter { it.category == selectedCategory }
    }
    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
                Text(
                    text = "Perfil Musical Personal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("profile_edit_button")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar perfil", tint = primary)
                }
            }
        }

        // Profile Banner Card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(bannerStart.copy(alpha = 0.45f), bannerEnd.copy(alpha = 0.32f))))
                    .border(1.dp, bannerStart.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.35f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Text(
                                    text = if (profileConfig.privateByDefault) "Privado por defecto" else "Respaldo opcional activo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                        Text(
                            text = "$unlockedCount/${achievements.size} logros",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = profileConfig.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = profileConfig.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileStatPill("Tiempo total", formatPlaybackTimeReadable(realTimeSummary.allTimeSeconds), Modifier.weight(1f))
                        ProfileStatPill("Reprod.", realTimeSummary.allTimePlays.toString(), Modifier.weight(1f))
                        ProfileStatPill("Favoritas", favoriteTracks.size.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        if (profileConfig.showRecapOnProfile) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resumen destacado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = onOpenRecap, modifier = Modifier.testTag("profile_open_recap_btn")) {
                                Text("Ver Recap completo")
                            }
                        }
                        if (!recapReport.hasEnoughData) {
                            Text(
                                text = "Escucha tu música local para llenar tu tarjeta destacada del mes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Artista top: ${recapReport.topArtist?.artistName ?: "—"} · Canción top: ${recapReport.topSong?.trackTitle ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val genres = recapReport.topGenres.take(3).joinToString(" · ") { it.genre }
                            if (genres.isNotBlank()) {
                                Text("Géneros: $genres", style = MaterialTheme.typography.labelMedium, color = secondary)
                            }
                        }
                    }
                }
            }
        }

        if (favoriteTracks.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
                    Text("Tus Favoritas destacadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            items(favoriteTracks.take(5), key = { "prof_fav_${it.trackKey}" }) { track ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { onPlayTrack(track) }
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AlbumArtwork(
                            albumArtUri = track.albumArtUri,
                            contentDescription = track.title,
                            modifier = Modifier.size(40.dp),
                            cornerRadius = 10.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir ${track.title}", tint = primary)
                    }
                }
            }
        }

        if (profileConfig.showAchievementsOnProfile) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                    Text("Logros Musicales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("Todos") }
                        )
                    }
                    items(AchievementCategory.entries, key = { it.name }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName) },
                            modifier = Modifier.testTag("achievement_cat_${cat.name.lowercase()}")
                        )
                    }
                }
            }

            items(filteredAchievements, key = { it.id }) { achievement ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (achievement.isUnlocked) primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (achievement.isUnlocked) primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            RoundedCornerShape(18.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(achievement.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (achievement.isUnlocked) primary else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = if (achievement.isUnlocked) "Desbloqueado" else "${achievement.currentValue}/${achievement.targetValue}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (achievement.isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        LinearProgressIndicator(
                            progress = { achievement.progressFraction },
                            color = if (achievement.isUnlocked) primary else secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditMusicalProfileDialog(
            initial = profileConfig,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                onSaveProfileConfig(updated)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ProfileStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.28f), modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.78f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun EditMusicalProfileDialog(
    initial: MusicalProfileConfig,
    onDismiss: () -> Unit,
    onSave: (MusicalProfileConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial.displayName) }
    var bio by remember { mutableStateOf(initial.bio) }
    var showRecap by remember { mutableStateOf(initial.showRecapOnProfile) }
    var showAchievements by remember { mutableStateOf(initial.showAchievementsOnProfile) }
    var selectedPrimary by remember { mutableLongStateOf(initial.bannerPrimaryArgb) }
    var selectedSecondary by remember { mutableLongStateOf(initial.bannerSecondaryArgb) }

    val presets = listOf(
        0xFF9D6BFFL to 0xFFD946EFL,
        0xFF06B6D4L to 0xFF3B82F6L,
        0xFFF59E0BL to 0xFFEF4444L,
        0xFF10B981L to 0xFF14B8A6L
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Perfil Musical") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Nombre visible") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(140) },
                    label = { Text("Descripción musical") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Color de cabecera", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presets.forEach { (p, s) ->
                        val active = p == selectedPrimary
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(p), Color(s))))
                                .border(if (active) 2.5.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable {
                                    selectedPrimary = p
                                    selectedSecondary = s
                                }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar Recap en perfil", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = showRecap, onCheckedChange = { showRecap = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar logros", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = showAchievements, onCheckedChange = { showAchievements = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            displayName = name.trim().ifBlank { "Oyente Ritmo Local" },
                            bio = bio.trim(),
                            bannerPrimaryArgb = selectedPrimary,
                            bannerSecondaryArgb = selectedSecondary,
                            showRecapOnProfile = showRecap,
                            showAchievementsOnProfile = showAchievements
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
