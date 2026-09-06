@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme

import androidx.compose.ui.graphics.compositeOver

// ─────────────────────────────────────────────────────────────────────────────
//  PhotonGalleryTheme — M3 Expressive entry point
//
//  Powered by MaterialKolor (https://github.com/jordond/MaterialKolor):
//    • rememberDynamicColorScheme() — full HCT tonal palette from any seed
//    • animateColorScheme()        — spring-physics animated theme transitions
//    • PaletteStyle enum           — TonalSpot, Neutral, Vibrant, Expressive…
//    • isAmoled                    — pure-black AMOLED dark surfaces built-in
//    • per-slot overrides          — independent secondary + tertiary seeds
//    • Reduced contrast (-1.0)     — pastel/soft palette mode
//
//  Dynamic Color: wallpaper-derived seed on Android 12+, falling back to appSeedColor.
//  All palette generation runs in-process via the HCT color space math.
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PhotonGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    useAmoledBlack: Boolean = false,
    appSeedColor: Int = 0xFF0A6EFF.toInt(),
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    contrastLevel: Double = 0.0,
    invertColors: Boolean = false,
    useSystemFont: Boolean = false,
    secondaryColorOverride: Int = -1,   // -1 = auto
    tertiaryColorOverride: Int = -1,    // -1 = auto
    animateTransitions: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current



    val systemSeedColor = remember(dynamicColor, context) {
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val color = context.getColor(android.R.color.system_accent1_500)
                if (color != 0) color else null
            } catch (e: Throwable) {
                null
            }
        } else null
    }

    var wallpaperSeedColor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(dynamicColor) {
        if (dynamicColor && systemSeedColor == null) {
            wallpaperSeedColor = WallpaperSeedExtractor.getWallpaperSeedColor(context)
        }
    }

    val seedArgb = if (dynamicColor) {
        systemSeedColor ?: wallpaperSeedColor ?: appSeedColor
    } else {
        appSeedColor
    }

    val schemeIsDark = if (invertColors) !darkTheme else darkTheme

    // MaterialKolor generates the full M3 tonal palette from a seed + style + contrast.
    val baseColorScheme = rememberDynamicColorScheme(
        seedColor = Color(seedArgb),
        isDark = schemeIsDark,
        isAmoled = useAmoledBlack,
        style = paletteStyle,
        contrastLevel = contrastLevel,
        secondary = if (secondaryColorOverride != -1) Color(secondaryColorOverride) else null,
        tertiary = if (tertiaryColorOverride != -1) Color(tertiaryColorOverride) else null,
    )

    val colorScheme = when {
        schemeIsDark && useAmoledBlack -> {
            baseColorScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceBright = Color(0xFF141414),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF080808),
                surfaceContainer = Color(0xFF0E0E0E),
                surfaceContainerHigh = Color(0xFF141414),
                surfaceContainerHighest = Color(0xFF1C1C1C),
                surfaceVariant = Color(0xFF161616),
            )
        }
        !schemeIsDark -> {
            // Soften surfaceContainerLowest from stark #FFFFFF with a warm 40% canvas tint (60% pure white)
            val softenedSheetWhite = baseColorScheme.surfaceContainer.copy(alpha = 0.40f).compositeOver(Color.White)
            baseColorScheme.copy(
                surfaceContainerLowest = softenedSheetWhite
            )
        }
        else -> {
            baseColorScheme
        }
    }

    // Optionally animate all color token changes using spring physics.
    val resolvedScheme = if (animateTransitions) animateColorScheme(colorScheme) else colorScheme

    val photonColors = when {
        useAmoledBlack && schemeIsDark -> AmoledPhotonColors
        schemeIsDark -> DarkPhotonColors
        else -> LightPhotonColors
    }

    MaterialExpressiveTheme(
        colorScheme = resolvedScheme,
        shapes = AppShapes,
        typography = appTypography(useSystemFont = useSystemFont, isDark = darkTheme),
        motionScheme = MotionScheme.expressive(),
    ) {
        // Provide harmonized accent colors blended toward dynamic primary and extended semantic colors
        val harmonized = harmonizedColors()
        CompositionLocalProvider(
            LocalHarmonizedColors provides harmonized,
            LocalPhotonColors provides photonColors,
            content = content,
        )
    }
}