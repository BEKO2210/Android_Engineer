package dev.slate.ai.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.slate.ai.core.common.DeviceCapability
import dev.slate.ai.core.common.DeviceTier
import dev.slate.ai.core.datastore.SlatePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val totalRamMb: Long = 0,
    val availableStorageBytes: Long = 0,
    val cpuCores: Int = 0,
    val deviceTier: DeviceTier = DeviceTier.BASIC,
    val tierDescription: String = "",
    val recommendedModelId: String = "",
    val recommendedModelName: String = "",
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SlatePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val info = DeviceCapability.getDeviceInfo(context)
        val recommendedId = DeviceCapability.getRecommendedModelId(info.deviceTier)
        val recommendedName = when (recommendedId) {
            "smollm2-1.7b-q4" -> "SmolLM2 1.7B"
            "qwen-2.5-3b-q4" -> "Qwen 2.5 3B"
            "phi-3-mini-q4" -> "Phi-3 Mini 3.8B"
            else -> "SmolLM2 1.7B"
        }

        _uiState.value = OnboardingUiState(
            totalRamMb = info.totalRamMb,
            availableStorageBytes = info.availableStorageMb * 1024 * 1024,
            cpuCores = info.cpuCores,
            deviceTier = info.deviceTier,
            tierDescription = DeviceCapability.getTierDescription(info.deviceTier),
            recommendedModelId = recommendedId,
            recommendedModelName = recommendedName,
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferences.setOnboardingComplete(true)
        }
    }
}
