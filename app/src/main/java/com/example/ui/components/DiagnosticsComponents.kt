package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.diagnostics.ComponentCheckResult
import com.example.diagnostics.ComponentConflict
import com.example.diagnostics.DiagnosticHistoryEntry
import com.example.diagnostics.DiagnosticIssue
import com.example.diagnostics.DiagnosticLogEntry
import com.example.diagnostics.DiagnosticReport
import com.example.diagnostics.DiagnosticStatus
import com.example.diagnostics.FunctionalTestItem
import com.example.diagnostics.PerformanceSnapshot
import com.example.diagnostics.PlaybackProtectionSnapshot
import com.example.diagnostics.RepairActionId
import com.example.diagnostics.accentColor
import com.example.ui.theme.EmeraldSync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticHealthSummaryCard(
    report: DiagnosticReport?,
    isAnalyzing: Boolean,
    isRunningSelfTest: Boolean,
    diagnosticModeEnabled: Boolean,
    protection: PlaybackProtectionSnapshot,
    onAnalyzeApp: () -> Unit,
    onRunSelfTest: () -> Unit,
    onToggleDiagnosticMode: (Boolean) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val statusColor = (report?.overallStatus ?: DiagnosticStatus.OK).accentColor()
    val score = report?.healthScore ?: 100

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(statusColor.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.18f))
                            .border(2.dp, statusColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$score%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor
                        )
                    }
                    Column {
                        Text(
                            text = "Estado general: ${(report?.overallStatus ?: DiagnosticStatus.OK).label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = report?.recommendationSummary
                                ?: "Presiona 'Analizar aplicación' para revisar los 15 módulos en tiempo real.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Playback Protection Guard Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EmeraldSync.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldSync.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Protección de reproducción",
                        tint = EmeraldSync,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Protección de reproducción activa",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSync
                        )
                        Text(
                            text = "${protection.lastGuardNote} (Navegaciones protegidas: ${protection.navigationCountWithoutInterrupt}, fallos aislados: ${protection.guardedErrorsCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAnalyzeApp,
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("diag_analyze_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Analizar aplicación", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                FilledTonalButton(
                    onClick = onRunSelfTest,
                    enabled = !isRunningSelfTest,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("diag_self_test_button")
                ) {
                    Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Probar funciones", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDiagnosticMode(!diagnosticModeEnabled) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Terminal, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
                    Column {
                        Text(
                            text = "Modo de diagnóstico técnico",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Registra logs locales detallados para soporte sin guardar contraseñas ni tokens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = diagnosticModeEnabled,
                    onCheckedChange = onToggleDiagnosticMode,
                    modifier = Modifier.testTag("diag_mode_switch")
                )
            }
        }
    }
}

@Composable
fun DiagnosticPerformanceCard(perf: PerformanceSnapshot) {
    val primary = MaterialTheme.colorScheme.primary
    val ramTint = when {
        perf.heapUsagePercent >= 85 || perf.isLowSystemMemory -> Color(0xFFEF4444)
        perf.heapUsagePercent >= 65 -> Color(0xFFF59E0B)
        else -> EmeraldSync
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                    Text("Memoria y rendimiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "${perf.usedHeapMb} / ${perf.maxHeapMb} MB (${perf.heapUsagePercent}%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = ramTint,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { (perf.heapUsagePercent / 100f).coerceIn(0.02f, 1f) },
                color = ramTint,
                trackColor = ramTint.copy(alpha = 0.18f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RAM libre sistema: ${perf.availableSystemRamMb} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Caché: ${perf.tempCacheSizeKb} KB · ${perf.audioRouteLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DiagnosticIssueCard(
    issue: DiagnosticIssue,
    onRequestRepair: (DiagnosticIssue) -> Unit,
    onDismissIssue: (String) -> Unit
) {
    val sevColor = issue.severity.accentColor()
    var showTechDetails by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, sevColor.copy(alpha = 0.42f), RoundedCornerShape(18.dp))
            .testTag("diag_issue_card_${issue.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = sevColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = issue.severity.label.uppercase(Locale.US),
                            style = MaterialTheme.typography.labelSmall,
                            color = sevColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = issue.module.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (issue.occurrenceCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Repetido ×${issue.occurrenceCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = issue.plainExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Solución: ${issue.proposedSolution}",
                style = MaterialTheme.typography.bodySmall,
                color = EmeraldSync,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTechDetails = !showTechDetails },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (showTechDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Ver detalle técnico",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (showTechDetails) "Ocultar detalle técnico" else "Ver detalle técnico (${formatDiagTime(issue.lastOccurredAtMs)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = showTechDetails) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = issue.technicalMessage.ifBlank { "Sin traza técnica adicional" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (issue.repairAction != RepairActionId.NONE && !issue.isResolved) {
                    Button(
                        onClick = { onRequestRepair(issue) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sevColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("diag_repair_${issue.id}")
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(issue.repairAction.buttonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                TextButton(
                    onClick = { onDismissIssue(issue.id) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Omitir")
                }
            }
        }
    }
}

@Composable
fun DiagnosticChecksSection(
    checks: List<ComponentCheckResult>,
    conflicts: List<ComponentConflict>
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Revisión por módulos (${checks.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            checks.forEach { check ->
                val color = check.status.accentColor()
                val icon = when (check.status) {
                    DiagnosticStatus.OK -> Icons.Rounded.CheckCircle
                    DiagnosticStatus.WARNING -> Icons.Rounded.Warning
                    DiagnosticStatus.ERROR -> Icons.Rounded.Error
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(icon, contentDescription = check.status.label, tint = color, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = check.module.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = check.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = check.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (conflicts.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Conflictos detectados (${conflicts.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
                conflicts.forEach { conf ->
                    Text(
                        text = "• ${conf.description} (${conf.mitigation})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticSelfTestCard(results: List<FunctionalTestItem>) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_self_test_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Science, contentDescription = null, tint = EmeraldSync, modifier = Modifier.size(18.dp))
                Text(
                    text = "Resultados de prueba de funcionamiento (${results.count { it.passed }}/${results.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            results.forEach { item ->
                val color = if (item.passed) EmeraldSync else Color(0xFFEF4444)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = if (item.passed) "OK · ${item.latencyMs}ms" else "FALLO",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosticHistoryAndExportCard(
    history: List<DiagnosticHistoryEntry>,
    logs: List<DiagnosticLogEntry>,
    diagnosticModeEnabled: Boolean,
    onCopyReport: () -> Unit,
    onExportReport: () -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_history_section")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("Historial y reporte técnico", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                if (history.isNotEmpty() || logs.isNotEmpty()) {
                    TextButton(onClick = onClearHistory, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyReport,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("diag_copy_report_btn")
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar reporte", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = onExportReport,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("diag_export_report_btn")
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar informe", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (history.isEmpty()) {
                Text(
                    text = "Aún no hay eventos registrados en el historial.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                history.take(12).forEach { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = entry.module.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = formatDiagTime(entry.timestampMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(entry.description, style = MaterialTheme.typography.bodySmall)
                        val outcomeText = buildString {
                            if (!entry.repairApplied.isNullOrBlank()) {
                                append("Reparación: ${entry.repairApplied} · ")
                            }
                            append(entry.outcomeLabel)
                        }
                        Text(
                            text = outcomeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.wasSuccessful) EmeraldSync else Color(0xFFF59E0B)
                        )
                    }
                }
            }

            if (diagnosticModeEnabled) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Text("Logs técnicos recientes (${logs.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                if (logs.isEmpty()) {
                    Text("Sin entradas de log aún.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    logs.take(15).forEach { log ->
                        Text(
                            text = "${formatDiagTime(log.timestampMs)} [${log.level}/${log.module.id}] ${log.message}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDiagTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    return SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(epochMs))
}
