package com.example.nsddemo.presentation.screen.question

sealed interface QuestionEvent {
    data object ShowWordDialog : QuestionEvent
    data object DismissWordDialog : QuestionEvent
    data object ConfirmWordDialog : QuestionEvent
    data object FinishAskingYourQuestion : QuestionEvent
}