package dev.slate.ai.inference.llamacpp

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface InferenceState {
    data object Idle : InferenceState
    data object Loading : InferenceState
    data class Ready(val modelPath: String) : InferenceState
    data object Generating : InferenceState
    data class Error(val message: String) : InferenceState
}

data class InferenceParams(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
)

data class ModelLoadParams(
    val contextLength: Int = 2048,
    val threadCount: Int = getDefaultThreadCount(),
    val gpuLayers: Int = 0,
    val useMmap: Boolean = true,
)

class InsufficientMemoryException(
    val requiredMb: Long,
    val availableMb: Long,
) : Exception("Insufficient memory: need ${requiredMb}MB, have ${availableMb}MB available")

private fun getDefaultThreadCount(): Int {
    val cores = Runtime.getRuntime().availableProcessors()
    return (cores - 2).coerceAtLeast(2)
}

@Singleton
class LlamaCppEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<InferenceState>(InferenceState.Idle)
    val state: StateFlow<InferenceState> = _state.asStateFlow()

    private var loadedModelPath: String? = null

    suspend fun loadModel(
        modelPath: String,
        params: ModelLoadParams = ModelLoadParams(),
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(modelPath)
            if (!file.exists()) {
                _state.value = InferenceState.Error("Model file not found")
                return@withContext Result.failure(Exception("Model file not found: $modelPath"))
            }

            val fileSizeMb = file.length() / (1024 * 1024)
            val requiredMb = (fileSizeMb * 1.3).toLong()
            val availableMb = getAvailableMemoryMb()

            if (availableMb < requiredMb) {
                val error = InsufficientMemoryException(requiredMb, availableMb)
                _state.value = InferenceState.Error(error.message ?: "Insufficient memory")
                return@withContext Result.failure(error)
            }

            _state.value = InferenceState.Loading

            if (LlamaCppNative.isModelLoaded()) {
                LlamaCppNative.unloadModel()
            }

            val handle = LlamaCppNative.loadModel(
                modelPath = modelPath,
                nCtx = params.contextLength,
                nThreads = params.threadCount,
                nGpuLayers = params.gpuLayers,
                useMmap = params.useMmap,
            )

            if (handle == 0L) {
                _state.value = InferenceState.Error("Failed to load model. File may be corrupted.")
                return@withContext Result.failure(Exception("Native model load returned 0"))
            }

            loadedModelPath = modelPath
            _state.value = InferenceState.Ready(modelPath)
            Result.success(Unit)

        } catch (e: UnsatisfiedLinkError) {
            _state.value = InferenceState.Error("Native library not available on this device")
            Result.failure(e)
        } catch (e: Exception) {
            _state.value = InferenceState.Error("Load failed: ${e.message.orEmpty()}")
            Result.failure(e)
        }
    }

    /**
     * Streaming generation: emits each token as it's produced.
     * Collects the Flow to receive tokens. The Flow completes when generation finishes.
     */
    fun generateStream(
        prompt: String,
        params: InferenceParams = InferenceParams(),
    ): Flow<String> = callbackFlow {
        if (!LlamaCppNative.isModelLoaded()) {
            close(Exception("No model loaded"))
            return@callbackFlow
        }

        _state.value = InferenceState.Generating

        try {
            val callback = object : TokenCallback {
                override fun onToken(token: String) {
                    trySend(token)
                }
            }

            LlamaCppNative.startCompletionWithCallback(
                prompt = prompt,
                maxTokens = params.maxTokens,
                temperature = params.temperature,
                topP = params.topP,
                topK = params.topK,
                repeatPenalty = params.repeatPenalty,
                callback = callback,
            )

            // Restore state after generation completes
            if (_state.value is InferenceState.Generating) {
                _state.value = InferenceState.Ready(loadedModelPath ?: "")
            }

            close()
        } catch (e: Exception) {
            _state.value = InferenceState.Error("Generation failed: ${e.message.orEmpty()}")
            close(e)
        }

        awaitClose {
            // If the collector cancels, stop generation
            if (LlamaCppNative.isGenerating()) {
                LlamaCppNative.stopGeneration()
            }
        }
    }.flowOn(Dispatchers.IO)

    /** Non-streaming generation (returns full text). */
    suspend fun generate(
        prompt: String,
        params: InferenceParams = InferenceParams(),
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!LlamaCppNative.isModelLoaded()) {
            return@withContext Result.failure(Exception("No model loaded"))
        }
        try {
            _state.value = InferenceState.Generating
            val result = LlamaCppNative.startCompletion(
                prompt = prompt,
                maxTokens = params.maxTokens,
                temperature = params.temperature,
                topP = params.topP,
                topK = params.topK,
                repeatPenalty = params.repeatPenalty,
            )
            _state.value = InferenceState.Ready(loadedModelPath ?: "")
            if (result.startsWith("[ERROR:")) Result.failure(Exception(result))
            else Result.success(result)
        } catch (e: Exception) {
            _state.value = InferenceState.Error("Generation failed: ${e.message.orEmpty()}")
            Result.failure(e)
        }
    }

    fun stopGeneration() {
        if (LlamaCppNative.isGenerating()) {
            LlamaCppNative.stopGeneration()
        }
    }

    suspend fun unload() = withContext(Dispatchers.IO) {
        try {
            LlamaCppNative.unloadModel()
            loadedModelPath = null
            _state.value = InferenceState.Idle
        } catch (e: Exception) {
            _state.value = InferenceState.Error("Unload failed: ${e.message.orEmpty()}")
        }
    }

    fun isModelLoaded(): Boolean = try { LlamaCppNative.isModelLoaded() } catch (_: UnsatisfiedLinkError) { false }

    private fun getAvailableMemoryMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024)
    }
}
