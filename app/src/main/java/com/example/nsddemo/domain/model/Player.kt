package com.example.nsddemo.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(val name: String, val color: String) {
    companion object {
        const val UNASSIGNED_COLOR = "Unassigned"
    }
}

