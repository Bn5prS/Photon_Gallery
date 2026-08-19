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

private fun createOutfitFamily(): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.outfit_variable,
            weight = FontWeight.Light,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(300)
            )
        ),
        Font(
            resId = R.font.outfit_variable,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400)
            )
        ),
        Font(
            resId = R.font.outfit_variable,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500)
            )
        ),
        Font(
            resId = R.font.outfit_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(600)
            )
        ),
        Font(
            resId = R.font.outfit_variable,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700)
            )
        ),
        Font(
            resId = R.font.outfit_variable,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800)
            )
        )
    )
}

private fun createUrbanistFamily(): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.urbanist_variable,
            weight = FontWeight.Light,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(300)
            )
        ),
        Font(
            resId = R.font.urbanist_variable,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400)
            )
        ),
        Font(
            resId = R.font.urbanist_variable,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500)
            )
        ),
        Font(
            resId = R.font.urbanist_variable,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(600)
            )
        ),
        Font(
            resId = R.font.urbanist_variable,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700)
            )
        ),
        Font(
            resId = R.font.urbanist_variable,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800)
            )
        )
    )
}

private fun createPlusJakartaFamily(): FontFamily {
    return FontFamily(
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight.Light,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(300)
            )
        ),
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400)
            )
        ),
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500)
            )
        ),
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(600)
            )
        ),
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700)
            )
        ),
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight.ExtraBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(800)
            )
        )
    )
}

val OutfitFont = createOutfitFamily()
val UrbanistFont = createUrbanistFamily()
val PlusJakartaSansFont = createPlusJakartaFamily()

fun appTypography(useSystemFont: Boolean): Typography {
    val outfitFamily = if (useSystemFont) FontFamily.Default else createOutfitFamily()
    val urbanistFamily = if (useSystemFont) FontFamily.Default else createUrbanistFamily()
    val plusJakartaFamily = if (useSystemFont) FontFamily.Default else createPlusJakartaFamily()

    return Typography(
        displayLarge = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = (-0.3).sp
        ),
        displaySmall = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.2).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.3).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.2).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.1).sp
        ),
        titleLarge = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.1).sp
        ),
        titleMedium = TextStyle(
            fontFamily = outfitFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = urbanistFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.4.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = plusJakartaFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = plusJakartaFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = plusJakartaFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = plusJakartaFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = plusJakartaFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp
        ),
        labelSmall = TextStyle(
            fontFamily = urbanistFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 0.6.sp
        )
    )
}
