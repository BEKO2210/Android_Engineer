package dev.slate.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.slate.ai.core.data.repository.ChatRepository
import dev.slate.ai.core.datastore.SlatePreferences
import dev.slate.ai.core.database.entity.MessageEntity
import dev.slate.ai.download.engine.ModelDownloadManager
import dev.slate.ai.inference.llamacpp.InferenceParams
import dev.slate.ai.inference.llamacpp.InferenceState
import dev.slate.ai.inference.llamacpp.LlamaCppEngine
import dev.slate.ai.inference.llamacpp.ModelLoadParams
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadedModelInfo(
    val id: String,
    val name: String,
)

data class ChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val isComplete: Boolean,
    val isStreaming: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val engine: LlamaCppEngine,
    private val chatRepository: ChatRepository,
    private val downloadManager: ModelDownloadManager,
    private val preferences: SlatePreferences,
) : ViewModel() {

    val inferenceState: StateFlow<InferenceState> = engine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InferenceState.Idle)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Downloaded models for the idle screen
    val downloadedModels: StateFlow<List<DownloadedModelInfo>> = downloadManager
        .observeAllDownloads()
        .map { list ->
            list.filter { it.status == "COMPLETE" }
                .map { DownloadedModelInfo(it.modelId, it.modelName) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var conversationId: String? = null
    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId.asStateFlow()
    private var generationJob: Job? = null
    private var currentAssistantMessageId: String? = null
    private var streamingBuffer = StringBuilder()
    private var chatHistoryEnabled = true

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            _statusMessage.value = "Looking for model..."
            val file = downloadManager.getModelFile(modelId)
            if (file == null) {
                _statusMessage.value = "Model not downloaded yet."
                return@launch
            }

            _statusMessage.value = "Loading model into memory..."
            val result = engine.loadModel(file.absolutePath, ModelLoadParams())
            result.fold(
                onSuccess = {
                    // If switching models, start a new conversation
                    if (_currentModelId.value != null && _currentModelId.value != modelId) {
                        conversationId = null
                        _messages.value = emptyList()
                    }
                    _currentModelId.value = modelId
                    _statusMessage.value = "Ready"

                    // Load existing conversation if any
                    loadExistingConversation(modelId)
                },
                onFailure = { _statusMessage.value = "Failed: ${it.message}" },
            )
        }
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        if (inferenceState.value !is InferenceState.Ready) return

        // Check if the model file still exists (may have been deleted from storage screen)
        _currentModelId.value?.let { modelId ->
            viewModelScope.launch {
                val file = downloadManager.getModelFile(modelId)
                if (file == null) {
                    _statusMessage.value = "Model file was deleted. Please reload or download again."
                    engine.unload()
                    return@launch
                }
                doSendMessage(text)
            }
        } ?: viewModelScope.launch { doSendMessage(text) }
    }

    private suspend fun doSendMessage(text: String) {
        _inputText.value = ""

        chatHistoryEnabled = preferences.isChatHistoryEnabled.first()
        val shouldPersist = chatHistoryEnabled

        viewModelScope.launch {
            // Create conversation if needed (only if persisting)
            if (shouldPersist && conversationId == null) {
                conversationId = chatRepository.createConversation(
                    modelId = _currentModelId.value ?: "unknown",
                    title = text.take(50),
                )
            }

            val convId = conversationId

            // Add user message
            val userMsgId = if (shouldPersist && convId != null) {
                chatRepository.addUserMessage(convId, text)
            } else {
                java.util.UUID.randomUUID().toString()
            }
            addMessageToUi(ChatMessage(userMsgId, "user", text, true))

            // Start generation
            startGeneration(convId, buildPrompt())
        }
    }

    fun stopGeneration() {
        engine.stopGeneration()
        generationJob?.cancel()
        generationJob = null

        // Mark current assistant message as complete with whatever we have
        viewModelScope.launch {
            currentAssistantMessageId?.let { msgId ->
                val content = cleanOutput(streamingBuffer.toString())
                if (chatHistoryEnabled && conversationId != null) {
                    chatRepository.updateMessageContent(msgId, content)
                    chatRepository.markMessageComplete(msgId)
                }
                updateLastAssistantMessage(content, isComplete = true, isStreaming = false)
            }
            currentAssistantMessageId = null
            _statusMessage.value = "Stopped"
        }
    }

    fun regenerate() {
        if (inferenceState.value !is InferenceState.Ready) return

        viewModelScope.launch {
            // Remove last assistant message from UI
            val lastAssistant = _messages.value.lastOrNull { it.role == "assistant" }
            if (lastAssistant != null) {
                if (chatHistoryEnabled && conversationId != null) {
                    chatRepository.deleteMessage(lastAssistant.id)
                }
                _messages.value = _messages.value.filter { it.id != lastAssistant.id }
            }

            // Re-generate
            startGeneration(conversationId, buildPrompt())
        }
    }

    fun clearConversation() {
        viewModelScope.launch {
            conversationId?.let { chatRepository.clearConversation(it) }
            conversationId = null
            _messages.value = emptyList()
            _statusMessage.value = "Conversation cleared"
        }
    }

    fun unloadModel() {
        viewModelScope.launch {
            generationJob?.cancel()
            engine.unload()
            _currentModelId.value = null
            _statusMessage.value = "Model unloaded"
        }
    }

    private fun startGeneration(convId: String?, prompt: String) {
        generationJob?.cancel()
        val persist = chatHistoryEnabled && convId != null

        generationJob = viewModelScope.launch {
            // Add placeholder assistant message
            val assistantMsgId = if (persist) {
                chatRepository.addAssistantMessage(convId!!)
            } else {
                java.util.UUID.randomUUID().toString()
            }
            currentAssistantMessageId = assistantMsgId
            streamingBuffer.clear()

            addMessageToUi(ChatMessage(assistantMsgId, "assistant", "", false, true))
            _statusMessage.value = "Generating..."

            try {
                var shouldStop = false
                engine.generateStream(prompt).collect { token ->
                    if (shouldStop) return@collect

                    streamingBuffer.append(token)

                    // Check if output contains any stop pattern
                    val current = streamingBuffer.toString()
                    val stopPatterns = listOf(
                        "<|im_end|>", "<|im_start|>", "<|end|>", "<|user|>", "<|system|>",
                        "</s>", "<|endoftext|>",
                        "\nUser:", "\nHuman:", "\nSystem:",
                    )
                    for (pattern in stopPatterns) {
                        val idx = current.indexOf(pattern)
                        if (idx >= 0) {
                            streamingBuffer.clear()
                            streamingBuffer.append(current.substring(0, idx))
                            shouldStop = true
                            engine.stopGeneration()
                            break
                        }
                    }

                    // Show cleaned text during streaming
                    val display = cleanOutput(streamingBuffer.toString())
                    updateLastAssistantMessage(display, isComplete = false, isStreaming = true)
                }

                // Generation complete — clean up output
                val finalContent = cleanOutput(streamingBuffer.toString())
                if (persist) {
                    chatRepository.updateMessageContent(assistantMsgId, finalContent)
                    chatRepository.markMessageComplete(assistantMsgId)
                }
                updateLastAssistantMessage(finalContent, isComplete = true, isStreaming = false)
                _statusMessage.value = "Done"

            } catch (e: Exception) {
                val partial = cleanOutput(streamingBuffer.toString())
                if (partial.isNotEmpty()) {
                    if (persist) {
                        chatRepository.updateMessageContent(assistantMsgId, partial)
                        chatRepository.markMessageComplete(assistantMsgId)
                    }
                    updateLastAssistantMessage(partial, isComplete = true, isStreaming = false)
                } else {
                    if (persist) chatRepository.deleteMessage(assistantMsgId)
                    _messages.value = _messages.value.filter { it.id != assistantMsgId }
                }
                _statusMessage.value = "Error: ${e.message}"
            } finally {
                currentAssistantMessageId = null
                generationJob = null
            }
        }
    }

    private fun buildPrompt(): String {
        val sb = StringBuilder()
        val modelId = _currentModelId.value ?: ""

        // Use the correct chat template per model family
        val isPhi = modelId.contains("phi", ignoreCase = true)

        // Qwen and SmolLM2 use <|im_start|>/<|im_end|>, Phi uses <|role|>/<|end|>
        if (isPhi) {
            buildPhiPrompt(sb)
        } else {
            buildImPrompt(sb)  // Works for Qwen, SmolLM2, Llama
        }

        return sb.toString()
    }

    /** Qwen / SmolLM2 / Llama chat template */
    private fun buildImPrompt(sb: StringBuilder) {
        sb.append("<|im_start|>system\n")
        sb.append("You are Slate, a helpful and concise AI assistant. ")
        sb.append("Answer directly. Be concise. Use the same language as the user.")
        sb.append("<|im_end|>\n")

        appendHistory(sb, "<|im_start|>user\n", "<|im_end|>\n", "<|im_start|>assistant\n", "<|im_end|>\n")

        sb.append("<|im_start|>assistant\n")
    }

    /** Phi-3 chat template */
    private fun buildPhiPrompt(sb: StringBuilder) {
        sb.append("<|system|>\nYou are Slate, a helpful and concise AI assistant. ")
        sb.append("Answer directly. Be concise. Use the same language as the user.<|end|>\n")

        appendHistory(sb, "<|user|>\n", "<|end|>\n", "<|assistant|>\n", "<|end|>\n")

        sb.append("<|assistant|>\n")
    }

    private fun appendHistory(
        sb: StringBuilder,
        userStart: String, userEnd: String,
        assistantStart: String, assistantEnd: String,
    ) {
        val maxPromptChars = 4000
        val msgs = _messages.value.filter { it.isComplete || it.role == "user" }

        val historyLines = mutableListOf<String>()
        var historyChars = 0

        for (msg in msgs.reversed()) {
            val content = msg.content.take(400)
            val line = when (msg.role) {
                "user" -> "$userStart$content$userEnd"
                "assistant" -> "$assistantStart$content$assistantEnd"
                else -> continue
            }
            if (historyChars + line.length + sb.length > maxPromptChars) break
            historyLines.add(0, line)
            historyChars += line.length
        }

        historyLines.forEach { sb.append(it) }
    }

    private fun cleanOutput(text: String): String {
        var cleaned = text

        // Remove control tokens from all model families
        val artifacts = listOf(
            "<|im_start|>", "<|im_end|>",
            "<|system|>", "<|user|>", "<|assistant|>", "<|end|>",
            "</s>", "<|endoftext|>",
            "### Instruction:", "### Conversation:", "### Response:",
        )
        for (artifact in artifacts) {
            cleaned = cleaned.replace(artifact, "")
        }

        // Cut at simulated conversation
        val cutPatterns = listOf(
            "\nUser:", "\nHuman:", "\nSystem:", "\nAssistant:", "\nSlate:",
            "\n### ", "\n<|im_start|>", "\n<|user|>", "\n<|system|>",
        )
        for (pattern in cutPatterns) {
            val idx = cleaned.indexOf(pattern)
            if (idx > 0) {
                cleaned = cleaned.substring(0, idx)
            }
        }

        // Remove role labels at the very start
        cleaned = cleaned.trimStart()
        val startLabels = listOf("assistant\n", "system\n", "user\n")
        for (label in startLabels) {
            if (cleaned.startsWith(label)) {
                cleaned = cleaned.removePrefix(label)
            }
        }

        return cleaned.trim()
    }

    private fun addMessageToUi(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    private fun updateLastAssistantMessage(content: String, isComplete: Boolean, isStreaming: Boolean) {
        val id = currentAssistantMessageId ?: return
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) msg.copy(content = content, isComplete = isComplete, isStreaming = isStreaming)
            else msg
        }
    }

    private suspend fun loadExistingConversation(modelId: String) {
        val conversations = chatRepository.observeConversations().first()
        val conv = conversations.firstOrNull { it.modelId == modelId }
        if (conv != null) {
            conversationId = conv.id
            val msgs = chatRepository.getMessages(conv.id)
            _messages.value = msgs.map { entity ->
                ChatMessage(
                    id = entity.id,
                    role = entity.role,
                    content = entity.content,
                    isComplete = entity.isComplete,
                )
            }
        }
    }
}
