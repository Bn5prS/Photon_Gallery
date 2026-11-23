@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.inferno.gallery.R
import com.inferno.gallery.data.PhotoStack
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull

/**
 * Lightweight, non-intrusive Photo Stack scrubber displayed inside DetailScreen when viewing
 * any shot belonging to a burst or similar-photo stack.
 *
 * Uses a bare, container-less floating filmstrip paired with a single compact, content-hugging
 * translucent pill so the full-screen photograph remains unobstructed.
 */
@Composable
fun PhotoStackViewerStrip(
    stack: PhotoStack,
    members: List<GalleryItem>,
    activeMediaId: Long,
    onSelectMember: (GalleryItem) -> Unit,
    onSetBestShot: (GalleryItem) -> Unit,
    onKeepBestOnly: (List<GalleryItem>) -> Unit,
    onUnstack: () -> Unit,
    onScrollInProgress: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (members.size < 2) return

    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    var showStackMenu by remember { mutableStateOf(false) }

    val activeIndex = remember(members, activeMediaId) {
        members.indexOfFirst { it.id.toLongOrNull() == activeMediaId }.coerceAtLeast(0)
    }
    val activeItem = members[activeIndex]
    val isViewingBest = activeMediaId == stack.bestShotId

    LaunchedEffect(listState.isScrollInProgress) {
        onScrollInProgress(listState.isScrollInProgress)
    }

    LaunchedEffect(activeIndex, members.size) {
        if (activeIndex in members.indices) {
            listState.animateScrollToItem(index = (activeIndex - 1).coerceAtLeast(0))
        }
    }

    val otherMembers = remember(members, stack.bestShotId) {
        members.filter { it.id.toLongOrNull() != stack.bestShotId }
    }
    val reclaimableBytes = remember(otherMembers) {
        otherMembers.sumOf { it.size.coerceAtLeast(0L) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Ultra-compact, content-hugging translucent pill — no full-width box
        Box {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.80f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .height(30.dp)
                        .padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Burst icon + shot counter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_burst_mode),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${activeIndex + 1}/${members.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Contextual Best Shot status or one-tap promote button
                    AnimatedContent(
                        targetState = isViewingBest,
                        transitionSpec = { fadeIn(MotionTokens.fastEffectsSpec()) togetherWith fadeOut(MotionTokens.fastEffectsSpec()) },
                        label = "stackBestStatus"
                    ) { viewingBest ->
                        if (viewingBest) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star_filled),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.5.dp)
                                )
                                Text(
                                    text = stringResource(R.string.stack_best_shot_cover),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSetBestShot(activeItem)
                                },
                                shape = ShapeFull,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star_filled),
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.stack_set_best_shot),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Subtle inline overflow icon for Keep best only / Unstack
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { showStackMenu = true }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_more_horiz),
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = showStackMenu,
                onDismissRequest = { showStackMenu = false },
                shape = MaterialTheme.shapes.large,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                if (otherMembers.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            val sizeLabel = formatStackSize(reclaimableBytes)
                            Text(
                                if (sizeLabel != null) {
                                    "${stringResource(R.string.stack_keep_best_only)} ($sizeLabel)"
                                } else {
                                    stringResource(R.string.stack_keep_best_only)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_cleaning_services),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showStackMenu = false
                            onKeepBestOnly(otherMembers)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.stack_unstack)) },
                    leadingIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_view_carousel),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showStackMenu = false
                        onUnstack()
                    }
                )
            }
        }

        // Bare, container-less floating horizontal filmstrip
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            itemsIndexed(
                items = members,
                key = { _, item -> "stack_thumb_${item.id}" }
            ) { _, member ->
                val memberIdLong = member.id.toLongOrNull()
                val isSelected = memberIdLong == activeMediaId
                val isCover = memberIdLong == stack.bestShotId

                val thumbWidth by animateDpAsState(
                    targetValue = if (isSelected) 42.dp else 34.dp,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "stackThumbWidth"
                )
                val thumbHeight by animateDpAsState(
                    targetValue = if (isSelected) 46.dp else 38.dp,
                    animationSpec = MotionTokens.snappySpring(),
                    label = "stackThumbHeight"
                )
                val thumbAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.78f,
                    animationSpec = MotionTokens.fastEffectsSpec(),
                    label = "stackThumbAlpha"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.32f)
                    },
                    animationSpec = MotionTokens.fastEffectsSpec(),
                    label = "stackThumbBorder"
                )
                val shape = RoundedCornerShape(if (isSelected) 9.dp else 7.dp)

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    shadowElevation = if (isSelected) 4.dp else 1.5.dp,
                    border = BorderStroke(if (isSelected) 2.dp else 0.75.dp, borderColor),
                    modifier = Modifier
                        .size(width = thumbWidth, height = thumbHeight)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectMember(member)
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = member.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(thumbAlpha)
                        )

                        if (isCover) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(2.dp)
                                    .size(13.dp)
                                    .background(Color(0xC8121216), CircleShape)
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star_filled),
                                    contentDescription = stringResource(R.string.stack_best_shot_cover),
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(8.5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatStackSize(bytes: Long): String? {
    if (bytes <= 0L) return null
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
    else "${(bytes / 1024L).coerceAtLeast(1L)} KB"
}

/**
 * Compact floating Material 3 Expressive undo pill displayed briefly in DetailScreen right after
 * unstacking a photo stack, letting the user immediately snap the stack back together in one tap.
 */
@Composable
fun PhotoStackUnstackedUndoPill(
    onRestack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_burst_mode),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.stack_unstacked_toast),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRestack()
                },
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = stringResource(R.string.stack_restack_action),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
