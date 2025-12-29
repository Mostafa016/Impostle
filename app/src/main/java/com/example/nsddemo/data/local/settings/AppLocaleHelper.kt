package com.example.nsddemo.data.local.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.nsddemo.domain.model.AppLocales

class AppLocaleHelper {
    fun changeLocale(locale: AppLocales) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(locale.countryCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getCurrentLocale(): AppLocales {
        return if (AppCompatDelegate.getApplicationLocales().isEmpty) AppLocales.English
        else AppLocales.fromCountryCode(AppCompatDelegate.getApplicationLocales()[0].toString())
    }
}