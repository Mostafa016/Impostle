package com.example.nsddemo.presentation.screen.role_reveal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.util.ConditionalComposable
import com.example.nsddemo.presentation.util.NavigationUtil.popBackStackAndNavigateTo
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.WordResourceMapper
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RoleRevealScreen(
    viewModel: RoleRevealViewModel = hiltViewModel<RoleRevealViewModel>(),
    navController: NavHostController
) {
    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is UiEvent.NavigateTo -> {
                    navController.popBackStackAndNavigateTo(event.destination)
                }

                else -> {
                    // Do nothing
                }
            }
        }
    }

    val uiState by viewModel.state.collectAsState()
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ConditionalComposable(condition = uiState.isConfirmPressed, composable = {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.waiting_for_all_players_to_confirm),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        }, fallbackComposable = {
            Text(
                stringResource(
                    R.string.category,
                    stringResource(viewModel.categoryNameResId)
                ),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (viewModel.isImposter) {
                    stringResource(R.string.you_are_the_imposter)
                } else {
                    stringResource(
                        R.string.word,
                        stringResource(WordResourceMapper.getResId(viewModel.word!!))
                    )
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        })
        Spacer(Modifier.height(16.dp))
        DefaultButton(
            stringResource(R.string.confirm),
            onClick = { viewModel.onEvent(RoleRevealEvent.ConfirmRoleReveal) },
            enabled = !uiState.isConfirmPressed
        )
    }
}