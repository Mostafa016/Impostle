package com.example.nsddemo.ui.lobby

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nsddemo.Categories
import com.example.nsddemo.Debugging
import com.example.nsddemo.GameState
import com.example.nsddemo.Player
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.theme.englishTypography

@Composable
fun LobbyScreen(
    gameViewModel: GameViewModel,
    onChooseCategoryClick: () -> Unit,
    onStartRound: () -> Unit
) {
    LobbyScreen(
        hasJoinedGame = gameViewModel.gameRepository.clientGameState.collectAsState().value is GameState.ClientGameStarted,
        players = gameViewModel.gameRepository.gameData.collectAsState().value.players,
        gameCode = gameViewModel.gameRepository.gameData.collectAsState().value.gameCode!!.uppercase(),
        isHost = gameViewModel.gameRepository.gameData.collectAsState().value.isHost!!,
        chosenCategory = gameViewModel.gameRepository.gameData.collectAsState().value.category,
        onChooseCategoryClick = onChooseCategoryClick,
        onStartRound = onStartRound,
        chooseWordRandomly = gameViewModel::chooseRandomWord
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LobbyScreen(
    hasJoinedGame: Boolean,
    players: List<Player>,
    gameCode: String,
    isHost: Boolean,
    chosenCategory: Categories? = null,
    onChooseCategoryClick: () -> Unit,
    onStartRound: () -> Unit,
    chooseWordRandomly: (Categories) -> Unit
) {
    if (hasJoinedGame) {
        LaunchedEffect(Unit) {
            Log.d(Debugging.TAG, "Joined game")
            onStartRound()
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.players),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                Modifier
                    .heightIn(max = 400.dp)
                    .width(150.dp)
            ) {
                LazyColumn(modifier = Modifier.animateContentSize()) {
                    itemsIndexed(players) { index, player ->
                        PlayerRow(modifier = Modifier.animateItemPlacement(), player)
                        if (index != players.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text(
                    stringResource(R.string.game_code),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    gameCode,
                    style = englishTypography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(
                    R.string.category, chosenCategory?.name ?: stringResource(
                        R.string.no_category_chosen
                    )
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (isHost) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = onChooseCategoryClick) {
                        Text(
                            stringResource(R.string.choose_category),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            chosenCategory?.let {
                                chooseWordRandomly(it)
                                onStartRound()
                            }
                        },
                        enabled = chosenCategory != null
                    ) {
                        Text(
                            stringResource(R.string.start_round),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, locale = "en")
@Composable
private fun LobbyScreenPreview() {
    LobbyScreen(
        hasJoinedGame = false,
        players = listOf(
            Player("Player_1", "FFFF0000"),
            Player("Player_2", "FF0000FF"),
            Player("Player_3", "FF00FF00"),
            Player("Player_4", "FF00FFFF"),
            Player("Player_5", "FFFF00FF"),
            Player("Player_6", "FFFFFF00"),
            Player("Player_7", "FF000000"),
            Player("Player_8", "FF000000"),
        ),
        gameCode = "ABCD",
        isHost = true,
        onChooseCategoryClick = {},
        onStartRound = {},
        chosenCategory = null,
        chooseWordRandomly = {},
    )
}


@Composable
private fun PlayerRow(modifier: Modifier, player: Player) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = player.name,
            style = englishTypography.headlineMedium,
            color = Color(player.color.toLong(radix = 16)),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
private fun PlayerRowPreview() {
    PlayerRow(modifier = Modifier, player = Player("Player_1", "FFFF00FF"))
}