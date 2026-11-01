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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.R
import com.inferno.gallery.data.MemoryDayGroup
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeLargeIncreased
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.pressScale

/**
 * Material 3 Expressive Spotlight Hero Card for "On This Day" memories.
 *
 * Designed to be non-intrusive:
 * - Single compact card that scrolls naturally with the feed.
 * - Layered swipeable deck when multiple years are available.
 * - Smooth dismiss with quick-collapse into an ambient timeline banner pill.
 * - Smooth animated size transition preventing any layout snapping.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemoriesSpotlightCard(
    memoryGroups: List<MemoryDayGroup>,
    isDismissed: Boolean,
    onOpenMemory: (MemoryDayGroup) -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
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
            .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S)
    ) { dismissed ->
        if (!dismissed) {
            val pagerState = rememberPagerState(pageCount = { memoryGroups.size })

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(0.dp),
                pageSpacing = SpacingTokens.S,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp)
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
                    shape = ShapeLargeIncreased,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .pressScale(pressedScale = 0.98f)
                        .clip(ShapeLargeIncreased)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenMemory(group)
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Hero Image
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = group.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Directional cinematic gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.40f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )

                        // Top Row: Minimal story bars (if multi) & Dismiss Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = SpacingTokens.M, end = SpacingTokens.S, top = SpacingTokens.S),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (memoryGroups.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = SpacingTokens.S),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    memoryGroups.forEachIndexed { index, _ ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.dp)
                                                .clip(ShapeFull)
                                                .background(
                                                    if (index == page) Color.White
                                                    else if (index < page) Color.White.copy(alpha = 0.65f)
                                                    else Color.White.copy(alpha = 0.25f)
                                                )
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }

                            // Dismiss Button (collapses into ambient timeline banner)
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.35f),
                                    contentColor = Color.White,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                            contentDescription = "Hide memories to banner",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Row: Clean Title & Date (entire card is clickable)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.M),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = group.formattedDate,
                                style = MaterialTheme.typography.labelMedium.copy(
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
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
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
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
