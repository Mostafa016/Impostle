package com.example.nsddemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.GameState
import com.example.nsddemo.Player

@Composable
fun ScoreScreen(
    viewModel: TestViewModel,
    onPlayAgainPress: () -> Unit,
    onNavigateToLobbyScreen: () -> Unit,
    onNavigateToJoinGameScreen: () -> Unit
) {
    val currentGameState = viewModel.gameState.collectAsState().value
    if (currentGameState is GameState.Replay) {
        if (currentGameState.replay) {
            LaunchedEffect(Unit) {
                viewModel.replayGame()
                if(viewModel.isHost){
                    onNavigateToLobbyScreen()
                }
                else{
                    onNavigateToJoinGameScreen()
                }
            }
        } else {
            Text("Game Ended")
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Scores",
            style = TextStyle(fontSize = 24.sp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        viewModel.playerScores.entries.forEachIndexed { index, (player, score) ->
            PlayerScoreItem(player = player, score = score)
            if (index != viewModel.votedPlayers.entries.size - 1) {
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
        }
        if (viewModel.isHost) {
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(onClick = viewModel.onEndGameClick) {
                    Text("End Game")
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onPlayAgainPress) {
                    Text("Play Again")
                }
            }
        }
    }
}

@Composable
fun PlayerScoreItem(player: Player, score: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            player.name,
            style = TextStyle(color = Color(player.color.toLong(radix = 16)))
        )
        Text(
            score.toString(),
        )
    }
}