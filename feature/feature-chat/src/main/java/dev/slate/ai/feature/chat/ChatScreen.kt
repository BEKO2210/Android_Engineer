package dev.slate.ai.feature.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.slate.ai.core.ui.component.SlateEmptyState
import dev.slate.ai.core.ui.component.SlatePrimaryButton
import dev.slate.ai.core.ui.component.SlateTextButton
import dev.slate.ai.core.ui.component.SlateTextField
import dev.slate.ai.core.ui.component.SlateThinkingIndicator
import dev.slate.ai.core.ui.theme.ModelAccent
import dev.slate.ai.inference.llamacpp.InferenceState
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val downloadedModels by viewModel.downloadedModels.collectAsStateWithLifecycle()
    val modelId by viewModel.currentModelId.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val accentColor = ModelAccent.forModelId(modelId)
    val modelName = ModelAccent.nameForModelId(modelId)
    val isReady = inferenceState is InferenceState.Ready
    val isGenerating = inferenceState is InferenceState.Generating
    val canRegenerate = isReady && messages.lastOrNull()?.role == "assistant"
    val isStreaming = messages.lastOrNull()?.isStreaming == true

    // Auto-scroll — only when message count changes or streaming starts/stops
    // NOT on every character (that causes flickering)
    LaunchedEffect(messages.size, isStreaming) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }
    // Smooth scroll to bottom periodically during streaming (every ~300ms via content length buckets)
    val scrollBucket = (messages.lastOrNull()?.content?.length ?: 0) / 80
    LaunchedEffect(scrollBucket) {
        if (isStreaming && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding(),
    ) {
        // === TOP BAR ===
        ChatTopBar(
            inferenceState = inferenceState,
            accentColor = accentColor,
            modelName = modelName,
            canRegenerate = canRegenerate,
            hasMessages = messages.isNotEmpty(),
            onRegenerate = viewModel::regenerate,
            onClear = viewModel::clearConversation,
            onUnload = viewModel::unloadModel,
        )

        // === CONTENT ===
        Box(modifier = Modifier.weight(1f)) {
            when {
                inferenceState is InferenceState.Idle -> {
                    IdleState(downloadedModels = downloadedModels, onLoadModel = viewModel::loadModel)
                }
                inferenceState is InferenceState.Loading -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SlateThinkingIndicator(size = 64.dp, accentColor = accentColor)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading model...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                inferenceState is InferenceState.Error -> {
                    ErrorState(
                        message = (inferenceState as InferenceState.Error).message,
                        onUnload = viewModel::unloadModel,
                    )
                }
                messages.isEmpty() && isReady -> {
                    // Conversation starters
                    ConversationStarterView(
                        starters = ChatViewModel.CONVERSATION_STARTERS,
                        accentColor = accentColor,
                        onSelect = { viewModel.sendConversationStarter(it) },
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageBubble(msg, accentColor, modelName, Modifier.animateItem())
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }

                    // Scroll-to-bottom FAB
                    val showFab by remember {
                        derivedStateOf {
                            val info = listState.layoutInfo
                            val last = info.visibleItemsInfo.lastOrNull()
                            last != null && last.index < info.totalItemsCount - 1
                        }
                    }
                    if (showFab) {
                        FloatingActionButton(
                            onClick = { scope.launch { listState.animateScrollToItem(messages.size - 1) } },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).size(40.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = accentColor,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp),
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Scroll to bottom", Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // === INPUT BAR ===
        if (isReady || isGenerating) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                    accentColor = accentColor,
                )
                Spacer(Modifier.width(8.dp))
                AnimatedSendButton(
                    isGenerating = isGenerating,
                    canSend = inputText.isNotBlank(),
                    accentColor = accentColor,
                    onSend = { viewModel.sendMessage() },
                    onStop = { viewModel.stopGeneration() },
                )
            }
        }
    }
}

// === CONVERSATION STARTERS ===
@Composable
private fun ConversationStarterView(
    starters: List<ConversationStarter>,
    accentColor: Color,
    onSelect: (ConversationStarter) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        SlateThinkingIndicator(size = 48.dp, accentColor = accentColor)
        Spacer(Modifier.height(16.dp))
        Text("Choose a language", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text("Slate will greet you and respond in that language", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        // Language chips
        val rows = starters.chunked(4)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                row.forEach { starter ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onSelect(starter) },
                        label = { Text(starter.langName, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Or type a message in any language below",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

// === TOP BAR ===
@Composable
private fun ChatTopBar(
    inferenceState: InferenceState,
    accentColor: Color,
    modelName: String,
    canRegenerate: Boolean,
    hasMessages: Boolean,
    onRegenerate: () -> Unit,
    onClear: () -> Unit,
    onUnload: () -> Unit,
) {
    val isLoaded = inferenceState is InferenceState.Ready || inferenceState is InferenceState.Generating || inferenceState is InferenceState.Error
    val animatedAccent by animateColorAsState(accentColor, tween(500), label = "accent")

    // Heartbeat glow animation for "Slate" text
    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = androidx.compose.animation.core.EaseInOutCubic), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "slateGlow",
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Logo icon
        val context = LocalContext.current
        val logoBitmap = remember {
            try {
                val stream = context.assets.open("Icon_logo.png")
                android.graphics.BitmapFactory.decodeStream(stream)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
        if (logoBitmap != null) {
            Image(
                bitmap = logoBitmap,
                contentDescription = "Slate",
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(10.dp))
        }

        // "Slate" with subtle blue glow heartbeat
        Text(
            text = "Slate",
            style = MaterialTheme.typography.titleLarge,
            color = Color(0xFF7EB8D0).copy(alpha = glowAlpha),
        )

        // Model indicator
        if (isLoaded && modelName.isNotEmpty()) {
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(animatedAccent))
            Spacer(Modifier.width(5.dp))
            Text(modelName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))

        // Action buttons — compact row
        if (canRegenerate) {
            IconButton(onClick = onRegenerate, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, "Regenerate", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (hasMessages && inferenceState is InferenceState.Ready) {
            IconButton(onClick = onClear, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Clear", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isLoaded) {
            IconButton(onClick = onUnload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.PowerSettingsNew, "Unload", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// === ERROR STATE ===
@Composable
private fun ErrorState(message: String, onUnload: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Could not load model", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        SlatePrimaryButton(text = "Unload and retry", onClick = onUnload)
    }
}

// === IDLE STATE ===
@Composable
private fun IdleState(downloadedModels: List<DownloadedModelInfo>, onLoadModel: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Slate", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text("Private AI on your device", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        if (downloadedModels.isEmpty()) {
            Text("No models downloaded yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("Go to the Models tab to download one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Select a model", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            downloadedModels.forEachIndexed { idx, model ->
                val color = ModelAccent.forModelId(model.id)
                if (idx == 0) {
                    SlatePrimaryButton(text = "Load ${model.name}", onClick = { onLoadModel(model.id) }, modifier = Modifier.fillMaxWidth())
                } else {
                    Spacer(Modifier.height(8.dp))
                    SlateTextButton(text = "Load ${model.name}", onClick = { onLoadModel(model.id) })
                }
            }
        }
    }
}

// === MESSAGE BUBBLE ===
@Composable
private fun MessageBubble(message: ChatMessage, accentColor: Color, modelName: String, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Model label above assistant messages
        if (!isUser && message.content.isNotEmpty()) {
            Text(
                text = if (modelName.isNotEmpty()) modelName else "Assistant",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (isUser) 280.dp else 340.dp)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 16.dp,
                    ))
                    .background(if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(12.dp),
            ) {
                if (message.content.isEmpty() && message.isStreaming) {
                    // Thinking entity
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SlateThinkingIndicator(size = 24.dp, accentColor = accentColor)
                        Spacer(Modifier.width(8.dp))
                        Text("Thinking...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SimpleMarkdownText(
                        text = message.content,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )

                    // Copy button
                    if (message.isComplete && !message.isStreaming && message.content.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                            IconButton(
                                onClick = {
                                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cb.setPrimaryClip(ClipData.newPlainText("message", message.content))
                                    copied = true
                                    scope.launch { kotlinx.coroutines.delay(1500); copied = false }
                                },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    if (copied) "Copied" else "Copy",
                                    Modifier.size(13.dp),
                                    tint = if (copied) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// === ANIMATED SEND/STOP BUTTON ===
@Composable
private fun AnimatedSendButton(
    isGenerating: Boolean,
    canSend: Boolean,
    accentColor: Color,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (canSend || isGenerating) 1f else 0.85f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "send_scale",
    )
    val iconColor by animateColorAsState(
        targetValue = when {
            isGenerating -> MaterialTheme.colorScheme.error
            canSend -> accentColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        },
        animationSpec = tween(200),
        label = "send_color",
    )

    IconButton(
        onClick = { if (isGenerating) onStop() else onSend() },
        enabled = isGenerating || canSend,
        modifier = Modifier.size(48.dp).scale(scale),
    ) {
        Icon(
            imageVector = if (isGenerating) Icons.Default.Stop else Icons.Default.Send,
            contentDescription = if (isGenerating) "Stop" else "Send",
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
