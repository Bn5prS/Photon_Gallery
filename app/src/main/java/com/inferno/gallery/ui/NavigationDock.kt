package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.utils.pressScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.utils.tick
import kotlin.math.roundToInt
import androidx.compose.ui.res.vectorResource
import com.inferno.gallery.R


@Composable
fun QuickFilterRow(
    selectedFilter: Int,
    onFilterSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                CustomFilterChip(
                    text = "All",
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_image),
                    selected = selectedFilter == 0,
                    onClick = { onFilterSelected(0) }
                )
            }
            item {
                CustomFilterChip(
                    text = "Camera",
                    icon = ImageVector.vectorResource(R.drawable.ic_ms_photo_camera),
                    selected = selectedFilter == 1,
                    onClick = { onFilterSelected(1) }
                )
            }
        }
    }
}


@Composable
fun CustomFilterChip(
    text: String,
    icon: ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .height(36.dp)
            .pressScale(pressedScale = 0.95f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Material 3 Expressive Floating Navigation Pill
 * Features a single sliding accented indicator pill that smoothly slides back and forth
 * between the active navigation tabs without bouncy animation.
 */
@Composable
fun FloatingNavigationPill(
    currentRoute: String?,
    onNavigateToPhotos: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = when {
        currentRoute == "photos" -> 0
        currentRoute?.startsWith("album") == true || currentRoute == "albums" -> 1
        currentRoute == "search" -> 2
        else -> 0
    }

    var tabPositions by remember { mutableStateOf(mapOf<Int, Pair<Float, Float>>()) }
    val density = LocalDensity.current

    val currentTabPos = tabPositions[selectedIndex]
    val targetLeft = currentTabPos?.first ?: 0f
    val targetWidth = currentTabPos?.second ?: 0f

    val indicatorLeft by animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillIndicatorLeft"
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = MotionTokens.snappySpring(),
        label = "pillIndicatorWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth(0.80f)
            .height(52.dp)
            .pointerInput(Unit) {},
        shape = ShapeFull,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // ── Single Sliding Accented Indicator Pill ────────────────────
            if (indicatorWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorLeft.roundToInt(), 0) }
                        .width(with(density) { indicatorWidth.toDp() })
                        .fillMaxHeight()
                        .clip(ShapeFull)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )
            }

            // ── Tab Items Row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlidingDockItem(
                    icon = { Icon(ImageVector.vectorResource(R.drawable.ic_ms_image), contentDescription = "Photos", modifier = Modifier.size(22.dp)) },
                    label = "Photos",
                    isSelected = selectedIndex == 0,
                    onPositioned = { left, width ->
                        if (tabPositions[0]?.first != left || tabPositions[0]?.second != width) {
                            tabPositions = tabPositions + (0 to (left to width))
                        }
                    },
                    onClick = onNavigateToPhotos
                )

                SlidingDockItem(
                    icon = {
                        Icon(
                            if (selectedIndex == 1) ImageVector.vectorResource(R.drawable.ic_ms_photo_album) else ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                            contentDescription = "Albums",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "Albums",
                    isSelected = selectedIndex == 1,
                    onPositioned = { left, width ->
                        if (tabPositions[1]?.first != left || tabPositions[1]?.second != width) {
                            tabPositions = tabPositions + (1 to (left to width))
                        }
                    },
                    onClick = onNavigateToAlbums
                )

                SlidingDockItem(
                    icon = { Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search), contentDescription = "Search", modifier = Modifier.size(22.dp)) },
                    label = "Search",
                    isSelected = selectedIndex == 2,
                    onPositioned = { left, width ->
                        if (tabPositions[2]?.first != left || tabPositions[2]?.second != width) {
                            tabPositions = tabPositions + (2 to (left to width))
                        }
                    },
                    onClick = onNavigateToSearch
                )
            }
        }
    }
}

@Composable
fun SlidingDockItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    onPositioned: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current

    // No bouncy scaling - smooth snappy compression on press
    val touchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = MotionTokens.snappySpring(),
        label = "dockItemTouchScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "dockItemContentColor"
    )

    Box(
        modifier = Modifier
            .scale(touchScale)
            .clip(ShapeFull)
            .onGloballyPositioned { coordinates ->
                val parentCoordinates = coordinates.parentLayoutCoordinates
                if (parentCoordinates != null) {
                    val positionInParent = parentCoordinates.localPositionOf(coordinates, Offset.Zero)
                    onPositioned(positionInParent.x, coordinates.size.width.toFloat())
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.tick()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(MotionTokens.snappySpring()) +
                            expandHorizontally(animationSpec = MotionTokens.snappySpring()) +
                            slideInHorizontally(animationSpec = MotionTokens.snappySpring(), initialOffsetX = { -it / 4 }),
                    exit = fadeOut(MotionTokens.snappySpring()) +
                            shrinkHorizontally(animationSpec = MotionTokens.snappySpring()) +
                            slideOutHorizontally(animationSpec = MotionTokens.snappySpring(), targetOffsetX = { -it / 4 })
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
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
        animationSpec = MotionTokens.snappySpring(),
        label = "dockItemTouchScale"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isTertiary) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "pillColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            if (isTertiary) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        },
        animationSpec = MotionTokens.snappySpring(),
        label = "contentColor"
    )

    Box(
        modifier = Modifier
            .scale(touchScale)
            .clip(ShapeFull)
            .background(pillColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    view.tick()
                    onClick()
                }
            )
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(MotionTokens.snappySpring()) +
                            expandHorizontally(animationSpec = MotionTokens.snappySpring()) +
                            slideInHorizontally(animationSpec = MotionTokens.snappySpring(), initialOffsetX = { -it / 4 }),
                    exit = fadeOut(MotionTokens.snappySpring()) +
                            shrinkHorizontally(animationSpec = MotionTokens.snappySpring()) +
                            slideOutHorizontally(animationSpec = MotionTokens.snappySpring(), targetOffsetX = { -it / 4 })
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
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
    }
}


fun getTabRouteIndex(route: String?): Int {
    return when (route) {
        "photos" -> 0
        "albums", "album/{bucketName}" -> 1
        "search" -> 2
        "settings" -> 3
        else -> 0
    }
}

// Directional tab transitions: slide left/right based on tab order
fun getEnterTransition(initialRoute: String?, targetRoute: String?): androidx.compose.animation.EnterTransition {
    val fromIndex = getTabRouteIndex(initialRoute)
    val toIndex = getTabRouteIndex(targetRoute)
    return fadeIn(
        animationSpec = MotionTokens.snappySpring()
    ) + slideInHorizontally(
        initialOffsetX = { if (toIndex > fromIndex) it / 5 else -it / 5 },
        animationSpec = MotionTokens.snappySpring()
    )
}

fun getExitTransition(initialRoute: String?, targetRoute: String?): androidx.compose.animation.ExitTransition {
    val fromIndex = getTabRouteIndex(initialRoute)
    val toIndex = getTabRouteIndex(targetRoute)
    return androidx.compose.animation.fadeOut(
        animationSpec = MotionTokens.snappySpring()
    ) + androidx.compose.animation.slideOutHorizontally(
        targetOffsetX = { if (toIndex > fromIndex) -it / 5 else it / 5 },
        animationSpec = MotionTokens.snappySpring()
    )
}
