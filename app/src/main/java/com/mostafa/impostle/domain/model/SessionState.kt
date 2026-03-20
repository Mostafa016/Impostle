package com.mostafa.impostle.domain.model

sealed interface SessionState {
    data object Idle : SessionState

    data object Connecting : SessionState

    data object Running : SessionState

    data class Error(
        val reason: String,
    ) : SessionState

    data object Disconnected : SessionState
}
