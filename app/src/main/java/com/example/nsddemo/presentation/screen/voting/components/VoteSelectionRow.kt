package com.example.nsddemo.presentation.screen.voting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.toComposeColor

@Composable
fun VoteSelectionRow(
    player: Player,
    isSelected: Boolean,
    isLocked: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline

    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else outlineColor.copy(alpha = 0.2f)
    val borderWidth = if (isSelected) BrutalistDimens.BorderThin else 2.dp
    val backgroundColor = if (isSelected)
        MaterialTheme.colorScheme.background.compositeOver(MaterialTheme.colorScheme.primary)
    else
        MaterialTheme.colorScheme.surface
    val shadow = if (isSelected) BrutalistDimens.ShadowSmall else 0.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .brutalistCard(
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                shadowOffset = shadow,
                cornerRadius = BrutalistDimens.CornerMedium,
                borderWidth = borderWidth
            )
            .clickable(enabled = !isLocked, onClick = onSelect)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = NewPlayerColors.fromHex(player.color).toComposeColor(),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .border(2.dp, outlineColor, RoundedCornerShape(6.dp))
            )

            Text(
                text = player.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, outlineColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.sharp_target_24),
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
