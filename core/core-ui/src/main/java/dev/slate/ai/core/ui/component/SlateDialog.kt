package dev.slate.ai.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SlateDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    SlateTextButton(
                        text = dismissText,
                        onClick = onDismiss,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isDestructive) {
                        SlatePrimaryButton(
                            text = confirmText,
                            onClick = onConfirm,
                        )
                    } else {
                        SlatePrimaryButton(
                            text = confirmText,
                            onClick = onConfirm,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SlateDeleteDialog(
    modelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    SlateDialog(
        title = "Delete model",
        message = "Remove \"$modelName\" from this device? The model file will be permanently deleted to free up storage.",
        confirmText = "Delete",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
    )
}

@Composable
fun SlateClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    SlateDialog(
        title = "Clear chat history",
        message = "All conversations will be permanently deleted. This cannot be undone.",
        confirmText = "Clear all",
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
    )
}

@Composable
fun SlateStorageWarningDialog(
    requiredSize: String,
    availableSize: String,
    onDismiss: () -> Unit,
) {
    SlateDialog(
        title = "Not enough storage",
        message = "This model requires $requiredSize of free space, but only $availableSize is available. Free up storage and try again.",
        confirmText = "OK",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
    )
}
