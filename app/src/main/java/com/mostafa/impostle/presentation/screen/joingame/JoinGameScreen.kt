package com.mostafa.impostle.presentation.screen.joingame

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.mostafa.impostle.R
import com.mostafa.impostle.presentation.components.common.CornerBrackets
import com.mostafa.impostle.presentation.components.common.MarqueeBanner
import com.mostafa.impostle.presentation.components.modifier.brutalistGridBackground
import com.mostafa.impostle.presentation.screen.joingame.components.JoinGameActions
import com.mostafa.impostle.presentation.screen.joingame.components.JoinGameInputCard
import com.mostafa.impostle.presentation.theme.AppTheme
import com.mostafa.impostle.presentation.theme.Dimens
import com.mostafa.impostle.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.mostafa.impostle.presentation.util.NoFeedbackIndication
import com.mostafa.impostle.presentation.util.Routes
import com.mostafa.impostle.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun JoinGameScreen(
    viewModel: JoinGameViewModel,
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(
                        event.destination,
                        Routes.JoinGameGraph.route,
                    )
                }

                else -> {}
            }
        }
    }

    val state = viewModel.state.collectAsState().value

    JoinGameContent(
        codeText = state.gameCodeTextFieldText,
        isJoinEnabled = state.isJoinGameButtonEnabled,
        onCodeChange = { viewModel.onEvent(JoinGameEvent.GameCodeTextFieldValueChange(it)) },
        onJoinClick = { viewModel.onEvent(JoinGameEvent.JoinGame) },
        onCancelClick = { viewModel.onEvent(JoinGameEvent.GoBackToMainMenu) },
    )
}

@Composable
fun JoinGameContent(
    codeText: String,
    isJoinEnabled: Boolean,
    onCodeChange: (String) -> Unit,
    onJoinClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .brutalistGridBackground(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    gridLineColor = gridColor,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .displayCutoutPadding(),
        ) {
            // --- MARQUEE ---
            MarqueeBanner(
                text = stringResource(R.string.join_game_enter_code_connect_who_is_the_imposter),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = Dimens.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // --- INPUT CARD ---
                JoinGameInputCard(
                    codeText = codeText,
                    onCodeChange = onCodeChange,
                    onJoinClick = onJoinClick,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // --- ACTIONS ---
                JoinGameActions(
                    isJoinEnabled = isJoinEnabled,
                    onJoinClick = onJoinClick,
                    onCancelClick = onCancelClick,
                )
            }
        }

        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

@Preview(name = "Join Game Light", showBackground = true, locale = "en")
@Composable
private fun PreviewJoinGameLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                JoinGameContent(
                    codeText = "ABCD",
                    isJoinEnabled = true,
                    onCodeChange = {},
                    onJoinClick = {},
                    onCancelClick = {},
                )
            }
        }
    }
}

@Preview(name = "Join Game Dark", showBackground = true, locale = "en")
@Preview(name = "Join Game Dark (Arabic)", showBackground = true, locale = "ar")
@Composable
private fun PreviewJoinGameDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                JoinGameContent(
                    codeText = "ABC",
                    isJoinEnabled = false,
                    onCodeChange = {},
                    onJoinClick = {},
                    onCancelClick = {},
                )
            }
        }
    }
}
