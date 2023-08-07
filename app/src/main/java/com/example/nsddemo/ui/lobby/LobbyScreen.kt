package com.example.nsddemo.ui.lobby

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.Player
import com.example.nsddemo.ui.GameViewModel

@Composable
fun LobbyScreen(gameViewModel: GameViewModel, onChooseCategoryClick: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Players",
            Modifier.fillMaxWidth(),
            style = TextStyle(fontSize = 32.sp),
            textAlign = TextAlign.Center
        )
        Column(
            Modifier
                .verticalScroll(scrollState)
                .width(150.dp)
        ) {
            for (player in gameViewModel.players.value) {
                PlayerRow(player)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Text("Game Code:", style = TextStyle(fontSize = 18.sp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                gameViewModel.gameCode,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            )
        }
        if (gameViewModel.isHost) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onChooseCategoryClick) {
                Text("Choose Category")
            }
        }
    }
}


@Composable
fun PlayerRow(player: Player) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.1F)),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = player.name,
            style = TextStyle(fontSize = 16.sp, color = Color(player.color.toLong(radix = 16))),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun PlayerRowPreview() {
    PlayerRow(player = Player("Player_1", "FFFF00FF"))
}