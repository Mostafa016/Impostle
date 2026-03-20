package com.example.nsddemo.presentation.screen.votingresults

import android.content.res.Configuration
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.BrutalistSectionHeader
import com.example.nsddemo.presentation.components.common.ImposterRevealCard
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.votingresults.components.VoteResultRow
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.toComposeColor

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun VotingResultsScreen(viewModel: VotingResultsViewModel = hiltViewModel()) {
    // Convert Map<Player, Int> to a sorted List<Pair> for stable rendering
    val voteCounts = viewModel.roundVotingCounts.toList().sortedByDescending { it.second }

    VotingResultsContent(
        imposter = viewModel.imposter,
        isUserImposter = viewModel.isImposter,
        voteCounts = voteCounts,
        isHost = viewModel.isHost,
        onShowScores = { viewModel.onEvent(VotingResultsEvent.ShowScores) },
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun VotingResultsContent(
    imposter: Player,
    isUserImposter: Boolean,
    voteCounts: List<Pair<Player, Int>>,
    isHost: Boolean,
    onShowScores: () -> Unit,
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
                text = stringResource(R.string.the_reveal_the_truth_is_out_the_reveal_game_over),
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
                ImposterRevealCard(
                    imposterName = imposter.name,
                    imposterColor = NewPlayerColors.fromHex(imposter.color).toComposeColor(),
                    isCurrentUserImposter = isUserImposter,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- LIST: VOTE RESULTS ---
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
                    // List Header
                    BrutalistSectionHeader(
                        text = stringResource(R.string.votes_received),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        trailingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.sharp_how_to_vote_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        },
                    )

                    // Scrollable List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(voteCounts) { (player, count) ->
                            VoteResultRow(
                                player = player,
                                voteCount = count,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- HOST ACTION ---
                if (isHost) {
                    BrutalistButton(
                        text = stringResource(R.string.show_score),
                        icon = ImageVector.vectorResource(R.drawable.sharp_trophy_24),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onShowScores,
                    )
                } else {
                    // Client waiting state
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
// 4. PREVIEWS
// ============================================================================

@Preview(name = "Host View (Light)", showBackground = true, locale = "en")
@Preview(name = "Host View (Light) (Arabic)", showBackground = true, locale = "ar")
@Composable
private fun PreviewResultsHost() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                VotingResultsContent(
                    imposter =
                        Player(
                            id = "p1",
                            name = "RoboPlayer",
                            color = NewPlayerColors.Red.hexCode,
                        ),
                    isUserImposter = false,
                    voteCounts =
                        listOf(
                            Player(
                                id = "p1",
                                name = "RoboPlayer",
                                color = NewPlayerColors.Red.hexCode,
                            ) to 2,
                            Player(
                                id = "p2",
                                name = "HappyGil",
                                color = NewPlayerColors.Purple.hexCode,
                            ) to 1,
                            Player(
                                id = "p3",
                                name = "You",
                                color = NewPlayerColors.Blue.hexCode,
                            ) to 0,
                        ),
                    isHost = true,
                    onShowScores = {},
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
private fun PreviewResultsClient() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                VotingResultsContent(
                    imposter =
                        Player(
                            id = "p3",
                            name = "You",
                            color = NewPlayerColors.Blue.hexCode,
                        ),
                    isUserImposter = true, // "YOU WAS THE IMPOSTER"
                    voteCounts =
                        listOf(
                            Player(
                                id = "p3",
                                name = "You",
                                color = NewPlayerColors.Blue.hexCode,
                            ) to 4,
                            Player(
                                id = "p1",
                                name = "RoboPlayer",
                                color = NewPlayerColors.Red.hexCode,
                            ) to 0,
                        ),
                    isHost = false,
                    onShowScores = {},
                )
            }
        }
    }
}
