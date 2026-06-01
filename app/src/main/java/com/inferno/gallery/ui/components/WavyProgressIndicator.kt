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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 8.dp,
    frequency: Float = 2f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(
        modifier = modifier.size(48.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val ampPx = amplitude.toPx()
        val strokePx = strokeWidth.toPx()

        val path = Path()
        var first = true

        val steps = 100
        for (i in 0..steps) {
            val x = (i.toFloat() / steps) * width
            // Map x to a wave using sine. Phase controls the animation sliding to the left
            val angle = (x / width) * frequency * 2f * Math.PI.toFloat() - phase
            val y = centerY + sin(angle) * ampPx

            if (first) {
                path.moveTo(x, y.toFloat())
                first = false
            } else {
                path.lineTo(x, y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
