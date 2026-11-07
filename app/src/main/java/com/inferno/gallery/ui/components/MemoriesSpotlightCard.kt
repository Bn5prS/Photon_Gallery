package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Material 3 Memories Spotlight supporting two clean layout styles:
 * 1. STORIES: Space-saving vertical story cards (~116.dp x 160.dp) in a LazyRow.
 * 2. SPOTLIGHT: Wide hero card (~80% width) with adjacent memories peeking in.
 *
 * Design Refinements:
 * - No heavy solid blocks covering the photos.
 * - Clean, non-cluttered typography directly over a soft bottom gradient scrim.
 * - Zero artificial outlines or drop shadows (shadowElevation = 0.dp, tonalElevation = 0.dp).
 * - Smooth collapse/restore animation into ambient timeline banner.
 */
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
// 1. STYLE: Compact Story Reels (LazyRow of vertical cards with soft bottom scrim)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CompactStoryReelsContent(
    memoryGroups: List<MemoryDayGroup>,
    onOpenMemory: (MemoryDayGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    LazyRow(
        contentPadding = PaddingValues(horizontal = SpacingTokens.M),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(
            items = memoryGroups,
            key = { it.key }
        ) { group ->
            CompactMemoryReelCard(
                group = group,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenMemory(group)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CompactMemoryReelCard(
    group: MemoryDayGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageRequest = remember(group.coverItem.uri) {
        ImageRequest.Builder(context)
            .data(if (group.coverItem.localExists) group.coverItem.resolvedUri else group.coverItem.uri)
            .size(400, 600)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .width(116.dp)
            .height(160.dp)
            .pressScale(pressedScale = 0.96f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = imageRequest,
                contentDescription = group.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Soft bottom gradient scrim (no heavy solid block)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.72f)
                            )
                        )
                    )
            )

            // Clean typography directly over scrim
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${group.items.size} photos",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.White.copy(alpha = 0.82f),
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
                modifier = Modifier
                    .fillMaxSize()
                    .pressScale(pressedScale = 0.98f)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenMemory(group)
                    }
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

        // Tonal Indicator Dots if multi-page
        if (memoryGroups.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                memoryGroups.forEachIndexed { index, _ ->
                    val isSelected = pagerState.currentPage == index
                    Surface(
                        shape = ShapeFull,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (isSelected) 18.dp else 6.dp, height = 6.dp)
                    ) {}
                }
            }
        }
    }
}
