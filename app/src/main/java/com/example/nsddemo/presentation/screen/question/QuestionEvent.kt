package com.example.nsddemo.presentation.screen.question

sealed interface QuestionEvent {
    object ShowWordDialog : QuestionEvent
    object DismissWordDialog : QuestionEvent
    object ConfirmWordDialog : QuestionEvent
    object FinishAskingYourQuestion : QuestionEvent
}