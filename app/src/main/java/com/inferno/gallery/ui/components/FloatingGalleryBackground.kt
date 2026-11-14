package com.inferno.gallery.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance AGSL RuntimeShader simulating true liquid fluid mechanics:
 *  1. Multi-octave domain warping (Navier-Stokes advection approximation) creating curling eddies and silk waves.
 *  2. Continuous scalar field interference for infinite C1 gradient smoothness (strictly zero circular blobs).
 *  3. Soothing pastel-tonal palette blending for zero jarring contrast and maximum text readability.
 */
private const val SOOTHING_LIQUID_AGSL_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform vec4 color1; // Deep resting base
uniform vec4 color2; // Soothing primary silk current
uniform vec4 color3; // Serene tertiary/ocean drift
uniform vec4 color4; // Gentle ambient warmth

vec4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float2 p = uv;
    p.x *= aspect;

    // Meditative, calming liquid flow rate
    float t = iTime * 0.12;

    // --- Multi-Octave Domain Warping (Simulates Fluid Advection & Eddies) ---
    // Octave 1: broad slow liquid current
    float2 q = float2(
        sin(p.x * 1.4 + t * 0.35) + cos(p.y * 1.6 - t * 0.28),
        cos(p.x * 1.5 - t * 0.30) + sin(p.y * 1.3 + t * 0.40)
    );

    // Octave 2: recursive coordinate curl (fluid shear)
    float2 r = float2(
        sin(p.x * 1.6 + q.x * 0.75 + t * 0.20),
        cos(p.y * 1.6 + q.y * 0.75 - t * 0.25)
    );

    // Continuous wave interference fields creating organic liquid silk
    float f1 = sin(p.y * 1.4 + r.x * 1.5 + t * 0.30) * 0.5 + 0.5;
    float f2 = cos(p.x * 1.5 + r.y * 1.4 - t * 0.25) * 0.5 + 0.5;
    float f3 = sin((p.x + p.y) * 1.2 + (q.x - q.y) * 0.85 + t * 0.18) * 0.5 + 0.5;

    // Organic continuous blending across the fluid field (zero hard edges, zero blobs)
    vec4 col = color1;
    col = mix(col, color2, smoothstep(0.12, 0.88, f1) * 0.75);
    col = mix(col, color3, smoothstep(0.18, 0.82, f2) * 0.55);
    col = mix(col, color4, smoothstep(0.22, 0.78, f3) * 0.35);

    // Soft global radial falloff to keep edges calm and anchored
    float vignette = smoothstep(1.35, 0.40, length((uv - 0.5) * float2(aspect * 0.8, 1.0)));
    col = mix(color1, col, 0.75 + 0.25 * vignette);

    return col;
}
"""

/**
 * Harmonized Fluid Canvas background for onboarding and hero screens.
 * Provides a soothing, organic, tranquil animated backdrop with zero harsh contrast or rigid shapes.
 */
@Composable
fun FloatingGalleryBackground(
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    primaryGlow: Color = MaterialTheme.colorScheme.primary,
    tertiaryGlow: Color = MaterialTheme.colorScheme.tertiary,
    secondaryGlow: Color = MaterialTheme.colorScheme.secondary,
    content: (@Composable () -> Unit)? = null
) {
    // AGSL RuntimeShader for API 33+ (Android 13+)
    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { RuntimeShader(SOOTHING_LIQUID_AGSL_SHADER) }.getOrNull()
        } else {
            null
        }
    }
    val shaderBrush = remember(runtimeShader) {
        if (runtimeShader != null) {
            ShaderBrush(runtimeShader)
        } else {
            null
        }
    }

    // Meditative, smooth continuous clock ticker
    val transition = rememberInfiniteTransition(label = "SoothingFluidTransition")
    val timeSeconds by transition.animateFloat(
        initialValue = 0f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SoothingFluidSeconds"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

            val t = timeSeconds

            // Soothing, non-jarring tonal color palette:
            // Lerping with baseColor softens neon saturation and ensures calm visual harmony
            val c1 = baseColor
            val c2 = lerp(baseColor, primaryGlow, 0.26f)
            val c3 = lerp(baseColor, tertiaryGlow, 0.20f)
            val c4 = lerp(baseColor, secondaryGlow, 0.16f)

            // Hardware AGSL Shader Rendering
            if (runtimeShader != null && shaderBrush != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runtimeShader.setFloatUniform("iResolution", canvasWidth, canvasHeight)
                runtimeShader.setFloatUniform("iTime", t)
                runtimeShader.setFloatUniform("color1", c1.red, c1.green, c1.blue, c1.alpha)
                runtimeShader.setFloatUniform("color2", c2.red, c2.green, c2.blue, c2.alpha)
                runtimeShader.setFloatUniform("color3", c3.red, c3.green, c3.blue, c3.alpha)
                runtimeShader.setFloatUniform("color4", c4.red, c4.green, c4.blue, c4.alpha)
                drawRect(brush = shaderBrush)
            } else {
                // Compose 2D Canvas Fallback (API < 33): Soft ambient fluid mesh
                drawRect(color = c1)

                val flowT = t * 0.15f
                val radius = maxOf(canvasWidth, canvasHeight) * 1.5f

                val cx1 = canvasWidth * 0.5f + (canvasWidth * 0.35f) * cos(flowT * 0.5f)
                val cy1 = canvasHeight * 0.4f + (canvasHeight * 0.30f) * sin(flowT * 0.4f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c2.copy(alpha = 0.50f), Color.Transparent),
                        center = Offset(cx1, cy1),
                        radius = radius
                    )
                )

                val cx2 = canvasWidth * 0.5f + (canvasWidth * 0.30f) * sin(flowT * 0.45f + 2.0f)
                val cy2 = canvasHeight * 0.6f + (canvasHeight * 0.25f) * cos(flowT * 0.35f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c3.copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(cx2, cy2),
                        radius = radius * 1.2f
                    )
                )

                val cx3 = canvasWidth * 0.5f + (canvasWidth * 0.25f) * cos(flowT * 0.35f + 4.0f)
                val cy3 = canvasHeight * 0.5f + (canvasHeight * 0.25f) * sin(flowT * 0.30f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(c4.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(cx3, cy3),
                        radius = radius * 1.3f
                    )
                )
            }
        }

        // Foreground content slot
        content?.invoke()
    }
}
