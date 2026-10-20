package com.inferno.gallery.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Curated Preset Theme Palettes.
 *
 * Each preset provides:
 * - A unique identifier and user-facing name.
 * - Exactly 4 quadrant colors for circular M3 swatches:
 *   [0] Top-Left: Primary / Main Accent
 *   [1] Top-Right: Secondary / Complementary Mist
 *   [2] Bottom-Left: Tertiary / Harmonious Accent
 *   [3] Bottom-Right: Primary Container / Deep Anchor Tone
 * - Full 5-tier elevation Light & Dark ColorSchemes compliant with M3 Expressive.
 * - Tonally tinted surfaces matching the theme hue (NO cold generic gray canvases).
 */
data class PresetThemePalette(
    val id: String,
    val name: String,
    val quadrantColors: List<Color>,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
) {
    init {
        require(quadrantColors.size == 4) { "Palette $id must have exactly 4 quadrant colors" }
    }

    fun getScheme(darkTheme: Boolean, useAmoledBlack: Boolean): ColorScheme {
        return if (darkTheme) {
            if (useAmoledBlack) {
                darkColorScheme.copy(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceDim = Color.Black,
                    surfaceBright = Color(0xFF2C2C2C),
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color.Black,
                    // Keep the palette's tinted resting and elevated card surfaces so cards stand out cleanly over pure OLED black
                    surfaceContainer = darkColorScheme.surfaceContainer,
                    surfaceContainerHigh = darkColorScheme.surfaceContainerHigh,
                    surfaceContainerHighest = darkColorScheme.surfaceContainerHighest,
                    surfaceVariant = darkColorScheme.surfaceVariant,
                )
            } else {
                darkColorScheme
            }
        } else {
            lightColorScheme
        }
    }
}

/**
 * Helper to build a complete Material 3 Light ColorScheme with tonally tinted 5-tier surface elevation.
 */
private fun buildLightScheme(
    primary: Color,
    onPrimary: Color = Color.White,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color = Color.White,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color = Color.White,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    surfaceLowest: Color = Color.White,
    surfaceLow: Color,
    surfaceContainer: Color,
    surfaceHigh: Color,
    surfaceHighest: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color = Color(0xFF44474F),
    onSurface: Color = Color(0xFF1A1C1E),
    outline: Color = Color(0xFF757780),
    outlineVariant: Color = Color(0xFFC5C6D0),
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = primaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = surfaceLow,
    onBackground = onSurface,
    surface = surfaceLow,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = Color.Black,
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    surfaceDim = surfaceHigh,
    surfaceBright = Color.White,
    surfaceContainerLowest = surfaceLowest,
    surfaceContainerLow = surfaceLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceHigh,
    surfaceContainerHighest = surfaceHighest,
)

/**
 * Helper to build a complete Material 3 Dark ColorScheme with tonally tinted 5-tier surface elevation.
 */
private fun buildDarkScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    surfaceLowest: Color,
    surfaceLow: Color,
    surfaceContainer: Color,
    surfaceHigh: Color,
    surfaceHighest: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color = Color(0xFFC4C6D0),
    onSurface: Color = Color(0xFFE1E2E8),
    outline: Color = Color(0xFF8E9099),
    outlineVariant: Color = Color(0xFF44474F),
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = surfaceLow,
    onBackground = onSurface,
    surface = surfaceLow,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    outline = outline,
    outlineVariant = outlineVariant,
    scrim = Color.Black,
    inverseSurface = Color(0xFFE1E2E8),
    inverseOnSurface = Color(0xFF2E3135),
    surfaceDim = surfaceLowest,
    surfaceBright = surfaceHighest,
    surfaceContainerLowest = surfaceLowest,
    surfaceContainerLow = surfaceLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceHigh,
    surfaceContainerHighest = surfaceHighest,
)

object ThemePalettes {

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. Ocean Periwinkle (Blue / Slate / Lilac / Deep Navy)
    // ─────────────────────────────────────────────────────────────────────────────
    val OceanPeriwinkle = PresetThemePalette(
        id = "ocean_periwinkle",
        name = "Ocean Periwinkle",
        quadrantColors = listOf(
            Color(0xFFA4C8FF), // Top-Left: Soft Periwinkle Blue
            Color(0xFFBCC7DD), // Top-Right: Slate Blue Mist
            Color(0xFFE5BEED), // Bottom-Left: Lilac Rose
            Color(0xFF25477B), // Bottom-Right: Deep Navy
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF005EB4),
            primaryContainer = Color(0xFFD6E3FF),
            onPrimaryContainer = Color(0xFF001B3E),
            secondary = Color(0xFF525F70),
            secondaryContainer = Color(0xFFD6E4F7),
            onSecondaryContainer = Color(0xFF101C2B),
            tertiary = Color(0xFF6E5676),
            tertiaryContainer = Color(0xFFF7D8FE),
            onTertiaryContainer = Color(0xFF27142F),
            surfaceLow = Color(0xFFEFF3FD),
            surfaceContainer = Color(0xFFE3EAF7),
            surfaceHigh = Color(0xFFD7E2F1),
            surfaceHighest = Color(0xFFCBD8EC),
            surfaceVariant = Color(0xFFD8E2F0),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFA4C8FF),
            onPrimary = Color(0xFF003062),
            primaryContainer = Color(0xFF004787),
            onPrimaryContainer = Color(0xFFD6E3FF),
            secondary = Color(0xFFBCC7DD),
            onSecondary = Color(0xFF253141),
            secondaryContainer = Color(0xFF3B4858),
            onSecondaryContainer = Color(0xFFD6E4F7),
            tertiary = Color(0xFFDCBEDD),
            onTertiary = Color(0xFF3D2845),
            tertiaryContainer = Color(0xFF553E5D),
            onTertiaryContainer = Color(0xFFF7D8FE),
            surfaceLowest = Color(0xFF0F141C),
            surfaceLow = Color(0xFF161C26),
            surfaceContainer = Color(0xFF1D2431),
            surfaceHigh = Color(0xFF27303E),
            surfaceHighest = Color(0xFF323B4C),
            surfaceVariant = Color(0xFF3C4554),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. Desert Peach (Warm Coral / Blush / Sand / Terracotta)
    // ─────────────────────────────────────────────────────────────────────────────
    val DesertPeach = PresetThemePalette(
        id = "desert_peach",
        name = "Desert Peach",
        quadrantColors = listOf(
            Color(0xFFFFB4A8), // Top-Left: Warm Coral Peach
            Color(0xFFE7BDB6), // Top-Right: Blush Muted Peach
            Color(0xFFDFC38C), // Bottom-Left: Golden Sand
            Color(0xFF642823), // Bottom-Right: Deep Terracotta
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF904A41),
            primaryContainer = Color(0xFFFFDAD4),
            onPrimaryContainer = Color(0xFF3B0906),
            secondary = Color(0xFF775651),
            secondaryContainer = Color(0xFFFFDAD4),
            onSecondaryContainer = Color(0xFF2C1512),
            tertiary = Color(0xFF705C2E),
            tertiaryContainer = Color(0xFFFCE0A6),
            onTertiaryContainer = Color(0xFF261A00),
            surfaceLow = Color(0xFFFFF0ED),
            surfaceContainer = Color(0xFFFDE7E2),
            surfaceHigh = Color(0xFFFADED7),
            surfaceHighest = Color(0xFFF6D4CD),
            surfaceVariant = Color(0xFFF5DDD8),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFFFB4A8),
            onPrimary = Color(0xFF561E18),
            primaryContainer = Color(0xFF73342C),
            onPrimaryContainer = Color(0xFFFFDAD4),
            secondary = Color(0xFFE7BDB6),
            onSecondary = Color(0xFF442926),
            secondaryContainer = Color(0xFF5D3F3B),
            onSecondaryContainer = Color(0xFFFFDAD4),
            tertiary = Color(0xFFDFC38C),
            onTertiary = Color(0xFF3E2E05),
            tertiaryContainer = Color(0xFF564419),
            onTertiaryContainer = Color(0xFFFCE0A6),
            surfaceLowest = Color(0xFF181110),
            surfaceLow = Color(0xFF221715),
            surfaceContainer = Color(0xFF2B1D1A),
            surfaceHigh = Color(0xFF362724),
            surfaceHighest = Color(0xFF42332F),
            surfaceVariant = Color(0xFF534340),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. Sage Forest (Mint / Sage Mist / Sky Aqua / Evergreen)
    // ─────────────────────────────────────────────────────────────────────────────
    val SageForest = PresetThemePalette(
        id = "sage_forest",
        name = "Sage Forest",
        quadrantColors = listOf(
            Color(0xFFA2D59E), // Top-Left: Fresh Sage Green
            Color(0xFFC1C9BF), // Top-Right: Muted Sage
            Color(0xFFA1D3DF), // Bottom-Left: Sky Aqua
            Color(0xFF245136), // Bottom-Right: Deep Evergreen
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF3B693E),
            primaryContainer = Color(0xFFBDF0BC),
            onPrimaryContainer = Color(0xFF002206),
            secondary = Color(0xFF536353),
            secondaryContainer = Color(0xFFD6E8D4),
            onSecondaryContainer = Color(0xFF111F13),
            tertiary = Color(0xFF38656E),
            tertiaryContainer = Color(0xFFBCEBF5),
            onTertiaryContainer = Color(0xFF001F25),
            surfaceLow = Color(0xFFF0F6EE),
            surfaceContainer = Color(0xFFE5EEE3),
            surfaceHigh = Color(0xFFDAE6D7),
            surfaceHighest = Color(0xFFD0DDCC),
            surfaceVariant = Color(0xFFDCE6D9),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFA2D59E),
            onPrimary = Color(0xFF073914),
            primaryContainer = Color(0xFF235028),
            onPrimaryContainer = Color(0xFFBDF0BC),
            secondary = Color(0xFFBBCBB9),
            onSecondary = Color(0xFF253427),
            secondaryContainer = Color(0xFF3B4B3D),
            onSecondaryContainer = Color(0xFFD6E8D4),
            tertiary = Color(0xFFA1D3DF),
            onTertiary = Color(0xFF00363E),
            tertiaryContainer = Color(0xFF1E4D55),
            onTertiaryContainer = Color(0xFFBCEBF5),
            surfaceLowest = Color(0xFF101611),
            surfaceLow = Color(0xFF172018),
            surfaceContainer = Color(0xFF1E281F),
            surfaceHigh = Color(0xFF283429),
            surfaceHighest = Color(0xFF344035),
            surfaceVariant = Color(0xFF414B42),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. Lavender Plum (Orchid / Lilac / Rose Blush / Regal Plum)
    // ─────────────────────────────────────────────────────────────────────────────
    val LavenderPlant = PresetThemePalette(
        id = "lavender_plum",
        name = "Lavender Plum",
        quadrantColors = listOf(
            Color(0xFFE2B6FF), // Top-Left: Soft Orchid Purple
            Color(0xFFD5C0E2), // Top-Right: Pale Lilac
            Color(0xFFF8B2B4), // Bottom-Left: Rose Blush
            Color(0xFF4B265C), // Bottom-Right: Regal Plum
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF7A4B96),
            primaryContainer = Color(0xFFF6D8FF),
            onPrimaryContainer = Color(0xFF30004B),
            secondary = Color(0xFF67586F),
            secondaryContainer = Color(0xFFEFDCF7),
            onSecondaryContainer = Color(0xFF22162A),
            tertiary = Color(0xFF7F4D56),
            tertiaryContainer = Color(0xFFFFD9DE),
            onTertiaryContainer = Color(0xFF320B15),
            surfaceLow = Color(0xFFF9F0FC),
            surfaceContainer = Color(0xFFF2E4F7),
            surfaceHigh = Color(0xFFEBD8F1),
            surfaceHighest = Color(0xFFE2CCEB),
            surfaceVariant = Color(0xFFE7DAED),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFE2B6FF),
            onPrimary = Color(0xFF481A64),
            primaryContainer = Color(0xFF61327C),
            onPrimaryContainer = Color(0xFFF6D8FF),
            secondary = Color(0xFFD2C1D9),
            onSecondary = Color(0xFF382B40),
            secondaryContainer = Color(0xFF4F4157),
            onSecondaryContainer = Color(0xFFEFDCF7),
            tertiary = Color(0xFFF8B2B4),
            onTertiary = Color(0xFF4B2029),
            tertiaryContainer = Color(0xFF64363F),
            onTertiaryContainer = Color(0xFFFFD9DE),
            surfaceLowest = Color(0xFF18111B),
            surfaceLow = Color(0xFF211826),
            surfaceContainer = Color(0xFF29202E),
            surfaceHigh = Color(0xFF342B3A),
            surfaceHighest = Color(0xFF413747),
            surfaceVariant = Color(0xFF4D4353),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. Warm Amber (Honey / Almond / Tan Ochre / Saddle Umber)
    // ─────────────────────────────────────────────────────────────────────────────
    val WarmAmber = PresetThemePalette(
        id = "warm_amber",
        name = "Warm Amber",
        quadrantColors = listOf(
            Color(0xFFFFB87A), // Top-Left: Warm Honey Amber
            Color(0xFFE6C3B1), // Top-Right: Warm Almond
            Color(0xFFCEC389), // Bottom-Left: Tan Ochre
            Color(0xFF653617), // Bottom-Right: Deep Saddle Umber
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF8B501A),
            primaryContainer = Color(0xFFFFDCBE),
            onPrimaryContainer = Color(0xFF2E1500),
            secondary = Color(0xFF74594B),
            secondaryContainer = Color(0xFFFFDCC9),
            onSecondaryContainer = Color(0xFF2B170D),
            tertiary = Color(0xFF646032),
            tertiaryContainer = Color(0xFFEBE4AB),
            onTertiaryContainer = Color(0xFF1E1C00),
            surfaceLow = Color(0xFFFFF2E7),
            surfaceContainer = Color(0xFFFDE7D8),
            surfaceHigh = Color(0xFFF8DEC9),
            surfaceHighest = Color(0xFFF3D3B9),
            surfaceVariant = Color(0xFFF0DFD2),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFFFB87A),
            onPrimary = Color(0xFF4D2600),
            primaryContainer = Color(0xFF6C3A02),
            onPrimaryContainer = Color(0xFFFFDCBE),
            secondary = Color(0xFFE4C1AF),
            onSecondary = Color(0xFF422B1F),
            secondaryContainer = Color(0xFF5A4134),
            onSecondaryContainer = Color(0xFFFFDCC9),
            tertiary = Color(0xFFCEC895),
            onTertiary = Color(0xFF353108),
            tertiaryContainer = Color(0xFF4C481D),
            onTertiaryContainer = Color(0xFFEBE4AB),
            surfaceLowest = Color(0xFF1A120A),
            surfaceLow = Color(0xFF241A11),
            surfaceContainer = Color(0xFF2C2218),
            surfaceHigh = Color(0xFF372D22),
            surfaceHighest = Color(0xFF44382D),
            surfaceVariant = Color(0xFF514539),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. Aqua Marine (Seafoam / Aqua Mist / Ice Blue / Deep Ocean)
    // ─────────────────────────────────────────────────────────────────────────────
    val AquaMarine = PresetThemePalette(
        id = "aqua_marine",
        name = "Aqua Marine",
        quadrantColors = listOf(
            Color(0xFF76DCBF), // Top-Left: Bright Seafoam Turquoise
            Color(0xFFBCD4CB), // Top-Right: Aqua Mist
            Color(0xFFAECFE7), // Bottom-Left: Ice Blue
            Color(0xFF004F4D), // Bottom-Right: Deep Teal Ocean
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF006B60),
            primaryContainer = Color(0xFF98F8DC),
            onPrimaryContainer = Color(0xFF00201C),
            secondary = Color(0xFF4A635D),
            secondaryContainer = Color(0xFFCCE8DF),
            onSecondaryContainer = Color(0xFF06201B),
            tertiary = Color(0xFF436278),
            tertiaryContainer = Color(0xFFC7E7FF),
            onTertiaryContainer = Color(0xFF001E2E),
            surfaceLow = Color(0xFFEEF8F5),
            surfaceContainer = Color(0xFFE1F1EC),
            surfaceHigh = Color(0xFFD4EAE3),
            surfaceHighest = Color(0xFFC6E2D9),
            surfaceVariant = Color(0xFFD4E5E0),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFF76DCBF),
            onPrimary = Color(0xFF003831),
            primaryContainer = Color(0xFF005047),
            onPrimaryContainer = Color(0xFF98F8DC),
            secondary = Color(0xFFB0CCC4),
            onSecondary = Color(0xFF1C3530),
            secondaryContainer = Color(0xFF324B46),
            onSecondaryContainer = Color(0xFFCCE8DF),
            tertiary = Color(0xFFA9CBE4),
            onTertiary = Color(0xFF113448),
            tertiaryContainer = Color(0xFF2B4A5F),
            onTertiaryContainer = Color(0xFFC7E7FF),
            surfaceLowest = Color(0xFF0E1715),
            surfaceLow = Color(0xFF14211E),
            surfaceContainer = Color(0xFF1B2926),
            surfaceHigh = Color(0xFF243431),
            surfaceHighest = Color(0xFF2E413C),
            surfaceVariant = Color(0xFF3F4A47),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. Dusty Rose (Dusty Pink / Mauve Ash / Thistle / Deep Wine)
    // ─────────────────────────────────────────────────────────────────────────────
    val DustyRose = PresetThemePalette(
        id = "dusty_rose",
        name = "Dusty Rose",
        quadrantColors = listOf(
            Color(0xFFF5B4BE), // Top-Left: Dusty Rose Pink
            Color(0xFFDEC1C5), // Top-Right: Mauve Ash
            Color(0xFFE2BDE8), // Bottom-Left: Pale Thistle
            Color(0xFF5E2335), // Bottom-Right: Deep Cabernet Wine
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF8B4A58),
            primaryContainer = Color(0xFFFFD9DF),
            onPrimaryContainer = Color(0xFF390717),
            secondary = Color(0xFF74575C),
            secondaryContainer = Color(0xFFFFD9DE),
            onSecondaryContainer = Color(0xFF2B151A),
            tertiary = Color(0xFF775573),
            tertiaryContainer = Color(0xFFFFD7F6),
            onTertiaryContainer = Color(0xFF2D122D),
            surfaceLow = Color(0xFFFDF0F2),
            surfaceContainer = Color(0xFFF9E4E7),
            surfaceHigh = Color(0xFFF4D8DC),
            surfaceHighest = Color(0xFFEECCD0),
            surfaceVariant = Color(0xFFEED7DA),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFF5B4BE),
            onPrimary = Color(0xFF541D2B),
            primaryContainer = Color(0xFF6F3341),
            onPrimaryContainer = Color(0xFFFFD9DF),
            secondary = Color(0xFFDEC0C5),
            onSecondary = Color(0xFF42292E),
            secondaryContainer = Color(0xFF5A4044),
            onSecondaryContainer = Color(0xFFFFD9DE),
            tertiary = Color(0xFFE5BCDF),
            onTertiary = Color(0xFF452743),
            tertiaryContainer = Color(0xFF5D3D5A),
            onTertiaryContainer = Color(0xFFFFD7F6),
            surfaceLowest = Color(0xFF191113),
            surfaceLow = Color(0xFF23171A),
            surfaceContainer = Color(0xFF2B1D21),
            surfaceHigh = Color(0xFF36272B),
            surfaceHighest = Color(0xFF433337),
            surfaceVariant = Color(0xFF524346),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 8. Twilight Iris (Periwinkle / Periwinkle Mist / Orchid / Midnight Indigo)
    // ─────────────────────────────────────────────────────────────────────────────
    val TwilightIris = PresetThemePalette(
        id = "twilight_iris",
        name = "Twilight Iris",
        quadrantColors = listOf(
            Color(0xFFB4C5FF), // Top-Left: Twilight Periwinkle
            Color(0xFFC4CBE7), // Top-Right: Soft Periwinkle Mist
            Color(0xFFE9BCE4), // Bottom-Left: Gentle Orchid
            Color(0xFF323E70), // Bottom-Right: Midnight Indigo
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF3E5B99),
            primaryContainer = Color(0xFFD9E2FF),
            onPrimaryContainer = Color(0xFF001944),
            secondary = Color(0xFF585F71),
            secondaryContainer = Color(0xFFDCE2F9),
            onSecondaryContainer = Color(0xFF151C2C),
            tertiary = Color(0xFF705574),
            tertiaryContainer = Color(0xFFF9D8FC),
            onTertiaryContainer = Color(0xFF29132E),
            surfaceLow = Color(0xFFF2F4FD),
            surfaceContainer = Color(0xFFE6EAF9),
            surfaceHigh = Color(0xFFDAE0F4),
            surfaceHighest = Color(0xFFCED5EE),
            surfaceVariant = Color(0xFFDFE3EF),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFB4C5FF),
            onPrimary = Color(0xFF042D68),
            primaryContainer = Color(0xFF24437F),
            onPrimaryContainer = Color(0xFFD9E2FF),
            secondary = Color(0xFFC1C7DD),
            onSecondary = Color(0xFF2A3142),
            secondaryContainer = Color(0xFF414759),
            onSecondaryContainer = Color(0xFFDCE2F9),
            tertiary = Color(0xFFDEBCDF),
            onTertiary = Color(0xFF3F2844),
            tertiaryContainer = Color(0xFF573E5B),
            onTertiaryContainer = Color(0xFFF9D8FC),
            surfaceLowest = Color(0xFF10141F),
            surfaceLow = Color(0xFF171B28),
            surfaceContainer = Color(0xFF1E2331),
            surfaceHigh = Color(0xFF292E3D),
            surfaceHighest = Color(0xFF353A4A),
            surfaceVariant = Color(0xFF434854),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 9. Sunlit Ochre (Marigold / Sand Mist / Olive Mint / Roasted Mocha)
    // ─────────────────────────────────────────────────────────────────────────────
    val SunlitOchre = PresetThemePalette(
        id = "sunlit_ochre",
        name = "Sunlit Ochre",
        quadrantColors = listOf(
            Color(0xFFFFB960), // Top-Left: Sunlit Marigold
            Color(0xFFE0C5AC), // Top-Right: Warm Sand Mist
            Color(0xFFC1CEB1), // Bottom-Left: Olive Mint
            Color(0xFF5F3A19), // Bottom-Right: Roasted Mocha
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF855318),
            primaryContainer = Color(0xFFFFDDB8),
            onPrimaryContainer = Color(0xFF2B1700),
            secondary = Color(0xFF715B45),
            secondaryContainer = Color(0xFFFDDFCD),
            onSecondaryContainer = Color(0xFF271808),
            tertiary = Color(0xFF596348),
            tertiaryContainer = Color(0xFFDCE8C6),
            onTertiaryContainer = Color(0xFF161E0A),
            surfaceLow = Color(0xFFFFF3E6),
            surfaceContainer = Color(0xFFFDE8D6),
            surfaceHigh = Color(0xFFF9DDC4),
            surfaceHighest = Color(0xFFF4D1B2),
            surfaceVariant = Color(0xFFEFE0D1),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFFFB960),
            onPrimary = Color(0xFF482900),
            primaryContainer = Color(0xFF673E02),
            onPrimaryContainer = Color(0xFFFFDDB8),
            secondary = Color(0xFFDFC3AC),
            onSecondary = Color(0xFF3F2D1B),
            secondaryContainer = Color(0xFF574330),
            onSecondaryContainer = Color(0xFFFDDFCD),
            tertiary = Color(0xFFC1CBAE),
            onTertiary = Color(0xFF2B341D),
            tertiaryContainer = Color(0xFF414B32),
            onTertiaryContainer = Color(0xFFDCE8C6),
            surfaceLowest = Color(0xFF1A120A),
            surfaceLow = Color(0xFF241C11),
            surfaceContainer = Color(0xFF2C2317),
            surfaceHigh = Color(0xFF382E22),
            surfaceHighest = Color(0xFF453A2E),
            surfaceVariant = Color(0xFF50453A),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 10. Electric Cyan (Cyan / Glacier Mist / Bluebell / Deep Petrol)
    // ─────────────────────────────────────────────────────────────────────────────
    val ElectricCyan = PresetThemePalette(
        id = "electric_cyan",
        name = "Electric Cyan",
        quadrantColors = listOf(
            Color(0xFF6FD6FF), // Top-Left: Vibrant Sky Cyan
            Color(0xFFB6C8D3), // Top-Right: Glacier Mist
            Color(0xFFBDC2EF), // Bottom-Left: Soft Bluebell
            Color(0xFF004A5C), // Bottom-Right: Deep Petrol Blue
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF006782),
            primaryContainer = Color(0xFFBBE9FF),
            onPrimaryContainer = Color(0xFF001F29),
            secondary = Color(0xFF4C626B),
            secondaryContainer = Color(0xFFCFE6F1),
            onSecondaryContainer = Color(0xFF071E26),
            tertiary = Color(0xFF5B5B7E),
            tertiaryContainer = Color(0xFFE2E0FF),
            onTertiaryContainer = Color(0xFF171737),
            surfaceLow = Color(0xFFEBF7FD),
            surfaceContainer = Color(0xFFDCEFFB),
            surfaceHigh = Color(0xFFCDE6F7),
            surfaceHighest = Color(0xFFBCDCF2),
            surfaceVariant = Color(0xFFD2E3EB),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFF6FD6FF),
            onPrimary = Color(0xFF003544),
            primaryContainer = Color(0xFF004D63),
            onPrimaryContainer = Color(0xFFBBE9FF),
            secondary = Color(0xFFB3CAD4),
            onSecondary = Color(0xFF1E333C),
            secondaryContainer = Color(0xFF354A53),
            onSecondaryContainer = Color(0xFFCFE6F1),
            tertiary = Color(0xFFC3C3EC),
            onTertiary = Color(0xFF2C2D4E),
            tertiaryContainer = Color(0xFF434365),
            onTertiaryContainer = Color(0xFFE2E0FF),
            surfaceLowest = Color(0xFF0C161C),
            surfaceLow = Color(0xFF121F27),
            surfaceContainer = Color(0xFF192831),
            surfaceHigh = Color(0xFF22343E),
            surfaceHighest = Color(0xFF2D414D),
            surfaceVariant = Color(0xFF3E484E),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 11. Flamingo Terracotta (Coral / Blush Tan / Khaki Sand / Sienna Rust)
    // ─────────────────────────────────────────────────────────────────────────────
    val FlamingoTerracotta = PresetThemePalette(
        id = "flamingo_terracotta",
        name = "Flamingo Terracotta",
        quadrantColors = listOf(
            Color(0xFFFFAF99), // Top-Left: Flamingo Coral
            Color(0xFFE5C2B8), // Top-Right: Blush Tan
            Color(0xFFD4C490), // Bottom-Left: Khaki Sand
            Color(0xFF6F2B1B), // Bottom-Right: Sienna Rust
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF964534),
            primaryContainer = Color(0xFFFFDAD3),
            onPrimaryContainer = Color(0xFF3C0802),
            secondary = Color(0xFF77574E),
            secondaryContainer = Color(0xFFFFDAD2),
            onSecondaryContainer = Color(0xFF2C1510),
            tertiary = Color(0xFF6C5D2F),
            tertiaryContainer = Color(0xFFF7E1A6),
            onTertiaryContainer = Color(0xFF231B00),
            surfaceLow = Color(0xFFFFF0EC),
            surfaceContainer = Color(0xFFFEE6E0),
            surfaceHigh = Color(0xFFFBDCD3),
            surfaceHighest = Color(0xFFF6CEC4),
            surfaceVariant = Color(0xFFF4DDD7),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFFFAF99),
            onPrimary = Color(0xFF5C190B),
            primaryContainer = Color(0xFF792F1F),
            onPrimaryContainer = Color(0xFFFFDAD3),
            secondary = Color(0xFFE7BDB3),
            onSecondary = Color(0xFF442A23),
            secondaryContainer = Color(0xFF5D4038),
            onSecondaryContainer = Color(0xFFFFDAD2),
            tertiary = Color(0xFFD9C58F),
            onTertiary = Color(0xFF3B2F05),
            tertiaryContainer = Color(0xFF53451A),
            onTertiaryContainer = Color(0xFFF7E1A6),
            surfaceLowest = Color(0xFF19100D),
            surfaceLow = Color(0xFF241814),
            surfaceContainer = Color(0xFF2D1F1A),
            surfaceHigh = Color(0xFF382A24),
            surfaceHighest = Color(0xFF45352F),
            surfaceVariant = Color(0xFF524340),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 12. Citrus Olive (Chartreuse / Celadon / Spring Green / Deep Olive)
    // ─────────────────────────────────────────────────────────────────────────────
    val CitrusOlive = PresetThemePalette(
        id = "citrus_olive",
        name = "Citrus Olive",
        quadrantColors = listOf(
            Color(0xFFD2E26C), // Top-Left: Chartreuse Lime
            Color(0xFFCED4C0), // Top-Right: Celadon Mist
            Color(0xFFA8D4B7), // Bottom-Left: Spring Green
            Color(0xFF414C14), // Bottom-Right: Deep Forest Olive
        ),
        lightColorScheme = buildLightScheme(
            primary = Color(0xFF5B6415),
            primaryContainer = Color(0xFFE0EC85),
            onPrimaryContainer = Color(0xFF1A1F00),
            secondary = Color(0xFF5E6152),
            secondaryContainer = Color(0xFFE3E5D4),
            onSecondaryContainer = Color(0xFF1B1E13),
            tertiary = Color(0xFF3F6650),
            tertiaryContainer = Color(0xFFC1ECD0),
            onTertiaryContainer = Color(0xFF002111),
            surfaceLow = Color(0xFFF5F7E8),
            surfaceContainer = Color(0xFFEBF0DB),
            surfaceHigh = Color(0xFFDFE7CB),
            surfaceHighest = Color(0xFFD4DEBD),
            surfaceVariant = Color(0xFFDFE4D2),
        ),
        darkColorScheme = buildDarkScheme(
            primary = Color(0xFFD2E26C),
            onPrimary = Color(0xFF2E3400),
            primaryContainer = Color(0xFF444B00),
            onPrimaryContainer = Color(0xFFE0EC85),
            secondary = Color(0xFFC6C8B7),
            onSecondary = Color(0xFF303326),
            secondaryContainer = Color(0xFF46493B),
            onSecondaryContainer = Color(0xFFE3E5D4),
            tertiary = Color(0xFFA6D0B4),
            onTertiary = Color(0xFF0D3724),
            tertiaryContainer = Color(0xFF274E39),
            onTertiaryContainer = Color(0xFFC1ECD0),
            surfaceLowest = Color(0xFF14170C),
            surfaceLow = Color(0xFF1C2112),
            surfaceContainer = Color(0xFF242A19),
            surfaceHigh = Color(0xFF2F3622),
            surfaceHighest = Color(0xFF3A422D),
            surfaceVariant = Color(0xFF45493B),
        ),
    )

    // ─────────────────────────────────────────────────────────────────────────────
    // 13. Default Photon Blue
    // ─────────────────────────────────────────────────────────────────────────────
    val DefaultPhotonBlue = PresetThemePalette(
        id = "default_blue",
        name = "Photon Blue",
        quadrantColors = listOf(
            Color(0xFFA5C8FF), // Top-Left: Light Blue
            Color(0xFFC1CBDD), // Top-Right: Slate Gray Blue
            Color(0xFF94CCFF), // Bottom-Left: Ice Cyan
            Color(0xFF004786), // Bottom-Right: Dark Blue
        ),
        lightColorScheme = DefaultLightColorScheme,
        darkColorScheme = DefaultDarkColorScheme,
    )

    val allPalettes: List<PresetThemePalette> = listOf(
        DefaultPhotonBlue,
        OceanPeriwinkle,
        DesertPeach,
        SageForest,
        LavenderPlant,
        WarmAmber,
        AquaMarine,
        DustyRose,
        TwilightIris,
        SunlitOchre,
        ElectricCyan,
        FlamingoTerracotta,
        CitrusOlive,
    )

    private val paletteMap: Map<String, PresetThemePalette> = allPalettes.associateBy { it.id }

    fun getById(id: String): PresetThemePalette {
        return paletteMap[id] ?: DefaultPhotonBlue
    }
}
