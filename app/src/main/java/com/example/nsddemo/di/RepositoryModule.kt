package com.example.nsddemo.di

import com.example.nsddemo.data.repository.DataStoreSettingsRepository
import com.example.nsddemo.data.repository.GameSessionRepositoryImpl
import com.example.nsddemo.data.repository.KtorClientNetworkRepository
import com.example.nsddemo.data.repository.KtorServerNetworkRepository
import com.example.nsddemo.domain.repository.ClientNetworkRepository
import com.example.nsddemo.domain.repository.GameSessionRepository
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import com.example.nsddemo.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerNetworkRepository(
        ktorServerNetworkRepository: KtorServerNetworkRepository
    ): ServerNetworkRepository

    @Binds
    @Singleton
    abstract fun bindClientNetworkRepository(
        ktorClientNetworkRepository: KtorClientNetworkRepository
    ): ClientNetworkRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        dataStoreSettingsRepository: DataStoreSettingsRepository
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindGameSessionRepository(
        impl: GameSessionRepositoryImpl
    ): GameSessionRepository
}