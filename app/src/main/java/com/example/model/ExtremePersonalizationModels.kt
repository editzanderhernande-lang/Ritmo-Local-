package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class MiniPlayerBarStyle(val id: String, val displayName: String) {
    FLOATING_PILL("floating_pill", "Barra flotante"),
    DOCKED_GLASS("docked_glass", "Cristal acoplado"),
    MINIMAL_LINE("minimal_line", "Línea minimal");

    companion object {
        fun fromId(id: String?): MiniPlayerBarStyle =
            entries.firstOrNull { it.id == id } ?: FLOATING_PILL
    }
}

enum class IconVisualMode(val id: String, val displayName: String) {
    ROUNDED("rounded", "Redondeados"),
    OUTLINED("outlined", "Línea fina");

    companion object {
        fun fromId(id: String?): IconVisualMode =
            entries.firstOrNull { it.id == id } ?: ROUNDED
    }
}

data class ExtremeVisualStyleConfig(
    val cardCornerDp: Float = 20f,
    val cardBorderWidthDp: Float = 1f,
    val surfaceTransparencyAlpha: Float = 0.94f,
    val shadowElevationDp: Float = 6f,
    val customGradientEnabled: Boolean = false,
    val gradientStartArgb: Long = 0xFF9D6BFF,
    val gradientEndArgb: Long = 0xFF1E1B4B,
    val gradientAngleDeg: Float = 135f,
    val animationSpeedScale: Float = 1.0f,
    val buttonCornerPercent: Int = 50,
    val iconStyleMode: IconVisualMode = IconVisualMode.ROUNDED,
    val miniPlayerBarStyle: MiniPlayerBarStyle = MiniPlayerBarStyle.FLOATING_PILL
) {
    fun normalized(): ExtremeVisualStyleConfig = copy(
        cardCornerDp = cardCornerDp.coerceIn(6f, 30f),
        cardBorderWidthDp = cardBorderWidthDp.coerceIn(0f, 3f),
        surfaceTransparencyAlpha = surfaceTransparencyAlpha.coerceIn(0.60f, 1.0f),
        shadowElevationDp = shadowElevationDp.coerceIn(0f, 14f),
        gradientAngleDeg = gradientAngleDeg.coerceIn(0f, 360f),
        animationSpeedScale = animationSpeedScale.coerceIn(0.5f, 1.8f),
        buttonCornerPercent = buttonCornerPercent.coerceIn(8, 50)
    )

    companion object {
        fun defaultConfig(): ExtremeVisualStyleConfig = ExtremeVisualStyleConfig()

        fun encode(cfg: ExtremeVisualStyleConfig): String = JSONObject().apply {
            val n = cfg.normalized()
            put("cardCornerDp", n.cardCornerDp.toDouble())
            put("cardBorderWidthDp", n.cardBorderWidthDp.toDouble())
            put("surfaceTransparencyAlpha", n.surfaceTransparencyAlpha.toDouble())
            put("shadowElevationDp", n.shadowElevationDp.toDouble())
            put("customGradientEnabled", n.customGradientEnabled)
            put("gradientStartArgb", n.gradientStartArgb)
            put("gradientEndArgb", n.gradientEndArgb)
            put("gradientAngleDeg", n.gradientAngleDeg.toDouble())
            put("animationSpeedScale", n.animationSpeedScale.toDouble())
            put("buttonCornerPercent", n.buttonCornerPercent)
            put("iconStyleMode", n.iconStyleMode.id)
            put("miniPlayerBarStyle", n.miniPlayerBarStyle.id)
        }.toString()

        fun decode(raw: String): ExtremeVisualStyleConfig {
            if (raw.isBlank()) return defaultConfig()
            return try {
                val obj = JSONObject(raw)
                ExtremeVisualStyleConfig(
                    cardCornerDp = obj.optDouble("cardCornerDp", 20.0).toFloat(),
                    cardBorderWidthDp = obj.optDouble("cardBorderWidthDp", 1.0).toFloat(),
                    surfaceTransparencyAlpha = obj.optDouble("surfaceTransparencyAlpha", 0.94).toFloat(),
                    shadowElevationDp = obj.optDouble("shadowElevationDp", 6.0).toFloat(),
                    customGradientEnabled = obj.optBoolean("customGradientEnabled", false),
                    gradientStartArgb = obj.optLong("gradientStartArgb", 0xFF9D6BFF),
                    gradientEndArgb = obj.optLong("gradientEndArgb", 0xFF1E1B4B),
                    gradientAngleDeg = obj.optDouble("gradientAngleDeg", 135.0).toFloat(),
                    animationSpeedScale = obj.optDouble("animationSpeedScale", 1.0).toFloat(),
                    buttonCornerPercent = obj.optInt("buttonCornerPercent", 50),
                    iconStyleMode = IconVisualMode.fromId(obj.optString("iconStyleMode")),
                    miniPlayerBarStyle = MiniPlayerBarStyle.fromId(obj.optString("miniPlayerBarStyle"))
                ).normalized()
            } catch (_: Exception) {
                defaultConfig()
            }
        }
    }
}

enum class ArtworkPositionMode(val id: String, val displayName: String) {
    TOP("top", "Arriba"),
    CENTER("center", "Centrada"),
    COMPACT_LEFT("compact_left", "Compacta lateral");

    companion object {
        fun fromId(id: String?): ArtworkPositionMode =
            entries.firstOrNull { it.id == id } ?: CENTER
    }
}

enum class ProgressBarVisualStyle(val id: String, val displayName: String) {
    SLIDER_GLOW("slider_glow", "Barra clásica"),
    WAVEFORM_BAR("waveform_bar", "Pulsos / Ondas"),
    THIN_LINE("thin_line", "Línea fina");

    companion object {
        fun fromId(id: String?): ProgressBarVisualStyle =
            entries.firstOrNull { it.id == id } ?: SLIDER_GLOW
    }
}

enum class PlayerQuickAction(val id: String, val label: String) {
    SHUFFLE("shuffle", "Aleatorio"),
    PREVIOUS("previous", "Anterior"),
    PLAY_PAUSE("play_pause", "Reproducir / Pausar"),
    NEXT("next", "Siguiente"),
    REPEAT("repeat", "Repetir"),
    FAVORITE("favorite", "Favorito"),
    LYRICS("lyrics", "Letras"),
    QUEUE("queue", "Cola"),
    VOICE("voice", "Voz"),
    SLEEP_TIMER("sleep_timer", "Temporizador");

    companion object {
        val defaultPrimaryOrder: List<PlayerQuickAction> = listOf(SHUFFLE, PREVIOUS, PLAY_PAUSE, NEXT, REPEAT)
        val defaultToolsOrder: List<PlayerQuickAction> = listOf(LYRICS, QUEUE, VOICE, SLEEP_TIMER, FAVORITE)
        fun fromId(id: String?): PlayerQuickAction? = entries.firstOrNull { it.id == id }
    }
}

data class PlayerDesignerConfig(
    val artworkPosition: ArtworkPositionMode = ArtworkPositionMode.CENTER,
    val artworkScale: Float = 1.0f,
    val buttonScale: Float = 1.0f,
    val progressBarStyle: ProgressBarVisualStyle = ProgressBarVisualStyle.SLIDER_GLOW,
    val hiddenActions: Set<PlayerQuickAction> = emptySet(),
    val primaryControlsOrder: List<PlayerQuickAction> = PlayerQuickAction.defaultPrimaryOrder,
    val toolActionsOrder: List<PlayerQuickAction> = PlayerQuickAction.defaultToolsOrder,
    val immersiveEffectsEnabled: Boolean = true
) {
    fun normalized(): PlayerDesignerConfig {
        val primaryValid = primaryControlsOrder.filter { it in PlayerQuickAction.defaultPrimaryOrder }.distinct()
        val primaryFull = primaryValid + PlayerQuickAction.defaultPrimaryOrder.filterNot { it in primaryValid }
        val toolsValid = toolActionsOrder.filter { it in PlayerQuickAction.defaultToolsOrder }.distinct()
        val toolsFull = toolsValid + PlayerQuickAction.defaultToolsOrder.filterNot { it in toolsValid }
        return copy(
            artworkScale = artworkScale.coerceIn(0.75f, 1.18f),
            buttonScale = buttonScale.coerceIn(0.8f, 1.25f),
            hiddenActions = hiddenActions - PlayerQuickAction.PLAY_PAUSE, // never allow hiding Play/Pause
            primaryControlsOrder = primaryFull,
            toolActionsOrder = toolsFull
        )
    }

    fun movePrimaryControl(action: PlayerQuickAction, delta: Int): PlayerDesignerConfig {
        val list = normalized().primaryControlsOrder.toMutableList()
        val idx = list.indexOf(action)
        val target = idx + delta
        if (idx < 0 || target !in list.indices) return this
        val moved = list.removeAt(idx)
        list.add(target, moved)
        return copy(primaryControlsOrder = list)
    }

    fun toggleActionVisibility(action: PlayerQuickAction): PlayerDesignerConfig {
        if (action == PlayerQuickAction.PLAY_PAUSE) return this
        val next = hiddenActions.toMutableSet()
        if (action in next) next.remove(action) else next.add(action)
        return copy(hiddenActions = next)
    }

    companion object {
        fun defaultConfig(): PlayerDesignerConfig = PlayerDesignerConfig()

        fun encode(cfg: PlayerDesignerConfig): String = JSONObject().apply {
            val n = cfg.normalized()
            put("artworkPosition", n.artworkPosition.id)
            put("artworkScale", n.artworkScale.toDouble())
            put("buttonScale", n.buttonScale.toDouble())
            put("progressBarStyle", n.progressBarStyle.id)
            put("hiddenActions", JSONArray(n.hiddenActions.map { it.id }))
            put("primaryControlsOrder", JSONArray(n.primaryControlsOrder.map { it.id }))
            put("toolActionsOrder", JSONArray(n.toolActionsOrder.map { it.id }))
            put("immersiveEffectsEnabled", n.immersiveEffectsEnabled)
        }.toString()

        fun decode(raw: String): PlayerDesignerConfig {
            if (raw.isBlank()) return defaultConfig()
            return try {
                val obj = JSONObject(raw)
                val hiddenArr = obj.optJSONArray("hiddenActions")
                val hidden = mutableSetOf<PlayerQuickAction>()
                if (hiddenArr != null) {
                    for (i in 0 until hiddenArr.length()) {
                        PlayerQuickAction.fromId(hiddenArr.optString(i))?.let { hidden.add(it) }
                    }
                }
                val primaryArr = obj.optJSONArray("primaryControlsOrder")
                val primary = mutableListOf<PlayerQuickAction>()
                if (primaryArr != null) {
                    for (i in 0 until primaryArr.length()) {
                        PlayerQuickAction.fromId(primaryArr.optString(i))?.let { primary.add(it) }
                    }
                }
                val toolsArr = obj.optJSONArray("toolActionsOrder")
                val tools = mutableListOf<PlayerQuickAction>()
                if (toolsArr != null) {
                    for (i in 0 until toolsArr.length()) {
                        PlayerQuickAction.fromId(toolsArr.optString(i))?.let { tools.add(it) }
                    }
                }
                PlayerDesignerConfig(
                    artworkPosition = ArtworkPositionMode.fromId(obj.optString("artworkPosition")),
                    artworkScale = obj.optDouble("artworkScale", 1.0).toFloat(),
                    buttonScale = obj.optDouble("buttonScale", 1.0).toFloat(),
                    progressBarStyle = ProgressBarVisualStyle.fromId(obj.optString("progressBarStyle")),
                    hiddenActions = hidden,
                    primaryControlsOrder = primary.ifEmpty { PlayerQuickAction.defaultPrimaryOrder },
                    toolActionsOrder = tools.ifEmpty { PlayerQuickAction.defaultToolsOrder },
                    immersiveEffectsEnabled = obj.optBoolean("immersiveEffectsEnabled", true)
                ).normalized()
            } catch (_: Exception) {
                defaultConfig()
            }
        }
    }
}

data class MusicalProfileConfig(
    val displayName: String = "Oyente Ritmo Local",
    val bio: String = "Mi archivo musical personal, privado y offline-first.",
    val bannerPrimaryArgb: Long = 0xFF9D6BFF,
    val bannerSecondaryArgb: Long = 0xFFD946EF,
    val showRecapOnProfile: Boolean = true,
    val showAchievementsOnProfile: Boolean = true,
    val privateByDefault: Boolean = true,
    val statsCloudSyncEnabled: Boolean = false
) {
    companion object {
        fun defaultConfig(): MusicalProfileConfig = MusicalProfileConfig()

        fun encode(cfg: MusicalProfileConfig): String = JSONObject().apply {
            put("displayName", cfg.displayName.take(40))
            put("bio", cfg.bio.take(140))
            put("bannerPrimaryArgb", cfg.bannerPrimaryArgb)
            put("bannerSecondaryArgb", cfg.bannerSecondaryArgb)
            put("showRecapOnProfile", cfg.showRecapOnProfile)
            put("showAchievementsOnProfile", cfg.showAchievementsOnProfile)
            put("privateByDefault", cfg.privateByDefault)
            put("statsCloudSyncEnabled", cfg.statsCloudSyncEnabled)
        }.toString()

        fun decode(raw: String): MusicalProfileConfig {
            if (raw.isBlank()) return defaultConfig()
            return try {
                val obj = JSONObject(raw)
                MusicalProfileConfig(
                    displayName = obj.optString("displayName", "Oyente Ritmo Local").ifBlank { "Oyente Ritmo Local" }.take(40),
                    bio = obj.optString("bio", "Mi archivo musical personal, privado y offline-first.").take(140),
                    bannerPrimaryArgb = obj.optLong("bannerPrimaryArgb", 0xFF9D6BFF),
                    bannerSecondaryArgb = obj.optLong("bannerSecondaryArgb", 0xFFD946EF),
                    showRecapOnProfile = obj.optBoolean("showRecapOnProfile", true),
                    showAchievementsOnProfile = obj.optBoolean("showAchievementsOnProfile", true),
                    privateByDefault = obj.optBoolean("privateByDefault", true),
                    statsCloudSyncEnabled = obj.optBoolean("statsCloudSyncEnabled", false)
                )
            } catch (_: Exception) {
                defaultConfig()
            }
        }
    }
}
