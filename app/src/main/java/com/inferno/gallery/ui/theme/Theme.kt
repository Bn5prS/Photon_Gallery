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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.materialkolor.PaletteStyle
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicColorScheme

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
    appSeedColor: Int = 0xFF6750A4.toInt(),
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

    // ── Adaptive Density (app-wide responsive scaling) ────────────────────────
    // Design target: 393dp — the iQOO Neo 7 dp width (1080px ÷ density 2.75).
    // On screens narrower than that (e.g. POCO C61 = 360dp), ALL dp values
    // (padding, icon sizes, heights, border radii, etc.) scale down automatically
    // — zero changes needed in any individual screen file.
    //
    // sp text is intentionally preserved at its original physical size via
    // fontScale compensation: newFontScale = originalFontScale / scaleFactor,
    // so sp.toPx() = sp * density * fontScale stays constant regardless of device.
    //
    //  Device          dp-width   scaleFactor   effect
    //  iQOO Neo 7       393dp       1.000        no change (design target)
    //  POCO C61         360dp       0.916        ~8.4% smaller dp values
    //  320dp device     320dp       0.815        ~18.5% smaller dp values
    val configuration = LocalConfiguration.current
    val systemDensity = LocalDensity.current
    val adaptiveDensity = remember(configuration.screenWidthDp, systemDensity) {
        val designTargetDp = 393f
        val scaleFactor = (configuration.screenWidthDp / designTargetDp).coerceAtMost(1f)
        if (scaleFactor < 1f) {
            Density(
                density = systemDensity.density * scaleFactor,
                fontScale = systemDensity.fontScale / scaleFactor
            )
        } else {
            systemDensity
        }
    }
    // ─────────────────────────────────────────────────────────────────────────

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
    // Optional per-slot overrides let secondary/tertiary use independent seed colors.
    val baseColorScheme = rememberDynamicColorScheme(
        seedColor = Color(seedArgb),
        isDark = schemeIsDark,
        isAmoled = useAmoledBlack,
        style = paletteStyle,
        contrastLevel = contrastLevel,
        secondary = if (secondaryColorOverride != -1) Color(secondaryColorOverride) else null,
        tertiary = if (tertiaryColorOverride != -1) Color(tertiaryColorOverride) else null,
    )

    // Scheme-slot overrides below are the sanctioned exception to the
    // "no raw Color(...) in composables" rule (see .agents/rules/
    // m3-expressive-design.md §1): this is scheme *definition* — the same
    // thing lightColorScheme() does internally — not scheme *application*.
    // Screens must still consume roles via MaterialTheme.colorScheme only.
    val colorScheme = if (schemeIsDark && useAmoledBlack) {
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
    } else {
        baseColorScheme
    }

    // Optionally animate all color token changes using spring physics.
    val resolvedScheme = if (animateTransitions) animateColorScheme(colorScheme) else colorScheme

    MaterialExpressiveTheme(
        colorScheme = resolvedScheme,
        shapes = AppShapes,
        typography = appTypography(useSystemFont = useSystemFont, isDark = darkTheme),
        motionScheme = MotionScheme.expressive(),
    ) {
        // Provide harmonized accent colors blended toward dynamic primary
        val harmonized = harmonizedColors()
        CompositionLocalProvider(
            LocalDensity provides adaptiveDensity,
            LocalHarmonizedColors provides harmonized,
            content = content,
        )
    }
}