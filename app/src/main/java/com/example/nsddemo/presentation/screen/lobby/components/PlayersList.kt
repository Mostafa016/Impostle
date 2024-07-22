package com.example.nsddemo.presentation.screen.lobby.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nsddemo.domain.model.Player

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayersList(modifier: Modifier = Modifier, players: List<Player>) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(players) { index, player ->
            PlayerNameItem(modifier = Modifier.animateItemPlacement(), player)
            if (index != players.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

