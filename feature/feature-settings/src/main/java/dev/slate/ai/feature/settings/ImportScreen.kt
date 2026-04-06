package dev.slate.ai.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.ui.component.SlateCard
import dev.slate.ai.core.ui.component.SlatePrimaryButton
import dev.slate.ai.core.ui.component.SlateRippleLoader
import dev.slate.ai.core.ui.component.SlateTopBar

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.importState.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) viewModel.importGgufFile(uri)
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SlateTopBar(
            title = "Import Model",
            navigationIcon = Icons.Default.ArrowBack,
            onNavigationClick = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            // Format info
            Text(
                text = "Import your own AI model",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Slate supports GGUF model files. You can download models from Hugging Face or other sources and import them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            // Supported format card
            SlateCard {
                Text(
                    text = "Supported format",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                FormatRow("Format", "GGUF (.gguf)")
                FormatRow("Quantization", "Q4_K_M recommended")
                FormatRow("Size", "Up to 4 GB")
                FormatRow("RAM needed", "~1.3x file size")
            }

            Spacer(Modifier.height(16.dp))

            // Warning card
            SlateCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.padding(start = 8.dp))
                    Column {
                        Text(
                            text = "Important",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFB74D),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "• Only import models from trusted sources\n" +
                                    "• Large models (>2 GB) need devices with 6+ GB RAM\n" +
                                    "• The file will be copied to app storage\n" +
                                    "• Not all GGUF models are compatible with every device\n" +
                                    "• Slate does not verify the safety of imported models",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Import button / state
            when (val s = state) {
                is ImportState.Idle -> {
                    SlatePrimaryButton(
                        text = "Select .gguf file",
                        onClick = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) },
                        icon = Icons.Default.FileOpen,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is ImportState.Importing -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SlateRippleLoader(label = "Importing...")
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Copying file to app storage...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is ImportState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "\"${s.modelName}\" imported",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Go to Chat and load it from there.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        SlatePrimaryButton(
                            text = "Import another",
                            onClick = {
                                viewModel.clearState()
                                filePicker.launch(arrayOf("application/octet-stream", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is ImportState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(12.dp))
                        SlatePrimaryButton(
                            text = "Try again",
                            onClick = {
                                viewModel.clearState()
                                filePicker.launch(arrayOf("application/octet-stream", "*/*"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
