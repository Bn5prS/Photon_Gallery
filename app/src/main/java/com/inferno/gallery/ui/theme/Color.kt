package com.inferno.gallery.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
//  Photon Gallery — Fallback Color Palette
//
//  These colors are used ONLY when Dynamic Color is unavailable (pre-Android 12
//  or when opted out). On Android 12+ the app uses dynamicLightColorScheme /
//  dynamicDarkColorScheme which auto-generates all tokens from the wallpaper.
//
//  The palette is built around a deep-indigo primary with cyan-teal secondary
//  and warm-peach tertiary — giving the gallery a vibrant, premium feel.
// ─────────────────────────────────────────────────────────────────────────────

// ── Primary (Deep Indigo) ──────────────────────────────────────────────────
val PrimaryLight = Color(0xFF3F51B5)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFDBE1FF)
val OnPrimaryContainerLight = Color(0xFF00105C)

val PrimaryDark = Color(0xFFB4C5FF)
val OnPrimaryDark = Color(0xFF002680)
val PrimaryContainerDark = Color(0xFF283593)
val OnPrimaryContainerDark = Color(0xFFDBE1FF)

// ── Secondary (Teal / Cyan) ────────────────────────────────────────────────
val SecondaryLight = Color(0xFF00897B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFA7F5EC)
val OnSecondaryContainerLight = Color(0xFF002018)

val SecondaryDark = Color(0xFF4FDBD0)
val OnSecondaryDark = Color(0xFF00382E)
val SecondaryContainerDark = Color(0xFF005046)
val OnSecondaryContainerDark = Color(0xFFA7F5EC)

// ── Tertiary (Warm Peach / Coral) ──────────────────────────────────────────
val TertiaryLight = Color(0xFFE64A19)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDBD0)
val OnTertiaryContainerLight = Color(0xFF3B0800)

val TertiaryDark = Color(0xFFFFB59E)
val OnTertiaryDark = Color(0xFF5F1500)
val TertiaryContainerDark = Color(0xFF862200)
val OnTertiaryContainerDark = Color(0xFFFFDBD0)

// ── Fixed Colors (Same across Light & Dark) ────────────────────────────────
val PrimaryFixed = Color(0xFFDBE1FF)
val OnPrimaryFixed = Color(0xFF00105C)
val PrimaryFixedDim = Color(0xFFB4C5FF)
val OnPrimaryFixedVariant = Color(0xFF283593)

val SecondaryFixed = Color(0xFFA7F5EC)
val OnSecondaryFixed = Color(0xFF002018)
val SecondaryFixedDim = Color(0xFF4FDBD0)
val OnSecondaryFixedVariant = Color(0xFF005046)

val TertiaryFixed = Color(0xFFFFDBD0)
val OnTertiaryFixed = Color(0xFF3B0800)
val TertiaryFixedDim = Color(0xFFFFB59E)
val OnTertiaryFixedVariant = Color(0xFF862200)

// ── Error ──────────────────────────────────────────────────────────────────
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ── Neutral / Surface ──────────────────────────────────────────────────────
val BackgroundLight = Color(0xFFFBF8FF)
val OnBackgroundLight = Color(0xFF1B1B21)
val SurfaceLight = Color(0xFFFBF8FF)
val SurfaceDimLight = Color(0xFFD8D6DC)
val SurfaceBrightLight = Color(0xFFFBF8FF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF3F0F6)
val SurfaceContainerLight = Color(0xFFEDEBF1)
val SurfaceContainerHighLight = Color(0xFFE8E5EC)
val SurfaceContainerHighestLight = Color(0xFFE2E1EC)
val OnSurfaceLight = Color(0xFF1B1B21)
val SurfaceVariantLight = Color(0xFFE2E1EC)
val OnSurfaceVariantLight = Color(0xFF45464F)
val OutlineLight = Color(0xFF757680)
val OutlineVariantLight = Color(0xFFC6C5D0)

val BackgroundDark = Color(0xFF121318)
val OnBackgroundDark = Color(0xFFE4E1E9)
val SurfaceDark = Color(0xFF121318)
val SurfaceDimDark = Color(0xFF121318)
val SurfaceBrightDark = Color(0xFF38393F)
val SurfaceContainerLowestDark = Color(0xFF0D0E13)
val SurfaceContainerLowDark = Color(0xFF1A1C22)
val SurfaceContainerDark = Color(0xFF1E2026)
val SurfaceContainerHighDark = Color(0xFF292A31)
val SurfaceContainerHighestDark = Color(0xFF34353B)
val OnSurfaceDark = Color(0xFFE4E1E9)
val SurfaceVariantDark = Color(0xFF45464F)
val OnSurfaceVariantDark = Color(0xFFC6C5D0)
val OutlineDark = Color(0xFF8F909A)
val OutlineVariantDark = Color(0xFF45464F)

// ── Inverse ────────────────────────────────────────────────────────────────
val InverseSurfaceLight = Color(0xFF303036)
val InverseOnSurfaceLight = Color(0xFFF2EFF7)
val InversePrimaryLight = Color(0xFFB4C5FF)

val InverseSurfaceDark = Color(0xFFE4E1E9)
val InverseOnSurfaceDark = Color(0xFF303036)
val InversePrimaryDark = Color(0xFF3F51B5)

// ── Scrim ──────────────────────────────────────────────────────────────────
val Scrim = Color(0xFF000000)