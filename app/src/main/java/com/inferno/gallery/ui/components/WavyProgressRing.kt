package com.inferno.gallery.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A beautiful, expressive wavy progress ring.
 *
 * Drawn dynamically using Path coordinates offset by a sine wave
 * driven by infinite transitions to create organic fluid movement.
 * Bypasses heavy layouts or animations, drawing directly to the hardware-accelerated Canvas.
 */
@Composable
fun WavyProgressRing(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    waveColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 3.dp,
    waveCount: Int = 8,
    waveAmplitude: Float = 3f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavyProgressRing")

    // Animates the wave phase shift (flows along the ring)
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Animates the overall rotation of the ring
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val width = this.size.width
        val height = this.size.height
        val centerX = width / 2f
        val centerY = height / 2f
        
        // Base radius of the ring
        val strokePx = strokeWidth.toPx()
        val baseRadius = (width.coerceAtMost(height) - strokePx - (waveAmplitude * 2)) / 2f

        if (baseRadius > 0) {
            val path = Path()
            
            // Build the closed wavy path
            val steps = 120
            val rotationRad = (rotation * PI / 180f).toFloat()

            for (i in 0..steps) {
                val theta = (i * 2 * PI / steps).toFloat()
                
                // Offset radius using the sine wave function
                val offset = waveAmplitude * sin(waveCount * theta + phase)
                val radius = baseRadius + offset
                
                val x = centerX + radius * cos(theta + rotationRad)
                val y = centerY + radius * sin(theta + rotationRad)
                
                if (i == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            path.close()

            drawPath(
                path = path,
                color = waveColor,
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
