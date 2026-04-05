package dev.slate.ai.inference.llamacpp

/**
 * JNI declarations for the llama.cpp native bridge.
 * Each method maps to a function in jni_bridge.cpp.
 */
object LlamaCppNative {

    init {
        System.loadLibrary("slate_inference")
    }

    /**
     * Load a GGUF model file. Returns a non-zero handle on success, 0 on failure.
     */
    external fun loadModel(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        nGpuLayers: Int,
        useMmap: Boolean,
    ): Long

    /**
     * Unload the currently loaded model and free all resources.
     */
    external fun unloadModel()

    /**
     * Check if a model is currently loaded.
     */
    external fun isModelLoaded(): Boolean

    /**
     * Run completion on the given prompt. Blocks until complete or stopped.
     * Returns the generated text.
     */
    external fun startCompletion(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
    ): String

    /**
     * Request generation to stop. The current startCompletion call will return
     * with whatever has been generated so far.
     */
    external fun stopGeneration()

    /**
     * Check if generation is currently in progress.
     */
    external fun isGenerating(): Boolean

    /**
     * Get system info string (CPU features, SIMD support).
     */
    external fun getSystemInfo(): String
}
