package com.example.nsddemo.domain.model

data class UserSettings(
    val playerId: String,
    val playerName: String?,
    val isDarkTheme: Boolean,
    val languageCode: String
)
