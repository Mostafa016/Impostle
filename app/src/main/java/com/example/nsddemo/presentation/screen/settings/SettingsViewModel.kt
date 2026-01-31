package com.example.nsddemo.presentation.screen.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.model.AppLocales
import com.example.nsddemo.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val languageSetting = settingsRepository.userSettings
        .map { GameLocales.toLocale(it.languageCode) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GameLocales.English)

    val darkThemeSetting = settingsRepository.userSettings
        .map { it.isDarkTheme }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _languageSettingDropdownExpanded = MutableStateFlow(false)
    val languageSettingDropdownExpanded = _languageSettingDropdownExpanded.asStateFlow()

    fun onLanguageChange(locale: GameLocales) {
        _languageSettingDropdownExpanded.value = false
        viewModelScope.launch {
            // Map UI GameLocales back to Domain AppLocales
            val appLocale = AppLocales.fromCountryCode(locale.countryCode)
            settingsRepository.setLanguage(appLocale)
        }
    }

    fun onLanguageDropDownExpandedChange(isExpanded: Boolean) {
        _languageSettingDropdownExpanded.value = isExpanded
    }

    fun onLanguageDropDownDismiss() {
        _languageSettingDropdownExpanded.value = false
    }

    fun onThemeChange(isDarkTheme: Boolean) {
        Log.d(TAG, "onThemeChange: $isDarkTheme")
        viewModelScope.launch {
            settingsRepository.setDarkTheme(isDarkTheme)
        }
    }
}