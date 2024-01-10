package com.example.nsddemo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.nsddemo.R


private val defaultTypography = Typography()

val caveatFontFamily = FontFamily(
    Font(R.font.caveat_regular, FontWeight.Normal),
    Font(R.font.caveat_medium, FontWeight.Medium),
    Font(R.font.caveat_semibold, FontWeight.SemiBold),
    Font(R.font.caveat_bold, FontWeight.Bold)
)

val englishTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = caveatFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = caveatFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = caveatFontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = caveatFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = caveatFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = caveatFontFamily),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = caveatFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = caveatFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = caveatFontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = caveatFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = caveatFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = caveatFontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = caveatFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = caveatFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = caveatFontFamily)
)

val tsAnamilFontFamily = FontFamily(
    Font(R.font.tsanamil_regular, FontWeight.Normal),
    Font(R.font.tsanamil_regular, FontWeight.Medium),
    Font(R.font.tsanamil_bold, FontWeight.SemiBold),
    Font(R.font.tsanamil_bold, FontWeight.Bold)
)

val arabicTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = tsAnamilFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = tsAnamilFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = tsAnamilFontFamily),

    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = tsAnamilFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = tsAnamilFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = tsAnamilFontFamily),

    titleLarge = defaultTypography.titleLarge.copy(fontFamily = tsAnamilFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = tsAnamilFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = tsAnamilFontFamily),

    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = tsAnamilFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = tsAnamilFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = tsAnamilFontFamily),

    labelLarge = defaultTypography.labelLarge.copy(fontFamily = tsAnamilFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = tsAnamilFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = tsAnamilFontFamily)
)