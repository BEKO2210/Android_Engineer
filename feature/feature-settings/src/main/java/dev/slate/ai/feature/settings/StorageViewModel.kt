package dev.slate.ai.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.slate.ai.core.database.dao.DownloadDao
import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.download.engine.ModelDownloadManager
import dev.slate.ai.download.engine.util.StorageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val downloadManager: ModelDownloadManager,
) : ViewModel() {

    val downloadedModels: StateFlow<List<DownloadEntity>> = downloadDao.observeAllDownloads()
        .map { list -> list.filter { it.status == "COMPLETE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _totalSize = MutableStateFlow(0L)
    val totalSize: StateFlow<Long> = _totalSize.asStateFlow()

    private val _availableSize = MutableStateFlow(0L)
    val availableSize: StateFlow<Long> = _availableSize.asStateFlow()

    init {
        refreshStorageInfo()
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch {
            downloadManager.deleteModel(modelId)
            refreshStorageInfo()
        }
    }

    private fun refreshStorageInfo() {
        _totalSize.value = StorageUtils.getTotalModelsSize(context)
        _availableSize.value = StorageUtils.getAvailableBytes(context)
    }
}
