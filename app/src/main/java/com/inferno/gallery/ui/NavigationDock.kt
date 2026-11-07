package com.inferno.gallery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.NavDockLabelStyle
import com.inferno.gallery.ui.utils.tick
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import com.inferno.gallery.R

/**
 * Google Photos style floating navigation dock with dynamic intrinsic content-sized tabs,
 * smooth pushing transitions, and satellite circular search pill.
 */
@Composable
fun FloatingNavigationPill(
    currentRoute: String?,
    onNavigateToPhotos: () -> Unit,
    onNavigateToAlbums: () -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSearch: () -> Unit,
    showVideosTab: Boolean = false,
    onNavigateToVideos: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selectedIndex = when {
        currentRoute == "photos" -> 0
        currentRoute?.startsWith("album") == true || currentRoute == "albums" -> 1
        currentRoute == "collections" -> 2
        else -> -1
    }

    val isSearchSelected = currentRoute == "search"

    Row(
        modifier = modifier
            .wrapContentWidth()
            .pointerInput(Unit) {},
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Main Capsule (Photos, Albums, Collections) ──
        Surface(
            modifier = Modifier.height(56.dp),
            shape = ShapeFull,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(8.dp)
                    .animateContentSize(
                        animationSpec = MotionTokens.snappySpring()
                    ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DynamicPillTabItem(
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_image),
                            contentDescription = stringResource(R.string.nav_photos),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(R.string.nav_photos),
                    isSelected = selectedIndex == 0,
                    onClick = onNavigateToPhotos
                )

                DynamicPillTabItem(
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_photo_album),
                            contentDescription = stringResource(R.string.nav_albums),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(R.string.nav_albums),
                    isSelected = selectedIndex == 1,
                    onClick = onNavigateToAlbums
                )

                DynamicPillTabItem(
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_dashboard),
                            contentDescription = stringResource(R.string.nav_collections),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = stringResource(R.string.nav_collections),
                    isSelected = selectedIndex == 2,
                    onClick = onNavigateToCollections
                )
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
            modifier = Modifier.size(56.dp),
            shape = ShapeFull,
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = searchTouchScale
                        scaleY = searchTouchScale
                    }
                    .clip(ShapeFull)
                    .clickable(
                        interactionSource = searchInteractionSource,
                        indication = ripple(
                            bounded = true,
                            color = searchContentColor
                        ),
                        onClick = {
                            view.tick()
                            onNavigateToSearch()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_search),
                    contentDescription = stringResource(R.string.nav_search),
                    tint = searchContentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun DynamicPillTabItem(
    icon: @Composable () -> Unit,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
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
        targetValue = if (isSelected) 16.dp else 13.dp,
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
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                Text(
                    text = label,
                    style = NavDockLabelStyle.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
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

// Fast, fluid tab crossfade transitions: eliminates heavy full-screen grid slide bottleneck
fun getEnterTransition(initialRoute: String?, targetRoute: String?): androidx.compose.animation.EnterTransition {
    return fadeIn(
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = MotionTokens.Durations.Short,
            easing = MotionTokens.EmphasizedDecelerateEasing
        )
    )
}

fun getExitTransition(initialRoute: String?, targetRoute: String?): androidx.compose.animation.ExitTransition {
    return fadeOut(
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = MotionTokens.Durations.Short,
            easing = MotionTokens.EmphasizedAccelerateEasing
        )
    )
}
