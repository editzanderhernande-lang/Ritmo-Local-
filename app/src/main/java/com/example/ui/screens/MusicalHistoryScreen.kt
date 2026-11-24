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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Loyalty
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.stats.MusicalHistoryReport
import com.example.model.formatMonthLabel
import com.example.model.formatPlaybackTimeReadable
import com.example.ui.components.AlbumArtwork
import com.example.ui.components.PeriodComparisonCard
import com.example.ui.components.RecapImageSharer

private enum class HistoryCompareMode(val label: String) {
    MONTH("Mes vs Anterior"),
    YEAR("Año vs Anterior"),
    HALVES("1ª vs 2ª Mitad")
}

@Composable
fun MusicalHistoryScreen(
    report: MusicalHistoryReport,
    localeTag: String = "es-ES",
    onSelectMonth: (String) -> Unit,
    onSelectYear: (Int) -> Unit,
    onPlayTrackByKey: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var compareMode by remember { mutableStateOf(HistoryCompareMode.MONTH) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("history_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("history_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
                    }
                }
                Column {
                    Text(
                        text = "Mi Historia Musical",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Línea de tiempo real de tus gustos, descubrimientos e hitos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!report.hasEnoughHistory) {
            item {
                HonestEmptyStatsNotice(
                    title = "Tu historia musical está por comenzar",
                    earliestDataMs = report.earliestDataMs
                )
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(report.availableMonths, key = { it }) { mk ->
                        FilterChip(
                            selected = mk == report.selectedMonthKey,
                            onClick = { onSelectMonth(mk) },
                            label = { Text(formatMonthLabel(mk, localeTag)) },
                            leadingIcon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("history_month_chip_$mk")
                        )
                    }
                    items(report.availableYears, key = { "yr_$it" }) { yr ->
                        FilterChip(
                            selected = yr == report.selectedYear,
                            onClick = { onSelectYear(yr) },
                            label = { Text("$yr") }
                        )
                    }
                }
            }

            // Period Comparison Selector
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryCompareMode.entries.forEach { mode ->
                        FilterChip(
                            selected = compareMode == mode,
                            onClick = { compareMode = mode },
                            label = { Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.weight(1f).testTag("history_compare_${mode.name.lowercase()}")
                        )
                    }
                }
            }

            item {
                val activeComparison = when (compareMode) {
                    HistoryCompareMode.MONTH -> report.monthComparison
                    HistoryCompareMode.YEAR -> report.yearComparison
                    HistoryCompareMode.HALVES -> report.yearHalvesComparison
                }
                PeriodComparisonCard(
                    title = "Comparador de períodos (${compareMode.label})",
                    comparison = activeComparison,
                    accentColor = primary
                )
            }

            // Automatic Milestones
            if (report.milestones.isNotEmpty()) {
                item {
                    Text("Hitos Musicales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(report.milestones, key = { it.id }) { milestone ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .width(240.dp)
                                    .border(1.dp, secondary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = secondary, modifier = Modifier.size(16.dp))
                                        Text(milestone.periodLabel, style = MaterialTheme.typography.labelSmall, color = secondary)
                                    }
                                    Text(milestone.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        milestone.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Loyal Artists
            if (report.loyalArtists.isNotEmpty()) {
                item {
                    Text("Artistas que se mantuvieron", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(report.loyalArtists, key = { "loyal_${it.artistName}" }) { entry ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(34.dp).clip(CircleShape).background(primary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Loyalty, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(entry.artistName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${entry.activeMonthsCount} mes(es) activo · ${entry.totalPlays} rep.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(formatPlaybackTimeReadable(entry.totalSeconds), style = MaterialTheme.typography.labelMedium, color = primary)
                        }
                    }
                }
            }

            // Forgotten Favorites / Canciones que dejaste atrás
            if (report.forgottenTracks.isNotEmpty()) {
                item {
                    Text("Canciones que dejaste atrás", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(report.forgottenTracks, key = { "forgotten_${it.trackKey}" }) { track ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AlbumArtwork(
                                albumArtUri = if (track.albumId > 0L) "content://media/external/audio/albumart/${track.albumId}" else null,
                                contentDescription = track.trackTitle,
                                modifier = Modifier.size(44.dp),
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.trackTitle, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${track.artistName} · Hace ${track.daysSinceLastPlay} días (${track.qualifiedPlays} rep.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onPlayTrackByKey(track.trackKey) },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("history_play_forgotten_${track.trackKey}")
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Reproducir ${track.trackTitle}", tint = primary)
                            }
                        }
                    }
                }
            }

            // Monthly Timeline Snapshots
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    Text("Línea de tiempo mensual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            items(report.timelinePeriods, key = { "tl_${it.monthKey}" }) { snapshot ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = formatMonthLabel(snapshot.monthKey, localeTag),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = primary
                            )
                            Text(
                                text = "${formatPlaybackTimeReadable(snapshot.totalSeconds)} · ${snapshot.totalPlays} rep.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        snapshot.topArtistName?.let { Text("Artista #1: $it", style = MaterialTheme.typography.bodyMedium) }
                        snapshot.topSongTitle?.let { Text("Canción #1: $it", style = MaterialTheme.typography.bodySmall) }
                        snapshot.topAlbumName?.let {
                            Text("Álbum destacado: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (snapshot.newArtistsDiscovered.isNotEmpty()) {
                            Text(
                                text = "Descubriste: ${snapshot.newArtistsDiscovered.joinToString(", ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                RecapImageSharer.shareHistoryMomentCard(
                                    context = context,
                                    snapshot = snapshot,
                                    milestone = report.milestones.firstOrNull { it.periodLabel == snapshot.monthKey },
                                    accentColorArgb = primary.toArgb(),
                                    secondaryColorArgb = secondary.toArgb(),
                                    localeTag = localeTag
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("history_share_moment_${snapshot.monthKey}")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartir momento como imagen")
                        }
                    }
                }
            }
        }
    }
}
