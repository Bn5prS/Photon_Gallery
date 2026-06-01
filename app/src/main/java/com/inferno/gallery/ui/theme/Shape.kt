@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * M3 Expressive – 35-shape scale
 *
 * The scale moves linearly from **None** (0 dp, fully square) through increasing
 * roundedness up to **Full** (circle). It is divided into families:
 *
 *   None → ExtraSmall → Small → Medium → Large → ExtraLarge → ExtraExtraLarge → Full
 *
 * Each family can have up to five increments
 * (base, Increased, Increased-2, Increased-3, Increased-4) resulting in the
 * full 35-token palette that the M3 Expressive spec defines.
 *
 * The [Shapes] class exposed by Compose Material 3 ≥ 1.4.0 directly supports:
 *   extraSmall, small, medium, large, largeIncreased,
 *   extraLarge, extraLargeIncreased, extraExtraLarge
 *
 * For the remaining granular tokens used by custom components (e.g. edge-hugging
 * containers, avatar clips, morphing bounding boxes), we expose them as
 * top-level vals in this file so the entire design system can reference them.
 */

// ─────────────────────────────────────────────────────────────────────────────
//  Family: None  (0 dp – fully square)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeNone: Shape = RoundedCornerShape(0.dp)

// ─────────────────────────────────────────────────────────────────────────────
//  Family: ExtraSmall  (base 4 dp)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeExtraSmall: Shape = RoundedCornerShape(4.dp)
val ShapeExtraSmallIncreased: Shape = RoundedCornerShape(6.dp)
val ShapeExtraSmallIncreased2: Shape = RoundedCornerShape(7.dp)
val ShapeExtraSmallIncreased3: Shape = RoundedCornerShape(8.dp)  // same as Small base

// ─────────────────────────────────────────────────────────────────────────────
//  Family: Small  (base 8 dp)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeSmall: Shape = RoundedCornerShape(8.dp)
val ShapeSmallIncreased: Shape = RoundedCornerShape(10.dp)
val ShapeSmallIncreased2: Shape = RoundedCornerShape(11.dp)
val ShapeSmallIncreased3: Shape = RoundedCornerShape(12.dp)  // same as Medium base

// ─────────────────────────────────────────────────────────────────────────────
//  Family: Medium  (base 12 dp)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeMedium: Shape = RoundedCornerShape(12.dp)
val ShapeMediumIncreased: Shape = RoundedCornerShape(14.dp)
val ShapeMediumIncreased2: Shape = RoundedCornerShape(15.dp)
val ShapeMediumIncreased3: Shape = RoundedCornerShape(16.dp)  // same as Large base

// ─────────────────────────────────────────────────────────────────────────────
//  Family: Large  (base 16 dp)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeLarge: Shape = RoundedCornerShape(16.dp)
val ShapeLargeIncreased: Shape = RoundedCornerShape(20.dp)
val ShapeLargeIncreased2: Shape = RoundedCornerShape(22.dp)
val ShapeLargeIncreased3: Shape = RoundedCornerShape(24.dp)
val ShapeLargeIncreased4: Shape = RoundedCornerShape(26.dp)

// ─────────────────────────────────────────────────────────────────────────────
//  Family: ExtraLarge  (base 28 dp)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeExtraLarge: Shape = RoundedCornerShape(28.dp)
val ShapeExtraLargeIncreased: Shape = RoundedCornerShape(32.dp)
val ShapeExtraLargeIncreased2: Shape = RoundedCornerShape(34.dp)
val ShapeExtraLargeIncreased3: Shape = RoundedCornerShape(36.dp)
val ShapeExtraLargeIncreased4: Shape = RoundedCornerShape(40.dp)

// ─────────────────────────────────────────────────────────────────────────────
//  Family: ExtraExtraLarge  (base 48 dp – edge-hugging / highly rounded)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeExtraExtraLarge: Shape = RoundedCornerShape(48.dp)
val ShapeExtraExtraLargeIncreased: Shape = RoundedCornerShape(56.dp)
val ShapeExtraExtraLargeIncreased2: Shape = RoundedCornerShape(64.dp)
val ShapeExtraExtraLargeIncreased3: Shape = RoundedCornerShape(72.dp)

// ─────────────────────────────────────────────────────────────────────────────
//  Family: Full  (50 % – fully circular / pill)
// ─────────────────────────────────────────────────────────────────────────────
val ShapeFull: Shape = CircleShape   // 50 % corner radius — true circle/pill
val ShapeFullIncreased: Shape = RoundedCornerShape(percent = 50) // alias for readability

// ─────────────────────────────────────────────────────────────────────────────
//  Edge-hugging shapes (asymmetric corners for sheet / drawer edges)
// ─────────────────────────────────────────────────────────────────────────────
/** Top-only rounding — used for bottom sheets, top-attached panels. */
val ShapeEdgeTop: Shape = RoundedCornerShape(
    topStart = 28.dp, topEnd = 28.dp,
    bottomStart = 0.dp, bottomEnd = 0.dp
)

/** Bottom-only rounding — used for top sheets, notification trays. */
val ShapeEdgeBottom: Shape = RoundedCornerShape(
    topStart = 0.dp, topEnd = 0.dp,
    bottomStart = 28.dp, bottomEnd = 28.dp
)

/** Start-only rounding — used for end-anchored navigation drawers (RTL-aware). */
val ShapeEdgeStart: Shape = RoundedCornerShape(
    topStart = 28.dp, topEnd = 0.dp,
    bottomStart = 28.dp, bottomEnd = 0.dp
)

/** End-only rounding — used for start-anchored navigation drawers (RTL-aware). */
val ShapeEdgeEnd: Shape = RoundedCornerShape(
    topStart = 0.dp, topEnd = 28.dp,
    bottomStart = 0.dp, bottomEnd = 28.dp
)

/** Large top rounding — used for highly rounded bottom sheets / modals. */
val ShapeEdgeTopLarge: Shape = RoundedCornerShape(
    topStart = 48.dp, topEnd = 48.dp,
    bottomStart = 0.dp, bottomEnd = 0.dp
)

// ─────────────────────────────────────────────────────────────────────────────
//  MaterialTheme.shapes – wired into the Compose Shapes() constructor
// ─────────────────────────────────────────────────────────────────────────────
/**
 * The [Shapes] instance consumed by [MaterialExpressiveTheme].
 *
 * This maps the 8 constructor-supported tokens of [Shapes] to our scale above.
 * Custom components should reference the granular tokens (e.g. [ShapeLargeIncreased2])
 * directly for the additional 27 tokens that complete the 35-shape palette.
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
