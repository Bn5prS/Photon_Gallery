@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.inferno.gallery.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import com.inferno.gallery.R
import com.inferno.gallery.data.PhotoStack
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import java.util.Locale

/**
 * Lightweight, non-intrusive Photo Stack scrubber displayed inside DetailScreen when viewing
 * any shot belonging to a burst or similar-photo stack.
 *
 * Uses a bare, container-less horizontal strip of crisp small-radius mini-thumbnails paired
 * with a matched menu tile right beside the far-right thumbnail that opens a single-layer M3
 * pop-up action card anchored directly above it.
 */
@Composable
fun PhotoStackViewerStrip(
    stack: PhotoStack,
    members: List<GalleryItem>,
    activeMediaId: Long,
    onSelectMember: (GalleryItem) -> Unit,
    onSetBestShot: (GalleryItem) -> Unit,
    onRemoveFromStack: (GalleryItem) -> Unit,
    onKeepThisDeleteRest: (keptItem: GalleryItem, extrasToDelete: List<GalleryItem>) -> Unit,
    onUnstack: () -> Unit,
    onScrollInProgress: (Boolean) -> Unit = {},
    onMenuOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (members.size < 2) return

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val platformLocale = LocalLocale.current.platformLocale
    val listState = rememberLazyListState()
    var showStackMenu by remember(stack.stackId) { mutableStateOf(false) }

    LaunchedEffect(showStackMenu) {
        onMenuOpenChange(showStackMenu)
    }
    DisposableEffect(Unit) {
        onDispose { onMenuOpenChange(false) }
    }

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

    val otherFromActive = remember(members, activeMediaId) {
        members.filter { it.id.toLongOrNull() != activeMediaId }
    }
    val reclaimableBytes = remember(otherFromActive) {
        otherFromActive.sumOf { it.size.coerceAtLeast(0L) }
    }
    val reclaimableLabel = remember(reclaimableBytes, platformLocale) {
        formatStackSize(reclaimableBytes, platformLocale)
    }

    val popupPositionProvider = remember(density) {
        val verticalGapPx = with(density) { 10.dp.roundToPx() }
        val edgeMarginPx = with(density) { 16.dp.roundToPx() }
        AboveEndPopupPositionProvider(verticalGapPx = verticalGapPx, edgeMarginPx = edgeMarginPx)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Hugged horizontal group so the menu button always sits directly next to the far-right thumbnail.
        Row(
            modifier = Modifier.widthIn(max = 380.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(46.dp)
            ) {
                itemsIndexed(
                    items = members,
                    key = { _, item -> "stack_thumb_${item.id}" }
                ) { _, member ->
                    val memberIdLong = member.id.toLongOrNull()
                    val isSelected = memberIdLong == activeMediaId
                    val isCover = memberIdLong == stack.bestShotId

                    val thumbWidth by animateDpAsState(
                        targetValue = if (isSelected) 40.dp else 29.dp,
                        animationSpec = MotionTokens.snappySpring(),
                        label = "stackThumbWidth"
                    )
                    val thumbHeight by animateDpAsState(
                        targetValue = if (isSelected) 44.dp else 38.dp,
                        animationSpec = MotionTokens.snappySpring(),
                        label = "stackThumbHeight"
                    )
                    val thumbAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.85f,
                        animationSpec = MotionTokens.fastEffectsSpec(),
                        label = "stackThumbAlpha"
                    )

                    val borderColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.White.copy(alpha = 0.22f)
                    }
                    val shape = RoundedCornerShape(4.dp)

                    Surface(
                        shape = shape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = if (isSelected) 3.dp else 1.dp,
                        border = BorderStroke(if (isSelected) 2.dp else 0.8.dp, borderColor),
                        modifier = Modifier
                            .padding(horizontal = if (isSelected) 2.dp else 0.dp)
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
                                        .background(Color(0xB8101014), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_star),
                                        contentDescription = stringResource(R.string.stack_best_shot_cover),
                                        tint = Color.White.copy(alpha = 0.92f),
                                        modifier = Modifier.size(8.5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Matched menu tile directly beside the rightmost thumbnail + anchored single-layer pop-up card
            Box {
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showStackMenu = !showStackMenu
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (showStackMenu) {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    border = null,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(width = 34.dp, height = 38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (showStackMenu) R.drawable.ic_ms_close else R.drawable.ic_ms_more_vert
                            ),
                            contentDescription = stringResource(R.string.cd_more_options),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showStackMenu) {
                    Popup(
                        popupPositionProvider = popupPositionProvider,
                        onDismissRequest = { showStackMenu = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            border = BorderStroke(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)
                            ),
                            tonalElevation = 0.dp,
                            shadowElevation = 10.dp,
                            modifier = Modifier.width(276.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                // Quiet header directly on the single card surface
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                                    ) {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_burst_mode),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.stack_popup_title),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${activeIndex + 1}/${members.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )

                                // 1. Keep as best shot (monochrome outlined star)
                                StackPopupMenuRow(
                                    iconRes = R.drawable.ic_ms_star,
                                    label = stringResource(R.string.stack_keep_as_best_shot),
                                    trailingLabel = if (isViewingBest) {
                                        stringResource(R.string.stack_current_best_badge)
                                    } else {
                                        null
                                    },
                                    enabled = !isViewingBest,
                                    onClick = {
                                        showStackMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSetBestShot(activeItem)
                                    }
                                )

                                // 2. Remove selected image from stack
                                StackPopupMenuRow(
                                    iconRes = R.drawable.ic_ms_stack_detach,
                                    label = stringResource(R.string.stack_remove_from_stack),
                                    onClick = {
                                        showStackMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onRemoveFromStack(activeItem)
                                    }
                                )

                                // 3. Unstack all
                                StackPopupMenuRow(
                                    iconRes = R.drawable.ic_ms_view_carousel,
                                    label = stringResource(R.string.stack_unstack_all),
                                    trailingLabel = "${members.size}",
                                    onClick = {
                                        showStackMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUnstack()
                                    }
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )

                                // 4. Keep this, delete rest (destructive)
                                StackPopupMenuRow(
                                    iconRes = R.drawable.ic_ms_delete,
                                    label = stringResource(R.string.stack_keep_this_delete_rest),
                                    trailingLabel = reclaimableLabel?.let { "-$it" },
                                    isDestructive = true,
                                    enabled = otherFromActive.isNotEmpty(),
                                    onClick = {
                                        showStackMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onKeepThisDeleteRest(activeItem, otherFromActive)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StackPopupMenuRow(
    @DrawableRes iconRes: Int,
    label: String,
    trailingLabel: String? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentTint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconTint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val trailingTint = if (isDestructive) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.52f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isDestructive) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = contentTint,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingLabel != null) {
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelSmall,
                color = trailingTint,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

private class AboveEndPopupPositionProvider(
    private val verticalGapPx: Int,
    private val edgeMarginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val isCompactCenteredAnchor = anchorBounds.right < (windowSize.width * 0.78f).toInt()
        val desiredX = if (isCompactCenteredAnchor) {
            (windowSize.width - popupContentSize.width) / 2
        } else if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        val maxX = (windowSize.width - popupContentSize.width - edgeMarginPx).coerceAtLeast(edgeMarginPx)
        val x = desiredX.coerceIn(edgeMarginPx, maxX)

        val aboveY = anchorBounds.top - popupContentSize.height - verticalGapPx
        val y = if (aboveY >= edgeMarginPx) {
            aboveY
        } else {
            (anchorBounds.bottom + verticalGapPx)
                .coerceAtMost((windowSize.height - popupContentSize.height - edgeMarginPx).coerceAtLeast(edgeMarginPx))
        }
        return IntOffset(x, y)
    }
}

private fun formatStackSize(bytes: Long, locale: Locale): String? {
    if (bytes <= 0L) return null
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(locale, "%.1f MB", mb)
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
