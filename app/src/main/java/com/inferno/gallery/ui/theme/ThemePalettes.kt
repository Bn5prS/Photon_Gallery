package com.inferno.gallery.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Material 3 Curated Preset Theme Palettes for Photon Gallery.
 *
 * Each preset provides:
 * - A unique identifier and user-facing name.
 * - Primary seed color generating an authentic 13-tone Material 3 Expressive palette via material-kolor.
 * - Exactly 4 quadrant colors for circular M3 swatches:
 *   [0] Top-Left: Primary / Main Accent
 *   [1] Top-Right: Secondary / Complementary Mist
 *   [2] Bottom-Left: Tertiary / Harmonious Accent
 *   [3] Bottom-Right: Primary Container / Deep Anchor Tone
 * - Full 5-tier surface container elevation hierarchy compliant with M3 Expressive.
 */
data class PresetThemePalette(
    val id: String,
    val name: String,
    val quadrantColors: List<Color>,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme,
    val seedColor: Color = quadrantColors[0],
) {
    init {
        require(quadrantColors.size == 4) { "Palette $id must have exactly 4 quadrant colors" }
    }

    fun getScheme(darkTheme: Boolean, useAmoledBlack: Boolean): ColorScheme {
        val baseScheme = if (darkTheme) darkColorScheme else lightColorScheme
        return if (darkTheme && useAmoledBlack) {
            baseScheme.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceDim = Color.Black,
                surfaceBright = baseScheme.surfaceBright,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = baseScheme.surfaceContainerLow,
                surfaceContainer = baseScheme.surfaceContainer,
                surfaceContainerHigh = baseScheme.surfaceContainerHigh,
                surfaceContainerHighest = baseScheme.surfaceContainerHighest,
                surfaceVariant = baseScheme.surfaceVariant,
            )
        } else {
            baseScheme.copy(
                background = baseScheme.surface,
            )
        }
    }
}

/**
 * Factory helper generating authentic 13-tone Material 3 Expressive palettes from a single seed color.
 */
private fun createPresetPalette(
    id: String,
    name: String,
    seedColor: Color,
    quadrantColors: List<Color>,
): PresetThemePalette {
    val light = dynamicColorScheme(
        seedColor = seedColor,
        isDark = false,
        style = PaletteStyle.TonalSpot,
    ).let { it.copy(background = it.surface) }

    val dark = dynamicColorScheme(
        seedColor = seedColor,
        isDark = true,
        style = PaletteStyle.TonalSpot,
    ).let { it.copy(background = it.surface) }

    return PresetThemePalette(
        id = id,
        name = name,
        quadrantColors = quadrantColors,
        lightColorScheme = light,
        darkColorScheme = dark,
        seedColor = seedColor,
    )
}

object ThemePalettes {

    // 1. Ocean Periwinkle (Blue / Slate / Lilac / Deep Navy)
    val OceanPeriwinkle = createPresetPalette(
        id = "ocean_periwinkle",
        name = "Ocean Periwinkle",
        seedColor = Color(0xFF005EB4),
        quadrantColors = listOf(
            Color(0xFFA4C8FF), // Top-Left: Soft Periwinkle Blue
            Color(0xFFBCC7DD), // Top-Right: Slate Blue Mist
            Color(0xFFE5BEED), // Bottom-Left: Lilac Rose
            Color(0xFF25477B), // Bottom-Right: Deep Navy
        ),
    )

    // 2. Desert Peach (Warm Coral / Blush / Sand / Terracotta)
    val DesertPeach = createPresetPalette(
        id = "desert_peach",
        name = "Desert Peach",
        seedColor = Color(0xFF904A41),
        quadrantColors = listOf(
            Color(0xFFFFB4A8), // Top-Left: Warm Coral Peach
            Color(0xFFE7BDB6), // Top-Right: Blush Muted Peach
            Color(0xFFDFC38C), // Bottom-Left: Golden Sand
            Color(0xFF642823), // Bottom-Right: Deep Terracotta
        ),
    )

    // 3. Sage Forest (Mint / Sage Mist / Sky Aqua / Evergreen)
    val SageForest = createPresetPalette(
        id = "sage_forest",
        name = "Sage Forest",
        seedColor = Color(0xFF3B693E),
        quadrantColors = listOf(
            Color(0xFFA2D59E), // Top-Left: Fresh Sage Green
            Color(0xFFC1C9BF), // Top-Right: Muted Sage
            Color(0xFFA1D3DF), // Bottom-Left: Sky Aqua
            Color(0xFF245136), // Bottom-Right: Deep Evergreen
        ),
    )

    // 4. Lavender Plum (Orchid / Lilac / Rose Blush / Regal Plum)
    val LavenderPlant = createPresetPalette(
        id = "lavender_plum",
        name = "Lavender Plum",
        seedColor = Color(0xFF7A4B96),
        quadrantColors = listOf(
            Color(0xFFE2B6FF), // Top-Left: Soft Orchid Purple
            Color(0xFFD5C0E2), // Top-Right: Pale Lilac
            Color(0xFFF8B2B4), // Bottom-Left: Rose Blush
            Color(0xFF4B265C), // Bottom-Right: Regal Plum
        ),
    )

    // 5. Warm Amber (Honey / Almond / Tan Ochre / Saddle Umber)
    val WarmAmber = createPresetPalette(
        id = "warm_amber",
        name = "Warm Amber",
        seedColor = Color(0xFF8B501A),
        quadrantColors = listOf(
            Color(0xFFFFB87A), // Top-Left: Warm Honey Amber
            Color(0xFFE6C3B1), // Top-Right: Warm Almond
            Color(0xFFCEC389), // Bottom-Left: Tan Ochre
            Color(0xFF653617), // Bottom-Right: Deep Saddle Umber
        ),
    )

    // 6. Aqua Marine (Seafoam / Aqua Mist / Ice Blue / Deep Ocean)
    val AquaMarine = createPresetPalette(
        id = "aqua_marine",
        name = "Aqua Marine",
        seedColor = Color(0xFF006B60),
        quadrantColors = listOf(
            Color(0xFF76DCBF), // Top-Left: Bright Seafoam Turquoise
            Color(0xFFBCD4CB), // Top-Right: Aqua Mist
            Color(0xFFAECFE7), // Bottom-Left: Ice Blue
            Color(0xFF004F4D), // Bottom-Right: Deep Teal Ocean
        ),
    )

    // 7. Dusty Rose (Dusty Pink / Mauve Ash / Thistle / Deep Wine)
    val DustyRose = createPresetPalette(
        id = "dusty_rose",
        name = "Dusty Rose",
        seedColor = Color(0xFF8B4A58),
        quadrantColors = listOf(
            Color(0xFFF5B4BE), // Top-Left: Dusty Rose Pink
            Color(0xFFDEC1C5), // Top-Right: Mauve Ash
            Color(0xFFE2BDE8), // Bottom-Left: Pale Thistle
            Color(0xFF5E2335), // Bottom-Right: Deep Cabernet Wine
        ),
    )

    // 8. Twilight Iris (Periwinkle / Periwinkle Mist / Orchid / Midnight Indigo)
    val TwilightIris = createPresetPalette(
        id = "twilight_iris",
        name = "Twilight Iris",
        seedColor = Color(0xFF3E5B99),
        quadrantColors = listOf(
            Color(0xFFB4C5FF), // Top-Left: Twilight Periwinkle
            Color(0xFFC4CBE7), // Top-Right: Soft Periwinkle Mist
            Color(0xFFE9BCE4), // Bottom-Left: Gentle Orchid
            Color(0xFF323E70), // Bottom-Right: Midnight Indigo
        ),
    )

    // 9. Sunlit Ochre (Marigold / Sand Mist / Olive Mint / Roasted Mocha)
    val SunlitOchre = createPresetPalette(
        id = "sunlit_ochre",
        name = "Sunlit Ochre",
        seedColor = Color(0xFF855318),
        quadrantColors = listOf(
            Color(0xFFFFB960), // Top-Left: Sunlit Marigold
            Color(0xFFE0C5AC), // Top-Right: Warm Sand Mist
            Color(0xFFC1CEB1), // Bottom-Left: Olive Mint
            Color(0xFF5F3A19), // Bottom-Right: Roasted Mocha
        ),
    )

    // 10. Electric Cyan (Cyan / Glacier Mist / Bluebell / Deep Petrol)
    val ElectricCyan = createPresetPalette(
        id = "electric_cyan",
        name = "Electric Cyan",
        seedColor = Color(0xFF006782),
        quadrantColors = listOf(
            Color(0xFF6FD6FF), // Top-Left: Vibrant Sky Cyan
            Color(0xFFB6C8D3), // Top-Right: Glacier Mist
            Color(0xFFBDC2EF), // Bottom-Left: Soft Bluebell
            Color(0xFF004A5C), // Bottom-Right: Deep Petrol Blue
        ),
    )

    // 11. Flamingo Terracotta (Coral / Blush Tan / Khaki Sand / Sienna Rust)
    val FlamingoTerracotta = createPresetPalette(
        id = "flamingo_terracotta",
        name = "Flamingo Terracotta",
        seedColor = Color(0xFF964534),
        quadrantColors = listOf(
            Color(0xFFFFAF99), // Top-Left: Flamingo Coral
            Color(0xFFE5C2B8), // Top-Right: Blush Tan
            Color(0xFFD4C490), // Bottom-Left: Khaki Sand
            Color(0xFF6F2B1B), // Bottom-Right: Sienna Rust
        ),
    )

    // 12. Citrus Olive (Chartreuse / Celadon / Spring Green / Deep Olive)
    val CitrusOlive = createPresetPalette(
        id = "citrus_olive",
        name = "Citrus Olive",
        seedColor = Color(0xFF5B6415),
        quadrantColors = listOf(
            Color(0xFFD2E26C), // Top-Left: Chartreuse Lime
            Color(0xFFCED4C0), // Top-Right: Celadon Mist
            Color(0xFFA8D4B7), // Bottom-Left: Spring Green
            Color(0xFF414C14), // Bottom-Right: Deep Forest Olive
        ),
    )

    // 13. Default Photon Blue
    val DefaultPhotonBlue = createPresetPalette(
        id = "default_blue",
        name = "Photon Blue",
        seedColor = Color(0xFF0A6EFF),
        quadrantColors = listOf(
            Color(0xFFA5C8FF), // Top-Left: Light Blue
            Color(0xFFC1CBDD), // Top-Right: Slate Gray Blue
            Color(0xFF94CCFF), // Bottom-Left: Ice Cyan
            Color(0xFF004786), // Bottom-Right: Dark Blue
        ),
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
