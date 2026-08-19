package com.inferno.gallery.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Photon Gallery — Container Modifier.
 *
 * Adds layered depth to any composable: subtle shadow, composited
 * background color, optional thin border, and shape clipping.
 *
 * The compositing trick: instead of using a flat tonal color, the
 * container color is blended over the screen background. This means
 * nested containers automatically get progressively deeper shades
 * without manually choosing colors for each layer.
 */

/**
 * Applies a premium container look with shadow, composited background,
 * optional border, and shape clipping.
 *
 * @param shape The corner shape. Defaults to `MaterialTheme.shapes.large`.
 * @param backgroundColor The container fill color. Defaults to surfaceContainerLow.
 * @param shadowElevation Shadow depth. Set to 0.dp to disable shadow.
 * @param borderColor Border stroke color. Set to [Color.Transparent] to disable.
 * @param borderWidth Border stroke width.
 */
fun Modifier.photonContainer(
    shape: Shape? = null,
    backgroundColor: Color? = null,
    shadowElevation: Dp = 0.5.dp,
    borderColor: Color? = null,
    borderWidth: Dp = 0.5.dp,
): Modifier = composed {
    val resolvedShape = shape ?: MaterialTheme.shapes.large
    val resolvedBg = backgroundColor ?: MaterialTheme.colorScheme.surfaceContainerLow
    val resolvedBorder = borderColor
        ?: MaterialTheme.colorScheme.onSecondaryContainer
            .copy(alpha = 0.08f)
            .compositeOver(resolvedBg)

    this
        .shadow(
            elevation = shadowElevation,
            shape = resolvedShape,
            ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f),
            spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
        )
        .clip(resolvedShape)
        .then(if (resolvedBorder != Color.Transparent) Modifier.border(borderWidth, resolvedBorder, resolvedShape) else Modifier)
        .background(resolvedBg)
}

/**
 * A lighter variant for secondary/nested containers.
 * Uses surfaceContainer (one step above surfaceContainerLow) and
 * a slightly more visible border for visual separation.
 */
fun Modifier.photonContainerNested(
    shape: Shape? = null,
): Modifier = composed {
    val resolvedShape = shape ?: MaterialTheme.shapes.medium
    val bg = MaterialTheme.colorScheme.surfaceContainer
    val border = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)

    this
        .shadow(
            elevation = 0.25.dp,
            shape = resolvedShape,
            ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
            spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.04f),
        )
        .clip(resolvedShape)
        .border(0.5.dp, border, resolvedShape)
        .background(bg)
}
