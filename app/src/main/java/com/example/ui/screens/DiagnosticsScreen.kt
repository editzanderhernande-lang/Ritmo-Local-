package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.diagnostics.AppDiagnosticsManager
import com.example.diagnostics.DiagnosticIssue
import com.example.diagnostics.DiagnosticsUiState
import com.example.ui.components.DiagnosticChecksSection
import com.example.ui.components.DiagnosticHealthSummaryCard
import com.example.ui.components.DiagnosticHistoryAndExportCard
import com.example.ui.components.DiagnosticIssueCard
import com.example.ui.components.DiagnosticPerformanceCard
import com.example.ui.components.DiagnosticSelfTestCard

@Composable
fun DiagnosticsScreen(
    uiState: DiagnosticsUiState,
    onAnalyzeApp: () -> Unit,
    onRunSelfTest: () -> Unit,
    onToggleDiagnosticMode: (Boolean) -> Unit,
    onExecuteRepair: (DiagnosticIssue) -> Unit,
    onDismissIssue: (String) -> Unit,
    onClearHistory: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var pendingConfirmIssue by remember { mutableStateOf<DiagnosticIssue?>(null) }

    val unresolvedIssues = remember(uiState.activeIssues) {
        uiState.activeIssues.filter { !it.isResolved }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("diagnostics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("diagnostics_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver"
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = "Centro de diagnóstico",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ritmo Local · Supervisión y reparación segura",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            DiagnosticHealthSummaryCard(
                report = uiState.latestReport,
                isAnalyzing = uiState.isAnalyzing,
                isRunningSelfTest = uiState.isRunningSelfTest,
                diagnosticModeEnabled = uiState.diagnosticModeEnabled,
                protection = uiState.playbackProtection,
                onAnalyzeApp = onAnalyzeApp,
                onRunSelfTest = onRunSelfTest,
                onToggleDiagnosticMode = onToggleDiagnosticMode
            )
        }

        item {
            DiagnosticPerformanceCard(perf = uiState.performance)
        }

        if (unresolvedIssues.isNotEmpty()) {
            item {
                Text(
                    text = "Problemas y optimizaciones detectadas (${unresolvedIssues.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(unresolvedIssues, key = { it.id }) { issue ->
                DiagnosticIssueCard(
                    issue = issue,
                    onRequestRepair = { target ->
                        if (target.repairAction.requiresConfirmation) {
                            pendingConfirmIssue = target
                        } else {
                            onExecuteRepair(target)
                        }
                    },
                    onDismissIssue = onDismissIssue
                )
            }
        }

        if (uiState.selfTestResults.isNotEmpty()) {
            item {
                DiagnosticSelfTestCard(results = uiState.selfTestResults)
            }
        }

        uiState.latestReport?.let { report ->
            item {
                DiagnosticChecksSection(
                    checks = report.componentChecks,
                    conflicts = report.conflicts
                )
            }
        }

        item {
            DiagnosticHistoryAndExportCard(
                history = uiState.history,
                logs = uiState.logs,
                diagnosticModeEnabled = uiState.diagnosticModeEnabled,
                onCopyReport = {
                    val text = AppDiagnosticsManager.buildShareableReportText(context)
                    clipboard.setText(AnnotatedString(text))
                    onShowMessage("Reporte técnico copiado al portapapeles.")
                },
                onExportReport = {
                    runCatching {
                        context.startActivity(AppDiagnosticsManager.buildExportShareIntent(context))
                    }.onFailure {
                        onShowMessage("No se encontró una app disponible para compartir.")
                    }
                },
                onClearHistory = onClearHistory
            )
        }
    }

    pendingConfirmIssue?.let { issue ->
        AlertDialog(
            onDismissRequest = { pendingConfirmIssue = null },
            icon = { Icon(Icons.Rounded.AutoFixHigh, contentDescription = null) },
            title = { Text("Confirmar reparación segura") },
            text = {
                Text(
                    issue.repairAction.confirmationMessage
                        ?: "Se aplicará '${issue.repairAction.buttonLabel}'. Tus canciones, listas y cuenta no se eliminarán."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingConfirmIssue = null
                        onExecuteRepair(issue)
                    },
                    modifier = Modifier.testTag("diag_confirm_repair_btn")
                ) {
                    Text("Reparar ahora")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingConfirmIssue = null },
                    modifier = Modifier.testTag("diag_cancel_repair_btn")
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
