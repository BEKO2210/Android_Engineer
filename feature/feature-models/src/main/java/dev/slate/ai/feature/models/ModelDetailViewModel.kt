package dev.slate.ai.feature.models

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.slate.ai.core.data.repository.ModelRepository
import dev.slate.ai.core.model.LlmModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val modelId: String = checkNotNull(savedStateHandle["modelId"])

    private val _uiState = MutableStateFlow<ModelDetailUiState>(ModelDetailUiState.Loading)
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()

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
}

sealed interface ModelDetailUiState {
    data object Loading : ModelDetailUiState
    data class Success(val model: LlmModel) : ModelDetailUiState
    data class Error(val message: String) : ModelDetailUiState
}
