package com.inferno.gallery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.theme.MotionTokens
import kotlinx.coroutines.isActive

internal fun calculateScrollProgress(
    gridState: LazyGridState?,
    listState: LazyListState?,
    totalItemsCount: Int?
): Float {
    if (gridState != null) {
        val layoutInfo = gridState.layoutInfo
        val totalItems = maxOf(totalItemsCount ?: layoutInfo.totalItemsCount, layoutInfo.totalItemsCount)
        if (totalItems <= 1) return 0f
        if (!gridState.canScrollBackward) return 0f

        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0f

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
        return (continuousItem / maxScrollable).coerceIn(0f, 1f)
    } else if (listState != null) {
        val layoutInfo = listState.layoutInfo
        val totalItems = maxOf(totalItemsCount ?: layoutInfo.totalItemsCount, layoutInfo.totalItemsCount)
        if (totalItems <= 1) return 0f
        if (!listState.canScrollBackward) return 0f

        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return 0f

        val topItem = visibleItems.first()
        val itemHeight = topItem.size.toFloat().coerceAtLeast(1f)
        val fraction = (listState.firstVisibleItemScrollOffset.toFloat() / itemHeight).coerceIn(0f, 1f)

        val continuousItem = topItem.index.toFloat() + fraction
        val maxScrollable = (totalItems - visibleItems.size).coerceAtLeast(1).toFloat()
        return (continuousItem / maxScrollable).coerceIn(0f, 1f)
    }
    return 0f
}

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
    val canScroll by remember(listState, gridState) {
        derivedStateOf {
            (listState?.canScrollForward == true || listState?.canScrollBackward == true ||
             gridState?.canScrollForward == true || gridState?.canScrollBackward == true)
        }
    }

    val expandedIndicatorWidth = (indicatorExpandedWidth + indicatorExpandedWidthBoost).coerceAtLeast(thickness)
    val totalWidth = expandedIndicatorWidth + paddingEnd
    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    var isPressed by remember(listState, gridState) { mutableStateOf(false) }
    var isDragging by remember(listState, gridState) { mutableStateOf(false) }
    val isInteracting = isPressed || isDragging

    var dragProgress by remember(listState, gridState) { mutableFloatStateOf(-1f) }
    var pendingScrollIndex by remember(listState, gridState) { mutableIntStateOf(-1) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val haptic = LocalHapticFeedback.current

    // Auto-hide visibility based on scroll / interaction via snapshotFlow (zero composition reads)
    var isScrollbarVisible by remember(listState, gridState) { mutableStateOf(false) }

    LaunchedEffect(listState, gridState, isInteracting) {
        snapshotFlow {
            (listState?.isScrollInProgress == true || gridState?.isScrollInProgress == true) || isInteracting
        }
            .distinctUntilChanged()
            .collect { active ->
                if (active) {
                    isScrollbarVisible = true
                } else {
                    kotlinx.coroutines.delay(1200L)
                    isScrollbarVisible = false
                }
            }
    }

    val scrollbarAlpha by animateFloatAsState(
        targetValue = if (isScrollbarVisible && (canScroll || isInteracting)) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = MotionTokens.EmphasizedEasing),
        label = "ScrollbarAlpha"
    )

    // Interaction expansion (0f = resting 28dp x 38dp, 1f = interacting 36dp x 46dp)
    val interactionProgress by animateFloatAsState(
        targetValue = if (isInteracting) 1f else 0f,
        animationSpec = MotionTokens.snappySpring(),
        label = "ScrollBarInteraction"
    )

    // Throttled grid scroll dispatcher: 30Hz throttle to prevent main-thread choking & Coil queue flooding
    LaunchedEffect(isDragging) {
        if (!isDragging) {
            com.inferno.gallery.data.ThumbnailScrollThrottle.isFastScrolling = false
            return@LaunchedEffect
        }
        com.inferno.gallery.data.ThumbnailScrollThrottle.isFastScrolling = true
        var lastScrolledIndex = -1
        while (isDragging) {
            val target = pendingScrollIndex
            if (target >= 0) {
                lastScrolledIndex = target
                try {
                    gridState?.scrollToItem(target)
                    listState?.scrollToItem(target)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException && !isActive) throw e
                }
            }
            kotlinx.coroutines.delay(32L)
        }
        val finalTarget = pendingScrollIndex
        if (finalTarget >= 0) {
            try {
                gridState?.scrollToItem(finalTarget)
                listState?.scrollToItem(finalTarget)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException && !isActive) throw e
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(totalWidth)
            .onSizeChanged { size ->
                trackHeightPx = size.height.toFloat()
            }
            .pointerInput(totalItemsCount) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    val trackHeight = size.height.toFloat()
                    val currentHandleHeight = (38f + 8f * interactionProgress).dp.toPx()
                    val currentScrollableHeight = (trackHeight - currentHandleHeight).coerceAtLeast(1f)

                    val currentProgress = if (isDragging && dragProgress >= 0f) {
                        dragProgress
                    } else {
                        calculateScrollProgress(gridState, listState, totalItemsCount)
                    }
                    val handleY = currentProgress * currentScrollableHeight
                    val touchBuffer = 20.dp.toPx()

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
                        val activeHandleHeight = (38f + 8f * interactionProgress).dp.toPx()
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
                            if (!change.pressed) break
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
        // Pure Draw-Phase Rendering:
        // gridState layoutInfo & firstVisibleItemScrollOffset are read ONLY inside onDraw!
        // ZERO Recompositions during scrolling! ZERO layout passes!
        Canvas(modifier = Modifier.fillMaxSize()) {
            val alpha = scrollbarAlpha
            if (alpha <= 0.001f) return@Canvas

            val progress = if (isDragging && dragProgress >= 0f) {
                dragProgress
            } else {
                calculateScrollProgress(gridState, listState, totalItemsCount)
            }

            val baseWidth = 28.dp.toPx()
            val expandedWidth = 36.dp.toPx()
            val baseHeight = 38.dp.toPx()
            val expandedHeight = 46.dp.toPx()

            val currentWidth = baseWidth + (expandedWidth - baseWidth) * interactionProgress
            val currentHeight = baseHeight + (expandedHeight - baseHeight) * interactionProgress

            val trackHeight = size.height
            val scrollableH = (trackHeight - currentHeight).coerceAtLeast(1f)
            val handleY = (progress * scrollableH).coerceIn(0f, scrollableH)

            val rightMargin = paddingEnd.toPx()
            val indicatorX = size.width - rightMargin - currentWidth
            val cornerRadius = 8.dp.toPx()

            // 1. Draw rounded handle pill
            drawRoundRect(
                color = primaryColor.copy(alpha = alpha),
                topLeft = Offset(indicatorX, handleY),
                size = Size(currentWidth, currentHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            // 2. Draw unfold chevron glyph directly in Canvas
            val glyphAlpha = alpha
            val glyphColor = onPrimaryColor.copy(alpha = glyphAlpha)
            val centerX = indicatorX + currentWidth / 2f
            val centerY = handleY + currentHeight / 2f

            val arrowHalfW = (3.5f + 0.5f * interactionProgress).dp.toPx()
            val arrowH = 2.5.dp.toPx()
            val arrowGap = 2.dp.toPx()
            val strokeW = 1.75.dp.toPx()

            // Up chevron
            drawLine(
                color = glyphColor,
                start = Offset(centerX - arrowHalfW, centerY - arrowGap),
                end = Offset(centerX, centerY - arrowGap - arrowH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = glyphColor,
                start = Offset(centerX, centerY - arrowGap - arrowH),
                end = Offset(centerX + arrowHalfW, centerY - arrowGap),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )

            // Down chevron
            drawLine(
                color = glyphColor,
                start = Offset(centerX - arrowHalfW, centerY + arrowGap),
                end = Offset(centerX, centerY + arrowGap + arrowH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
            drawLine(
                color = glyphColor,
                start = Offset(centerX, centerY + arrowGap + arrowH),
                end = Offset(centerX + arrowHalfW, centerY + arrowGap),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}
