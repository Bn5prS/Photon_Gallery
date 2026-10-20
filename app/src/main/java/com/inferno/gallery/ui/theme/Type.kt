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
    roundness: Float = 100f,
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

// Global references to bundled Google Fonts
val GoogleSansFlexFont = googleSansFlex()

/**
 * Headline style configured with font weight 700 and 28sp font size.
 */
val EditorialPunchyHeadline = TextStyle(
    fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 28f, roundness = 100f),
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.2).sp
)

/**
 * Spaced title style configured with font weight 400, 24sp font size, and 1.0sp tracking.
 */
val TechnicalMinimalistTitle = TextStyle(
    fontFamily = googleSansFlex(weight = 400, width = 100f, opticalSize = 24f, roundness = 100f),
    fontWeight = FontWeight.Normal,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = 1.0.sp
)

/**
 * Timeline date group header style (Google Sans Flex Bold 17.5sp, tracking 0sp).
 */
val TimelineDateHeaderStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 18f, roundness = 100f),
    fontWeight = FontWeight.Bold,
    fontSize = 17.5.sp,
    lineHeight = 22.5.sp,
    letterSpacing = 0.sp
)

/**
 * EXIF parameter value style (Google Sans Flex SemiBold 14sp, tracking 0.15sp).
 */
val TechnicalParamValueStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 14f, roundness = 100f),
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.15.sp
)

/**
 * EXIF parameter label style (Google Sans Flex Medium 11sp, tracking 0.2sp).
 */
val TechnicalParamLabelStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 500, width = 100f, opticalSize = 11f, roundness = 100f),
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.2.sp
)

/**
 * Section Header Title Style (Google Sans Flex SemiBold 17sp)
 */
val SectionHeaderTitleStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 17f, roundness = 100f),
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 23.sp,
    letterSpacing = 0.sp
)

/**
 * Sliding Navigation Pill Dock Label Style (Google Sans Flex SemiBold 12sp)
 */
val NavDockLabelStyle = TextStyle(
    fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 12f, roundness = 100f),
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.1.sp
)

fun appTypography(useSystemFont: Boolean, isDark: Boolean = false): Typography {
    if (useSystemFont) {
        return Typography(
            displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
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

    val grade = if (isDark) 25f else 0f

    return Typography(
        displayLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 56f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 44f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 36f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 38f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 30f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 24f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 700, width = 100f, opticalSize = 22f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 17f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 14f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 400, width = 100f, opticalSize = 16f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 400, width = 100f, opticalSize = 14f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = googleSansFlex(weight = 450, width = 100f, opticalSize = 12f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 14f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 12f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelSmall = TextStyle(
            fontFamily = googleSansFlex(weight = 600, width = 100f, opticalSize = 11f, roundness = 100f, grade = grade, slant = 0f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.3.sp
        )
    )
}
