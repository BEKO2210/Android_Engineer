package dev.slate.ai.download.engine.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.slate.ai.core.database.dao.DownloadDao
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val downloadDao: DownloadDao,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MODEL_ID = "model_id"
        const val KEY_URL = "url"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_EXPECTED_SHA256 = "expected_sha256"
        const val KEY_MODEL_NAME = "model_name"

        const val PROGRESS_BYTES = "progress_bytes"
        const val PROGRESS_TOTAL = "progress_total"

        const val CHANNEL_ID = "slate_download"
        const val NOTIFICATION_ID = 1001

        private const val BUFFER_SIZE = 65536 // 64KB
        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L

        // HTTP codes that are retryable
        private val RETRYABLE_CODES = setOf(408, 429, 500, 502, 503, 504)
    }

    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val expectedTotal = inputData.getLong(KEY_TOTAL_BYTES, 0L)
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"

        createNotificationChannel()

        val partFile = File("$filePath.part")
        val targetFile = File(filePath)

        // If final file already exists with correct size and no partial file, skip to verify
        if (targetFile.exists() && !partFile.exists() && expectedTotal > 0 && targetFile.length() == expectedTotal) {
            updateDbStatus(modelId, "VERIFYING")
            return Result.success(workDataOf(KEY_MODEL_ID to modelId))
        }

        // If both target and part file exist, clean up inconsistent state
        if (targetFile.exists() && partFile.exists()) {
            targetFile.delete()
        }

        try {
            // Try to show foreground notification (may fail on some devices)
            try {
                setForeground(createForegroundInfo(modelName, 0, expectedTotal))
            } catch (e: Exception) {
                Log.w("SlateDownload", "Could not set foreground: ${e.message}")
                // Continue without foreground — download still works
            }

            // Check existing partial file for resume
            val existingBytes = if (partFile.exists()) partFile.length() else 0L

            val requestBuilder = Request.Builder().url(url)
            if (existingBytes > 0) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()

            try {
                // Handle error responses
                if (!response.isSuccessful && response.code != 206) {
                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    return if (response.code in RETRYABLE_CODES) {
                        updateDbError(modelId, errorMsg)
                        Result.retry()
                    } else {
                        // Non-retryable (404, 403, etc.) — permanent failure
                        updateDbError(modelId, errorMsg)
                        Result.failure()
                    }
                }

                val body = response.body ?: run {
                    updateDbError(modelId, "Empty response body")
                    return Result.retry()
                }

                val isResumed = response.code == 206
                val contentLength = body.contentLength()

                // Validate resume: if we asked for range but server returned 200, start fresh
                val startOffset: Long
                val appendMode: Boolean
                if (existingBytes > 0 && isResumed) {
                    // Server supports range — append to partial file
                    startOffset = existingBytes
                    appendMode = true
                } else {
                    // Fresh download (server returned 200, or no partial file)
                    if (partFile.exists()) partFile.delete()
                    startOffset = 0L
                    appendMode = false
                }

                val totalBytes = if (isResumed) {
                    existingBytes + contentLength
                } else {
                    if (contentLength > 0) contentLength else expectedTotal
                }

                val outputStream = FileOutputStream(partFile, appendMode)

                updateDbStatus(modelId, "DOWNLOADING")
                updateDbProgress(modelId, startOffset)

                var bytesWritten = startOffset
                var lastProgressUpdate = System.currentTimeMillis()
                val buffer = ByteArray(BUFFER_SIZE)

                body.byteStream().use { inputStream ->
                    outputStream.use { output ->
                        while (true) {
                            if (isStopped) {
                                updateDbStatus(modelId, "PAUSED")
                                updateDbProgress(modelId, bytesWritten)
                                return Result.failure()
                            }

                            val read = inputStream.read(buffer)
                            if (read == -1) break

                            output.write(buffer, 0, read)
                            bytesWritten += read

                            val now = System.currentTimeMillis()
                            if (now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                                lastProgressUpdate = now

                                setProgress(workDataOf(
                                    PROGRESS_BYTES to bytesWritten,
                                    PROGRESS_TOTAL to totalBytes,
                                ))

                                updateDbProgress(modelId, bytesWritten)

                                try {
                                    setForeground(createForegroundInfo(modelName, bytesWritten, totalBytes))
                                } catch (_: Exception) {
                                    // Notification update failure is non-fatal
                                }
                            }
                        }
                    }
                }

                // Final progress update
                updateDbProgress(modelId, bytesWritten)
                setProgress(workDataOf(
                    PROGRESS_BYTES to bytesWritten,
                    PROGRESS_TOTAL to totalBytes,
                ))

                // Rename .part to final file
                if (partFile.exists()) {
                    if (targetFile.exists()) targetFile.delete()
                    if (!partFile.renameTo(targetFile)) {
                        // Fallback: copy and delete
                        partFile.copyTo(targetFile, overwrite = true)
                        if (!partFile.delete()) {
                            // Non-fatal: .part file lingers but download is complete
                        }
                    }
                }

                updateDbStatus(modelId, "VERIFYING")
                return Result.success(workDataOf(KEY_MODEL_ID to modelId))

            } finally {
                response.close()
            }

        } catch (e: IOException) {
            updateDbError(modelId, "Network error: ${e.message.orEmpty()}")
            return Result.retry()
        } catch (e: Exception) {
            updateDbError(modelId, "Download failed: ${e.message.orEmpty()}")
            return Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "Model"
        return createForegroundInfo(modelName, 0, 0)
    }

    private fun createForegroundInfo(
        modelName: String,
        bytesDownloaded: Long,
        totalBytes: Long,
    ): ForegroundInfo {
        val progress = if (totalBytes > 0) {
            ((bytesDownloaded * 100) / totalBytes).toInt()
        } else 0

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading $modelName")
            .setContentText("$progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, totalBytes <= 0)
            .setOngoing(true)
            .setSilent(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Model Downloads",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows download progress for AI models"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private suspend fun updateDbStatus(modelId: String, status: String) {
        downloadDao.updateStatus(modelId, status, System.currentTimeMillis())
    }

    private suspend fun updateDbProgress(modelId: String, bytes: Long) {
        downloadDao.updateProgress(modelId, bytes, System.currentTimeMillis())
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
