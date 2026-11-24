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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MonthlyStat
import com.example.model.PlaybackProgress
import com.example.model.TimeOfDaySlot
import com.example.model.UiState
import com.example.model.formatDurationMs
import com.example.model.formatPlaybackTimeReadable
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerUiState
import com.example.player.formatSleepTimerCountdown
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AfternoonAccent
import com.example.ui.theme.CoralPulse
import com.example.ui.theme.EmeraldSync
import com.example.ui.theme.MorningAccent
import com.example.ui.theme.NightAccent
import com.example.ui.viewmodel.CloudStatusUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimeCloudScreen(
    isSignedIn: Boolean,
    userEmail: String?,
    userDisplayName: String?,
    liveTotalPlaybackSeconds: Long,
    sessionSeconds: Long,
    playbackState: PlaybackUiState,
    cloudProgressState: UiState<PlaybackProgress?>,
    currentMonthSummary: MonthlyStat?,
    cloudStatus: CloudStatusUi,
    sleepTimerState: SleepTimerUiState,
    onSaveNow: () -> Unit,
    onResumeFromCloud: (PlaybackProgress) -> Unit,
    onOpenSleepTimer: () -> Unit,
    onExtendSleepTimer: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenThemeCustomization: () -> Unit,
    onOpenMonthlyStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tiempo y Nube",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isSignedIn) {
                            "Tiempo de reproducción y progreso sincronizados en la nube."
                        } else {
                            "Modo offline: tiempo y progreso guardados en tu dispositivo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onOpenMonthlyStats,
                    modifier = Modifier.testTag("time_open_monthly_button")
                ) {
                    Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.navMonthly)
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(listOf(primary.copy(alpha = 0.6f), secondary.copy(alpha = 0.5f))),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(primary.copy(alpha = 0.14f), Color.Transparent)))
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccessTimeFilled, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tiempo total de reproducción", style = MaterialTheme.typography.titleSmall, color = primary)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = (if (isSignedIn) EmeraldSync else secondary).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        !isSignedIn -> Icons.Rounded.CloudOff
                                        cloudStatus.isSyncing -> Icons.Rounded.CloudSync
                                        else -> Icons.Rounded.CloudDone
                                    },
                                    contentDescription = null,
                                    tint = if (isSignedIn) EmeraldSync else secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        !isSignedIn -> "Modo Offline"
                                        cloudStatus.isSyncing -> "Guardando…"
                                        else -> "Nube activa"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSignedIn) EmeraldSync else secondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = formatPlaybackTimeReadable(liveTotalPlaybackSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("total_playback_time_text")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "Sesión actual: ${formatPlaybackTimeReadable(sessionSeconds)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        cloudStatus.lastSyncedAtMs?.let { syncedAt ->
                            val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            Text(
                                text = "Últ. guardado: ${fmt.format(Date(syncedAt))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (isSignedIn) "Progreso guardado en la nube" else "Último progreso local",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    when (cloudProgressState) {
                        is UiState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primary, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Consultando progreso…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        is UiState.Error -> Text(cloudProgressState.message, style = MaterialTheme.typography.bodySmall, color = CoralPulse)
                        is UiState.Success -> {
                            val saved = cloudProgressState.data
                            val activeTrack = playbackState.currentTrack
                            if (saved != null && saved.trackTitle.isNotBlank()) {
                                val slot = TimeOfDaySlot.fromWire(saved.timeOfDaySlot)
                                Text(saved.trackTitle, style = MaterialTheme.typography.titleLarge, color = primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${saved.artistName} · ${saved.albumName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(10.dp))
                                val ratio = if (saved.durationMs > 0L) (saved.positionMs.toFloat() / saved.durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = primary,
                                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Posición: ${formatDurationMs(saved.positionMs)} / ${formatDurationMs(saved.durationMs)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Mix ${slot.displayName}", style = MaterialTheme.typography.labelSmall, color = primary)
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onResumeFromCloud(saved) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                        modifier = Modifier.weight(1f).testTag("time_tab_resume_button")
                                    ) {
                                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reanudar", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = onSaveNow,
                                        enabled = !cloudStatus.isSyncing && activeTrack != null,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f).testTag("time_tab_save_now_button")
                                    ) {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Guardar")
                                    }
                                }
                            } else {
                                Text(
                                    text = "Reproduce una pista para guardar automáticamente la canción, posición y tiempo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onSaveNow,
                                    enabled = !cloudStatus.isSyncing && activeTrack != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                                    modifier = Modifier.testTag("time_tab_save_now_button")
                                ) {
                                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Guardar progreso ahora", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    cloudStatus.statusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = msg, style = MaterialTheme.typography.bodySmall, color = EmeraldSync)
                    }
                }
            }
        }

        if (isSignedIn) {
            item {
                val morningSec = currentMonthSummary?.morningSeconds ?: 0L
                val afternoonSec = currentMonthSummary?.afternoonSeconds ?: 0L
                val nightSec = currentMonthSummary?.nightSeconds ?: 0L
                val sumSec = (morningSec + afternoonSec + nightSec).coerceAtLeast(1L)

                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Distribución por hora este mes", style = MaterialTheme.typography.titleMedium)
                        SlotTimeBarRow("Mañana (05:00 – 11:59)", morningSec, morningSec.toFloat() / sumSec.toFloat(), MorningAccent)
                        SlotTimeBarRow("Tarde (12:00 – 18:59)", afternoonSec, afternoonSec.toFloat() / sumSec.toFloat(), AfternoonAccent)
                        SlotTimeBarRow("Noche (19:00 – 04:59)", nightSec, nightSec.toFloat() / sumSec.toFloat(), NightAccent)
                    }
                }
            }
        }

        // Sleep Timer Card
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(primary.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Temporizador de apagado", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = when {
                                    sleepTimerState.isActive && sleepTimerState.stopAfterCurrentTrack -> "Se detendrá al finalizar la canción actual"
                                    sleepTimerState.isActive -> "Deteniendo música en ${formatSleepTimerCountdown(sleepTimerState.remainingSeconds)}"
                                    else -> "Programa una cuenta regresiva para detener la reproducción"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sleepTimerState.isActive) primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(onClick = onOpenSleepTimer, modifier = Modifier.testTag("time_open_sleep_timer_button")) {
                            Text(if (sleepTimerState.isActive) "Ajustar" else "Configurar")
                        }
                    }

                    if (sleepTimerState.isActive) {
                        if (!sleepTimerState.stopAfterCurrentTrack) {
                            LinearProgressIndicator(
                                progress = { sleepTimerState.progressFraction },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                                color = primary,
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (!sleepTimerState.stopAfterCurrentTrack) {
                                OutlinedButton(onClick = onExtendSleepTimer, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).testTag("time_extend_sleep_timer_button")) {
                                    Text("+5 min")
                                }
                            }
                            OutlinedButton(onClick = onCancelSleepTimer, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f).testTag("time_cancel_sleep_timer_button")) {
                                Text("Cancelar", color = CoralPulse)
                            }
                        }
                    }
                }
            }
        }

        // Quick Links: Theme & Settings
        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onOpenThemeCustomization, modifier = Modifier.weight(1f).testTag("time_open_theme_button")) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Colores")
                    }
                    FilledTonalButton(onClick = onOpenSettings, modifier = Modifier.weight(1f).testTag("time_open_settings_button")) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.settingsTitle)
                    }
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isSignedIn) userDisplayName?.takeIf { it.isNotBlank() } ?: "Cuenta Google" else "Modo Offline",
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isSignedIn) userEmail ?: "Sincronización activa" else "Inicia sesión para estadísticas mensuales y nube",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isSignedIn) {
                        OutlinedButton(onClick = onSignOut, shape = RoundedCornerShape(12.dp), modifier = Modifier.testTag("sign_out_button")) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.signOut)
                        }
                    } else {
                        Button(
                            onClick = onSignIn,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.testTag("time_sign_in_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Conectar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotTimeBarRow(label: String, seconds: Long, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = formatPlaybackTimeReadable(seconds), style = MaterialTheme.typography.labelMedium, color = color)
        }
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    }
}
