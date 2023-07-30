package com.example.nsddemo.ui

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.GameState

@Composable
fun ExtraQuestionsScreen(viewModel: TestViewModel, onNavigateToVotingScreen: () -> Unit) {
    val gameState = viewModel.gameState.collectAsState()
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
        if (viewModel.isHost) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                viewModel.onStartVoteClick()
            }) {
                Text(text = "Start Vote")
            }
        }
    }
}