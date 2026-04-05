package dev.slate.ai.feature.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.slate.ai.core.data.repository.ModelRepository
import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.core.model.LlmModel
import dev.slate.ai.download.engine.InsufficientStorageException
import dev.slate.ai.download.engine.ModelDownloadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modelRepository: ModelRepository,
    private val downloadManager: ModelDownloadManager,
) : ViewModel() {

    private val modelId: String = checkNotNull(savedStateHandle["modelId"])

    private val _uiState = MutableStateFlow<ModelDetailUiState>(ModelDetailUiState.Loading)
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()

    val downloadState: StateFlow<DownloadEntity?> = downloadManager
        .observeDownload(modelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _errorEvent = MutableStateFlow<String?>(null)
    val errorEvent: StateFlow<String?> = _errorEvent.asStateFlow()

    init {
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch {
            val model = modelRepository.getModelById(modelId)
            _uiState.value = if (model != null) {
                ModelDetailUiState.Success(model)
            } else {
                ModelDetailUiState.Error("Model not found")
            }
        }
    }

    fun startDownload() {
        val model = (_uiState.value as? ModelDetailUiState.Success)?.model ?: return
        viewModelScope.launch {
            try {
                downloadManager.enqueueDownload(model)
            } catch (e: InsufficientStorageException) {
                _errorEvent.value = "NOT_ENOUGH_STORAGE"
            } catch (e: Exception) {
                _errorEvent.value = e.message
            }
        }
    }

    fun pauseDownload() {
        viewModelScope.launch {
            downloadManager.cancelDownload(modelId)
        }
    }

    fun resumeDownload() {
        val model = (_uiState.value as? ModelDetailUiState.Success)?.model ?: return
        viewModelScope.launch {
            try {
                downloadManager.resumeDownload(model)
            } catch (e: Exception) {
                _errorEvent.value = e.message
            }
        }
    }

    fun cancelDownload() {
        viewModelScope.launch {
            downloadManager.deleteModel(modelId)
        }
    }

    fun retryDownload() {
        val model = (_uiState.value as? ModelDetailUiState.Success)?.model ?: return
        viewModelScope.launch {
            try {
                // Delete old state and start fresh
                downloadManager.deleteModel(modelId)
                downloadManager.enqueueDownload(model)
            } catch (e: InsufficientStorageException) {
                _errorEvent.value = "NOT_ENOUGH_STORAGE"
            } catch (e: Exception) {
                _errorEvent.value = e.message
            }
        }
    }

    fun clearError() {
        _errorEvent.value = null
    }
}

sealed interface ModelDetailUiState {
    data object Loading : ModelDetailUiState
    data class Success(val model: LlmModel) : ModelDetailUiState
    data class Error(val message: String) : ModelDetailUiState
}
