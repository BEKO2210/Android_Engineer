package dev.slate.ai.feature.chat

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.ui.component.SlateChipStyle
import dev.slate.ai.core.ui.component.SlateEmptyState
import dev.slate.ai.core.ui.component.SlatePrimaryButton
import dev.slate.ai.core.ui.component.SlatePulsingDot
import dev.slate.ai.core.ui.component.SlateRippleLoader
import dev.slate.ai.core.ui.component.SlateStatusChip
import dev.slate.ai.core.ui.component.SlateTextButton
import dev.slate.ai.core.ui.component.SlateTextField
import dev.slate.ai.inference.llamacpp.InferenceState

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val downloadedModels by viewModel.downloadedModels.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new content arrives during streaming
    val lastMessageContent = messages.lastOrNull()?.content?.length ?: 0
    val isStreaming = messages.lastOrNull()?.isStreaming == true
    LaunchedEffect(lastMessageContent, messages.size) {
        if (messages.isNotEmpty()) {
            // Scroll to the last item and push it as far up as possible
            listState.animateScrollToItem(
                index = messages.size - 1,
                scrollOffset = if (isStreaming) Int.MAX_VALUE / 2 else 0,
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding(),
    ) {
        // Top bar
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

        // Messages or empty state
        Box(modifier = Modifier.weight(1f)) {
            when {
                inferenceState is InferenceState.Idle -> {
                    IdleState(
                        downloadedModels = downloadedModels,
                        onLoadModel = viewModel::loadModel,
                    )
                }
                inferenceState is InferenceState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SlateRippleLoader(label = "Loading model")
                    }
                }
                messages.isEmpty() && inferenceState is InferenceState.Ready -> {
                    SlateEmptyState(
                        icon = Icons.Default.ChatBubble,
                        title = "Start a conversation",
                        description = "Type a message below to begin.",
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(message = msg)
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }

        // Action bar (regenerate, clear) — shown when there are messages and model is ready
        val canRegenerate = messages.isNotEmpty() &&
            inferenceState is InferenceState.Ready &&
            messages.lastOrNull()?.role == "assistant"

        AnimatedVisibility(visible = canRegenerate || messages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (canRegenerate) {
                    SlateTextButton(
                        text = "Regenerate",
                        onClick = { viewModel.regenerate() },
                    )
                }
                Spacer(Modifier.weight(1f))
                if (messages.isNotEmpty() && inferenceState is InferenceState.Ready) {
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear conversation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Input bar — visible when model is loaded
        if (inferenceState is InferenceState.Ready || inferenceState is InferenceState.Generating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                SlateTextField(
                    value = inputText,
                    onValueChange = viewModel::updateInput,
                    placeholder = "Message...",
                    singleLine = false,
                    maxLines = 4,
                    imeAction = ImeAction.Send,
                    onImeAction = { viewModel.sendMessage() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (inferenceState is InferenceState.Generating) {
                    IconButton(onClick = { viewModel.stopGeneration() }) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.sendMessage() },
                        enabled = inputText.isNotBlank(),
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Unload button
        if (inferenceState is InferenceState.Ready || inferenceState is InferenceState.Error) {
            SlateTextButton(
                text = "Unload model",
                onClick = { viewModel.unloadModel() },
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
private fun IdleState(
    downloadedModels: List<DownloadedModelInfo>,
    onLoadModel: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Slate",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Private AI on your device",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))

        if (downloadedModels.isEmpty()) {
            Text(
                text = "No models downloaded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Go to the Models tab to download one.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = "Select a model to start",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            downloadedModels.forEachIndexed { index, model ->
                if (index == 0) {
                    SlatePrimaryButton(
                        text = "Load ${model.name}",
                        onClick = { onLoadModel(model.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    SlateTextButton(
                        text = "Load ${model.name}",
                        onClick = { onLoadModel(model.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = if (isUser) 280.dp else 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp,
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .padding(12.dp),
        ) {
            if (message.content.isEmpty() && message.isStreaming) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SlatePulsingDot(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    SlatePulsingDot(color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    SlatePulsingDot(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                SimpleMarkdownText(
                    text = message.content,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )

                // Copy button — only on completed messages
                if (message.isComplete && !message.isStreaming && message.content.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("message", message.content))
                                copied = true
                                scope.launch {
                                    kotlinx.coroutines.delay(1500)
                                    copied = false
                                }
                            },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = if (copied) "Copied" else "Copy message",
                                tint = if (copied) Color(0xFF81C784)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
