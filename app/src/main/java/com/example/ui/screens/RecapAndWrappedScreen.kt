package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.stats.RealTimeStatsSummary
import com.example.data.stats.RitmoRecapReport
import com.example.data.stats.RitmoWrappedReport
import com.example.model.MonthlyTrackStat
import com.example.model.formatMonthLabel
import com.example.model.formatPlaybackTimeReadable
import com.example.ui.components.ActivityBarChart
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.GenreDistributionCard
import com.example.ui.components.PeriodComparisonCard
import com.example.ui.components.RecapImageSharer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RecapModeTab(val label: String) {
    RECAP("Recap Mensual"),
    WRAPPED("Wrapped Anual"),
    REALTIME("Tiempo Real")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecapAndWrappedScreen(
    recapReport: RitmoRecapReport,
    wrappedReport: RitmoWrappedReport,
    realTimeSummary: RealTimeStatsSummary,
    savedWrappedYears: List<Int>,
    cloudSyncEnabled: Boolean,
    isSignedIn: Boolean,
    localeTag: String = "es-ES",
    onSelectRecapMonth: (String) -> Unit,
    onSelectWrappedYear: (Int) -> Unit,
    onSaveWrappedSnapshot: (Int) -> Unit,
    onToggleCloudSync: (Boolean) -> Unit,
    onExportStatsJson: () -> Unit,
    onClearAllStatsConfirmed: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProfile: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(RecapModeTab.RECAP) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("recap_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.minimumInteractiveComponentSize().testTag("recap_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                        }
                    }
                    Column {
                        Text(
                            text = "Ritmo Recap & Wrapped",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Datos locales reales basados en reproducciones válidas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("recap_open_history_btn")
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = "Mi Historia Musical", tint = primary)
                    }
                    IconButton(
                        onClick = onOpenProfile,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("recap_open_profile_btn")
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = "Perfil Musical", tint = secondary)
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RecapModeTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable { activeTab = tab }
                                .testTag("recap_tab_${tab.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        when (activeTab) {
            RecapModeTab.RECAP -> {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recapReport.availableMonths, key = { it }) { mk ->
                            FilterChip(
                                selected = mk == recapReport.monthKey,
                                onClick = { onSelectRecapMonth(mk) },
                                label = { Text(formatMonthLabel(mk, localeTag)) },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("recap_month_chip_$mk")
                            )
                        }
                    }
                }

                if (!recapReport.hasEnoughData) {
                    item {
                        HonestEmptyStatsNotice(
                            title = "Aún no hay suficientes datos en ${formatMonthLabel(recapReport.monthKey, localeTag)}",
                            earliestDataMs = recapReport.earliestDataMs
                        )
                    }
                } else {
                    item {
                        RecapHeroCard(
                            badge = "RITMO RECAP",
                            headline = formatMonthLabel(recapReport.monthKey, localeTag),
                            totalTime = formatPlaybackTimeReadable(recapReport.totalSeconds),
                            plays = recapReport.totalPlays,
                            topArtist = recapReport.topArtist?.artistName ?: "—",
                            topSong = recapReport.topSong?.trackTitle ?: "—",
                            topAlbum = recapReport.topAlbum?.albumName ?: "—",
                            extraHighlight = buildString {
                                recapReport.peakDayDateKey?.let { append("Día pico: $it") }
                                recapReport.usualHourOfDay?.let { h ->
                                    if (isNotEmpty()) append(" · ")
                                    append("Hora habitual: ${String.format(Locale.US, "%02d:00", h)}")
                                }
                            },
                            primary = primary,
                            secondary = secondary,
                            onShareImage = {
                                RecapImageSharer.shareRecapCard(
                                    context = context,
                                    report = recapReport,
                                    accentColorArgb = primary.toArgb(),
                                    secondaryColorArgb = secondary.toArgb(),
                                    localeTag = localeTag
                                )
                            }
                        )
                    }

                    item {
                        ActivityBarChart(
                            title = "Actividad diaria del mes",
                            points = recapReport.dailyBarsInMonth,
                            primaryColor = primary,
                            secondaryColor = secondary
                        )
                    }

                    item {
                        PeriodComparisonCard(
                            title = "Comparativa con el mes anterior",
                            comparison = recapReport.comparisonWithPreviousMonth,
                            accentColor = primary
                        )
                    }

                    if (recapReport.newlyDiscoveredArtists.isNotEmpty()) {
                        item {
                            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Nuevos artistas descubiertos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        recapReport.newlyDiscoveredArtists.forEach { artist ->
                                            Surface(shape = RoundedCornerShape(50), color = secondary.copy(alpha = 0.16f)) {
                                                Text(
                                                    text = artist,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = secondary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GenreDistributionCard(
                            title = "Géneros más escuchados",
                            genres = recapReport.topGenres,
                            primaryColor = primary,
                            secondaryColor = secondary
                        )
                    }

                    item {
                        Text("Top 10 Canciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(recapReport.top10Songs, key = { _, it -> "recap_s_${it.trackKey}" }) { index, song ->
                        RecapRankedSongRow(rank = index + 1, song = song, accent = primary)
                    }

                    if (recapReport.top10Artists.isNotEmpty()) {
                        item {
                            Text("Top 10 Artistas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        itemsIndexed(recapReport.top10Artists, key = { _, it -> "recap_a_${it.artistName}" }) { idx, artist ->
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("#${idx + 1} · ${artist.artistName}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${formatPlaybackTimeReadable(artist.totalSeconds)} · ${artist.totalPlays} rep.",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            RecapModeTab.WRAPPED -> {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(wrappedReport.availableYears, key = { it }) { yr ->
                            val isSaved = yr in savedWrappedYears
                            FilterChip(
                                selected = yr == wrappedReport.year,
                                onClick = { onSelectWrappedYear(yr) },
                                label = { Text(if (isSaved) "$yr ★" else "$yr") },
                                modifier = Modifier.testTag("wrapped_year_chip_$yr")
                            )
                        }
                    }
                }

                if (!wrappedReport.hasEnoughData) {
                    item {
                        HonestEmptyStatsNotice(
                            title = "Aún no hay suficientes datos registrados en ${wrappedReport.year}",
                            earliestDataMs = wrappedReport.earliestDataMs
                        )
                    }
                } else {
                    item {
                        RecapHeroCard(
                            badge = "RITMO WRAPPED ${wrappedReport.year}",
                            headline = "Tu año ${wrappedReport.year}",
                            totalTime = formatPlaybackTimeReadable(wrappedReport.totalSeconds),
                            plays = wrappedReport.totalPlays,
                            topArtist = wrappedReport.topArtist?.artistName ?: "—",
                            topSong = wrappedReport.topSong?.trackTitle ?: "—",
                            topAlbum = wrappedReport.topAlbum?.albumName ?: "—",
                            extraHighlight = listOfNotNull(
                                wrappedReport.peakMonthKey?.let { "Mes pico: ${formatMonthLabel(it, localeTag)}" },
                                wrappedReport.discoveryOfYearArtist?.let { "Descubrimiento: $it" }
                            ).joinToString(" · "),
                            primary = secondary,
                            secondary = primary,
                            onShareImage = {
                                RecapImageSharer.shareWrappedCard(
                                    context = context,
                                    report = wrappedReport,
                                    accentColorArgb = secondary.toArgb(),
                                    secondaryColorArgb = primary.toArgb()
                                )
                            }
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = { onSaveWrappedSnapshot(wrappedReport.year) },
                            modifier = Modifier.fillMaxWidth().testTag("wrapped_save_snapshot_btn")
                        ) {
                            Icon(Icons.Rounded.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (wrappedReport.year in savedWrappedYears) {
                                    "Wrapped ${wrappedReport.year} guardado (actualizar)"
                                } else {
                                    "Guardar resumen anual ${wrappedReport.year}"
                                }
                            )
                        }
                    }

                    item {
                        ActivityBarChart(
                            title = "Evolución mes a mes (${wrappedReport.year})",
                            points = wrappedReport.monthlyEvolution12,
                            primaryColor = secondary,
                            secondaryColor = primary
                        )
                    }

                    item {
                        PeriodComparisonCard(
                            title = "Comparativa con ${wrappedReport.year - 1}",
                            comparison = wrappedReport.comparisonWithPreviousYear,
                            accentColor = secondary
                        )
                    }

                    item {
                        GenreDistributionCard(
                            title = "Géneros principales del año",
                            genres = wrappedReport.mainGenres,
                            primaryColor = secondary,
                            secondaryColor = primary
                        )
                    }

                    item {
                        Text("Top 20 Canciones de ${wrappedReport.year}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(wrappedReport.top20Songs, key = { _, it -> "wrap_s_${it.trackKey}" }) { index, song ->
                        RecapRankedSongRow(rank = index + 1, song = song, accent = secondary)
                    }
                }
            }

            RecapModeTab.REALTIME -> {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timeline, contentDescription = null, tint = primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Estadísticas en tiempo real", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RealTimeKpiBox("Hoy", formatPlaybackTimeReadable(realTimeSummary.todaySeconds), Modifier.weight(1f))
                                RealTimeKpiBox("Reprod. hoy", realTimeSummary.todayPlays.toString(), Modifier.weight(1f))
                                RealTimeKpiBox("Artistas hoy", realTimeSummary.todayUniqueArtists.toString(), Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RealTimeKpiBox("Esta semana", formatPlaybackTimeReadable(realTimeSummary.weeklyTotalSeconds), Modifier.weight(1f))
                                RealTimeKpiBox("Histórico", formatPlaybackTimeReadable(realTimeSummary.allTimeSeconds), Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    ActivityBarChart(
                        title = "Últimos 7 días",
                        points = realTimeSummary.weeklyDailyBars,
                        primaryColor = primary,
                        secondaryColor = secondary
                    )
                }

                item {
                    ActivityBarChart(
                        title = "Actividad mensual reciente",
                        points = realTimeSummary.monthlyBars,
                        primaryColor = secondary,
                        secondaryColor = primary
                    )
                }
            }
        }

        // Privacy, Export & History Control Section
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Privacidad y control de datos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Respaldo opcional en la nube", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    if (isSignedIn) "Privado por defecto. Actívalo solo si quieres sincronizar." else "Actívalo cuando inicies sesión con Google.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = cloudSyncEnabled,
                            onCheckedChange = onToggleCloudSync,
                            modifier = Modifier.testTag("recap_cloud_sync_switch")
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onExportStatsJson,
                            modifier = Modifier.weight(1f).testTag("recap_export_json_btn")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exportar JSON")
                        }
                        OutlinedButton(
                            onClick = { showConfirmDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("recap_clear_history_btn")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Borrar historial")
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text("¿Borrar todo el historial de escucha?") },
            text = {
                Text("Se eliminarán todas tus estadísticas, gráficos y resúmenes Recap locales. Tus canciones, carátulas y playlists no se borrarán.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAllStatsConfirmed()
                        showConfirmDeleteDialog = false
                    }
                ) {
                    Text("Borrar historial", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RecapHeroCard(
    badge: String,
    headline: String,
    totalTime: String,
    plays: Long,
    topArtist: String,
    topSong: String,
    topAlbum: String,
    extraHighlight: String,
    primary: Color,
    secondary: Color,
    onShareImage: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.28f), secondary.copy(alpha = 0.24f))))
            .border(1.dp, primary.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = primary) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = secondary)
            }
            Text(headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "$totalTime escuchados · $plays reproducciones válidas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Artista #1: $topArtist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Canción #1: $topSong", style = MaterialTheme.typography.bodyMedium)
            Text("Álbum #1: $topAlbum", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (extraHighlight.isNotBlank()) {
                Text(extraHighlight, style = MaterialTheme.typography.labelSmall, color = primary)
            }
            Button(
                onClick = onShareImage,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("recap_share_image_btn")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Compartir resumen como imagen")
            }
        }
    }
}

@Composable
private fun RealTimeKpiBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecapRankedSongRow(rank: Int, song: MonthlyTrackStat, accent: Color) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("#$rank", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            AlbumArtwork(
                albumArtUri = if (song.albumId > 0L) "content://media/external/audio/albumart/${song.albumId}" else null,
                contentDescription = song.trackTitle,
                modifier = Modifier.size(40.dp),
                cornerRadius = 10.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.trackTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${song.artistName} · ${song.playCount} rep.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(formatPlaybackTimeReadable(song.totalSeconds), style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HonestEmptyStatsNotice(title: String, earliestDataMs: Long?) {
    val dateNote = earliestDataMs?.let {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        "Datos disponibles desde ${fmt.format(Date(it))}."
    } ?: "Escucha canciones reales (al menos unos segundos continuos) para construir tus métricas sin datos inventados."
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(dateNote, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
