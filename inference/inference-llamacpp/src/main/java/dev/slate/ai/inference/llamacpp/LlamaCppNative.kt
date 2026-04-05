package dev.slate.ai.inference.llamacpp

/**
 * JNI declarations for the llama.cpp native bridge.
 */
object LlamaCppNative {

    init {
        System.loadLibrary("slate_inference")
    }

    external fun loadModel(
        modelPath: String,
        nCtx: Int,
        nThreads: Int,
        nGpuLayers: Int,
        useMmap: Boolean,
    ): Long

    external fun unloadModel()
    external fun isModelLoaded(): Boolean

    /** Non-streaming: blocks until all tokens generated. */
    external fun startCompletion(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
    ): String

    /** Streaming: calls callback.onToken(String) per token, returns full text. */
    external fun startCompletionWithCallback(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        topK: Int,
        repeatPenalty: Float,
        callback: TokenCallback,
    ): String

    external fun stopGeneration()
    external fun isGenerating(): Boolean
    external fun getSystemInfo(): String
}

/** Callback interface for streaming token delivery from native code. */
interface TokenCallback {
    fun onToken(token: String)
}
