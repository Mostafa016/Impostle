package com.example.nsddemo.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.nsddemo.R


val chakraPetchFontFamily = FontFamily(
    Font(R.font.chakra_petch_light, FontWeight.Normal),
    Font(R.font.chakra_petch_medium, FontWeight.Medium),
    Font(R.font.chakra_petch_semibold, FontWeight.SemiBold),
    Font(R.font.chakra_petch_bold, FontWeight.Bold),
    // If you have a Black/ExtraBold weight, add it here:
    // Font(R.font.chakra_petch_black, FontWeight.Black)
)

val spaceMonoFontFamily = FontFamily(
    Font(R.font.space_mono, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

// 2. Map Material 3 Roles to the "Cyber-Brutalist" System
val englishTypography = Typography(

    // --- DISPLAY (Hero Titles like "IMPOSTLE", "GAME OVER") ---
    displayLarge = defaultTypography.displayLarge.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Black, // Thick and punchy
        letterSpacing = (-1).sp // Tight tracking for large text
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Bold
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Bold
    ),

    // --- HEADLINE (Screen Titles like "SETTINGS", "LOBBY") ---
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Bold
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.SemiBold
    ),

    // --- TITLE (Card Headers, Primary Buttons) ---
    titleLarge = defaultTypography.titleLarge.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp // Slight spacing for uppercase buttons
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontFamily = chakraPetchFontFamily,
        fontWeight = FontWeight.Medium
    ),

    // --- BODY (Player Lists, Descriptions, Instructions) ---
    // Swaps to Space Mono for that "Data/Terminal" look
    bodyLarge = defaultTypography.bodyLarge.copy(
        fontFamily = spaceMonoFontFamily,
        fontWeight = FontWeight.Bold, // Brutalist body is often bold
        fontSize = 16.sp
    ),
    bodyMedium = defaultTypography.bodyMedium.copy(
        fontFamily = spaceMonoFontFamily,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = defaultTypography.bodySmall.copy(
        fontFamily = spaceMonoFontFamily,
        fontWeight = FontWeight.Normal
    ),

    // --- LABEL (Tags, Room Codes, Small Metadata) ---
    labelLarge = defaultTypography.labelLarge.copy(
        fontFamily = spaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp // Widely tracked for "tech" feel
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontFamily = spaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontFamily = spaceMonoFontFamily,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
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