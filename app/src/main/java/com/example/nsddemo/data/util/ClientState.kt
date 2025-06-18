package com.example.nsddemo.data.util

sealed interface ClientState {
    data object Idle : ClientState
    data object Discovering : ClientState
    data object Resolving : ClientState
    data object Connecting : ClientState
    data object Connected : ClientState
    data object Disconnected : ClientState
    data class Error(val message: String, val canRetry: Boolean = false) : ClientState
}