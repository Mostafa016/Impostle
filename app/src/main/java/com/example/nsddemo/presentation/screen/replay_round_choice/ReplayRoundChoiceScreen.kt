package com.example.nsddemo.presentation.screen.replay_round_choice

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nsddemo.presentation.components.common.CornerBrackets
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.replay_round_choice.components.ClientWaitingMessage
import com.example.nsddemo.presentation.screen.replay_round_choice.components.HeroDecisionCard
import com.example.nsddemo.presentation.screen.replay_round_choice.components.HostDecisionControls
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun ChooseExtraQuestionsScreen(
    viewModel: ReplayRoundChoiceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ChooseExtraQuestionsContent(
        isHost = viewModel.isHost,
        isReplayEnabled = state.isReplayRoundButtonEnabled,
        isVoteEnabled = state.isStartVoteButtonEnabled,
        onReplayClick = { viewModel.onEvent(ReplayRoundChoiceEvent.ReplayRound) },
        onVoteClick = { viewModel.onEvent(ReplayRoundChoiceEvent.StartVote) }
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
    onVoteClick: () -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                gridLineColor = gridColor
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = "TIME IS UP! /// ROUND COMPLETE /// TIME IS UP! /// DECISION PHASE /// ",
                backgroundColor = MaterialTheme.colorScheme.primary, // Yellow
                contentColor = Color.Black
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = BrutalistDimens.SpacingLarge)
                    .padding(
                        top = BrutalistDimens.SpacingXLarge,
                        bottom = BrutalistDimens.SpacingLarge
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
                        onVoteClick = onVoteClick
                    )
                } else {
                    ClientWaitingMessage()
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

@Preview(name = "Host View (Light)", showBackground = true)
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
                    onVoteClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "Client View (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
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
                    onVoteClick = {}
                )
            }

        }
    }
}