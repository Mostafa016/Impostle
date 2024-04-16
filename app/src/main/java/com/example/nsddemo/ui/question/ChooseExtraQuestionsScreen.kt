package com.example.nsddemo.ui.question

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nsddemo.GameState
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel

@Composable
fun ChooseExtraQuestionsScreen(
    gameViewModel: GameViewModel,
    onNavigateToVotingScreen: () -> Unit,
    onNavigateToQuestionScreen: () -> Unit
) {
    val gameState = gameViewModel.gameState.collectAsState()
    if (gameState.value is GameState.StartVote) {
        LaunchedEffect(Unit) {
            onNavigateToVotingScreen()
        }
    } else if (gameState.value is GameState.AskQuestion) {
        LaunchedEffect(Unit) {
            gameViewModel.resetUIStates()
            onNavigateToQuestionScreen()
        }
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.ask_extra_questions),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        if (gameViewModel.gameData.collectAsState().value.isHost!!) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = gameViewModel.onRestartQuestionsClick
                ) {
                    Text(
                        text = stringResource(R.string.additional_round),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = gameViewModel.onStartVoteClick
                ) {
                    Text(
                        text = stringResource(R.string.start_vote),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}