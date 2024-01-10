package com.example.nsddemo.ui.score

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

@Composable
fun ScoreScreen(
    viewModel: GameViewModel,
    onPlayAgainPress: () -> Unit,
    onNavigateToLobbyScreen: () -> Unit,
    onNavigateToJoinGameScreen: () -> Unit,
    onNavigateToEndGameScreen: () -> Unit
) {
    val currentGameState = viewModel.gameState.collectAsState().value
    if (currentGameState is GameState.Replay) {
        if (currentGameState.replay) {
            LaunchedEffect(Unit) {
                viewModel.replayGame()
                if (viewModel.isHost) {
                    onNavigateToLobbyScreen()
                } else {
                    onNavigateToJoinGameScreen()
                }
            }
        } else {
            LaunchedEffect(Unit) {
                onNavigateToEndGameScreen()
            }
        }
    }
    val imposterPlayer = remember { viewModel.imposterPlayer }
    val isImposter = remember { viewModel.isImposter!! }
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
                if (isImposter) stringResource(R.string.you) else imposterPlayer.name,
                style = englishTypography.headlineLarge,
                color = Color(imposterPlayer.color.toLong(radix = 16)),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(R.string.scores),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        viewModel.playerScores.entries.forEachIndexed { index, (player, score) ->
            PlayerScoreItem(
                player = player,
                score = score,
                isCurrentPlayer = player == viewModel.currentPlayer.value
            )
            if (index < viewModel.playerScores.entries.size - 1) {
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
                    Text(
                        stringResource(R.string.end_game),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onPlayAgainPress) {
                    Text(
                        stringResource(R.string.play_again),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerScoreItem(player: Player, score: Int, isCurrentPlayer: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (isCurrentPlayer) stringResource(R.string.you) else player.name,
            style = englishTypography.headlineSmall,
            color = Color(player.color.toLong(radix = 16)),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            score.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}