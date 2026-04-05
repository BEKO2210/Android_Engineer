package dev.slate.ai.feature.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.slate.ai.download.engine.ModelDownloadManager
import dev.slate.ai.inference.llamacpp.InferenceParams
import dev.slate.ai.inference.llamacpp.InferenceState
import dev.slate.ai.inference.llamacpp.LlamaCppEngine
import dev.slate.ai.inference.llamacpp.ModelLoadParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val engine: LlamaCppEngine,
    private val downloadManager: ModelDownloadManager,
) : ViewModel() {

    val inferenceState: StateFlow<InferenceState> = engine.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InferenceState.Idle)

    private val _generatedText = MutableStateFlow("")
    val generatedText: StateFlow<String> = _generatedText.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun updateInput(text: String) {
        _inputText.value = text
    }

    fun loadModel(modelId: String) {
        viewModelScope.launch {
            _statusMessage.value = "Looking for model file..."
            val file = downloadManager.getModelFile(modelId)
            if (file == null) {
                _statusMessage.value = "Model not downloaded. Go to Models tab first."
                return@launch
            }

            _statusMessage.value = "Loading model into memory..."
            val result = engine.loadModel(file.absolutePath, ModelLoadParams())
            result.fold(
                onSuccess = { _statusMessage.value = "Model ready." },
                onFailure = { _statusMessage.value = "Load failed: ${it.message}" },
            )
        }
    }

    fun generate() {
        val prompt = _inputText.value.trim()
        if (prompt.isEmpty()) return

        viewModelScope.launch {
            _generatedText.value = ""
            _statusMessage.value = "Generating..."
            val result = engine.generate(prompt, InferenceParams())
            result.fold(
                onSuccess = {
                    _generatedText.value = it
                    _statusMessage.value = "Done."
                },
                onFailure = {
                    _statusMessage.value = "Error: ${it.message}"
                },
            )
        }
    }

    fun stopGeneration() {
        engine.stopGeneration()
    }

    fun unloadModel() {
        viewModelScope.launch {
            engine.unload()
            _statusMessage.value = "Model unloaded."
            _generatedText.value = ""
        }
    }
}
