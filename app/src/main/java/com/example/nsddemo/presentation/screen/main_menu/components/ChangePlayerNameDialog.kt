package com.example.nsddemo.presentation.screen.main_menu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nsddemo.R
import com.example.nsddemo.presentation.screen.main_menu.MainMenuEvent
import com.example.nsddemo.presentation.screen.main_menu.MainMenuViewModel

@Composable
fun ChangePlayerNameDialog(
    mainMenuViewModel: MainMenuViewModel
) {
    val playerNameTextFieldText =
        mainMenuViewModel.state.collectAsState().value.playerNameTextFieldText
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
                value = playerNameTextFieldText,
                onValueChange = {
                    mainMenuViewModel.onEvent(
                        MainMenuEvent.PlayerNameDialogTextChange(
                            it
                        )
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                TextButton(modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                    onClick = { mainMenuViewModel.onEvent(MainMenuEvent.PlayerNameDialogCancel) }) {
                    Text(
                        stringResource(R.string.cancel), style = MaterialTheme.typography.titleLarge
                    )
                }
                HorizontalDivider(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(vertical = 4.dp)
                )
                TextButton(modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                    enabled = playerNameTextFieldText.isNotBlank(),
                    onClick = {
                        mainMenuViewModel.onEvent(
                            MainMenuEvent.PlayerNameDialogSave(
                                playerNameTextFieldText
                            )
                        )
                    }) {
                    Text(
                        stringResource(R.string.save), style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}