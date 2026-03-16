package com.example.nsddemo.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.data.local.settings.AppLocaleHelper
import com.example.nsddemo.domain.model.AppLocales
import com.example.nsddemo.domain.model.UserSettings
import com.example.nsddemo.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class DataStoreSettingsRepository
    @Inject
    constructor(
        private val settingsDataStore: DataStore<Preferences>,
        private val appLocaleHelper: AppLocaleHelper,
    ) : SettingsRepository {
        @OptIn(ExperimentalUuidApi::class)
        override val userSettings: Flow<UserSettings> =
            settingsDataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Log.e(
                            TAG,
                            "DataStoreSettingsRepository: " + exception.message.toString(),
                            exception,
                        )
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }.map { preferences ->
                    val playerId = preferences[PLAYER_ID] ?: Uuid.random().toString()
                    UserSettings(
                        playerId = playerId,
                        playerName = preferences[PLAYER_NAME],
                        isDarkTheme = preferences[DARK_THEME] ?: false,
                        languageCode =
                            preferences[LANGUAGE]
                                ?: appLocaleHelper.getCurrentLocale().countryCode,
                    )
                }

        override suspend fun setPlayerName(name: String) {
            settingsDataStore.edit {
                it[PLAYER_NAME] = name
                if (it[PLAYER_ID] == null) {
                    @OptIn(ExperimentalUuidApi::class)
                    it[PLAYER_ID] = Uuid.random().toString()
                }
            }
        }

        override suspend fun setDarkTheme(enabled: Boolean) {
            settingsDataStore.edit { it[DARK_THEME] = enabled }
        }

        override suspend fun setLanguage(locale: AppLocales) {
            appLocaleHelper.changeLocale(locale)
            settingsDataStore.edit { it[LANGUAGE] = locale.countryCode }
        }

        private companion object {
            val PLAYER_ID = stringPreferencesKey("player_id")
            val PLAYER_NAME = stringPreferencesKey("player_name")
            val DARK_THEME = booleanPreferencesKey("dark_theme")
            val LANGUAGE = stringPreferencesKey("language")
        }
    }
