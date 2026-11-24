package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class TimeOfDaySlot(
    val wireValue: String,
    val displayName: String,
    val timeRangeLabel: String,
    val description: String
) {
    MORNING(
        wireValue = "MORNING",
        displayName = "Mañana",
        timeRangeLabel = "05:00 – 11:59",
        description = "Ritmos luminosos y energía fresca para comenzar el día"
    ),
    AFTERNOON(
        wireValue = "AFTERNOON",
        displayName = "Tarde",
        timeRangeLabel = "12:00 – 18:59",
        description = "Mezcla equilibrada y dinámica para mantener el pulso"
    ),
    NIGHT(
        wireValue = "NIGHT",
        displayName = "Noche",
        timeRangeLabel = "19:00 – 04:59",
        description = "Atmósfera envolvente y cortes profundos para desconectar"
    );

    companion object {
        fun fromHour(hourOfDay: Int): TimeOfDaySlot = when (hourOfDay) {
            in 5..11 -> MORNING
            in 12..18 -> AFTERNOON
            else -> NIGHT
        }

        fun current(): TimeOfDaySlot {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return fromHour(hour)
        }

        fun fromWire(raw: String?): TimeOfDaySlot =
            entries.firstOrNull { it.wireValue == raw } ?: MORNING
    }
}

enum class AppAccentPalette(
    val id: String,
    val displayName: String,
    val primaryDark: Color,
    val secondaryDark: Color,
    val primaryLight: Color,
    val secondaryLight: Color
) {
    NEON_VIOLET(
        id = "neon_violet",
        displayName = "Neón Púrpura",
        primaryDark = Color(0xFF9D6BFF),
        secondaryDark = Color(0xFFD946EF),
        primaryLight = Color(0xFF7C3AED),
        secondaryLight = Color(0xFFC026D3)
    ),
    AMBER_VINYL(
        id = "amber_vinyl",
        displayName = "Ámbar Vinilo",
        primaryDark = Color(0xFFF5A623),
        secondaryDark = Color(0xFFFF6B4A),
        primaryLight = Color(0xFFD97706),
        secondaryLight = Color(0xFFEA580C)
    ),
    INDIGO_NOCTURNE(
        id = "indigo_nocturne",
        displayName = "Índigo Nocturno",
        primaryDark = Color(0xFF818CF8),
        secondaryDark = Color(0xFF38BDF8),
        primaryLight = Color(0xFF4F46E5),
        secondaryLight = Color(0xFF0284C7)
    ),
    EMERALD_STUDIO(
        id = "emerald_studio",
        displayName = "Esmeralda Studio",
        primaryDark = Color(0xFF34D399),
        secondaryDark = Color(0xFF2DD4BF),
        primaryLight = Color(0xFF059669),
        secondaryLight = Color(0xFF0D9488)
    ),
    CORAL_NEON(
        id = "coral_neon",
        displayName = "Coral Neón",
        primaryDark = Color(0xFFFB7185),
        secondaryDark = Color(0xFFF59E0B),
        primaryLight = Color(0xFFE11D48),
        secondaryLight = Color(0xFFD97706)
    ),
    CYAN_HIFI(
        id = "cyan_hifi",
        displayName = "Cian Hi-Fi",
        primaryDark = Color(0xFF22D3EE),
        secondaryDark = Color(0xFFA78BFA),
        primaryLight = Color(0xFF0891B2),
        secondaryLight = Color(0xFF7C3AED)
    ),
    RUBY_ANALOG(
        id = "ruby_analog",
        displayName = "Rubí Analógico",
        primaryDark = Color(0xFFF87171),
        secondaryDark = Color(0xFFFBBF24),
        primaryLight = Color(0xFFDC2626),
        secondaryLight = Color(0xFFD97706)
    ),
    GRAPHITE_MINIMAL(
        id = "graphite_minimal",
        displayName = "Grafito Minimal",
        primaryDark = Color(0xFFE2E8F0),
        secondaryDark = Color(0xFF94A3B8),
        primaryLight = Color(0xFF1E293B),
        secondaryLight = Color(0xFF475569)
    ),
    SAGE_CALM(
        id = "sage_calm",
        displayName = "Salvia Zen",
        primaryDark = Color(0xFFA7F3D0),
        secondaryDark = Color(0xFF6EE7B7),
        primaryLight = Color(0xFF047857),
        secondaryLight = Color(0xFF10B981)
    );

    companion object {
        fun fromId(id: String?): AppAccentPalette =
            entries.firstOrNull { it.id == id } ?: NEON_VIOLET
    }
}

enum class UiStyleMode(val id: String, val displayName: String) {
    NEON_MODERN("neon_modern", "Moderno Neón"),
    MINIMALIST("minimalist", "Minimalista"),
    ORIGINAL("original", "Original");

    companion object {
        fun fromId(id: String?): UiStyleMode =
            entries.firstOrNull { it.id == id } ?: NEON_MODERN
    }
}

enum class AppFontStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    SPACE_JAKARTA(
        id = "space_jakarta",
        displayName = "Moderna (Space Grotesk)",
        description = "Títulos geométricos + lectura limpia"
    ),
    OUTFIT_ROUNDED(
        id = "outfit_rounded",
        displayName = "Futurista (Outfit)",
        description = "Redondeada y luminosa"
    ),
    PLAYFAIR_EDITORIAL(
        id = "playfair_editorial",
        displayName = "Editorial (Playfair)",
        description = "Serif clásica de revista + Inter"
    ),
    MONTSERRAT_URBAN(
        id = "montserrat_urban",
        displayName = "Urbana (Montserrat)",
        description = "Fuerte contraste y presencia"
    ),
    POPPINS_SOFT(
        id = "poppins_soft",
        displayName = "Suave (Poppins)",
        description = "Cálida y equilibrada"
    ),
    JETBRAINS_TECH(
        id = "jetbrains_tech",
        displayName = "Estudio Hi-Fi (Mono)",
        description = "Estética técnica de consola"
    );

    companion object {
        fun fromId(id: String?): AppFontStyle =
            entries.firstOrNull { it.id == id } ?: SPACE_JAKARTA
    }
}

enum class StudioAudioMode(
    val id: String,
    val displayName: String,
    val description: String,
    val subLiftDb: Float,
    val airLiftDb: Float,
    val warmMidDb: Float,
    val mbcRatio: Float
) {
    HIFI_MASTER(
        id = "hifi_master",
        displayName = "Hi-Fi Studio 32-bit",
        description = "Claridad cristalina, rango dinámico balanceado y graves profundos sin saturar",
        subLiftDb = 2.0f,
        airLiftDb = 2.6f,
        warmMidDb = 0.6f,
        mbcRatio = 1.65f
    ),
    ULTRA_CLARITY(
        id = "ultra_clarity",
        displayName = "Ultra Claridad HD",
        description = "Separa voces e instrumentos con nitidez máxima y brillo espacial",
        subLiftDb = 1.0f,
        airLiftDb = 3.8f,
        warmMidDb = 1.8f,
        mbcRatio = 1.9f
    ),
    WARM_ANALOG(
        id = "warm_analog",
        displayName = "Vinilo Cálido",
        description = "Cuerpo analógico cálido y golpe suave tipo amplificador de tubos",
        subLiftDb = 3.0f,
        airLiftDb = 1.2f,
        warmMidDb = 1.5f,
        mbcRatio = 1.45f
    ),
    PURE_DIRECT(
        id = "pure_direct",
        displayName = "Directo Puro",
        description = "Señal de 32-bit sin compresión adicional con limitador anti-clipping",
        subLiftDb = 0f,
        airLiftDb = 0f,
        warmMidDb = 0f,
        mbcRatio = 1.0f
    );

    companion object {
        fun fromId(id: String?): StudioAudioMode =
            entries.firstOrNull { it.id == id } ?: HIFI_MASTER
    }
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flagEmoji: String
) {
    ES_LATAM("es-419", "Español (Latinoamérica)", "🌎"),
    ES_ES("es-ES", "Español (Castellano)", "🇪🇸"),
    EN("en", "English", "🇺🇸"),
    JA("ja", "日本語 (Japonés)", "🇯🇵"),
    ZH("zh", "中文 (Chino)", "🇨🇳"),
    ID("id", "Bahasa Indonesia", "🇮🇩"),
    HI("hi", "हिन्दी (India)", "🇮🇳"),
    TR("tr", "Türkçe (Turquía)", "🇹🇷");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ES_LATAM
    }
}

enum class Spatial3dPreset(
    val id: String,
    val displayName: String,
    val virtualizerStrength: Int,
    val clarityBoostMb: Int
) {
    MUSIC_3D("music_3d", "Música Espacial", 650, 220),
    CINEMA_3D("cinema_3d", "Cine Atmos 3D", 900, 320),
    CRYSTAL_VOICE("crystal_voice", "Voz Cristalina", 450, 480),
    STADIUM("stadium", "Estadio Envolvente", 1000, 180),
    CUSTOM("custom", "Personalizado", 500, 150);

    companion object {
        fun fromId(id: String?): Spatial3dPreset =
            entries.firstOrNull { it.id == id } ?: MUSIC_3D
    }
}

enum class HearingProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val bandOffsetsMb: List<Int>,
    val loudnessGainMb: Int
) {
    STANDARD(
        id = "standard",
        displayName = "Estándar",
        description = "Sin compensación auditiva adicional",
        bandOffsetsMb = listOf(0, 0, 0, 0, 0),
        loudnessGainMb = 0
    ),
    HIGH_FREQ_LOSS(
        id = "high_freq_loss",
        displayName = "Claridad en agudos",
        description = "Realza frecuencias medias-altas y detalle vocal",
        bandOffsetsMb = listOf(0, 120, 280, 520, 680),
        loudnessGainMb = 250
    ),
    LOW_SENSITIVITY(
        id = "low_sensitivity",
        displayName = "Baja sensibilidad",
        description = "Aumento suave y equilibrado de volumen y presencia",
        bandOffsetsMb = listOf(220, 260, 320, 320, 240),
        loudnessGainMb = 450
    ),
    TINNITUS_COMFORT(
        id = "tinnitus_comfort",
        displayName = "Confort (menos fatiga)",
        description = "Suaviza picos agudos para escucha prolongada sin molestia",
        bandOffsetsMb = listOf(180, 120, 0, -280, -450),
        loudnessGainMb = 100
    ),
    LOW_FREQ_LOSS(
        id = "low_freq_loss",
        displayName = "Refuerzo en graves",
        description = "Compensa caída en frecuencias graves y cuerpo cálido",
        bandOffsetsMb = listOf(620, 420, 150, 0, 0),
        loudnessGainMb = 200
    );

    companion object {
        fun fromId(id: String?): HearingProfile =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}

enum class AudioChannelMode(val id: String) {
    STEREO("stereo"),
    MONO("mono"),
    BALANCED("balanced");

    companion object {
        fun fromId(id: String?): AudioChannelMode =
            entries.firstOrNull { it.id == id } ?: STEREO
    }
}

enum class AudioDeviceProfileType(
    val id: String,
    val displayName: String,
    val shortLabel: String
) {
    PHONE_SPEAKER("speaker", "Altavoz del teléfono", "Altavoz"),
    WIRED_HEADSET("wired", "Auriculares con cable", "Cable"),
    BLUETOOTH("bluetooth", "Auriculares / Altavoz Bluetooth", "Bluetooth"),
    OTHER_OUTPUT("other", "Otro dispositivo de salida", "Otro");

    companion object {
        fun fromId(id: String?): AudioDeviceProfileType =
            entries.firstOrNull { it.id == id } ?: PHONE_SPEAKER
    }
}

data class DeviceSoundProfile(
    val deviceType: AudioDeviceProfileType,
    val enabled: Boolean = true,
    val presetIndex: Int = 0,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val trebleBoostMb: Int = 0,
    val spatial3dEnabled: Boolean = false,
    val spatial3dStrength: Int = 650,
    val clarityBoostMb: Int = 220,
    val balanceLR: Float = 0f,
    val updatedAtMs: Long = System.currentTimeMillis()
)

enum class ChatRegion(val id: String, val displayName: String) {
    LATAM("LATAM", "Latinoamérica"),
    ESPANA("ESPANA", "España"),
    NORTEAMERICA("NORTEAMERICA", "Norteamérica"),
    EUROPA("EUROPA", "Europa"),
    ASIA("ASIA", "Asia"),
    GLOBAL("GLOBAL", "Global");

    companion object {
        fun fromId(id: String?): ChatRegion =
            entries.firstOrNull { it.id == id } ?: LATAM
    }
}

enum class PlayerVisualStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    GLASS_AURORA(
        id = "glass_aurora",
        displayName = "Cristal Aurora",
        description = "Tarjeta translúcida con halo ambiental de portada"
    ),
    VINYL_STUDIO(
        id = "vinyl_studio",
        displayName = "Vinilo Hi-Fi",
        description = "Disco giratorio elegante y reflejo analógico"
    ),
    MINIMAL_ZEN(
        id = "minimal_zen",
        displayName = "Minimalista Zen",
        description = "Portada protagonista y tipografía depurada"
    ),
    NEON_PULSE(
        id = "neon_pulse",
        displayName = "Pulso Neón",
        description = "Bordes luminosos y espectro vibrante"
    );

    companion object {
        fun fromId(id: String?): PlayerVisualStyle =
            entries.firstOrNull { it.id == id } ?: GLASS_AURORA
    }
}

enum class PlayerBackgroundMode(
    val id: String,
    val displayName: String
) {
    DYNAMIC_COVER("dynamic_cover", "Dinámico (Portada)"),
    GRADIENT_CUSTOM("gradient_custom", "Degradado"),
    DARK_SOLID("dark_solid", "Oscuro profundo"),
    LIGHT_CLEAN("light_clean", "Claro luminoso");

    companion object {
        fun fromId(id: String?): PlayerBackgroundMode =
            entries.firstOrNull { it.id == id } ?: DYNAMIC_COVER
    }
}

data class PlayerColorSwatch(
    val id: String,
    val label: String,
    val colorArgb: Long
)

val PLAYER_COLOR_SWATCHES = listOf(
    PlayerColorSwatch("theme", "Tema app", 0L),
    PlayerColorSwatch("violet", "Violeta", 0xFF9D6BFF),
    PlayerColorSwatch("amber", "Ámbar", 0xFFF5A623),
    PlayerColorSwatch("cyan", "Cian", 0xFF22D3EE),
    PlayerColorSwatch("emerald", "Esmeralda", 0xFF34D399),
    PlayerColorSwatch("coral", "Coral", 0xFFFB7185),
    PlayerColorSwatch("indigo", "Índigo", 0xFF818CF8),
    PlayerColorSwatch("gold", "Dorado", 0xFFFBBF24)
)

enum class VisualizerStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    BARS("bars", "Barras", "Barras rítmicas sincronizadas con graves y agudos"),
    WAVES("waves", "Ondas", "Curva continua modulada por el flujo musical"),
    CIRCLES("circles", "Círculos", "Ondas radiales concéntricas al pulso"),
    SPECTRUM("spectrum", "Espectro", "Analizador dual simétrico de estudio");

    companion object {
        fun fromId(id: String?): VisualizerStyle =
            entries.firstOrNull { it.id == id } ?: BARS
    }
}

enum class CrossfadeCurve(
    val id: String,
    val displayName: String
) {
    EQUAL_POWER("equal_power", "Potencia constante (DJ)"),
    SMOOTH_S("smooth_s", "Curva S cinematográfica"),
    LINEAR("linear", "Lineal clásico");

    companion object {
        fun fromId(id: String?): CrossfadeCurve =
            entries.firstOrNull { it.id == id } ?: EQUAL_POWER
    }
}

enum class DjTransitionStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    SMOOTH_CLUB("smooth_club", "Club Suave", "Transición larga equilibrando ritmo y volumen"),
    AMBIENT_FLOW("ambient_flow", "Flujo Ambiental", "Mezcla extendida ultra progresiva sin cortes"),
    QUICK_CUT("quick_cut", "Corte Rápido", "Entrada directa saltando silencios largos");

    companion object {
        fun fromId(id: String?): DjTransitionStyle =
            entries.firstOrNull { it.id == id } ?: SMOOTH_CLUB
    }
}

data class PlaybackEnginePrefs(
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationSec: Int = 5,
    val crossfadeCurve: CrossfadeCurve = CrossfadeCurve.EQUAL_POWER,
    val fadeInEnabled: Boolean = true,
    val fadeOutEnabled: Boolean = true,
    val fadeDurationMs: Int = 650,
    val smartDjEnabled: Boolean = false,
    val djTransitionStyle: DjTransitionStyle = DjTransitionStyle.SMOOTH_CLUB,
    val djSkipSilenceEnabled: Boolean = true,
    val djVolumeNormalization: Boolean = true,
    val avoidQueueRepeats: Boolean = true
)

data class SavedQueuePreset(
    val id: String,
    val name: String,
    val trackKeys: List<String>,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class PlayerVisualPrefs(
    val style: PlayerVisualStyle = PlayerVisualStyle.GLASS_AURORA,
    val backgroundMode: PlayerBackgroundMode = PlayerBackgroundMode.DYNAMIC_COVER,
    val customPrimaryColorArgb: Long = 0L,
    val customButtonColorArgb: Long = 0L,
    val showVisualizer: Boolean = true,
    val showInlineLyricsBar: Boolean = true,
    val visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    val visualizerIntensity: Float = 1.0f,
    val blurIntensity: Float = 0.65f,
    val controlStyle: PlayerControlStyle = PlayerControlStyle.PILL,
    val layoutMode: PlayerLayoutMode = PlayerLayoutMode.STANDARD
)

data class SavedCustomEqProfile(
    val id: String,
    val name: String,
    val bandLevelsMb: List<Int>,
    val bassBoostStrength: Int,
    val spatial3dEnabled: Boolean = false
)

data class UserAppearancePrefs(
    val isDarkTheme: Boolean = true,
    val accentPalette: AppAccentPalette = AppAccentPalette.NEON_VIOLET,
    val uiStyleMode: UiStyleMode = UiStyleMode.NEON_MODERN,
    val fontStyle: AppFontStyle = AppFontStyle.SPACE_JAKARTA,
    val wallpaperUri: String? = null,
    val wallpaperDimAlpha: Float = 0.62f,
    val wallpaperBlurDp: Float = 0f,
    val fontSizeScale: Float = 1.0f,
    val activeCustomThemeId: String? = null,
    val useDynamicMaterialYou: Boolean = false
)

data class AppSettingsPrefs(
    val appLanguage: AppLanguage = AppLanguage.ES_LATAM,
    val voiceLanguageCode: String = "",
    val handsFreeVoiceEnabled: Boolean = false,
    val screenOffVoiceEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val hideShortWhatsAppAudio: Boolean = false,
    val autoDownloadArtwork: Boolean = false,
    val playAlongOtherApps: Boolean = false,
    val notifShowArtwork: Boolean = true,
    val notifShowFavorite: Boolean = true,
    val notifShowModeToggle: Boolean = true,
    val scheduledMixRemindersEnabled: Boolean = false,
    val chatPushNotificationsEnabled: Boolean = true,
    val chatPushOnlyActiveRegion: Boolean = true,
    val savedShuffleEnabled: Boolean = false,
    val savedRepeatMode: Int = 0,
    val guestModeEnabled: Boolean = false,
    val preferredChatRegion: ChatRegion = ChatRegion.LATAM,
    val customChatUsername: String = "",
    val chatAvatarUri: String? = null,
    val chatAvatarBase64: String = "",
    val diagnosticModeEnabled: Boolean = false,
    val sleepTimerFadeOutSeconds: Int = 3
)

data class CustomPlaylist(
    val id: String,
    val name: String,
    val trackKeys: List<String>,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class LocalTrack(
    val id: Long,
    val trackKey: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val formatLabel: String = "AUDIO",
    val genre: String = ""
)

data class GeneratedPlaylist(
    val slot: TimeOfDaySlot,
    val title: String,
    val subtitle: String,
    val tracks: List<LocalTrack>,
    val generatedAtMs: Long
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class AlbumPlaylist(
    val albumName: String,
    val artistName: String,
    val albumArtUri: String?,
    val tracks: List<LocalTrack>
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

data class ArtistPlaylist(
    val artistName: String,
    val albumCount: Int,
    val albums: List<AlbumPlaylist> = emptyList(),
    val tracks: List<LocalTrack>
) {
    val totalDurationMs: Long get() = tracks.sumOf { it.durationMs }
}

@Keep
@IgnoreExtraProperties
data class PlaybackProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackKey: String = "",
    val trackTitle: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val trackUri: String = "",
    val albumId: Long = 0L,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val totalPlaybackSeconds: Long = 0L,
    val timeOfDaySlot: String = TimeOfDaySlot.MORNING.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthlyStat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthKey: String = "",
    val totalSeconds: Long = 0L,
    val totalPlays: Long = 0L,
    val morningSeconds: Long = 0L,
    val afternoonSeconds: Long = 0L,
    val nightSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthlyTrackStat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val monthKey: String = "",
    val trackKey: String = "",
    val trackTitle: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val albumId: Long = 0L,
    val playCount: Long = 0L,
    val totalSeconds: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackKey: String = "",
    val trackTitle: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val trackUri: String = "",
    val albumId: Long = 0L,
    val durationMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RegionalChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarBase64: String = "",
    val regionId: String = ChatRegion.LATAM.id,
    val messageText: String = "",
    val recommendedTrackTitle: String = "",
    val recommendedArtist: String = "",
    val audioFileName: String = "",
    val audioMimeType: String = "",
    val audioClipBase64: String = "",
    val audioChunkCount: Int = 0,
    val audioSizeBytes: Long = 0L,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun hasDownloadableAudio(): Boolean = audioClipBase64.isNotBlank() || audioChunkCount > 0
}

data class ArtistStatSummary(
    val artistName: String,
    val totalSeconds: Long,
    val totalPlays: Long,
    val trackCount: Int
)

data class AlbumStatSummary(
    val albumName: String,
    val artistName: String,
    val albumId: Long,
    val totalSeconds: Long,
    val totalPlays: Long,
    val trackCount: Int
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatPlaybackTimeReadable(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun currentMonthKey(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    return String.format(Locale.US, "%04d-%02d", year, month)
}

fun formatMonthLabel(monthKey: String, localeTag: String = "es-ES"): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM", Locale.US)
        val date = parser.parse(monthKey) ?: return monthKey
        val locale = Locale.forLanguageTag(localeTag)
        val formatter = SimpleDateFormat("MMMM yyyy", locale)
        formatter.format(date).replaceFirstChar { it.titlecase(locale) }
    } catch (_: Exception) {
        monthKey
    }
}

fun stableTrackStatId(trackKey: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest(trackKey.toByteArray(Charsets.UTF_8))
    val hex = digest.joinToString("") { "%02x".format(it) }
    return "trk_${hex.take(24)}"
}
