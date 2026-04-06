package dev.slate.ai.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.slate.ai.core.database.dao.DownloadDao
import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.download.engine.util.StorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Success(val modelName: String) : ImportState
    data class Error(val message: String) : ImportState
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
) : ViewModel() {

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun importGgufFile(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.Importing

            try {
                val fileName = getFileName(uri) ?: "custom-model.gguf"

                // Validate file extension
                if (!fileName.endsWith(".gguf", ignoreCase = true)) {
                    _importState.value = ImportState.Error(
                        "Only .gguf files are supported. Selected file: $fileName"
                    )
                    return@launch
                }

                // Generate a model ID
                val modelId = "custom-${System.currentTimeMillis()}"
                val modelName = fileName.removeSuffix(".gguf").take(30)

                // Copy file to app storage
                val targetDir = File(StorageUtils.getModelsDir(context), modelId)
                targetDir.mkdirs()
                val targetFile = File(targetDir, fileName)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output, bufferSize = 65536)
                        }
                    } ?: throw Exception("Could not read file")
                }

                // Verify copied file
                if (!targetFile.exists() || targetFile.length() == 0L) {
                    _importState.value = ImportState.Error("File copy failed")
                    return@launch
                }

                // Register in download database as COMPLETE
                val now = System.currentTimeMillis()
                downloadDao.insert(DownloadEntity(
                    modelId = modelId,
                    modelName = modelName,
                    url = "local://$fileName",
                    expectedSha256 = "",
                    filePath = targetFile.absolutePath,
                    totalBytes = targetFile.length(),
                    downloadedBytes = targetFile.length(),
                    status = "COMPLETE",
                    workManagerId = null,
                    errorMessage = null,
                    createdAt = now,
                    updatedAt = now,
                ))

                _importState.value = ImportState.Success(modelName)

            } catch (e: Exception) {
                _importState.value = ImportState.Error("Import failed: ${e.message}")
            }
        }
    }

    fun clearState() {
        _importState.value = ImportState.Idle
    }

    private fun getFileName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                if (nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) { null }
    }
}
