package com.example.nsddemo.presentation.screen.role_reveal

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.role_reveal.components.ConfirmedSynchronizationList
import com.example.nsddemo.presentation.screen.role_reveal.components.ImposterRoleCard
import com.example.nsddemo.presentation.screen.role_reveal.components.InnocentRoleCard
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.WordResourceMapper

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun RoleRevealScreen(
    viewModel: RoleRevealViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // Resolve dynamic text
    val categoryText = stringResource(viewModel.categoryNameResId)
    val wordText = viewModel.word?.let { stringResource(WordResourceMapper.getResId(it)) } ?: ""

    // Determine visual state
    val revealState = when {
        uiState.isConfirmPressed -> RoleRevealUIState.CONFIRMED
        viewModel.isImposter -> RoleRevealUIState.IMPOSTER
        else -> RoleRevealUIState.INNOCENT
    }

    RoleRevealContent(
        state = revealState,
        category = categoryText,
        secretWord = wordText,
        playersWithReadyState = uiState.playersWithReadyState,
        localPlayerId = viewModel.localPlayerId,
        onConfirmClick = { viewModel.onEvent(RoleRevealEvent.ConfirmRoleReveal) },
    )
}

enum class RoleRevealUIState {
    INNOCENT, IMPOSTER, CONFIRMED
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun RoleRevealContent(
    state: RoleRevealUIState,
    category: String,
    secretWord: String,
    playersWithReadyState: List<PlayerWithReadyState>,
    localPlayerId: String,
    onConfirmClick: () -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

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
                .systemBarsPadding()
        ) {
            // --- DYNAMIC TOP BANNER ---
            if (state == RoleRevealUIState.CONFIRMED) {
                MarqueeBanner(
                    text = "GAME IN PROGRESS /// WAIT FOR INSTRUCTIONS /// DO NOT REVEAL YOUR ROLE /// ",
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                MarqueeBanner(
                    text = "SECRET ROLE REVEALED /// DO NOT SHARE /// MEMORIZE YOUR WORD /// ",
                )
            }

            // --- MAIN CONTENT ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = BrutalistDimens.SpacingLarge)
                    .padding(
                        top = BrutalistDimens.SpacingLarge,
                        bottom = BrutalistDimens.SpacingMedium
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spacer to account for overlapping badge
                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    RoleRevealUIState.INNOCENT ->  // The Central Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .brutalistCard(
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    borderColor = outlineColor,
                                    shadowOffset = BrutalistDimens.ShadowLarge,
                                    borderWidth = BrutalistDimens.BorderThick
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            InnocentRoleCard(category, secretWord)
                        }

                    RoleRevealUIState.IMPOSTER -> // The Central Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .brutalistCard(
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    borderColor = outlineColor,
                                    shadowOffset = BrutalistDimens.ShadowLarge,
                                    borderWidth = BrutalistDimens.BorderThick
                                ),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            ImposterRoleCard(category)
                        }

                    RoleRevealUIState.CONFIRMED -> ConfirmedSynchronizationList(
                        players = playersWithReadyState,
                        localPlayerId = localPlayerId
                    )
                }
                Spacer(modifier = Modifier.weight(1f))

                // --- ACTION BUTTON ---
                if (state == RoleRevealUIState.CONFIRMED) {
                    BrutalistButton(
                        text = "WAITING FOR OTHERS...",
                        enabled = false,
                        onClick = {}
                    )
                } else {
                    BrutalistButton(
                        text = stringResource(R.string.confirm),
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = onConfirmClick
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "1. Innocent Role (Light)", showBackground = true)
@Composable
private fun PreviewInnocentLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                RoleRevealContent(
                    state = RoleRevealUIState.INNOCENT,
                    category = "Food",
                    secretWord = "Burger",
                    playersWithReadyState = emptyList(),
                    localPlayerId = "p1",
                    onConfirmClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "2. Imposter Role (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewImposterDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                RoleRevealContent(
                    state = RoleRevealUIState.IMPOSTER,
                    category = "Food",
                    secretWord = "", // Imposter doesn't see the word
                    playersWithReadyState = emptyList(),
                    localPlayerId = "p1",
                    onConfirmClick = {}
                )
            }
        }
    }
}

@Preview(name = "3. Confirmed Wait State (Light)", showBackground = true)
@Composable
private fun PreviewWaitLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                RoleRevealContent(
                    state = RoleRevealUIState.CONFIRMED,
                    category = "",
                    secretWord = "",
                    playersWithReadyState = listOf(
                        PlayerWithReadyState(
                            id = "p1",
                            name = "Example User",
                            color = NewPlayerColors.Blue.hexCode,
                            isReady = true
                        ),
                        PlayerWithReadyState(
                            id = "p2",
                            name = "This is me",
                            color = NewPlayerColors.Red.hexCode,
                            isReady = true
                        ),
                        PlayerWithReadyState(
                            id = "p3",
                            name = "AnotherUser",
                            color = NewPlayerColors.DarkGreen.hexCode,
                            isReady = false
                        )
                    ),
                    localPlayerId = "p2",
                    onConfirmClick = {}
                )
            }
        }
    }
}