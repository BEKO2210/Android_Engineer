package dev.slate.ai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.slate.ai.core.database.dao.DownloadDao
import dev.slate.ai.core.database.entity.DownloadEntity

@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SlateDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
