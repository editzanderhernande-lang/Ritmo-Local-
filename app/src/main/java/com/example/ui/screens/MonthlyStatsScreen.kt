package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.UiState
import com.example.model.formatMonthLabel
import com.example.model.formatPlaybackTimeReadable
import com.example.ui.components.AlbumArtwork
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.NightAccent
import com.example.ui.viewmodel.MonthlyReportData

private enum class StatsCategory(val label: String) {
    SONGS("Canciones"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes")
}

@Composable
fun MonthlyStatsScreen(
    reportState: UiState<MonthlyReportData>,
    onSelectMonth: (String) -> Unit,
    isSignedIn: Boolean = true,
    onRequestSignIn: () -> Unit = {},
    onOpenRecapAndWrapped: () -> Unit = {},
    onOpenMusicalHistory: () -> Unit = {},
    onOpenMusicalProfile: () -> Unit = {},
    localeTag: String = "es-ES",
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val isNeon = com.example.ui.theme.LocalUiStyleMode.current == com.example.model.UiStyleMode.NEON_MODERN
    var selectedCategory by remember { mutableStateOf(StatsCategory.SONGS) }
    var showAnnual by remember { mutableStateOf(false) }
    var showFullReport by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    when (reportState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primary)
            }
        }
        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(text = reportState.message, style = MaterialTheme.typography.bodyMedium, color = CoralPulse)
            }
        }
        is UiState.Success -> {
            val data = reportState.data
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isNeon) {
                    val summary = data.summary
                    val totalMonthSeconds = summary?.totalSeconds ?: data.topSongs.sumOf { it.totalSeconds }
                    val totalMonthPlays = summary?.totalPlays ?: data.topSongs.sumOf { it.playCount }
                    val displayHours = (totalMonthSeconds / 3600L).toInt()
                    val displayMinutes = ((totalMonthSeconds % 3600L) / 60L).toInt()
                    val hoursText = if (displayHours > 0) "$displayHours h" else "$displayMinutes m"
                    val songCountText = if (data.topSongs.isNotEmpty()) "${data.topSongs.size}" else "$totalMonthPlays"

                    item {
                        Text(
                            text = "Estadísticas",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    item {
                        Ritmo2StatsQuickRow(
                            primary = primary,
                            secondary = secondary,
                            onOpenRecapAndWrapped = onOpenRecapAndWrapped,
                            onOpenMusicalHistory = onOpenMusicalHistory,
                            onOpenMusicalProfile = onOpenMusicalProfile
                        )
                    }

                    item {
                        // Mockup Panel 8: [ Mensual | Anual ] pill toggle
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(50))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                listOf(false to "Mensual", true to "Anual").forEach { (isYear, label) ->
                                    val selected = showAnnual == isYear
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selected) primary else Color.Transparent,
                                        onClick = { showAnnual = isYear },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (showAnnual) {
                                "Tu informe anual (${data.availableMonths.size} mes(es))"
                            } else {
                                "Tu informe de ${formatMonthLabel(data.selectedMonthKey, localeTag)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    item {
                        // Mockup Panel 8: Two big side-by-side KPI cards with circular purple/blue badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.BarChart, contentDescription = null, tint = primary)
                                    }
                                    Column {
                                        Text(
                                            text = hoursText,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Tiempo total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, secondary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(secondary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = secondary)
                                    }
                                    Column {
                                        Text(
                                            text = songCountText,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Canciones",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Más escuchados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    val topPreview = data.topSongs.take(if (showFullReport) 25 else 3)
                    if (topPreview.isEmpty()) {
                        item { EmptyStatsCard() }
                    } else {
                        val maxSec = topPreview.maxOf { it.totalSeconds }.coerceAtLeast(1L)
                        itemsIndexed(topPreview, key = { _, it -> "neon_top_${it.trackKey}" }) { index, song ->
                            RankedStatCard(
                                rank = index + 1,
                                title = song.trackTitle,
                                subtitle = "${song.artistName} · ${song.playCount} reprod.",
                                durationText = formatPlaybackTimeReadable(song.totalSeconds),
                                fraction = song.totalSeconds.toFloat() / maxSec.toFloat(),
                                accent = primary,
                                leadingArtworkUri = if (song.albumId > 0L) {
                                    "content://media/external/audio/albumart/${song.albumId}"
                                } else null
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = { showFullReport = !showFullReport },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                text = if (showFullReport) "Ocultar informe completo" else "Ver informe completo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (!isSignedIn) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Modo Offline activo", style = MaterialTheme.typography.titleSmall, color = primary)
                                    Text(
                                        text = "Tu tiempo local se guarda en el dispositivo. Inicia sesión opcionalmente para respaldar estadísticas entre dispositivos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onRequestSignIn,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
                                        modifier = Modifier.testTag("monthly_stats_sign_in_button")
                                    ) {
                                        Text(strings.signInGoogle, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (showFullReport) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(data.availableMonths, key = { it }) { monthKey ->
                                    val selected = monthKey == data.selectedMonthKey
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectMonth(monthKey) },
                                        label = { Text(formatMonthLabel(monthKey, localeTag)) },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primary.copy(alpha = 0.22f),
                                            selectedLabelColor = primary,
                                            selectedLeadingIconColor = primary
                                        ),
                                        modifier = Modifier.testTag("month_chip_$monthKey")
                                    )
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatsCategory.entries.forEach { category ->
                                    val selected = category == selectedCategory
                                    val icon = when (category) {
                                        StatsCategory.SONGS -> Icons.Rounded.MusicNote
                                        StatsCategory.ARTISTS -> Icons.Rounded.Person
                                        StatsCategory.ALBUMS -> Icons.Rounded.Album
                                    }
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category.label) },
                                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = secondary.copy(alpha = 0.22f),
                                            selectedLabelColor = secondary,
                                            selectedLeadingIconColor = secondary
                                        ),
                                        modifier = Modifier.weight(1f).testTag("stats_category_${category.name.lowercase()}")
                                    )
                                }
                            }
                        }
                        if (selectedCategory == StatsCategory.ARTISTS && data.topArtists.isNotEmpty()) {
                            val maxSec = data.topArtists.maxOf { it.totalSeconds }.coerceAtLeast(1L)
                            itemsIndexed(data.topArtists, key = { _, it -> "neon_art_${it.artistName}" }) { index, artist ->
                                RankedStatCard(
                                    rank = index + 1,
                                    title = artist.artistName,
                                    subtitle = "${artist.trackCount} canciones · ${artist.totalPlays} reprod.",
                                    durationText = formatPlaybackTimeReadable(artist.totalSeconds),
                                    fraction = artist.totalSeconds.toFloat() / maxSec.toFloat(),
                                    accent = secondary,
                                    leadingArtworkUri = null
                                )
                            }
                        } else if (selectedCategory == StatsCategory.ALBUMS && data.topAlbums.isNotEmpty()) {
                            val maxSec = data.topAlbums.maxOf { it.totalSeconds }.coerceAtLeast(1L)
                            itemsIndexed(data.topAlbums, key = { _, it -> "neon_alb_${it.albumName}|${it.artistName}" }) { index, album ->
                                RankedStatCard(
                                    rank = index + 1,
                                    title = album.albumName,
                                    subtitle = "${album.artistName} · ${album.totalPlays} reprod.",
                                    durationText = formatPlaybackTimeReadable(album.totalSeconds),
                                    fraction = album.totalSeconds.toFloat() / maxSec.toFloat(),
                                    accent = NightAccent,
                                    leadingArtworkUri = if (album.albumId > 0L) "content://media/external/audio/albumart/${album.albumId}" else null
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Estadísticas",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Canciones, artistas y álbumes que más escuchaste en el mes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(shape = RoundedCornerShape(50), color = primary) {
                                Text(
                                    text = "Mensual",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    item {
                        Ritmo2StatsQuickRow(
                            primary = primary,
                            secondary = secondary,
                            onOpenRecapAndWrapped = onOpenRecapAndWrapped,
                            onOpenMusicalHistory = onOpenMusicalHistory,
                            onOpenMusicalProfile = onOpenMusicalProfile
                        )
                    }

                    if (!isSignedIn) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = "Modo Offline activo", style = MaterialTheme.typography.titleMedium, color = primary)
                                    Text(
                                        text = "Tu tiempo local se está guardando en el dispositivo. Inicia sesión opcionalmente con Google si quieres sincronizar tu informe mensual en la nube entre dispositivos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = onRequestSignIn,
                                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier.testTag("monthly_stats_sign_in_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(strings.signInGoogle, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(data.availableMonths, key = { it }) { monthKey ->
                                val selected = monthKey == data.selectedMonthKey
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectMonth(monthKey) },
                                    label = { Text(formatMonthLabel(monthKey, localeTag)) },
                                    leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primary.copy(alpha = 0.22f),
                                        selectedLabelColor = primary,
                                        selectedLeadingIconColor = primary
                                    ),
                                    modifier = Modifier.testTag("month_chip_$monthKey")
                                )
                            }
                        }
                    }

                    item {
                        val summary = data.summary
                        val totalMonthSeconds = summary?.totalSeconds ?: data.topSongs.sumOf { it.totalSeconds }
                        val totalMonthPlays = summary?.totalPlays ?: data.topSongs.sumOf { it.playCount }
                        val favoriteSlotLabel = when {
                            summary == null -> "—"
                            summary.morningSeconds >= summary.afternoonSeconds && summary.morningSeconds >= summary.nightSeconds && summary.morningSeconds > 0 -> "Mañana"
                            summary.afternoonSeconds >= summary.morningSeconds && summary.afternoonSeconds >= summary.nightSeconds && summary.afternoonSeconds > 0 -> "Tarde"
                            summary.nightSeconds > 0 -> "Noche"
                            else -> "—"
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(listOf(primary.copy(alpha = 0.5f), secondary.copy(alpha = 0.5f))),
                                    shape = RoundedCornerShape(22.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Resumen · ${formatMonthLabel(data.selectedMonthKey, localeTag)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = primary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    KpiTile("Tiempo", formatPlaybackTimeReadable(totalMonthSeconds), Modifier.weight(1f))
                                    KpiTile("Reproducciones", totalMonthPlays.toString(), Modifier.weight(1f))
                                    KpiTile("Horario top", favoriteSlotLabel, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatsCategory.entries.forEach { category ->
                                val selected = category == selectedCategory
                                val icon = when (category) {
                                    StatsCategory.SONGS -> Icons.Rounded.MusicNote
                                    StatsCategory.ARTISTS -> Icons.Rounded.Person
                                    StatsCategory.ALBUMS -> Icons.Rounded.Album
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category.label) },
                                    leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = secondary.copy(alpha = 0.22f),
                                        selectedLabelColor = secondary,
                                        selectedLeadingIconColor = secondary
                                    ),
                                    modifier = Modifier.weight(1f).testTag("stats_category_${category.name.lowercase()}")
                                )
                            }
                        }
                    }

                    when (selectedCategory) {
                        StatsCategory.SONGS -> {
                            if (data.topSongs.isEmpty()) {
                                item { EmptyStatsCard() }
                            } else {
                                val maxSec = data.topSongs.maxOf { it.totalSeconds }.coerceAtLeast(1L)
                                itemsIndexed(data.topSongs, key = { _, it -> it.trackKey }) { index, song ->
                                    RankedStatCard(
                                        rank = index + 1,
                                        title = song.trackTitle,
                                        subtitle = "${song.artistName} · ${song.playCount} reprod.",
                                        durationText = formatPlaybackTimeReadable(song.totalSeconds),
                                        fraction = song.totalSeconds.toFloat() / maxSec.toFloat(),
                                        accent = primary,
                                        leadingArtworkUri = if (song.albumId > 0L) "content://media/external/audio/albumart/${song.albumId}" else null
                                    )
                                }
                            }
                        }
                        StatsCategory.ARTISTS -> {
                            if (data.topArtists.isEmpty()) {
                                item { EmptyStatsCard() }
                            } else {
                                val maxSec = data.topArtists.maxOf { it.totalSeconds }.coerceAtLeast(1L)
                                itemsIndexed(data.topArtists, key = { _, it -> it.artistName }) { index, artist ->
                                    RankedStatCard(
                                        rank = index + 1,
                                        title = artist.artistName,
                                        subtitle = "${artist.trackCount} canciones · ${artist.totalPlays} reprod.",
                                        durationText = formatPlaybackTimeReadable(artist.totalSeconds),
                                        fraction = artist.totalSeconds.toFloat() / maxSec.toFloat(),
                                        accent = secondary,
                                        leadingArtworkUri = null
                                    )
                                }
                            }
                        }
                        StatsCategory.ALBUMS -> {
                            if (data.topAlbums.isEmpty()) {
                                item { EmptyStatsCard() }
                            } else {
                                val maxSec = data.topAlbums.maxOf { it.totalSeconds }.coerceAtLeast(1L)
                                itemsIndexed(data.topAlbums, key = { _, it -> "${it.albumName}|${it.artistName}" }) { index, album ->
                                    RankedStatCard(
                                        rank = index + 1,
                                        title = album.albumName,
                                        subtitle = "${album.artistName} · ${album.totalPlays} reprod.",
                                        durationText = formatPlaybackTimeReadable(album.totalSeconds),
                                        fraction = album.totalSeconds.toFloat() / maxSec.toFloat(),
                                        accent = NightAccent,
                                        leadingArtworkUri = if (album.albumId > 0L) "content://media/external/audio/albumart/${album.albumId}" else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RankedStatCard(
    rank: Int,
    title: String,
    subtitle: String,
    durationText: String,
    fraction: Float,
    accent: Color,
    leadingArtworkUri: String?
) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (rank <= 3) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (rank <= 3) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                AlbumArtwork(
                    albumArtUri = leadingArtworkUri,
                    contentDescription = "Imagen de $title",
                    modifier = Modifier.size(44.dp),
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accent,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
private fun EmptyStatsCard() {
    val primary = MaterialTheme.colorScheme.primary
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.BarChart, contentDescription = null, tint = primary, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Sin estadísticas en este mes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Escucha tu música local y aquí aparecerán tus canciones, artistas y álbumes más reproducidos del mes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Ritmo2StatsQuickRow(
    primary: Color,
    secondary: Color,
    onOpenRecapAndWrapped: () -> Unit,
    onOpenMusicalHistory: () -> Unit,
    onOpenMusicalProfile: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onOpenRecapAndWrapped,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1.2f).testTag("stats_open_recap_btn")
        ) {
            Text("Recap / Wrapped", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = onOpenMusicalHistory,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = secondary.copy(alpha = 0.22f), contentColor = secondary),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("stats_open_history_btn")
        ) {
            Text("Historia", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = onOpenMusicalProfile,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer, contentColor = MaterialTheme.colorScheme.onSurface),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("stats_open_profile_btn")
        ) {
            Text("Perfil", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
        }
    }
}
