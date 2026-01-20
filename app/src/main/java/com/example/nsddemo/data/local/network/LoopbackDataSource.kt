package com.example.nsddemo.data.local.network

import com.example.nsddemo.domain.model.ClientMessage
import com.example.nsddemo.domain.model.ServerMessage
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow

@Singleton
class LoopbackDataSource @Inject constructor() {
    val clientToServer = MutableSharedFlow<Pair<String, ClientMessage>>(extraBufferCapacity = 64)
    val serverToClient = MutableSharedFlow<Pair<String, ServerMessage>>(extraBufferCapacity = 64)

    companion object {
        const val LOCAL_HOST_CLIENT_ID = "LOCAL_HOST_CONNECTION"
    }
}