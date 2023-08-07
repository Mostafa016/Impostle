package com.example.nsddemo.ui.voting

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.GameState
import com.example.nsddemo.Player
import com.example.nsddemo.ui.GameViewModel

@Composable
fun VotingScreen(
    gameViewModel: GameViewModel,
    onNavigateToVotingResultsScreen: () -> Unit
) {
    val gameState = gameViewModel.gameState.collectAsState()
    val currentGameState = gameState.value
    if (currentGameState is GameState.EndVote) {
        LaunchedEffect(Unit) {
            Log.d(TAG, "Navigated to VotingResults screen.")
            onNavigateToVotingResultsScreen()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vote for the person you suspect", style = TextStyle(fontSize = 18.sp))
        Spacer(modifier = Modifier.height(16.dp))
        for (player in gameViewModel.players.value) {
            PlayerVoteItem(
                player = player,
                gameViewModel.votedPlayer.value,
                onVoteForPlayer = gameViewModel.onVoteForPlayer
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (gameViewModel.votedPlayer.value != null) {
                    gameViewModel.onConfirmVoteClick()
                } else {
                    Log.e(TAG, "votedPlayer is null.")
                }
            },
            enabled = !gameViewModel.isVoteConfirmed.collectAsState().value
        ) {
            Text("Confirm Vote")
        }
    }
}

@Composable
fun PlayerVoteItem(player: Player, votedPlayer: Player?, onVoteForPlayer: (Player) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onVoteForPlayer(player) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            player.name,
            style = TextStyle(color = Color(player.color.toLong(radix = 16)))
        )
        if (votedPlayer != null && player == votedPlayer) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Player voted for"
            )
        }
    }
}