package dev.slate.ai.feature.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.slate.ai.core.data.repository.ModelRepository
import dev.slate.ai.core.model.LlmModel
import dev.slate.ai.core.model.ModelTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ModelsUiState>(ModelsUiState.Loading)
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<ModelTier?>(null)
    val selectedFilter: StateFlow<ModelTier?> = _selectedFilter.asStateFlow()

    init {
        loadModels()
    }

    private fun loadModels() {
        viewModelScope.launch {
            try {
                val models = modelRepository.getAvailableModels().first()
                _uiState.value = ModelsUiState.Success(models)
            } catch (e: Exception) {
                _uiState.value = ModelsUiState.Error(e.message ?: "Failed to load models")
            }
        }
    }

    fun setFilter(tier: ModelTier?) {
        _selectedFilter.value = tier
    }

    fun getFilteredModels(models: List<LlmModel>, filter: ModelTier?): List<LlmModel> {
        return if (filter == null) models else models.filter { it.tier == filter }
    }
}

sealed interface ModelsUiState {
    data object Loading : ModelsUiState
    data class Success(val models: List<LlmModel>) : ModelsUiState
    data class Error(val message: String) : ModelsUiState
}
