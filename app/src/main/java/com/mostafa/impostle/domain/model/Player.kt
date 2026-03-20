package com.mostafa.impostle.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val name: String,
    val color: String,
    val id: String = "",
    val isConnected: Boolean = true,
)
