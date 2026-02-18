package com.example.nsddemo.presentation.screen.main_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.presentation.screen.main_menu.components.AnimatedRandomColorText
import com.example.nsddemo.presentation.screen.main_menu.components.ChangePlayerNameButton
import com.example.nsddemo.presentation.screen.main_menu.components.ChangePlayerNameDialog
import com.example.nsddemo.presentation.screen.main_menu.components.MainMenuOptionButton
import com.example.nsddemo.presentation.theme.englishTypography
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel = hiltViewModel<MainMenuViewModel>(),
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    if (event.popPrevious) {
                        navController.popBackStackAndNavigateTo(route = event.destination)
                    } else {
                        navController.navigate(route = event.destination)
                    }
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    val state by viewModel.state.collectAsState()
    val playerName = state.playerName
    if (state.isPlayerNameDialogVisible) {
        ChangePlayerNameDialog(mainMenuViewModel = viewModel)
    }
    Column(
        Modifier.fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(
                onClick = { viewModel.onEvent(MainMenuEvent.SettingsClick) },
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.player_name_icon),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.weight(1f))
            ChangePlayerNameButton(
                playerName = playerName, onClick = {
                    viewModel.onEvent(MainMenuEvent.PlayerNameClick)
                })
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedRandomColorText(
                GameConstants.GAME_NAME,
                style = englishTypography.displayLarge.copy(fontWeight = FontWeight.Bold),
            )
            Spacer(modifier = Modifier.height(24.dp))
            MainMenuOptionButton(
                textResID = R.string.create_game,
                onClick = {
                    viewModel.onEvent(MainMenuEvent.CreateGameClick)
                },
                enabled = state.isCreateGameButtonEnabled
            )
            Spacer(modifier = Modifier.height(16.dp))
            MainMenuOptionButton(
                textResID = R.string.join_game,
                onClick = {
                    viewModel.onEvent(MainMenuEvent.JoinGameClick)
                },
                enabled = state.isJoinGameButtonEnabled
            )
        }
    }
}
