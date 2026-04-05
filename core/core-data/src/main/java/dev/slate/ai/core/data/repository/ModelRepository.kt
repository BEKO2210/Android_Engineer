package dev.slate.ai.core.data.repository

import dev.slate.ai.core.model.LlmModel
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing the model catalog and local model state.
 */
interface ModelRepository {
    fun getAvailableModels(): Flow<List<LlmModel>>
    suspend fun getModelById(id: String): LlmModel?
}
