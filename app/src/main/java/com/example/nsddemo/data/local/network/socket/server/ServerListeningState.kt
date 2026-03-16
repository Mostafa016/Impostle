package com.example.nsddemo.data.local.network.socket.server

sealed interface ServerListeningState {
    data object Idle : ServerListeningState

    data class Listening(
        val port: Int,
    ) : ServerListeningState

    data class Error(
        val message: String,
    ) : ServerListeningState
}
