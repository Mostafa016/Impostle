package com.example.nsddemo.presentation.screen.votingresults.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.toComposeColor

@Composable
fun VoteResultRow(
    player: Player,
    voteCount: Int,
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val hasVotes = voteCount > 0

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = outlineColor.copy(alpha = 0.3f),
                    shadowOffset = Dimens.ShadowSmall,
                    cornerRadius = Dimens.CornerMedium,
                    borderWidth = 2.dp,
                ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            color = NewPlayerColors.fromHex(player.color).toComposeColor(),
                            shape = RoundedCornerShape(6.dp),
                        ).border(2.dp, outlineColor, RoundedCornerShape(6.dp)),
            )

            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .background(
                        color =
                            if (hasVotes) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.secondary.copy(
                                    alpha = 0.7f,
                                )
                            },
                        shape = RoundedCornerShape(8.dp),
                    ).border(2.dp, outlineColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = voteCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (hasVotes) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
