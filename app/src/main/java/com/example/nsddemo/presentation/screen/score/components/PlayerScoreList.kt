package com.example.nsddemo.presentation.screen.score.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsddemo.domain.model.Player

@Composable
fun PlayerScoreList(playerScores: Map<Player, Int>, currentPlayer: Player) {
    playerScores.entries.forEachIndexed { index, (player, score) ->
        PlayerScoreItem(
            player = player, score = score, isCurrentPlayer = player == currentPlayer
        )
        if (index < playerScores.entries.size - 1) {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}