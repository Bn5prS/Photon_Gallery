package com.inferno.gallery.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Highly distinct, non-overlapping curated color palettes across the chromatic spectrum,
 * matching Photon Drive's signature palette set.
 */
data class CuratedPalette(
    val name: String,
    val seedColor: Color
)

object CuratedColorPalettes {
    val items = listOf(
        CuratedPalette("Photon Blue", Color(0xFF0A6EFF)),
        CuratedPalette("Royal Velvet", Color(0xFF7C3AED)),
        CuratedPalette("Emerald Forest", Color(0xFF059669)),
        CuratedPalette("Sunset Coral", Color(0xFFFF5722)),
        CuratedPalette("Golden Honey", Color(0xFFF59E0B)),
        CuratedPalette("Ruby Crimson", Color(0xFFE11D48)),
        CuratedPalette("Pacific Aqua", Color(0xFF06B6D4)),
        CuratedPalette("Hot Fuchsia", Color(0xFFEC4899)),
        CuratedPalette("Nordic Mint", Color(0xFF10B981)),
        CuratedPalette("Espresso Mocha", Color(0xFF78350F)),
        CuratedPalette("Titanium Slate", Color(0xFF64748B)),
        CuratedPalette("Monochrome Pro", Color(0xFF757575))
    )

    fun findByArgb(argb: Int): CuratedPalette? {
        return items.firstOrNull { it.seedColor.toArgb() == argb }
    }
}
