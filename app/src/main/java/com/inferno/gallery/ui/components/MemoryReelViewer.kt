package com.inferno.gallery.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.inferno.gallery.R
import com.inferno.gallery.data.MemoryDayGroup
import com.inferno.gallery.ui.GalleryItem
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeEdgeTop
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.ShapeMedium
import com.inferno.gallery.ui.theme.SpacingTokens
import com.inferno.gallery.ui.utils.ShareUtils
import com.inferno.gallery.ui.utils.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Full-screen interactive Memory Reel Viewer.
 *
 * Implements:
 * - Dynamic blurred ambient lighting using the photo's colors.
 * - Story-style segmented progress bar with auto-advance (4.5s per photo).
 * - Hold to pause, tap left/right to navigate, swipe down to dismiss.
 * - Action bar: Favorite, Share (with metadata stripping), and Open in Viewer.
 * - Quick-grid bottom sheet to jump to any photo in the memory day.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MemoryReelViewer(
    group: MemoryDayGroup,
    favoriteIds: Set<String>,
    onToggleFavorite: (String) -> Unit,
    onOpenPhotoDetail: (String) -> Unit,
    onDismiss: () -> Unit,
    stripMetadataFlow: kotlinx.coroutines.flow.Flow<Boolean>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var showGridSheet by remember { mutableStateOf(false) }

    val currentItem = group.items.getOrElse(currentIndex) { group.coverItem }
    val isFavorite = favoriteIds.contains(currentItem.id)

    // Vertical drag for swipe-to-dismiss gesture
    val dismissOffsetY = remember { Animatable(0f) }
    val dismissProgress = (kotlin.math.abs(dismissOffsetY.value) / 500f).coerceIn(0f, 1f)
    val scrimAlpha = 1f - dismissProgress * 0.75f
    val contentScale = 1f - dismissProgress * 0.15f

    // 4.5 seconds per photo
    val photoDurationMs = 4500L
    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }

    // Auto-advance ticker
    LaunchedEffect(currentIndex, isPaused, showGridSheet) {
        if (isPaused || showGridSheet) return@LaunchedEffect
        val stepMs = 30L
        val increment = stepMs.toFloat() / photoDurationMs.toFloat()

        while (progress < 1f) {
            delay(stepMs)
            progress = (progress + increment).coerceAtMost(1f)
        }

        // Advance to next photo or dismiss when finished
        if (currentIndex < group.items.lastIndex) {
            progress = 0f
            currentIndex++
        } else {
            // Finished entire memory reel
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = remember(view) {
            var parent = view.parent
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    return@remember parent.window
                }
                parent = parent.parent
            }
            null
        }

        SideEffect {
            dialogWindow?.let { window ->
                window.setDimAmount(0f)
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                window.setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.MATCH_PARENT
                )
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }

        BackHandler(enabled = showGridSheet) {
            showGridSheet = false
            isPaused = false
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .graphicsLayer {
                    translationY = dismissOffsetY.value
                    scaleX = contentScale
                    scaleY = contentScale
                }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            dismissOffsetY.snapTo(dismissOffsetY.value + dragAmount)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            if (dismissOffsetY.value > 120f) {
                                onDismiss()
                            } else {
                                dismissOffsetY.animateTo(0f, MotionTokens.snappySpring())
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dismissOffsetY.animateTo(0f, MotionTokens.snappySpring())
                        }
                    }
                )
            }
    ) {
        // Ambient dynamic background blur
        val ambientRequest = remember(currentItem.uri) {
            ImageRequest.Builder(context)
                .data(if (currentItem.localExists) currentItem.resolvedUri else currentItem.uri)
                .size(160, 160)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }
        AsyncImage(
            model = ambientRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(32.dp)
                .alpha(0.35f * scrimAlpha)
        )

        // Subtle Ken Burns slow pan-zoom
        val zoomScale by animateFloatAsState(
            targetValue = if (!isPaused) 1.05f else 1.0f,
            animationSpec = tween(4500, easing = LinearEasing),
            label = "kenBurnsScale"
        )

        // Main Photo
        val mainImageRequest = remember(currentItem.uri) {
            ImageRequest.Builder(context)
                .data(if (currentItem.localExists) currentItem.resolvedUri else currentItem.uri)
                .size(1400, 1400)
                .crossfade(true)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 72.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = mainImageRequest,
                contentDescription = currentItem.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoomScale)
            )
        }

        // Tap Detection Layer (Left 35% = Prev, Right 65% = Next, Hold = Pause)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentIndex, group.items.size) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.35f) {
                                // Previous
                                if (currentIndex > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    progress = 0f
                                    currentIndex--
                                }
                            } else {
                                // Next
                                if (currentIndex < group.items.lastIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    progress = 0f
                                    currentIndex++
                                } else {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        )

        // Top Chrome: Story Progress Segments & Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = SpacingTokens.M, vertical = SpacingTokens.S)
        ) {
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SpacingTokens.S),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                group.items.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress
                        else -> 0f
                    }

                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(ShapeFull),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.30f)
                    )
                }
            }

            // Top Bar with Title, Date, and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
                    modifier = Modifier.weight(1f)
                ) {
                    // Close button (48dp touch target)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.50f),
                            contentColor = Color.White,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                    contentDescription = "Close reel",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${group.formattedDate} • ${currentIndex + 1} of ${group.items.size}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.80f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Grid view button
                IconButton(
                    onClick = {
                        isPaused = true
                        showGridSheet = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.50f),
                        contentColor = Color.White,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_grid_view),
                                contentDescription = "View all in grid",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Chrome: Floating Action Pill
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = SpacingTokens.L),
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ShapeFull,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Favorite button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite(currentItem.id)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_favorite),
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val strip = stripMetadataFlow.first()
                                ShareUtils.shareMedia(context, listOf(currentItem.uri), strip)
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_share),
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Open in full viewer button
                    IconButton(
                        onClick = {
                            onOpenPhotoDetail(currentItem.id)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_open_in_full),
                            contentDescription = "Open in viewer",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Quick Grid Overlay Sheet
        AnimatedVisibility(
            visible = showGridSheet,
            enter = fadeIn(MotionTokens.snappySpring()) + slideInVertically(MotionTokens.snappySpring()) { it },
            exit = fadeOut(MotionTokens.snappySpring()) + slideOutVertically(MotionTokens.snappySpring()) { it }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showGridSheet = false
                        isPaused = false
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = ShapeEdgeTop,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 6.dp,
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .navigationBarsPadding()
                ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.M)
                        .padding(top = SpacingTokens.M, bottom = SpacingTokens.XL)
                ) {
                    // Drag handle pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = SpacingTokens.M)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(ShapeFull)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = SpacingTokens.M),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${group.formattedDate} • ${group.items.size} moments",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                showGridSheet = false
                                isPaused = false
                            }
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_ms_close),
                                contentDescription = "Close grid",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        itemsIndexed(group.items) { idx, item ->
                            val isSelected = (idx == currentIndex)
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(ShapeMedium)
                                    .pressScale(pressedScale = 0.95f)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        currentIndex = idx
                                        progress = 0f
                                        showGridSheet = false
                                        isPaused = false
                                    }
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(if (item.localExists) item.resolvedUri else item.uri)
                                        .size(300, 300)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}
