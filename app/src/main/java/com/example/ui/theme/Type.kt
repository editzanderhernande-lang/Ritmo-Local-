package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppFontStyle

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val OutfitFamily = FontFamily(
    Font(R.font.outfit, FontWeight.Normal),
    Font(R.font.outfit, FontWeight.Medium),
    Font(R.font.outfit, FontWeight.SemiBold),
    Font(R.font.outfit, FontWeight.Bold)
)

val PlayfairDisplayFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.SemiBold),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val MontserratFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal),
    Font(R.font.montserrat, FontWeight.Medium),
    Font(R.font.montserrat, FontWeight.SemiBold),
    Font(R.font.montserrat, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
)

val PoppinsFamily = FontFamily(
    Font(R.font.poppins, FontWeight.Normal),
    Font(R.font.poppins, FontWeight.Medium),
    Font(R.font.poppins, FontWeight.SemiBold),
    Font(R.font.poppins, FontWeight.Bold)
)

fun displayFamilyFor(fontStyle: AppFontStyle): FontFamily = when (fontStyle) {
    AppFontStyle.SPACE_JAKARTA -> SpaceGroteskFamily
    AppFontStyle.OUTFIT_ROUNDED -> OutfitFamily
    AppFontStyle.PLAYFAIR_EDITORIAL -> PlayfairDisplayFamily
    AppFontStyle.MONTSERRAT_URBAN -> MontserratFamily
    AppFontStyle.POPPINS_SOFT -> PoppinsFamily
    AppFontStyle.JETBRAINS_TECH -> JetBrainsMonoFamily
}

fun bodyFamilyFor(fontStyle: AppFontStyle): FontFamily = when (fontStyle) {
    AppFontStyle.SPACE_JAKARTA -> PlusJakartaSansFamily
    AppFontStyle.OUTFIT_ROUNDED -> OutfitFamily
    AppFontStyle.PLAYFAIR_EDITORIAL -> InterFamily
    AppFontStyle.MONTSERRAT_URBAN -> InterFamily
    AppFontStyle.POPPINS_SOFT -> PoppinsFamily
    AppFontStyle.JETBRAINS_TECH -> InterFamily
}

fun typographyFor(fontStyle: AppFontStyle, fontSizeScale: Float = 1.0f): Typography {
    val scale = fontSizeScale.coerceIn(0.85f, 1.25f)
    val display = displayFamilyFor(fontStyle)
    val body = bodyFamilyFor(fontStyle)
    val labelMono = if (fontStyle == AppFontStyle.PLAYFAIR_EDITORIAL) InterFamily else JetBrainsMonoFamily

    return Typography(
        displayLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = (36f * scale).sp,
            lineHeight = (42f * scale).sp,
            letterSpacing = (-0.5).sp
        ),
        displaySmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = (28f * scale).sp,
            lineHeight = (34f * scale).sp,
            letterSpacing = (-0.25).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.Bold,
            fontSize = (24f * scale).sp,
            lineHeight = (30f * scale).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = (20f * scale).sp,
            lineHeight = (26f * scale).sp
        ),
        titleLarge = TextStyle(
            fontFamily = display,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18f * scale).sp,
            lineHeight = (24f * scale).sp
        ),
        titleMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = (16f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        titleSmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = (15f * scale).sp,
            lineHeight = (22f * scale).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        bodySmall = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.Normal,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelLarge = TextStyle(
            fontFamily = body,
            fontWeight = FontWeight.SemiBold,
            fontSize = (14f * scale).sp,
            lineHeight = (20f * scale).sp
        ),
        labelMedium = TextStyle(
            fontFamily = labelMono,
            fontWeight = FontWeight.Medium,
            fontSize = (12f * scale).sp,
            lineHeight = (16f * scale).sp
        ),
        labelSmall = TextStyle(
            fontFamily = labelMono,
            fontWeight = FontWeight.Normal,
            fontSize = (11f * scale).sp,
            lineHeight = (14f * scale).sp
        )
    )
}

val AppTypography = typographyFor(AppFontStyle.SPACE_JAKARTA)
