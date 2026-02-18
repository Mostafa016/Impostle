package com.example.nsddemo.presentation.screen.question

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.domain.engine.GameSession
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.domain.model.RoundData
import com.example.nsddemo.presentation.util.BaseGameViewModel
import com.example.nsddemo.presentation.util.UiEvent
import com.example.nsddemo.presentation.util.uiCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    gameSession: GameSession
) : BaseGameViewModel(gameSession) {

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // --- Static Data (Fetched Synchronously) ---
    // Safe because we only navigate here after the data is set in the previous phase
    val categoryNameResId: Int = gameData.value.category!!.uiCategory.nameResId
    val word: String? = gameData.value.word?.lowercase()
    val isImposter: Boolean = gameData.value.isImposter

    // --- Dynamic Data (Changes per turn) ---
    private val _uiState = MutableStateFlow(
        QuestionState(
            askingPlayer = Player("Loading...", "FF000000"),
            askedPlayer = Player("Loading...", "FF000000"),
            isCurrentPlayerAsking = false,
            isCurrentPlayerAsked = false
        )
    )
    val state = _uiState.asStateFlow()

    init {
        // Observe Round Changes (Next Question)
        viewModelScope.launch {
            gameData.collect { data ->
                val round = data.roundData
                if (round is RoundData.QuestionRoundData) {
                    val askerId = round.currentAskerId!!
                    val askedId = round.currentAskedId!!

                    val asker = data.players[askerId]!!
                    val asked = data.players[askedId]!!
                    val localId = data.localPlayerId

                    _uiState.value = _uiState.value.copy(
                        askingPlayer = asker,
                        askedPlayer = asked,
                        isCurrentPlayerAsking = data.isMyTurn,
                        isCurrentPlayerAsked = askedId == localId,
                        isDoneAskingQuestionClicked = false
                    )
                }
            }
        }
    }

    fun onEvent(event: QuestionEvent) {
        when (event) {
            QuestionEvent.ShowWordDialog -> {
                _uiState.value = _uiState.value.copy(isWordDialogVisible = true)
            }

            QuestionEvent.DismissWordDialog, QuestionEvent.ConfirmWordDialog -> {
                _uiState.value = _uiState.value.copy(isWordDialogVisible = false)
            }

            QuestionEvent.FinishAskingYourQuestion -> {
                _uiState.value = _uiState.value.copy(isDoneAskingQuestionClicked = true)
                viewModelScope.launch {
                    activeClient?.endTurn()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "QuestionViewModel: Cleared!")
    }
}