package com.example.nsddemo.presentation.screen.voting.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.theme.englishTypography

@Composable
fun PlayerVoteItem(
    modifier: Modifier = Modifier,
    player: Player,
    votedPlayer: Player?,
    onVoteForPlayer: (Player) -> Unit
) {
    Row(
        modifier.clickable { onVoteForPlayer(player) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = player.name,
            style = englishTypography.headlineSmall,
            color = Color(player.color.toLong(radix = 16)),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        if (votedPlayer != null && player == votedPlayer) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Player voted for",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
@Preview
fun PlayerVoteItemPreview() {
    PlayerVoteItem(modifier = Modifier.fillMaxWidth(),
        player = Player("Player 1", "FFFF0000"),
        votedPlayer = Player("Player 1", "FFFF0000"),
        onVoteForPlayer = {})
}