package dev.slate.ai.core.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.surfaceContainerHigh,
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_translate",
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim),
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = 16.dp,
) {
    val brush = shimmerBrush()
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(brush),
    )
}

@Composable
fun ShimmerModelCard(
    modifier: Modifier = Modifier,
) {
    SlateCard(modifier = modifier) {
        Row {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush()),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(width = 140.dp, height = 18.dp)
                Spacer(Modifier.height(8.dp))
                ShimmerBox(width = 200.dp, height = 14.dp)
                Spacer(Modifier.height(8.dp))
                ShimmerBox(width = 100.dp, height = 12.dp)
            }
        }
        Spacer(Modifier.height(16.dp))
        ShimmerBox(height = 8.dp)
        Spacer(Modifier.height(12.dp))
        Row {
            ShimmerBox(width = 60.dp, height = 24.dp)
            Spacer(Modifier.width(8.dp))
            ShimmerBox(width = 80.dp, height = 24.dp)
        }
    }
}

@Composable
fun ShimmerChatMessage(
    isUser: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val alignment = if (isUser) Modifier.padding(start = 48.dp) else Modifier.padding(end = 48.dp)
    Column(modifier = modifier.then(alignment)) {
        ShimmerBox(height = 14.dp)
        Spacer(Modifier.height(6.dp))
        ShimmerBox(width = 200.dp, height = 14.dp)
        if (!isUser) {
            Spacer(Modifier.height(6.dp))
            ShimmerBox(width = 160.dp, height = 14.dp)
        }
    }
}
