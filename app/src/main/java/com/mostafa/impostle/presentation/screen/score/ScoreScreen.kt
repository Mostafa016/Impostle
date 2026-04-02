package com.mostafa.impostle.presentation.screen.score

import android.annotation.SuppressLint
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.model.NewPlayerColors
import com.mostafa.impostle.domain.model.Player
import com.mostafa.impostle.presentation.components.common.BrutalistButton
import com.mostafa.impostle.presentation.components.common.BrutalistSectionHeader
import com.mostafa.impostle.presentation.components.common.MarqueeBanner
import com.mostafa.impostle.presentation.components.modifier.brutalistCard
import com.mostafa.impostle.presentation.components.modifier.brutalistGridBackground
import com.mostafa.impostle.presentation.screen.score.components.ImposterHeaderCard
import com.mostafa.impostle.presentation.screen.score.components.ScoreRow
import com.mostafa.impostle.presentation.theme.AppTheme
import com.mostafa.impostle.presentation.theme.Dimens
import com.mostafa.impostle.presentation.util.NoFeedbackIndication
import com.mostafa.impostle.presentation.util.UiEvent
import com.mostafa.impostle.presentation.util.toComposeColor
import kotlinx.coroutines.flow.collectLatest

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ScoreScreen(
    viewModel: ScoreViewModel = hiltViewModel(),
    showSnackBar: (String) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackBar -> {
                    showSnackBar(context.getString(event.messageResId))
                }

                else -> {}
            }
        }
    }

    val sortedScores = viewModel.playerScores.toList().sortedByDescending { it.second }

    ScoreContent(
        imposter = viewModel.imposter,
        isUserImposter = viewModel.isImposter,
        scores = sortedScores,
        isHost = viewModel.isHost,
        currentPlayer = viewModel.currentPlayer,
        isEndGameEnabled = state.isEndGameButtonEnabled,
        isReplayEnabled = state.isReplayGameButtonEnabled,
        onEndGameClick = { viewModel.onEvent(ScoreEvent.EndGame) },
        onReplayClick = { viewModel.onEvent(ScoreEvent.ReplayGame) },
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun ScoreContent(
    imposter: Player,
    isUserImposter: Boolean,
    scores: List<Pair<Player, Int>>,
    isHost: Boolean,
    currentPlayer: Player,
    isEndGameEnabled: Boolean,
    isReplayEnabled: Boolean,
    onEndGameClick: () -> Unit,
    onReplayClick: () -> Unit,
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
            MarqueeBanner(
                text = stringResource(R.string.game_over_final_results_game_over_final_results),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
            )

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
            ) {
                // --- HERO: IMPOSTER REVEAL ---
                ImposterHeaderCard(
                    imposterName = imposter.name,
                    imposterColor = NewPlayerColors.fromHex(imposter.color).toComposeColor(),
                    isCurrentUserImposter = isUserImposter,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- SCORES LIST ---
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .brutalistCard(
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                borderColor = MaterialTheme.colorScheme.outline,
                                shadowOffset = Dimens.ShadowMedium,
                                borderWidth = Dimens.BorderThick,
                            ),
                ) {
                    // Header
                    BrutalistSectionHeader(
                        text = stringResource(R.string.final_ranking),
                        trailingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.sharp_leaderboard_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )

                    // List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(scores) { index, (player, score) ->
                            ScoreRow(
                                player = player,
                                score = score,
                                rank = index + 1,
                                isImposter = player.id == imposter.id,
                                isCurrentPlayer = player.id == currentPlayer.id,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- HOST ACTIONS OR CLIENT WAITING ---
                if (isHost) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BrutalistButton(
                            text = stringResource(R.string.play_again).uppercase(),
                            icon = ImageVector.vectorResource(R.drawable.sharp_refresh_24),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            enabled = isReplayEnabled,
                            onClick = onReplayClick,
                        )

                        BrutalistButton(
                            text = stringResource(R.string.end_game).uppercase(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            enabled = isEndGameEnabled,
                            onClick = onEndGameClick,
                        )
                    }
                } else {
                    BrutalistButton(
                        text = stringResource(R.string.waiting_for_host),
                        enabled = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "Score Screen (Light)", showBackground = true, heightDp = 900, locale = "en")
@Preview(name = "Score Screen (Light) (Arabic)", showBackground = true, heightDp = 900, locale = "ar")
@Composable
private fun PreviewScoreLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ScoreContent(
                    imposter =
                        Player(
                            id = "p1",
                            name = "RoboPlayer",
                            color = NewPlayerColors.Orange.hexCode,
                        ),
                    isUserImposter = false,
                    scores =
                        listOf(
                            Player(
                                id = "p1",
                                name = "RoboPlayer",
                                color = NewPlayerColors.Orange.hexCode,
                            ) to 1500,
                            Player(
                                id = "p3",
                                name = "You",
                                color = NewPlayerColors.Blue.hexCode,
                            ) to 1200,
                            Player(
                                id = "p2",
                                name = "HappyGil",
                                color = NewPlayerColors.Purple.hexCode,
                            ) to 950,
                            Player(
                                id = "p4",
                                name = "DogLover99",
                                color = NewPlayerColors.DarkGreen.hexCode,
                            ) to 800,
                        ),
                    isHost = true,
                    currentPlayer = Player("p3", "You", NewPlayerColors.Blue.hexCode),
                    isEndGameEnabled = true,
                    isReplayEnabled = true,
                    onEndGameClick = {},
                    onReplayClick = {},
                )
            }
        }
    }
}

// ============================================================================
// 4. PREVIEWS
// ============================================================================

@Preview(name = "Score Screen (Light 2)", showBackground = true, heightDp = 900, locale = "en")
@Preview(name = "Score Screen (Light 2) (Arabic)", showBackground = true, heightDp = 900, locale = "ar")
@Composable
private fun PreviewScoreLight2() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ScoreContent(
                    imposter =
                        Player(
                            id = "p3",
                            name = "You",
                            color = NewPlayerColors.Blue.hexCode,
                        ),
                    isUserImposter = true,
                    scores =
                        listOf(
                            Player(
                                id = "p1",
                                name = "RoboPlayer",
                                color = NewPlayerColors.Orange.hexCode,
                            ) to 1500,
                            Player(
                                id = "p3",
                                name = "You",
                                color = NewPlayerColors.Blue.hexCode,
                            ) to 1200,
                            Player(
                                id = "p2",
                                name = "HappyGil",
                                color = NewPlayerColors.Purple.hexCode,
                            ) to 950,
                            Player(
                                id = "p4",
                                name = "DogLover99",
                                color = NewPlayerColors.DarkGreen.hexCode,
                            ) to 800,
                        ),
                    isHost = true,
                    currentPlayer = Player("p3", "You", NewPlayerColors.Blue.hexCode),
                    isEndGameEnabled = true,
                    isReplayEnabled = true,
                    onEndGameClick = {},
                    onReplayClick = {},
                )
            }
        }
    }
}

@Preview(name = "Score Screen (Dark)", showBackground = true, heightDp = 900, locale = "en")
@Preview(name = "Score Screen (Dark) (Arabic)", showBackground = true, heightDp = 900, locale = "ar")
@Composable
private fun PreviewScoreDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ScoreContent(
                    imposter =
                        Player(
                            id = "p1",
                            name = "RoboPlayer",
                            color = NewPlayerColors.Lime.hexCode,
                        ),
                    isUserImposter = false,
                    scores =
                        listOf(
                            Player(
                                id = "p1",
                                name = "RoboPlayer",
                                color = NewPlayerColors.Lime.hexCode,
                            ) to 1500,
                            Player(
                                id = "p3",
                                name = "You",
                                color = NewPlayerColors.Blue.hexCode,
                            ) to 1200,
                            Player(
                                id = "p2",
                                name = "HappyGil",
                                color = NewPlayerColors.Purple.hexCode,
                            ) to 950,
                            Player(
                                id = "p4",
                                name = "DogLover99",
                                color = NewPlayerColors.DarkGreen.hexCode,
                            ) to 800,
                        ),
                    isHost = false,
                    currentPlayer = Player("p3", "You", NewPlayerColors.Blue.hexCode),
                    isEndGameEnabled = true,
                    isReplayEnabled = true,
                    onEndGameClick = {},
                    onReplayClick = {},
                )
            }
        }
    }
}
