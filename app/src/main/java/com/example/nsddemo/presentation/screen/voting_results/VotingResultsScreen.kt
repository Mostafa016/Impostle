package com.example.nsddemo.presentation.screen.voting_results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.components.ListTitleText
import com.example.nsddemo.presentation.screen.voting_results.components.PlayerVoteResultList
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

// Maybe Use PreviewParameters for all screens
@Composable
fun VotingResultsScreen(
    viewModel: VotingResultsViewModel,
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
    val currentGameState = viewModel.gameState.collectAsState().value
    if (currentGameState is GameState.Replay) {
        if (currentGameState.replay) {
            LaunchedEffect(Unit) {
                viewModel.onEvent(VotingResultsEvent.ReplayGame)
            }
        } else {
            LaunchedEffect(Unit) {
                viewModel.onEvent(VotingResultsEvent.EndGame)
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Text(
                stringResource(R.string.imposter),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (viewModel.isImposter) stringResource(R.string.you) else viewModel.imposter.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color(viewModel.imposter.color.toLong(radix = 16)),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(32.dp))
        ListTitleText(stringResource(R.string.votes))
        Spacer(modifier = Modifier.height(8.dp))
        PlayerVoteResultList(
            roundVotingCounts = viewModel.roundVotingCounts,
            currentPlayer = viewModel.currentPlayer,
        )
        Spacer(Modifier.height(16.dp))
        DefaultButton(
            stringResource(R.string.show_score),
            onClick = { viewModel.onEvent(VotingResultsEvent.ShowScores) },
        )
    }
}

