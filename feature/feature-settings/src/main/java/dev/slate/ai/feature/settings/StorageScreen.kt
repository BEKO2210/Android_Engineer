package dev.slate.ai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.common.formatFileSize
import dev.slate.ai.core.ui.component.SlateCard
import dev.slate.ai.core.ui.component.SlateDeleteDialog
import dev.slate.ai.core.ui.component.SlateEmptyState
import dev.slate.ai.core.ui.component.SlateTopBar

@Composable
fun StorageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = hiltViewModel(),
) {
    val models by viewModel.downloadedModels.collectAsStateWithLifecycle()
    val totalSize by viewModel.totalSize.collectAsStateWithLifecycle()
    val availableSize by viewModel.availableSize.collectAsStateWithLifecycle()
    var modelToDelete by remember { mutableStateOf<String?>(null) }

    if (modelToDelete != null) {
        val name = models.find { it.modelId == modelToDelete }?.modelName ?: "this model"
        SlateDeleteDialog(
            modelName = name,
            onConfirm = {
                viewModel.deleteModel(modelToDelete!!)
                modelToDelete = null
            },
            onDismiss = { modelToDelete = null },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SlateTopBar(
            title = "Storage",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
        )

        // Summary
        SlateCard(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Models",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = totalSize.formatFileSize(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = availableSize.formatFileSize(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        if (models.isEmpty()) {
            SlateEmptyState(
                icon = Icons.Default.Storage,
                title = "No downloaded models",
                description = "Downloaded models will appear here.",
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp, vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(models, key = { it.modelId }) { model ->
                    SlateCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.modelName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = model.totalBytes.formatFileSize(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = { modelToDelete = model.modelId }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${model.modelName}",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
