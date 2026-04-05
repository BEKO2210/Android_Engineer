package dev.slate.ai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.slate.ai.core.database.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE modelId = :modelId")
    fun observeDownload(modelId: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads")
    fun observeAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE modelId = :modelId")
    suspend fun getDownload(modelId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETE'")
    suspend fun getCompletedDownloads(): List<DownloadEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE modelId = :modelId")
    suspend fun delete(modelId: String)

    @Query("UPDATE downloads SET status = :status, updatedAt = :updatedAt WHERE modelId = :modelId")
    suspend fun updateStatus(modelId: String, status: String, updatedAt: Long)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, updatedAt = :updatedAt WHERE modelId = :modelId")
    suspend fun updateProgress(modelId: String, bytes: Long, updatedAt: Long)
}
