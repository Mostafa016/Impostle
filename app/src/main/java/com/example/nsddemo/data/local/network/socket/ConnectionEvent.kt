package com.example.nsddemo.data.local.network.socket

sealed interface ConnectionEvent {
    data class Connected(val id: String) : ConnectionEvent
    data class Disconnected(val clientId: String) : ConnectionEvent
    data class Error(val id: String?, val message: String) : ConnectionEvent
}