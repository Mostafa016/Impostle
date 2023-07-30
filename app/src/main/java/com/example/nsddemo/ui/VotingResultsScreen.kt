package com.example.nsddemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.nsddemo.Player

// Maybe Use PreviewParameters for all screens
@Composable
fun VotingResultsScreen(
    viewModel: TestViewModel,
    onShowScoreClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Votes",
            style = TextStyle(fontSize = 24.sp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        viewModel.votedPlayers.entries.forEachIndexed { index, (player, numberOfVotes) ->
            PlayerVoteResultItem(player = player, numberOfVotes = numberOfVotes)
            if (index != viewModel.votedPlayers.entries.size - 1) {
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 8.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onShowScoreClick) {
            Text("Show Score")
        }
    }
}

@Composable
fun PlayerVoteResultItem(player: Player, numberOfVotes: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            player.name,
            style = TextStyle(color = Color(player.color.toLong(radix = 16)))
        )
        Text(
            numberOfVotes.toString(),
        )
    }
}