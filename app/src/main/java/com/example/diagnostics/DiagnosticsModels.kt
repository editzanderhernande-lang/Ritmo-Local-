package com.example.diagnostics

import androidx.compose.ui.graphics.Color

enum class DiagnosticSeverity(
    val id: String,
    val label: String,
    val rank: Int
) {
    CRITICAL("critical", "Crítico", 4),
    HIGH("high", "Alto", 3),
    MEDIUM("medium", "Medio", 2),
    LOW("low", "Bajo", 1);

    companion object {
        fun fromId(id: String?): DiagnosticSeverity =
            entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

enum class DiagnosticStatus(val id: String, val label: String) {
    OK("ok", "Correcto"),
    WARNING("warning", "Advertencia"),
    ERROR("error", "Con error");

    companion object {
        fun fromId(id: String?): DiagnosticStatus =
            entries.firstOrNull { it.id == id } ?: OK
    }
}

enum class DiagnosticModule(
    val id: String,
    val displayName: String,
    val categoryLabel: String
) {
    MUSIC_PLAYER("music_player", "Reproductor de música", "Audio"),
    BACKGROUND_PLAYBACK("background_playback", "Reproducción en 2.º plano", "Servicios"),
    PLAYBACK_SERVICE("playback_service", "Servicio de reproducción", "Servicios"),
    VOICE_CONTROL("voice_control", "Reconocimiento de voz", "Voz"),
    EQUALIZER("equalizer", "Ecualizador y Dolby", "Audio"),
    PLAYLISTS("playlists", "Listas de reproducción", "Biblioteca"),
    DATABASE("database", "Base de datos local", "Datos"),
    CLOUD_SYNC("cloud_sync", "Sincronización en la nube", "Nube"),
    SLEEP_TIMER("sleep_timer", "Temporizador de apagado", "Utilidades"),
    STATS("stats", "Estadísticas musicales", "Datos"),
    NOTIFICATIONS("notifications", "Notificaciones", "Sistema"),
    LOCKSCREEN_CONTROLS("lockscreen_controls", "Controles en pantalla bloqueada", "Audio"),
    BLUETOOTH("bluetooth", "Conexiones Bluetooth", "Hardware"),
    PERMISSIONS("permissions", "Permisos de Android", "Sistema"),
    PERFORMANCE_MEMORY("performance_memory", "Memoria y rendimiento", "Sistema");

    companion object {
        fun fromId(id: String?): DiagnosticModule =
            entries.firstOrNull { it.id == id } ?: MUSIC_PLAYER
    }
}

enum class RepairActionId(
    val id: String,
    val buttonLabel: String,
    val requiresConfirmation: Boolean,
    val confirmationMessage: String? = null
) {
    NONE("none", "Sin acción", false, null),
    RECONNECT_PLAYER("reconnect_player", "Reconectar servicio de audio", false, null),
    RESTORE_PLAYBACK_POSITION("restore_playback", "Restaurar última posición", false, null),
    REAPPLY_EQUALIZER("reapply_eq", "Reaplicar ecualizador", false, null),
    RETRY_CLOUD_SYNC("retry_cloud", "Reintentar sincronización", false, null),
    REFRESH_LIBRARY("refresh_library", "Re-escanear biblioteca", false, null),
    RESET_SLEEP_TIMER("reset_sleep_timer", "Restablecer temporizador", false, null),
    REQUEST_AUDIO_PERMISSION("request_audio_perm", "Conceder permiso de audio", false, null),
    REQUEST_NOTIF_PERMISSION("request_notif_perm", "Activar permiso de notificaciones", false, null),
    REQUEST_MIC_PERMISSION("request_mic_perm", "Conceder permiso de micrófono", false, null),
    OPEN_BATTERY_SETTINGS("open_battery_settings", "Abrir ajustes de batería", false, null),
    CLEAR_TEMP_CACHE(
        id = "clear_temp_cache",
        buttonLabel = "Limpiar caché temporal",
        requiresConfirmation = true,
        confirmationMessage = "Se eliminarán únicamente archivos temporales y caché de portadas antiguos. Tus canciones, listas y cuenta NO se verán afectadas."
    ),
    CLEAN_ORPHAN_PLAYLIST_REFS(
        id = "clean_playlist_refs",
        buttonLabel = "Reparar listas dañadas",
        requiresConfirmation = true,
        confirmationMessage = "Se limpiarán únicamente referencias a pistas que ya no existen en el dispositivo. Tus listas y canciones actuales permanecerán intactas."
    );

    companion object {
        fun fromId(id: String?): RepairActionId =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

data class DiagnosticIssue(
    val id: String,
    val module: DiagnosticModule,
    val title: String,
    val plainExplanation: String,
    val technicalMessage: String,
    val proposedSolution: String,
    val severity: DiagnosticSeverity,
    val firstOccurredAtMs: Long,
    val lastOccurredAtMs: Long,
    val occurrenceCount: Int = 1,
    val isResolved: Boolean = false,
    val repairAction: RepairActionId = RepairActionId.NONE,
    val lastRepairSummary: String? = null
)

data class ComponentCheckResult(
    val module: DiagnosticModule,
    val status: DiagnosticStatus,
    val summary: String,
    val detail: String = "",
    val checkedAtMs: Long = System.currentTimeMillis()
)

data class PerformanceSnapshot(
    val usedHeapMb: Long = 0L,
    val maxHeapMb: Long = 0L,
    val heapUsagePercent: Int = 0,
    val availableSystemRamMb: Long = 0L,
    val isLowSystemMemory: Boolean = false,
    val activeThreadCount: Int = 0,
    val libraryTrackCount: Int = 0,
    val tempCacheSizeKb: Long = 0L,
    val audioRouteLabel: String = "Altavoz",
    val capturedAtMs: Long = System.currentTimeMillis()
)

data class ComponentConflict(
    val id: String,
    val modulesInvolved: List<DiagnosticModule>,
    val description: String,
    val mitigation: String,
    val severity: DiagnosticSeverity
)

data class DiagnosticHistoryEntry(
    val id: String,
    val timestampMs: Long,
    val module: DiagnosticModule,
    val severity: DiagnosticSeverity,
    val description: String,
    val repairApplied: String? = null,
    val outcomeLabel: String = "Registrado",
    val wasSuccessful: Boolean = true
)

data class DiagnosticLogEntry(
    val id: String,
    val timestampMs: Long,
    val level: String, // INFO, WARN, ERROR
    val module: DiagnosticModule,
    val message: String,
    val stackSummary: String = ""
)

data class FunctionalTestItem(
    val name: String,
    val module: DiagnosticModule,
    val passed: Boolean,
    val latencyMs: Long,
    val detail: String
)

data class PlaybackProtectionSnapshot(
    val isProtected: Boolean = true,
    val guardedErrorsCount: Int = 0,
    val lastProtectedTrackTitle: String? = null,
    val lastProtectedTrackKey: String? = null,
    val lastProtectedPositionMs: Long = 0L,
    val navigationCountWithoutInterrupt: Int = 0,
    val lastGuardNote: String = "Escudo activo: la navegación y los ajustes no detienen tu música."
)

data class DiagnosticReport(
    val generatedAtMs: Long = System.currentTimeMillis(),
    val appVersion: String = "2.3",
    val healthScore: Int = 100,
    val overallStatus: DiagnosticStatus = DiagnosticStatus.OK,
    val componentChecks: List<ComponentCheckResult> = emptyList(),
    val activeIssues: List<DiagnosticIssue> = emptyList(),
    val conflicts: List<ComponentConflict> = emptyList(),
    val missingPermissions: List<String> = emptyList(),
    val performance: PerformanceSnapshot = PerformanceSnapshot(),
    val recommendationSummary: String = "Todo funciona correctamente."
)

data class DiagnosticsUiState(
    val isAnalyzing: Boolean = false,
    val isRunningSelfTest: Boolean = false,
    val diagnosticModeEnabled: Boolean = false,
    val latestReport: DiagnosticReport? = null,
    val activeIssues: List<DiagnosticIssue> = emptyList(),
    val history: List<DiagnosticHistoryEntry> = emptyList(),
    val logs: List<DiagnosticLogEntry> = emptyList(),
    val selfTestResults: List<FunctionalTestItem> = emptyList(),
    val performance: PerformanceSnapshot = PerformanceSnapshot(),
    val playbackProtection: PlaybackProtectionSnapshot = PlaybackProtectionSnapshot(),
    val lastRepairFeedback: String? = null
)

fun DiagnosticSeverity.accentColor(): Color = when (this) {
    DiagnosticSeverity.CRITICAL -> Color(0xFFEF4444)
    DiagnosticSeverity.HIGH -> Color(0xFFF97316)
    DiagnosticSeverity.MEDIUM -> Color(0xFFF59E0B)
    DiagnosticSeverity.LOW -> Color(0xFF38BDF8)
}

fun DiagnosticStatus.accentColor(): Color = when (this) {
    DiagnosticStatus.OK -> Color(0xFF10B981)
    DiagnosticStatus.WARNING -> Color(0xFFF59E0B)
    DiagnosticStatus.ERROR -> Color(0xFFEF4444)
}
