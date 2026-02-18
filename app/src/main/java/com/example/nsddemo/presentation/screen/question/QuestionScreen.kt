package com.example.nsddemo.presentation.screen.question

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.nsddemo.R
import com.example.nsddemo.presentation.components.DefaultButton
import com.example.nsddemo.presentation.screen.question.components.CategoryAndWordDialog
import com.example.nsddemo.presentation.screen.question.components.ShowWordButton
import com.example.nsddemo.presentation.theme.englishTypography
import com.example.nsddemo.presentation.util.WordResourceMapper


@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun QuestionScreen(
    viewModel: QuestionViewModel = hiltViewModel<QuestionViewModel>(),
) {
    val state = viewModel.state.collectAsState()
    if (state.value.isWordDialogVisible) {
        CategoryAndWordDialog(
            category = stringResource(viewModel.categoryNameResId),
            word = if (viewModel.isImposter) null else stringResource(
                WordResourceMapper.getResId(
                    viewModel.word!!
                )
            ),
            isImposter = viewModel.isImposter,
            onDismissRequest = { viewModel.onEvent(QuestionEvent.DismissWordDialog) },
            onOkClick = { viewModel.onEvent(QuestionEvent.ConfirmWordDialog) })
    }
    Column(Modifier.fillMaxSize()) {
        ShowWordButton(
            stringResource(R.string.show_word),
            modifier = Modifier.align(Alignment.Start),
            onClick = { viewModel.onEvent(QuestionEvent.ShowWordDialog) },
        )
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(
                            500, easing = EaseIn
                        )
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(
                            500, easing = EaseOut
                        )
                    )
                },
                label = "question_state_transition",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        if (state.value.isCurrentPlayerAsking) {
                            stringResource(R.string.you)
                        } else {
                            state.value.askingPlayer.name
                        },
                        style = englishTypography.headlineLarge,
                        // TODO: Unify sending colors as long values and converting them to
                        //  Color in the UI.
                        //  make playerColors the type of the color in Player data class.
                        color = Color(state.value.askingPlayer.color.toLong(radix = 16)),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (state.value.isCurrentPlayerAsking) {
                            stringResource(R.string.are_asking)
                        } else {
                            if (state.value.isCurrentPlayerAsked) {
                                stringResource(R.string.is_asking_you)
                            } else {
                                stringResource(R.string.is_asking)

                            }
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (state.value.isCurrentPlayerAsked) {
                            stringResource(R.string.you_asked)
                        } else {
                            state.value.askedPlayer.name
                        },
                        style = englishTypography.headlineLarge,
                        color = Color(state.value.askedPlayer.color.toLong(radix = 16)),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state.value.isCurrentPlayerAsking) {
                DefaultButton(
                    stringResource(R.string.done),
                    onClick = { viewModel.onEvent(QuestionEvent.FinishAskingYourQuestion) },
                    enabled = !(state.value.isDoneAskingQuestionClicked),
                )
            }
        }
    }
}