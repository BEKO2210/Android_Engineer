#include <jni.h>
#include <android/log.h>
#include <string>
#include <atomic>
#include <mutex>
#include "llama.h"
#include <vector>

#define LOG_TAG "SlateInference"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global state — single model/context at a time
static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static llama_sampler *g_sampler = nullptr;
static std::atomic<bool> g_is_generating{false};
static std::atomic<bool> g_stop_requested{false};
static std::mutex g_mutex;

// Helper: convert llama_token to string
static std::string token_to_string(const llama_model *model, llama_token token) {
    char buf[128];
    int n = llama_token_to_piece(model, token, buf, sizeof(buf), 0, true);
    if (n < 0) {
        // Buffer too small, allocate dynamically
        std::string result(static_cast<size_t>(-n), '\0');
        llama_token_to_piece(model, token, result.data(), result.size(), 0, true);
        return result;
    }
    return std::string(buf, n);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_loadModel(
        JNIEnv *env, jobject /* this */,
        jstring jModelPath, jint nCtx, jint nThreads, jint nGpuLayers, jboolean useMmap) {

    std::lock_guard<std::mutex> lock(g_mutex);

    // Unload existing model if any
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }

    const char *modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    if (!modelPath) {
        LOGE("loadModel: null model path");
        return 0;
    }

    LOGI("loadModel: loading %s (ctx=%d, threads=%d)", modelPath, nCtx, nThreads);

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = useMmap;
    model_params.n_gpu_layers = nGpuLayers;

    g_model = llama_load_model_from_file(modelPath, model_params);
    env->ReleaseStringUTFChars(jModelPath, modelPath);

    if (!g_model) {
        LOGE("loadModel: failed to load model");
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = nCtx;
    ctx_params.n_threads = nThreads;
    ctx_params.n_threads_batch = nThreads;

    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("loadModel: failed to create context");
        llama_free_model(g_model);
        g_model = nullptr;
        return 0;
    }

    LOGI("loadModel: success, n_ctx=%d", llama_n_ctx(g_ctx));
    return reinterpret_cast<jlong>(g_model);
}

JNIEXPORT void JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_unloadModel(
        JNIEnv *env, jobject /* this */) {

    std::lock_guard<std::mutex> lock(g_mutex);

    g_stop_requested.store(true);

    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }

    LOGI("unloadModel: done");
}

JNIEXPORT jboolean JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_isModelLoaded(
        JNIEnv *env, jobject /* this */) {
    return g_model != nullptr && g_ctx != nullptr;
}

JNIEXPORT jstring JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_startCompletion(
        JNIEnv *env, jobject /* this */,
        jstring jPrompt, jint maxTokens, jfloat temperature,
        jfloat topP, jint topK, jfloat repeatPenalty) {

    if (!g_model || !g_ctx) {
        return env->NewStringUTF("");
    }

    const char *promptCStr = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt(promptCStr);
    env->ReleaseStringUTFChars(jPrompt, promptCStr);

    g_is_generating.store(true);
    g_stop_requested.store(false);

    // Tokenize the prompt using llama_tokenize directly
    const llama_model *model = llama_get_model(g_ctx);
    int n_tokens = llama_tokenize(model, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    std::vector<llama_token> tokens(std::abs(n_tokens));
    n_tokens = llama_tokenize(model, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    if (n_tokens < 0) {
        tokens.resize(-n_tokens);
        n_tokens = llama_tokenize(model, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    }
    tokens.resize(n_tokens);
    if (tokens.empty()) {
        g_is_generating.store(false);
        return env->NewStringUTF("");
    }

    int n_ctx = llama_n_ctx(g_ctx);
    if ((int)tokens.size() > n_ctx - 4) {
        LOGE("startCompletion: prompt too long (%zu tokens, ctx=%d)", tokens.size(), n_ctx);
        g_is_generating.store(false);
        return env->NewStringUTF("[ERROR: prompt too long]");
    }

    // Clear KV cache
    llama_kv_cache_clear(g_ctx);

    // Create batch for prompt
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("startCompletion: prompt decode failed");
        g_is_generating.store(false);
        return env->NewStringUTF("[ERROR: decode failed]");
    }

    // Set up sampler chain
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }

    llama_sampler_chain_params chain_params = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(chain_params);

    if (repeatPenalty != 1.0f) {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_penalties(
            llama_n_vocab(g_model),        // n_vocab
            llama_token_eos(g_model),      // special_eos_id
            0,                              // linefeed_id (0 = auto)
            64,                             // penalty_last_n
            repeatPenalty,                  // penalty_repeat
            0.0f,                           // penalty_freq
            0.0f,                           // penalty_present
            false,                          // penalize_nl
            false                           // ignore_eos
        ));
    }

    if (temperature <= 0.0f) {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(topK));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(42));
    }

    // Generate tokens
    std::string result;
    llama_token eos_token = llama_token_eos(g_model);
    int n_decoded = tokens.size();

    for (int i = 0; i < maxTokens; i++) {
        if (g_stop_requested.load()) {
            LOGI("startCompletion: stop requested after %d tokens", i);
            break;
        }

        llama_token new_token = llama_sampler_sample(g_sampler, g_ctx, -1);
        llama_sampler_accept(g_sampler, new_token);

        if (llama_token_is_eog(g_model, new_token)) {
            break;
        }

        std::string piece = token_to_string(g_model, new_token);
        result += piece;

        // Prepare next batch (single token)
        llama_batch next_batch = llama_batch_get_one(&new_token, 1);
        if (llama_decode(g_ctx, next_batch) != 0) {
            LOGE("startCompletion: decode failed at token %d", i);
            break;
        }

        n_decoded++;

        // Check context overflow
        if (n_decoded >= n_ctx) {
            LOGI("startCompletion: context full at %d tokens", n_decoded);
            break;
        }
    }

    g_is_generating.store(false);
    LOGI("startCompletion: generated %zu chars", result.size());

    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_stopGeneration(
        JNIEnv *env, jobject /* this */) {
    g_stop_requested.store(true);
    LOGI("stopGeneration: requested");
}

JNIEXPORT jboolean JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_isGenerating(
        JNIEnv *env, jobject /* this */) {
    return g_is_generating.load();
}

JNIEXPORT jstring JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_getSystemInfo(
        JNIEnv *env, jobject /* this */) {
    std::string info = llama_print_system_info();
    return env->NewStringUTF(info.c_str());
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    LOGI("JNI_OnLoad: slate_inference library loaded");
    llama_backend_init();
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void * /* reserved */) {
    LOGI("JNI_OnUnload: cleaning up");
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    llama_backend_free();
}

} // extern "C"
