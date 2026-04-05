package dev.slate.ai.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Uiverse.io-inspired thinking entity.
 * A glowing circle containing multiple rotating, overlapping triangles
 * with gradient fill and slow hue-shift. Exactly matches the design by andrew-manzyk.
 *
 * Features:
 * - Outer glowing circle with gradient border
 * - 6 triangles rotating at different speeds and directions
 * - Gradient fill (warm to cool) on the triangle mass
 * - Slow hue rotation via color interpolation (6s cycle)
 * - Inner glow and shadow effects
 */
@Composable
fun SlateThinkingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFFA8C4D4),
) {
    val transition = rememberInfiniteTransition(label = "entity")

    // === Hue rotation (6s full cycle, matches CSS colorize keyframe) ===
    val hueShift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "hue",
    )

    // === 6 triangle rotations at different speeds/directions ===
    // Triangle 1: reverse, 2s
    val rot1 by transition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "r1",
    )
    // Triangle 2: forward, 2s, delayed feel via different start
    val rot2 by transition.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "r2",
    )
    // Triangle 3: reverse, 2s
    val rot3 by transition.animateFloat(
        initialValue = 240f, targetValue = -120f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "r3",
    )
    // Triangle 4: reverse, 2s, offset
    val rot4 by transition.animateFloat(
        initialValue = 180f, targetValue = -180f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "r4",
    )
    // Triangle 5: forward, 2s
    val rot5 by transition.animateFloat(
        initialValue = 60f, targetValue = 420f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "r5",
    )
    // Triangle 6: forward, different speed (2.67s)
    val rot6 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2670, easing = LinearEasing)),
        label = "r6",
    )

    // === Glow pulse (breathing) ===
    val glowAlpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow",
    )

    // Derive hue-shifted colors
    val colorOne = hueRotate(accentColor, hueShift)
    val colorTwo = hueRotate(darken(accentColor, 0.6f), hueShift)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w / 2f

        // === Outer glow ===
        drawCircle(
            color = colorOne.copy(alpha = glowAlpha * 0.3f),
            radius = radius * 1.15f,
            center = Offset(cx, cy),
        )

        // === Outer circle with gradient border ===
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colorOne.copy(alpha = 0.15f),
                    colorTwo.copy(alpha = 0.15f),
                ),
            ),
            radius = radius,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = colorOne.copy(alpha = 0.4f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = radius * 0.02f),
        )

        // === Inner gradient background ===
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    colorOne.copy(alpha = 0.08f),
                    colorTwo.copy(alpha = 0.12f),
                ),
            ),
            radius = radius * 0.95f,
            center = Offset(cx, cy),
        )

        // === Rotating triangles (the core entity) ===
        // Large triangles
        val bigR = radius * 0.55f
        val smallR = radius * 0.38f

        // Each triangle rotates around a slightly offset pivot
        drawRotatedTriangle(cx, cy, bigR, rot1, Offset(cx * 1.0f, cy * 0.85f), colorOne, colorTwo, 0.7f)
        drawRotatedTriangle(cx, cy, bigR, rot2, Offset(cx * 1.0f, cy * 1.1f), colorTwo, colorOne, 0.6f)
        drawRotatedTriangle(cx, cy, smallR, rot3, Offset(cx * 0.9f, cy * 0.95f), colorOne, colorTwo, 0.8f)
        drawRotatedTriangle(cx, cy, smallR, rot4, Offset(cx * 1.1f, cy * 0.95f), colorTwo, colorOne, 0.5f)
        drawRotatedTriangle(cx, cy, smallR * 0.85f, rot5, Offset(cx, cy), colorOne, colorTwo, 0.65f)
        drawRotatedTriangle(cx, cy, smallR * 0.85f, rot6, Offset(cx, cy), colorTwo, colorOne, 0.55f)

        // === Center bright dot ===
        drawCircle(
            color = colorOne.copy(alpha = glowAlpha),
            radius = radius * 0.06f,
            center = Offset(cx, cy),
        )

        // === Inner glow (top highlight) ===
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorOne.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(cx, cy * 0.6f),
                radius = radius * 0.6f,
            ),
            radius = radius * 0.6f,
            center = Offset(cx, cy * 0.6f),
        )
    }
}

private fun DrawScope.drawRotatedTriangle(
    cx: Float, cy: Float,
    radius: Float,
    rotation: Float,
    pivot: Offset,
    colorA: Color, colorB: Color,
    alpha: Float,
) {
    rotate(rotation, pivot) {
        val path = Path()
        for (i in 0..2) {
            val angle = Math.toRadians((i * 120.0 - 90.0))
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // Filled triangle with gradient
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(
                    colorA.copy(alpha = alpha * 0.7f),
                    colorB.copy(alpha = alpha * 0.5f),
                ),
            ),
            style = Fill,
        )

        // Triangle border
        drawPath(
            path = path,
            color = colorA.copy(alpha = alpha * 0.3f),
            style = Stroke(width = 1f, cap = StrokeCap.Round),
        )
    }
}

/** Simulate hue rotation by interpolating through color stops */
private fun hueRotate(base: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    // 5-stop hue cycle matching the CSS colorize keyframes
    val stops = listOf(
        base,                                           // 0%
        shiftHue(base, -30f),                           // 20%
        shiftHue(base, -60f),                           // 40%
        shiftHue(base, -90f),                           // 60%
        shiftHue(base, -45f),                           // 80%
        base,                                           // 100%
    )
    val segment = f * (stops.size - 1)
    val idx = segment.toInt().coerceIn(0, stops.size - 2)
    val localF = segment - idx
    return lerp(stops[idx], stops[idx + 1], localF)
}

private fun shiftHue(color: Color, degrees: Float): Color {
    val r = color.red; val g = color.green; val b = color.blue
    val rad = Math.toRadians(degrees.toDouble()).toFloat()
    val cosA = cos(rad.toDouble()).toFloat()
    val sinA = sin(rad.toDouble()).toFloat()
    // Approximate hue rotation matrix
    val nr = (0.213f + cosA * 0.787f - sinA * 0.213f) * r +
             (0.715f - cosA * 0.715f - sinA * 0.715f) * g +
             (0.072f - cosA * 0.072f + sinA * 0.928f) * b
    val ng = (0.213f - cosA * 0.213f + sinA * 0.143f) * r +
             (0.715f + cosA * 0.285f + sinA * 0.140f) * g +
             (0.072f - cosA * 0.072f - sinA * 0.283f) * b
    val nb = (0.213f - cosA * 0.213f - sinA * 0.787f) * r +
             (0.715f - cosA * 0.715f + sinA * 0.715f) * g +
             (0.072f + cosA * 0.928f + sinA * 0.072f) * b
    return Color(nr.coerceIn(0f, 1f), ng.coerceIn(0f, 1f), nb.coerceIn(0f, 1f), color.alpha)
}

private fun darken(color: Color, factor: Float): Color {
    return Color(color.red * factor, color.green * factor, color.blue * factor, color.alpha)
}

private fun lerp(a: Color, b: Color, f: Float): Color {
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f,
    )
}
