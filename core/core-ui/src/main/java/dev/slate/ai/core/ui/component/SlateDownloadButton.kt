package dev.slate.ai.core.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed interface DownloadButtonState {
    data object NotDownloaded : DownloadButtonState
    data class Downloading(
        val progress: Float,
        val downloadedSize: String,
        val totalSize: String,
        val speedText: String? = null,
    ) : DownloadButtonState
    data object Paused : DownloadButtonState
    data object Verifying : DownloadButtonState
    data object Completed : DownloadButtonState
    data class Error(val message: String) : DownloadButtonState
}

@Composable
fun SlateDownloadButton(
    state: DownloadButtonState,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.95f))
                    .togetherWith(fadeOut() + scaleOut(targetScale = 0.95f))
            },
            label = "download_state",
        ) { currentState ->
            when (currentState) {
                is DownloadButtonState.NotDownloaded -> {
                    SlatePrimaryButton(
                        text = "Download",
                        onClick = onDownload,
                        icon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is DownloadButtonState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SlateDownloadProgress(
                            progress = currentState.progress,
                            downloadedSize = currentState.downloadedSize,
                            totalSize = currentState.totalSize,
                            speedText = currentState.speedText,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(
                                onClick = onPause,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = "Pause download",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = onCancel,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel download",
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                is DownloadButtonState.Paused -> {
                    SlateOutlinedButton(
                        text = "Resume",
                        onClick = onResume,
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is DownloadButtonState.Verifying -> {
                    SlatePrimaryButton(
                        text = "Verifying integrity",
                        onClick = {},
                        loading = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                is DownloadButtonState.Completed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Ready to use",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                is DownloadButtonState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = currentState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        SlateOutlinedButton(
                            text = "Retry",
                            onClick = onRetry,
                            icon = Icons.Default.Refresh,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
