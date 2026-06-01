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

// ─────────────────────────────────────────────────────────────────────────────
//  Variable Font: Roboto Flex
//
//  Roboto Flex exposes several variation axes that we leverage for the
//  M3 Expressive Emphasized Typography system:
//
//    • wght  (Weight)  — 100..1000   — controls stroke thickness
//    • wdth  (Width)   — 25..151     — controls horizontal proportion
//    • opsz  (Optical Size) — 8..144 — optimises glyph shapes for size
//
//  By dynamically adjusting these axes per type-scale role we create an
//  Emphasized hierarchy: display/headline roles use wider, heavier settings
//  while body/label roles stay narrow and light, producing a clear visual
//  contrast without changing the font family.
// ─────────────────────────────────────────────────────────────────────────────

// ── Display / Headline — expanded width, heavy weight ──────────────────────
private fun displayFontFamily(weight: Int = 700, width: Float = 120f, opticalSize: Float = 48f) =
    FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    )

// ── Title — semi-expanded width, medium-bold weight ────────────────────────
private fun titleFontFamily(weight: Int = 600, width: Float = 110f, opticalSize: Float = 24f) =
    FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    )

// ── Body — normal width, regular weight ────────────────────────────────────
private fun bodyFontFamily(weight: Int = 400, width: Float = 100f, opticalSize: Float = 14f) =
    FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    )

// ── Label — slightly condensed width, medium weight ────────────────────────
private fun labelFontFamily(weight: Int = 500, width: Float = 95f, opticalSize: Float = 11f) =
    FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width),
                FontVariation.Setting("opsz", opticalSize),
            ),
        )
    )

// ─────────────────────────────────────────────────────────────────────────────
//  M3 Expressive Typography Scale — Emphasized
//
//  Each role dynamically shifts font weight AND width to create visual
//  hierarchy, as prescribed by the M3 Expressive spec. The display &
//  headline families use wider glyphs for presence, while body & label
//  families stay compact for readability.
// ─────────────────────────────────────────────────────────────────────────────
val AppTypography = Typography(
    // ── Display ─────────────────────────────────────────────────────────────
    displayLarge = TextStyle(
        fontFamily = displayFontFamily(weight = 800, width = 125f, opticalSize = 57f),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = displayFontFamily(weight = 700, width = 120f, opticalSize = 45f),
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = displayFontFamily(weight = 600, width = 115f, opticalSize = 36f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp,
    ),

    // ── Headline ────────────────────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily = displayFontFamily(weight = 700, width = 115f, opticalSize = 32f),
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = displayFontFamily(weight = 600, width = 110f, opticalSize = 28f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = titleFontFamily(weight = 600, width = 108f, opticalSize = 24f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),

    // ── Title ───────────────────────────────────────────────────────────────
    titleLarge = TextStyle(
        fontFamily = titleFontFamily(weight = 600, width = 108f, opticalSize = 22f),
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = titleFontFamily(weight = 550, width = 105f, opticalSize = 16f),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = titleFontFamily(weight = 500, width = 105f, opticalSize = 14f),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ── Body ────────────────────────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily = bodyFontFamily(weight = 400, width = 100f, opticalSize = 16f),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = bodyFontFamily(weight = 400, width = 100f, opticalSize = 14f),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = bodyFontFamily(weight = 400, width = 100f, opticalSize = 12f),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // ── Label ───────────────────────────────────────────────────────────────
    labelLarge = TextStyle(
        fontFamily = labelFontFamily(weight = 550, width = 98f, opticalSize = 14f),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = labelFontFamily(weight = 500, width = 95f, opticalSize = 12f),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = labelFontFamily(weight = 500, width = 95f, opticalSize = 11f),
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)