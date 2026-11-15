package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.utils.pressScale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.NavDockLabelStyle
import com.inferno.gallery.ui.utils.tick
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import com.inferno.gallery.R

/**
 * Expressive floating navigation dock with auto-widening stadium slots,
 * a smooth sliding capsule indicator, optional compact stacked labels,
 * and a round satellite circular search pill.
 */
@Composable
fun FloatingNavigationPill(
    currentRoute: String?,
    onNavigateToPhotos: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSearch: () -> Unit,
    showVideosTab: Boolean = false,
    showLabels: Boolean = false,
    onNavigateToVideos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedIndex = when {
        currentRoute == "photos" -> 0
        showVideosTab && currentRoute == "videos" -> 1
        currentRoute?.startsWith("album") == true || currentRoute == "albums" -> if (showVideosTab) 2 else 1
        currentRoute == "collections" -> if (showVideosTab) 3 else 2
        else -> -1
    }

    val isSearchSelected = currentRoute == "search"

    val dockHeight by animateDpAsState(
        targetValue = if (showLabels) 62.dp else 60.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillDockHeight"
    )
    val slotHeight by animateDpAsState(
        targetValue = if (showLabels) 50.dp else 48.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillSlotHeight"
    )
    val slotGap by animateDpAsState(
        targetValue = if (showLabels) 4.dp else 5.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillSlotGap"
    )
    val slotWidth by animateDpAsState(
        targetValue = when {
            showLabels && showVideosTab -> 60.dp
            showLabels -> 70.dp
            showVideosTab -> 56.dp
            else -> 70.dp
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "pillSlotWidth"
    )
    val indicatorOffsetX by animateDpAsState(
        targetValue = if (selectedIndex >= 0) (slotWidth + slotGap) * selectedIndex else 0.dp,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillIndicatorOffsetX"
    )
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "pillIndicatorAlpha"
    )

    Row(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .wrapContentWidth()
            .pointerInput(Unit) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Main Capsule (Photos, Videos, Albums, Collections) ──
        Surface(
            modifier = Modifier.height(dockHeight),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(6.dp)
                    .animateContentSize(
                        animationSpec = MotionTokens.snappySpring()
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                // Sliding full-pill active indicator
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffsetX.roundToPx(), 0) }
                        .size(width = slotWidth, height = slotHeight)
                        .graphicsLayer { alpha = indicatorAlpha }
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                )

                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(slotGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val iconSize = if (showLabels) 19.dp else 23.dp

                    val photosLabel = stringResource(R.string.nav_photos)
                    PillDockSlotItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                                contentDescription = photosLabel,
                                modifier = Modifier.size(iconSize)
                            )
                        },
                        label = photosLabel,
                        isSelected = selectedIndex == 0,
                        showLabel = showLabels,
                        slotWidth = slotWidth,
                        slotHeight = slotHeight,
                        onClick = onNavigateToPhotos
                    )

                    if (showVideosTab) {
                        val videosLabel = stringResource(R.string.album_videos)
                        PillDockSlotItem(
                            icon = {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                                    contentDescription = videosLabel,
                                    modifier = Modifier.size(iconSize)
                                )
                            },
                            label = videosLabel,
                            isSelected = selectedIndex == 1,
                            showLabel = showLabels,
                            slotWidth = slotWidth,
                            slotHeight = slotHeight,
                            onClick = onNavigateToVideos
                        )
                    }

                    val albumsLabel = stringResource(R.string.nav_albums)
                    PillDockSlotItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                                contentDescription = albumsLabel,
                                modifier = Modifier.size(iconSize)
                            )
                        },
                        label = albumsLabel,
                        isSelected = selectedIndex == if (showVideosTab) 2 else 1,
                        showLabel = showLabels,
                        slotWidth = slotWidth,
                        slotHeight = slotHeight,
                        onClick = onNavigateToAlbums
                    )

                    val collectionsLabel = stringResource(R.string.nav_collections)
                    PillDockSlotItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_dashboard),
                                contentDescription = collectionsLabel,
                                modifier = Modifier.size(iconSize)
                            )
                        },
                        label = collectionsLabel,
                        isSelected = selectedIndex == if (showVideosTab) 3 else 2,
                        showLabel = showLabels,
                        slotWidth = slotWidth,
                        slotHeight = slotHeight,
                        onClick = onNavigateToCollections
                    )
                }
            }
        }

        // ── Satellite Circular Search Pill ──
        val searchContainerColor by animateColorAsState(
            targetValue = if (isSearchSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
            animationSpec = MotionTokens.fastEffectsSpec(),
            label = "searchContainerColor"
        )
        val searchContentColor by animateColorAsState(
            targetValue = if (isSearchSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = MotionTokens.fastEffectsSpec(),
            label = "searchContentColor"
        )

        Surface(
            modifier = Modifier.size(dockHeight),
            shape = CircleShape,
            color = searchContainerColor,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            val searchInteractionSource = remember { MutableInteractionSource() }
            val isSearchPressed by searchInteractionSource.collectIsPressedAsState()
            val searchTouchScale by animateFloatAsState(
                targetValue = if (isSearchPressed) 0.93f else 1f,
                animationSpec = MotionTokens.bouncySpring(),
                label = "searchTouchScale"
            )
            val view = LocalView.current
            val searchLabel = stringResource(R.string.nav_search)
            var showSearchTooltip by remember { mutableStateOf(false) }

            @OptIn(ExperimentalFoundationApi::class)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = searchTouchScale
                        scaleY = searchTouchScale
                    }
                    .clip(CircleShape)
                    .combinedClickable(
                        interactionSource = searchInteractionSource,
                        indication = ripple(
                            bounded = true,
                            color = searchContentColor
                        ),
                        onClick = {
                            showSearchTooltip = false
                            view.tick()
                            onNavigateToSearch()
                        },
                        onLongClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            showSearchTooltip = true
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                    contentDescription = searchLabel,
                    tint = searchContentColor,
                    modifier = Modifier.size(24.dp)
                )
                DockTabTooltipPopup(
                    visible = showSearchTooltip,
                    label = searchLabel,
                    onDismiss = { showSearchTooltip = false }
                )
            }
        }
    }
}

/**
 * Sleek, compact full-width navigation dock (DockStyle.FULL_WIDTH / DockStyle.BAR).
 * Provides a refined 64.dp profile (down from the bulky 80.dp M3 default)
 * with proper navigationBars window insets, centered icons & labels without clipping,
 * responsive capsule selection indicator, and tactile haptic feedback.
 */
@Composable
fun FullWidthNavigationBar(
    currentRoute: String?,
    onNavigateToPhotos: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSearch: () -> Unit,
    showVideosTab: Boolean = false,
    onNavigateToVideos: () -> Unit = {},
    modifier: Modifier = Modifier,
    barHeight: Dp = 60.dp
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
            FullWidthNavItem(
                selected = currentRoute == "photos",
                onClick = onNavigateToPhotos,
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                        contentDescription = stringResource(R.string.nav_photos),
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = stringResource(R.string.nav_photos)
            )

            if (showVideosTab) {
                FullWidthNavItem(
                    selected = currentRoute == "videos",
                    onClick = onNavigateToVideos,
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_play_arrow),
                            contentDescription = stringResource(R.string.album_videos),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = stringResource(R.string.album_videos)
                )
            }

            FullWidthNavItem(
                selected = currentRoute?.startsWith("album") == true || currentRoute == "albums",
                onClick = onNavigateToAlbums,
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                        contentDescription = stringResource(R.string.nav_albums),
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = stringResource(R.string.nav_albums)
            )

            FullWidthNavItem(
                selected = currentRoute == "collections",
                onClick = onNavigateToCollections,
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_dashboard),
                        contentDescription = stringResource(R.string.nav_collections),
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = stringResource(R.string.nav_collections)
            )

            FullWidthNavItem(
                selected = currentRoute == "search",
                onClick = onNavigateToSearch,
                icon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                        contentDescription = stringResource(R.string.nav_search),
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = stringResource(R.string.nav_search)
            )
        }
    }
}
}

@Composable
private fun RowScope.FullWidthNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "fullWidthNavItemScale"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "fullWidthNavItemIndicatorColor"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "fullWidthNavItemIconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "fullWidthNavItemTextColor"
    )

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = touchScale
                scaleY = touchScale
            }
            .clip(ShapeFull)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = {
                    view.tick()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(30.dp)
                    .clip(ShapeFull)
                    .background(indicatorColor),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    icon()
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DockTabTooltipPopup(
    visible: Boolean,
    label: String,
    onDismiss: () -> Unit
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(1800L)
            onDismiss()
        }
    }

    if (visible && label.isNotEmpty()) {
        val offsetPx = with(LocalDensity.current) { 46.dp.roundToPx() }
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, -offsetPx),
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = false)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PillDockSlotItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    showLabel: Boolean,
    slotWidth: Dp,
    slotHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    var showTooltip by remember { mutableStateOf(false) }

    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "pillSlotTouchScale"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) (if (showLabel) 1.04f else 1.08f) else 1f,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillSlotIconScale"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "pillSlotContentColor"
    )

    Box(
        modifier = modifier
            .size(width = slotWidth, height = slotHeight)
            .graphicsLayer {
                scaleX = touchScale
                scaleY = touchScale
            }
            .clip(CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = {
                    showTooltip = false
                    view.tick()
                    onClick()
                },
                onLongClick = {
                    if (!showLabel && label.isNotEmpty()) {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        showTooltip = true
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (showLabel) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        icon()
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Box(
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
        }
        DockTabTooltipPopup(
            visible = showTooltip,
            label = label,
            onDismiss = { showTooltip = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconPillTabItem(
    icon: @Composable () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    label: String = "",
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    var showTooltip by remember { mutableStateOf(false) }

    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "iconTabTouchScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = MotionTokens.snappySpring(),
        label = "iconTabIconScale"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "iconTabPillColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "iconTabContentColor"
    )

    Box(
        modifier = modifier
            .size(46.dp)
            .graphicsLayer {
                scaleX = touchScale
                scaleY = touchScale
            }
            .clip(CircleShape)
            .background(pillColor, CircleShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                onClick = {
                    showTooltip = false
                    view.tick()
                    onClick()
                },
                onLongClick = {
                    if (label.isNotEmpty()) {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                        showTooltip = true
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                },
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
        }
        DockTabTooltipPopup(
            visible = showTooltip,
            label = label,
            onDismiss = { showTooltip = false }
        )
    }
}

@Composable
fun DynamicPillTabItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    unselectedHorizontalPadding: Dp = 12.dp,
    selectedHorizontalPadding: Dp = 16.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = MotionTokens.bouncySpring(),
        label = "tabItemTouchScale"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "tabItemPillColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = MotionTokens.fastEffectsSpec(),
        label = "tabItemContentColor"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (isSelected) selectedHorizontalPadding else unselectedHorizontalPadding,
        animationSpec = MotionTokens.snappySpring(),
        label = "tabItemHorizontalPadding"
    )

    val rippleColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = touchScale
                scaleY = touchScale
            }
            .clip(ShapeFull)
            .background(pillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = rippleColor
                ),
                onClick = {
                    view.tick()
                    onClick()
                }
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(
                        animationSpec = MotionTokens.fastEffectsSpec()
                    ) + expandHorizontally(
                        animationSpec = MotionTokens.snappySpring()
                    ),
                    exit = fadeOut(
                        animationSpec = MotionTokens.fastEffectsSpec()
                    ) + shrinkHorizontally(
                        animationSpec = MotionTokens.snappySpring()
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon()
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                }

                Text(
                    text = label,
                    style = NavDockLabelStyle.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = fontSize
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun DockItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    isTertiary: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dockItemTouchScale"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isTertiary) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isTertiary) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "contentColor"
    )

    val rippleColor = if (isSelected) {
        if (isTertiary) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    // Outer box = full-height touch target; inner Row keeps the pill at content size
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = touchScale
                scaleY = touchScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    color = rippleColor
                ),
                onClick = {
                    view.tick()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(ShapeFull)
                .background(pillColor)
                .padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        initialOffsetX = { -it / 4 }
                    ),
                    exit = fadeOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + shrinkHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + slideOutHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        targetOffsetX = { -it / 4 }
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon()
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

fun getTabRouteIndex(route: String?): Int {
    return when (route) {
        "photos" -> 0
        "albums", "album/{bucketName}" -> 1
        "collections" -> 2
        "search" -> 3
        "settings" -> 4
        else -> 0
    }
}

fun getMainTabRouteIndex(route: String?): Int {
    if (route == null) return -1
    val base = route.substringBefore('?').substringBefore('/')
    return when (base) {
        "photos" -> 0
        "videos" -> 1
        "albums" -> 2
        "collections" -> 3
        "search" -> 4
        else -> -1
    }
}

/**
 * Directional Shared-Axis Motion for main bottom-dock navigation.
 * Uses a subtle 12% horizontal slide + scale (0.98f) + spring fade to preserve spatial continuity
 * without the heavy GPU/layout cost of a 100% full-screen slide.
 */
fun getEnterTransition(initialRoute: String?, targetRoute: String?): EnterTransition {
    val fromIdx = getMainTabRouteIndex(initialRoute)
    val toIdx = getMainTabRouteIndex(targetRoute)
    return when {
        fromIdx != -1 && toIdx != -1 && toIdx > fromIdx -> {
            // Forward (e.g. photos -> albums): enter from right with subtle scale & spring fade
            slideInHorizontally(
                animationSpec = MotionTokens.snappySpring(),
                initialOffsetX = { (it * 0.12f).toInt() }
            ) + scaleIn(
                animationSpec = MotionTokens.snappySpring(),
                initialScale = 0.98f
            ) + fadeIn(animationSpec = MotionTokens.snappySpring())
        }
        fromIdx != -1 && toIdx != -1 && toIdx < fromIdx -> {
            // Backward (e.g. albums -> photos): enter from left with subtle scale & spring fade
            slideInHorizontally(
                animationSpec = MotionTokens.snappySpring(),
                initialOffsetX = { -(it * 0.12f).toInt() }
            ) + scaleIn(
                animationSpec = MotionTokens.snappySpring(),
                initialScale = 0.98f
            ) + fadeIn(animationSpec = MotionTokens.snappySpring())
        }
        else -> {
            // Non-tab or same tab: smooth spring fade and gentle scale
            fadeIn(animationSpec = MotionTokens.snappySpring()) + scaleIn(
                animationSpec = MotionTokens.snappySpring(),
                initialScale = 0.98f
            )
        }
    }
}

fun getExitTransition(initialRoute: String?, targetRoute: String?): ExitTransition {
    val fromIdx = getMainTabRouteIndex(initialRoute)
    val toIdx = getMainTabRouteIndex(targetRoute)
    return when {
        fromIdx != -1 && toIdx != -1 && toIdx > fromIdx -> {
            // Forward (e.g. photos -> albums): exit to left
            slideOutHorizontally(
                animationSpec = MotionTokens.snappySpring(),
                targetOffsetX = { -(it * 0.12f).toInt() }
            ) + scaleOut(
                animationSpec = MotionTokens.snappySpring(),
                targetScale = 0.98f
            ) + fadeOut(animationSpec = MotionTokens.snappySpring())
        }
        fromIdx != -1 && toIdx != -1 && toIdx < fromIdx -> {
            // Backward (e.g. albums -> photos): exit to right
            slideOutHorizontally(
                animationSpec = MotionTokens.snappySpring(),
                targetOffsetX = { (it * 0.12f).toInt() }
            ) + scaleOut(
                animationSpec = MotionTokens.snappySpring(),
                targetScale = 0.98f
            ) + fadeOut(animationSpec = MotionTokens.snappySpring())
        }
        else -> {
            fadeOut(animationSpec = MotionTokens.snappySpring()) + scaleOut(
                animationSpec = MotionTokens.snappySpring(),
                targetScale = 0.98f
            )
        }
    }
}

/**
 * Standard Material 3 Expressive transitions for sub-destinations (e.g., Album Detail,
 * Settings, People, Map, Cleaner tools).
 * Provides a tactile forward slide with subtle scale elevation on enter, and a matching
 * symmetric spring retreat on pop back.
 */
fun subScreenEnterTransition(): EnterTransition {
    return slideInHorizontally(
        animationSpec = MotionTokens.snappySpring(),
        initialOffsetX = { (it * 0.25f).toInt() }
    ) + scaleIn(
        animationSpec = MotionTokens.snappySpring(),
        initialScale = 0.94f
    ) + fadeIn(animationSpec = MotionTokens.snappySpring())
}

fun subScreenExitTransition(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = MotionTokens.snappySpring(),
        targetOffsetX = { -(it * 0.12f).toInt() }
    ) + scaleOut(
        animationSpec = MotionTokens.snappySpring(),
        targetScale = 0.96f
    ) + fadeOut(animationSpec = MotionTokens.snappySpring())
}

fun subScreenPopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        animationSpec = MotionTokens.snappySpring(),
        initialOffsetX = { -(it * 0.12f).toInt() }
    ) + scaleIn(
        animationSpec = MotionTokens.snappySpring(),
        initialScale = 0.96f
    ) + fadeIn(animationSpec = MotionTokens.snappySpring())
}

fun subScreenPopExitTransition(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = MotionTokens.snappySpring(),
        targetOffsetX = { (it * 0.25f).toInt() }
    ) + scaleOut(
        animationSpec = MotionTokens.snappySpring(),
        targetScale = 0.94f
    ) + fadeOut(animationSpec = MotionTokens.snappySpring())
}

/**
 * Material 3 Shared Axis (Z-axis) / Container-Transform style transitions for opening
 * and closing an album folder, zooming cleanly into and out of the album grid.
 */
fun albumOpenEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, easing = MotionTokens.EmphasizedDecelerateEasing)
    )
}

fun albumOpenExitTransition(): ExitTransition {
    return scaleOut(
        animationSpec = MotionTokens.sharedElementSpring(),
        targetScale = 0.94f
    ) + fadeOut(
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = MotionTokens.EmphasizedAccelerateEasing)
    )
}

fun albumClosePopEnterTransition(): EnterTransition {
    return scaleIn(
        animationSpec = MotionTokens.sharedElementSpring(),
        initialScale = 0.94f
    ) + fadeIn(
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = MotionTokens.EmphasizedDecelerateEasing)
    )
}

fun albumClosePopExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 150, easing = MotionTokens.EmphasizedAccelerateEasing)
    )
}
