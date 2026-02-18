package com.example.nsddemo.presentation.screen.lobby

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.screen.lobby.components.CategoryText
import com.example.nsddemo.presentation.screen.lobby.components.GameCodeText
import com.example.nsddemo.presentation.screen.lobby.components.PlayerListTitle
import com.example.nsddemo.presentation.screen.lobby.components.PlayersList
import com.example.nsddemo.presentation.util.ConditionalComposable
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiCategory
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.uiCategory
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel = hiltViewModel<LobbyViewModel>(),
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(
                        event.destination,
                        popUpToRoute = Routes.GameSessionGraph.route
                    )
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }
    val state = viewModel.state.collectAsState()
    LobbyScreen(
        players = state.value.players,
        gameCode = viewModel.gameCode.uppercase(),
        isHost = viewModel.isHost,
        chosenCategory = state.value.chosenCategory?.uiCategory,
        onChooseCategoryClick = { viewModel.onEvent(LobbyEvent.ChooseCategoryButtonClick) },
        onStartRound = { viewModel.onEvent(LobbyEvent.StartRound) },
        isStartRoundButtonEnabled = state.value.isStartRoundButtonEnabled,
        onPlayerKick = { viewModel.onEvent(LobbyEvent.KickPlayer(it)) },
        localPlayerId = viewModel.localPlayerId
    )
}

@Composable
private fun LobbyScreen(
    players: List<Player>,
    gameCode: String,
    isHost: Boolean,
    chosenCategory: UiCategory? = null,
    onChooseCategoryClick: () -> Unit,
    onStartRound: () -> Unit,
    isStartRoundButtonEnabled: Boolean,
    onPlayerKick: (String) -> Unit,
    localPlayerId: String
) {
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
            PlayerListTitle(
                stringResource(R.string.players),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                Modifier
                    .heightIn(max = 400.dp)
                    .width(150.dp)
            ) {
                ConditionalComposable(
                    condition = players.isNotEmpty(),
                    fallbackComposable = { CircularProgressIndicator() }) {
                    PlayersList(
                        modifier = Modifier.animateContentSize(),
                        players = players,
                        onPlayerKick = onPlayerKick,
                        localPlayerId = localPlayerId,
                        isHost = isHost,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            GameCodeText(text = gameCode)
            Spacer(modifier = Modifier.height(8.dp))
            CategoryText(chosenCategory?.nameResId?.let { stringResource(it) })
            ConditionalComposable(condition = isHost) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DefaultButton(
                        stringResource(R.string.choose_category),
                        onClick = onChooseCategoryClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DefaultButton(
                        stringResource(R.string.start_round),
                        onClick = {
                            chosenCategory?.let {
                                onStartRound()
                            }
                        },
                        enabled = isStartRoundButtonEnabled,
                    )
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
        players = listOf(
            Player("Player_1", "FFFF0000"),
            Player("Player_2", "FF0000FF"),
            Player("Player_3", "FF00FF00"),
            Player("Player_4", "FF00FFFF"),
            Player("Player_5", "FFFF00FF"),
            Player("Player_6", "FFFFFF00"),
            Player("Player_7", "FF000000"),
            Player("Player_8", "FF000000"),
            Player("Player_9", "FFFF0000"),
            Player("Player_10", "FF0000FF"),
            Player("Player_11", "FF00FF00"),
            Player("Player_12", "FF00FFFF"),
        ),
        gameCode = "abcd",
        isHost = true,
        onChooseCategoryClick = {},
        onStartRound = {},
        chosenCategory = null,
        isStartRoundButtonEnabled = false,
        onPlayerKick = {},
        localPlayerId = "Player_1"
    )
}


