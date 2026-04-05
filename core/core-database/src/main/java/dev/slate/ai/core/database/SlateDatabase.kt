package dev.slate.ai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.slate.ai.core.database.dao.ConversationDao
import dev.slate.ai.core.database.dao.DownloadDao
import dev.slate.ai.core.database.dao.MessageDao
import dev.slate.ai.core.database.entity.ConversationEntity
import dev.slate.ai.core.database.entity.DownloadEntity
import dev.slate.ai.core.database.entity.MessageEntity

@Database(
    entities = [
        DownloadEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class SlateDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}
