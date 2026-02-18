package com.example.nsddemo.domain.util

import com.example.nsddemo.domain.model.GamePhase

enum class MessageDirection { ClientToServer, ServerToClient }

data class MessageDefinition(
    val code: String, // Short code for logs
    val description: String,
    val direction: MessageDirection,
    val expectedPhase: GamePhase?,
    val isLoggable: Boolean = true
)