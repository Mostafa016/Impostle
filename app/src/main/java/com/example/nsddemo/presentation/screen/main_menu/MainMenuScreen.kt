package com.example.nsddemo.presentation.screen.main_menu

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.CornerBrackets
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.main_menu.components.ChangePlayerNameDialog
import com.example.nsddemo.presentation.screen.main_menu.components.ChangePlayerNameDialogContent
import com.example.nsddemo.presentation.screen.main_menu.components.HeroTitleCard
import com.example.nsddemo.presentation.screen.main_menu.components.MainMenuHeader
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

// ============================================================================
// 1. STATEFUL ROOT (ViewModel Integration)
// ============================================================================

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsState()

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

    if (state.isPlayerNameDialogVisible) {
        ChangePlayerNameDialog(mainMenuViewModel = viewModel)
    }

    // Pass state down to the stateless UI
    MainMenuContent(
        playerName = state.playerName ?: "???",
        isCreateGameEnabled = state.isCreateGameButtonEnabled,
        isJoinGameEnabled = state.isJoinGameButtonEnabled,
        onCreateGameClick = { viewModel.onEvent(MainMenuEvent.CreateGameClick) },
        onJoinGameClick = { viewModel.onEvent(MainMenuEvent.JoinGameClick) },
        onSettingsClick = { viewModel.onEvent(MainMenuEvent.SettingsClick) },
        onPlayerNameClick = { viewModel.onEvent(MainMenuEvent.PlayerNameClick) }
    )
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
        modifier = Modifier
            .fillMaxSize()
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                gridLineColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
        ) {
            MarqueeBanner(
                text = "IMPOSTER /// TRUST NO ONE /// SUSPICION IS KEY /// WHO DO YOU TRUST? /// "
            )

            // --- HEADER (Avatar & Settings) ---
            MainMenuHeader(
                playerName = playerName,
                onPlayerNameClick = onPlayerNameClick,
                onSettingsClick = onSettingsClick
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BrutalistDimens.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HeroTitleCard()

                Spacer(modifier = Modifier.height(BrutalistDimens.SpacingLarge))

                BrutalistButton(
                    text = "Create Game",
                    subtext = "HOST A LOCAL LOBBY",
                    icon = Icons.Default.Add,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = isCreateGameEnabled,
                    onClick = onCreateGameClick
                )

                Spacer(modifier = Modifier.height(BrutalistDimens.SpacingMedium))

                BrutalistButton(
                    text = "Join Game",
                    subtext = "ENTER CODE TO CONNECT",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    enabled = isJoinGameEnabled,
                    onClick = onJoinGameClick
                )
            }

            Spacer(modifier = Modifier.weight(1.2f))
        }
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "Light Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLight() {
    AppTheme(useDarkTheme = false) { // Use your AppTheme wrapper!
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                MainMenuContent(
                    playerName = "ROBOPLAYER",
                    isCreateGameEnabled = true,
                    isJoinGameEnabled = true,
                    onCreateGameClick = {},
                    onJoinGameClick = {},
                    onSettingsClick = {},
                    onPlayerNameClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDark() {
    AppTheme(useDarkTheme = true) { // Use your AppTheme wrapper!
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                MainMenuContent(
                    playerName = "DARKMODE_USER",
                    isCreateGameEnabled = true,
                    isJoinGameEnabled = true,
                    onCreateGameClick = {},
                    onJoinGameClick = {},
                    onSettingsClick = {},
                    onPlayerNameClick = {}
                )
            }
        }
    }
}

@Preview(name = "Dialog - Light Mode", showBackground = true)
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
                        onSave = {}
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Dialog - Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
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
                        onSave = {}
                    )
                }
            }
        }
    }
}


