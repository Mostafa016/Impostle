package com.example.nsddemo.presentation.screen.creategame

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.BrutalistLoaderVisual
import com.example.nsddemo.presentation.components.common.BrutalistStatusLabel
import com.example.nsddemo.presentation.components.common.CornerBrackets
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CreateGameLoadingScreen(
    viewModel: CreateGameViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> {}
            }
        }
    }

    // Handle side-effects for success/error
    if (state is GameCreationState.Success) {
        LaunchedEffect(Unit) { viewModel.onEvent(CreateGameEvent.GameCreated) }
    }
    if (state is GameCreationState.Error) {
        LaunchedEffect(Unit) { viewModel.onEvent(CreateGameEvent.GameCreationFailed) }
    }

    CreateGameLoadingContent(
        onCancel = { viewModel.onEvent(CreateGameEvent.CancelGameCreation) },
    )
}

@Composable
fun CreateGameLoadingContent(onCancel: () -> Unit) {
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
            // --- MARQUEE BANNER ---
            MarqueeBanner(
                text = stringResource(R.string.lobby_creating_session_stand_by_lobby_creating_session_stand_by),
                backgroundColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                // --- LOADING VISUAL ---
                BrutalistLoaderVisual()

                Spacer(modifier = Modifier.height(48.dp))

                // --- STATUS LABEL ---
                BrutalistStatusLabel(text = stringResource(R.string.creating_game))

                Spacer(modifier = Modifier.height(64.dp))

                // --- CANCEL BUTTON ---
                BrutalistButton(
                    text = stringResource(R.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onCancel,
                )
            }
        }

        // Decoration
        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

@Preview(name = "Create Game Loading", showBackground = true, locale = "en")
@Composable
private fun PreviewCreateLoadingLight() {
    AppTheme(useDarkTheme = false) {
        CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
            Surface {
                CreateGameLoadingContent(onCancel = {})
            }
        }
    }
}

@Preview(
    name = "Create Game Loading",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "en",
)
@Preview(
    name = "Create Game Loading (Arabic)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ar",
)
@Composable
private fun PreviewCreateLoadingDark() {
    AppTheme(useDarkTheme = true) {
        CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
            Surface {
                CreateGameLoadingContent(onCancel = {})
            }
        }
    }
}
