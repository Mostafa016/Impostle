package com.example.nsddemo.ui

import android.widget.Space
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MainMenuScreen(
    viewModel: TestViewModel,
    onNavigateToCreateGame: () -> Unit,
    onNavigateToJoinGame: () -> Unit,
) {
    if (viewModel.playerNameDialogVisibilityState.value) {
        Dialog(onDismissRequest = {}) {
            Column(
                Modifier.background(Color.White),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Choose Your Name",
                    Modifier.padding(4.dp),
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = viewModel.playerNameTextFieldState.value,
                    onValueChange = viewModel.onPlayerNameChange
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    OutlinedButton(onClick = viewModel.onCancelPlayerNameClick) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(16.dp))
                    OutlinedButton(onClick = viewModel.onSavePlayerNameClick) {
                        Text("Save")
                    }
                }
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier
                .align(Alignment.End)
                .clickable { viewModel.onPlayerNameClick() }) {
            Icon(imageVector = Icons.Filled.Person, contentDescription = "Player name icon")
            Spacer(Modifier.width(8.dp))
            Text(
                viewModel.currentPlayer.value?.name ?: "???", style = TextStyle(
                    color = Color.Black
                )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "IMPOSTLE",
                style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.SemiBold),
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = {
                if (viewModel.currentPlayer.value == null) {
                    viewModel.showPlayerNameDialog()
                } else {
                    onNavigateToCreateGame()
                    viewModel.onRegisterServiceClick()
                }
            }) {
                Text(
                    "Create Game",
                    style = TextStyle(fontSize = 24.sp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                if (viewModel.currentPlayer.value == null) {
                    viewModel.showPlayerNameDialog()
                } else {
                    onNavigateToJoinGame()
                }
            }) {
                Text(
                    "Join Game",
                    style = TextStyle(fontSize = 24.sp),
                )
            }
        }
    }
}