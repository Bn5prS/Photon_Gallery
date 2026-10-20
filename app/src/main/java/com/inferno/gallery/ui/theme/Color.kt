package com.inferno.gallery.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Light scheme — full M3 token set (primary brand: Blue #0A6EFF)
// Matching Photon Drive official color tokens
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_light_primary                = Color(0xFF0A6EFF)
val md_theme_light_onPrimary              = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer       = Color(0xFFD4E3FF)
val md_theme_light_onPrimaryContainer     = Color(0xFF001C3A)

val md_theme_light_secondary              = Color(0xFF5A6376)
val md_theme_light_onSecondary            = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer     = Color(0xFFDEE7F9)
val md_theme_light_onSecondaryContainer   = Color(0xFF171E2B)

val md_theme_light_tertiary               = Color(0xFF00639B)
val md_theme_light_onTertiary             = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer      = Color(0xFFCDE5FF)
val md_theme_light_onTertiaryContainer    = Color(0xFF001E33)

val md_theme_light_error                  = Color(0xFFBA1A1A)
val md_theme_light_onError                = Color(0xFFFFFFFF)
val md_theme_light_errorContainer         = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer       = Color(0xFF410002)

val md_theme_light_background             = Color(0xFFF8F9FF)
val md_theme_light_onBackground           = Color(0xFF191C20)
val md_theme_light_surface                = Color(0xFFF8F9FF)
val md_theme_light_onSurface              = Color(0xFF191C20)
val md_theme_light_surfaceVariant         = Color(0xFFE1E2EC)
val md_theme_light_onSurfaceVariant       = Color(0xFF44474F)
val md_theme_light_outline                = Color(0xFF74777F)
val md_theme_light_outlineVariant         = Color(0xFFC4C6D0)
val md_theme_light_scrim                  = Color(0xFF000000)
val md_theme_light_inverseSurface         = Color(0xFF2E3135)
val md_theme_light_inverseOnSurface       = Color(0xFFEFF0F7)
val md_theme_light_inversePrimary         = Color(0xFFA5C8FF)

// Surface container tier (light) — pure-white canvas & calibrated M3 Expressive elevation
val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow    = Color(0xFFF3F3F9)
val md_theme_light_surfaceContainer       = Color(0xFFEEEDF4)
val md_theme_light_surfaceContainerHigh   = Color(0xFFE8E7EE)
val md_theme_light_surfaceContainerHighest= Color(0xFFE2E1E9)

// ─────────────────────────────────────────────────────────────────────────────
// Dark scheme — full M3 token set
// ─────────────────────────────────────────────────────────────────────────────
val md_theme_dark_primary                 = Color(0xFFA5C8FF)
val md_theme_dark_onPrimary               = Color(0xFF00315F)
val md_theme_dark_primaryContainer        = Color(0xFF004786)
val md_theme_dark_onPrimaryContainer      = Color(0xFFD4E3FF)

val md_theme_dark_secondary               = Color(0xFFC1CBDD)
val md_theme_dark_onSecondary             = Color(0xFF2B3341)
val md_theme_dark_secondaryContainer      = Color(0xFF424A58)
val md_theme_dark_onSecondaryContainer    = Color(0xFFDEE7F9)

val md_theme_dark_tertiary                = Color(0xFF94CCFF)
val md_theme_dark_onTertiary              = Color(0xFF003353)
val md_theme_dark_tertiaryContainer       = Color(0xFF004A75)
val md_theme_dark_onTertiaryContainer     = Color(0xFFCDE5FF)

val md_theme_dark_error                   = Color(0xFFFFB4AB)
val md_theme_dark_onError                 = Color(0xFF690005)
val md_theme_dark_errorContainer          = Color(0xFF93000A)
val md_theme_dark_onErrorContainer        = Color(0xFFFFDAD6)

val md_theme_dark_background              = Color(0xFF15181E)
val md_theme_dark_onBackground            = Color(0xFFE1E2E8)
val md_theme_dark_surface                 = Color(0xFF15181E)
val md_theme_dark_onSurface               = Color(0xFFE1E2E8)
val md_theme_dark_surfaceVariant          = Color(0xFF44474F)
val md_theme_dark_onSurfaceVariant        = Color(0xFFC4C6D0)
val md_theme_dark_outline                 = Color(0xFF8E9099)
val md_theme_dark_outlineVariant          = Color(0xFF44474F)
val md_theme_dark_scrim                   = Color(0xFF000000)
val md_theme_dark_inverseSurface          = Color(0xFFE1E2E8)
val md_theme_dark_inverseOnSurface        = Color(0xFF2E3135)
val md_theme_dark_inversePrimary          = Color(0xFF0A6EFF)

// Surface container tier (dark)
val md_theme_dark_surfaceContainerLowest  = Color(0xFF15181E)
val md_theme_dark_surfaceContainerLow     = Color(0xFF1D2027)
val md_theme_dark_surfaceContainer        = Color(0xFF22252D)
val md_theme_dark_surfaceContainerHigh    = Color(0xFF2A2D36)
val md_theme_dark_surfaceContainerHighest = Color(0xFF343741)

// Backward-compatibility aliases
val PrimaryLight = md_theme_light_primary
val OnPrimaryLight = md_theme_light_onPrimary
val PrimaryContainerLight = md_theme_light_primaryContainer
val OnPrimaryContainerLight = md_theme_light_onPrimaryContainer
val PrimaryDark = md_theme_dark_primary
val OnPrimaryDark = md_theme_dark_onPrimary
val PrimaryContainerDark = md_theme_dark_primaryContainer
val OnPrimaryContainerDark = md_theme_dark_onPrimaryContainer

// ─────────────────────────────────────────────────────────────────────────────
// Official Material 3 ColorSchemes for Photon Gallery
// ─────────────────────────────────────────────────────────────────────────────
val DefaultLightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_surfaceContainerLow,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surfaceContainerLow,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    scrim = md_theme_light_scrim,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceDim = md_theme_light_surfaceContainerHigh,
    surfaceBright = Color.White,
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest
)

val DefaultDarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    scrim = md_theme_dark_scrim,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceDim = md_theme_dark_surfaceContainerLow,
    surfaceBright = md_theme_dark_surfaceContainerHigh,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest
)

val DefaultAmoledColorScheme = DefaultDarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF2C2C2C),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF282828),
    surfaceVariant = Color(0xFF1F1F1F),
)

