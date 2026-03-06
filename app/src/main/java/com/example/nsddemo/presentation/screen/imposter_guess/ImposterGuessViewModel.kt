package com.example.nsddemo.presentation.screen.imposter_guess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.domain.engine.GameSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImposterGuessViewModel @Inject constructor(
    private val gameSession: GameSession
) : ViewModel() {

    val wordOptions: StateFlow<List<String>> = gameSession.gameData
        .map { it.wordOptions }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val category = gameSession.gameData
        .map { it.category }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isImposter = gameSession.gameData
        .map { it.isImposter }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _selectedWord = MutableStateFlow<String?>(null)
    val selectedWord: StateFlow<String?> = _selectedWord.asStateFlow()

    fun onEvent(event: ImposterGuessEvent) {
        when (event) {
            is ImposterGuessEvent.WordChosen -> {
                _selectedWord.value = event.word
            }

            ImposterGuessEvent.ConfirmSelection -> {
                viewModelScope.launch {
                    val word = _selectedWord.value ?: return@launch
                    gameSession.activeClient?.submitImposterGuess(word)
                }
            }
        }
    }
}
