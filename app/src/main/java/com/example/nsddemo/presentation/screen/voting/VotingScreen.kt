package com.example.nsddemo.presentation.screen.voting

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.BrutalistSectionHeader
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.voting.components.VoteSelectionRow
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun VotingScreen(
    viewModel: VotingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    VotingContent(
        players = viewModel.playersExcludingCurrent,
        votedPlayer = state.votedPlayer,
        isVoteConfirmed = state.isVoteConfirmed,
        onVoteForPlayer = { viewModel.onEvent(VotingEvent.VoteForPlayer(it)) },
        onConfirmVote = { viewModel.onEvent(VotingEvent.VoteConfirmed) }
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun VotingContent(
    players: List<Player>,
    votedPlayer: Player?,
    isVoteConfirmed: Boolean,
    onVoteForPlayer: (Player) -> Unit,
    onConfirmVote: () -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                gridLineColor = gridColor
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = "WHO IS THE IMPOSTER? /// WARNING /// TRUST NO ONE /// VOTE CAREFULLY /// ",
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = BrutalistDimens.SpacingLarge)
                    .padding(
                        top = BrutalistDimens.SpacingLarge,
                        bottom = BrutalistDimens.SpacingMedium
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- LIST CONTAINER ---
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            shadowOffset = BrutalistDimens.ShadowMedium,
                            borderWidth = BrutalistDimens.BorderThick
                        )
                ) {
                    // Internal Header
                    BrutalistSectionHeader(
                        text = "CAST YOUR VOTE",
                        trailingContent = {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.sharp_how_to_vote_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    )

                    // Scrollable List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(players) { player ->
                            VoteSelectionRow(
                                player = player,
                                isSelected = (player == votedPlayer),
                                isLocked = isVoteConfirmed,
                                onSelect = { onVoteForPlayer(player) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- BOTTOM ACTIONS ---
                if (isVoteConfirmed) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "VOTE CAST!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Black
                            )
                        }

                        BrutalistButton(
                            text = "WAITING FOR OTHERS...",
                            enabled = false,
                            onClick = {}
                        )
                    }
                } else {
                    BrutalistButton(
                        text = stringResource(R.string.confirm_vote),
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        enabled = votedPlayer != null,
                        onClick = onConfirmVote
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "Active Voting (Light)", showBackground = true)
@Composable
private fun PreviewVotingActive() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                VotingContent(
                    players = listOf(
                        Player(
                            id = "p1",
                            name = "RoboPlayer",
                            color = NewPlayerColors.DarkGreen.hexCode
                        ),
                        Player(
                            id = "p2",
                            name = "HappyGil",
                            color = NewPlayerColors.Purple.hexCode
                        ),
                        Player(
                            id = "p3",
                            name = "DogLover99",
                            color = NewPlayerColors.Orange.hexCode
                        ),
                        Player(id = "p4", name = "ExtraGuest", color = NewPlayerColors.Red.hexCode),
                    ),
                    votedPlayer = Player(
                        id = "p3",
                        name = "DogLover99",
                        color = NewPlayerColors.Orange.hexCode
                    ),
                    isVoteConfirmed = false,
                    onVoteForPlayer = {},
                    onConfirmVote = {}
                )
            }
        }
    }
}

@Preview(
    name = "Confirmed/Waiting (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewVotingConfirmed() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                VotingContent(
                    players = listOf(
                        Player(
                            id = "p1",
                            name = "RoboPlayer",
                            color = NewPlayerColors.DarkGreen.hexCode
                        ),
                        Player(
                            id = "p2",
                            name = "HappyGil",
                            color = NewPlayerColors.Purple.hexCode
                        ),
                    ),
                    votedPlayer = Player(
                        id = "p1",
                        name = "RoboPlayer",
                        color = NewPlayerColors.DarkGreen.hexCode,
                    ),
                    isVoteConfirmed = true,
                    onVoteForPlayer = {},
                    onConfirmVote = {}
                )
            }
        }
    }
}
