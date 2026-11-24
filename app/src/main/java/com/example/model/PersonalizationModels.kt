package com.example.model

import org.json.JSONArray
import org.json.JSONObject

data class CustomThemeConfig(
    val id: String,
    val name: String,
    val isDark: Boolean = true,
    val primaryArgb: Long = 0xFF9D6BFF,
    val secondaryArgb: Long = 0xFFD946EF,
    val backgroundArgb: Long = 0xFF0A0814,
    val surfaceArgb: Long = 0xFF151226,
    val buttonArgb: Long = 0xFF9D6BFF
) {
    fun toJsonString(): String = JSONObject().apply {
        put("format", "ritmo_local_theme_v1")
        put("id", id)
        put("name", name)
        put("isDark", isDark)
        put("primaryArgb", primaryArgb)
        put("secondaryArgb", secondaryArgb)
        put("backgroundArgb", backgroundArgb)
        put("surfaceArgb", surfaceArgb)
        put("buttonArgb", buttonArgb)
    }.toString(2)

    companion object {
        fun fromJsonString(raw: String, forceNewId: Boolean = false): CustomThemeConfig? {
            if (raw.isBlank()) return null
            return try {
                val obj = JSONObject(raw.trim())
                val name = obj.optString("name").trim().take(36)
                if (name.isBlank()) return null
                val rawId = obj.optString("id").trim()
                val resolvedId = if (forceNewId || rawId.isBlank()) {
                    "theme_${System.currentTimeMillis()}_${(100..999).random()}"
                } else {
                    rawId
                }
                val isDark = obj.optBoolean("isDark", true)
                CustomThemeConfig(
                    id = resolvedId,
                    name = name,
                    isDark = isDark,
                    primaryArgb = obj.optLong("primaryArgb", if (isDark) 0xFF9D6BFF else 0xFF7C3AED),
                    secondaryArgb = obj.optLong("secondaryArgb", if (isDark) 0xFFD946EF else 0xFFC026D3),
                    backgroundArgb = obj.optLong("backgroundArgb", if (isDark) 0xFF0A0814 else 0xFFF7F5FB),
                    surfaceArgb = obj.optLong("surfaceArgb", if (isDark) 0xFF151226 else 0xFFFFFFFF),
                    buttonArgb = obj.optLong("buttonArgb", obj.optLong("primaryArgb", 0xFF9D6BFF))
                )
            } catch (_: Exception) {
                null
            }
        }

        fun encodeList(themes: List<CustomThemeConfig>): String {
            val arr = JSONArray()
            themes.forEach { t ->
                arr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("name", t.name)
                        put("isDark", t.isDark)
                        put("primaryArgb", t.primaryArgb)
                        put("secondaryArgb", t.secondaryArgb)
                        put("backgroundArgb", t.backgroundArgb)
                        put("surfaceArgb", t.surfaceArgb)
                        put("buttonArgb", t.buttonArgb)
                    }
                )
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<CustomThemeConfig> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val out = mutableListOf<CustomThemeConfig>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val parsed = fromJsonString(obj.toString(), forceNewId = false)
                    if (parsed != null) out.add(parsed)
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

val CUSTOM_THEME_PRIMARY_SWATCHES = listOf(
    0xFF9D6BFF,
    0xFF22D3EE,
    0xFF34D399,
    0xFFF5A623,
    0xFFFB7185,
    0xFF818CF8,
    0xFFEC4899,
    0xFFEAB308
)

val CUSTOM_THEME_BG_DARK_SWATCHES = listOf(
    0xFF0A0814,
    0xFF07090F,
    0xFF0D1412,
    0xFF160B14,
    0xFF12100E,
    0xFF0F172A
)

val CUSTOM_THEME_BG_LIGHT_SWATCHES = listOf(
    0xFFF7F5FB,
    0xFFF5F6F8,
    0xFFFBF7EE,
    0xFFF0FDF4,
    0xFFFFF1F2,
    0xFFEFF6FF
)

enum class HomeSectionId(
    val id: String,
    val displayName: String,
    val description: String
) {
    QUICK_TABS("quick_tabs", "Pestañas rápidas", "Inicio, Listas, Artistas y Álbumes"),
    MOOD_BANNERS("mood_banners", "Banners por momento del día", "Mañana, Tarde y Noche"),
    RECENT_PLAYS("recent_plays", "Reproducción reciente", "Carrusel horizontal de carátulas"),
    PINNED_SHORTCUTS("pinned_shortcuts", "Accesos directos fijados", "Botones rápidos a tus funciones favoritas"),
    LIVE_STATS("live_stats", "Estadísticas en vivo y Recap", "Tiempo de hoy, artistas, resumen del mes e historia"),
    TRACK_LIST("track_list", "Lista de canciones", "Biblioteca local completa");

    companion object {
        val defaultOrder: List<HomeSectionId> = listOf(
            QUICK_TABS,
            PINNED_SHORTCUTS,
            LIVE_STATS,
            MOOD_BANNERS,
            RECENT_PLAYS,
            TRACK_LIST
        )

        fun fromId(id: String?): HomeSectionId? = entries.firstOrNull { it.id == id }
    }
}

enum class HomeShortcutType(
    val id: String,
    val label: String
) {
    FAVORITES("favorites", "Favoritas"),
    MIXES("mixes", "Mixes"),
    EQUALIZER("equalizer", "Ecualizador"),
    STATS("stats", "Recap y Stats"),
    PROFILE("profile", "Perfil"),
    HISTORY("history", "Mi Historia"),
    ORGANIZER("organizer", "Organizador"),
    DIAGNOSTICS("diagnostics", "Diagnóstico");

    companion object {
        val defaultShortcuts: List<HomeShortcutType> = listOf(FAVORITES, MIXES, STATS, PROFILE)
        fun fromId(id: String?): HomeShortcutType? = entries.firstOrNull { it.id == id }
    }
}

data class HomeLayoutConfig(
    val orderedSections: List<HomeSectionId> = HomeSectionId.defaultOrder,
    val hiddenSections: Set<HomeSectionId> = setOf(HomeSectionId.PINNED_SHORTCUTS),
    val pinnedShortcuts: List<HomeShortcutType> = HomeShortcutType.defaultShortcuts,
    val showFormatBadge: Boolean = true,
    val showAlbumName: Boolean = true,
    val showDuration: Boolean = true,
    val showTodayTimeStat: Boolean = true,
    val showTodayPlaysStat: Boolean = true,
    val showTodayArtistsStat: Boolean = true,
    val showRecapBannerCard: Boolean = true
) {
    fun normalized(): HomeLayoutConfig {
        val uniqueValid = orderedSections.distinct()
        val missing = HomeSectionId.defaultOrder.filterNot { it in uniqueValid }
        return copy(
            orderedSections = uniqueValid + missing,
            pinnedShortcuts = pinnedShortcuts.distinct()
        )
    }

    fun moveSection(section: HomeSectionId, delta: Int): HomeLayoutConfig {
        val norm = normalized().orderedSections.toMutableList()
        val idx = norm.indexOf(section)
        val target = idx + delta
        if (idx < 0 || target !in norm.indices) return this
        val item = norm.removeAt(idx)
        norm.add(target, item)
        return copy(orderedSections = norm)
    }

    fun toggleSectionVisibility(section: HomeSectionId): HomeLayoutConfig {
        // Always keep at least one main content section visible (never lock user out of tracks if everything else is hidden)
        val nextHidden = hiddenSections.toMutableSet()
        if (section in nextHidden) {
            nextHidden.remove(section)
        } else {
            val remainingVisible = HomeSectionId.entries.count { it != section && it !in nextHidden }
            if (remainingVisible >= 1) {
                nextHidden.add(section)
            }
        }
        return copy(hiddenSections = nextHidden)
    }

    fun togglePinnedShortcut(shortcut: HomeShortcutType): HomeLayoutConfig {
        val next = pinnedShortcuts.toMutableList()
        if (shortcut in next) next.remove(shortcut) else next.add(shortcut)
        return copy(pinnedShortcuts = next)
    }

    companion object {
        fun defaultConfig(): HomeLayoutConfig = HomeLayoutConfig()

        fun encode(config: HomeLayoutConfig): String {
            val norm = config.normalized()
            return JSONObject().apply {
                put("orderedSections", JSONArray(norm.orderedSections.map { it.id }))
                put("hiddenSections", JSONArray(norm.hiddenSections.map { it.id }))
                put("pinnedShortcuts", JSONArray(norm.pinnedShortcuts.map { it.id }))
                put("showFormatBadge", norm.showFormatBadge)
                put("showAlbumName", norm.showAlbumName)
                put("showDuration", norm.showDuration)
                put("showTodayTimeStat", norm.showTodayTimeStat)
                put("showTodayPlaysStat", norm.showTodayPlaysStat)
                put("showTodayArtistsStat", norm.showTodayArtistsStat)
                put("showRecapBannerCard", norm.showRecapBannerCard)
            }.toString()
        }

        fun decode(raw: String): HomeLayoutConfig {
            if (raw.isBlank()) return defaultConfig()
            return try {
                val obj = JSONObject(raw)
                val orderArr = obj.optJSONArray("orderedSections")
                val ordered = mutableListOf<HomeSectionId>()
                if (orderArr != null) {
                    for (i in 0 until orderArr.length()) {
                        HomeSectionId.fromId(orderArr.optString(i))?.let { ordered.add(it) }
                    }
                }
                val hiddenArr = obj.optJSONArray("hiddenSections")
                val hidden = mutableSetOf<HomeSectionId>()
                if (hiddenArr != null) {
                    for (i in 0 until hiddenArr.length()) {
                        HomeSectionId.fromId(hiddenArr.optString(i))?.let { hidden.add(it) }
                    }
                }
                val scArr = obj.optJSONArray("pinnedShortcuts")
                val shortcuts = mutableListOf<HomeShortcutType>()
                if (scArr != null) {
                    for (i in 0 until scArr.length()) {
                        HomeShortcutType.fromId(scArr.optString(i))?.let { shortcuts.add(it) }
                    }
                }
                HomeLayoutConfig(
                    orderedSections = ordered.ifEmpty { HomeSectionId.defaultOrder },
                    hiddenSections = if (obj.has("hiddenSections")) hidden else setOf(HomeSectionId.PINNED_SHORTCUTS),
                    pinnedShortcuts = if (obj.has("pinnedShortcuts")) shortcuts else HomeShortcutType.defaultShortcuts,
                    showFormatBadge = obj.optBoolean("showFormatBadge", true),
                    showAlbumName = obj.optBoolean("showAlbumName", true),
                    showDuration = obj.optBoolean("showDuration", true),
                    showTodayTimeStat = obj.optBoolean("showTodayTimeStat", true),
                    showTodayPlaysStat = obj.optBoolean("showTodayPlaysStat", true),
                    showTodayArtistsStat = obj.optBoolean("showTodayArtistsStat", true),
                    showRecapBannerCard = obj.optBoolean("showRecapBannerCard", true)
                ).normalized()
            } catch (_: Exception) {
                defaultConfig()
            }
        }
    }
}

data class HomeLayoutPreset(
    val id: String,
    val name: String,
    val config: HomeLayoutConfig,
    val createdAtMs: Long = System.currentTimeMillis()
) {
    companion object {
        fun encodeList(presets: List<HomeLayoutPreset>): String {
            val arr = JSONArray()
            presets.forEach { preset ->
                arr.put(
                    JSONObject().apply {
                        put("id", preset.id)
                        put("name", preset.name)
                        put("createdAtMs", preset.createdAtMs)
                        put("config", JSONObject(HomeLayoutConfig.encode(preset.config)))
                    }
                )
            }
            return arr.toString()
        }

        fun decodeList(raw: String): List<HomeLayoutPreset> {
            if (raw.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(raw)
                val out = mutableListOf<HomeLayoutPreset>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
                    val cfgRaw = obj.optJSONObject("config")?.toString().orEmpty()
                    out.add(
                        HomeLayoutPreset(
                            id = id,
                            name = name,
                            config = HomeLayoutConfig.decode(cfgRaw),
                            createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                        )
                    )
                }
                out
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}

enum class PlayerControlStyle(
    val id: String,
    val displayName: String,
    val description: String
) {
    PILL("pill", "Píldora", "Botón circular clásico con dock suave"),
    MINIMAL_OUTLINE("minimal_outline", "Minimalista", "Bordes finos depurados y ligereza visual"),
    CLASSIC_STUDIO("classic_studio", "Estudio Hi-Fi", "Controles cuadrados técnicos estilo consola");

    companion object {
        fun fromId(id: String?): PlayerControlStyle =
            entries.firstOrNull { it.id == id } ?: PILL
    }
}

enum class PlayerLayoutMode(
    val id: String,
    val displayName: String,
    val description: String
) {
    STANDARD("standard", "Estándar", "Equilibrio clásico de portada, ondas y controles"),
    IMMERSIVE("immersive", "Inmersivo", "Portada protagonista extra grande con fondo vivo"),
    COMPACT("compact", "Compacto", "Menos relleno vertical ideal para pantallas pequeñas");

    companion object {
        fun fromId(id: String?): PlayerLayoutMode =
            entries.firstOrNull { it.id == id } ?: STANDARD
    }
}
