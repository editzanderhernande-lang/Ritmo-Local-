package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppAccentPalette
import com.example.model.AppFontStyle
import com.example.model.CustomThemeConfig
import com.example.model.ExtremeVisualStyleConfig
import com.example.model.UiStyleMode
import kotlin.math.cos
import kotlin.math.sin

val LocalAccentPalette = staticCompositionLocalOf { AppAccentPalette.NEON_VIOLET }
val LocalUiStyleMode = staticCompositionLocalOf { UiStyleMode.NEON_MODERN }
val LocalFontStyle = staticCompositionLocalOf { AppFontStyle.SPACE_JAKARTA }
val LocalExtremeVisualStyle = staticCompositionLocalOf { ExtremeVisualStyleConfig.defaultConfig() }

@Composable
fun RitmoLocalTheme(
    darkTheme: Boolean = true,
    accentPalette: AppAccentPalette = AppAccentPalette.NEON_VIOLET,
    uiStyleMode: UiStyleMode = UiStyleMode.NEON_MODERN,
    fontStyle: AppFontStyle = AppFontStyle.SPACE_JAKARTA,
    fontSizeScale: Float = 1.0f,
    hasWallpaper: Boolean = false,
    customTheme: CustomThemeConfig? = null,
    extremeStyle: ExtremeVisualStyleConfig = ExtremeVisualStyleConfig.defaultConfig(),
    useDynamicMaterialYou: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val normalizedExtreme = extremeStyle.normalized()
    val effectiveDark = customTheme?.isDark ?: darkTheme
    val isMinimal = uiStyleMode == UiStyleMode.MINIMALIST
    val isNeon = uiStyleMode == UiStyleMode.NEON_MODERN
    val baseAlpha = when {
        hasWallpaper && isMinimal -> 0.70f
        hasWallpaper && isNeon -> 0.78f
        hasWallpaper -> 0.86f
        isMinimal -> 0.94f
        else -> 1.0f
    }
    val surfaceAlpha = (baseAlpha * normalizedExtreme.surfaceTransparencyAlpha).coerceIn(0.55f, 1.0f)

    val colorScheme = when {
        useDynamicMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicBase = if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamicBase.copy(
                background = if (hasWallpaper) Color.Transparent else dynamicBase.background,
                surface = dynamicBase.surface.copy(alpha = surfaceAlpha),
                surfaceContainer = dynamicBase.surfaceContainer.copy(alpha = surfaceAlpha),
                surfaceContainerLow = dynamicBase.surfaceContainerLow.copy(alpha = surfaceAlpha),
                surfaceContainerHigh = dynamicBase.surfaceContainerHigh.copy(alpha = surfaceAlpha)
            )
        }
        customTheme != null -> {
            val primaryColor = Color(customTheme.primaryArgb)
            val secondaryColor = Color(customTheme.secondaryArgb)
            val bgColor = Color(customTheme.backgroundArgb)
            val surfColor = Color(customTheme.surfaceArgb)
            val onPrimary = if (primaryColor.luminance() > 0.55f) Color(0xFF101018) else Color.White
            val onSecondary = if (secondaryColor.luminance() > 0.55f) Color(0xFF101018) else Color.White
            if (effectiveDark) {
                darkColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimary,
                    primaryContainer = primaryColor.copy(alpha = 0.24f),
                    onPrimaryContainer = Color.White,
                    secondary = secondaryColor,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryColor.copy(alpha = 0.22f),
                    onSecondaryContainer = secondaryColor,
                    tertiary = NightAccent,
                    onTertiary = Color(0xFF0E1030),
                    background = if (hasWallpaper) Color.Transparent else bgColor,
                    onBackground = TextPrimaryDark,
                    surface = surfColor.copy(alpha = surfaceAlpha),
                    onSurface = TextPrimaryDark,
                    surfaceVariant = surfColor.copy(alpha = (surfaceAlpha * 0.96f).coerceIn(0.4f, 1f)),
                    onSurfaceVariant = TextSecondaryDark,
                    surfaceContainerLow = surfColor.copy(alpha = surfaceAlpha),
                    surfaceContainer = surfColor.copy(alpha = surfaceAlpha),
                    surfaceContainerHigh = Color(
                        red = (surfColor.red + 0.06f).coerceIn(0f, 1f),
                        green = (surfColor.green + 0.06f).coerceIn(0f, 1f),
                        blue = (surfColor.blue + 0.08f).coerceIn(0f, 1f),
                        alpha = surfaceAlpha
                    ),
                    outline = OutlineDark,
                    error = ErrorRed,
                    onError = Color.White
                )
            } else {
                lightColorScheme(
                    primary = primaryColor,
                    onPrimary = onPrimary,
                    primaryContainer = primaryColor.copy(alpha = 0.16f),
                    onPrimaryContainer = primaryColor,
                    secondary = secondaryColor,
                    onSecondary = onSecondary,
                    secondaryContainer = secondaryColor.copy(alpha = 0.15f),
                    onSecondaryContainer = secondaryColor,
                    tertiary = NightAccent,
                    onTertiary = Color.White,
                    background = if (hasWallpaper) Color.Transparent else bgColor,
                    onBackground = TextPrimaryLight,
                    surface = surfColor.copy(alpha = surfaceAlpha),
                    onSurface = TextPrimaryLight,
                    surfaceVariant = bgColor.copy(alpha = surfaceAlpha),
                    onSurfaceVariant = TextSecondaryLight,
                    surfaceContainerLow = surfColor.copy(alpha = surfaceAlpha),
                    surfaceContainer = bgColor.copy(alpha = surfaceAlpha),
                    surfaceContainerHigh = bgColor.copy(alpha = surfaceAlpha),
                    outline = OutlineLight,
                    error = ErrorRed,
                    onError = Color.White
                )
            }
        }
        effectiveDark -> {
            val baseBg = when {
                isNeon -> NeonBg
                isMinimal -> Color(0xFF07080C)
                else -> ObsidianBg
            }
            val baseSurface = when {
                isNeon -> NeonSurface
                isMinimal -> Color(0xFF101219)
                else -> ObsidianSurface
            }
            val baseElevated = when {
                isNeon -> NeonSurfaceElevated
                isMinimal -> Color(0xFF171A24)
                else -> ObsidianSurfaceElevated
            }
            val baseHighest = when {
                isNeon -> NeonSurfaceHighest
                else -> ObsidianSurfaceHighest
            }
            val baseOutline = when {
                isNeon -> NeonOutline
                isMinimal -> OutlineDark.copy(alpha = 0.48f)
                else -> OutlineDark
            }
            darkColorScheme(
                primary = accentPalette.primaryDark,
                onPrimary = if (isNeon) Color.White else Color(0xFF111216),
                primaryContainer = accentPalette.primaryDark.copy(alpha = if (isMinimal) 0.16f else 0.24f),
                onPrimaryContainer = if (isNeon) Color.White else accentPalette.primaryDark,
                secondary = accentPalette.secondaryDark,
                onSecondary = if (isNeon) Color.White else Color(0xFF111216),
                secondaryContainer = accentPalette.secondaryDark.copy(alpha = if (isMinimal) 0.16f else 0.22f),
                onSecondaryContainer = accentPalette.secondaryDark,
                tertiary = NightAccent,
                onTertiary = Color(0xFF0E1030),
                background = if (hasWallpaper) Color.Transparent else baseBg,
                onBackground = TextPrimaryDark,
                surface = baseSurface.copy(alpha = surfaceAlpha),
                onSurface = TextPrimaryDark,
                surfaceVariant = baseElevated.copy(alpha = surfaceAlpha),
                onSurfaceVariant = TextSecondaryDark,
                surfaceContainerLow = baseSurface.copy(alpha = surfaceAlpha),
                surfaceContainer = baseElevated.copy(alpha = surfaceAlpha),
                surfaceContainerHigh = baseHighest.copy(alpha = surfaceAlpha),
                outline = baseOutline,
                error = ErrorRed,
                onError = Color.White
            )
        }
        else -> {
            val baseBg = when {
                isNeon -> Color(0xFFF5F3FF)
                isMinimal -> Color(0xFFF6F6F4)
                else -> WarmIvoryBg
            }
            val baseSurface = when {
                isNeon -> Color(0xFFFFFFFF)
                isMinimal -> Color(0xFFFFFFFF)
                else -> WarmIvorySurface
            }
            val baseElevated = when {
                isNeon -> Color(0xFFEDE9FE)
                isMinimal -> Color(0xFFF0F1F4)
                else -> WarmIvoryElevated
            }
            lightColorScheme(
                primary = accentPalette.primaryLight,
                onPrimary = Color.White,
                primaryContainer = accentPalette.primaryLight.copy(alpha = if (isMinimal) 0.12f else 0.18f),
                onPrimaryContainer = accentPalette.primaryLight,
                secondary = accentPalette.secondaryLight,
                onSecondary = Color.White,
                secondaryContainer = accentPalette.secondaryLight.copy(alpha = if (isMinimal) 0.12f else 0.16f),
                onSecondaryContainer = accentPalette.secondaryLight,
                tertiary = NightAccent,
                onTertiary = Color.White,
                background = if (hasWallpaper) Color.Transparent else baseBg,
                onBackground = TextPrimaryLight,
                surface = baseSurface.copy(alpha = surfaceAlpha),
                onSurface = TextPrimaryLight,
                surfaceVariant = baseElevated.copy(alpha = surfaceAlpha),
                onSurfaceVariant = TextSecondaryLight,
                surfaceContainerLow = baseSurface.copy(alpha = surfaceAlpha),
                surfaceContainer = baseElevated.copy(alpha = surfaceAlpha),
                surfaceContainerHigh = Color(0xFFE7DFD2).copy(alpha = surfaceAlpha),
                outline = if (isMinimal) OutlineLight.copy(alpha = 0.55f) else OutlineLight,
                error = ErrorRed,
                onError = Color.White
            )
        }
    }

    val appShapes = Shapes(
        small = RoundedCornerShape((normalizedExtreme.cardCornerDp * 0.65f).dp),
        medium = RoundedCornerShape(normalizedExtreme.cardCornerDp.dp),
        large = RoundedCornerShape((normalizedExtreme.cardCornerDp * 1.25f).coerceAtMost(32f).dp)
    )

    CompositionLocalProvider(
        LocalAccentPalette provides accentPalette,
        LocalUiStyleMode provides uiStyleMode,
        LocalFontStyle provides fontStyle,
        LocalExtremeVisualStyle provides normalizedExtreme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typographyFor(fontStyle, fontSizeScale),
            shapes = appShapes,
            content = content
        )
    }
}

@Composable
fun AppWallpaperBackdrop(
    wallpaperUri: String?,
    dimAlpha: Float,
    darkTheme: Boolean,
    uiStyleMode: UiStyleMode,
    accentPalette: AppAccentPalette,
    blurRadiusDp: Float = 0f,
    customTheme: CustomThemeConfig? = null,
    extremeStyle: ExtremeVisualStyleConfig = ExtremeVisualStyleConfig.defaultConfig(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveDark = customTheme?.isDark ?: darkTheme
    val baseBg = when {
        customTheme != null -> Color(customTheme.backgroundArgb)
        effectiveDark && uiStyleMode == UiStyleMode.NEON_MODERN -> NeonBg
        effectiveDark && uiStyleMode == UiStyleMode.MINIMALIST -> Color(0xFF07080C)
        effectiveDark -> ObsidianBg
        uiStyleMode == UiStyleMode.NEON_MODERN -> Color(0xFFF5F3FF)
        uiStyleMode == UiStyleMode.MINIMALIST -> Color(0xFFF6F6F4)
        else -> WarmIvoryBg
    }

    Box(modifier = modifier.fillMaxSize().background(baseBg)) {
        if (!wallpaperUri.isNullOrBlank()) {
            val safeBlurDp = blurRadiusDp.coerceIn(0f, 28f)
            val canRenderEffectBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && safeBlurDp > 0.5f
            AsyncImage(
                model = ImageRequest.Builder(context).data(wallpaperUri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (canRenderEffectBlur) Modifier.blur(safeBlurDp.dp) else Modifier)
            )
            val scrimColor = if (effectiveDark) Color.Black else Color.White
            val legacyFallbackBoost = if (!canRenderEffectBlur && safeBlurDp > 0.5f) 0.14f else 0f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = (dimAlpha + legacyFallbackBoost).coerceIn(0.15f, 0.92f)))
            )
        } else if (extremeStyle.customGradientEnabled) {
            val rad = Math.toRadians(extremeStyle.gradientAngleDeg.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            val startCol = Color(extremeStyle.gradientStartArgb)
            val endCol = Color(extremeStyle.gradientEndArgb)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                startCol.copy(alpha = if (effectiveDark) 0.42f else 0.25f),
                                baseBg,
                                endCol.copy(alpha = if (effectiveDark) 0.55f else 0.30f)
                            ),
                            start = Offset(if (dx < 0) 900f else 0f, if (dy < 0) 1400f else 0f),
                            end = Offset(if (dx >= 0) 900f else 0f, if (dy >= 0) 1400f else 0f)
                        )
                    )
            )
        } else if (uiStyleMode == UiStyleMode.NEON_MODERN) {
            val accent = customTheme?.let { Color(it.primaryArgb) }
                ?: if (effectiveDark) accentPalette.primaryDark else accentPalette.primaryLight
            val secondary = customTheme?.let { Color(it.secondaryArgb) }
                ?: if (effectiveDark) accentPalette.secondaryDark else accentPalette.secondaryLight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accent.copy(alpha = if (effectiveDark) 0.16f else 0.08f),
                                secondary.copy(alpha = if (effectiveDark) 0.05f else 0.02f),
                                Color.Transparent
                            )
                        )
                    )
            )
        } else if (uiStyleMode == UiStyleMode.MINIMALIST) {
            val accent = customTheme?.let { Color(it.primaryArgb) }
                ?: if (effectiveDark) accentPalette.primaryDark else accentPalette.primaryLight
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.06f), Color.Transparent)
                        )
                    )
            )
        }
    }
}
