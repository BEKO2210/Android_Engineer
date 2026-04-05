# Slate ProGuard Rules

# Keep JNI methods for llama.cpp inference
-keepclassmembers class dev.slate.ai.inference.llamacpp.LlamaCppNative {
    native <methods>;
}

# Room entities
-keep class dev.slate.ai.core.database.entity.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
