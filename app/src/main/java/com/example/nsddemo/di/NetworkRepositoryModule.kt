package com.example.nsddemo.di

import com.example.nsddemo.data.repository.ClientNetworkRepository
import com.example.nsddemo.data.repository.KtorClientNetworkRepository
import com.example.nsddemo.data.repository.KtorServerNetworkRepository
import com.example.nsddemo.data.repository.ServerNetworkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkRepositoryModule { // Changed to abstract class

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
}