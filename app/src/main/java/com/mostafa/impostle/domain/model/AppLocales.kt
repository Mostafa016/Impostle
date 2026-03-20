package com.mostafa.impostle.domain.model

enum class AppLocales(
    val countryCode: String,
) {
    English("en"),
    Arabic("ar"),
    ;

    companion object {
        fun fromCountryCode(countryCode: String): AppLocales =
            entries.find { it.countryCode == countryCode }
                ?: throw IllegalArgumentException("Invalid or unsupported country code.")
    }
}
