package dev.slate.ai.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Card with a subtle static gradient border.
 * No animation — stable and performant for lists.
 */
@Composable
fun SlateGlowCard(
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    secondaryGlow: Color = Color(0xFF4A6B7A).copy(alpha = 0.1f),
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 16.dp,
    animated: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor,
                        Color.Transparent,
                        secondaryGlow,
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
