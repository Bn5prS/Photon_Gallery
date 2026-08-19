package com.inferno.gallery.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * A named, curated color preset.
 *
 * @param name         Human-readable label shown in the UI.
 * @param seedColor    Primary seed color. Null means "use wallpaper / Material You".
 * @param style        MaterialKolor PaletteStyle variant.
 * @param contrastPreset "Reduced" | "Default" | "Medium" | "High"
 */
data class PhotonColorPreset(
    val name: String,
    val seedColor: Color?,
    val style: PaletteStyle,
    val contrastPreset: String = "Default"
)

/** Sentinel value meaning "no override — auto-generate from seed". */
const val COLOR_OVERRIDE_AUTO = -1

/** All built-in named presets. Order matches the Settings UI carousel. */
val PhotonColorPresets: List<PhotonColorPreset> = listOf(
    PhotonColorPreset(
        name = "Material You",
        seedColor = null,
        style = PaletteStyle.TonalSpot,
        contrastPreset = "Default"
    ),
    PhotonColorPreset(
        name = "Lavender Dream",
        seedColor = Color(0xFF7C4DFF.toInt()),
        style = PaletteStyle.TonalSpot,
        contrastPreset = "Default"
    ),
    PhotonColorPreset(
        name = "Ocean Depth",
        seedColor = Color(0xFF0077B6.toInt()),
        style = PaletteStyle.Fidelity,
        contrastPreset = "Default"
    ),
    PhotonColorPreset(
        name = "Sunset Rose",
        seedColor = Color(0xFFFF6B6B.toInt()),
        style = PaletteStyle.Vibrant,
        contrastPreset = "Default"
    ),
    PhotonColorPreset(
        name = "Midnight Forest",
        seedColor = Color(0xFF1B4332.toInt()),
        style = PaletteStyle.Neutral,
        contrastPreset = "High"
    ),
    PhotonColorPreset(
        name = "Pastel Cloud",
        seedColor = Color(0xFFAEC6CF.toInt()),
        style = PaletteStyle.TonalSpot,
        contrastPreset = "Reduced"
    ),
    PhotonColorPreset(
        name = "Monochrome Pro",
        seedColor = Color(0xFF808080.toInt()),
        style = PaletteStyle.Monochrome,
        contrastPreset = "High"
    ),
    PhotonColorPreset(
        name = "Fruit Punch",
        seedColor = Color(0xFFFF5722.toInt()),
        style = PaletteStyle.FruitSalad,
        contrastPreset = "Default"
    ),
    PhotonColorPreset(
        name = "Expressive Pop",
        seedColor = Color(0xFFE91E63.toInt()),
        style = PaletteStyle.Expressive,
        contrastPreset = "Default"
    ),
    PhotonColorPreset(
        name = "Earth Tones",
        seedColor = Color(0xFF8B5E3C.toInt()),
        style = PaletteStyle.Content,
        contrastPreset = "Default"
    ),
)

/** Returns null if no preset matches (i.e. user is in "Custom" mode). */
fun presetByName(name: String): PhotonColorPreset? =
    PhotonColorPresets.find { it.name == name }

/** Maps a contrast preset name to the Double value expected by MaterialKolor. */
fun contrastPresetToDouble(preset: String): Double = when (preset) {
    "Reduced" -> -1.0
    "Medium"  -> 0.5
    "High"    -> 1.0
    else      -> 0.0
}
