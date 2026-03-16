package com.example.nsddemo.domain.repository

import com.example.nsddemo.domain.model.AppLocales
import com.example.nsddemo.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userSettings: Flow<UserSettings>

    suspend fun setPlayerName(name: String)

    suspend fun setDarkTheme(enabled: Boolean)

    suspend fun setLanguage(locale: AppLocales)
}
