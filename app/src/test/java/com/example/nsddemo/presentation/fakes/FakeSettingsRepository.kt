package com.example.nsddemo.presentation.fakes

import com.example.nsddemo.domain.model.AppLocales
import com.example.nsddemo.domain.model.UserSettings
import com.example.nsddemo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeSettingsRepository(
    initialPlayerName: String? = null,
) : SettingsRepository {
    private val _userSettings =
        MutableStateFlow(
            UserSettings(
                playerId = "test-uuid-1234",
                playerName = initialPlayerName,
                isDarkTheme = false,
                languageCode = "en",
            ),
        )

    override val userSettings: Flow<UserSettings> = _userSettings

    override suspend fun setPlayerName(name: String) {
        _userSettings.update { it.copy(playerName = name) }
    }

    override suspend fun setDarkTheme(enabled: Boolean) {
        _userSettings.update { it.copy(isDarkTheme = enabled) }
    }

    override suspend fun setLanguage(locale: AppLocales) {
        _userSettings.update { it.copy(languageCode = locale.countryCode) }
    }
}
