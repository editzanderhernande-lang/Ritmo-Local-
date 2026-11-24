package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

data class CoverPaletteColors(
    val dominant: Color,
    val vibrant: Color,
    val bgTop: Color,
    val bgBottom: Color
)

private val coverPaletteCache = LruCache<String, CoverPaletteColors>(24)

fun defaultCoverPalette(fallbackPrimary: Color, isDark: Boolean): CoverPaletteColors {
    return if (isDark) {
        CoverPaletteColors(
            dominant = fallbackPrimary,
            vibrant = lerp(fallbackPrimary, Color.White, 0.2f),
            bgTop = lerp(Color(0xFF100B1C), fallbackPrimary, 0.34f),
            bgBottom = Color(0xFF07050D)
        )
    } else {
        CoverPaletteColors(
            dominant = fallbackPrimary,
            vibrant = lerp(fallbackPrimary, Color(0xFF1E1B4B), 0.15f),
            bgTop = lerp(Color(0xFFF6F4FA), fallbackPrimary, 0.24f),
            bgBottom = Color(0xFFF9F8FC)
        )
    }
}

suspend fun extractCoverPaletteColors(
    context: Context,
    albumArtUri: String?,
    fallbackPrimary: Color,
    isDark: Boolean
): CoverPaletteColors {
    if (albumArtUri.isNullOrBlank()) {
        return defaultCoverPalette(fallbackPrimary, isDark)
    }
    val cacheKey = "$albumArtUri|$isDark|${fallbackPrimary.toArgb()}"
    coverPaletteCache.get(cacheKey)?.let { return it }

    val extracted = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 8
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            bitmap = when {
                albumArtUri.startsWith("/") -> {
                    val f = File(albumArtUri)
                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath, opts) else null
                }
                albumArtUri.startsWith("file://") -> {
                    val path = Uri.parse(albumArtUri).path
                    if (!path.isNullOrBlank()) BitmapFactory.decodeFile(path, opts) else null
                }
                albumArtUri.startsWith("content://") -> {
                    context.contentResolver.openInputStream(Uri.parse(albumArtUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }
                else -> null
            }

            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                defaultCoverPalette(fallbackPrimary, isDark)
            } else {
                computePaletteFromBitmap(bitmap, fallbackPrimary, isDark)
            }
        } catch (_: Throwable) {
            defaultCoverPalette(fallbackPrimary, isDark)
        } finally {
            bitmap?.recycle()
        }
    }

    coverPaletteCache.put(cacheKey, extracted)
    return extracted
}

private fun computePaletteFromBitmap(
    bitmap: Bitmap,
    fallbackPrimary: Color,
    isDark: Boolean
): CoverPaletteColors {
    val stepX = max(1, bitmap.width / 16)
    val stepY = max(1, bitmap.height / 16)
    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0

    var bestVibrantColor: Color? = null
    var bestScore = -1f

    val hsv = FloatArray(3)
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val a = (pixel ushr 24) and 0xFF
            if (a >= 180) {
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                sumR += r
                sumG += g
                sumB += b
                count++

                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val sat = hsv[1]
                val value = hsv[2]
                // Favor rich saturated midtones for readable vibrant accents
                val score = sat * 2.2f + (1f - kotlin.math.abs(value - 0.62f))
                if (sat >= 0.25f && value in 0.22f..0.95f && score > bestScore) {
                    bestScore = score
                    bestVibrantColor = Color(r, g, b)
                }
            }
            x += stepX
        }
        y += stepY
    }

    if (count == 0) {
        return defaultCoverPalette(fallbackPrimary, isDark)
    }

    val avgDominant = Color(
        red = ((sumR / count).toInt()).coerceIn(0, 255),
        green = ((sumG / count).toInt()).coerceIn(0, 255),
        blue = ((sumB / count).toInt()).coerceIn(0, 255)
    )
    val rawVibrant = bestVibrantColor ?: avgDominant

    return if (isDark) {
        val liftedVibrant = ensureMinLuminance(rawVibrant, minVal = 0.58f)
        CoverPaletteColors(
            dominant = avgDominant,
            vibrant = liftedVibrant,
            bgTop = lerp(Color(0xFF120E1D), rawVibrant, 0.44f),
            bgBottom = lerp(Color(0xFF06040B), avgDominant, 0.16f)
        )
    } else {
        val richAccent = clampForLightSurface(rawVibrant)
        CoverPaletteColors(
            dominant = avgDominant,
            vibrant = richAccent,
            bgTop = lerp(Color(0xFFF6F4FB), rawVibrant, 0.30f),
            bgBottom = lerp(Color(0xFFFCFBFE), avgDominant, 0.10f)
        )
    }
}

private fun ensureMinLuminance(color: Color, minVal: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = max(hsv[1], 0.42f)
    hsv[2] = max(hsv[2], minVal)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

private fun clampForLightSurface(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[1] = max(hsv[1], 0.50f)
    hsv[2] = min(hsv[2], 0.72f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
