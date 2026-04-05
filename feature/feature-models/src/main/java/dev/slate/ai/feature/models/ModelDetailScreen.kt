package dev.slate.ai.feature.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.common.formatFileSize
import dev.slate.ai.core.model.LlmModel
import dev.slate.ai.core.model.ModelTier
import dev.slate.ai.core.ui.component.DownloadButtonState
import dev.slate.ai.core.ui.component.SlateCard
import dev.slate.ai.core.ui.component.SlateChipStyle
import dev.slate.ai.core.ui.component.SlateDownloadButton
import dev.slate.ai.core.ui.component.SlateGlowCard
import dev.slate.ai.core.ui.component.SlateStorageWarningDialog
import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.core.ui.component.SlateRippleLoader
import dev.slate.ai.core.ui.component.SlateStatusChip
import dev.slate.ai.core.ui.component.SlateTopBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ModelDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvent.collectAsStateWithLifecycle()

    var showStorageDialog by remember { mutableStateOf(false) }

    if (errorEvent == "NOT_ENOUGH_STORAGE") {
        SlateStorageWarningDialog(
            requiredSize = (uiState as? ModelDetailUiState.Success)?.model?.sizeBytes?.formatFileSize() ?: "",
            availableSize = "insufficient",
            onDismiss = {
                viewModel.clearError()
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SlateTopBar(
            title = "Model Details",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
        )

        when (val state = uiState) {
            is ModelDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    SlateRippleLoader(label = "Loading")
                }
            }

            is ModelDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is ModelDetailUiState.Success -> {
                ModelDetailContent(
                    model = state.model,
                    downloadEntity = downloadState,
                    onDownload = viewModel::startDownload,
                    onPause = viewModel::pauseDownload,
                    onResume = viewModel::resumeDownload,
                    onCancel = viewModel::cancelDownload,
                    onRetry = viewModel::retryDownload,
                )
            }
        }
    }
}

@ExperimentalLayoutApi
@Composable
private fun ModelDetailContent(
    model: LlmModel,
    downloadEntity: DownloadEntity?,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        // Hero section with glow
        SlateGlowCard(
            animated = true,
            glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = model.parameterCount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = model.family,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SlateStatusChip(
                        text = when (model.tier) {
                            ModelTier.TINY -> "Tiny"
                            ModelTier.BALANCED -> "Balanced"
                            ModelTier.HEAVY -> "Heavy"
                        },
                        style = when (model.tier) {
                            ModelTier.TINY -> SlateChipStyle.SUCCESS
                            ModelTier.BALANCED -> SlateChipStyle.INFO
                            ModelTier.HEAVY -> SlateChipStyle.WARNING
                        },
                    )
                    SlateStatusChip(
                        text = model.license,
                        style = SlateChipStyle.DEFAULT,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Description
        Text(
            text = model.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // Specs grid
        SlateCard {
            SpecRow(icon = Icons.Default.Storage, label = "Size", value = model.sizeBytes.formatFileSize())
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SpecRow(icon = Icons.Default.Memory, label = "Min RAM", value = "${model.minRamMb / 1024} GB")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SpecRow(icon = Icons.Default.Scale, label = "Quantization", value = model.quantization)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SpecRow(icon = Icons.Default.Settings, label = "Context", value = "${model.contextLength} tokens")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SpecRow(icon = Icons.Default.Security, label = "License", value = model.license)
        }

        Spacer(Modifier.height(20.dp))

        // Capabilities
        if (model.capabilities.isNotEmpty()) {
            Text(
                text = "Capabilities",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                model.capabilities.forEach { cap ->
                    SlateStatusChip(text = cap, style = SlateChipStyle.INFO)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Device recommendation
        if (model.deviceRecommendation.isNotEmpty()) {
            SlateCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = model.deviceRecommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Download button — wired to real download engine
        val buttonState = mapDownloadState(downloadEntity)
        SlateDownloadButton(
            state = buttonState,
            onDownload = onDownload,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
            onRetry = onRetry,
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SpecRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun mapDownloadState(entity: DownloadEntity?): DownloadButtonState {
    if (entity == null) return DownloadButtonState.NotDownloaded

    return when (entity.status) {
        "QUEUED" -> DownloadButtonState.Downloading(
            progress = 0f,
            downloadedSize = "Queued",
            totalSize = formatBytes(entity.totalBytes),
        )
        "DOWNLOADING" -> {
            val progress = if (entity.totalBytes > 0) {
                entity.downloadedBytes.toFloat() / entity.totalBytes.toFloat()
            } else 0f
            DownloadButtonState.Downloading(
                progress = progress,
                downloadedSize = formatBytes(entity.downloadedBytes),
                totalSize = formatBytes(entity.totalBytes),
            )
        }
        "PAUSED" -> DownloadButtonState.Paused
        "VERIFYING" -> DownloadButtonState.Verifying
        "COMPLETE" -> DownloadButtonState.Completed
        "FAILED" -> DownloadButtonState.Error(
            message = entity.errorMessage ?: "Download failed",
        )
        else -> DownloadButtonState.NotDownloaded
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val size = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format("%.1f %s", size, units[digitGroups.coerceIn(0, 3)])
}
