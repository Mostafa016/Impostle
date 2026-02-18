package com.example.nsddemo.di

import com.example.nsddemo.data.local.network.socket.client.KtorSocketClient
import com.example.nsddemo.data.local.network.socket.client.SocketClient
import com.example.nsddemo.data.local.network.socket.server.KtorSocketServer
import com.example.nsddemo.data.local.network.socket.server.SocketServer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
abstract class SocketModule {

    @Binds
    abstract fun bindSocketServer(
        ktorSocketServer: KtorSocketServer
    ): SocketServer

    @Binds
    abstract fun bindSocketClient(
        ktorSocketClient: KtorSocketClient
    ): SocketClient
}
