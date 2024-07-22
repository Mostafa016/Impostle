package com.example.nsddemo.presentation.screen.category_and_word

sealed interface CategoryAndWordEvent {
    object StartQuestions : CategoryAndWordEvent
    object ConfirmCategoryAndWord : CategoryAndWordEvent
}