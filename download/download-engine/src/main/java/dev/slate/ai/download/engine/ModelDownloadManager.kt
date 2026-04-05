package dev.slate.ai.download.engine

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.slate.ai.core.database.dao.DownloadDao
import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.core.model.LlmModel
import dev.slate.ai.download.engine.util.StorageUtils
import dev.slate.ai.download.engine.worker.ModelDownloadWorker
import dev.slate.ai.download.engine.worker.ModelVerificationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Start downloading a model. Returns the WorkManager request ID.
     * Throws IllegalStateException if insufficient storage.
     */
    suspend fun enqueueDownload(model: LlmModel): UUID {
        // Storage check
        if (!StorageUtils.hasEnoughSpace(context, model.sizeBytes)) {
            throw InsufficientStorageException(
                required = model.sizeBytes,
                available = StorageUtils.getAvailableBytes(context),
            )
        }

        val filePath = StorageUtils.getModelFilePath(context, model.id, model.fileName)

        // Insert download record
        val now = System.currentTimeMillis()
        val entity = DownloadEntity(
            modelId = model.id,
            modelName = model.name,
            url = model.downloadUrl,
            expectedSha256 = model.sha256,
            filePath = filePath,
            totalBytes = model.sizeBytes,
            downloadedBytes = 0L,
            status = "QUEUED",
            workManagerId = null,
            errorMessage = null,
            createdAt = now,
            updatedAt = now,
        )
        downloadDao.insert(entity)

        // Create download → verification chain
        val downloadWork = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(
                ModelDownloadWorker.KEY_MODEL_ID to model.id,
                ModelDownloadWorker.KEY_URL to model.downloadUrl,
                ModelDownloadWorker.KEY_FILE_PATH to filePath,
                ModelDownloadWorker.KEY_TOTAL_BYTES to model.sizeBytes,
                ModelDownloadWorker.KEY_EXPECTED_SHA256 to model.sha256,
                ModelDownloadWorker.KEY_MODEL_NAME to model.name,
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("download_${model.id}")
            .build()

        val verifyWork = OneTimeWorkRequestBuilder<ModelVerificationWorker>()
            .setInputData(workDataOf(
                ModelVerificationWorker.KEY_MODEL_ID to model.id,
                ModelVerificationWorker.KEY_FILE_PATH to filePath,
                ModelVerificationWorker.KEY_EXPECTED_SHA256 to model.sha256,
            ))
            .addTag("verify_${model.id}")
            .build()

        workManager
            .beginUniqueWork(
                "model_download_${model.id}",
                ExistingWorkPolicy.KEEP,
                downloadWork,
            )
            .then(verifyWork)
            .enqueue()

        // Store work ID
        downloadDao.update(entity.copy(
            workManagerId = downloadWork.id.toString(),
            updatedAt = System.currentTimeMillis(),
        ))

        return downloadWork.id
    }

    /**
     * Cancel an active download. Keeps partial file for resume.
     */
    suspend fun cancelDownload(modelId: String) {
        workManager.cancelUniqueWork("model_download_$modelId")
        downloadDao.updateStatus(modelId, "PAUSED", System.currentTimeMillis())
    }

    /**
     * Resume a paused download by re-enqueueing.
     */
    suspend fun resumeDownload(model: LlmModel) {
        val existing = downloadDao.getDownload(model.id)
        if (existing != null && (existing.status == "PAUSED" || existing.status == "FAILED")) {
            // Reset status to queued and re-enqueue
            downloadDao.updateStatus(model.id, "QUEUED", System.currentTimeMillis())
            enqueueNewDownload(model, existing.filePath)
        }
    }

    /**
     * Delete a downloaded model (files + DB record).
     */
    suspend fun deleteModel(modelId: String) {
        workManager.cancelUniqueWork("model_download_$modelId")
        StorageUtils.deleteModelFiles(context, modelId)
        downloadDao.delete(modelId)
    }

    /**
     * Observe download state for a specific model.
     */
    fun observeDownload(modelId: String): Flow<DownloadEntity?> {
        return downloadDao.observeDownload(modelId)
    }

    /**
     * Observe all downloads.
     */
    fun observeAllDownloads(): Flow<List<DownloadEntity>> {
        return downloadDao.observeAllDownloads()
    }

    /**
     * Get the local file path for a downloaded model, or null if not downloaded.
     */
    suspend fun getModelFile(modelId: String): File? {
        val download = downloadDao.getDownload(modelId) ?: return null
        if (download.status != "COMPLETE") return null
        val file = File(download.filePath)
        return if (file.exists()) file else null
    }

    /**
     * Check if download state is stale (e.g. app was killed).
     * Called on app start to recover from inconsistent states.
     */
    suspend fun recoverStaleDownloads() {
        // One-shot: get current state, don't hang on the Flow
        val staleDownloads = downloadDao.observeAllDownloads()
            .map { list -> list.filter { it.status == "DOWNLOADING" || it.status == "QUEUED" } }
            .first()

        for (download in staleDownloads) {
            val workId = download.workManagerId
            if (workId != null) {
                try {
                    val workInfo = workManager.getWorkInfoById(UUID.fromString(workId)).get()
                    if (workInfo == null || workInfo.state.isFinished) {
                        // Work is gone but status says downloading — mark as paused
                        downloadDao.updateStatus(
                            download.modelId,
                            "PAUSED",
                            System.currentTimeMillis(),
                        )
                    }
                } catch (e: Exception) {
                    downloadDao.updateStatus(
                        download.modelId,
                        "PAUSED",
                        System.currentTimeMillis(),
                    )
                }
            } else {
                // No work ID — stale entry, mark as paused
                downloadDao.updateStatus(
                    download.modelId,
                    "PAUSED",
                    System.currentTimeMillis(),
                )
            }
        }
    }

    private suspend fun enqueueNewDownload(model: LlmModel, filePath: String) {
        val downloadWork = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setInputData(workDataOf(
                ModelDownloadWorker.KEY_MODEL_ID to model.id,
                ModelDownloadWorker.KEY_URL to model.downloadUrl,
                ModelDownloadWorker.KEY_FILE_PATH to filePath,
                ModelDownloadWorker.KEY_TOTAL_BYTES to model.sizeBytes,
                ModelDownloadWorker.KEY_EXPECTED_SHA256 to model.sha256,
                ModelDownloadWorker.KEY_MODEL_NAME to model.name,
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("download_${model.id}")
            .build()

        val verifyWork = OneTimeWorkRequestBuilder<ModelVerificationWorker>()
            .setInputData(workDataOf(
                ModelVerificationWorker.KEY_MODEL_ID to model.id,
                ModelVerificationWorker.KEY_FILE_PATH to filePath,
                ModelVerificationWorker.KEY_EXPECTED_SHA256 to model.sha256,
            ))
            .addTag("verify_${model.id}")
            .build()

        workManager
            .beginUniqueWork(
                "model_download_${model.id}",
                ExistingWorkPolicy.REPLACE,
                downloadWork,
            )
            .then(verifyWork)
            .enqueue()

        downloadDao.getDownload(model.id)?.let { entity ->
            downloadDao.update(
                entity.copy(
                    workManagerId = downloadWork.id.toString(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }
}

class InsufficientStorageException(
    val required: Long,
    val available: Long,
) : Exception("Insufficient storage: need $required bytes, have $available bytes")
