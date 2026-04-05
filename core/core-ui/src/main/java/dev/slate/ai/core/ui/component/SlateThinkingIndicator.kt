package dev.slate.ai.core.ui.component

import androidx.compose.animation.core.EaseInOutCubic
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Geometric thinking entity — inspired by Uiverse.io rotating triangles.
 * Three overlapping triangles rotate at different speeds with pulsing alpha
 * and slow hue-shift via color interpolation. Creates a living, breathing
 * geometric entity that represents AI processing.
 */
@Composable
fun SlateThinkingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFFA8C4D4),
) {
    val transition = rememberInfiniteTransition(label = "thinking")

    // Three triangles rotating at different speeds
    val rotation1 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "rot1",
    )
    val rotation2 by transition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "rot2",
    )
    val rotation3 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "rot3",
    )

    // Alpha pulse — breathing effect
    val alpha1 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "alpha1",
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.9f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "alpha2",
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.5f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "alpha3",
    )

    // Color shift — subtle hue rotation effect via color lerp
    val colorShift by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "colorShift",
    )

    // Derive shifted colors
    val warmAccent = lerpColor(accentColor, Color(0xFFFFAB91), 0.3f)
    val coolAccent = lerpColor(accentColor, Color(0xFF80CBC4), 0.3f)

    val color1 = lerpColor(accentColor, warmAccent, colorShift)
    val color2 = lerpColor(coolAccent, accentColor, colorShift)
    val color3 = lerpColor(warmAccent, coolAccent, colorShift)

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension * 0.38f

        val stroke = Stroke(
            width = this.size.minDimension * 0.035f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        // Triangle 1 — large, slow color shift
        rotate(rotation1, Offset(cx, cy)) {
            drawTriangle(cx, cy, radius, color1.copy(alpha = alpha1), stroke)
        }

        // Triangle 2 — medium, counter-rotating
        rotate(rotation2, Offset(cx, cy)) {
            drawTriangle(cx, cy, radius * 0.75f, color2.copy(alpha = alpha2), stroke)
        }

        // Triangle 3 — small, fastest rotation feel via alpha
        rotate(rotation3, Offset(cx, cy)) {
            drawTriangle(cx, cy, radius * 0.5f, color3.copy(alpha = alpha3), stroke)
        }

        // Center glow dot
        drawCircle(
            color = accentColor.copy(alpha = alpha1 * 0.4f),
            radius = this.size.minDimension * 0.05f,
            center = Offset(cx, cy),
        )
    }
}

private fun DrawScope.drawTriangle(
    cx: Float, cy: Float, radius: Float, color: Color, stroke: Stroke,
) {
    val path = Path()
    for (i in 0..2) {
        val angle = Math.toRadians((i * 120.0 - 90.0))
        val x = cx + radius * cos(angle).toFloat()
        val y = cy + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = stroke)
}

private fun lerpColor(a: Color, b: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = a.alpha + (b.alpha - a.alpha) * f,
    )
}
