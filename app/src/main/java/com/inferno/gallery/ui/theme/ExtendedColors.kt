package com.inferno.gallery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that fall outside the standard M3 [androidx.compose.material3.ColorScheme]
 * but still respond to light / dark / AMOLED theming.
 */
@Immutable
data class PhotonColors(
    /** Positive / success affordance. */
    val success: Color,
    /** Content color drawn on top of [success]. */
    val onSuccess: Color,
    /** Low-emphasis success container tint (e.g. success banners). */
    val successContainer: Color,
    /** Star / favorite accent. */
    val star: Color,
)

// Tuned per scheme so contrast holds up on light, dark, and pure-black AMOLED surfaces.
internal val LightPhotonColors = PhotonColors(
    success = Color(0xFF10B981),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFD1FAE5),
    star = Color(0xFFE0A400),
)

internal val DarkPhotonColors = PhotonColors(
    success = Color(0xFF34D399),
    onSuccess = Color(0xFF00382A),
    successContainer = Color(0xFF0B3D2E),
    star = Color(0xFFFFD700),
)

// Slightly brighter accents so they don't sink into a pure-black background.
internal val AmoledPhotonColors = PhotonColors(
    success = Color(0xFF3DDC97),
    onSuccess = Color(0xFF00291E),
    successContainer = Color(0xFF08251B),
    star = Color(0xFFFFD54A),
)

val LocalPhotonColors = staticCompositionLocalOf { LightPhotonColors }

/** Convenience accessor: `MaterialTheme.photonColors.success`. */
val MaterialTheme.photonColors: PhotonColors
    @Composable
    @ReadOnlyComposable
    get() = LocalPhotonColors.current
