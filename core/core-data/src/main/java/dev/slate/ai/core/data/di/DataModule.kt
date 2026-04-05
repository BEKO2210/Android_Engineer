package dev.slate.ai.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.slate.ai.core.data.repository.ModelRepository
import dev.slate.ai.core.data.repository.ModelRepositoryImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindModelRepository(
        impl: ModelRepositoryImpl,
    ): ModelRepository
}
