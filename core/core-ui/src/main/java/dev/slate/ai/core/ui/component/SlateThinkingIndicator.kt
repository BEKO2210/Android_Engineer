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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slate thinking indicator — original design.
 *
 * A single orbital ring with 3 luminous dots orbiting at different speeds.
 * The ring itself breathes (alpha pulse). The dots leave no trail, just
 * smooth orbital motion. Minimal GPU load — only 1 arc + 3 circles per frame.
 *
 * Design rationale:
 * - Feels like atoms orbiting a nucleus, or satellites around a planet
 * - Much lighter than 6 rotating triangles (no Canvas path redraws)
 * - The 3 dots at different speeds create complex-looking patterns
 *   from very simple math
 * - Model accent color makes each model feel alive differently
 */
@Composable
fun SlateThinkingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    accentColor: Color = Color(0xFFA8C4D4),
) {
    val transition = rememberInfiniteTransition(label = "think")

    // 3 orbital dots at different speeds
    val angle1 by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "o1",
    )
    val angle2 by transition.animateFloat(
        initialValue = 120f, targetValue = 480f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "o2",
    )
    val angle3 by transition.animateFloat(
        initialValue = 240f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "o3",
    )

    // Ring breathing
    val ringAlpha by transition.animateFloat(
        initialValue = 0.15f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse),
        label = "ring",
    )

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension * 0.4f
        val dotRadius = this.size.minDimension * 0.055f

        // Orbital ring
        drawCircle(
            color = accentColor.copy(alpha = ringAlpha),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = this.size.minDimension * 0.02f, cap = StrokeCap.Round),
        )

        // Dot 1 — fastest, brightest
        val rad1 = Math.toRadians(angle1.toDouble())
        drawCircle(
            color = accentColor.copy(alpha = 0.9f),
            radius = dotRadius,
            center = Offset(cx + radius * cos(rad1).toFloat(), cy + radius * sin(rad1).toFloat()),
        )

        // Dot 2 — medium speed, slightly dimmer
        val rad2 = Math.toRadians(angle2.toDouble())
        drawCircle(
            color = accentColor.copy(alpha = 0.6f),
            radius = dotRadius * 0.8f,
            center = Offset(cx + radius * cos(rad2).toFloat(), cy + radius * sin(rad2).toFloat()),
        )

        // Dot 3 — slowest, dimmest
        val rad3 = Math.toRadians(angle3.toDouble())
        drawCircle(
            color = accentColor.copy(alpha = 0.4f),
            radius = dotRadius * 0.65f,
            center = Offset(cx + radius * cos(rad3).toFloat(), cy + radius * sin(rad3).toFloat()),
        )

        // Center nucleus — tiny static dot
        drawCircle(
            color = accentColor.copy(alpha = 0.5f),
            radius = dotRadius * 0.4f,
            center = Offset(cx, cy),
        )
    }
}
