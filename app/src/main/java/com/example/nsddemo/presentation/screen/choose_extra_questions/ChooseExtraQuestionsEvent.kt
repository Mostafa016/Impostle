package com.example.nsddemo.presentation.screen.choose_extra_questions

sealed interface ChooseExtraQuestionsEvent {
    object StartVote : ChooseExtraQuestionsEvent
    object AskExtraQuestions : ChooseExtraQuestionsEvent
}