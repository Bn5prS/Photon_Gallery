package com.inferno.gallery.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

    // Natural, clearly visible liquid & wind flow rate
    float t = iTime * 0.70;
    float2 adv = p + float2(-t * 0.28, t * 0.14);

    // --- Multi-Octave Domain Warping (Simulates Fluid Advection & Wind Eddies) ---
    // Octave 1: broad sweeping liquid/wind current
    float2 q = float2(
        sin(adv.y * 2.1 + t * 0.72) + cos(adv.x * 1.6 - t * 0.52),
        cos(adv.x * 1.9 - t * 0.60) + sin(adv.y * 1.7 + t * 0.78)
    );

    // Octave 2: recursive coordinate curl (fluid shear)
    float2 r = float2(
        sin(adv.x * 2.3 + q.x * 1.10 + t * 0.55),
        cos(adv.y * 2.2 + q.y * 1.10 - t * 0.62)
    );

    // Traveling silk/wind bands that sweep smoothly across the viewport
    float w1 = sin(adv.x * 1.8 - adv.y * 1.1 + r.x * 1.4 + t * 0.65) * 0.5 + 0.5;
    float w2 = cos(adv.y * 1.7 + adv.x * 0.9 + r.y * 1.5 - t * 0.55) * 0.5 + 0.5;
    float w3 = sin((adv.x - adv.y) * 2.2 + (q.x + q.y) * 1.0 + t * 0.48) * 0.5 + 0.5;

    float f1 = smoothstep(0.15, 0.85, w1);
    float f2 = smoothstep(0.18, 0.82, w2);
    float f3 = smoothstep(0.20, 0.80, w3);

    // Organic continuous blending across the fluid field (zero hard edges, zero blobs)
    vec4 col = color1;
    col = mix(col, color2, f1 * 0.88);
    col = mix(col, color3, f2 * 0.72);
    col = mix(col, color4, f3 * 0.55);

    // Soft global radial falloff to keep edges calm and anchored
    float vignette = smoothstep(1.45, 0.30, length((uv - 0.5) * float2(aspect * 0.75, 1.0)));
    col = mix(color1, col, 0.88 + 0.12 * vignette);

    // Subtle 2.5% procedural film grain for tactile photo-paper depth & zero gradient banding
    float grain = fract(sin(dot(fragCoord, float2(12.9898, 78.233))) * 43758.5453);
    col.rgb += (grain - 0.5) * 0.025;

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
    primaryContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    secondaryContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
    tertiaryContainer: Color = MaterialTheme.colorScheme.tertiaryContainer,
    primaryGlow: Color = MaterialTheme.colorScheme.primary,
    content: (@Composable () -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
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

    val timeState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    timeState.floatValue += dt
                }
                lastNanos = frameNanos
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas
            val elapsed = timeState.floatValue

            // Calm Material 3 wallpaper-extracted tonal sweep anchored on surfaceContainerLowest:
            // keeps chrome and cards legible while preserving subtle editorial warmth/depth.
            val c1 = baseColor
            val c2 = lerp(baseColor, primaryContainer, if (isDark) 0.82f else 0.78f)
            val c3 = lerp(baseColor, tertiaryContainer, if (isDark) 0.72f else 0.68f)
            val c4 = lerp(baseColor, secondaryContainer, if (isDark) 0.64f else 0.60f)

            // Hardware AGSL Shader Rendering (smooth continuous liquid/wind flow + 2.5% film grain)
            if (runtimeShader != null && shaderBrush != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runtimeShader.setFloatUniform("iResolution", canvasWidth, canvasHeight)
                runtimeShader.setFloatUniform("iTime", elapsed)
                runtimeShader.setFloatUniform("color1", c1.red, c1.green, c1.blue, c1.alpha)
                runtimeShader.setFloatUniform("color2", c2.red, c2.green, c2.blue, c2.alpha)
                runtimeShader.setFloatUniform("color3", c3.red, c3.green, c3.blue, c3.alpha)
                runtimeShader.setFloatUniform("color4", c4.red, c4.green, c4.blue, c4.alpha)
                drawRect(brush = shaderBrush)
            } else {
                // Compose 2D Canvas Fallback (API < 33): Gentle drifting tonal sweep
                val driftX = sin(elapsed * 0.35f) * canvasWidth * 0.2f
                val driftY = cos(elapsed * 0.28f) * canvasHeight * 0.2f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(c1, c2, c3),
                        start = Offset(driftX, driftY),
                        end = Offset(canvasWidth - driftX, canvasHeight - driftY)
                    )
                )
            }
        }

        // Foreground content slot
        content?.invoke()
    }
}
