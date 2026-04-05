# Slate ProGuard / R8 Rules

# ---- JNI (llama.cpp native bridge) ----
# Keep all JNI methods and the TokenCallback interface
-keep class dev.slate.ai.inference.llamacpp.LlamaCppNative {
    native <methods>;
    *;
}
-keep class dev.slate.ai.inference.llamacpp.LlamaCppNative$* { *; }
-keep interface dev.slate.ai.inference.llamacpp.TokenCallback { *; }
-keep class * implements dev.slate.ai.inference.llamacpp.TokenCallback { *; }

# ---- Room ----
-keep class dev.slate.ai.core.database.entity.** { *; }
-keep class dev.slate.ai.core.database.dao.** { *; }
-keep class dev.slate.ai.core.database.SlateDatabase { *; }

# ---- Hilt / Dagger ----
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ---- WorkManager + Hilt Workers ----
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# ---- Kotlin ----
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- General ----
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
