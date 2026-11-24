package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlayerVisualStyle
import com.example.model.VisualizerStyle
import com.example.player.AudioAnalysisBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val VISUALIZER_BAR_COUNT = 20

@Composable
fun AudioVisualizerView(
    @Suppress("UNUSED_PARAMETER") audioSessionId: Int,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    showVisualizer: Boolean,
    accentColor: Color,
    secondaryColor: Color,
    style: PlayerVisualStyle,
    visualizerStyle: VisualizerStyle = VisualizerStyle.BARS,
    intensity: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    if (!showVisualizer) return

    val clampedIntensity = intensity.coerceIn(0.35f, 1.5f)
    val shouldStreamLive = isPlaying && animationsEnabled

    DisposableEffect(shouldStreamLive) {
        if (shouldStreamLive) {
            AudioAnalysisBus.registerCollector()
        }
        onDispose {
            if (shouldStreamLive) {
                AudioAnalysisBus.unregisterCollector()
            }
        }
    }

    val liveSnapshot by AudioAnalysisBus.snapshot.collectAsStateWithLifecycle()
    var fallbackBands by remember { mutableStateOf(FloatArray(VISUALIZER_BAR_COUNT) { 0.12f }) }

    LaunchedEffect(shouldStreamLive, clampedIntensity) {
        if (!shouldStreamLive) {
            fallbackBands = FloatArray(VISUALIZER_BAR_COUNT) { idx ->
                val centerDist = abs(idx - VISUALIZER_BAR_COUNT / 2f) / (VISUALIZER_BAR_COUNT / 2f)
                0.08f + (1f - centerDist) * 0.08f
            }
            return@LaunchedEffect
        }
        var phase = 0f
        while (isActive) {
            val staleMs = System.currentTimeMillis() - liveSnapshot.updatedAtMs
            if (!liveSnapshot.isLiveAudio || staleMs > 420L) {
                phase += 0.24f
                fallbackBands = FloatArray(VISUALIZER_BAR_COUNT) { idx ->
                    val arch = 1f - (abs(idx - (VISUALIZER_BAR_COUNT - 1) / 2f) / (VISUALIZER_BAR_COUNT * 0.62f))
                    val wave1 = 0.5f + 0.5f * sin(phase + idx * 0.58f)
                    val wave2 = 0.5f + 0.5f * sin(phase * 1.45f - idx * 0.37f)
                    val valNorm = (0.12f + arch * (0.34f * wave1 + 0.42f * wave2)) * clampedIntensity
                    valNorm.coerceIn(0.08f, 1.0f)
                }
            }
            delay(85L)
        }
    }

    val liveFresh = shouldStreamLive && liveSnapshot.isLiveAudio &&
        (System.currentTimeMillis() - liveSnapshot.updatedAtMs) <= 450L

    val activeMagnitudes = remember(liveFresh, liveSnapshot, fallbackBands, clampedIntensity) {
        if (liveFresh && liveSnapshot.bands.size >= VISUALIZER_BAR_COUNT) {
            FloatArray(VISUALIZER_BAR_COUNT) { idx ->
                val bassBoost = if (idx < 5) liveSnapshot.bassEnergy * 0.18f else 0f
                val trebleBoost = if (idx >= 14) liveSnapshot.trebleEnergy * 0.18f else 0f
                ((liveSnapshot.bands[idx] + bassBoost + trebleBoost) * clampedIntensity).coerceIn(0.08f, 1.0f)
            }
        } else {
            fallbackBands
        }
    }

    val barBrush = remember(accentColor, secondaryColor) {
        Brush.verticalGradient(listOf(secondaryColor, accentColor))
    }
    val horizontalBrush = remember(accentColor, secondaryColor) {
        Brush.horizontalGradient(listOf(accentColor, secondaryColor, accentColor))
    }

    val heightDp = when (visualizerStyle) {
        VisualizerStyle.CIRCLES -> 46.dp
        VisualizerStyle.SPECTRUM -> 42.dp
        else -> 36.dp
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .testTag("player_audio_visualizer")
    ) {
        val count = activeMagnitudes.size
        if (count == 0 || size.width <= 0f || size.height <= 0f) return@Canvas

        when (visualizerStyle) {
            VisualizerStyle.BARS -> {
                val slotWidth = size.width / count.toFloat()
                val barWidth = when (style) {
                    PlayerVisualStyle.MINIMAL_ZEN -> (slotWidth * 0.32f).coerceIn(2.5f, 6f)
                    PlayerVisualStyle.VINYL_STUDIO -> (slotWidth * 0.48f).coerceIn(3.5f, 10f)
                    else -> (slotWidth * 0.56f).coerceIn(4f, 12f)
                }
                val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
                for (i in 0 until count) {
                    val mag = activeMagnitudes[i].coerceIn(0.08f, 1f)
                    val barHeight = (size.height * mag).coerceAtLeast(4f)
                    val left = i * slotWidth + (slotWidth - barWidth) / 2f
                    val top = if (style == PlayerVisualStyle.VINYL_STUDIO || style == PlayerVisualStyle.MINIMAL_ZEN) {
                        (size.height - barHeight) / 2f
                    } else {
                        size.height - barHeight
                    }
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = corner,
                        alpha = if (isPlaying) 0.92f else 0.35f
                    )
                    if (style == PlayerVisualStyle.NEON_PULSE && isPlaying && animationsEnabled) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.72f),
                            topLeft = Offset(left, (top - 3f).coerceAtLeast(0f)),
                            size = Size(barWidth, 2.5f),
                            cornerRadius = corner
                        )
                    }
                }
            }
            VisualizerStyle.WAVES -> {
                val midY = size.height / 2f
                val stepX = size.width / (count - 1).coerceAtLeast(1)
                val upperPath = Path()
                val lowerPath = Path()
                for (i in 0 until count) {
                    val x = i * stepX
                    val env = sin((i.toFloat() / (count - 1)) * PI.toFloat())
                    val amp = activeMagnitudes[i] * (size.height * 0.44f) * (0.4f + 0.6f * env)
                    if (i == 0) {
                        upperPath.moveTo(x, midY - amp)
                        lowerPath.moveTo(x, midY + amp * 0.65f)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevEnv = sin(((i - 1).toFloat() / (count - 1)) * PI.toFloat())
                        val prevAmp = activeMagnitudes[i - 1] * (size.height * 0.44f) * (0.4f + 0.6f * prevEnv)
                        val cx = (prevX + x) / 2f
                        upperPath.cubicTo(cx, midY - prevAmp, cx, midY - amp, x, midY - amp)
                        lowerPath.cubicTo(cx, midY + prevAmp * 0.65f, cx, midY + amp * 0.65f, x, midY + amp * 0.65f)
                    }
                }
                drawPath(
                    path = upperPath,
                    brush = horizontalBrush,
                    alpha = if (isPlaying) 0.95f else 0.4f,
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = lowerPath,
                    brush = horizontalBrush,
                    alpha = if (isPlaying) 0.45f else 0.2f,
                    style = Stroke(width = 2.0f, cap = StrokeCap.Round)
                )
            }
            VisualizerStyle.CIRCLES -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                val bassNorm = if (liveFresh) liveSnapshot.bassEnergy else activeMagnitudes.take(5).average().toFloat()
                val midNorm = if (liveFresh) liveSnapshot.midEnergy else activeMagnitudes.slice(6..13).average().toFloat()
                val trebleNorm = if (liveFresh) liveSnapshot.trebleEnergy else activeMagnitudes.takeLast(5).average().toFloat()
                val maxRadius = (size.height * 0.46f)
                val radii = listOf(
                    (maxRadius * (0.35f + 0.65f * bassNorm * clampedIntensity)).coerceAtLeast(6f),
                    (maxRadius * (0.30f + 0.58f * midNorm * clampedIntensity)).coerceAtLeast(5f),
                    (maxRadius * (0.24f + 0.50f * trebleNorm * clampedIntensity)).coerceAtLeast(4f)
                )
                radii.forEachIndexed { idx, r ->
                    drawCircle(
                        color = if (idx % 2 == 0) accentColor else secondaryColor,
                        radius = r.coerceAtMost(maxRadius),
                        center = center,
                        alpha = if (isPlaying) (0.85f - idx * 0.2f) else 0.3f,
                        style = Stroke(width = 3.0f - idx * 0.5f)
                    )
                }
                // Symmetrical radial satellite dots synced with bands
                val dotCount = 10
                val usableHalfW = (size.width * 0.42f)
                for (i in 0 until dotCount) {
                    val mag = activeMagnitudes[(i * 2).coerceIn(0, count - 1)]
                    val dx = (i + 1) / dotCount.toFloat() * usableHalfW
                    val r = (2.5f + mag * 5.5f).coerceIn(2f, 8f)
                    drawCircle(color = accentColor, radius = r, center = Offset(center.x - dx, center.y), alpha = 0.78f)
                    drawCircle(color = secondaryColor, radius = r, center = Offset(center.x + dx, center.y), alpha = 0.78f)
                }
            }
            VisualizerStyle.SPECTRUM -> {
                val halfH = size.height / 2f
                val slotWidth = size.width / count.toFloat()
                val barW = (slotWidth * 0.48f).coerceIn(3f, 9f)
                val corner = CornerRadius(barW / 2f, barW / 2f)
                for (i in 0 until count) {
                    val mag = activeMagnitudes[i].coerceIn(0.08f, 1f)
                    val armH = (halfH * mag * 0.95f).coerceAtLeast(2.5f)
                    val left = i * slotWidth + (slotWidth - barW) / 2f
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(left, halfH - armH),
                        size = Size(barW, armH * 2f),
                        cornerRadius = corner,
                        alpha = if (isPlaying) 0.92f else 0.35f
                    )
                }
            }
        }
    }
}
