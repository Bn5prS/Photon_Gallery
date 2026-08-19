@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive shape scale — official corner-radius tokens only.
 *
 * Source: https://m3.material.io/styles/shape/corner-radius-scale
 *
 *   None 0dp · Extra small 4dp · Small 8dp · Medium 12dp · Large 16dp ·
 *   Large increased 20dp · Extra large 28dp · Extra large increased 32dp ·
 *   Extra extra large 48dp · Full (100%)
 *
 * The 8 constructor-supported tokens are wired into [AppShapes]
 * (`MaterialTheme.shapes`); use [MaterialTheme.shapes.*] or these vals.
 * Never introduce a corner radius outside this scale.
 */

// ── Family: None (0dp — fully square, edge-to-edge media) ────────────────────
val ShapeNone: Shape = RoundedCornerShape(0.dp)

// ── Family: Extra small (4dp — badges, compact tags) ─────────────────────────
val ShapeExtraSmall = RoundedCornerShape(4.dp)

// ── Family: Small (8dp — menus, small controls) ──────────────────────────────
val ShapeSmall = RoundedCornerShape(8.dp)

// ── Family: Medium (12dp — cards, inputs) ────────────────────────────────────
val ShapeMedium = RoundedCornerShape(12.dp)

// ── Family: Large (16dp — cards, media containers) ───────────────────────────
val ShapeLarge = RoundedCornerShape(16.dp)

// ── Family: Large increased (20dp — featured cards; Expressive) ──────────────
val ShapeLargeIncreased = RoundedCornerShape(20.dp)

// ── Family: Extra large (28dp — dialogs, sheet tops) ─────────────────────────
val ShapeExtraLarge = RoundedCornerShape(28.dp)

// ── Family: Extra large increased (32dp — Expressive containers) ─────────────
val ShapeExtraLargeIncreased = RoundedCornerShape(32.dp)

// ── Family: Extra extra large (48dp — edge-hugging; Expressive) ──────────────
val ShapeExtraExtraLarge = RoundedCornerShape(48.dp)

// ── Family: Full (100% — buttons, chips, FABs, pills) ────────────────────────
val ShapeFull: Shape = CircleShape

// ─────────────────────────────────────────────────────────────────────────────
//  Edge-hugging shapes (asymmetric corners for sheet / drawer edges).
//  Top-corner 28dp/48dp rounding is the official M3 bottom-sheet treatment.
// ─────────────────────────────────────────────────────────────────────────────
/** Top-only rounding — bottom sheets, top-attached panels. */
val ShapeEdgeTop: Shape = RoundedCornerShape(
    topStart = 28.dp, topEnd = 28.dp,
    bottomStart = 0.dp, bottomEnd = 0.dp
)

/** Bottom-only rounding — top sheets, notification trays. */
val ShapeEdgeBottom: Shape = RoundedCornerShape(
    topStart = 0.dp, topEnd = 0.dp,
    bottomStart = 28.dp, bottomEnd = 28.dp
)

/** Start-only rounding — end-anchored navigation drawers (RTL-aware). */
val ShapeEdgeStart: Shape = RoundedCornerShape(
    topStart = 28.dp, topEnd = 0.dp,
    bottomStart = 28.dp, bottomEnd = 0.dp
)

/** End-only rounding — start-anchored navigation drawers (RTL-aware). */
val ShapeEdgeEnd: Shape = RoundedCornerShape(
    topStart = 0.dp, topEnd = 28.dp,
    bottomStart = 0.dp, bottomEnd = 28.dp
)

/** Large top rounding (48dp) — highly rounded bottom sheets / modals. */
val ShapeEdgeTopLarge: Shape = RoundedCornerShape(
    topStart = 48.dp, topEnd = 48.dp,
    bottomStart = 0.dp, bottomEnd = 0.dp
)

// ─────────────────────────────────────────────────────────────────────────────
//  Spacing tokens — 4/8dp grid
// ─────────────────────────────────────────────────────────────────────────────
object SpacingTokens {
    val XS = 4.dp
    val S  = 8.dp
    val M  = 12.dp
    val L  = 16.dp
    val XL = 24.dp
    val XXL = 32.dp
}

// ─────────────────────────────────────────────────────────────────────────────
//  Icon size tokens — Material Symbols baseline is 24dp
// ─────────────────────────────────────────────────────────────────────────────
object IconSizeTokens {
    val S  = 16.dp
    val M  = 20.dp
    val L  = 24.dp
    val XL = 32.dp
    val XXL = 48.dp
}

// ─────────────────────────────────────────────────────────────────────────────
//  MaterialTheme.shapes — wired into MaterialExpressiveTheme
// ─────────────────────────────────────────────────────────────────────────────
/**
 * The [Shapes] instance consumed by [MaterialExpressiveTheme], mapping the
 * full official scale into the Compose `Shapes` slots.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val AppShapes = Shapes(
    extraSmall = ShapeExtraSmall as RoundedCornerShape,
    small = ShapeSmall as RoundedCornerShape,
    medium = ShapeMedium as RoundedCornerShape,
    large = ShapeLarge as RoundedCornerShape,
    largeIncreased = ShapeLargeIncreased as RoundedCornerShape,
    extraLarge = ShapeExtraLarge as RoundedCornerShape,
    extraLargeIncreased = ShapeExtraLargeIncreased as RoundedCornerShape,
    extraExtraLarge = ShapeExtraExtraLarge as RoundedCornerShape,
)
