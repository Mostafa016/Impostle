package com.example.nsddemo.presentation.screen.joingame

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.presentation.components.common.CornerBrackets
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.joingame.components.JoinGameActions
import com.example.nsddemo.presentation.screen.joingame.components.JoinGameInputCard
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
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
                text = "JOIN GAME /// ENTER CODE /// CONNECT /// WHO IS THE IMPOSTER? /// ",
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

@Preview(name = "Join Game Light", showBackground = true)
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

@Preview(name = "Join Game Dark", showBackground = true)
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
