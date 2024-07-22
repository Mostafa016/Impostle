package com.example.nsddemo.presentation.screen.voting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.components.ListTitleText
import com.example.nsddemo.presentation.screen.voting.components.PlayerVoteList
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun VotingScreen(
    viewModel: VotingViewModel,
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }
    val state = viewModel.state.collectAsState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ListTitleText(
            stringResource(R.string.vote_for_the_person_you_suspect),
            style = TextStyle(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlayerVoteList(
            Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState()),
            players = viewModel.playersExcludingCurrent,
            votedPlayer = state.value.votedPlayer,
            onVoteForPlayer = { viewModel.onEvent(VotingEvent.onVotedForPlayer(it)) },
        )
        Spacer(modifier = Modifier.height(16.dp))
        DefaultButton(
            stringResource(R.string.confirm_vote),
            onClick = { viewModel.onEvent(VotingEvent.onVoteConfirmed) },
            enabled = state.value.hasChosenVote && (!state.value.isVoteConfirmed),
        )
    }
}

