package com.example.nsddemo.presentation.screen.score

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.components.ListTitleText
import com.example.nsddemo.presentation.screen.score.components.PlayerScoreList
import com.example.nsddemo.presentation.theme.englishTypography
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ScoreScreen(
    viewModel: ScoreViewModel = hiltViewModel<ScoreViewModel>(), navController: NavHostController
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
    val state by viewModel.state.collectAsState()
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
                style = englishTypography.headlineLarge,
                color = Color(viewModel.imposter.color.toLong(radix = 16)),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(32.dp))
        ListTitleText(stringResource(R.string.scores))
        Spacer(modifier = Modifier.height(8.dp))
        PlayerScoreList(
            playerScores = viewModel.playerScores,
            currentPlayer = viewModel.currentPlayer,
        )
        if (viewModel.isHost) {
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                DefaultButton(
                    stringResource(R.string.end_game),
                    onClick = { viewModel.onEvent(ScoreEvent.EndGame) },
                    enabled = state.isEndGameButtonEnabled,
                )
                Spacer(Modifier.width(16.dp))
                DefaultButton(
                    stringResource(R.string.play_again),
                    onClick = { viewModel.onEvent(ScoreEvent.ReplayGame) },
                    enabled = state.isReplayGameButtonEnabled,
                )
            }
        }
    }
}