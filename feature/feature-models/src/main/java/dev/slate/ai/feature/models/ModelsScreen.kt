package dev.slate.ai.feature.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.common.formatFileSize
import dev.slate.ai.core.model.LlmModel
import dev.slate.ai.core.model.ModelTier
import dev.slate.ai.core.ui.component.ShimmerModelCard
import dev.slate.ai.core.ui.component.SlateEmptyState
import dev.slate.ai.core.ui.component.SlateGlowCard
import dev.slate.ai.core.ui.component.SlateStatusChip
import dev.slate.ai.core.ui.component.SlateChipStyle

@Composable
fun ModelsScreen(
    modifier: Modifier = Modifier,
    onModelClick: (String) -> Unit = {},
    onImportClick: () -> Unit = {},
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Header
        Text(
            text = "Models",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
        )
        Text(
            text = "Download and run AI locally on your device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, bottom = 16.dp),
        )

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == ModelTier.TINY,
                    onClick = { viewModel.setFilter(ModelTier.TINY) },
                    label = { Text("Tiny") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == ModelTier.BALANCED,
                    onClick = { viewModel.setFilter(ModelTier.BALANCED) },
                    label = { Text("Balanced") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == ModelTier.HEAVY,
                    onClick = { viewModel.setFilter(ModelTier.HEAVY) },
                    label = { Text("Heavy") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }

        // Import own model button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            androidx.compose.material3.TextButton(onClick = onImportClick) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text("Import own model", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Content
        when (val state = uiState) {
            is ModelsUiState.Loading -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(3) { ShimmerModelCard() }
                }
            }

            is ModelsUiState.Success -> {
                val filtered = viewModel.getFilteredModels(state.models, selectedFilter)
                if (filtered.isEmpty()) {
                    SlateEmptyState(
                        icon = Icons.Default.Memory,
                        title = "No models in this category",
                        description = "Try selecting a different filter.",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filtered, key = { it.id }) { model ->
                            ModelCard(
                                model = model,
                                onClick = { onModelClick(model.id) },
                                isCompatible = viewModel.isModelCompatible(model),
                                hasStorage = viewModel.hasEnoughStorage(model),
                                isDownloaded = viewModel.isModelDownloaded(model.id, allDownloads),
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }

            is ModelsUiState.Error -> {
                SlateEmptyState(
                    icon = Icons.Default.Storage,
                    title = "Could not load models",
                    description = state.message,
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: LlmModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompatible: Boolean = true,
    hasStorage: Boolean = true,
    isDownloaded: Boolean = false,
) {
    SlateGlowCard(
        modifier = modifier,
        onClick = onClick,
        animated = false,
        glowColor = when (model.tier) {
            ModelTier.TINY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ModelTier.BALANCED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ModelTier.HEAVY -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Tier indicator dot
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = model.parameterCount,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = model.family,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                if (isDownloaded) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = model.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )

            Spacer(Modifier.height(12.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MetadataItem(
                    icon = Icons.Default.Storage,
                    text = model.sizeBytes.formatFileSize(),
                )
                MetadataItem(
                    icon = Icons.Default.Memory,
                    text = "${model.minRamMb / 1024} GB+ RAM",
                )
                MetadataItem(
                    icon = Icons.Default.Download,
                    text = model.quantization,
                )
            }

            // Capabilities
            if (model.capabilities.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    model.capabilities.take(3).forEach { cap ->
                        SlateStatusChip(
                            text = cap,
                            style = SlateChipStyle.DEFAULT,
                        )
                    }
                }
            }

            // Compatibility warnings
            if (!isCompatible) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your device may not have enough RAM for this model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (!hasStorage) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Not enough free storage for this model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MetadataItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
