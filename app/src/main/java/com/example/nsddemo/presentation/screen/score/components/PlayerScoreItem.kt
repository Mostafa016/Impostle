package com.example.nsddemo.presentation.screen.score.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.theme.englishTypography

@Composable
fun PlayerScoreItem(player: Player, score: Int, isCurrentPlayer: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (isCurrentPlayer) stringResource(R.string.you) else player.name,
            style = englishTypography.headlineSmall,
            color = Color(player.color.toLong(radix = 16)),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            score.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
