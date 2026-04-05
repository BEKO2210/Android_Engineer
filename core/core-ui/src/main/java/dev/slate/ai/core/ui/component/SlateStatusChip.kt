package dev.slate.ai.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class SlateChipStyle {
    DEFAULT,
    SUCCESS,
    WARNING,
    ERROR,
    INFO,
}

@Composable
fun SlateStatusChip(
    text: String,
    style: SlateChipStyle = SlateChipStyle.DEFAULT,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
) {
    val (backgroundColor, contentColor) = when (style) {
        SlateChipStyle.DEFAULT -> Pair(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SlateChipStyle.SUCCESS -> Pair(
            Color(0xFF1B3D2F),
            Color(0xFF81C784),
        )
        SlateChipStyle.WARNING -> Pair(
            Color(0xFF3D3520),
            Color(0xFFFFD54F),
        )
        SlateChipStyle.ERROR -> Pair(
            Color(0xFF3D1A22),
            MaterialTheme.colorScheme.error,
        )
        SlateChipStyle.INFO -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(contentColor),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
