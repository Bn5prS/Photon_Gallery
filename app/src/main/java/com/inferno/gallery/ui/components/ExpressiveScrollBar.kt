package com.inferno.gallery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R
import androidx.compose.ui.graphics.vector.ImageVector


private data class ScrollMetrics(
    val progress: Float,
    val totalItemsCount: Int,
    val maxScrollIndex: Int,
    val scrollableHeight: Float
)

private data class VisibleGridLineMetrics(
    val index: Int,
    val offsetPx: Int,
    val sizePx: Int
)

private fun estimateListFallbackStridePx(
    visibleItems: List<LazyListItemInfo>,
    spacingPx: Int
): Float {
    val strideSamples = visibleItems
        .zipWithNext()
        .mapNotNull { (current, next) ->
            (next.offset - current.offset)
                .takeIf { next.index == current.index + 1 && it > 0 }
                ?.toFloat()
        }

    return medianOrNull(strideSamples)
        ?: medianOrNull(visibleItems.map { it.size.toFloat() + spacingPx })
        ?: 1f
}

private fun observeListLayoutMetrics(
    layoutInfo: LazyListLayoutInfo,
    tracker: AxisObservationTracker
) {
    tracker.resetIfNeeded(
        totalItemsCount = layoutInfo.totalItemsCount,
        spacingPx = layoutInfo.mainAxisItemSpacing
    )

    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return

    tracker.observeRepresentativeSample(
        strideSamplePx = estimateListFallbackStridePx(
            visibleItems = visibleItems,
            spacingPx = layoutInfo.mainAxisItemSpacing
        ),
        itemSizeSamplePx = medianOrNull(visibleItems.map { it.size.toFloat() })
    )

    visibleItems.forEach { item ->
        tracker.observeItemSize(index = item.index, sizePx = item.size.toFloat())
    }

    visibleItems
        .zipWithNext()
        .forEach { (current, next) ->
            if (next.index == current.index + 1) {
                tracker.observeStride(
                    index = current.index,
                    stridePx = (next.offset - current.offset).toFloat()
                )
            }
        }

    val lastVisibleItem = visibleItems.last()
    if (lastVisibleItem.index < layoutInfo.totalItemsCount - 1) {
        tracker.observeStride(
            index = lastVisibleItem.index,
            stridePx = (lastVisibleItem.size + layoutInfo.mainAxisItemSpacing).toFloat()
        )
    }
}

private fun buildVisibleGridLines(layoutInfo: LazyGridLayoutInfo): List<VisibleGridLineMetrics> {
    val isVertical =
        layoutInfo.orientation == androidx.compose.foundation.gestures.Orientation.Vertical
    val groupedLines = linkedMapOf<Int, MutableList<LazyGridItemInfo>>()

    layoutInfo.visibleItemsInfo.forEach { item ->
        val lineIndex = if (isVertical) item.row else item.column
        if (lineIndex >= 0) {
            groupedLines.getOrPut(lineIndex) { mutableListOf() }.add(item)
        }
    }

    return groupedLines
        .entries
        .map { (lineIndex, itemsInLine) ->
            VisibleGridLineMetrics(
                index = lineIndex,
                offsetPx = itemsInLine.minOf { if (isVertical) it.offset.y else it.offset.x },
                sizePx = itemsInLine.maxOf { if (isVertical) it.size.height else it.size.width }
            )
        }
        .sortedBy { it.index }
}

private fun estimateGridFallbackStridePx(
    visibleLines: List<VisibleGridLineMetrics>,
    spacingPx: Int
): Float {
    val strideSamples = visibleLines
        .zipWithNext()
        .mapNotNull { (current, next) ->
            (next.offsetPx - current.offsetPx)
                .takeIf { next.index == current.index + 1 && it > 0 }
                ?.toFloat()
        }

    return medianOrNull(strideSamples)
        ?: medianOrNull(visibleLines.map { it.sizePx.toFloat() + spacingPx })
        ?: 1f
}

private fun observeGridLayoutMetrics(
    layoutInfo: LazyGridLayoutInfo,
    tracker: AxisObservationTracker
): List<VisibleGridLineMetrics> {
    tracker.resetIfNeeded(
        totalItemsCount = layoutInfo.totalItemsCount,
        spacingPx = layoutInfo.mainAxisItemSpacing
    )

    val visibleLines = buildVisibleGridLines(layoutInfo)
    if (visibleLines.isEmpty()) return visibleLines

    tracker.observeRepresentativeSample(
        strideSamplePx = estimateGridFallbackStridePx(
            visibleLines = visibleLines,
            spacingPx = layoutInfo.mainAxisItemSpacing
        ),
        itemSizeSamplePx = medianOrNull(visibleLines.map { it.sizePx.toFloat() })
    )

    visibleLines.forEach { line ->
        tracker.observeItemSize(index = line.index, sizePx = line.sizePx.toFloat())
    }

    visibleLines
        .zipWithNext()
        .forEach { (current, next) ->
            if (next.index == current.index + 1) {
                tracker.observeStride(
                    index = current.index,
                    stridePx = (next.offsetPx - current.offsetPx).toFloat()
                )
            }
        }

    val totalLines = ((layoutInfo.totalItemsCount + layoutInfo.maxSpan - 1) / layoutInfo.maxSpan)
        .coerceAtLeast(1)
    val lastVisibleLine = visibleLines.last()
    if (lastVisibleLine.index < totalLines - 1) {
        tracker.observeStride(
            index = lastVisibleLine.index,
            stridePx = (lastVisibleLine.sizePx + layoutInfo.mainAxisItemSpacing).toFloat()
        )
    }

    return visibleLines
}

@Composable
fun ExpressiveScrollBar(
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    gridState: LazyGridState? = null,
    minHeight: Dp = 36.dp,
    thickness: Dp = 10.dp,
    indicatorExpandedWidth: Dp = 32.dp,
    indicatorExpandedWidthBoost: Dp = 0.dp,
    indicatorRightCornerRadius: Dp = 0.dp,
    paddingEnd: Dp = 0.dp,
    trackGap: Dp = 0.dp,
    dragLabelProvider: ((Int) -> String?)? = null,
    dragLabelSize: Dp = 40.dp,
    dragLabelGap: Dp = 12.dp
) {
    val canScrollForward by remember(listState, gridState) { derivedStateOf { listState?.canScrollForward ?: gridState?.canScrollForward ?: false } }
    val canScrollBackward by remember(listState, gridState) { derivedStateOf { listState?.canScrollBackward ?: gridState?.canScrollBackward ?: false } }
    val canScroll = canScrollForward || canScrollBackward

    val listMetricsTracker = remember(listState) { AxisObservationTracker() }
    val gridMetricsTracker = remember(gridState) { AxisObservationTracker() }
    val expandedIndicatorWidth = (indicatorExpandedWidth + indicatorExpandedWidthBoost).coerceAtLeast(thickness)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(if (canScroll) expandedIndicatorWidth + paddingEnd else 0.dp)
    ) {
        if (!canScroll) return@BoxWithConstraints

        var isPressed by remember(listState, gridState) { mutableStateOf(false) }
        var isDragging by remember(listState, gridState) { mutableStateOf(false) }
        var dragProgress by remember(listState, gridState) { mutableFloatStateOf(-1f) }
        var pendingScrollIndex by remember(listState, gridState) { mutableIntStateOf(-1) }
        var retainedDragLabel by remember(listState, gridState) { mutableStateOf<String?>(null) }
        val displayedProgress = remember(listState, gridState) { Animatable(0f) }
        var hasSyncedDisplayedProgress by remember(listState, gridState) { mutableStateOf(false) }

        val primaryColor = MaterialTheme.colorScheme.primary
        val restingColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        val innerIcon = ImageVector.vectorResource(R.drawable.ic_ms_unfold_more)

        val isInteracting = isPressed || isDragging
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
            targetValue = if (isInteracting) 34.dp else 26.dp,
            animationSpec = MotionTokens.snappySpring(),
            label = "WidthAnimation"
        )

        val animatedHeight by animateDpAsState(
            targetValue = if (isInteracting) 44.dp else 36.dp,
            animationSpec = MotionTokens.snappySpring(),
            label = "HeightAnimation"
        )

        val iconSize by animateDpAsState(
            targetValue = if (isInteracting) 18.dp else 15.dp,
            animationSpec = MotionTokens.snappySpring(),
            label = "IconSize"
        )

        val animatedColor = primaryColor
        val density = LocalDensity.current
        val constraintsMaxWidth = maxWidth
        val constraintsMaxHeight = maxHeight
        val coarseJumpThresholdPx = with(density) { 16.dp.toPx() }
        val smoothJumpMinDistancePx = with(density) { 10.dp.toPx() }

        val availableHeight = with(density) { constraintsMaxHeight.toPx() }
        val handleHeightPx = with(density) { animatedHeight.toPx() }
        val scrollableHeight = (availableHeight - handleHeightPx).coerceAtLeast(1f)
        
        fun getScrollStats(): ScrollMetrics {
            val totalItemsCount: Int
            val currentScrollPx: Float
            val totalScrollableContentPx: Float
            val approximateMaxScrollIndex: Int

            if (listState != null) {
                val layoutInfo = listState.layoutInfo
                totalItemsCount = layoutInfo.totalItemsCount
                if (totalItemsCount == 0) return ScrollMetrics(0f, 0, 1, scrollableHeight)
                
                val visibleItems = layoutInfo.visibleItemsInfo
                val firstItem = visibleItems.firstOrNull() ?: return ScrollMetrics(0f, totalItemsCount, 1, scrollableHeight)
                
                val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat().coerceAtLeast(1f)
                var sizeSum = 0
                for (v in visibleItems) sizeSum += v.size
                val avgItemSize = (sizeSum.toFloat() / visibleItems.size).coerceAtLeast(1f)
                val estimatedTotalHeight = totalItemsCount * avgItemSize
                
                currentScrollPx = (firstItem.index * avgItemSize) - firstItem.offset.toFloat()
                totalScrollableContentPx = (estimatedTotalHeight - viewportHeightPx).coerceAtLeast(1f)
                approximateMaxScrollIndex = totalItemsCount - 1
            } else if (gridState != null) {
                val layoutInfo = gridState.layoutInfo
                totalItemsCount = layoutInfo.totalItemsCount
                if (totalItemsCount == 0) return ScrollMetrics(0f, 0, 1, scrollableHeight)
                
                val visibleItems = layoutInfo.visibleItemsInfo
                val firstItem = visibleItems.firstOrNull() ?: return ScrollMetrics(0f, totalItemsCount, 1, scrollableHeight)
                
                val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat().coerceAtLeast(1f)
                val spanCount = layoutInfo.maxSpan
                val totalRows = (totalItemsCount + spanCount - 1) / spanCount
                
                var heightSum = 0
                for (v in visibleItems) heightSum += v.size.height
                val avgItemSize = (heightSum.toFloat() / visibleItems.size).coerceAtLeast(1f)
                val estimatedTotalHeight = totalRows * avgItemSize
                
                val currentRow = firstItem.index / spanCount
                currentScrollPx = (currentRow * avgItemSize) - firstItem.offset.y.toFloat()
                totalScrollableContentPx = (estimatedTotalHeight - viewportHeightPx).coerceAtLeast(1f)
                approximateMaxScrollIndex = totalItemsCount - 1
            } else {
                return ScrollMetrics(0f, 0, 1, scrollableHeight)
            }

            val progress = (currentScrollPx / totalScrollableContentPx).coerceIn(0f, 1f)
            return ScrollMetrics(progress, totalItemsCount, approximateMaxScrollIndex.coerceAtLeast(1), scrollableHeight)
        }

        fun resolveDragTargetIndex(progress: Float, maxScrollIndex: Int, totalItemsCount: Int): Int {
            if (totalItemsCount <= 0) return 0
            if (progress <= 0f) return 0
            if (progress >= 1f) return maxScrollIndex
            return (progress * maxScrollIndex).toInt().coerceIn(0, maxScrollIndex)
        }

        fun updateProgressFromTouch(touchY: Float, grabOffset: Float) {
            val stats = getScrollStats()
            val scrollableHeight = stats.scrollableHeight

            val targetHandleTop = touchY - grabOffset
            val newProgress = (targetHandleTop / scrollableHeight).coerceIn(0f, 1f)

            dragProgress = newProgress
            pendingScrollIndex = resolveDragTargetIndex(
                progress = newProgress,
                maxScrollIndex = stats.maxScrollIndex,
                totalItemsCount = stats.totalItemsCount
            )
        }

        LaunchedEffect(listState, gridState) {
            snapshotFlow { pendingScrollIndex }
                .distinctUntilChanged()
                .collectLatest { index ->
                    if (index >= 0) {
                        listState?.scrollToItem(index)
                        gridState?.scrollToItem(index)
                    }
                }
        }

        LaunchedEffect(listState, gridState, constraintsMaxHeight, minHeight, isDragging) {
            if (isDragging) return@LaunchedEffect

            snapshotFlow { getScrollStats() }
                .distinctUntilChanged()
                .collectLatest { stats ->
                    val targetProgress = stats.progress
                    if (!hasSyncedDisplayedProgress) {
                        displayedProgress.snapTo(targetProgress)
                        hasSyncedDisplayedProgress = true
                    } else {
                        val sourceIsScrolling = listState?.isScrollInProgress == true || gridState?.isScrollInProgress == true
                        val handleDeltaPx = abs(targetProgress - displayedProgress.value) * stats.scrollableHeight
                        val estimatedStepPx = stats.scrollableHeight / stats.maxScrollIndex.coerceAtLeast(1).toFloat()
                        val shouldSmoothJump = !sourceIsScrolling && estimatedStepPx >= coarseJumpThresholdPx && handleDeltaPx >= smoothJumpMinDistancePx

                        if (sourceIsScrolling) {
                            displayedProgress.snapTo(targetProgress)
                        } else if (shouldSmoothJump) {
                            displayedProgress.animateTo(
                                targetValue = targetProgress,
                                animationSpec = tween(durationMillis = MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedDecelerateEasing)
                            )
                        } else {
                            displayedProgress.snapTo(targetProgress)
                        }
                    }
                }
        }

        LaunchedEffect(isDragging, dragProgress) {
            if (isDragging && dragProgress >= 0f) {
                displayedProgress.snapTo(dragProgress)
                hasSyncedDisplayedProgress = true
            }
        }

        val dragLabelTargetIndex = when {
            pendingScrollIndex >= 0 -> pendingScrollIndex
            listState != null -> listState.firstVisibleItemIndex
            gridState != null -> gridState.firstVisibleItemIndex
            else -> -1
        }
        val activeDragLabel =
            if (isDragging && dragLabelProvider != null && dragLabelTargetIndex >= 0) {
                dragLabelProvider(dragLabelTargetIndex)
            } else {
                null
            }
        val showDragLabel = isDragging && !activeDragLabel.isNullOrBlank()

        LaunchedEffect(activeDragLabel) {
            if (!activeDragLabel.isNullOrBlank()) {
                retainedDragLabel = activeDragLabel
            }
        }

        val dragLabelAlpha by animateFloatAsState(
            targetValue = if (showDragLabel) 1f else 0f,
            animationSpec = tween(durationMillis = MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedEasing),
            label = "DragLabelAlpha"
        )
        val dragLabelScale by animateFloatAsState(
            targetValue = if (showDragLabel) 1f else 0.82f,
            animationSpec = tween(durationMillis = MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedEasing),
            label = "DragLabelScale"
        )
        val dragLabelSlide by animateDpAsState(
            targetValue = if (showDragLabel) 0.dp else 8.dp,
            animationSpec = tween(durationMillis = MotionTokens.Durations.Short, easing = MotionTokens.EmphasizedEasing),
            label = "DragLabelSlide"
        )

        val indicatorPath = remember { Path() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    var grabOffset = 0f

                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true

                            val stats = getScrollStats()
                            val scrollableHeight = stats.scrollableHeight
                            val handleHeightPx = with(density) { minHeight.toPx() }

                            val visualProgress = displayedProgress.value
                            val handleY = visualProgress * scrollableHeight

                            val isTouchOnHandle = offset.y >= handleY && offset.y <= (handleY + handleHeightPx)

                            if (isTouchOnHandle) {
                                grabOffset = offset.y - handleY
                                dragProgress = visualProgress
                                pendingScrollIndex =
                                    listState?.firstVisibleItemIndex
                                        ?: gridState?.firstVisibleItemIndex
                                        ?: 0
                            } else {
                                grabOffset = handleHeightPx / 2f
                                updateProgressFromTouch(offset.y, grabOffset)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        },
                        onDragCancel = {
                            isDragging = false
                            dragProgress = -1f
                            pendingScrollIndex = -1
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            updateProgressFromTouch(change.position.y, grabOffset)
                        }
                    )
                }
        ) {
            val rightAnchorX = with(density) { (constraintsMaxWidth - paddingEnd).toPx() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrollbarAlpha }
            ) {
                val visualProgress = displayedProgress.value
                val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else visualProgress
                val handleY = displayProgress * scrollableHeight
                val handleHeightPx = animatedHeight.toPx()

                val indicatorWidthPx = animatedWidth.toPx()
                val leftCornerRadius = 8.dp.toPx()

                val currentIndicatorX = rightAnchorX - indicatorWidthPx

                // Draw thumb: pill shape with rounded left corners and flat right edge
                indicatorPath.reset()
                indicatorPath.addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(currentIndicatorX, handleY),
                            size = Size(indicatorWidthPx, handleHeightPx)
                        ),
                        topLeft = CornerRadius(leftCornerRadius, leftCornerRadius),
                        topRight = CornerRadius.Zero,
                        bottomRight = CornerRadius.Zero,
                        bottomLeft = CornerRadius(leftCornerRadius, leftCornerRadius)
                    )
                )
                drawPath(
                    path = indicatorPath,
                    color = animatedColor
                )
            }
            
            if (scrollbarAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .offset {
                            val visualProgress = displayedProgress.value
                            val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else visualProgress
                            val handleY = displayProgress * scrollableHeight
                            val handleHeightPx = with(density) { animatedHeight.toPx() }
                            
                            val iconSizePx = with(density) { iconSize.toPx() }
                            val paddingEndPx = with(density) { paddingEnd.toPx() }
                            val animatedWidthPx = with(density) { animatedWidth.toPx() }
                            val maxWidthPx = with(density) { constraintsMaxWidth.toPx() }
                            
                            val x = maxWidthPx - paddingEndPx - (animatedWidthPx / 2) - (iconSizePx / 2)
                            val y = handleY + (handleHeightPx / 2) - (iconSizePx / 2)
                            
                            androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt())
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

            val displayedDragLabel = activeDragLabel ?: retainedDragLabel
            if (dragLabelAlpha > 0f && !displayedDragLabel.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .offset {
                            val visualProgress = displayedProgress.value
                            val displayProgress = if (isDragging && dragProgress >= 0f) dragProgress else visualProgress
                            val handleY = displayProgress * scrollableHeight
                            val handleHeightPx = with(density) { minHeight.toPx() }
                            val dragLabelGapPx = with(density) { dragLabelGap.toPx() }
                            val dragLabelSlidePx = with(density) { dragLabelSlide.toPx() }
                            val paddingEndPx = with(density) { paddingEnd.toPx() }
                            val animatedWidthPx = with(density) { animatedWidth.toPx() }
                            val maxWidthPx = with(density) { constraintsMaxWidth.toPx() }

                            val indicatorX = maxWidthPx - paddingEndPx - animatedWidthPx
                            val x = indicatorX - dragLabelGapPx - dragLabelSlidePx
                            val y = handleY + (handleHeightPx / 2f)

                            androidx.compose.ui.unit.IntOffset(x.toInt(), y.toInt())
                        }
                        .graphicsLayer {
                            alpha = dragLabelAlpha
                            scaleX = dragLabelScale
                            scaleY = dragLabelScale
                            translationX = -size.width.toFloat()
                            translationY = -size.height.toFloat() / 2f
                        },
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = displayedDragLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
