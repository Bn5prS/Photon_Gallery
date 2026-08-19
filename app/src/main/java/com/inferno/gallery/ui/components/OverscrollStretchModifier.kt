package com.inferno.gallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/**
 * A custom modifier that implements a high-performance, system-wide vertical
 * rubber-banding/overscroll stretch effect.
 *
 * When the boundary of a scroll container is reached, the entire viewport stretches
 * dynamically from the active pivot (top or bottom) and smoothly springs back to 0
 * once the gesture is released.
 */
fun Modifier.overscrollStretch(): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val overscrollOffset = remember { Animatable(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero
                val delta = available.y
                val currentOffset = overscrollOffset.value
                if (currentOffset > 0 && delta < 0) { // Pulling down at top, now scrolling back up
                    val consumed = delta.coerceAtLeast(-currentOffset)
                    coroutineScope.launch {
                        overscrollOffset.snapTo(currentOffset + consumed)
                    }
                    return Offset(0f, consumed)
                }
                if (currentOffset < 0 && delta > 0) { // Pulling up at bottom, now scrolling back down
                    val consumed = delta.coerceAtMost(-currentOffset)
                    coroutineScope.launch {
                        overscrollOffset.snapTo(currentOffset + consumed)
                    }
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    val currentOffset = overscrollOffset.value
                    
                    // Apply resistance to natural scroll boundaries (rubber-banding)
                    val newOffset = currentOffset + delta * 0.35f
                    coroutineScope.launch {
                        overscrollOffset.snapTo(newOffset.coerceIn(-200f, 200f))
                    }
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (overscrollOffset.value != 0f) {
                    coroutineScope.launch {
                        overscrollOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    this
        .graphicsLayer {
            val translationYValue = overscrollOffset.value
            val scaleYValue = 1f + (Math.abs(translationYValue) / 5000f)
            
            translationY = translationYValue
            scaleY = scaleYValue
            transformOrigin = if (translationYValue > 0) {
                TransformOrigin(0.5f, 0f) // pivot at top
            } else {
                TransformOrigin(0.5f, 1f) // pivot at bottom
            }
        }
        .nestedScroll(nestedScrollConnection)
}
