package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.R
import com.inferno.gallery.data.MemoriesCarouselStyle
import com.inferno.gallery.data.MemoryDayGroup
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Material 3 Memories Spotlight supporting two clean layout styles:
 * 1. STORIES: Tall Google Photos–style portrait story cards (~2 full cards + peek) in a snapped LazyRow with section indicator pills.
 * 2. SPOTLIGHT: Wide hero card (~80% width, 196.dp height) with adjacent memories peeking in and section indicator pills.
 *
 * Design Refinements:
 * - No heavy solid blocks covering the photos.
 * - Clean typography directly over a soft bottom gradient scrim.
 * - Zero artificial outlines or drop shadows (shadowElevation = 0.dp, tonalElevation = 0.dp).
 * - Smooth collapse/restore animation into ambient timeline banner.
 */

internal fun computeStoryCardWidthDp(screenWidthDp: Float): Float {
    return ((screenWidthDp - 42f) / 2.15f).coerceIn(148f, 188f)
}

internal fun computeStoryCardHeightDp(cardWidthDp: Float): Float {
    return (cardWidthDp * 1.35f).coerceIn(220f, 244f)
}

internal fun computeStoriesIndicatorIndex(
    totalItems: Int,
    firstVisibleIndex: Int,
    scrolledFromStartPx: Int,
    itemSizePx: Int,
    itemSpacingPx: Int,
    beforeContentPaddingPx: Int,
    afterContentPaddingPx: Int,
    viewportWidthPx: Int,
    canScrollForward: Boolean
): Int {
    if (totalItems <= 1) return 0
    if (!canScrollForward) {
        return if (firstVisibleIndex == 0 && scrolledFromStartPx <= 0) 0 else totalItems - 1
    }
    if (itemSizePx <= 0 || viewportWidthPx <= 0) {
        return firstVisibleIndex.coerceIn(0, totalItems - 1)
    }
    val stepPx = (itemSizePx + itemSpacingPx).coerceAtLeast(1)
    val totalContentPx = beforeContentPaddingPx +
        (totalItems * itemSizePx) +
        ((totalItems - 1) * itemSpacingPx) +
        afterContentPaddingPx
    val maxScrollPx = (totalContentPx - viewportWidthPx).coerceAtLeast(1)
    val currentScrollPx = (firstVisibleIndex * stepPx + scrolledFromStartPx).coerceIn(0, maxScrollPx)
    val ratio = (currentScrollPx.toFloat() / maxScrollPx.toFloat()).coerceIn(0f, 1f)
    return (ratio * (totalItems - 1)).roundToInt().coerceIn(0, totalItems - 1)
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemoriesSpotlightCard(
    memoryGroups: List<MemoryDayGroup>,
    isDismissed: Boolean = false,
    onOpenMemory: (MemoryDayGroup) -> Unit,
    onDismiss: () -> Unit = {},
    onRestore: () -> Unit = {},
    modifier: Modifier = Modifier,
    style: MemoriesCarouselStyle = MemoriesCarouselStyle.SPOTLIGHT
) {
    if (memoryGroups.isEmpty()) return

    val haptic = LocalHapticFeedback.current

    AnimatedContent(
        targetState = isDismissed,
        transitionSpec = {
            (fadeIn(animationSpec = MotionTokens.fastEffectsSpec()) +
                scaleIn(animationSpec = MotionTokens.gentleSpring(), initialScale = 0.96f))
                .togetherWith(
                    fadeOut(animationSpec = MotionTokens.fastEffectsSpec()) +
                        scaleOut(animationSpec = MotionTokens.gentleSpring(), targetScale = 0.96f)
                ).using(
                    SizeTransform(clip = true) { _, _ -> MotionTokens.gentleSpring<IntSize>() }
                )
        },
        label = "MemoriesSpotlightTransition",
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingTokens.S)
    ) { dismissed ->
        if (!dismissed) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(SpacingTokens.S)
            ) {
                // Section Header: Clean "Memories" typography matching timeline date headers
                Text(
                    text = "Memories",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = SpacingTokens.M)
                )

                // Render selected style
                when (style) {
                    MemoriesCarouselStyle.STORIES -> {
                        CompactStoryReelsContent(
                            memoryGroups = memoryGroups,
                            onOpenMemory = onOpenMemory
                        )
                    }
                    MemoriesCarouselStyle.SPOTLIGHT -> {
                        MultiBrowsePeekContent(
                            memoryGroups = memoryGroups,
                            onOpenMemory = onOpenMemory
                        )
                    }
                }
            }
        } else {
            // Ambient Collapsed Memories Banner Pill
            val firstGroup = memoryGroups.first()
            val bannerText = if (memoryGroups.size > 1) {
                "${firstGroup.title} (+${memoryGroups.size - 1})"
            } else {
                firstGroup.title
            }

            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingTokens.M)
                    .pressScale(pressedScale = 0.98f)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRestore()
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = ShapeFull,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.primary,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_history),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = ShapeFull,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .pressScale(pressedScale = 0.92f)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRestore()
                            }
                    ) {
                        Text(
                            text = "Show",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. STYLE: Portrait Story Reels (Snapped LazyRow of tall portrait cards + centered text + indicators)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CompactStoryReelsContent(
    memoryGroups: List<MemoryDayGroup>,
    onOpenMemory: (MemoryDayGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Start)
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val cardWidth: Dp = remember(configuration.screenWidthDp) {
        computeStoryCardWidthDp(configuration.screenWidthDp.toFloat()).dp
    }
    val cardHeight: Dp = remember(cardWidth) {
        computeStoryCardHeightDp(cardWidth.value).dp
    }

    val activeIndicatorIndex by remember(memoryGroups.size, listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstInfo = layoutInfo.visibleItemsInfo.firstOrNull()
            computeStoriesIndicatorIndex(
                totalItems = memoryGroups.size,
                firstVisibleIndex = firstInfo?.index ?: listState.firstVisibleItemIndex,
                scrolledFromStartPx = if (firstInfo != null) -firstInfo.offset else listState.firstVisibleItemScrollOffset,
                itemSizePx = firstInfo?.size ?: 0,
                itemSpacingPx = layoutInfo.mainAxisItemSpacing,
                beforeContentPaddingPx = layoutInfo.beforeContentPadding,
                afterContentPaddingPx = layoutInfo.afterContentPadding,
                viewportWidthPx = layoutInfo.viewportSize.width,
                canScrollForward = listState.canScrollForward
            )
        }
    }

    val isScrollable by remember(listState) {
        derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            state = listState,
            flingBehavior = snapFlingBehavior,
            contentPadding = PaddingValues(horizontal = SpacingTokens.M),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(
                items = memoryGroups,
                key = { it.key }
            ) { group ->
                CompactMemoryReelCard(
                    group = group,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenMemory(group)
                    }
                )
            }
        }

        if (memoryGroups.size > 1 && isScrollable) {
            CarouselPageIndicators(
                count = memoryGroups.size,
                activeIndex = activeIndicatorIndex,
                onSelectIndex = { targetIndex ->
                    coroutineScope.launch {
                        listState.animateScrollToItem(targetIndex)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompactMemoryReelCard(
    group: MemoryDayGroup,
    cardWidth: Dp,
    cardHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(group.coverItem.uri) {
        ImageRequest.Builder(context)
            .data(if (group.coverItem.localExists) group.coverItem.resolvedUri else group.coverItem.uri)
            .size(640, 900)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .pressScale(pressedScale = 0.96f)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = group.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Soft bottom gradient scrim for clean editorial text legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            // Centered editorial typography directly over scrim
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.items.size} photos",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. STYLE: Multi-Browse Peek Deck (Spotlight with soft bottom scrim)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MultiBrowsePeekContent(
    memoryGroups: List<MemoryDayGroup>,
    onOpenMemory: (MemoryDayGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val pagerState = rememberPagerState(pageCount = { memoryGroups.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
        ) { page ->
            val group = memoryGroups.getOrNull(page) ?: return@HorizontalPager
            SpotlightMemoryCard(
                group = group,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenMemory(group)
                }
            )
        }

        // Tonal Indicator Dots if multi-page
        if (memoryGroups.size > 1) {
            CarouselPageIndicators(
                count = memoryGroups.size,
                activeIndex = pagerState.currentPage,
                onSelectIndex = { page ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
    }
}

@Composable
private fun CarouselPageIndicators(
    count: Int,
    activeIndex: Int,
    onSelectIndex: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until count) {
            val isSelected = activeIndex == index
            val pillWidth by animateDpAsState(
                targetValue = if (isSelected) 18.dp else 6.dp,
                animationSpec = MotionTokens.snappySpring(),
                label = "carouselIndicatorWidth"
            )
            val pillColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                animationSpec = MotionTokens.fastEffectsSpec(),
                label = "carouselIndicatorColor"
            )
            Box(
                modifier = Modifier
                    .clip(ShapeFull)
                    .then(
                        if (onSelectIndex != null) {
                            Modifier.clickable { onSelectIndex(index) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 3.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = ShapeFull,
                    color = pillColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.size(width = pillWidth, height = 6.dp)
                ) {}
            }
        }
    }
}

@Composable
private fun SpotlightMemoryCard(
    group: MemoryDayGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(group.coverItem.uri) {
        ImageRequest.Builder(context)
            .data(if (group.coverItem.localExists) group.coverItem.resolvedUri else group.coverItem.uri)
            .size(800, 600)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxSize()
            .pressScale(pressedScale = 0.98f)
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = group.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Soft bottom gradient scrim (no heavy solid block, no play button badge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Clean typography directly over scrim
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.formattedDate} • ${group.items.size} photos",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
