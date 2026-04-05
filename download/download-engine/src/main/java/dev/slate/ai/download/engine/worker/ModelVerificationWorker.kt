package dev.slate.ai.download.engine.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.slate.ai.core.database.dao.DownloadDao
import java.io.File
import java.security.MessageDigest

@HiltWorker
class ModelVerificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_EXPECTED_SHA256 = "expected_sha256"

        const val RESULT_VERIFIED = "verified"
        const val RESULT_FILE_PATH = "file_path"

        private const val HASH_BUFFER_SIZE = 65536 // 64KB
    }

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val expectedSha256 = inputData.getString(KEY_EXPECTED_SHA256) ?: ""

        val file = File(filePath)
        if (!file.exists()) {
            updateDbError(modelId, "Downloaded file not found")
            return Result.failure()
        }

        // If no expected hash provided, skip verification and mark complete
        if (expectedSha256.isBlank()) {
            updateDbStatus(modelId, "COMPLETE")
            return Result.success(workDataOf(
                RESULT_VERIFIED to true,
                RESULT_FILE_PATH to filePath,
            ))
        }

        try {
            updateDbStatus(modelId, "VERIFYING")

            val actualHash = computeSha256(file)

            if (actualHash.equals(expectedSha256, ignoreCase = true)) {
                updateDbStatus(modelId, "COMPLETE")
                return Result.success(workDataOf(
                    RESULT_VERIFIED to true,
                    RESULT_FILE_PATH to filePath,
                ))
            } else {
                // Hash mismatch — corrupted download
                file.delete()
                File("$filePath.part").delete()
                updateDbError(modelId, "Integrity check failed. File has been removed.")
                return Result.failure()
            }
        } catch (e: Exception) {
            updateDbError(modelId, "Verification error: ${e.message}")
            return Result.failure()
        }
    }

    private fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(HASH_BUFFER_SIZE)

        file.inputStream().use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun updateDbStatus(modelId: String, status: String) {
        downloadDao.updateStatus(modelId, status, System.currentTimeMillis())
    }

    private suspend fun updateDbError(modelId: String, message: String) {
        val entity = downloadDao.getDownload(modelId) ?: return
        downloadDao.update(entity.copy(
            status = "FAILED",
            errorMessage = message,
            updatedAt = System.currentTimeMillis(),
        ))
    }
}
