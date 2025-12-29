package com.example.nsddemo.presentation.screen.settings

import com.example.nsddemo.R

enum class GameLocales(val countryCode: String, val languageStringResId: Int) {
    English("en", R.string.english),
    Arabic("ar", R.string.arabic);

    companion object {
        fun toLocale(countryCode: String): GameLocales =
            entries.find { it.countryCode == countryCode }!!
    }
}