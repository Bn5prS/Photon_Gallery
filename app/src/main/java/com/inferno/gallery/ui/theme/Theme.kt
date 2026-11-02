@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

// ─────────────────────────────────────────────────────────────────────────────
//  PhotonGalleryTheme — Flagship M3 Expressive Theme Engine for Photon Gallery
//
//  • Dynamic 13-tone tonal palette generation via material-kolor with PaletteStyle.TonalSpot.
//  • When dynamicColor is true on Android 12+: derives the seed color from system wallpaper.
//  • When a curated palette preset is active: derives the seed color from the selected palette.
//  • Full 5-level surface container hierarchy:
//    - Canvas / scaffold background: surface & background
//    - Recessed / grouping cards: surfaceContainerLow
//    - Standard cards / items: surfaceContainer
//    - Elevated dialogs / sheets: surfaceContainerHigh
//    - Floating badges / chips: surfaceContainerHighest
//  • AMOLED dark mode: pure Color.Black canvas while surfaceContainer & surfaceContainerHigh
//    retain distinct tinted dark tones so cards stand out cleanly above OLED pitch black.
//  • Fully memoized typography, photon colors, and harmonized colors for zero frame drops.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PhotonGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    colorPresetName: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "default_blue",
    paletteStyleName: String = "TonalSpot",
    appSeedColor: Int = -1,
    useAmoledBlack: Boolean = false,
    useSystemFont: Boolean = false,
    fontScale: Float = 1.0f,
    animateTransitions: Boolean = true,
    motionStyle: String = "expressive",
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Memoize the target color scheme to prevent re-querying system resources on arbitrary recompositions
    val targetColorScheme: ColorScheme = remember(darkTheme, dynamicColor, colorPresetName, paletteStyleName, appSeedColor, useAmoledBlack, context) {
        val useDynamic = (dynamicColor || colorPresetName == "dynamic") &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (colorPresetName == "dynamic" || dynamicColor) &&
            (colorPresetName != "custom")

        val seedColor: Color = if (colorPresetName == "custom" && appSeedColor != -1) {
            Color(appSeedColor)
        } else if (useDynamic) {
            dynamicLightColorScheme(context).primary
        } else {
            val palette = ThemePalettes.getById(colorPresetName)
            palette.seedColor
        }

        val resolvedStyle = try {
            PaletteStyle.valueOf(paletteStyleName)
        } catch (e: Exception) {
            PaletteStyle.TonalSpot
        }

        val effectiveSeed = if (resolvedStyle == PaletteStyle.Monochrome) Color.Gray else seedColor

        val baseScheme = dynamicColorScheme(
            seedColor = effectiveSeed,
            isDark = darkTheme,
            isAmoled = useAmoledBlack && darkTheme,
            style = resolvedStyle,
        )

        if (darkTheme && useAmoledBlack) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceBright = Color(0xFF2C2C2C),
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF141414),
                surfaceContainer = Color(0xFF1E1E1E),
                surfaceContainerHigh = Color(0xFF282828),
                surfaceContainerHighest = Color(0xFF333333),
                surfaceVariant = baseScheme.surfaceVariant,
            )
        } else {
            baseScheme.copy(
                background = baseScheme.surface,
            )
        }
    }

    // Track darkTheme and colorPresetName to run animated scheme transitions when flipping between Light <-> Dark mode or switching palettes.
    var previousDarkTheme by remember { mutableStateOf(darkTheme) }
    var previousColorPreset by remember { mutableStateOf(colorPresetName) }
    var previousDynamicColor by remember { mutableStateOf(dynamicColor) }
    var shouldAnimateTheme by remember { mutableStateOf(false) }

    if (darkTheme != previousDarkTheme || colorPresetName != previousColorPreset || dynamicColor != previousDynamicColor) {
        previousDarkTheme = darkTheme
        previousColorPreset = colorPresetName
        previousDynamicColor = dynamicColor
        shouldAnimateTheme = true
    }

    val resolvedScheme = if (animateTransitions && shouldAnimateTheme) {
        animateColorScheme(
            target = targetColorScheme,
            onFinished = { shouldAnimateTheme = false }
        )
    } else {
        targetColorScheme
    }

    val photonColors = remember(useAmoledBlack, darkTheme) {
        when {
            useAmoledBlack && darkTheme -> AmoledPhotonColors
            darkTheme -> DarkPhotonColors
            else -> LightPhotonColors
        }
    }

    val isStandardMotion = (motionStyle == "standard")
    MotionTokens.isStandardMotion = isStandardMotion
    val motionScheme = remember(isStandardMotion) {
        if (isStandardMotion) MotionScheme.standard() else MotionScheme.expressive()
    }

    // Memoize typography so font families and variation settings are never reallocated on color changes
    val typography = remember(useSystemFont, darkTheme) {
        appTypography(useSystemFont = useSystemFont, isDark = darkTheme)
    }

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, fontScale) {
        androidx.compose.ui.unit.Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * fontScale
        )
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides scaledDensity
    ) {
        MaterialExpressiveTheme(
            colorScheme = resolvedScheme,
            shapes = AppShapes,
            typography = typography,
            motionScheme = motionScheme,
        ) {
            val harmonized = harmonizedColors()
            CompositionLocalProvider(
                LocalHarmonizedColors provides harmonized,
                LocalPhotonColors provides photonColors,
                content = content,
            )
        }
    }
}

/**
 * Fast, lightweight Compose implementation of ColorScheme surface animation for Light/Dark mode transitions.
 * Uses a crisp 150ms tween on active surface and accent tokens.
 */
@Composable
private fun animateColorScheme(
    target: ColorScheme,
    animationSpec: AnimationSpec<Color> = tween(durationMillis = 150, easing = FastOutSlowInEasing),
    onFinished: () -> Unit = {},
): ColorScheme {
    val bg = animateColorAsState(
        target.background,
        animationSpec,
        label = "bg",
        finishedListener = { onFinished() }
    ).value
    val surface = animateColorAsState(target.surface, animationSpec, label = "surface").value
    val surfaceDim = animateColorAsState(target.surfaceDim, animationSpec, label = "surfaceDim").value
    val surfaceLow = animateColorAsState(target.surfaceContainerLow, animationSpec, label = "surfaceLow").value
    val surfaceLowest = animateColorAsState(target.surfaceContainerLowest, animationSpec, label = "surfaceLowest").value
    val surfaceContainer = animateColorAsState(target.surfaceContainer, animationSpec, label = "surfaceContainer").value
    val surfaceHigh = animateColorAsState(target.surfaceContainerHigh, animationSpec, label = "surfaceHigh").value
    val surfaceHighest = animateColorAsState(target.surfaceContainerHighest, animationSpec, label = "surfaceHighest").value

    val primary = animateColorAsState(target.primary, animationSpec, label = "primary").value
    val onPrimary = animateColorAsState(target.onPrimary, animationSpec, label = "onPrimary").value
    val primaryContainer = animateColorAsState(target.primaryContainer, animationSpec, label = "primaryContainer").value
    val onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value

    return target.copy(
        background = bg,
        surface = surface,
        surfaceDim = surfaceDim,
        surfaceContainerLow = surfaceLow,
        surfaceContainerLowest = surfaceLowest,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = surfaceHighest,
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
    )
}
