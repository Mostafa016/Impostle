package com.example.nsddemo.presentation.screen.lobby.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsddemo.domain.model.Player

@Composable
fun PlayersList(
    modifier: Modifier = Modifier,
    players: List<Player>,
    onPlayerKick: (String) -> Unit,
    localPlayerId: String,
    isHost: Boolean,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(players) { index, player ->
            PlayerNameItem(
                modifier = Modifier.animateItem(),
                player,
                onPlayerKick,
                isLocalPlayer = player.id == localPlayerId,
                isHost = isHost
            )
            if (index != players.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

