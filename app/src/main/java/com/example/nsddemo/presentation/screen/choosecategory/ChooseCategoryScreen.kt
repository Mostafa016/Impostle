package com.example.nsddemo.presentation.screen.choosecategory

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.choosecategory.components.CategorySelectionCard
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiCategory
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun ChooseCategoryScreen(
    viewModel: ChooseCategoryViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(
                        event.destination,
                        Routes.GameSessionGraph.route,
                    )
                }

                else -> {}
            }
        }
    }

    ChooseCategoryContent(
        selectedCategory = selectedCategory,
        onCategorySelect = { viewModel.onEvent(ChooseCategoryEvent.CategoryChosen(it.domainCategory)) },
        onConfirmClick = { viewModel.onEvent(ChooseCategoryEvent.ConfirmSelection) },
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun ChooseCategoryContent(
    selectedCategory: UiCategory?,
    onCategorySelect: (UiCategory) -> Unit,
    onConfirmClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .brutalistGridBackground(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    gridLineColor = MaterialTheme.colorScheme.surfaceVariant,
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
                text = "HOST CONTROLS /// SELECT CATEGORY /// CHOOSE WISELY /// HOST CONTROLS /// SELECT CATEGORY /// ",
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
                // --- TITLE CARD ---
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .brutalistCard(
                                backgroundColor = MaterialTheme.colorScheme.surface,
                                borderColor = MaterialTheme.colorScheme.outline,
                                shadowOffset = Dimens.ShadowMedium,
                                borderWidth = Dimens.BorderThin,
                            ).padding(Dimens.SpacingMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.choose_a_category).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.SpacingMedium))

                // --- CATEGORY GRID ---
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = Dimens.SpacingLarge),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                ) {
                    items(UiCategory.entries) { category ->
                        CategorySelectionCard(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = { onCategorySelect(category) },
                        )
                    }
                }

                // --- CONFIRM BUTTON ---
                BrutalistButton(
                    text = "Confirm",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onConfirmClick,
                    enabled = selectedCategory != null,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// ============================================================================
// 4. PREVIEWS
// ============================================================================

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun CategoryPreviewLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                ChooseCategoryContent(
                    selectedCategory = UiCategory.Food,
                    onCategorySelect = {},
                    onConfirmClick = {},
                )
            }
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryPreviewDark() {
    AppTheme(useDarkTheme = true) {
        CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
            Surface {
                ChooseCategoryContent(
                    selectedCategory = null,
                    onCategorySelect = {},
                    onConfirmClick = {},
                )
            }
        }
    }
}
