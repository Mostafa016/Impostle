package com.example.nsddemo.presentation.screen.imposter_guess

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.BrutalistDashedDivider
import com.example.nsddemo.presentation.components.common.CornerBrackets
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.common.WaitingMessage
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.imposter_guess.components.ImposterGuessHeroCard
import com.example.nsddemo.presentation.screen.imposter_guess.components.ImposterGuessWordSelectionCard
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun ImposterGuessScreen(
    viewModel: ImposterGuessViewModel = hiltViewModel()
) {
    val wordOptions by viewModel.wordOptions.collectAsStateWithLifecycle()
    val selectedWord by viewModel.selectedWord.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val isImposter by viewModel.isImposter.collectAsStateWithLifecycle()

    if (isImposter) {
        ImposterGuessContent(
            wordOptions = wordOptions,
            selectedWord = selectedWord,
            category = category,
            onWordSelect = { viewModel.onEvent(ImposterGuessEvent.WordChosen(it)) },
            onConfirmClick = { viewModel.onEvent(ImposterGuessEvent.ConfirmSelection) }
        )
    } else {
        WaitImposterGuessContent()
    }
}

// ============================================================================
// 2. STATELESS UI (WAITING STATE)
// ============================================================================

@Composable
fun WaitImposterGuessContent() {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.background,
                gridLineColor = gridColor
            )
    ) {
        // Decoration behind
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .displayCutoutPadding()
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = "WAITING FOR IMPOSTER /// THE TRUTH IS NEAR /// DO NOT LEAVE /// DECISION PHASE /// ",
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = BrutalistDimens.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Reusing standard Hero card style
                ImposterGuessHeroCard()

                Spacer(Modifier.height(48.dp))

                WaitingMessage(text = "WAITING FOR THE TRUTH...")
            }
        }
    }
}

// ============================================================================
// 3. STATELESS UI (ACTIVE GUESSING)
// ============================================================================

@Composable
fun ImposterGuessContent(
    wordOptions: List<String>,
    selectedWord: String?,
    category: GameCategory?,
    onWordSelect: (String) -> Unit,
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
                .displayCutoutPadding()
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = "GUESS THE SECRET WORD /// MAKE YOUR CHOICE /// GUESS THE SECRET WORD /// MAKE YOUR CHOICE /// ",
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
                // --- TITLE CARD (Consistent with Screen 2) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            borderColor = outlineColor,
                            shadowOffset = BrutalistDimens.ShadowMedium,
                            borderWidth = BrutalistDimens.BorderThin
                        )
                        .padding(BrutalistDimens.SpacingMedium),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CategoryInfo(category?.name?.uppercase() ?: "UNKNOWN")
                        BrutalistDashedDivider(
                            modifier = Modifier
                                .padding(horizontal = BrutalistDimens.SpacingSmall)
                                .fillMaxWidth()
                        )
                        Text(
                            text = "GUESS THE WORD",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(BrutalistDimens.SpacingMedium))

                // --- WORD GRID (Consistent with Screen 2 Grid) ---
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = BrutalistDimens.SpacingLarge),
                    horizontalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingMedium)
                ) {
                    items(wordOptions, key = { it }) { wordOption ->
                        ImposterGuessWordSelectionCard(
                            word = wordOption,
                            isSelected = selectedWord == wordOption,
                            onClick = { onWordSelect(wordOption) }
                        )
                    }
                }

                // --- CONFIRM BUTTON ---
                BrutalistButton(
                    text = "Confirm Guess",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onConfirmClick,
                    enabled = selectedWord != null,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun CategoryInfo(category: String) {
    Text(
        text = "CATEGORY",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Text(
        text = category.uppercase(),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
// ============================================================================
// 4. PREVIEWS
// ============================================================================

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun GuessPreviewLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ImposterGuessContent(
                    wordOptions = listOf("Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig"),
                    selectedWord = "Cherry",
                    category = GameCategory.FOOD,
                    onWordSelect = {},
                    onConfirmClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "Wait Screen (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun WaitPreview() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                WaitImposterGuessContent()
            }
        }
    }
}
