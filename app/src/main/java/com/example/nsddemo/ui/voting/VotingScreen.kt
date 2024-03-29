package com.example.nsddemo.ui.voting

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.GameState
import com.example.nsddemo.Player
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.theme.englishTypography

@Composable
fun VotingScreen(
    gameViewModel: GameViewModel,
    onNavigateToVotingResultsScreen: () -> Unit
) {
    val gameState = gameViewModel.gameState.collectAsState()
    val currentGameState = gameState.value
    val playersState = gameViewModel.players.collectAsState()
    if (currentGameState is GameState.EndVote) {
        LaunchedEffect(Unit) {
            onNavigateToVotingResultsScreen()
        }
    }
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.vote_for_the_person_you_suspect),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            Modifier
                .heightIn(max = 400.dp)
                .verticalScroll(scrollState)
        ) {
            for ((i, player) in playersState.value.withIndex()) {
                PlayerVoteItem(
                    player = player,
                    gameViewModel.votedPlayer.value,
                    onVoteForPlayer = gameViewModel.onVoteForPlayer
                )
                if (i != playersState.value.lastIndex) Divider()
            }
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
            Text(
                stringResource(R.string.confirm_vote),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = player.name,
            style = englishTypography.headlineSmall,
            color = Color(player.color.toLong(radix = 16)),
            fontWeight = FontWeight.SemiBold,
        )
        if (votedPlayer != null && player == votedPlayer) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Player voted for",
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Box(Modifier.size(4.dp)) {}
        }
    }
}