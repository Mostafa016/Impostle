package com.example.nsddemo.presentation.screen.voting_results.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsddemo.domain.model.Player

@Composable
fun PlayerVoteResultList(roundVotingCounts: Map<Player, Int>, currentPlayer: Player) {
    roundVotingCounts.entries.forEachIndexed { index, (player, numberOfVotes) ->
        PlayerVoteResultItem(
            player = player,
            numberOfVotes = numberOfVotes,
            isCurrentPlayer = player == currentPlayer
        )
        if (index < roundVotingCounts.entries.size - 1) {
            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }

}