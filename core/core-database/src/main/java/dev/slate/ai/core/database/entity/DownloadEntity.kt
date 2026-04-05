package dev.slate.ai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val modelId: String,
    val modelName: String,
    val url: String,
    val expectedSha256: String,
    val filePath: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String,
    val workManagerId: String?,
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
