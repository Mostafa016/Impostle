package com.mostafa.impostle.presentation.screen.settings

import com.mostafa.impostle.R

enum class GameLocales(
    val countryCode: String,
    val languageStringResId: Int,
) {
    English("en", R.string.english),
    Arabic("ar", R.string.arabic),
    ;

    companion object {
        fun toLocale(countryCode: String): GameLocales = entries.find { it.countryCode == countryCode }!!
    }
}
