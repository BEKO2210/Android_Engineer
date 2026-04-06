package dev.slate.ai.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.common.formatFileSize
import dev.slate.ai.core.ui.component.SlateClearHistoryDialog

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToImport: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionResult by viewModel.actionResult.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        SlateClearHistoryDialog(
            onConfirm = {
                viewModel.clearChatHistory()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 16.dp),
        )

        // Appearance
        SectionHeader("Appearance")
        SettingsToggle(
            icon = Icons.Default.Brightness4,
            title = "Dark theme",
            description = "Use dark color scheme",
            checked = state.isDarkTheme,
            onCheckedChange = viewModel::setDarkTheme,
        )
        SettingsDivider()

        // Privacy
        SectionHeader("Privacy")
        SettingsToggle(
            icon = Icons.Default.WifiOff,
            title = "Offline only",
            description = "Block all network access except model downloads",
            checked = state.isOfflineOnly,
            onCheckedChange = viewModel::setOfflineOnly,
        )
        SettingsToggle(
            icon = Icons.Default.History,
            title = "Save chat history",
            description = "Store conversations locally on this device",
            checked = state.isChatHistoryEnabled,
            onCheckedChange = viewModel::setChatHistoryEnabled,
        )
        SettingsDivider()

        // Data
        SectionHeader("Data")
        SettingsAction(
            icon = Icons.Default.Delete,
            title = "Clear chat history",
            description = "Delete all saved conversations",
            onClick = { showClearDialog = true },
        )
        SettingsAction(
            icon = Icons.Default.Storage,
            title = "Model storage",
            description = "${state.modelsStorageBytes.formatFileSize()} used",
            onClick = onNavigateToStorage,
            showChevron = true,
        )
        SettingsAction(
            icon = Icons.Default.FileOpen,
            title = "Import custom model",
            description = "Load your own .gguf file from device",
            onClick = onNavigateToImport,
            showChevron = true,
        )
        SettingsDivider()

        // About
        SectionHeader("About")
        SettingsAction(
            icon = Icons.Default.Security,
            title = "Privacy policy",
            description = "How Slate handles your data",
            onClick = onNavigateToPrivacy,
            showChevron = true,
        )
        val context = LocalContext.current
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
            } catch (e: Exception) { "?" }
        }
        SettingsAction(
            icon = Icons.Default.Info,
            title = "About Slate",
            description = "Version $versionName",
            onClick = {},
        )

        Spacer(Modifier.height(8.dp))

        // Footer
        Text(
            text = "Slate runs AI entirely on your device. No data leaves your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        // Action result feedback
        if (actionResult != null) {
            Text(
                text = actionResult!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LaunchedEffect(actionResult) {
                kotlinx.coroutines.delay(2000)
                viewModel.clearActionResult()
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsAction(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    showChevron: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
