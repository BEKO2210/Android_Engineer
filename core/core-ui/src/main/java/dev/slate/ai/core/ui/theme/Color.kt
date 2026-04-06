package dev.slate.ai.core.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme palette
val SlateSurface = Color(0xFF0D0D0D)
val SlateSurfaceContainer = Color(0xFF1A1A1A)
val SlateSurfaceContainerHigh = Color(0xFF242424)
val SlateSurfaceContainerHighest = Color(0xFF2E2E2E)

val SlateOnSurface = Color(0xFFE8E8E8)
val SlateOnSurfaceVariant = Color(0xFF9E9E9E)

val SlatePrimary = Color(0xFFA8C4D4)           // Ice blue accent
val SlatePrimaryContainer = Color(0xFF1E3A4A)
val SlateOnPrimary = Color(0xFF0D0D0D)
val SlateOnPrimaryContainer = Color(0xFFA8C4D4)

val SlateSecondary = Color(0xFFB0BEC5)          // Silver-gray
val SlateSecondaryContainer = Color(0xFF263238)
val SlateOnSecondary = Color(0xFF0D0D0D)

val SlateError = Color(0xFFCF6679)
val SlateErrorContainer = Color(0xFF3D1A22)
val SlateOnError = Color(0xFF0D0D0D)

val SlateOutline = Color(0xFF2E2E2E)
val SlateOutlineVariant = Color(0xFF1E1E1E)

// Light theme palette (secondary, for accessibility)
val SlateLightSurface = Color(0xFFF5F5F5)
val SlateLightSurfaceContainer = Color(0xFFFFFFFF)
val SlateLightOnSurface = Color(0xFF1A1A1A)
val SlateLightOnSurfaceVariant = Color(0xFF616161)
val SlateLightPrimary = Color(0xFF37474F)
val SlateLightPrimaryContainer = Color(0xFFCFD8DC)
val SlateLightOutline = Color(0xFFE0E0E0)

// Model accent colors — each model gets its own identity
val SlateAccentQwen = Color(0xFFA8C4D4)     // Ice-blue (flagship)
val SlateAccentSmolLM = Color(0xFFB39DDB)   // Soft lavender
val SlateAccentPhi = Color(0xFF80CBC4)       // Teal-mint
val SlateAccentLlama = Color(0xFFFFAB91)     // Warm coral
val SlateAccentGemma = Color(0xFF90CAF9)     // Sky blue

object ModelAccent {
    fun forModelId(modelId: String?): Color {
        if (modelId == null) return SlatePrimary
        return when {
            modelId.contains("qwen", ignoreCase = true) -> SlateAccentQwen
            modelId.contains("smol", ignoreCase = true) -> SlateAccentSmolLM
            modelId.contains("phi", ignoreCase = true) -> SlateAccentPhi
            modelId.contains("llama", ignoreCase = true) -> SlateAccentLlama
            modelId.contains("gemma", ignoreCase = true) -> SlateAccentGemma
            else -> SlatePrimary
        }
    }

    fun nameForModelId(modelId: String?): String {
        if (modelId == null) return ""
        return when {
            modelId.contains("qwen", ignoreCase = true) -> "Qwen"
            modelId.contains("smol", ignoreCase = true) -> "SmolLM"
            modelId.contains("phi", ignoreCase = true) -> "Phi"
            modelId.contains("llama", ignoreCase = true) -> "Llama"
            modelId.contains("gemma", ignoreCase = true) -> "Gemma"
            else -> ""
        }
    }
}
