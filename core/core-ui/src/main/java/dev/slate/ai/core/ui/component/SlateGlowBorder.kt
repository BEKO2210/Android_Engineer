package dev.slate.ai.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Subtle animated glow border inspired by Uiverse.io.
 * A rotating conic gradient creates a soft animated border effect.
 * Adapted for professional mobile aesthetic — not flashy, just refined.
 */
@Composable
fun SlateGlowCard(
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    secondaryGlow: Color = Color(0xFF4A6B7A).copy(alpha = 0.2f),
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    animated: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "glow_rotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (animated) 6000 else 0, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glow_angle",
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .drawBehind {
                // Outer glow — subtle blurred light
                val radius = size.maxDimension * 0.8f
                val angleRad = Math.toRadians(angle.toDouble())
                val glowX = center.x + (radius * 0.3f * cos(angleRad)).toFloat()
                val glowY = center.y + (radius * 0.3f * sin(angleRad)).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor,
                            Color.Transparent,
                        ),
                        center = Offset(glowX, glowY),
                        radius = radius * 0.5f,
                    ),
                    radius = radius * 0.5f,
                    center = Offset(glowX, glowY),
                )

                // Secondary glow on opposite side
                val angleRad2 = Math.toRadians(angle.toDouble() + 180.0)
                val glowX2 = center.x + (radius * 0.3f * cos(angleRad2)).toFloat()
                val glowY2 = center.y + (radius * 0.3f * sin(angleRad2)).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryGlow,
                            Color.Transparent,
                        ),
                        center = Offset(glowX2, glowY2),
                        radius = radius * 0.4f,
                    ),
                    radius = radius * 0.4f,
                    center = Offset(glowX2, glowY2),
                )
            }
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.08f),
                        Color.Transparent,
                        secondaryGlow.copy(alpha = 0.06f),
                    ),
                ),
            )
            .padding(borderWidth)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
    ) {
        content()
    }
}

/**
 * Subtle gradient border for input fields — inspired by the Uiverse search input.
 * On focus, the border subtly glows. At rest, it's nearly invisible.
 */
@Composable
fun SlateGlowBorder(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "border_glow")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "border_angle",
    )

    val shape = RoundedCornerShape(cornerRadius)
    val borderAlpha = if (active) 0.4f else 0.1f

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFA8C4D4).copy(alpha = borderAlpha),
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xFF4A6B7A).copy(alpha = borderAlpha * 0.7f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(1.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        content()
    }
}
