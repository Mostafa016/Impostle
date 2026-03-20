package com.mostafa.impostle.presentation.screen.rolereveal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.model.NewPlayerColors
import com.mostafa.impostle.presentation.components.common.BrutalistPlayerRow
import com.mostafa.impostle.presentation.components.common.PlayerStatusBadge
import com.mostafa.impostle.presentation.components.modifier.brutalistCard
import com.mostafa.impostle.presentation.screen.rolereveal.PlayerWithReadyState
import com.mostafa.impostle.presentation.theme.Dimens
import com.mostafa.impostle.presentation.util.toComposeColor

@Composable
fun ConfirmedSynchronizationList(
    players: List<PlayerWithReadyState>,
    localPlayerId: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
    ) {
        ReadyStatusHero()

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .brutalistCard(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outline,
                        shadowOffset = Dimens.ShadowMedium,
                    ),
        ) {
            val readyPlayersSorted =
                remember(players) {
                    val playersSorted = players.sortedByDescending { it.isReady }
                    val localPlayer = playersSorted.find { it.id == localPlayerId }
                    if (localPlayer != null) {
                        listOf(localPlayer) + (playersSorted - localPlayer)
                    } else {
                        playersSorted
                    }
                }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(Dimens.SpacingMedium),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSmall),
            ) {
                items(readyPlayersSorted) { player ->
                    BrutalistPlayerRow(
                        playerName = player.name,
                        avatarColor = NewPlayerColors.fromHex(player.color).toComposeColor(),
                        isLocalPlayer = player.id == localPlayerId,
                        trailingContent = {
                            PlayerStatusBadge(player.isReady)
                        },
                    )
                }
            }
        }

        SynchronizationFooter(
            current = players.count { it.isReady },
            total = players.size,
        )
    }
}

@Composable
private fun ReadyStatusHero() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = Dimens.ShadowMedium,
                    borderWidth = Dimens.BorderThick,
                ).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(Color.Black, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.sharp_visibility_24),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .background(Color(0xFF22C55E), CircleShape)
                        .border(2.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Column {
            Text(
                text = stringResource(R.string.you_are),
                style = MaterialTheme.typography.labelLarge,
                color = Color.Gray,
            )
            Text(
                text = stringResource(R.string.ready),
                style =
                    MaterialTheme.typography.displayMedium.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 34.sp,
            )
        }
    }
}

@Composable
private fun SynchronizationFooter(
    current: Int,
    total: Int,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .brutalistCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    shadowOffset = Dimens.ShadowMedium,
                    borderWidth = Dimens.BorderThick,
                ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.waiting_for_others),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "($current/$total)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(Color(0xFFE5E7EB), CircleShape)
                    .border(2.dp, Color.Black, CircleShape)
                    .clip(CircleShape),
        ) {
            val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
