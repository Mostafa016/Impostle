package com.example.nsddemo.presentation.screen.disconnected

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.common.MarqueeBanner
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.disconnected.components.DisconnectedHeroCard
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.Dimens
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DisconnectedScreen(
    viewModel: DisconnectedViewModel = hiltViewModel(),
    navController: NavHostController,
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> { // Do nothing
                }
            }
        }
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    DisconnectedContent(
        isReconnectEnabled = state.isReconnectButtonEnabled,
        isMainMenuEnabled = state.isGoToMainMenuButtonEnabled,
        onReconnectClick = { viewModel.onEvent(DisconnectedEvent.ReconnectButtonPressed) },
        onMainMenuClick = { viewModel.onEvent(DisconnectedEvent.GoToMainMenuButtonPressed) },
    )
}

@Composable
fun DisconnectedContent(
    isReconnectEnabled: Boolean,
    isMainMenuEnabled: Boolean,
    onReconnectClick: () -> Unit,
    onMainMenuClick: () -> Unit,
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
                text = "CONNECTION ERROR /// DISCONNECTED /// CONNECTION ERROR /// RETRYING /// ",
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
                Spacer(modifier = Modifier.weight(0.2f))

                // --- HERO CARD ---
                DisconnectedHeroCard()

                Spacer(modifier = Modifier.weight(1f))

                // --- ACTIONS ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMedium),
                ) {
                    // Rejoin Button (Primary)
                    BrutalistButton(
                        text = stringResource(R.string.rejoin),
                        icon = Icons.Default.Refresh,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        enabled = isReconnectEnabled,
                        onClick = onReconnectClick,
                    )

                    // Main Menu Button (Secondary)
                    BrutalistButton(
                        text = stringResource(R.string.go_to_main_menu),
                        icon = Icons.Default.ArrowBack,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        enabled = isMainMenuEnabled,
                        onClick = onMainMenuClick,
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun PreviewDisconnectedLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                DisconnectedContent(
                    isReconnectEnabled = true,
                    isMainMenuEnabled = true,
                    onReconnectClick = {},
                    onMainMenuClick = {},
                )
            }
        }
    }
}

@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDisconnectedDark() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                DisconnectedContent(
                    isReconnectEnabled = false,
                    isMainMenuEnabled = true,
                    onReconnectClick = {},
                    onMainMenuClick = {},
                )
            }
        }
    }
}
