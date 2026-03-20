package com.mostafa.impostle.presentation.screen.imposterguess

sealed class ImposterGuessEvent {
    data class WordChosen(
        val word: String,
    ) : ImposterGuessEvent()

    data object ConfirmSelection : ImposterGuessEvent()
}
