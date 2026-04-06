package dev.slate.ai.core.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun SlateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200),
        label = "border",
    )
    val glowAlpha = if (isFocused) 0.12f else 0f

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = accentColor.copy(alpha = glowAlpha),
                        cornerRadius = CornerRadius(18.dp.toPx()),
                        size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                        topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                    )
                }
            },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = borderColor,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onSend = { onImeAction?.invoke() },
        ),
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
