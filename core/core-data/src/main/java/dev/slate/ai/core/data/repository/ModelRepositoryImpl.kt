package dev.slate.ai.core.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.slate.ai.core.model.LlmModel
import dev.slate.ai.core.model.ModelTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ModelRepository {

    private var cachedModels: List<LlmModel>? = null

    override fun getAvailableModels(): Flow<List<LlmModel>> = flow {
        emit(loadModels())
    }

    override suspend fun getModelById(id: String): LlmModel? {
        return loadModels().find { it.id == id }
    }

    private fun loadModels(): List<LlmModel> {
        cachedModels?.let { return it }

        val json = context.assets.open("model_catalog.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(json)
        val models = mutableListOf<LlmModel>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val capabilitiesArray = obj.optJSONArray("capabilities")
            val capabilities = mutableListOf<String>()
            if (capabilitiesArray != null) {
                for (j in 0 until capabilitiesArray.length()) {
                    capabilities.add(capabilitiesArray.getString(j))
                }
            }

            models.add(
                LlmModel(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    description = obj.getString("description"),
                    sizeBytes = obj.getLong("sizeBytes"),
                    quantization = obj.getString("quantization"),
                    parameterCount = obj.getString("parameterCount"),
                    tier = ModelTier.valueOf(obj.getString("tier")),
                    license = obj.getString("license"),
                    downloadUrl = obj.getString("downloadUrl"),
                    sha256 = obj.optString("sha256", ""),
                    minRamMb = obj.getInt("minRamMb"),
                    fileName = obj.getString("fileName"),
                    contextLength = obj.optInt("contextLength", 2048),
                    family = obj.optString("family", ""),
                    capabilities = capabilities,
                    deviceRecommendation = obj.optString("deviceRecommendation", ""),
                ),
            )
        }

        cachedModels = models
        return models
    }
}
