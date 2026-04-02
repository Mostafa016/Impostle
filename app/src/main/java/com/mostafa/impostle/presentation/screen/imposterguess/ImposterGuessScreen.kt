package com.mostafa.impostle.presentation.screen.imposterguess

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mostafa.impostle.R
import com.mostafa.impostle.domain.model.GameCategory
import com.mostafa.impostle.presentation.components.common.BrutalistButton
import com.mostafa.impostle.presentation.components.common.BrutalistDashedDivider
import com.mostafa.impostle.presentation.components.common.CornerBrackets
import com.mostafa.impostle.presentation.components.common.MarqueeBanner
import com.mostafa.impostle.presentation.components.common.WaitingMessage
import com.mostafa.impostle.presentation.components.modifier.brutalistCard
import com.mostafa.impostle.presentation.components.modifier.brutalistGridBackground
import com.mostafa.impostle.presentation.screen.imposterguess.components.ImposterGuessHeroCard
import com.mostafa.impostle.presentation.screen.imposterguess.components.ImposterGuessWordSelectionCard
import com.mostafa.impostle.presentation.theme.AppTheme
import com.mostafa.impostle.presentation.theme.Dimens
import com.mostafa.impostle.presentation.util.NoFeedbackIndication
import com.mostafa.impostle.presentation.util.uiCategory

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun ImposterGuessScreen(viewModel: ImposterGuessViewModel = hiltViewModel()) {
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
            onConfirmClick = { viewModel.onEvent(ImposterGuessEvent.ConfirmSelection) },
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
        modifier =
            Modifier
                .fillMaxSize()
                .brutalistGridBackground(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    gridLineColor = gridColor,
                ),
    ) {
        // Decoration behind
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding(),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .displayCutoutPadding(),
        ) {
            // --- TOP BANNER ---
            MarqueeBanner(
                text = stringResource(R.string.waiting_for_imposter_the_truth_is_near_do_not_leave_decision_phase),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(horizontal = Dimens.SpacingLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Reusing standard Hero card style
                ImposterGuessHeroCard()

                Spacer(Modifier.height(48.dp))

                WaitingMessage(text = stringResource(R.string.waiting_for_the_truth))
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
    onConfirmClick: () -> Unit,
) {
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

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
                text = stringResource(R.string.guess_the_secret_word_make_your_choice_guess_the_secret_word_make_your_choice),
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
                // --- TITLE CARD (Consistent with Screen 2) ---
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .brutalistCard(
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                borderColor = outlineColor,
                                shadowOffset = Dimens.ShadowMedium,
                                borderWidth = Dimens.BorderThin,
                            ).padding(Dimens.SpacingMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CategoryInfo(
                            category?.let { stringResource(it.uiCategory.nameResId) }?.uppercase()
                                ?: stringResource(
                                    R.string.unknown,
                                ),
                        )
                        BrutalistDashedDivider(
                            modifier =
                                Modifier
                                    .padding(horizontal = Dimens.SpacingSmall)
                                    .fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.guess_the_word),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                // --- WORD GRID (Consistent with Screen 2 Grid) ---
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Dimens.SpacingLarge),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                ) {
                    items(wordOptions, key = { it }) { wordOption ->
                        ImposterGuessWordSelectionCard(
                            word = wordOption,
                            isSelected = selectedWord == wordOption,
                            onClick = { onWordSelect(wordOption) },
                        )
                    }
                }

                // --- CONFIRM BUTTON ---
                BrutalistButton(
                    text = stringResource(R.string.confirm_guess),
                    icon = ImageVector.vectorResource(R.drawable.sharp_arrow_forward_24),
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
        text = stringResource(R.string.category),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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

@Preview(name = "Light Mode", showBackground = true, locale = "en")
@Preview(name = "Light Mode (Arabic)", showBackground = true, locale = "ar")
@Composable
private fun GuessPreviewLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ImposterGuessContent(
                    wordOptions =
                        listOf(
                            stringResource(R.string.cake),
                            stringResource(R.string.pizza),
                            stringResource(R.string.burger),
                            stringResource(R.string.cookie),
                            stringResource(R.string.beef),
                            stringResource(R.string.popcorn),
                        ),
                    selectedWord = stringResource(R.string.pizza),
                    category = GameCategory.FOOD,
                    onWordSelect = {},
                    onConfirmClick = {},
                )
            }
        }
    }
}

@Preview(
    name = "Wait Screen (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "en",
)
@Preview(
    name = "Wait Screen (Dark) (Arabic)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ar",
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
