package com.example.nsddemo.di

import com.example.nsddemo.data.local.network.socket.client.KtorSocketClient
import com.example.nsddemo.data.local.network.socket.client.SocketClient
import com.example.nsddemo.data.local.network.socket.server.KtorSocketServer
import com.example.nsddemo.data.local.network.socket.server.SocketServer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class SocketModule {

    @Binds
    @Singleton
    abstract fun bindSocketServer(
        ktorSocketServer: KtorSocketServer
    ): SocketServer

    @Binds
    @Singleton
    abstract fun bindSocketClient(
        ktorSocketClient: KtorSocketClient
    ): SocketClient
}
