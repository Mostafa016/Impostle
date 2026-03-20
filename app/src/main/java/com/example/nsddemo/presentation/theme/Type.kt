package com.example.nsddemo.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.nsddemo.R

val chakraPetchFontFamily =
    FontFamily(
        Font(R.font.chakra_petch_light, FontWeight.Normal),
        Font(R.font.chakra_petch_medium, FontWeight.Medium),
        Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
        Font(R.font.chakra_petch_bold, FontWeight.Bold),
    )

val spaceMonoFontFamily =
    FontFamily(
        Font(R.font.space_mono, FontWeight.Normal),
        Font(R.font.space_mono_bold, FontWeight.Bold),
    )

private val defaultTypography = Typography()

val englishTypography =
    Typography(
        // --- DISPLAY (Hero Titles like "IMPOSTLE", "GAME OVER") ---
        displayLarge =
            defaultTypography.displayLarge.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Black, // Thick and punchy
                letterSpacing = (-1).sp, // Tight tracking for large text
            ),
        displayMedium =
            defaultTypography.displayMedium.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        displaySmall =
            defaultTypography.displaySmall.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        // --- HEADLINE (Screen Titles like "SETTINGS", "LOBBY") ---
        headlineLarge =
            defaultTypography.headlineLarge.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            defaultTypography.headlineMedium.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
            ),
        headlineSmall =
            defaultTypography.headlineSmall.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.SemiBold,
            ),
        // --- TITLE (Card Headers, Primary Buttons) ---
        titleLarge =
            defaultTypography.titleLarge.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, // Slight spacing for uppercase buttons
            ),
        titleMedium =
            defaultTypography.titleMedium.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Medium,
            ),
        titleSmall =
            defaultTypography.titleSmall.copy(
                fontFamily = chakraPetchFontFamily,
                fontWeight = FontWeight.Medium,
            ),
        // --- BODY (Player Lists, Descriptions, Instructions) ---
        // Swaps to Space Mono for that "Data/Terminal" look
        bodyLarge =
            defaultTypography.bodyLarge.copy(
                fontFamily = spaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            ),
        bodyMedium =
            defaultTypography.bodyMedium.copy(
                fontFamily = spaceMonoFontFamily,
                fontWeight = FontWeight.Normal,
            ),
        bodySmall =
            defaultTypography.bodySmall.copy(
                fontFamily = spaceMonoFontFamily,
                fontWeight = FontWeight.Normal,
            ),
        // --- LABEL (Tags, Room Codes, Small Metadata) ---
        labelLarge =
            defaultTypography.labelLarge.copy(
                fontFamily = spaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
        labelMedium =
            defaultTypography.labelMedium.copy(
                fontFamily = spaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            ),
        labelSmall =
            defaultTypography.labelSmall.copy(
                fontFamily = spaceMonoFontFamily,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
    )

val cairoFontFamily =
    FontFamily(
        Font(R.font.cairo_light, FontWeight.Normal),
        Font(R.font.cairo_medium, FontWeight.Medium),
        Font(R.font.cairo_semibold, FontWeight.SemiBold),
        Font(R.font.cairo_bold, FontWeight.Bold),
        Font(R.font.cairo_black, FontWeight.Black),
    )

val ibmPlexSansArabicFontFamily =
    FontFamily(
        Font(R.font.ibm_plex_sans_arabic, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
    )

val arabicTypography =
    Typography(
        displayLarge =
            englishTypography.displayLarge.copy(
                fontFamily = cairoFontFamily,
                letterSpacing = 0.sp, // Arabic doesn't handle negative tracking well
            ),
        displayMedium = englishTypography.displayMedium.copy(fontFamily = cairoFontFamily),
        displaySmall = englishTypography.displaySmall.copy(fontFamily = cairoFontFamily),
        headlineLarge = englishTypography.headlineLarge.copy(fontFamily = cairoFontFamily),
        headlineMedium = englishTypography.headlineMedium.copy(fontFamily = cairoFontFamily),
        headlineSmall = englishTypography.headlineSmall.copy(fontFamily = cairoFontFamily),
        titleLarge =
            englishTypography.titleLarge.copy(
                fontFamily = cairoFontFamily,
                letterSpacing = 0.sp, // Disable letter spacing for Arabic scripts
            ),
        // Body & Labels use IBM Plex Sans Arabic
        bodyLarge =
            englishTypography.bodyLarge.copy(
                fontFamily = ibmPlexSansArabicFontFamily,
                fontSize = 17.sp, // Bumped slightly for Arabic legibility
            ),
        bodyMedium = englishTypography.bodyMedium.copy(fontFamily = ibmPlexSansArabicFontFamily),
        bodySmall = englishTypography.bodySmall.copy(fontFamily = ibmPlexSansArabicFontFamily),
        labelLarge =
            englishTypography.labelLarge.copy(
                fontFamily = ibmPlexSansArabicFontFamily,
                letterSpacing = 0.sp,
            ),
        labelMedium = englishTypography.labelMedium.copy(fontFamily = ibmPlexSansArabicFontFamily),
        labelSmall = englishTypography.labelSmall.copy(fontFamily = ibmPlexSansArabicFontFamily),
    )
