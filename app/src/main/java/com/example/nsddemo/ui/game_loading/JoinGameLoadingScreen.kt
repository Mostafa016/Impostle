package com.example.nsddemo.ui.game_loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.join_game.JoinGameViewModel
import kotlinx.coroutines.delay

@Composable
fun JoinGameLoadingScreen(
    gameViewModel: GameViewModel,
    joinGameViewModel: JoinGameViewModel,
    onGameJoined: () -> Unit,
    onGameFound: () -> Unit,
    onGameNotFound: () -> Unit
) {
    val hasFoundGameState = joinGameViewModel.hasFoundGame.collectAsState()
    val hasJoinedGameState = gameViewModel.hasJoinedGame.collectAsState()
    if (hasFoundGameState.value) {
        LaunchedEffect(Unit) {
            onGameFound()
        }
    }
    if (hasJoinedGameState.value) {
        LaunchedEffect(Unit) {
            onGameJoined()
        }
    }
    LaunchedEffect(Unit) {
        delay(5000L)
        if (!hasFoundGameState.value) {
            joinGameViewModel.stopSearchingForGame()
            onGameNotFound()
        }
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.finding_game),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
    }

}