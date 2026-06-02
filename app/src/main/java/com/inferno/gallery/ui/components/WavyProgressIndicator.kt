package com.inferno.gallery.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

// Wave speed in radians per second — feels continuous and smooth
private const val WAVE_SPEED_RAD_PER_SEC = 3.5f

@Composable
fun WavyProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 8.dp,
    frequency: Float = 2f
) {
    // Raw mutable state updated per-frame — zero allocation, no duration spec, no tween
    var phase by remember { mutableFloatStateOf(0f) }
    var lastFrameNanos by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameNanos ->
                val frameNanosFloat = frameNanos.toFloat()
                if (lastFrameNanos != 0f) {
                    val deltaSeconds = (frameNanosFloat - lastFrameNanos) / 1_000_000_000f
                    phase = (phase + WAVE_SPEED_RAD_PER_SEC * deltaSeconds) % (2f * Math.PI.toFloat())
                }
                lastFrameNanos = frameNanosFloat
            }
        }
    }

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
