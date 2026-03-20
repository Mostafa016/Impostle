package com.mostafa.impostle.presentation.screen.question

import com.mostafa.impostle.domain.model.Player

data class QuestionState(
    val isWordDialogVisible: Boolean = false,
    val askingPlayer: Player,
    val askedPlayer: Player,
    val isCurrentPlayerAsking: Boolean,
    val isCurrentPlayerAsked: Boolean,
    val isDoneAskingQuestionClicked: Boolean = false,
)
