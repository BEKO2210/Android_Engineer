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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    private var conversationId: String? = null
    private var currentModelId: String? = null
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
                    if (currentModelId != null && currentModelId != modelId) {
                        conversationId = null
                        _messages.value = emptyList()
                    }
                    currentModelId = modelId
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
        currentModelId?.let { modelId ->
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
                    modelId = currentModelId ?: "unknown",
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
                val content = streamingBuffer.toString()
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
            currentModelId = null
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
                engine.generateStream(prompt).collect { token ->
                    streamingBuffer.append(token)
                    updateLastAssistantMessage(
                        streamingBuffer.toString(),
                        isComplete = false,
                        isStreaming = true,
                    )
                }

                // Generation complete
                val finalContent = streamingBuffer.toString()
                if (persist) {
                    chatRepository.updateMessageContent(assistantMsgId, finalContent)
                    chatRepository.markMessageComplete(assistantMsgId)
                }
                updateLastAssistantMessage(finalContent, isComplete = true, isStreaming = false)
                _statusMessage.value = "Done"

            } catch (e: Exception) {
                val partial = streamingBuffer.toString()
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
        // Build a simple chat prompt from message history
        val msgs = _messages.value.filter { it.isComplete || it.role == "user" }
        val sb = StringBuilder()
        for (msg in msgs) {
            when (msg.role) {
                "user" -> sb.append("User: ${msg.content}\n")
                "assistant" -> sb.append("Assistant: ${msg.content}\n")
            }
        }
        sb.append("Assistant:")
        return sb.toString()
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
