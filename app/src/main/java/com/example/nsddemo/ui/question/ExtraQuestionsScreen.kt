package com.example.nsddemo.ui.question

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nsddemo.GameState
import com.example.nsddemo.ui.GameViewModel

@Composable
fun ExtraQuestionsScreen(gameViewModel: GameViewModel, onNavigateToVotingScreen: () -> Unit) {
    val gameState = gameViewModel.gameState.collectAsState()
    if (gameState.value is GameState.StartVote) {
        LaunchedEffect(Unit) {
            onNavigateToVotingScreen()
        }
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Ask extra questions", style = TextStyle(fontSize = 24.sp)
        )
        if (gameViewModel.isHost) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = gameViewModel.onStartVoteClick) {
                Text(text = "Start Vote")
            }
        }
    }
}