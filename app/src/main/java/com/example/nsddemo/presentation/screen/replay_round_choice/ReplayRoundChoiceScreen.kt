package com.example.nsddemo.presentation.screen.replay_round_choice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.DefaultButton

@Composable
fun ChooseExtraQuestionsScreen(
    viewModel: ReplayRoundChoiceViewModel = hiltViewModel<ReplayRoundChoiceViewModel>(),
) {
    val state by viewModel.state.collectAsState()
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.decide_on_playing_another_round_or_start_voting),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        if (viewModel.isHost) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DefaultButton(
                    stringResource(R.string.additional_round),
                    onClick = { viewModel.onEvent(ReplayRoundChoiceEvent.ReplayRound) },
                    style = TextStyle(textAlign = TextAlign.Center),
                    enabled = state.isReplayRoundButtonEnabled
                )
                Spacer(modifier = Modifier.height(16.dp))
                DefaultButton(
                    stringResource(R.string.start_vote),
                    onClick = { viewModel.onEvent(ReplayRoundChoiceEvent.StartVote) },
                    enabled = state.isStartVoteButtonEnabled
                )
            }
        }
    }
}