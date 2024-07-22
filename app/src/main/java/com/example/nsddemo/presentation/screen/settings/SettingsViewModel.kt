package com.example.nsddemo.presentation.screen.settings

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SettingsViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {
    private val _languageSetting = mutableStateOf(getAppLocale())
    val languageSetting: State<GameLocales> = _languageSetting

    private val _languageSettingDropdownExpanded = mutableStateOf(false)
    val languageSettingDropdownExpanded: State<Boolean> = _languageSettingDropdownExpanded

    private val _darkThemeSetting = mutableStateOf(false)
    val darkThemeSetting: State<Boolean> = _darkThemeSetting

    init {
        _darkThemeSetting.value = sharedPreferences.getBoolean(THEME_SHARED_PREF_KEY, false)
    }

    fun onLanguageChange(locale: GameLocales) {
        _languageSetting.value = locale
        _languageSettingDropdownExpanded.value = false
        changeAppLocale(countryCode = locale.countryCode)
    }

    fun onLanguageDropDownExpandedChange(isExpanded: Boolean) {
        _languageSettingDropdownExpanded.value = isExpanded
    }

    fun onLanguageDropDownDismiss() {
        _languageSettingDropdownExpanded.value = false
    }

    fun onThemeChange(isDarkTheme: Boolean) {
        _darkThemeSetting.value = isDarkTheme
        with(sharedPreferences.edit()) {
            putBoolean(THEME_SHARED_PREF_KEY, isDarkTheme)
            apply()
        }
    }

    private fun changeAppLocale(countryCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(countryCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    private fun getAppLocale(): GameLocales {
        return if (AppCompatDelegate.getApplicationLocales().isEmpty) GameLocales.English
        else GameLocales.toLocale(AppCompatDelegate.getApplicationLocales()[0].toString())
    }

    companion object {
        private const val THEME_SHARED_PREF_KEY = "theme_impostle"

        @Suppress("UNCHECKED_CAST")
        class SettingsViewModelFactory(
            private val sharedPreferences: SharedPreferences
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(sharedPreferences) as T
        }
    }
}