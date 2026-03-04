package com.example.nsddemo.presentation.screen.settings

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.CornerBrackets
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.settings.components.BrutalistDropdown
import com.example.nsddemo.presentation.screen.settings.components.BrutalistToggle
import com.example.nsddemo.presentation.screen.settings.components.SettingColumn
import com.example.nsddemo.presentation.screen.settings.components.SettingRow
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavHostController
) {
    val isDarkTheme by viewModel.darkThemeSetting.collectAsState()
    val language by viewModel.languageSetting.collectAsState()
    val isDropdownExpanded by viewModel.languageSettingDropdownExpanded.collectAsState()

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    if (event.popPrevious) {
                        navController.popBackStackAndNavigateTo(route = event.destination)
                    } else {
                        navController.navigate(route = event.destination)
                    }
                }

                else -> { /* Handle other one-off events */
                }
            }
        }
    }

    SettingsContent(
        isDarkTheme = isDarkTheme,
        currentLanguage = language,
        isDropdownExpanded = isDropdownExpanded,
        onThemeChange = viewModel::onThemeChange,
        onLanguageDropdownExpandChange = viewModel::onLanguageDropDownExpandedChange,
        onLanguageChange = viewModel::onLanguageChange,
        onDropdownDismiss = viewModel::onLanguageDropDownDismiss,
        onSaveChangesClick = viewModel::onSaveChangesClick
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun SettingsContent(
    isDarkTheme: Boolean,
    currentLanguage: GameLocales,
    isDropdownExpanded: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLanguageDropdownExpandChange: (Boolean) -> Unit,
    onLanguageChange: (GameLocales) -> Unit,
    onDropdownDismiss: () -> Unit,
    onSaveChangesClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                gridLineColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = "SETTINGS /// CONFIGURATION /// SYSTEM PREFERENCES /// AUDIO & VISUAL /// ",
                backgroundColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = BrutalistDimens.SpacingLarge)
                    .padding(top = BrutalistDimens.SpacingLarge),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- MAIN SETTINGS CARD ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            shadowOffset = BrutalistDimens.ShadowMedium,
                            borderWidth = BrutalistDimens.BorderThick
                        )
                        .padding(BrutalistDimens.SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingXLarge)
                ) {

                    // 1. Language Setting
                    SettingColumn(
                        label = stringResource(R.string.language),
                        iconResId = R.drawable.sharp_language_24
                    ) {
                        BrutalistDropdown(
                            currentValue = stringResource(currentLanguage.languageStringResId),
                            expanded = isDropdownExpanded,
                            onExpandChange = onLanguageDropdownExpandChange,
                            onDismissRequest = onDropdownDismiss,
                            options = GameLocales.entries,
                            onOptionSelected = onLanguageChange
                        )
                    }

                    // 2. Dark Mode Setting
                    SettingRow(
                        label = stringResource(R.string.dark_theme),
                        iconResId = if (isDarkTheme) R.drawable.sharp_dark_mode_24 else R.drawable.sharp_light_mode_24
                    ) {
                        BrutalistToggle(
                            checked = isDarkTheme,
                            onCheckedChange = onThemeChange
                        )
                    }

                    // 3. SAVE CHANGES BUTTON
                    BrutalistButton(
                        text = "SAVE CHANGES",
                        icon = ImageVector.vectorResource(R.drawable.sharp_save_24),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onSaveChangesClick
                    )
                }
            }
        }

        // --- DECORATION ---
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding()
        )
    }
}

// ============================================================================
// 4. PREVIEWS
// ============================================================================

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun SettingsPreviewLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                SettingsContent(
                    isDarkTheme = false,
                    currentLanguage = GameLocales.English,
                    isDropdownExpanded = false,
                    onThemeChange = {},
                    onLanguageDropdownExpandChange = {},
                    onLanguageChange = {},
                    onDropdownDismiss = {},
                    onSaveChangesClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsPreviewDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                SettingsContent(
                    isDarkTheme = true,
                    currentLanguage = GameLocales.English,
                    isDropdownExpanded = true,
                    onThemeChange = {},
                    onLanguageDropdownExpandChange = {},
                    onLanguageChange = {},
                    onDropdownDismiss = {},
                    onSaveChangesClick = {}
                )
            }
        }
    }
}