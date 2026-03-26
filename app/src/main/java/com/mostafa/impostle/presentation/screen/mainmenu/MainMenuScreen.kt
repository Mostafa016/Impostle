package com.mostafa.impostle.presentation.screen.mainmenu

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.model.AppPermission
import com.mostafa.impostle.presentation.components.common.BrutalistButton
import com.mostafa.impostle.presentation.components.common.CornerBrackets
import com.mostafa.impostle.presentation.components.common.MarqueeBanner
import com.mostafa.impostle.presentation.components.common.WithPermission
import com.mostafa.impostle.presentation.components.modifier.brutalistGridBackground
import com.mostafa.impostle.presentation.screen.mainmenu.components.ChangePlayerNameDialog
import com.mostafa.impostle.presentation.screen.mainmenu.components.ChangePlayerNameDialogContent
import com.mostafa.impostle.presentation.screen.mainmenu.components.HeroTitleCard
import com.mostafa.impostle.presentation.screen.mainmenu.components.MainMenuHeader
import com.mostafa.impostle.presentation.theme.AppTheme
import com.mostafa.impostle.presentation.theme.Dimens
import com.mostafa.impostle.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.mostafa.impostle.presentation.util.NoFeedbackIndication
import com.mostafa.impostle.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

// ============================================================================
// 1. STATEFUL ROOT (ViewModel Integration)
// ============================================================================

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionStates by viewModel.permissionStates.collectAsStateWithLifecycle()

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

                else -> { // Handle other one-off events
                }
            }
        }
    }

    WithPermission(
        appPermission = AppPermission.POST_NOTIFICATIONS,
        rationaleText = stringResource(R.string.notification_permission_rationale),
        hasRequested = permissionStates[AppPermission.POST_NOTIFICATIONS] ?: false,
        markPermissionRequested = { viewModel.onEvent(MainMenuEvent.PermissionRequested(AppPermission.POST_NOTIFICATIONS)) },
    ) { requestNotificationPermission, isPermissionDialogShowing ->
        if (state.isPlayerNameDialogVisible && !isPermissionDialogShowing) {
            ChangePlayerNameDialog(mainMenuViewModel = viewModel)
        }

        MainMenuContent(
            playerName = state.playerName ?: stringResource(R.string.unset_player_name),
            isCreateGameEnabled = state.isCreateGameButtonEnabled,
            isJoinGameEnabled = state.isJoinGameButtonEnabled,
            onCreateGameClick = {
                requestNotificationPermission()
                viewModel.onEvent(MainMenuEvent.CreateGameClick)
            },
            onJoinGameClick = {
                requestNotificationPermission()
                viewModel.onEvent(MainMenuEvent.JoinGameClick)
            },
            onSettingsClick = { viewModel.onEvent(MainMenuEvent.SettingsClick) },
            onPlayerNameClick = { viewModel.onEvent(MainMenuEvent.PlayerNameClick) },
        )
    }
}

// ============================================================================
// 2. STATELESS UI (Layout & Rendering)
// ============================================================================

@Composable
fun MainMenuContent(
    playerName: String,
    isCreateGameEnabled: Boolean,
    isJoinGameEnabled: Boolean,
    onCreateGameClick: () -> Unit,
    onJoinGameClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlayerNameClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .brutalistGridBackground(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    gridLineColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
    ) {
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding(),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .displayCutoutPadding(),
        ) {
            MarqueeBanner(
                text = stringResource(R.string.imposter_trust_no_one_suspicion_is_key_who_do_you_trust),
            )

            // --- HEADER (Avatar & Settings) ---
            MainMenuHeader(
                playerName = playerName,
                onPlayerNameClick = onPlayerNameClick,
                onSettingsClick = onSettingsClick,
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HeroTitleCard()

                Spacer(modifier = Modifier.height(Dimens.SpacingLarge))

                BrutalistButton(
                    text = stringResource(R.string.create_game),
                    subtext = stringResource(R.string.host_a_local_lobby),
                    icon = Icons.Default.Add,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = isCreateGameEnabled,
                    onClick = onCreateGameClick,
                )

                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                BrutalistButton(
                    text = stringResource(R.string.join_game),
                    subtext = stringResource(R.string.enter_code_to_connect),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = isJoinGameEnabled,
                    onClick = onJoinGameClick,
                )
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO, locale = "en")
@Composable
private fun PreviewLight() {
    AppTheme {
        // Use your AppTheme wrapper!
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                MainMenuContent(
                    playerName = "ROBOPLAYER",
                    isCreateGameEnabled = true,
                    isJoinGameEnabled = true,
                    onCreateGameClick = {},
                    onJoinGameClick = {},
                    onSettingsClick = {},
                    onPlayerNameClick = {},
                )
            }
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "en")
@Preview(name = "Dark Mode (Arabic)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, locale = "ar", group = "Arabic")
@Composable
private fun PreviewDark() {
    AppTheme {
        // Use your AppTheme wrapper!
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                MainMenuContent(
                    playerName = "أحمد",
                    isCreateGameEnabled = true,
                    isJoinGameEnabled = true,
                    onCreateGameClick = {},
                    onJoinGameClick = {},
                    onSettingsClick = {},
                    onPlayerNameClick = {},
                )
            }
        }
    }
}

@Preview(name = "Dialog - Light Mode", showBackground = true, locale = "en")
@Composable
private fun PreviewNameDialogLight() {
    AppTheme(useDarkTheme = false) {
        Surface(color = Color.Transparent) {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                Box(Modifier.padding(16.dp)) {
                    ChangePlayerNameDialogContent(
                        playerNameTextFieldText = "RoboPlayer",
                        onNameChange = {},
                        onCancel = {},
                        onSave = {},
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Dialog - Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "en",
)
@Preview(
    name = "Dialog - Dark Mode (Arabic)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ar",
    group = "Arabic",
)
@Composable
private fun PreviewNameDialogDark() {
    AppTheme(useDarkTheme = true) {
        Surface(color = Color.Transparent) {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                Box(Modifier.padding(16.dp)) {
                    ChangePlayerNameDialogContent(
                        playerNameTextFieldText = "",
                        onNameChange = {},
                        onCancel = {},
                        onSave = {},
                    )
                }
            }
        }
    }
}
