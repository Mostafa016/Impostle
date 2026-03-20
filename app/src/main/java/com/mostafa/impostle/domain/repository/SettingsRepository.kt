package com.mostafa.impostle.domain.repository

import com.mostafa.impostle.domain.model.AppLocales
import com.mostafa.impostle.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userSettings: Flow<UserSettings>

    suspend fun setPlayerName(name: String)

    suspend fun setDarkTheme(enabled: Boolean)

    suspend fun setLanguage(locale: AppLocales)
}
