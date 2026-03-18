package com.example.nsddemo.presentation.screen.pause

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.BrutalistSectionHeader
import com.example.nsddemo.presentation.components.common.HeroRoomCodeCard
import com.example.nsddemo.presentation.components.modifier.brutalistBorderBottom
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.pause.components.ClientWaitingStatus
import com.example.nsddemo.presentation.screen.pause.components.DisconnectedPlayerRow
import com.example.nsddemo.presentation.screen.pause.components.HostPauseControls
import com.example.nsddemo.presentation.screen.pause.components.PauseBanner
import com.example.nsddemo.presentation.screen.pause.components.PulsingText
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun PauseScreen(viewModel: PauseViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    PauseContent(
        players = state.disconnectedPlayers,
        gameCode = viewModel.gameCode.uppercase(),
        isHost = viewModel.isHost,
        isEndGameButtonEnabled = state.isEndGameButtonEnabled,
        onGameEnd = { viewModel.onEvent(PauseEvent.EndGame) },
        onPlayerKick = { viewModel.onEvent(PauseEvent.KickPlayer(it)) },
        onResume = { viewModel.onEvent(PauseEvent.ContinueGameAnyway) },
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun PauseContent(
    players: List<Player>,
    gameCode: String,
    isHost: Boolean,
    isEndGameButtonEnabled: Boolean,
    onGameEnd: () -> Unit,
    onPlayerKick: (String) -> Unit,
    onResume: () -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .brutalistGridBackground(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    gridLineColor = gridColor,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .displayCutoutPadding(),
        ) {
            // --- TOP BANNER ---
            PauseBanner()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = Dimens.SpacingLarge)
                        .padding(
                            top = Dimens.SpacingLarge,
                            bottom = Dimens.SpacingMedium,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // --- ROOM CODE CARD ---
                HeroRoomCodeCard(gameCode)

                // --- DISCONNECTED PLAYERS LIST ---
                if (players.isNotEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                    ) {
                        // List Container
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .brutalistCard(
                                        backgroundColor = MaterialTheme.colorScheme.surface,
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        shadowOffset = Dimens.ShadowMedium,
                                        borderWidth = Dimens.BorderThick,
                                    ),
                        ) {
                            // List Title
                            BrutalistSectionHeader(
                                text = stringResource(R.string.disconnected_players, players.size),
                                contentColor = MaterialTheme.colorScheme.error,
                                trailingContent = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.sharp_wifi_off_24),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                },
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(players) { player ->
                                    DisconnectedPlayerRow(
                                        player = player,
                                        isHost = isHost,
                                        onKick = { onPlayerKick(player.id) },
                                    )
                                }
                            }

                            // Footer Note
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background)
                                        .brutalistBorderBottom(
                                            MaterialTheme.colorScheme.outline,
                                            2.dp,
                                        ).padding(12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                PulsingText(
                                    text = stringResource(R.string.waiting_for_players_to_return),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // --- ACTIONS & CLIENT STATUS ---
                if (isHost) {
                    HostPauseControls(
                        isEndGameEnabled = isEndGameButtonEnabled,
                        onResume = onResume,
                        onGameEnd = onGameEnd,
                    )
                } else {
                    ClientWaitingStatus()
                    BrutalistButton(
                        text = stringResource(R.string.leave_game),
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = onGameEnd, // Client leaving acts as ending their game
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. PREVIEWS
// ============================================================================

@Preview(name = "Host View (Light)", showBackground = true, locale = "en")
@Preview(name = "Host View (Light) (Arabic)", showBackground = true, locale = "ar")
@Composable
private fun PreviewPauseHostLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                PauseContent(
                    players =
                        listOf(
                            Player(
                                id = "p2",
                                name = "HappyGil",
                                color = NewPlayerColors.Red.hexCode,
                            ),
                            Player(
                                id = "p3",
                                name = "ExtraGuest",
                                color = NewPlayerColors.Blue.hexCode,
                            ),
                        ),
                    gameCode = "B7X2",
                    isHost = true,
                    isEndGameButtonEnabled = true,
                    onGameEnd = {},
                    onPlayerKick = {},
                    onResume = {},
                )
            }
        }
    }
}

@Preview(
    name = "Client View (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "en",
)
@Preview(
    name = "Client View (Dark) (Arabic)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ar",
)
@Composable
private fun PreviewPauseClientDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                PauseContent(
                    players =
                        listOf(
                            Player(
                                id = "p2",
                                name = "HappyGil",
                                color = NewPlayerColors.Red.hexCode,
                            ),
                        ),
                    gameCode = "B7X2",
                    isHost = false,
                    isEndGameButtonEnabled = true,
                    onGameEnd = {},
                    onPlayerKick = {},
                    onResume = {},
                )
            }
        }
    }
}
