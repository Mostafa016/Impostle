package com.example.nsddemo.ui.game_loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsddemo.ui.GameViewModel

//TODO: Merge both screens into one (simple if statement)
// look if you can make hasJoinedGame in a local viewModel
@Composable
fun JoinGameLoadingScreen(gameViewModel: GameViewModel, onGameJoined: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (gameViewModel.hasJoinedGame.value) {
            LaunchedEffect(Unit) {
                onGameJoined()
            }
        }
        Text("Joined game. Waiting for host to start game...")
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator()
    }
}