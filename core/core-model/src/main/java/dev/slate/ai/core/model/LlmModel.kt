package dev.slate.ai.core.model

/**
 * Domain model representing an LLM available in the catalog.
 */
data class LlmModel(
    val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val quantization: String,
    val parameterCount: String,
    val tier: ModelTier,
    val license: String,
    val downloadUrl: String,
    val sha256: String,
    val minRamMb: Int,
    val fileName: String,
)

enum class ModelTier {
    TINY,
    BALANCED,
    HEAVY,
}

/**
 * Represents the current state of a model on this device.
 */
enum class ModelStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    VERIFYING,
    READY,
    LOADING,
    LOADED,
    ERROR,
}
