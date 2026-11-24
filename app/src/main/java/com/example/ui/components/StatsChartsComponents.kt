package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.stats.ChartBarPoint
import com.example.data.stats.GenreStatSummary
import com.example.data.stats.PeriodComparisonSummary
import com.example.model.formatPlaybackTimeReadable
import kotlin.math.roundToInt

@Composable
fun ActivityBarChart(
    title: String,
    points: List<ChartBarPoint>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val peak = points.maxByOrNull { it.seconds }
                if (peak != null && peak.seconds > 0L) {
                    Text(
                        text = "Pico: ${peak.label} (${formatPlaybackTimeReadable(peak.seconds)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor
                    )
                }
            }

            if (points.isEmpty() || points.all { it.seconds == 0L }) {
                Text(
                    text = "Aún no hay actividad suficiente en este gráfico.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxSeconds = points.maxOf { it.seconds }.coerceAtLeast(1L)
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                ) {
                    val count = points.size.coerceAtLeast(1)
                    val gap = size.width * 0.015f
                    val barW = ((size.width - gap * (count + 1)) / count).coerceAtLeast(6f)
                    val barBrush = Brush.verticalGradient(listOf(primaryColor, secondaryColor))
                    val trackColor = primaryColor.copy(alpha = 0.10f)

                    points.forEachIndexed { idx, pt ->
                        val left = gap + idx * (barW + gap)
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barW, size.height),
                            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                        )
                        val ratio = (pt.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
                        val activeH = if (pt.seconds > 0L) (size.height * ratio).coerceAtLeast(10f) else 0f
                        if (activeH > 0f) {
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(left, size.height - activeH),
                                size = Size(barW, activeH),
                                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                            )
                        }
                    }
                }

                // Compact bottom labels
                val step = when {
                    points.size > 16 -> 4
                    points.size > 10 -> 2
                    else -> 1
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { idx, pt ->
                        if (idx % step == 0 || idx == points.lastIndex) {
                            Text(
                                text = pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodComparisonCard(
    title: String,
    comparison: PeriodComparisonSummary,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (comparison.hasPriorPeriodData) {
                    val sign = if (comparison.deltaPercentSeconds >= 0) "+" else ""
                    Text(
                        text = "$sign${comparison.deltaPercentSeconds}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = comparison.summaryMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (comparison.hasPriorPeriodData) {
                val maxSec = maxOf(comparison.currentSeconds, comparison.previousSeconds, 1L)
                ComparisonBarRow(
                    label = comparison.currentLabel,
                    valueLabel = "${formatPlaybackTimeReadable(comparison.currentSeconds)} · ${comparison.currentPlays} rep.",
                    fraction = comparison.currentSeconds.toFloat() / maxSec.toFloat(),
                    barColor = accentColor
                )
                ComparisonBarRow(
                    label = comparison.previousLabel,
                    valueLabel = "${formatPlaybackTimeReadable(comparison.previousSeconds)} · ${comparison.previousPlays} rep.",
                    fraction = comparison.previousSeconds.toFloat() / maxSec.toFloat(),
                    barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    valueLabel: String,
    fraction: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0.03f, 1f) },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}

@Composable
fun GenreDistributionCard(
    title: String,
    genres: List<GenreStatSummary>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (genres.isEmpty()) {
                Text(
                    text = "Todavía no hay etiquetas de género registradas en tus reproducciones recientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                genres.forEachIndexed { idx, genre ->
                    val barColor = if (idx % 2 == 0) primaryColor else secondaryColor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = genre.genre,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = "${(genre.percentage * 100f).roundToInt()}% · ${formatPlaybackTimeReadable(genre.totalSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = barColor
                        )
                    }
                    LinearProgressIndicator(
                        progress = { genre.percentage.coerceIn(0.05f, 1f) },
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}
