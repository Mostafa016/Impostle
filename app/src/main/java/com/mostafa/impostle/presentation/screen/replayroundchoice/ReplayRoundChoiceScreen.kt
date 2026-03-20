package com.mostafa.impostle.presentation.screen.replayroundchoice

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mostafa.impostle.R
import com.mostafa.impostle.presentation.components.common.CornerBrackets
import com.mostafa.impostle.presentation.components.common.MarqueeBanner
import com.mostafa.impostle.presentation.components.common.WaitingMessage
import com.mostafa.impostle.presentation.components.modifier.brutalistGridBackground
import com.mostafa.impostle.presentation.screen.replayroundchoice.components.HeroDecisionCard
import com.mostafa.impostle.presentation.screen.replayroundchoice.components.HostDecisionControls
import com.mostafa.impostle.presentation.theme.AppTheme
import com.mostafa.impostle.presentation.theme.Dimens
import com.mostafa.impostle.presentation.util.NoFeedbackIndication

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun ChooseExtraQuestionsScreen(viewModel: ReplayRoundChoiceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    ChooseExtraQuestionsContent(
        isHost = viewModel.isHost,
        isReplayEnabled = state.isReplayRoundButtonEnabled,
        isVoteEnabled = state.isStartVoteButtonEnabled,
        onReplayClick = { viewModel.onEvent(ReplayRoundChoiceEvent.ReplayRound) },
        onVoteClick = { viewModel.onEvent(ReplayRoundChoiceEvent.StartVote) },
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun ChooseExtraQuestionsContent(
    isHost: Boolean,
    isReplayEnabled: Boolean,
    isVoteEnabled: Boolean,
    onReplayClick: () -> Unit,
    onVoteClick: () -> Unit,
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
                    .systemBarsPadding(),
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = stringResource(R.string.time_is_up_round_complete_time_is_up_decision_phase),
                backgroundColor = MaterialTheme.colorScheme.primary, // Yellow
                contentColor = Color.Black,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.SpacingLarge)
                        .padding(
                            top = Dimens.SpacingXLarge,
                            bottom = Dimens.SpacingLarge,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // --- HERO CARD ---
                HeroDecisionCard()

                Spacer(modifier = Modifier.height(48.dp))

                // --- CONTROLS ---
                if (isHost) {
                    HostDecisionControls(
                        isReplayEnabled = isReplayEnabled,
                        isVoteEnabled = isVoteEnabled,
                        onReplayClick = onReplayClick,
                        onVoteClick = onVoteClick,
                    )
                } else {
                    WaitingMessage(text = stringResource(R.string.waiting_for_host_to_decide))
                }
            }
        }

        // --- DECORATION ---
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

// ============================================================================
// 4. PREVIEWS
// ============================================================================

@Preview(name = "Host View (Light)", showBackground = true, locale = "en")
@Preview(name = "Host View (Light) (Arabic)", showBackground = true, locale = "ar")
@Composable
private fun PreviewHostLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ChooseExtraQuestionsContent(
                    isHost = true,
                    isReplayEnabled = true,
                    isVoteEnabled = true,
                    onReplayClick = {},
                    onVoteClick = {},
                )
            }
        }
    }
}

@Preview(
    name = "Client View (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "en",
)
@Preview(
    name = "Client View (Dark) (Arabic)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ar",
)
@Composable
private fun PreviewClientDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ChooseExtraQuestionsContent(
                    isHost = false,
                    isReplayEnabled = false,
                    isVoteEnabled = false,
                    onReplayClick = {},
                    onVoteClick = {},
                )
            }
        }
    }
}
