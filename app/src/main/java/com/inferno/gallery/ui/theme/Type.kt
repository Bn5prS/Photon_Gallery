@file:OptIn(ExperimentalTextApi::class)

package com.inferno.gallery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.inferno.gallery.R

/**
 * Creates a Google Sans Flex [FontFamily] tailored with exact variable axes:
 * @param weight Weight axis 'wght' (1..1000)
 * @param width Width axis 'wdth' (25..151, 100 = normal)
 * @param opticalSize Optical size axis 'opsz' (6..144)
 * @param roundness Roundness axis 'ROND' (0..100)
 * @param grade Grade axis 'GRAD' (0..100)
 * @param slant Slant axis 'slnt' (-10..0)
 */
fun googleSansFlex(
    weight: Int = 400,
    width: Float = 100f,
    opticalSize: Float = 14f,
    roundness: Float = 0f,
    grade: Float = 0f,
    slant: Float = 0f
): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.google_sans_flex,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.Setting("opsz", opticalSize),
                FontVariation.Setting("ROND", roundness),
                FontVariation.Setting("GRAD", grade),
                FontVariation.slant(slant)
            )
        )
    )
}

// Global reference
val GoogleSansFlexFont = googleSansFlex()
val OutfitFont = GoogleSansFlexFont
val UrbanistFont = googleSansFlex(width = 65f)
val PlusJakartaSansFont = GoogleSansFlexFont

/**
 * Style 3: Punchy Heavy Editorial Headline ("Your tools" style)
 * Extra bold/black weight, solid width, large optical size, sharp geometric terminals,
 * and tight negative tracking.
 */
val EditorialPunchyHeadline = TextStyle(
    fontFamily = googleSansFlex(weight = 900, width = 108f, opticalSize = 56f, roundness = 0f),
    fontWeight = FontWeight.Black,
    fontSize = 38.sp,
    lineHeight = 44.sp,
    letterSpacing = (-1.6).sp
)

/**
 * Style 4: Technical Spaced Minimalist Title ("Settings" style)
 * Clean regular/book weight with generous tracking giving an architectural, technical aesthetic.
 */
val TechnicalMinimalistTitle = TextStyle(
    fontFamily = googleSansFlex(weight = 400, width = 100f, opticalSize = 22f, roundness = 0f),
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 3.2.sp
)

/**
 * Architectural Condensed Timeline Date Header ("TODAY", "YESTERDAY", "DEC 24, 2025")
 * Condensed geometric tracking with clean stroke distribution for editorial timeline dividers.
 */
val TimelineDateHeaderStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 750, width = 82f, opticalSize = 13f, roundness = 0f),
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.2.sp
)

/**
 * Technical Camera Spec Value ("f/1.8", "1/250s", "ISO 64", "24mm")
 * Precise, balanced modern camera spec indicator.
 */
val TechnicalParamValueStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 750, width = 106f, opticalSize = 14f, roundness = 0f),
    fontWeight = FontWeight.Bold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.3.sp
)

/**
 * Technical Camera Spec Label ("APERTURE", "SHUTTER", "ISO", "FOCAL LENGTH")
 * Clean uppercase micro-header with wide tracking.
 */
val TechnicalParamLabelStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 650, width = 85f, opticalSize = 11f, roundness = 0f),
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.8.sp
)

/**
 * Section Header Title Style (with the primary vertical accent bar)
 */
val SectionHeaderTitleStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 750, width = 98f, opticalSize = 15f, roundness = 0f),
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.1.sp
)

/**
 * Sliding Navigation Pill Dock Label Style
 */
val NavDockLabelStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 700, width = 104f, opticalSize = 12f, roundness = 0f),
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.3.sp
)

fun appTypography(useSystemFont: Boolean, isDark: Boolean = false): Typography {
    val grade = if (isDark) 30f else 0f

    if (useSystemFont) {
        return Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
            displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.3).sp),
            displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
            headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.2).sp),
            headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp, letterSpacing = (-0.1).sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
            titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
            titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.15.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp)
        )
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 900, width = 110f, opticalSize = 56f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Black,
            fontSize = 44.sp,
            lineHeight = 50.sp,
            letterSpacing = (-1.8).sp
        ),
        displayMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 900, width = 108f, opticalSize = 44f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            letterSpacing = (-1.4).sp
        ),
        displaySmall = TextStyle(
            fontFamily = googleSansFlex(weight = 850, width = 106f, opticalSize = 36f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-1.0).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 900, width = 108f, opticalSize = 38f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-1.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 800, width = 104f, opticalSize = 30f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.6).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = googleSansFlex(weight = 750, width = 100f, opticalSize = 24f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.3).sp
        ),
        titleLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 750, width = 102f, opticalSize = 22f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.2).sp
        ),
        titleMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 650, width = 100f, opticalSize = 17f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 85f, opticalSize = 14f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 400, width = 100f, opticalSize = 16f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 400, width = 100f, opticalSize = 14f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = googleSansFlex(weight = 450, width = 95f, opticalSize = 12f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 105f, opticalSize = 14f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 105f, opticalSize = 12f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = googleSansFlex(weight = 500, width = 100f, opticalSize = 11f, roundness = 0f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.4.sp
        )
    )
}
