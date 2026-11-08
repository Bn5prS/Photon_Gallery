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

    // ── Row 1 ──
    val DefaultPhotonBlue = createPresetPalette(
        id = "default_blue",
        name = "Photon Blue",
        seedColor = Color(0xFF0066FF),
        quadrantColors = listOf(
            Color(0xFF82B1FF), // Bright Royal Blue
            Color(0xFFADC6FF), // Ice Periwinkle
            Color(0xFF2979FF), // Vivid Cobalt
            Color(0xFF003D99), // Deep Royal Navy
        ),
    )

    val SkyBlue = createPresetPalette(
        id = "sky_blue",
        name = "Sky Blue",
        seedColor = Color(0xFF0288D1),
        quadrantColors = listOf(
            Color(0xFF81D4FA), // Clear Summer Sky
            Color(0xFFB3E5FC), // Pale Cloud Blue
            Color(0xFF00B0FF), // Vibrant Cerulean
            Color(0xFF01579B), // Deep Ocean Navy
        ),
    )

    val SunnyYellow = createPresetPalette(
        id = "sunny_yellow",
        name = "Sunny Yellow",
        seedColor = Color(0xFFFFB300),
        quadrantColors = listOf(
            Color(0xFFFFEE58), // Bright Sunny Lemon
            Color(0xFFFFF9C4), // Morning Sunlight
            Color(0xFFFFCA28), // Sunburst Gold
            Color(0xFFE65100), // Deep Honey Amber
        ),
    )

    val PurplePink = createPresetPalette(
        id = "purple_pink",
        name = "Purple Pink",
        seedColor = Color(0xFFE91E63),
        quadrantColors = listOf(
            Color(0xFFFF80AB), // Neon Pink
            Color(0xFFF8BBD0), // Pale Magenta Mist
            Color(0xFFEA80FC), // Electric Orchid
            Color(0xFF880E4F), // Deep Mulberry Wine
        ),
    )

    // ── Row 2 ──
    val RichGold = createPresetPalette(
        id = "rich_gold",
        name = "Rich Gold",
        seedColor = Color(0xFFD4AF37),
        quadrantColors = listOf(
            Color(0xFFFFE082), // Shimmering Gold
            Color(0xFFFFF8E1), // Pale Gold Mist
            Color(0xFFFFD54F), // Imperial Pure Gold
            Color(0xFF7F6000), // Deep Burnished Bronze
        ),
    )

    val EmeraldMint = createPresetPalette(
        id = "emerald_mint",
        name = "Emerald Mint",
        seedColor = Color(0xFF00C853),
        quadrantColors = listOf(
            Color(0xFF69F0AE), // Crisp Mint
            Color(0xFFB9F6CA), // Frost Mint
            Color(0xFF00E676), // Radiant Jade Green
            Color(0xFF00600F), // Deep Pine Forest
        ),
    )

    val CrimsonRuby = createPresetPalette(
        id = "crimson_ruby",
        name = "Crimson Ruby",
        seedColor = Color(0xFFD32F2F),
        quadrantColors = listOf(
            Color(0xFFFF5252), // Bright Ruby Coral
            Color(0xFFFFCDD2), // Pale Rose Mist
            Color(0xFFFF1744), // Vivid Scarlet
            Color(0xFF7F0000), // Deep Cabernet Wine
        ),
    )

    val SunsetOrange = createPresetPalette(
        id = "sunset_orange",
        name = "Sunset Orange",
        seedColor = Color(0xFFFF6D00),
        quadrantColors = listOf(
            Color(0xFFFFAB40), // Juicy Tangerine
            Color(0xFFFFE0B2), // Apricot Cream
            Color(0xFFFF6D00), // Vibrant Sunset
            Color(0xFFBF360C), // Deep Burnt Sienna
        ),
    )

    // ── Row 3 ──
    val RoyalAmethyst = createPresetPalette(
        id = "royal_amethyst",
        name = "Royal Amethyst",
        seedColor = Color(0xFF7C4DFF),
        quadrantColors = listOf(
            Color(0xFFB388FF), // Electric Lavender
            Color(0xFFEDE7F6), // Soft Lilac Mist
            Color(0xFF7C4DFF), // Regal Purple
            Color(0xFF311B92), // Midnight Velvet
        ),
    )

    val AquaTurquoise = createPresetPalette(
        id = "aqua_turquoise",
        name = "Aqua Turquoise",
        seedColor = Color(0xFF00B4D8),
        quadrantColors = listOf(
            Color(0xFF84FFFF), // Seafoam Aqua
            Color(0xFFE0F7FA), // Glacier Mist
            Color(0xFF00E5FF), // Tropical Turquoise
            Color(0xFF006064), // Deep Teal Lagoon
        ),
    )

    val ElectricLime = createPresetPalette(
        id = "electric_lime",
        name = "Electric Lime",
        seedColor = Color(0xFFAEEA00),
        quadrantColors = listOf(
            Color(0xFFEEFF41), // Luminous Citron
            Color(0xFFF4FF81), // Chartreuse Tint
            Color(0xFF76FF03), // Acid Lime
            Color(0xFF436000), // Deep Olive Moss
        ),
    )

    val BubblegumPink = createPresetPalette(
        id = "bubblegum_pink",
        name = "Bubblegum Pink",
        seedColor = Color(0xFFF06292),
        quadrantColors = listOf(
            Color(0xFFFF80AB), // Cotton Candy
            Color(0xFFFCE4EC), // Sakura Blossom
            Color(0xFFF06292), // Sweet Rose Pink
            Color(0xFF880E4F), // Deep Plum
        ),
    )

    // ── Row 4 ──
    val DesertPeach = createPresetPalette(
        id = "desert_peach",
        name = "Desert Peach",
        seedColor = Color(0xFFE07A5F),
        quadrantColors = listOf(
            Color(0xFFFFB4A8), // Warm Coral Peach
            Color(0xFFFBE9E7), // Blush Sand
            Color(0xFFE07A5F), // Terracotta Clay
            Color(0xFF642823), // Deep Rust
        ),
    )

    val SageForest = createPresetPalette(
        id = "sage_forest",
        name = "Sage Forest",
        seedColor = Color(0xFF3B693E),
        quadrantColors = listOf(
            Color(0xFFA2D59E), // Fresh Sage Green
            Color(0xFFE8F5E9), // Earthy Mist
            Color(0xFF4CAF50), // Spring Green
            Color(0xFF1B5E20), // Evergreen Pine
        ),
    )

    val MidnightIndigo = createPresetPalette(
        id = "midnight_indigo",
        name = "Midnight Indigo",
        seedColor = Color(0xFF3949AB),
        quadrantColors = listOf(
            Color(0xFF9FA8DA), // Twilight Blue
            Color(0xFFE8EAF6), // Star Ice
            Color(0xFF5C6BC0), // Cosmic Indigo
            Color(0xFF1A237E), // Deep Space Navy
        ),
    )

    val SilverSlate = createPresetPalette(
        id = "silver_slate",
        name = "Silver Slate",
        seedColor = Color(0xFF78909C),
        quadrantColors = listOf(
            Color(0xFFECEFF1), // Silver Frost
            Color(0xFFCFD8DC), // Cool Platinum
            Color(0xFF78909C), // Titanium Slate
            Color(0xFF263238), // Deep Carbon Noir
        ),
    )

    val allPalettes: List<PresetThemePalette> = listOf(
        DefaultPhotonBlue,
        SkyBlue,
        SunnyYellow,
        PurplePink,
        RichGold,
        EmeraldMint,
        CrimsonRuby,
        SunsetOrange,
        RoyalAmethyst,
        AquaTurquoise,
        ElectricLime,
        BubblegumPink,
        DesertPeach,
        SageForest,
        MidnightIndigo,
        SilverSlate,
    )

    private val paletteMap: Map<String, PresetThemePalette> = allPalettes.associateBy { it.id }

    fun getById(id: String): PresetThemePalette {
        return paletteMap[id] ?: when (id) {
            "ocean_periwinkle", "electric_cyan" -> SkyBlue
            "sunlit_ochre", "warm_amber" -> SunnyYellow
            "lavender_plum" -> PurplePink
            "flamingo_terracotta" -> DesertPeach
            "dusty_rose" -> BubblegumPink
            "twilight_iris" -> RoyalAmethyst
            "citrus_olive" -> ElectricLime
            "aqua_marine" -> AquaTurquoise
            else -> DefaultPhotonBlue
        }
    }
}
