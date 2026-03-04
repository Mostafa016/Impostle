package com.example.nsddemo.presentation.components.common

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.theme.BrutalistDimens

@Composable
fun BrutalistPlayerRow(
    playerName: String,
    avatarColor: Color,
    isLocalPlayer: Boolean,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val borderColor =
        if (isLocalPlayer) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(
            alpha = 0.2f
        )
    // subtle background tint for local player
    val bgColor = if (isLocalPlayer)
        MaterialTheme.colorScheme.background.compositeOver(MaterialTheme.colorScheme.primary)
    else
        MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .brutalistCard(
                backgroundColor = bgColor,
                borderColor = borderColor,
                cornerRadius = BrutalistDimens.CornerMedium, // 8.dp
                shadowOffset = BrutalistDimens.ShadowSmall, // 2.dp
                borderWidth = BrutalistDimens.BorderThin // 2.dp
            )
            .padding(BrutalistDimens.SpacingSmall + 4.dp), // Approx 12dp
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingMedium)
        ) {
            // Avatar Square
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = avatarColor,
                        shape = RoundedCornerShape(BrutalistDimens.CornerSmall)
                    )
                    .border(
                        width = BrutalistDimens.BorderThin,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(BrutalistDimens.CornerSmall)
                    )
            )

            Text(
                text = if (isLocalPlayer) "YOU ($playerName)" else playerName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}