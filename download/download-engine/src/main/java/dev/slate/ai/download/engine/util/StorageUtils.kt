package dev.slate.ai.download.engine.util

import android.content.Context
import android.os.StatFs
import java.io.File

object StorageUtils {

    /**
     * Get the models directory under app-specific external storage.
     * Creates the directory if it doesn't exist.
     */
    fun getModelsDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get the file path for a specific model.
     */
    fun getModelFilePath(context: Context, modelId: String, fileName: String): String {
        val modelDir = File(getModelsDir(context), modelId)
        if (!modelDir.exists()) modelDir.mkdirs()
        return File(modelDir, fileName).absolutePath
    }

    /**
     * Check available storage space in bytes on the models directory.
     */
    fun getAvailableBytes(context: Context): Long {
        val modelsDir = getModelsDir(context)
        return try {
            val stat = StatFs(modelsDir.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check if there is enough space for a download.
     * Requires 10% margin above the required size.
     */
    fun hasEnoughSpace(context: Context, requiredBytes: Long): Boolean {
        val available = getAvailableBytes(context)
        val requiredWithMargin = (requiredBytes * 1.1).toLong()
        return available >= requiredWithMargin
    }

    /**
     * Delete a downloaded model and its directory.
     */
    fun deleteModelFiles(context: Context, modelId: String): Boolean {
        val modelDir = File(getModelsDir(context), modelId)
        return if (modelDir.exists()) {
            modelDir.deleteRecursively()
        } else {
            true
        }
    }

    /**
     * Get total size of downloaded models.
     */
    fun getTotalModelsSize(context: Context): Long {
        val modelsDir = getModelsDir(context)
        return if (modelsDir.exists()) {
            modelsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
    }
}
