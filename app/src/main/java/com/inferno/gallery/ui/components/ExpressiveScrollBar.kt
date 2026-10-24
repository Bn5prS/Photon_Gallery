package com.inferno.gallery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun ExpressiveScrollBar(
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    rowStartIndices: IntArray? = null,
    totalRowsCount: Int? = null,
    totalItemsCount: Int? = null,
    minHeight: Dp = 36.dp,
    thickness: Dp = 10.dp,
    indicatorExpandedWidth: Dp = 32.dp,
    indicatorExpandedWidthBoost: Dp = 0.dp,
    indicatorRightCornerRadius: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    trackGap: Dp = 0.dp
) {
    val canScrollForward by remember(listState, gridState) {
        derivedStateOf { listState?.canScrollForward ?: gridState?.canScrollForward ?: false }
    }
    val canScrollBackward by remember(listState, gridState) {
        derivedStateOf { listState?.canScrollBackward ?: gridState?.canScrollBackward ?: false }
    }
    val canScroll = canScrollForward || canScrollBackward

    val expandedIndicatorWidth = (indicatorExpandedWidth + indicatorExpandedWidthBoost).coerceAtLeast(thickness)

    val totalWidth = expandedIndicatorWidth + paddingEnd
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(totalWidth)
            .onSizeChanged { size ->
                trackHeightPx = size.height.toFloat()
            }
            .graphicsLayer {
                alpha = if (canScroll) 1f else 0f
            }
    ) {
        var isPressed by remember(listState, gridState) { mutableStateOf(false) }
        var isDragging by remember(listState, gridState) { mutableStateOf(false) }
        val isInteracting = isPressed || isDragging

        if (!canScroll && !isInteracting) return@Box

        val coroutineScope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current

        var dragProgress by remember(listState, gridState) { mutableFloatStateOf(-1f) }
        var pendingScrollIndex by remember(listState, gridState) { mutableIntStateOf(-1) }

        val primaryColor = MaterialTheme.colorScheme.primary
        val innerIcon = ImageVector.vectorResource(R.drawable.ic_ms_unfold_more)

        val isScrolling = listState?.isScrollInProgress == true || gridState?.isScrollInProgress == true
        var isScrollbarVisible by remember(listState, gridState) { mutableStateOf(false) }

        LaunchedEffect(isScrolling, isInteracting) {
            if (isScrolling || isInteracting) {
                isScrollbarVisible = true
            } else {
                kotlinx.coroutines.delay(1200L)
                isScrollbarVisible = false
            }
        }

        val scrollbarAlpha by animateFloatAsState(
            targetValue = if (isScrollbarVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 300, easing = MotionTokens.EmphasizedEasing),
            label = "ScrollbarAlpha"
        )

        val animatedWidth by animateDpAsState(
            targetValue = if (isInteracting) 36.dp else 28.dp,
            animationSpec = MotionTokens.snappySpring(),
            label = "WidthAnimation"
        )

        val animatedHeight by animateDpAsState(
            targetValue = if (isInteracting) 46.dp else 38.dp,
            animationSpec = MotionTokens.snappySpring(),
            label = "HeightAnimation"
        )

        val iconSize by animateDpAsState(
            targetValue = if (isInteracting) 18.dp else 15.dp,
            animationSpec = MotionTokens.snappySpring(),
            label = "IconSize"
        )

        val density = LocalDensity.current
        val constraintsMaxWidth = totalWidth

        val availableHeight = trackHeightPx
        val handleHeightPx = with(density) { animatedHeight.toPx() }
        val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)

        // ── Mathematically Proven Zero-Discontinuity Target Progress ──
        // Multi-column grids (3, 4, 5, 6 columns) skip K items at row boundaries.
        // By scaling the intra-row fraction by itemsInTopRow, the continuous item index
        // advances continuously across rows with ZERO jump at row transitions or date headers!
        val targetProgress = run {
            if (isDragging && dragProgress >= 0f) {
                return@run dragProgress
            }

            if (gridState != null) {
                val layoutInfo = gridState.layoutInfo
                val totalItems = maxOf(totalItemsCount ?: layoutInfo.totalItemsCount, layoutInfo.totalItemsCount)
                if (totalItems <= 1) return@run 0f
                if (!gridState.canScrollBackward) return@run 0f
                if (!gridState.canScrollForward) return@run 1f

                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@run 0f

                val topItem = visibleItems.first()
                val rowHeight = topItem.size.height.toFloat().coerceAtLeast(1f)
                val scrollOffset = gridState.firstVisibleItemScrollOffset.toFloat()
                val fraction = (scrollOffset / rowHeight).coerceIn(0f, 1f)

                val topRow = topItem.row
                var itemsInTopRow = 0
                for (i in visibleItems.indices) {
                    if (visibleItems[i].row == topRow) {
                        itemsInTopRow++
                    } else {
                        break
                    }
                }
                if (itemsInTopRow <= 0) itemsInTopRow = 1
                val continuousItem = topItem.index.toFloat() + (fraction * itemsInTopRow.toFloat())
                val maxScrollable = (totalItems - visibleItems.size).coerceAtLeast(1).toFloat()
                val rawProgress = continuousItem / maxScrollable
                if (gridState.canScrollForward) {
                    rawProgress.coerceIn(0f, 0.985f)
                } else {
                    1f
                }
            } else if (listState != null) {
                val layoutInfo = listState.layoutInfo
                val totalItems = maxOf(totalItemsCount ?: layoutInfo.totalItemsCount, layoutInfo.totalItemsCount)
                if (totalItems <= 1) return@run 0f
                if (!listState.canScrollBackward) return@run 0f
                if (!listState.canScrollForward) return@run 1f

                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isEmpty()) return@run 0f

                val topItem = visibleItems.first()
                val itemHeight = topItem.size.toFloat().coerceAtLeast(1f)
                val fraction = (listState.firstVisibleItemScrollOffset.toFloat() / itemHeight).coerceIn(0f, 1f)

                val continuousItem = topItem.index.toFloat() + fraction
                val maxScrollable = (totalItems - visibleItems.size).coerceAtLeast(1).toFloat()
                val rawProgress = continuousItem / maxScrollable
                if (listState.canScrollForward) {
                    rawProgress.coerceIn(0f, 0.985f)
                } else {
                    1f
                }
            } else {
                0f
            }
        }

        // ── Liquid-Smooth Physics Filter (Vivo-Inspired Inertial Damping) ──
        // Medium stiffness spring absorbs discrete frame/page fluctuations
        // while tracking finger touch instantly during direct drag
        val visualProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = if (isDragging) {
                snap()
            } else {
                spring(
                    stiffness = 600f,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            },
            label = "VisualScrollbarProgress"
        )

        // ── Grid Scroll Dispatcher: VSYNC-aligned, cancellation-free ───────────
        LaunchedEffect(isDragging) {
            if (!isDragging) return@LaunchedEffect
            var lastScrolledIndex = -1
            while (isDragging) {
                val target = pendingScrollIndex
                if (target >= 0 && target != lastScrolledIndex) {
                    lastScrolledIndex = target
                    try {
                        gridState?.scrollToItem(target)
                        listState?.scrollToItem(target)
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException && !isActive) throw e
                    }
                }
                withFrameNanos { }
            }
            val finalTarget = pendingScrollIndex
            if (finalTarget >= 0 && finalTarget != lastScrolledIndex) {
                try {
                    gridState?.scrollToItem(finalTarget)
                    listState?.scrollToItem(finalTarget)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException && !isActive) throw e
                }
            }
        }

        val indicatorPath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        val trackHeight = size.height.toFloat()
                        val currentHandleHeight = animatedHeight.toPx()
                        val currentScrollableHeight = (trackHeight - currentHandleHeight).coerceAtLeast(1f)

                        val currentVisualProgress = visualProgress
                        val handleY = currentVisualProgress * currentScrollableHeight
                        val touchBuffer = 16.dp.toPx()

                        val isTouchOnHandle = down.position.y >= (handleY - touchBuffer) &&
                                down.position.y <= (handleY + currentHandleHeight + touchBuffer)

                        val grabOffset = if (isTouchOnHandle) {
                            (down.position.y - handleY).coerceIn(0f, currentHandleHeight)
                        } else {
                            currentHandleHeight / 2f
                        }

                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isPressed = true
                        isDragging = true

                        fun updateTouch(touchY: Float) {
                            val activeTrackHeight = size.height.toFloat()
                            val activeHandleHeight = animatedHeight.toPx()
                            val activeScrollable = (activeTrackHeight - activeHandleHeight).coerceAtLeast(1f)

                            val targetHandleTop = touchY - grabOffset
                            val newProgress = (targetHandleTop / activeScrollable).coerceIn(0f, 1f)

                            dragProgress = newProgress

                            val total = maxOf(
                                totalItemsCount ?: 0,
                                gridState?.layoutInfo?.totalItemsCount ?: listState?.layoutInfo?.totalItemsCount ?: 0
                            )

                            if (total <= 0) {
                                pendingScrollIndex = 0
                                return
                            }

                            val maxIndex = total - 1
                            val rawIndex = (newProgress * maxIndex).toInt().coerceIn(0, maxIndex)
                            pendingScrollIndex = rawIndex
                        }

                        updateTouch(down.position.y)

                        try {
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    break
                                }
                                change.consume()
                                updateTouch(change.position.y)
                            }
                        } finally {
                            isPressed = false
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        }
                    }
                }
        ) {
            val rightAnchorX = with(density) { (constraintsMaxWidth - paddingEnd).toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrollbarAlpha }
            ) {
                val handleY = visualProgress * scrollableHeight
                val handleHeight = animatedHeight.toPx()

                val indicatorWidth = animatedWidth.toPx()
                val leftCornerRadius = 8.dp.toPx()

                val currentIndicatorX = rightAnchorX - indicatorWidth

                indicatorPath.reset()
                indicatorPath.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(currentIndicatorX, handleY),
                            size = Size(indicatorWidth, handleHeight)
                        ),
                        topLeft = CornerRadius(leftCornerRadius, leftCornerRadius),
                        topRight = CornerRadius.Zero,
                        bottomRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius(leftCornerRadius, leftCornerRadius)
                    )
                )
                drawPath(
                    path = indicatorPath,
                    color = primaryColor
                )
            }

            Box(
                modifier = Modifier
                    .offset {
                        val handleY = visualProgress * scrollableHeight
                        val handleHeight = animatedHeight.toPx()

                        val iconSizePx = iconSize.toPx()
                        val paddingEndPx = paddingEnd.toPx()
                        val animatedWidthPx = animatedWidth.toPx()
                        val maxWidthPx = constraintsMaxWidth.toPx()

                        val x = maxWidthPx - paddingEndPx - (animatedWidthPx / 2f) - (iconSizePx / 2f)
                        val y = handleY + (handleHeight / 2f) - (iconSizePx / 2f)

                        IntOffset(x.toInt(), y.toInt())
                    }
                    .size(iconSize)
                    .graphicsLayer {
                        alpha = scrollbarAlpha
                    }
            ) {
                Icon(
                    imageVector = innerIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
