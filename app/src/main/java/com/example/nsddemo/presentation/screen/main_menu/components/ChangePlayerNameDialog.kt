package com.example.nsddemo.presentation.screen.main_menu.components

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.main_menu.MainMenuEvent
import com.example.nsddemo.presentation.screen.main_menu.MainMenuViewModel
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication

// ============================================================================
// 1. STATEFUL WRAPPER (Used in App)
// ============================================================================

@Composable
fun ChangePlayerNameDialog(
    mainMenuViewModel: MainMenuViewModel
) {
    val state by mainMenuViewModel.state.collectAsState()

    Dialog(onDismissRequest = { mainMenuViewModel.onEvent(MainMenuEvent.PlayerNameDialogCancel) }) {
        ChangePlayerNameDialogContent(
            playerNameTextFieldText = state.playerNameTextFieldText,
            onNameChange = { mainMenuViewModel.onEvent(MainMenuEvent.PlayerNameDialogTextChange(it)) },
            onCancel = { mainMenuViewModel.onEvent(MainMenuEvent.PlayerNameDialogCancel) },
            onSave = { mainMenuViewModel.onEvent(MainMenuEvent.PlayerNameDialogSave(it)) }
        )
    }
}

// ============================================================================
// 2. STATELESS CONTENT (Pure UI / Previewable)
// ============================================================================

@Composable
fun ChangePlayerNameDialogContent(
    playerNameTextFieldText: String,
    onNameChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .brutalistCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline,
                shadowOffset = BrutalistDimens.ShadowLarge,
                borderWidth = BrutalistDimens.BorderThick
            )
            .brutalistGridBackground(
                backgroundColor = MaterialTheme.colorScheme.surface,
                gridLineColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
    ) {
        Column(
            modifier = Modifier.padding(BrutalistDimens.SpacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.enter_your_name).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(BrutalistDimens.SpacingMedium))

            OutlinedTextField(
                value = playerNameTextFieldText,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true,
                placeholder = {
                    Text(
                        "Type name...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(BrutalistDimens.CornerMedium)
            )

            Spacer(modifier = Modifier.height(BrutalistDimens.SpacingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrutalistDimens.SpacingMedium)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BrutalistButton(
                        text = stringResource(R.string.cancel),
                        onClick = onCancel,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    BrutalistButton(
                        text = stringResource(R.string.save),
                        enabled = playerNameTextFieldText.isNotBlank(),
                        onClick = { onSave(playerNameTextFieldText) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. PREVIEWS
// ============================================================================

@Preview(name = "Dialog - Light Mode", showBackground = true)
@Composable
private fun PreviewNameDialogLight() {
    AppTheme(useDarkTheme = false) {
        Surface(color = Color.Transparent) {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                Box(Modifier.padding(16.dp)) {
                    ChangePlayerNameDialogContent(
                        playerNameTextFieldText = "RoboPlayer",
                        onNameChange = {},
                        onCancel = {},
                        onSave = {}
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Dialog - Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewNameDialogDark() {
    AppTheme(useDarkTheme = true) {
        Surface(color = Color.Transparent) {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                Box(Modifier.padding(16.dp)) {
                    ChangePlayerNameDialogContent(
                        playerNameTextFieldText = "",
                        onNameChange = {},
                        onCancel = {},
                        onSave = {}
                    )
                }
            }
        }
    }
}