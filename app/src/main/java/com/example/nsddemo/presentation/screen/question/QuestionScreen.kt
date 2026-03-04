package com.example.nsddemo.presentation.screen.question

import android.content.res.Configuration
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nsddemo.R
import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.components.common.BrutalistButton
import com.example.nsddemo.presentation.components.modifier.brutalistBorderBottom
import com.example.nsddemo.presentation.components.modifier.brutalistCard
import com.example.nsddemo.presentation.components.modifier.brutalistGridBackground
import com.example.nsddemo.presentation.screen.question.components.CategoryAndWordDialog
import com.example.nsddemo.presentation.screen.question.components.PlayerExchangeCards
import com.example.nsddemo.presentation.theme.AppTheme
import com.example.nsddemo.presentation.theme.BrutalistDimens
import com.example.nsddemo.presentation.util.NoFeedbackIndication
import com.example.nsddemo.presentation.util.WordResourceMapper

// ============================================================================
// 1. STATEFUL ROOT
// ============================================================================

@Composable
fun QuestionScreen(
    viewModel: QuestionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Peek Dialog
    if (state.isWordDialogVisible) {
        CategoryAndWordDialog(
            category = stringResource(viewModel.categoryNameResId),
            word = if (viewModel.isImposter) null else stringResource(
                WordResourceMapper.getResId(
                    viewModel.word!!
                )
            ),
            isImposter = viewModel.isImposter,
            onDismissRequest = { viewModel.onEvent(QuestionEvent.DismissWordDialog) },
            onOkClick = { viewModel.onEvent(QuestionEvent.ConfirmWordDialog) }
        )
    }

    QuestionContent(
        isCurrentPlayerAsking = state.isCurrentPlayerAsking,
        isCurrentPlayerAsked = state.isCurrentPlayerAsked,
        isDoneAsking = state.isDoneAskingQuestionClicked,
        askingPlayer = state.askingPlayer,
        askedPlayer = state.askedPlayer,
        onShowWordClick = { viewModel.onEvent(QuestionEvent.ShowWordDialog) },
        onDoneAskingClick = { viewModel.onEvent(QuestionEvent.FinishAskingYourQuestion) }
    )
}

// ============================================================================
// 2. STATELESS UI
// ============================================================================

@Composable
fun QuestionContent(
    isCurrentPlayerAsking: Boolean,
    isCurrentPlayerAsked: Boolean,
    isDoneAsking: Boolean,
    askingPlayer: Player,
    askedPlayer: Player,
    onShowWordClick: () -> Unit,
    onDoneAskingClick: () -> Unit
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
            QuestionPhaseBanner()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = BrutalistDimens.SpacingLarge)
                    .padding(
                        top = BrutalistDimens.SpacingMedium,
                        bottom = BrutalistDimens.SpacingMedium
                    )
            ) {
                // --- PEEK WORD BUTTON ---
                Row(
                    modifier = Modifier
                        .brutalistCard(
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            borderColor = MaterialTheme.colorScheme.outline,
                            shadowOffset = BrutalistDimens.ShadowSmall,
                            cornerRadius = BrutalistDimens.CornerMedium,
                            borderWidth = BrutalistDimens.BorderThin
                        )
                        .clickable(onClick = onShowWordClick)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.sharp_visibility_24),
                        contentDescription = "Peek Word",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "PEEK WORD",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- PLAYER EXCHANGE LAYOUT ---
                PlayerExchangeCards(
                    isCurrentPlayerAsking = isCurrentPlayerAsking,
                    isCurrentPlayerAsked = isCurrentPlayerAsked,
                    askingPlayer = askingPlayer,
                    askedPlayer = askedPlayer
                )

                Spacer(modifier = Modifier.weight(1f))

                // --- ACTION BUTTON ---
                if (isCurrentPlayerAsking) {
                    BrutalistButton(
                        text = stringResource(R.string.done),
                        icon = Icons.Default.CheckCircle,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        enabled = !isDoneAsking,
                        onClick = onDoneAskingClick
                    )
                } else {
                    // Waiting State for Asked Player and Observers
                    BrutalistButton(
                        text = "WAITING...",
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }
    }
}

// ============================================================================
// 3. SUB-COMPONENTS
// ============================================================================

@Composable
private fun QuestionPhaseBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .brutalistBorderBottom(MaterialTheme.colorScheme.outline, 2.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            ImageVector.vectorResource(R.drawable.sharp_question_mark_24),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "QUESTION PHASE",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            letterSpacing = 4.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            ImageVector.vectorResource(R.drawable.sharp_question_mark_24),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ============================================================================
// 5. PREVIEWS
// ============================================================================

@Preview(name = "Asker View (Light)", showBackground = true)
@Composable
private fun PreviewAsker() {
    AppTheme(useDarkTheme = false) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                QuestionContent(
                    isCurrentPlayerAsking = true,
                    isCurrentPlayerAsked = false,
                    isDoneAsking = false,
                    askingPlayer = Player(
                        id = "p1",
                        name = "Alice",
                        color = NewPlayerColors.Blue.hexCode
                    ),
                    askedPlayer = Player(
                        id = "p2",
                        name = "Bob",
                        color = NewPlayerColors.Lime.hexCode
                    ),
                    onShowWordClick = {},
                    onDoneAskingClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "Asked View (Dark)",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewAsked() {
    AppTheme(useDarkTheme = true) {
        Surface {
            CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
                QuestionContent(
                    isCurrentPlayerAsking = false,
                    isCurrentPlayerAsked = true,
                    isDoneAsking = false,
                    askingPlayer = Player(
                        id = "p1",
                        name = "Alice",
                        color = NewPlayerColors.Blue.hexCode
                    ),
                    askedPlayer = Player(
                        id = "p2",
                        name = "Bob",
                        color = NewPlayerColors.Red.hexCode
                    ),
                    onShowWordClick = {},
                    onDoneAskingClick = {}
                )
            }

        }
    }
}

@Preview(name = "Dialog: Innocent", showBackground = true)
@Composable
private fun PreviewDialogInnocent() {
    AppTheme {
        CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
            CategoryAndWordDialog(
                category = "Food",
                word = "Burger",
                isImposter = false,
                onDismissRequest = {},
                onOkClick = {}
            )
        }
    }
}

@Preview(name = "Dialog: Imposter", showBackground = true)
@Composable
private fun PreviewDialogImposter() {
    AppTheme {
        CompositionLocalProvider(LocalIndication provides NoFeedbackIndication()) {
            CategoryAndWordDialog(
                category = "Movie Set",
                word = null,
                isImposter = true,
                onDismissRequest = {},
                onOkClick = {}
            )
        }
    }
}