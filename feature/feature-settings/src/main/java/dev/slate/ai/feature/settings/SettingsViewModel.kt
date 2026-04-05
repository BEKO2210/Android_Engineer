package dev.slate.ai.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.slate.ai.core.data.repository.ChatRepository
import dev.slate.ai.core.datastore.SlatePreferences
import dev.slate.ai.download.engine.ModelDownloadManager
import dev.slate.ai.download.engine.util.StorageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean = true,
    val isOfflineOnly: Boolean = false,
    val isChatHistoryEnabled: Boolean = true,
    val modelsStorageBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SlatePreferences,
    private val chatRepository: ChatRepository,
    private val downloadManager: ModelDownloadManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.isDarkTheme,
        preferences.isOfflineOnly,
        preferences.isChatHistoryEnabled,
    ) { dark, offline, history ->
        SettingsUiState(
            isDarkTheme = dark,
            isOfflineOnly = offline,
            isChatHistoryEnabled = history,
            modelsStorageBytes = StorageUtils.getTotalModelsSize(context),
            availableStorageBytes = StorageUtils.getAvailableBytes(context),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferences.setDarkTheme(enabled) }
    }

    fun setOfflineOnly(enabled: Boolean) {
        viewModelScope.launch { preferences.setOfflineOnly(enabled) }
    }

    fun setChatHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setChatHistoryEnabled(enabled) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatRepository.deleteAllConversations()
            _actionResult.value = "Chat history cleared"
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
