package com.example.nsddemo.presentation.screen.pause

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.screen.lobby.components.GameCodeText
import com.example.nsddemo.presentation.screen.lobby.components.PlayersList
import com.example.nsddemo.presentation.util.ConditionalComposable

@Composable
fun PauseScreen(
    viewModel: PausedViewModel = hiltViewModel<PausedViewModel>(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PauseScreen(
        players = state.disconnectedPlayers,
        gameCode = viewModel.gameCode.uppercase(),
        isHost = viewModel.isHost,
        onGameEnd = { viewModel.onEvent(PauseEvent.EndGame) },
        isEndGameButtonEnabled = state.isEndGameButtonEnabled,
        onPlayerKick = { viewModel.onEvent(PauseEvent.KickPlayer(it)) },
        localPlayerId = viewModel.localPlayerId
    )
}

@Composable
private fun PauseScreen(
    players: List<Player>,
    gameCode: String,
    isHost: Boolean,
    onGameEnd: () -> Unit,
    isEndGameButtonEnabled: Boolean,
    onPlayerKick: (String) -> Unit,
    localPlayerId: String
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.game_paused),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.disconnected_players),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                Modifier
                    .heightIn(max = 400.dp)
                    .width(150.dp)
            ) {
                ConditionalComposable(
                    condition = players.isNotEmpty(),
                    fallbackComposable = { CircularProgressIndicator() }) {
                    PlayersList(
                        modifier = Modifier.animateContentSize(),
                        players = players,
                        onPlayerKick = onPlayerKick,
                        localPlayerId = localPlayerId,
                        isHost = isHost,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            GameCodeText(text = gameCode)
            ConditionalComposable(condition = isHost) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DefaultButton(
                        stringResource(R.string.end_game),
                        onClick = {
                            onGameEnd()
                        },
                        enabled = isEndGameButtonEnabled,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, locale = "en")
@Composable
private fun PauseScreenPreview() {
    PauseScreen(
        players = listOf(
            Player("Player_1", "FFFF0000"),
            Player("Player_2", "FF0000FF"),
            Player("Player_3", "FF00FF00"),
            Player("Player_4", "FF00FFFF"),
            Player("Player_5", "FFFF00FF"),
            Player("Player_6", "FFFFFF00"),
            Player("Player_7", "FF000000"),
            Player("Player_8", "FF000000"),
            Player("Player_9", "FFFF0000"),
            Player("Player_10", "FF0000FF"),
            Player("Player_11", "FF00FF00"),
            Player("Player_12", "FF00FFFF"),
        ),
        gameCode = "abcd",
        isHost = true,
        onGameEnd = {},
        isEndGameButtonEnabled = false,
        onPlayerKick = {},
        localPlayerId = "Player_1"
    )
}