package com.example.nsddemo.presentation.screen.lobby

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.util.PlayerCountLimits
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.BrutalistPlayerRow
import com.example.nsddemo.presentation.components.common.BrutalistSectionHeader
import com.example.nsddemo.presentation.components.common.HeroRoomCodeCard
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.lobby.components.CategoryDisplay
import com.example.nsddemo.presentation.screen.lobby.components.LobbyClientStatus
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiCategory
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.toComposeColor
import com.example.nsddemo.presentation.util.uiCategory
import kotlinx.coroutines.flow.collectLatest

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun LobbyScreen(
    viewModel: LobbyViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(
                        event.destination,
                        popUpToRoute = Routes.GameSessionGraph.route
                    )
                }

                else -> { /* Do nothing */
                }
            }
        }
    }

    val state by viewModel.state.collectAsState()

    LobbyContent(
        players = state.players,
        gameCode = viewModel.gameCode.uppercase(),
        isHost = viewModel.isHost,
        chosenCategory = state.chosenCategory?.uiCategory,
        localPlayerId = viewModel.localPlayerId,
        isStartRoundButtonEnabled = state.isStartRoundButtonEnabled,
        onChooseCategoryClick = { viewModel.onEvent(LobbyEvent.ChooseCategoryButtonClick) },
        onStartRound = { viewModel.onEvent(LobbyEvent.StartRound) },
        onPlayerKick = { viewModel.onEvent(LobbyEvent.KickPlayer(it)) }
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun LobbyContent(
    players: List<Player>,
    gameCode: String,
    isHost: Boolean,
    chosenCategory: UiCategory?,
    localPlayerId: String,
    isStartRoundButtonEnabled: Boolean,
    onChooseCategoryClick: () -> Unit,
    onStartRound: () -> Unit,
    onPlayerKick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                gridLineColor = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
        ) {
            // 1. Dynamic Banner
            val bannerText = if (isHost) "HOST CONTROLS ///..." else "CONNECTED ///..."
            MarqueeBanner(text = bannerText)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = BrutalistDimens.SpacingLarge)
                    .padding(
                        top = BrutalistDimens.SpacingLarge,
                        bottom = BrutalistDimens.SpacingMedium
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingMedium)
            ) {
                // --- ROOM CODE HERO CARD ---
                HeroRoomCodeCard(gameCode)

                // --- PLAYER LIST CARD ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            shadowOffset = BrutalistDimens.ShadowMedium
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        BrutalistSectionHeader(
                            text = "PLAYERS JOINED",
                            trailingContent = {
                                Text(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    text = "${players.size}/${PlayerCountLimits.MAX_PLAYERS}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        )
                        val playersSorted = remember(players) {
                            if (players.isEmpty()) {
                                emptyList()
                            } else {
                                val localPlayer =
                                    (players.find { sortedList -> sortedList.id == localPlayerId }!!)
                                listOf(localPlayer) + (players - localPlayer)
                            }
                        }
                        // List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .animateContentSize(),
                            contentPadding = PaddingValues(BrutalistDimens.SpacingMedium),
                            verticalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingSmall)
                        ) {
                            items(playersSorted) { player ->
                                BrutalistPlayerRow(
                                    playerName = player.name,
                                    avatarColor = NewPlayerColors.fromHex(player.color)
                                        .toComposeColor(),
                                    isLocalPlayer = player.id == localPlayerId,
                                    trailingContent = {
                                        if (isHost && player.id != localPlayerId) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { onPlayerKick(player.id) }
                                                    .background(
                                                        MaterialTheme.colorScheme.errorContainer,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.error,
                                                        RoundedCornerShape(4.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Kick",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // --- CATEGORY DISPLAY (Visible to ALL) ---
                CategoryDisplay(
                    chosenCategory = chosenCategory,
                    isHost = isHost,
                    onClick = onChooseCategoryClick
                )

                // --- HOST ACTIONS OR CLIENT WAITING ---
                if (isHost) {
                    BrutalistButton(
                        text = stringResource(R.string.start_round).uppercase(),
                        icon = Icons.Default.PlayArrow,
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        enabled = isStartRoundButtonEnabled,
                        onClick = onStartRound
                    )
                } else {
                    LobbyClientStatus()
                }
            }
        }
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "Host View", showBackground = true)
@Composable
private fun LobbyPreviewHost() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                LobbyContent(
                    players = listOf(
                        Player(
                            id = "p1",
                            name = "Example User",
                            color = NewPlayerColors.Blue.hexCode
                        ),
                        Player(
                            id = "p2",
                            name = "Me",
                            color = NewPlayerColors.Red.hexCode
                        ),
                        Player(
                            id = "p3",
                            name = "RoboPlayer",
                            color = NewPlayerColors.DarkGreen.hexCode
                        )
                    ),
                    gameCode = "B7X2",
                    isHost = true,
                    chosenCategory = null,
                    localPlayerId = "p2",
                    isStartRoundButtonEnabled = true,
                    onChooseCategoryClick = {},
                    onStartRound = {},
                    onPlayerKick = {}
                )
            }
        }
    }
}

@Preview(name = "Client View", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LobbyPreviewClient() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                LobbyContent(
                    players = listOf(
                        Player(
                            id = "p1",
                            name = "Host Player",
                            color = NewPlayerColors.Blue.hexCode
                        ),
                        Player(
                            id = "p2",
                            name = "Me",
                            color = NewPlayerColors.Red.hexCode
                        ),
                        Player(
                            id = "p3",
                            name = "AnotherUser",
                            color = NewPlayerColors.DarkGreen.hexCode
                        )
                    ),
                    gameCode = "B7X2",
                    isHost = false,
                    chosenCategory = UiCategory.Animals,
                    localPlayerId = "p2",
                    isStartRoundButtonEnabled = false,
                    onChooseCategoryClick = {},
                    onStartRound = {},
                    onPlayerKick = {}
                )
            }
        }
    }
}


