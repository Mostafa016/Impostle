package com.example.nsddemo.data.util

sealed interface ServerState {
    data object Idle : ServerState
    data class Running(val port: Int, val gameCode: String) : ServerState
    data class Error(val message: String) : ServerState
}