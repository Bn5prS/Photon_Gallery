package com.inferno.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.inferno.gallery.R
import com.inferno.gallery.ui.theme.IconSizeTokens
import com.inferno.gallery.ui.theme.MotionTokens
import com.inferno.gallery.ui.theme.ShapeFull
import com.inferno.gallery.ui.theme.SpacingTokens
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Material 3 Expressive glowing hairline halo overlaid on a person's head
 * when navigating into a photo from their Person album.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonHeadGlowOverlay(
    headRect: Rect,
    personName: String?,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    displayDurationMs: Long = 3500L
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(headRect) {
        isVisible = true
        delay(displayDurationMs)
        isVisible = false
        delay(300) // Allow exit fade transition to complete
        onDismiss()
    }

    val entranceProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = if (isVisible) MotionTokens.bouncySpring() else MotionTokens.fastEffectsSpec(),
        label = "personHeadGlowEntrance"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "personHeadGlowPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val density = LocalDensity.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val cornerRadiusPx = with(density) { 24.dp.toPx() }
    val outerGlowStrokePx = with(density) { 6.dp.toPx() }
    val hairlineStrokePx = with(density) { 1.8.dp.toPx() }

    AnimatedVisibility(
        visible = entranceProgress > 0.05f,
        enter = fadeIn(MotionTokens.fastEffectsSpec()),
        exit = fadeOut(MotionTokens.fastEffectsSpec()),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    alpha = entranceProgress
                }
        ) {
            // Glowing hairline canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topLeft = Offset(headRect.left, headRect.top)
                val rectSize = Size(headRect.width, headRect.height)
                val cRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)

                // 1. Outer soft glowing halo
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.28f * pulseAlpha * entranceProgress),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = cRadius,
                    style = Stroke(width = outerGlowStrokePx)
                )

                // 2. Core glowing hairline
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.95f * pulseAlpha * entranceProgress),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = cRadius,
                    style = Stroke(width = hairlineStrokePx)
                )
            }

            // Floating Name Pill above the head
            if (!personName.isNullOrBlank()) {
                val pillOffsetY = (headRect.top - with(density) { 36.dp.toPx() }).coerceAtLeast(with(density) { 12.dp.toPx() })
                val pillCenterX = headRect.center.x

                Surface(
                    shape = ShapeFull,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (pillCenterX - with(density) { 48.dp.toPx() }).roundToInt().coerceAtLeast(16),
                                y = pillOffsetY.roundToInt()
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = SpacingTokens.S, vertical = SpacingTokens.XS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_ms_person),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(IconSizeTokens.S)
                        )
                        Spacer(modifier = Modifier.width(SpacingTokens.XS))
                        Text(
                            text = personName,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
