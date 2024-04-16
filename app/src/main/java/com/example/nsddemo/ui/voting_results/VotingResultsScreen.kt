package com.example.nsddemo.ui.voting_results

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nsddemo.GameState
import com.example.nsddemo.Player
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.theme.englishTypography
import kotlinx.coroutines.flow.map

// Maybe Use PreviewParameters for all screens
@Composable
fun VotingResultsScreen(
    gameViewModel: GameViewModel,
    onShowScoreClick: () -> Unit,
    onNavigateToLobbyScreen: () -> Unit,
    onNavigateToJoinGameScreen: () -> Unit,
    onNavigateToEndGameScreen: () -> Unit
) {
    val currentGameState = gameViewModel.gameState.collectAsState().value
    val currentPlayerState = gameViewModel.gameRepository.gameData.map {
        it.currentPlayer
    }.collectAsState(null)
    if (currentGameState is GameState.Replay) {
        if (currentGameState.replay) {
            LaunchedEffect(Unit) {
                gameViewModel.replayGame()
                // TODO: Both host and clients should be navigated to lobby since the players list
                //  is already sent to players by then.
                //  This might break something
                onNavigateToLobbyScreen()
//                if (gameViewModel.gameData.value.isHost!!) {
//                    onNavigateToLobbyScreen()
//                } else {
//                    onNavigateToJoinGameScreen()
//                }
            }
        } else {
            LaunchedEffect(Unit) {
                onNavigateToEndGameScreen()
            }
        }
    }
    val imposterPlayer = remember { gameViewModel.gameRepository.gameData.value.imposter }
    val isImposter = remember { gameViewModel.gameRepository.gameData.value.isImposter!! }
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
                if (isImposter) stringResource(R.string.you) else imposterPlayer!!.name,
                style = MaterialTheme.typography.headlineLarge,
                color = Color(imposterPlayer!!.color.toLong(radix = 16)),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.votes),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        gameViewModel.gameData.collectAsState().value.roundVotingCounts.entries.forEachIndexed { index, (player, numberOfVotes) ->
            PlayerVoteResultItem(
                player = player,
                numberOfVotes = numberOfVotes,
                isCurrentPlayer = player == currentPlayerState.value
            )
            if (index < gameViewModel.gameData.collectAsState().value.roundVotingCounts.entries.size - 1) {
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onShowScoreClick) {
            Text(
                stringResource(R.string.show_score),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun PlayerVoteResultItem(player: Player, numberOfVotes: Int, isCurrentPlayer: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (isCurrentPlayer) stringResource(R.string.you) else player.name,
            style = englishTypography.headlineSmall,
            color = Color(player.color.toLong(radix = 16)),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            numberOfVotes.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}