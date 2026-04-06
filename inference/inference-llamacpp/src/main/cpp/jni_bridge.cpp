#include <jni.h>
#include <android/log.h>
#include <string>
#include <atomic>
#include <mutex>
#include <vector>
#include "llama.h"

#define TAG "SlateInference"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static llama_model *g_model = nullptr;
static llama_context *g_ctx = nullptr;
static llama_sampler *g_sampler = nullptr;
static std::atomic<bool> g_is_generating{false};
static std::atomic<bool> g_stop_requested{false};
static std::mutex g_mutex;

static std::string token_to_string(const llama_vocab *vocab, llama_token token) {
    char buf[256];
    int n = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, true);
    if (n < 0) {
        std::string result(static_cast<size_t>(-n), '\0');
        llama_token_to_piece(vocab, token, result.data(), result.size(), 0, true);
        return result;
    }
    return std::string(buf, n);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_loadModel(
        JNIEnv *env, jobject, jstring jModelPath, jint nCtx, jint nThreads, jint nGpuLayers, jboolean useMmap) {
    std::lock_guard<std::mutex> lock(g_mutex);

    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }

    const char *path = env->GetStringUTFChars(jModelPath, nullptr);
    if (!path) { LOGE("null path"); return 0; }

    LOGI("Loading: %s (ctx=%d, threads=%d)", path, nCtx, nThreads);

    llama_model_params mp = llama_model_default_params();
    mp.use_mmap = useMmap;
    mp.n_gpu_layers = nGpuLayers;

    g_model = llama_load_model_from_file(path, mp);
    env->ReleaseStringUTFChars(jModelPath, path);

    if (!g_model) { LOGE("Failed to load model"); return 0; }

    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = nCtx;
    cp.n_threads = nThreads;
    cp.n_threads_batch = nThreads;

    g_ctx = llama_new_context_with_model(g_model, cp);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_free_model(g_model); g_model = nullptr;
        return 0;
    }

    LOGI("Model loaded, n_ctx=%d", llama_n_ctx(g_ctx));
    return reinterpret_cast<jlong>(g_model);
}

JNIEXPORT void JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_unloadModel(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_mutex);
    g_stop_requested.store(true);
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
}

JNIEXPORT jboolean JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_isModelLoaded(JNIEnv*, jobject) {
    return g_model != nullptr && g_ctx != nullptr;
}

JNIEXPORT jstring JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_startCompletionWithCallback(
        JNIEnv *env, jobject, jstring jPrompt, jint maxTokens, jfloat temperature,
        jfloat topP, jint topK, jfloat repeatPenalty, jobject callback) {

    if (!g_model || !g_ctx) return env->NewStringUTF("");

    const char *promptC = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt(promptC);
    env->ReleaseStringUTFChars(jPrompt, promptC);

    g_is_generating.store(true);
    g_stop_requested.store(false);

    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onToken = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");
    if (!onToken) { g_is_generating.store(false); return env->NewStringUTF(""); }

    const llama_vocab *vocab = llama_model_get_vocab(g_model);

    // Tokenize
    int n = llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    std::vector<llama_token> tokens(std::abs(n));
    n = llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    if (n < 0) {
        tokens.resize(-n);
        n = llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    }
    tokens.resize(n);

    if (tokens.empty()) { g_is_generating.store(false); return env->NewStringUTF(""); }

    int n_ctx = llama_n_ctx(g_ctx);
    if ((int)tokens.size() > n_ctx - 4) {
        g_is_generating.store(false);
        return env->NewStringUTF("[ERROR: prompt too long]");
    }

    llama_kv_self_clear(g_ctx);

    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(g_ctx, batch) != 0) {
        g_is_generating.store(false);
        return env->NewStringUTF("[ERROR: decode failed]");
    }

    // Sampler
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(sp);

    if (repeatPenalty != 1.0f) {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_penalties(
            64, repeatPenalty, 0.0f, 0.0f));
    }
    if (temperature <= 0.0f) {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_greedy());
    } else {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(topK));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(42));
    }

    // Generate
    std::string result;
    int n_decoded = tokens.size();

    for (int i = 0; i < maxTokens; i++) {
        if (g_stop_requested.load()) break;

        llama_token tok = llama_sampler_sample(g_sampler, g_ctx, -1);
        llama_sampler_accept(g_sampler, tok);

        if (llama_vocab_is_eog(vocab, tok)) break;

        std::string piece = token_to_string(vocab, tok);
        result += piece;

        jstring jPiece = env->NewStringUTF(piece.c_str());
        env->CallVoidMethod(callback, onToken, jPiece);
        env->DeleteLocalRef(jPiece);
        if (env->ExceptionCheck()) { env->ExceptionClear(); break; }

        llama_batch nb = llama_batch_get_one(&tok, 1);
        if (llama_decode(g_ctx, nb) != 0) break;

        n_decoded++;
        if (n_decoded >= n_ctx) break;
    }

    g_is_generating.store(false);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jstring JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_startCompletion(
        JNIEnv *env, jobject, jstring jPrompt, jint maxTokens, jfloat temperature,
        jfloat topP, jint topK, jfloat repeatPenalty) {
    if (!g_model || !g_ctx) return env->NewStringUTF("");

    const char *promptC = env->GetStringUTFChars(jPrompt, nullptr);
    std::string prompt(promptC);
    env->ReleaseStringUTFChars(jPrompt, promptC);

    g_is_generating.store(true);
    g_stop_requested.store(false);

    const llama_vocab *vocab = llama_model_get_vocab(g_model);
    int n = llama_tokenize(vocab, prompt.c_str(), prompt.size(), nullptr, 0, true, true);
    std::vector<llama_token> tokens(std::abs(n));
    n = llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true);
    if (n < 0) { tokens.resize(-n); n = llama_tokenize(vocab, prompt.c_str(), prompt.size(), tokens.data(), tokens.size(), true, true); }
    tokens.resize(n);
    if (tokens.empty()) { g_is_generating.store(false); return env->NewStringUTF(""); }

    int n_ctx = llama_n_ctx(g_ctx);
    if ((int)tokens.size() > n_ctx - 4) { g_is_generating.store(false); return env->NewStringUTF("[ERROR: prompt too long]"); }

    llama_kv_self_clear(g_ctx);
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(g_ctx, batch) != 0) { g_is_generating.store(false); return env->NewStringUTF("[ERROR: decode failed]"); }

    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    g_sampler = llama_sampler_chain_init(sp);
    if (repeatPenalty != 1.0f) { llama_sampler_chain_add(g_sampler, llama_sampler_init_penalties(64, repeatPenalty, 0.0f, 0.0f)); }
    if (temperature <= 0.0f) { llama_sampler_chain_add(g_sampler, llama_sampler_init_greedy()); }
    else {
        llama_sampler_chain_add(g_sampler, llama_sampler_init_top_k(topK));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_top_p(topP, 1));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(g_sampler, llama_sampler_init_dist(42));
    }

    std::string result;
    int n_decoded = tokens.size();
    for (int i = 0; i < maxTokens; i++) {
        if (g_stop_requested.load()) break;
        llama_token tok = llama_sampler_sample(g_sampler, g_ctx, -1);
        llama_sampler_accept(g_sampler, tok);
        if (llama_vocab_is_eog(vocab, tok)) break;
        result += token_to_string(vocab, tok);
        llama_batch nb = llama_batch_get_one(&tok, 1);
        if (llama_decode(g_ctx, nb) != 0) break;
        n_decoded++;
        if (n_decoded >= n_ctx) break;
    }
    g_is_generating.store(false);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_stopGeneration(JNIEnv*, jobject) {
    g_stop_requested.store(true);
}

JNIEXPORT jboolean JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_isGenerating(JNIEnv*, jobject) {
    return g_is_generating.load();
}

JNIEXPORT jstring JNICALL
Java_dev_slate_ai_inference_llamacpp_LlamaCppNative_getSystemInfo(JNIEnv *env, jobject) {
    return env->NewStringUTF(llama_print_system_info());
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM*, void*) {
    LOGI("slate_inference loaded");
    llama_backend_init();
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM*, void*) {
    if (g_sampler) { llama_sampler_free(g_sampler); g_sampler = nullptr; }
    if (g_ctx) { llama_free(g_ctx); g_ctx = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }
    llama_backend_free();
}

} // extern "C"
