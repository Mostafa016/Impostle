package com.example.nsddemo.di

import com.example.nsddemo.data.local.network.LoopbackDataSource
import com.example.nsddemo.data.local.network.nsd.discovery.NetworkDiscovery
import com.example.nsddemo.data.local.network.nsd.resolution.NetworkResolution
import com.example.nsddemo.data.local.network.socket.client.SocketClient
import com.example.nsddemo.data.repository.DataStoreSettingsRepository
import com.example.nsddemo.data.repository.GameSessionRepositoryImpl
import com.example.nsddemo.data.repository.HostServerNetworkRepository
import com.example.nsddemo.data.repository.InMemoryWordRepository
import com.example.nsddemo.data.repository.LoopbackClientNetworkRepository
import com.example.nsddemo.data.repository.RemoteClientNetworkRepository
import com.example.nsddemo.domain.repository.GameSessionRepository
import com.example.nsddemo.domain.repository.ServerNetworkRepository
import com.example.nsddemo.domain.repository.SettingsRepository
import com.example.nsddemo.domain.repository.WordRepository
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
