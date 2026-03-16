package com.example.nsddemo.presentation.screen.score.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.common.RankBadge
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.toComposeColor

@Composable
fun ScoreRow(
    player: Player,
    score: Int,
    rank: Int,
    isImposter: Boolean,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val isTopRank = rank == 1
    val backgroundColor =
        if (isTopRank) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = backgroundColor,
                    borderColor = outlineColor,
                    shadowOffset = Dimens.ShadowSmall,
                    cornerRadius = Dimens.CornerMedium,
                    borderWidth = 2.dp,
                ).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank)

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(
                        color = NewPlayerColors.fromHex(player.color).toComposeColor(),
                        shape = RoundedCornerShape(4.dp),
                    ).border(2.dp, outlineColor, RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentPlayer) "YOU" else player.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isImposter) {
                Text(
                    text = "IMPOSTER",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            text = "$score PTS",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
    }
}
