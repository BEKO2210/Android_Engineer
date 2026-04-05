package dev.slate.ai.download.engine.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.slate.ai.core.database.SlateDatabase
import dev.slate.ai.core.database.dao.DownloadDao
import javax.inject.Singleton

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
        ).build()
    }

    @Provides
    fun provideDownloadDao(database: SlateDatabase): DownloadDao {
        return database.downloadDao()
    }
}
