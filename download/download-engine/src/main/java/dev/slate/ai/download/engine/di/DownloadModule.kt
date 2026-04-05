package dev.slate.ai.download.engine.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.slate.ai.core.database.SlateDatabase
import dev.slate.ai.core.database.dao.ConversationDao
import dev.slate.ai.core.database.dao.DownloadDao
import dev.slate.ai.core.database.dao.MessageDao
import javax.inject.Singleton

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS conversations (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                modelId TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS messages (
                id TEXT NOT NULL PRIMARY KEY,
                conversationId TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                isComplete INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(conversationId) REFERENCES conversations(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_messages_conversationId ON messages(conversationId)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SlateDatabase {
        return Room.databaseBuilder(
            context,
            SlateDatabase::class.java,
            "slate_database",
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDownloadDao(database: SlateDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideConversationDao(database: SlateDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    fun provideMessageDao(database: SlateDatabase): MessageDao {
        return database.messageDao()
    }
}
