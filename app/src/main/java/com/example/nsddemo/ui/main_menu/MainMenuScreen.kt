@file:Suppress("InfinitePropertiesLabel")

package com.example.nsddemo.ui.main_menu

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nsddemo.R
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.PlayerColors
import com.example.nsddemo.ui.theme.englishTypography

@Composable
fun MainMenuScreen(
    gameViewModel: GameViewModel,
    mainMenuViewModel: MainMenuViewModel,
    onNavigateToCreateGame: () -> Unit,
    onNavigateToJoinGame: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    if (mainMenuViewModel.playerNameDialogVisibilityState.value) {
        Dialog(onDismissRequest = {}) {
            Column(
                Modifier.background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.enter_your_name),
                    Modifier.padding(4.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = mainMenuViewModel.playerNameTextFieldState.value,
                    onValueChange = { mainMenuViewModel.onPlayerNameChange(it) },
                    textStyle = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        onClick = { mainMenuViewModel.onCancelPlayerNameClick() }) {
                        Text(
                            stringResource(R.string.cancel),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Divider(
                        Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .padding(vertical = 4.dp)
                    )
                    TextButton(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        enabled = mainMenuViewModel.playerNameTextFieldState.value.isNotBlank(),
                        onClick = { mainMenuViewModel.savePlayerName() }) {
                        Text(
                            stringResource(R.string.save),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
    val titleTransition = rememberInfiniteTransition(label = "title")
    val titleAnimationInitialColor = remember { PlayerColors.values().random() }
    val titleAnimationTargetColor =
        remember { PlayerColors.values().filter { it != titleAnimationInitialColor }.random() }
    val titleColor by titleTransition.animateColor(
        initialValue = Color(titleAnimationInitialColor.argb),
        targetValue = Color(titleAnimationTargetColor.argb),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, delayMillis = 1000),
            repeatMode = RepeatMode.Reverse
        )
    )
    Column(
        Modifier
            .fillMaxSize()
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(
                onClick = onNavigateToSettings
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.player_name_icon),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clickable { mainMenuViewModel.onPlayerNameClick() }
                    .padding(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.player_name_icon),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    gameViewModel.currentPlayer.value?.name
                        ?: stringResource(R.string.player_name_placeholder),
                    style = englishTypography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "IMPOSTLE",
                style = englishTypography.displayLarge,
                color = titleColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = {
                if (gameViewModel.currentPlayer.value == null) {
                    mainMenuViewModel.showPlayerNameDialog(
                        onPlayerNameSave = {
                            onNavigateToCreateGame()
                            gameViewModel.onRegisterServiceClick()
                        })
                } else {
                    onNavigateToCreateGame()
                    gameViewModel.onRegisterServiceClick()
                }
            }) {
                Text(
                    stringResource(R.string.create_game),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                if (gameViewModel.currentPlayer.value == null) {
                    mainMenuViewModel.showPlayerNameDialog(
                        onPlayerNameSave = onNavigateToJoinGame
                    )
                } else {
                    onNavigateToJoinGame()
                }
            }) {
                Text(
                    stringResource(R.string.join_game),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}