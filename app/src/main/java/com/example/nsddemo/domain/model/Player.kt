package com.example.nsddemo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val color: String,
    val id: String = "",
    val isConnected: Boolean = true
) {
    companion object {
        const val UNASSIGNED_COLOR = "Unassigned"
    }
}

