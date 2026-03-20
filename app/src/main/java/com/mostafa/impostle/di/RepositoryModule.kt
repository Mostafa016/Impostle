package com.mostafa.impostle.di

import com.mostafa.impostle.data.local.network.LoopbackDataSource
import com.mostafa.impostle.data.local.network.nsd.discovery.NetworkDiscovery
import com.mostafa.impostle.data.local.network.nsd.resolution.NetworkResolution
import com.mostafa.impostle.data.local.network.socket.client.SocketClient
import com.mostafa.impostle.data.repository.DataStoreSettingsRepository
import com.mostafa.impostle.data.repository.GameSessionRepositoryImpl
import com.mostafa.impostle.data.repository.HostServerNetworkRepository
import com.mostafa.impostle.data.repository.InMemoryWordRepository
import com.mostafa.impostle.data.repository.LoopbackClientNetworkRepository
import com.mostafa.impostle.data.repository.RemoteClientNetworkRepository
import com.mostafa.impostle.domain.repository.GameSessionRepository
import com.mostafa.impostle.domain.repository.ServerNetworkRepository
import com.mostafa.impostle.domain.repository.SettingsRepository
import com.mostafa.impostle.domain.repository.WordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindServerNetworkRepository(hostServerNetworkRepository: HostServerNetworkRepository): ServerNetworkRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(dataStoreSettingsRepository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindGameSessionRepository(impl: GameSessionRepositoryImpl): GameSessionRepository

    @Binds
    @Singleton
    abstract fun bindWordRepository(inMemoryWordRepository: InMemoryWordRepository): WordRepository

    companion object {
        @Provides
        fun provideRemoteClientNetworkRepository(
            networkDiscovery: NetworkDiscovery,
            networkResolution: NetworkResolution,
            socketClient: SocketClient,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ) = RemoteClientNetworkRepository(
            networkDiscovery,
            networkResolution,
            socketClient,
            ioDispatcher,
        )

        @Provides
        fun provideLoopbackClientNetworkRepository(loopbackDataSource: LoopbackDataSource) =
            LoopbackClientNetworkRepository(loopbackDataSource)

        @Provides
        @Singleton
        fun provideLoopbackDataSource() = LoopbackDataSource()
    }
}
