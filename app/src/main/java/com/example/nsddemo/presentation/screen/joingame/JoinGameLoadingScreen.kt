package com.example.nsddemo.presentation.screen.joingame

import android.annotation.SuppressLint
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun JoinGameLoadingScreen(
    viewModel: JoinGameViewModel,
    navController: NavHostController,
    showSnackBar: (String) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                is UiEvent.ShowSnackBar -> {
                    showSnackBar(context.getString(event.messageResId))
                }

                else -> {}
            }
        }
    }

    JoinGameLoadingContent(
        onCancel = { navController.popBackStack() },
    )
}

@Composable
fun JoinGameLoadingContent(onCancel: () -> Unit) {
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
            // --- MARQUEE ---
            MarqueeBanner(
                text = stringResource(R.string.wait_scanning_network_lobby_wait_scanning_network),
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
                BrutalistStatusLabel(text = stringResource(R.string.finding_game))

                Spacer(modifier = Modifier.height(64.dp))

                BrutalistButton(
                    text = stringResource(R.string.cancel),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onCancel,
                )
            }
        }

        CornerBrackets(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

@Preview(name = "Join Loading Light", showBackground = true, locale = "en")
@Composable
private fun PreviewJoinLoadingLight() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                JoinGameLoadingContent(onCancel = {})
            }
        }
    }
}

@Preview(name = "Join Loading Dark", showBackground = true, locale = "en")
@Preview(name = "Join Loading Dark (Arabic)", showBackground = true, locale = "ar")
@Composable
private fun PreviewJoinLoadingDark() {
    AppTheme(useDarkTheme = true) {
        CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
            Surface {
                JoinGameLoadingContent(onCancel = {})
            }
        }
    }
}
