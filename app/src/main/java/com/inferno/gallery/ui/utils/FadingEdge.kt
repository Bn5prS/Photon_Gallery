package com.inferno.gallery.ui.utils

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws soft gradient fades at the top and/or bottom edges of a scrollable container.
 * The fade only appears when there is content to scroll in that direction.
 *
 * Uses [drawWithContent] which composites on the GPU with zero bitmap allocations per
 * frame. Brush objects are stable and only rebuilt when [color] changes.
 *
 * Uses [derivedStateOf] for the at-bottom check so the draw phase only invalidates
 * when the boolean changes, not on every scroll pixel.
 *
 * @param fadeLength Height of each fade gradient.
 * @param color      The color to fade into (should match the screen background).
 */
fun Modifier.verticalFadingEdge(
    scrollState: LazyGridState,
    fadeLength: Dp = 16.dp,
    color: Color = Color.Black,
): Modifier = this.composed {
    val topBrush = remember(color) {
        Brush.verticalGradient(listOf(color, Color.Transparent))
    }
    val bottomBrush = remember(color) {
        Brush.verticalGradient(listOf(Color.Transparent, color))
    }

    val canScrollUp by remember(scrollState) {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0
        }
    }
    val isAtBottom by remember(scrollState) {
        derivedStateOf {
            val layoutInfo = scrollState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisible != null &&
                lastVisible.index == layoutInfo.totalItemsCount - 1 &&
                lastVisible.offset.y + lastVisible.size.height <= layoutInfo.viewportEndOffset
        }
    }

    drawWithContent {
        drawContent()
        val fadePx = fadeLength.toPx()

        if (canScrollUp) {
            drawRect(
                brush = topBrush,
                size = size.copy(height = fadePx),
            )
        }

        if (!isAtBottom && scrollState.layoutInfo.totalItemsCount > 0) {
            drawRect(
                brush = bottomBrush,
                size = size.copy(height = fadePx),
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - fadePx),
            )
        }
    }
}
