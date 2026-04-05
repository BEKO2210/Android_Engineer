package dev.slate.ai.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.ui.component.SlateCard
import dev.slate.ai.core.ui.component.SlateEmptyState
import dev.slate.ai.core.ui.component.SlatePrimaryButton
import dev.slate.ai.core.ui.component.SlateRippleLoader
import dev.slate.ai.core.ui.component.SlateStatusChip
import dev.slate.ai.core.ui.component.SlateChipStyle
import dev.slate.ai.core.ui.component.SlateTextField
import dev.slate.ai.core.ui.component.SlateTextButton
import dev.slate.ai.inference.llamacpp.InferenceState

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val generatedText by viewModel.generatedText.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Slate",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            SlateStatusChip(
                text = when (inferenceState) {
                    is InferenceState.Idle -> "No model"
                    is InferenceState.Loading -> "Loading..."
                    is InferenceState.Ready -> "Ready"
                    is InferenceState.Generating -> "Generating"
                    is InferenceState.Error -> "Error"
                },
                style = when (inferenceState) {
                    is InferenceState.Idle -> SlateChipStyle.DEFAULT
                    is InferenceState.Loading -> SlateChipStyle.WARNING
                    is InferenceState.Ready -> SlateChipStyle.SUCCESS
                    is InferenceState.Generating -> SlateChipStyle.INFO
                    is InferenceState.Error -> SlateChipStyle.ERROR
                },
                showDot = true,
            )
        }

        // Status message
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // Content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            when (inferenceState) {
                is InferenceState.Idle -> {
                    Spacer(Modifier.height(80.dp))
                    SlateEmptyState(
                        icon = Icons.Default.ChatBubble,
                        title = "No model loaded",
                        description = "Download a model from the Models tab, then load it here to start chatting.",
                    ) {
                        // Quick load buttons for downloaded models
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SlatePrimaryButton(
                                text = "Load SmolLM2 1.7B",
                                onClick = { viewModel.loadModel("smollm2-1.7b-q4") },
                            )
                            Spacer(Modifier.height(8.dp))
                            SlateTextButton(
                                text = "Load Qwen 2.5 3B",
                                onClick = { viewModel.loadModel("qwen-2.5-3b-q4") },
                            )
                        }
                    }
                }

                is InferenceState.Loading -> {
                    Spacer(Modifier.height(80.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SlateRippleLoader(label = "Loading model")
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "This may take a moment...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is InferenceState.Ready, is InferenceState.Generating -> {
                    if (generatedText.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        SlateCard {
                            Text(
                                text = generatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    if (inferenceState is InferenceState.Ready && generatedText.isEmpty()) {
                        Spacer(Modifier.height(80.dp))
                        SlateEmptyState(
                            icon = Icons.Default.ChatBubble,
                            title = "Model ready",
                            description = "Type a prompt below to generate a response.",
                        )
                    }
                }

                is InferenceState.Error -> {
                    Spacer(Modifier.height(40.dp))
                    SlateEmptyState(
                        icon = Icons.Default.ChatBubble,
                        title = "Error",
                        description = (inferenceState as InferenceState.Error).message,
                    ) {
                        SlateTextButton(
                            text = "Unload model",
                            onClick = { viewModel.unloadModel() },
                        )
                    }
                }
            }
        }

        // Input area — visible when model is loaded
        if (inferenceState is InferenceState.Ready || inferenceState is InferenceState.Generating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlateTextField(
                    value = inputText,
                    onValueChange = viewModel::updateInput,
                    placeholder = "Type a prompt...",
                    singleLine = false,
                    maxLines = 4,
                    imeAction = ImeAction.Send,
                    onImeAction = { viewModel.generate() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (inferenceState is InferenceState.Generating) {
                    IconButton(onClick = { viewModel.stopGeneration() }) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop generation",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.generate() },
                        enabled = inputText.isNotBlank(),
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Unload button when model is loaded
        if (inferenceState is InferenceState.Ready) {
            SlateTextButton(
                text = "Unload model",
                onClick = { viewModel.unloadModel() },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}
