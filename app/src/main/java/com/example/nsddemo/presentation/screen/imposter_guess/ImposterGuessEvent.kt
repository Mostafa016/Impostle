package com.example.nsddemo.presentation.screen.imposter_guess

sealed class ImposterGuessEvent {
    data class WordChosen(val word: String) : ImposterGuessEvent()
    data object ConfirmSelection : ImposterGuessEvent()
}
