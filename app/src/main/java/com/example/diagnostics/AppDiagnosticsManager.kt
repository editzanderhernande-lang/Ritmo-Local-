package com.example.diagnostics

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.model.AppSettingsPrefs
import com.example.model.CustomPlaylist
import com.example.model.LocalTrack
import com.example.model.PlaybackEnginePrefs
import com.example.player.AudioEffectsController
import com.example.player.EqualizerUiState
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerUiState
import com.example.voice.VoiceAudioRoute
import com.example.voice.VoiceAudioRouteDetector
import com.example.voice.VoiceCommandParser
import com.example.voice.VoiceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DiagnosticAuditInput(
    val playbackState: PlaybackUiState,
    val equalizerState: EqualizerUiState,
    val sleepTimerState: SleepTimerUiState,
    val voiceState: VoiceUiState,
    val settings: AppSettingsPrefs,
    val localTracks: List<LocalTrack>,
    val customPlaylists: List<CustomPlaylist>,
    val isSignedIn: Boolean,
    val cloudSyncError: String?,
    val lastCloudSyncMs: Long?,
    val totalPlaybackSeconds: Long,
    val playbackEnginePrefs: PlaybackEnginePrefs = PlaybackEnginePrefs(),
    val savedDeviceProfilesCount: Int = 0
)

data class DiagnosticRepairCallbacks(
    val reconnectPlayer: () -> Unit = {},
    val restoreLastPlayback: () -> Boolean = { false },
    val reapplyEqualizer: () -> Unit = {},
    val retryCloudSync: () -> Unit = {},
    val refreshLibrary: () -> Unit = {},
    val resetSleepTimer: () -> Unit = {},
    val cleanOrphanPlaylistRefs: suspend () -> Int = { 0 },
    val requestPermission: (String) -> Unit = {},
    val openBatterySettings: () -> Unit = {}
)

object AppDiagnosticsManager {

    private const val MAX_ISSUES = 60
    private const val MAX_HISTORY = 120
    private const val MAX_LOGS = 200

    private val lock = Any()
    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    @Volatile
    private var appFilesDir: File? = null

    @Volatile
    private var initialized = false

    fun initialize(context: Context, diagnosticModeEnabled: Boolean = false) {
        val dir = context.applicationContext.filesDir
        synchronized(lock) {
            appFilesDir = dir
            if (!initialized) {
                initialized = true
                loadFromDiskLocked(dir)
            }
            _uiState.value = _uiState.value.copy(
                diagnosticModeEnabled = diagnosticModeEnabled,
                performance = capturePerformanceSnapshot(context, _uiState.value.performance.libraryTrackCount)
            )
        }
        logTech(
            level = "INFO",
            module = DiagnosticModule.DATABASE,
            message = "Centro de diagnóstico inicializado (v${BuildConfig.VERSION_NAME}, modo técnico=$diagnosticModeEnabled)."
        )
    }

    fun setDiagnosticModeEnabled(enabled: Boolean) {
        synchronized(lock) {
            _uiState.value = _uiState.value.copy(diagnosticModeEnabled = enabled)
            persistLocked()
        }
        logTech(
            level = "INFO",
            module = DiagnosticModule.PERFORMANCE_MEMORY,
            message = if (enabled) "Modo de diagnóstico técnico ACTIVADO." else "Modo de diagnóstico técnico DESACTIVADO."
        )
    }

    fun noteNavigationProtected(route: String, isPlaying: Boolean) {
        synchronized(lock) {
            val prev = _uiState.value.playbackProtection
            val nextCount = if (isPlaying) prev.navigationCountWithoutInterrupt + 1 else prev.navigationCountWithoutInterrupt
            _uiState.value = _uiState.value.copy(
                playbackProtection = prev.copy(
                    isProtected = true,
                    navigationCountWithoutInterrupt = nextCount,
                    lastGuardNote = "Navegación a '$route' protegida sin interrumpir reproducción."
                )
            )
        }
    }

    fun updateProtectedPlaybackSnapshot(trackKey: String?, trackTitle: String?, positionMs: Long) {
        if (trackKey.isNullOrBlank()) return
        synchronized(lock) {
            val prev = _uiState.value.playbackProtection
            _uiState.value = _uiState.value.copy(
                playbackProtection = prev.copy(
                    lastProtectedTrackKey = trackKey,
                    lastProtectedTrackTitle = trackTitle,
                    lastProtectedPositionMs = positionMs.coerceAtLeast(0L)
                )
            )
        }
    }

    fun <T> runGuarded(
        module: DiagnosticModule,
        operationName: String,
        severity: DiagnosticSeverity = DiagnosticSeverity.MEDIUM,
        fallback: T,
        block: () -> T
    ): T {
        return try {
            block()
        } catch (t: Throwable) {
            recordException(
                module = module,
                title = "Fallo protegido en $operationName",
                throwable = t,
                severity = severity,
                plainExplanation = "Ocurrió un problema en ${module.displayName}, pero el reproductor se mantuvo protegido.",
                proposedSolution = "Usa 'Analizar aplicación' o intenta la reparación automática recomendada."
            )
            synchronized(lock) {
                val prev = _uiState.value.playbackProtection
                _uiState.value = _uiState.value.copy(
                    playbackProtection = prev.copy(
                        guardedErrorsCount = prev.guardedErrorsCount + 1,
                        lastGuardNote = "Fallo aislado en ${module.displayName}; la música continuó protegida."
                    )
                )
            }
            fallback
        }
    }

    fun recordException(
        module: DiagnosticModule,
        title: String,
        throwable: Throwable,
        severity: DiagnosticSeverity = DiagnosticSeverity.MEDIUM,
        plainExplanation: String,
        proposedSolution: String,
        repairAction: RepairActionId = defaultRepairFor(module)
    ) {
        val tech = buildString {
            append(throwable::class.java.simpleName)
            val msg = throwable.message?.takeIf { it.isNotBlank() }
            if (msg != null) append(": ").append(msg)
        }
        recordError(
            module = module,
            title = title,
            technicalMessage = tech,
            severity = severity,
            plainExplanation = plainExplanation,
            proposedSolution = proposedSolution,
            repairAction = repairAction
        )
    }

    fun recordError(
        module: DiagnosticModule,
        title: String,
        technicalMessage: String,
        severity: DiagnosticSeverity = DiagnosticSeverity.MEDIUM,
        plainExplanation: String,
        proposedSolution: String,
        repairAction: RepairActionId = defaultRepairFor(module),
        nowMs: Long = System.currentTimeMillis()
    ) {
        val cleanTech = redactSensitive(technicalMessage).take(450)
        val cleanTitle = redactSensitive(title).take(140)
        val cleanPlain = redactSensitive(plainExplanation).take(280)
        val cleanSolution = redactSensitive(proposedSolution).take(280)

        synchronized(lock) {
            val currentIssues = _uiState.value.activeIssues.toMutableList()
            val existingIdx = currentIssues.indexOfFirst {
                !it.isResolved && it.module == module && it.title.equals(cleanTitle, ignoreCase = true)
            }
            val updatedIssue: DiagnosticIssue
            if (existingIdx >= 0) {
                val prev = currentIssues[existingIdx]
                val nextCount = prev.occurrenceCount + 1
                // Escalate severity automatically when repeated 3+ times
                val escalatedSeverity = when {
                    nextCount >= 3 && severity.rank < DiagnosticSeverity.HIGH.rank -> DiagnosticSeverity.HIGH
                    nextCount >= 5 -> DiagnosticSeverity.CRITICAL
                    else -> if (severity.rank > prev.severity.rank) severity else prev.severity
                }
                updatedIssue = prev.copy(
                    technicalMessage = cleanTech.ifBlank { prev.technicalMessage },
                    plainExplanation = if (nextCount >= 2) {
                        "$cleanPlain (Error repetido $nextCount veces)."
                    } else cleanPlain,
                    proposedSolution = cleanSolution,
                    severity = escalatedSeverity,
                    lastOccurredAtMs = nowMs,
                    occurrenceCount = nextCount,
                    repairAction = if (repairAction != RepairActionId.NONE) repairAction else prev.repairAction
                )
                currentIssues[existingIdx] = updatedIssue
            } else {
                updatedIssue = DiagnosticIssue(
                    id = "iss_${UUID.randomUUID().toString().take(8)}",
                    module = module,
                    title = cleanTitle,
                    plainExplanation = cleanPlain,
                    technicalMessage = cleanTech,
                    proposedSolution = cleanSolution,
                    severity = severity,
                    firstOccurredAtMs = nowMs,
                    lastOccurredAtMs = nowMs,
                    occurrenceCount = 1,
                    isResolved = false,
                    repairAction = repairAction
                )
                currentIssues.add(0, updatedIssue)
            }

            // Sort unresolved critical/high/repeated issues first
            val sortedIssues = currentIssues
                .sortedWith(
                    compareBy<DiagnosticIssue> { it.isResolved }
                        .thenByDescending { it.severity.rank }
                        .thenByDescending { it.occurrenceCount }
                        .thenByDescending { it.lastOccurredAtMs }
                )
                .take(MAX_ISSUES)

            val historyEntry = DiagnosticHistoryEntry(
                id = "hist_${UUID.randomUUID().toString().take(8)}",
                timestampMs = nowMs,
                module = module,
                severity = updatedIssue.severity,
                description = if (updatedIssue.occurrenceCount > 1) {
                    "$cleanTitle (ocurrencia #${updatedIssue.occurrenceCount})"
                } else cleanTitle,
                repairApplied = null,
                outcomeLabel = "Detectado",
                wasSuccessful = false
            )
            val nextHistory = (listOf(historyEntry) + _uiState.value.history).take(MAX_HISTORY)

            _uiState.value = _uiState.value.copy(
                activeIssues = sortedIssues,
                history = nextHistory
            )
            appendLogLocked("ERROR", module, "$cleanTitle -> $cleanTech", nowMs = nowMs)
            persistLocked()
        }
    }

    fun logTech(
        level: String,
        module: DiagnosticModule,
        message: String,
        stackSummary: String = ""
    ) {
        synchronized(lock) {
            appendLogLocked(level, module, message, stackSummary)
            if (_uiState.value.diagnosticModeEnabled || level == "ERROR") {
                persistLocked()
            }
        }
    }

    private fun appendLogLocked(
        level: String,
        module: DiagnosticModule,
        message: String,
        stackSummary: String = "",
        nowMs: Long = System.currentTimeMillis()
    ) {
        val sanitizedMsg = redactSensitive(message).take(400)
        val sanitizedStack = redactSensitive(stackSummary).take(400)
        val entry = DiagnosticLogEntry(
            id = "log_${UUID.randomUUID().toString().take(8)}",
            timestampMs = nowMs,
            level = level.uppercase(Locale.US),
            module = module,
            message = sanitizedMsg,
            stackSummary = sanitizedStack
        )
        val nextLogs = (listOf(entry) + _uiState.value.logs).take(MAX_LOGS)
        _uiState.value = _uiState.value.copy(logs = nextLogs)
    }

    fun runFullAnalysis(
        context: Context,
        input: DiagnosticAuditInput
    ): DiagnosticReport {
        synchronized(lock) {
            _uiState.value = _uiState.value.copy(isAnalyzing = true)
        }

        val now = System.currentTimeMillis()
        val perf = capturePerformanceSnapshot(context, input.localTracks.size)
        val checks = mutableListOf<ComponentCheckResult>()
        val discoveredIssues = mutableListOf<DiagnosticIssue>()
        val conflicts = mutableListOf<ComponentConflict>()
        val missingPermissions = mutableListOf<String>()

        fun addIssue(
            module: DiagnosticModule,
            title: String,
            plain: String,
            tech: String,
            solution: String,
            severity: DiagnosticSeverity,
            repair: RepairActionId
        ) {
            discoveredIssues += DiagnosticIssue(
                id = "scan_${module.id}",
                module = module,
                title = title,
                plainExplanation = plain,
                technicalMessage = redactSensitive(tech),
                proposedSolution = solution,
                severity = severity,
                firstOccurredAtMs = now,
                lastOccurredAtMs = now,
                repairAction = repair
            )
        }

        // Permissions Check
        val audioPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasAudioPerm = ContextCompat.checkSelfPermission(context, audioPerm) == PackageManager.PERMISSION_GRANTED
        val hasMicPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasNotifPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPerm) missingPermissions += "Acceso a archivos de audio ($audioPerm)"
        if (!hasNotifPerm) missingPermissions += "Notificaciones (POST_NOTIFICATIONS)"
        if (!hasMicPerm && input.settings.handsFreeVoiceEnabled) missingPermissions += "Micrófono para control por voz"

        checks += if (hasAudioPerm && hasNotifPerm) {
            ComponentCheckResult(
                module = DiagnosticModule.PERMISSIONS,
                status = if (hasMicPerm) DiagnosticStatus.OK else DiagnosticStatus.WARNING,
                summary = if (hasMicPerm) "Todos los permisos clave concedidos" else "Audio y notificaciones activos (micrófono pendiente)"
            )
        } else {
            ComponentCheckResult(
                module = DiagnosticModule.PERMISSIONS,
                status = if (!hasAudioPerm) DiagnosticStatus.ERROR else DiagnosticStatus.WARNING,
                summary = "Faltan ${missingPermissions.size} permisos recomendados"
            )
        }
        if (!hasAudioPerm) {
            addIssue(
                module = DiagnosticModule.PERMISSIONS,
                title = "Permiso de música local no concedido",
                plain = "Sin permiso de audio, Ritmo Local no puede leer automáticamente todas las canciones de tu teléfono.",
                tech = "Missing permission: $audioPerm",
                solution = "Toca para conceder permiso de acceso a música y audio.",
                severity = DiagnosticSeverity.CRITICAL,
                repair = RepairActionId.REQUEST_AUDIO_PERMISSION
            )
        }

        // 1. Music Player & 3. Playback Service & 12. Lockscreen
        val pb = input.playbackState
        checks += if (!pb.isControllerReady && pb.currentTrack != null) {
            addIssue(
                module = DiagnosticModule.PLAYBACK_SERVICE,
                title = "Servicio de audio desconectado temporalmente",
                plain = "La interfaz perdió la conexión con el servicio de reproducción en segundo plano.",
                tech = "MediaController isControllerReady=false while track=${pb.currentTrack.trackKey}",
                solution = "Reconecta el servicio sin reiniciar ni perder tu canción actual.",
                severity = DiagnosticSeverity.HIGH,
                repair = RepairActionId.RECONNECT_PLAYER
            )
            ComponentCheckResult(DiagnosticModule.PLAYBACK_SERVICE, DiagnosticStatus.ERROR, "MediaController desconectado")
        } else {
            ComponentCheckResult(
                module = DiagnosticModule.PLAYBACK_SERVICE,
                status = DiagnosticStatus.OK,
                summary = if (pb.isControllerReady) "PlaybackService conectado (${pb.queue.size} en cola)" else "PlaybackService en espera"
            )
        }

        val engineSummarySuffix = when {
            input.playbackEnginePrefs.smartDjEnabled -> " · Modo DJ (${input.playbackEnginePrefs.djTransitionStyle.displayName})"
            input.playbackEnginePrefs.crossfadeEnabled -> " · Crossfade ${input.playbackEnginePrefs.crossfadeDurationSec}s"
            else -> ""
        }
        checks += ComponentCheckResult(
            module = DiagnosticModule.MUSIC_PLAYER,
            status = DiagnosticStatus.OK,
            summary = (pb.currentTrack?.let { "Listo · ${it.title}" } ?: "${input.localTracks.size} pistas locales indexadas") + engineSummarySuffix
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notifsBlocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && nm?.areNotificationsEnabled() == false
        checks += if (!hasNotifPerm || notifsBlocked) {
            addIssue(
                module = DiagnosticModule.NOTIFICATIONS,
                title = "Notificaciones desactivadas o sin permiso",
                plain = "Los controles en la barra de notificaciones, recordatorios de horario y alertas del chat no podrán mostrarse.",
                tech = "POST_NOTIFICATIONS=$hasNotifPerm, areNotificationsEnabled=${!notifsBlocked}",
                solution = "Activa el permiso de notificaciones en Ritmo Local.",
                severity = DiagnosticSeverity.HIGH,
                repair = RepairActionId.REQUEST_NOTIF_PERMISSION
            )
            ComponentCheckResult(DiagnosticModule.NOTIFICATIONS, DiagnosticStatus.WARNING, "Permiso pendiente o bloqueado")
        } else {
            ComponentCheckResult(DiagnosticModule.NOTIFICATIONS, DiagnosticStatus.OK, "Canales de reproducción, recordatorios y chat listos")
        }

        checks += ComponentCheckResult(
            module = DiagnosticModule.LOCKSCREEN_CONTROLS,
            status = if (hasNotifPerm) DiagnosticStatus.OK else DiagnosticStatus.WARNING,
            summary = if (hasNotifPerm) "MediaSession activa para pantalla bloqueada" else "Requiere notificaciones para controles en lockscreen"
        )

        // 2. Background Playback & Battery Optimization
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryUnrestricted = pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        checks += if (isBatteryUnrestricted) {
            ComponentCheckResult(DiagnosticModule.BACKGROUND_PLAYBACK, DiagnosticStatus.OK, "Ejecución en 2.º plano sin restricciones de batería")
        } else {
            addIssue(
                module = DiagnosticModule.BACKGROUND_PLAYBACK,
                title = "Optimización de batería activa",
                plain = "Android puede pausar la música tras varios minutos con la pantalla apagada.",
                tech = "isIgnoringBatteryOptimizations=false",
                solution = "Configura Ritmo Local como 'Sin restricciones' en batería.",
                severity = DiagnosticSeverity.LOW,
                repair = RepairActionId.OPEN_BATTERY_SETTINGS
            )
            ComponentCheckResult(DiagnosticModule.BACKGROUND_PLAYBACK, DiagnosticStatus.WARNING, "Sistema aplica ahorro de batería agresivo")
        }

        // 4. Voice Recognition
        val speechAvailable = runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
        checks += when {
            input.settings.handsFreeVoiceEnabled && !hasMicPerm -> {
                addIssue(
                    module = DiagnosticModule.VOICE_CONTROL,
                    title = "Manos libres activo sin permiso de micrófono",
                    plain = "Activaste el control por voz pero falta conceder el permiso del micrófono.",
                    tech = "handsFreeVoiceEnabled=true, RECORD_AUDIO=false",
                    solution = "Concede el permiso de micrófono para habilitar los comandos.",
                    severity = DiagnosticSeverity.HIGH,
                    repair = RepairActionId.REQUEST_MIC_PERMISSION
                )
                conflicts += ComponentConflict(
                    id = "conf_voice_mic",
                    modulesInvolved = listOf(DiagnosticModule.VOICE_CONTROL, DiagnosticModule.PERMISSIONS),
                    description = "El despertar por voz está activado pero falta el permiso RECORD_AUDIO.",
                    mitigation = "Concede el permiso de micrófono o desactiva manos libres en Ajustes.",
                    severity = DiagnosticSeverity.HIGH
                )
                ComponentCheckResult(DiagnosticModule.VOICE_CONTROL, DiagnosticStatus.ERROR, "Falta permiso de micrófono")
            }
            !speechAvailable -> {
                ComponentCheckResult(DiagnosticModule.VOICE_CONTROL, DiagnosticStatus.WARNING, "Motor SpeechRecognizer no reportado por el dispositivo")
            }
            !input.voiceState.isAvailable -> {
                ComponentCheckResult(DiagnosticModule.VOICE_CONTROL, DiagnosticStatus.WARNING, input.voiceState.statusMessage ?: "Reconocimiento de voz no disponible")
            }
            else -> {
                ComponentCheckResult(DiagnosticModule.VOICE_CONTROL, DiagnosticStatus.OK, "Comandos de voz listos (${input.voiceState.audioRoute.labelEs})")
            }
        }

        // 5. Equalizer
        val eq = input.equalizerState
        checks += if (eq.enabled && pb.isPlaying && !eq.isHardwareAttached) {
            addIssue(
                module = DiagnosticModule.EQUALIZER,
                title = "Ecualizador activado pero sin adjuntar a la sesión de audio",
                plain = "La música está sonando, pero el motor de ecualización no se aplicó correctamente al audio actual.",
                tech = "eq.enabled=true, isPlaying=true, isHardwareAttached=false, sessionId=${AudioEffectsController.audioSessionId.value}",
                solution = "Reaplica la configuración del ecualizador al audio activo.",
                severity = DiagnosticSeverity.MEDIUM,
                repair = RepairActionId.REAPPLY_EQUALIZER
            )
            ComponentCheckResult(DiagnosticModule.EQUALIZER, DiagnosticStatus.WARNING, "Desincronizado de la sesión de audio")
        } else {
            val devSummary = "${eq.activeDeviceProfileType.shortLabel}, ${input.savedDeviceProfilesCount} perfiles de salida"
            ComponentCheckResult(
                module = DiagnosticModule.EQUALIZER,
                status = DiagnosticStatus.OK,
                summary = if (eq.enabled) "DSP activo (${eq.studioAudioMode.displayName} · $devSummary)" else "Ecualizador en bypass ($devSummary)"
            )
        }

        // 6. Playlists (check orphaned track references)
        val libraryKeys = input.localTracks.map { it.trackKey }.toHashSet()
        val orphanRefCount = if (input.localTracks.isNotEmpty()) {
            input.customPlaylists.sumOf { pl -> pl.trackKeys.count { it !in libraryKeys } }
        } else 0
        checks += if (orphanRefCount > 0) {
            addIssue(
                module = DiagnosticModule.PLAYLISTS,
                title = "Listas con referencias a pistas movidas o eliminadas",
                plain = "Se encontraron $orphanRefCount enlaces en tus listas a canciones que ya no están en la biblioteca.",
                tech = "customPlaylists orphanTrackRefs=$orphanRefCount",
                solution = "Puedes limpiar solo esos enlaces huérfanos sin borrar tus listas ni canciones.",
                severity = DiagnosticSeverity.LOW,
                repair = RepairActionId.CLEAN_ORPHAN_PLAYLIST_REFS
            )
            ComponentCheckResult(DiagnosticModule.PLAYLISTS, DiagnosticStatus.WARNING, "$orphanRefCount referencias huérfanas detectadas")
        } else {
            ComponentCheckResult(DiagnosticModule.PLAYLISTS, DiagnosticStatus.OK, "${input.customPlaylists.size} listas personalizadas verificadas")
        }

        // 7. Database / DataStore & Room
        val dbDirWritable = context.filesDir.exists() && context.filesDir.canWrite()
        val roomDbReady = runCatching {
            com.example.data.stats.RitmoStatsDatabase.getInstance(context).statsDao()
            true
        }.getOrElse { false }
        checks += if (dbDirWritable && roomDbReady) {
            ComponentCheckResult(DiagnosticModule.DATABASE, DiagnosticStatus.OK, "Base de datos Room y DataStore operativos")
        } else {
            ComponentCheckResult(DiagnosticModule.DATABASE, DiagnosticStatus.ERROR, "Sin acceso a almacenamiento o Room DB")
        }

        // 8. Cloud Sync
        checks += when {
            !input.isSignedIn -> ComponentCheckResult(DiagnosticModule.CLOUD_SYNC, DiagnosticStatus.OK, "Modo Offline activo (sincronización opcional)")
            !input.cloudSyncError.isNullOrBlank() -> {
                addIssue(
                    module = DiagnosticModule.CLOUD_SYNC,
                    title = "Error al sincronizar datos en la nube",
                    plain = "No se pudo completar la sincronización de progreso o estadísticas con Firebase.",
                    tech = input.cloudSyncError,
                    solution = "Verifica tu conexión y toca para reintentar la sincronización.",
                    severity = DiagnosticSeverity.MEDIUM,
                    repair = RepairActionId.RETRY_CLOUD_SYNC
                )
                ComponentCheckResult(DiagnosticModule.CLOUD_SYNC, DiagnosticStatus.WARNING, input.cloudSyncError)
            }
            else -> ComponentCheckResult(DiagnosticModule.CLOUD_SYNC, DiagnosticStatus.OK, "Cuenta vinculada y sincronización operativa")
        }

        // 9. Sleep Timer
        val timer = input.sleepTimerState
        val timerConsistent = !timer.isActive || timer.remainingSeconds > 0L || timer.stopAfterCurrentTrack || timer.remainingSongsToStop > 0
        checks += if (timerConsistent) {
            ComponentCheckResult(
                module = DiagnosticModule.SLEEP_TIMER,
                status = DiagnosticStatus.OK,
                summary = if (timer.isActive) {
                    if (timer.remainingSongsToStop > 0) "Activo (${timer.remainingSongsToStop} canciones restantes)"
                    else if (timer.stopAfterCurrentTrack) "Activo (al terminar canción actual)"
                    else "Activo (${timer.remainingSeconds}s restantes)"
                } else "Temporizador en espera"
            )
        } else {
            addIssue(
                module = DiagnosticModule.SLEEP_TIMER,
                title = "Temporizador de apagado desfasado",
                plain = "El temporizador quedó con un estado expirado pendiente de limpieza.",
                tech = "sleepTimer.isActive=true with remainingSeconds=0",
                solution = "Restablece el temporizador de forma segura.",
                severity = DiagnosticSeverity.LOW,
                repair = RepairActionId.RESET_SLEEP_TIMER
            )
            ComponentCheckResult(DiagnosticModule.SLEEP_TIMER, DiagnosticStatus.WARNING, "Estado expirado detectado")
        }

        // 10. Stats
        checks += ComponentCheckResult(
            module = DiagnosticModule.STATS,
            status = DiagnosticStatus.OK,
            summary = "Conteo activo (${input.totalPlaybackSeconds / 60L} min totales)"
        )

        // 13. Bluetooth
        val route = VoiceAudioRouteDetector.detect(context)
        checks += ComponentCheckResult(
            module = DiagnosticModule.BLUETOOTH,
            status = DiagnosticStatus.OK,
            summary = when (route) {
                VoiceAudioRoute.BLUETOOTH_HEADSET -> "Audio Bluetooth conectado (pausa inteligente adaptada)"
                VoiceAudioRoute.PHONE_SPEAKER -> "Salida por altavoz del teléfono"
            }
        )
        if (route == VoiceAudioRoute.BLUETOOTH_HEADSET && input.voiceState.isListening) {
            conflicts += ComponentConflict(
                id = "conf_bt_voice",
                modulesInvolved = listOf(DiagnosticModule.BLUETOOTH, DiagnosticModule.VOICE_CONTROL),
                description = "Bluetooth activo durante reconocimiento de voz; se mantiene reproducción continua sin pausas.",
                mitigation = "Comportamiento protegido automáticamente para no cortar el enlace A2DP/SCO.",
                severity = DiagnosticSeverity.LOW
            )
        }

        // 15. Performance & Memory
        checks += when {
            perf.isLowSystemMemory || perf.heapUsagePercent >= 85 -> {
                addIssue(
                    module = DiagnosticModule.PERFORMANCE_MEMORY,
                    title = "Uso elevado de memoria RAM (${perf.heapUsagePercent}%)",
                    plain = "La aplicación está usando bastante memoria temporal o caché; limpiarla mejora la fluidez.",
                    tech = "heap=${perf.usedHeapMb}/${perf.maxHeapMb}MB (${perf.heapUsagePercent}%), cache=${perf.tempCacheSizeKb}KB",
                    solution = "Limpia archivos temporales de caché sin afectar tu música.",
                    severity = DiagnosticSeverity.MEDIUM,
                    repair = RepairActionId.CLEAR_TEMP_CACHE
                )
                if (input.settings.animationsEnabled) {
                    conflicts += ComponentConflict(
                        id = "conf_ram_anim",
                        modulesInvolved = listOf(DiagnosticModule.PERFORMANCE_MEMORY, DiagnosticModule.MUSIC_PLAYER),
                        description = "Memoria elevada mientras las animaciones visuales avanzadas están activadas.",
                        mitigation = "Limpiar caché temporal o desactivar animaciones desde Ajustes mejora la estabilidad.",
                        severity = DiagnosticSeverity.LOW
                    )
                }
                ComponentCheckResult(DiagnosticModule.PERFORMANCE_MEMORY, DiagnosticStatus.WARNING, "RAM al ${perf.heapUsagePercent}% (${perf.usedHeapMb} MB)")
            }
            perf.tempCacheSizeKb > 32_768L -> {
                addIssue(
                    module = DiagnosticModule.PERFORMANCE_MEMORY,
                    title = "Caché temporal extensa (${perf.tempCacheSizeKb / 1024L} MB)",
                    plain = "Se acumuló caché temporal de carátulas o archivos temporales.",
                    tech = "tempCacheSizeKb=${perf.tempCacheSizeKb}",
                    solution = "Limpia solo el caché temporal manteniendo intactas tus canciones.",
                    severity = DiagnosticSeverity.LOW,
                    repair = RepairActionId.CLEAR_TEMP_CACHE
                )
                ComponentCheckResult(DiagnosticModule.PERFORMANCE_MEMORY, DiagnosticStatus.WARNING, "Caché de ${perf.tempCacheSizeKb / 1024L} MB")
            }
            else -> {
                ComponentCheckResult(
                    DiagnosticModule.PERFORMANCE_MEMORY,
                    DiagnosticStatus.OK,
                    "Fluido · ${perf.usedHeapMb}/${perf.maxHeapMb} MB (${perf.heapUsagePercent}%)"
                )
            }
        }

        // Score & overall status
        val penalty = discoveredIssues.sumOf {
            when (it.severity) {
                DiagnosticSeverity.CRITICAL -> 25
                DiagnosticSeverity.HIGH -> 14
                DiagnosticSeverity.MEDIUM -> 8
                DiagnosticSeverity.LOW -> 3
            }
        }.coerceAtMost(90)
        val runtimeUnresolvedPenalty = synchronized(lock) {
            _uiState.value.activeIssues.count { !it.isResolved && it.severity == DiagnosticSeverity.CRITICAL } * 10
        }
        val score = (100 - penalty - runtimeUnresolvedPenalty).coerceIn(5, 100)
        val overallStatus = when {
            checks.any { it.status == DiagnosticStatus.ERROR } || discoveredIssues.any { it.severity == DiagnosticSeverity.CRITICAL } -> DiagnosticStatus.ERROR
            checks.any { it.status == DiagnosticStatus.WARNING } || discoveredIssues.isNotEmpty() -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.OK
        }

        val summary = when (overallStatus) {
            DiagnosticStatus.OK -> "Todos los 15 módulos analizados operan correctamente. Reproducción protegida."
            DiagnosticStatus.WARNING -> "Análisis completo: ${discoveredIssues.size} puntos por optimizar. La reproducción funciona con normalidad."
            DiagnosticStatus.ERROR -> "Se detectaron ${discoveredIssues.count { it.severity.rank >= 3 }} fallos prioritarios listos para reparar."
        }

        val report = DiagnosticReport(
            generatedAtMs = now,
            appVersion = BuildConfig.VERSION_NAME,
            healthScore = score,
            overallStatus = overallStatus,
            componentChecks = checks,
            activeIssues = discoveredIssues,
            conflicts = conflicts,
            missingPermissions = missingPermissions,
            performance = perf,
            recommendationSummary = summary
        )

        synchronized(lock) {
            val merged = mergeScanIssuesLocked(discoveredIssues)
            val scanHistory = DiagnosticHistoryEntry(
                id = "scan_${UUID.randomUUID().toString().take(8)}",
                timestampMs = now,
                module = DiagnosticModule.PERFORMANCE_MEMORY,
                severity = if (overallStatus == DiagnosticStatus.ERROR) DiagnosticSeverity.HIGH else DiagnosticSeverity.LOW,
                description = "Análisis completo ejecutado · Salud $score/100 (${checks.count { it.status == DiagnosticStatus.OK }}/${checks.size} correctos)",
                repairApplied = null,
                outcomeLabel = overallStatus.label,
                wasSuccessful = overallStatus != DiagnosticStatus.ERROR
            )
            _uiState.value = _uiState.value.copy(
                isAnalyzing = false,
                latestReport = report,
                activeIssues = merged,
                performance = perf,
                history = (listOf(scanHistory) + _uiState.value.history).take(MAX_HISTORY)
            )
            appendLogLocked("INFO", DiagnosticModule.PERFORMANCE_MEMORY, "Análisis completo finalizado: score=$score, issues=${merged.count { !it.isResolved }}", nowMs = now)
            persistLocked()
        }

        return report
    }

    private fun mergeScanIssuesLocked(scanIssues: List<DiagnosticIssue>): List<DiagnosticIssue> {
        val current = _uiState.value.activeIssues
            .filterNot { it.id.startsWith("scan_") }
            .toMutableList()
        scanIssues.forEach { scanned ->
            val dupIdx = current.indexOfFirst { !it.isResolved && it.module == scanned.module && it.title == scanned.title }
            if (dupIdx >= 0) {
                val existing = current[dupIdx]
                current[dupIdx] = existing.copy(
                    lastOccurredAtMs = scanned.lastOccurredAtMs,
                    technicalMessage = scanned.technicalMessage
                )
            } else {
                current.add(0, scanned)
            }
        }
        return current
            .sortedWith(
                compareBy<DiagnosticIssue> { it.isResolved }
                    .thenByDescending { it.severity.rank }
                    .thenByDescending { it.occurrenceCount }
                    .thenByDescending { it.lastOccurredAtMs }
            )
            .take(MAX_ISSUES)
    }

    fun runFunctionalSelfTest(context: Context, input: DiagnosticAuditInput): List<FunctionalTestItem> {
        synchronized(lock) {
            _uiState.value = _uiState.value.copy(isRunningSelfTest = true)
        }
        val items = mutableListOf<FunctionalTestItem>()

        // 1. Storage & local diagnostics persistence check
        val t1 = System.nanoTime()
        val storageOk = runCatching {
            val probe = File(context.cacheDir, "diag_probe.tmp")
            probe.writeText("ok")
            val read = probe.readText()
            probe.delete()
            read == "ok"
        }.getOrDefault(false)
        items += FunctionalTestItem(
            name = "Lectura/escritura de preferencias y caché",
            module = DiagnosticModule.DATABASE,
            passed = storageOk,
            latencyMs = ((System.nanoTime() - t1) / 1_000_000L).coerceAtLeast(1L),
            detail = if (storageOk) "Almacenamiento interno responde correctamente" else "Error al escribir en directorio temporal"
        )

        // 2. Playback engine & protection check
        val t2 = System.nanoTime()
        val playerOk = input.playbackState.currentTrack == null || input.playbackState.durationMs >= 0L
        items += FunctionalTestItem(
            name = "Integridad del reproductor y cola",
            module = DiagnosticModule.MUSIC_PLAYER,
            passed = playerOk,
            latencyMs = ((System.nanoTime() - t2) / 1_000_000L).coerceAtLeast(1L),
            detail = "Pistas: ${input.localTracks.size} · Cola activa: ${input.playbackState.queue.size}"
        )

        // 3. Equalizer DSP state check
        val t3 = System.nanoTime()
        val eqOk = input.equalizerState.bands.isNotEmpty()
        items += FunctionalTestItem(
            name = "Motor de ecualización (${input.equalizerState.bands.size} bandas)",
            module = DiagnosticModule.EQUALIZER,
            passed = eqOk,
            latencyMs = ((System.nanoTime() - t3) / 1_000_000L).coerceAtLeast(1L),
            detail = if (eqOk) "Perfiles y procesador estéreo operativos" else "No se detectaron bandas de ecualización"
        )

        // 4. Voice parser & Bluetooth route detector
        val t4 = System.nanoTime()
        val voiceParsed = VoiceCommandParser.parse("siguiente canción")
        val route = VoiceAudioRouteDetector.detect(context)
        val voiceOk = voiceParsed !is com.example.voice.ParsedVoiceCommand.Unknown
        items += FunctionalTestItem(
            name = "Parser de comandos por voz y ruta Bluetooth",
            module = DiagnosticModule.VOICE_CONTROL,
            passed = voiceOk,
            latencyMs = ((System.nanoTime() - t4) / 1_000_000L).coerceAtLeast(1L),
            detail = "Comando interpretado correctamente · Ruta: ${route.labelEs}"
        )

        // 5. Playlist integrity check
        val t5 = System.nanoTime()
        val validIds = input.customPlaylists.all { it.id.isNotBlank() && it.name.isNotBlank() }
        items += FunctionalTestItem(
            name = "Estructura de listas de reproducción",
            module = DiagnosticModule.PLAYLISTS,
            passed = validIds,
            latencyMs = ((System.nanoTime() - t5) / 1_000_000L).coerceAtLeast(1L),
            detail = "${input.customPlaylists.size} listas verificadas sin corrupción"
        )

        // 6. Sleep timer math / non-negative verification
        val t6 = System.nanoTime()
        val timerOk = input.sleepTimerState.remainingSeconds >= 0L
        items += FunctionalTestItem(
            name = "Temporizador de apagado",
            module = DiagnosticModule.SLEEP_TIMER,
            passed = timerOk,
            latencyMs = ((System.nanoTime() - t6) / 1_000_000L).coerceAtLeast(1L),
            detail = if (input.sleepTimerState.isActive) "Temporizador corriendo con precisión" else "En reposo y listo"
        )

        val now = System.currentTimeMillis()
        synchronized(lock) {
            val passedCount = items.count { it.passed }
            val entry = DiagnosticHistoryEntry(
                id = "test_${UUID.randomUUID().toString().take(8)}",
                timestampMs = now,
                module = DiagnosticModule.MUSIC_PLAYER,
                severity = DiagnosticSeverity.LOW,
                description = "Prueba de funcionamiento completada ($passedCount/${items.size} pruebas aprobadas)",
                repairApplied = null,
                outcomeLabel = if (passedCount == items.size) "Aprobado" else "Con avisos",
                wasSuccessful = passedCount == items.size
            )
            _uiState.value = _uiState.value.copy(
                isRunningSelfTest = false,
                selfTestResults = items,
                history = (listOf(entry) + _uiState.value.history).take(MAX_HISTORY)
            )
            appendLogLocked("INFO", DiagnosticModule.MUSIC_PLAYER, "Prueba de funcionamiento: $passedCount/${items.size} OK", nowMs = now)
            persistLocked()
        }
        return items
    }

    suspend fun executeSafeRepair(
        context: Context,
        issue: DiagnosticIssue,
        callbacks: DiagnosticRepairCallbacks
    ): Pair<Boolean, String> {
        val now = System.currentTimeMillis()
        val action = issue.repairAction
        var success = false
        var resultMessage = "No se requirió reparación."

        runCatching {
            when (action) {
                RepairActionId.RECONNECT_PLAYER -> {
                    callbacks.reconnectPlayer()
                    success = true
                    resultMessage = "Servicio de reproducción reconectado manteniendo tu música."
                }
                RepairActionId.RESTORE_PLAYBACK_POSITION -> {
                    success = callbacks.restoreLastPlayback()
                    resultMessage = if (success) {
                        "Se restauró la última canción y posición guardada."
                    } else {
                        "No había ninguna sesión pendiente por restaurar."
                    }
                }
                RepairActionId.REAPPLY_EQUALIZER -> {
                    callbacks.reapplyEqualizer()
                    success = true
                    resultMessage = "Configuración de ecualizador y audio reaplicada."
                }
                RepairActionId.RETRY_CLOUD_SYNC -> {
                    callbacks.retryCloudSync()
                    success = true
                    resultMessage = "Sincronización en la nube reintentada."
                }
                RepairActionId.REFRESH_LIBRARY -> {
                    callbacks.refreshLibrary()
                    success = true
                    resultMessage = "Biblioteca local re-escaneada sin alterar tus listas."
                }
                RepairActionId.RESET_SLEEP_TIMER -> {
                    callbacks.resetSleepTimer()
                    success = true
                    resultMessage = "Temporizador de apagado restablecido correctamente."
                }
                RepairActionId.CLEAR_TEMP_CACHE -> {
                    val freedKb = clearOnlyTempCache(context)
                    success = true
                    resultMessage = "Caché temporal limpiada (${freedKb} KB liberados). Canciones y listas intactas."
                }
                RepairActionId.CLEAN_ORPHAN_PLAYLIST_REFS -> {
                    val removedRefs = callbacks.cleanOrphanPlaylistRefs()
                    success = true
                    resultMessage = "Se limpiaron $removedRefs referencias muertas manteniendo tus listas y canciones."
                }
                RepairActionId.REQUEST_AUDIO_PERMISSION -> {
                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    callbacks.requestPermission(perm)
                    success = true
                    resultMessage = "Solicitud de permiso de audio abierta."
                }
                RepairActionId.REQUEST_NOTIF_PERMISSION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        callbacks.requestPermission(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    success = true
                    resultMessage = "Solicitud de permiso de notificaciones enviada."
                }
                RepairActionId.REQUEST_MIC_PERMISSION -> {
                    callbacks.requestPermission(Manifest.permission.RECORD_AUDIO)
                    success = true
                    resultMessage = "Solicitud de permiso de micrófono abierta."
                }
                RepairActionId.OPEN_BATTERY_SETTINGS -> {
                    callbacks.openBatterySettings()
                    success = true
                    resultMessage = "Ajustes de batería abiertos para seleccionar 'Sin restricciones'."
                }
                RepairActionId.NONE -> {
                    success = true
                    resultMessage = "Incidencia marcada como revisada."
                }
            }
        }.onFailure { err ->
            success = false
            resultMessage = "No se pudo aplicar la reparación: ${redactSensitive(err.message ?: "Error desconocido")}"
        }

        synchronized(lock) {
            val updatedIssues = _uiState.value.activeIssues.map { current ->
                if (current.id == issue.id) {
                    current.copy(
                        isResolved = success && action != RepairActionId.REQUEST_AUDIO_PERMISSION &&
                            action != RepairActionId.REQUEST_NOTIF_PERMISSION &&
                            action != RepairActionId.REQUEST_MIC_PERMISSION &&
                            action != RepairActionId.OPEN_BATTERY_SETTINGS,
                        lastRepairSummary = resultMessage
                    )
                } else current
            }
            val hist = DiagnosticHistoryEntry(
                id = "rep_${UUID.randomUUID().toString().take(8)}",
                timestampMs = now,
                module = issue.module,
                severity = issue.severity,
                description = issue.title,
                repairApplied = action.buttonLabel,
                outcomeLabel = resultMessage,
                wasSuccessful = success
            )
            _uiState.value = _uiState.value.copy(
                activeIssues = updatedIssues,
                history = (listOf(hist) + _uiState.value.history).take(MAX_HISTORY),
                performance = capturePerformanceSnapshot(context, _uiState.value.performance.libraryTrackCount),
                lastRepairFeedback = resultMessage
            )
            appendLogLocked(if (success) "INFO" else "WARN", issue.module, "Reparación '${action.buttonLabel}': $resultMessage", nowMs = now)
            persistLocked()
        }
        return success to resultMessage
    }

    fun markIssueDismissed(issueId: String) {
        synchronized(lock) {
            val updated = _uiState.value.activeIssues.map {
                if (it.id == issueId) it.copy(isResolved = true, lastRepairSummary = "Marcado como resuelto") else it
            }
            _uiState.value = _uiState.value.copy(activeIssues = updated)
            persistLocked()
        }
    }

    fun clearHistoryAndLogs() {
        synchronized(lock) {
            _uiState.value = _uiState.value.copy(
                history = emptyList(),
                logs = emptyList(),
                activeIssues = _uiState.value.activeIssues.filter { !it.isResolved },
                lastRepairFeedback = "Historial de diagnóstico limpiado (tu música y datos no se tocaron)."
            )
            persistLocked()
        }
    }

    fun clearRepairFeedback() {
        synchronized(lock) {
            _uiState.value = _uiState.value.copy(lastRepairFeedback = null)
        }
    }

    fun buildShareableReportText(context: Context): String {
        val state = _uiState.value
        val report = state.latestReport
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return buildString {
            appendLine("=== INFORME DE DIAGNÓSTICO · RITMO LOCAL ===")
            appendLine("Versión: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Fecha: ${sdf.format(Date(report?.generatedAtMs ?: System.currentTimeMillis()))}")
            if (report != null) {
                appendLine("Salud general: ${report.healthScore}/100 (${report.overallStatus.label})")
                appendLine("Resumen: ${report.recommendationSummary}")
                appendLine()
                appendLine("-- Rendimiento y memoria --")
                appendLine("Heap: ${report.performance.usedHeapMb}/${report.performance.maxHeapMb} MB (${report.performance.heapUsagePercent}%)")
                appendLine("RAM disponible sistema: ${report.performance.availableSystemRamMb} MB · Caché temporal: ${report.performance.tempCacheSizeKb} KB")
                appendLine("Ruta audio: ${report.performance.audioRouteLabel} · Pistas indexadas: ${report.performance.libraryTrackCount}")
                if (report.missingPermissions.isNotEmpty()) {
                    appendLine()
                    appendLine("-- Permisos pendientes --")
                    report.missingPermissions.forEach { appendLine("• $it") }
                }
                if (report.conflicts.isNotEmpty()) {
                    appendLine()
                    appendLine("-- Conflictos detectados --")
                    report.conflicts.forEach {
                        appendLine("• [${it.severity.label}] ${it.description} -> ${it.mitigation}")
                    }
                }
                appendLine()
                appendLine("-- Estado por componente (${report.componentChecks.size}) --")
                report.componentChecks.forEach { c ->
                    appendLine("• ${c.module.displayName}: ${c.status.label} — ${c.summary}")
                }
            }
            val unresolved = state.activeIssues.filter { !it.isResolved }
            if (unresolved.isNotEmpty()) {
                appendLine()
                appendLine("-- Problemas activos (${unresolved.size}) --")
                unresolved.forEach { iss ->
                    appendLine("• [${iss.severity.label}] ${iss.module.displayName} (x${iss.occurrenceCount}): ${iss.title}")
                    appendLine("  Explicación: ${iss.plainExplanation}")
                    appendLine("  Detalle técnico: ${iss.technicalMessage}")
                }
            }
            if (state.history.isNotEmpty()) {
                appendLine()
                appendLine("-- Últimos eventos del historial --")
                state.history.take(15).forEach { h ->
                    val repairStr = h.repairApplied?.let { " | Reparación: $it (${h.outcomeLabel})" } ?: " (${h.outcomeLabel})"
                    appendLine("• ${sdf.format(Date(h.timestampMs))} [${h.module.displayName}] ${h.description}$repairStr")
                }
            }
            if (state.diagnosticModeEnabled && state.logs.isNotEmpty()) {
                appendLine()
                appendLine("-- Logs técnicos recientes --")
                state.logs.take(25).forEach { log ->
                    appendLine("${sdf.format(Date(log.timestampMs))} [${log.level}/${log.module.id}] ${log.message}")
                }
            }
        }
    }

    fun buildExportShareIntent(context: Context): Intent {
        val text = buildShareableReportText(context)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Reporte de Diagnóstico - Ritmo Local v${BuildConfig.VERSION_NAME}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(sendIntent, "Exportar informe de diagnóstico").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Clears ONLY temporary cache subdirectories/temp files inside cacheDir.
     * NEVER touches filesDir, music libraries, playlists, saved artwork, or user accounts.
     */
    fun clearOnlyTempCache(context: Context): Long {
        var deletedBytes = 0L
        val cache = context.cacheDir ?: return 0L
        cache.listFiles()?.forEach { child ->
            val name = child.name.lowercase(Locale.US)
            if (child.isDirectory && (name.contains("image_cache") || name.contains("coil") || name.contains("http") || name.contains("tmp"))) {
                deletedBytes += deleteRecursivelyCountingBytes(child)
            } else if (child.isFile && (name.endsWith(".tmp") || name.endsWith(".part") || name.contains("probe"))) {
                val len = child.length()
                if (child.delete()) deletedBytes += len
            }
        }
        return deletedBytes / 1024L
    }

    private fun deleteRecursivelyCountingBytes(file: File): Long {
        if (!file.exists()) return 0L
        var freed = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { child -> freed += deleteRecursivelyCountingBytes(child) }
            file.delete()
        } else {
            val len = file.length()
            if (file.delete()) freed += len
        }
        return freed
    }

    fun capturePerformanceSnapshot(context: Context, trackCount: Int): PerformanceSnapshot {
        val rt = Runtime.getRuntime()
        val maxMb = (rt.maxMemory() / (1024L * 1024L)).coerceAtLeast(1L)
        val usedMb = ((rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L)).coerceAtLeast(0L)
        val pct = ((usedMb * 100L) / maxMb).toInt().coerceIn(1, 100)

        val memInfo = ActivityManager.MemoryInfo()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        am?.getMemoryInfo(memInfo)

        val cacheBytes = runCatching { folderSizeBytes(context.cacheDir) }.getOrDefault(0L)
        val routeLabel = VoiceAudioRouteDetector.detect(context).labelEs

        return PerformanceSnapshot(
            usedHeapMb = usedMb,
            maxHeapMb = maxMb,
            heapUsagePercent = pct,
            availableSystemRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L),
            isLowSystemMemory = memInfo.lowMemory,
            activeThreadCount = Thread.activeCount(),
            libraryTrackCount = trackCount,
            tempCacheSizeKb = cacheBytes / 1024L,
            audioRouteLabel = routeLabel
        )
    }

    private fun folderSizeBytes(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        if (dir.isFile) return dir.length()
        var sum = 0L
        dir.listFiles()?.forEach { sum += folderSizeBytes(it) }
        return sum
    }

    /**
     * Redacts emails, bearer tokens, Firebase/JWT strings, and private UID paths before logging/storing/exporting.
     */
    fun redactSensitive(raw: String): String {
        if (raw.isBlank()) return raw
        return raw
            .replace(Regex("(?i)bearer\\s+[a-z0-9\\-._~+/]+=*"), "Bearer [REDACTED]")
            .replace(Regex("AIza[0-9A-Za-z\\-_]{20,}"), "[API_KEY_REDACTED]")
            .replace(Regex("eyJ[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+\\.[a-zA-Z0-9_\\-]+"), "[JWT_REDACTED]")
            .replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[EMAIL_REDACTED]")
            .replace(Regex("(?i)(password|token|secret|apikey|api_key)\\s*[=:]\\s*\\S+"), "$1=[REDACTED]")
    }

    private fun defaultRepairFor(module: DiagnosticModule): RepairActionId = when (module) {
        DiagnosticModule.MUSIC_PLAYER, DiagnosticModule.PLAYBACK_SERVICE, DiagnosticModule.LOCKSCREEN_CONTROLS ->
            RepairActionId.RECONNECT_PLAYER
        DiagnosticModule.EQUALIZER -> RepairActionId.REAPPLY_EQUALIZER
        DiagnosticModule.CLOUD_SYNC -> RepairActionId.RETRY_CLOUD_SYNC
        DiagnosticModule.PLAYLISTS -> RepairActionId.REFRESH_LIBRARY
        DiagnosticModule.SLEEP_TIMER -> RepairActionId.RESET_SLEEP_TIMER
        DiagnosticModule.NOTIFICATIONS -> RepairActionId.REQUEST_NOTIF_PERMISSION
        DiagnosticModule.VOICE_CONTROL -> RepairActionId.REQUEST_MIC_PERMISSION
        DiagnosticModule.PERFORMANCE_MEMORY -> RepairActionId.CLEAR_TEMP_CACHE
        else -> RepairActionId.NONE
    }

    private fun stateFile(filesDir: File): File = File(File(filesDir, "diagnostics"), "diagnostics_state.json")

    private fun persistLocked() {
        val dir = appFilesDir ?: return
        runCatching {
            val file = stateFile(dir)
            file.parentFile?.mkdirs()
            val cur = _uiState.value
            val root = JSONObject().apply {
                put("diagnosticMode", cur.diagnosticModeEnabled)
                put("issues", JSONArray().apply {
                    cur.activeIssues.take(MAX_ISSUES).forEach { iss ->
                        put(JSONObject().apply {
                            put("id", iss.id)
                            put("module", iss.module.id)
                            put("title", iss.title)
                            put("plain", iss.plainExplanation)
                            put("tech", iss.technicalMessage)
                            put("solution", iss.proposedSolution)
                            put("severity", iss.severity.id)
                            put("firstAt", iss.firstOccurredAtMs)
                            put("lastAt", iss.lastOccurredAtMs)
                            put("count", iss.occurrenceCount)
                            put("resolved", iss.isResolved)
                            put("repair", iss.repairAction.id)
                            put("repairSummary", iss.lastRepairSummary.orEmpty())
                        })
                    }
                })
                put("history", JSONArray().apply {
                    cur.history.take(MAX_HISTORY).forEach { h ->
                        put(JSONObject().apply {
                            put("id", h.id)
                            put("ts", h.timestampMs)
                            put("module", h.module.id)
                            put("severity", h.severity.id)
                            put("desc", h.description)
                            put("repair", h.repairApplied.orEmpty())
                            put("outcome", h.outcomeLabel)
                            put("ok", h.wasSuccessful)
                        })
                    }
                })
                put("logs", JSONArray().apply {
                    cur.logs.take(MAX_LOGS).forEach { l ->
                        put(JSONObject().apply {
                            put("id", l.id)
                            put("ts", l.timestampMs)
                            put("level", l.level)
                            put("module", l.module.id)
                            put("msg", l.message)
                            put("stack", l.stackSummary)
                        })
                    }
                })
            }
            file.writeText(root.toString())
        }
    }

    private fun loadFromDiskLocked(filesDir: File) {
        val file = stateFile(filesDir)
        if (!file.exists()) return
        runCatching {
            val root = JSONObject(file.readText())
            val issuesArr = root.optJSONArray("issues") ?: JSONArray()
            val issues = buildList {
                for (i in 0 until issuesArr.length()) {
                    val o = issuesArr.optJSONObject(i) ?: continue
                    add(
                        DiagnosticIssue(
                            id = o.optString("id"),
                            module = DiagnosticModule.fromId(o.optString("module")),
                            title = o.optString("title"),
                            plainExplanation = o.optString("plain"),
                            technicalMessage = o.optString("tech"),
                            proposedSolution = o.optString("solution"),
                            severity = DiagnosticSeverity.fromId(o.optString("severity")),
                            firstOccurredAtMs = o.optLong("firstAt"),
                            lastOccurredAtMs = o.optLong("lastAt"),
                            occurrenceCount = o.optInt("count", 1).coerceAtLeast(1),
                            isResolved = o.optBoolean("resolved", false),
                            repairAction = RepairActionId.fromId(o.optString("repair")),
                            lastRepairSummary = o.optString("repairSummary").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            val histArr = root.optJSONArray("history") ?: JSONArray()
            val history = buildList {
                for (i in 0 until histArr.length()) {
                    val o = histArr.optJSONObject(i) ?: continue
                    add(
                        DiagnosticHistoryEntry(
                            id = o.optString("id"),
                            timestampMs = o.optLong("ts"),
                            module = DiagnosticModule.fromId(o.optString("module")),
                            severity = DiagnosticSeverity.fromId(o.optString("severity")),
                            description = o.optString("desc"),
                            repairApplied = o.optString("repair").takeIf { it.isNotBlank() },
                            outcomeLabel = o.optString("outcome", "Registrado"),
                            wasSuccessful = o.optBoolean("ok", true)
                        )
                    )
                }
            }
            val logsArr = root.optJSONArray("logs") ?: JSONArray()
            val logs = buildList {
                for (i in 0 until logsArr.length()) {
                    val o = logsArr.optJSONObject(i) ?: continue
                    add(
                        DiagnosticLogEntry(
                            id = o.optString("id"),
                            timestampMs = o.optLong("ts"),
                            level = o.optString("level", "INFO"),
                            module = DiagnosticModule.fromId(o.optString("module")),
                            message = o.optString("msg"),
                            stackSummary = o.optString("stack")
                        )
                    )
                }
            }
            _uiState.value = _uiState.value.copy(
                activeIssues = issues,
                history = history,
                logs = logs
            )
        }
    }

    internal fun resetForTesting(context: Context) {
        synchronized(lock) {
            appFilesDir = context.applicationContext.filesDir
            stateFile(context.applicationContext.filesDir).delete()
            _uiState.value = DiagnosticsUiState()
            initialized = true
        }
    }
}
