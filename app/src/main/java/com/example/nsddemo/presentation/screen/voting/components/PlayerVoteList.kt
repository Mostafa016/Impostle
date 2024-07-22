package com.example.nsddemo.presentation.screen.voting.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nsddemo.domain.model.Player

@Composable
fun PlayerVoteList(
    modifier: Modifier = Modifier,
    players: List<Player>,
    votedPlayer: Player?,
    onVoteForPlayer: (Player) -> Unit
) {
    Column(
        modifier
    ) {
        for ((i, player) in players.withIndex()) {
            PlayerVoteItem(
                modifier = Modifier.fillMaxWidth(),
                player = player,
                votedPlayer = votedPlayer,
                onVoteForPlayer = onVoteForPlayer,
            )
            if (i != players.lastIndex) HorizontalDivider()
        }
    }
}