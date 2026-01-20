package com.example.nsddemo.presentation.screen.lobby

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.model.Categories
import com.example.nsddemo.domain.util.PlayerCountLimits
import com.example.nsddemo.presentation.util.GameStateHandler
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LobbyViewModel(
    private val gameRepository: GameRepository, private val gameStateHandler: GameStateHandler
) : ViewModel() {
    val clientGameState = gameRepository.clientGameState
    val gameCode = gameRepository.gameData.value.gameCode!!
    val isHost = gameRepository.gameData.value.isHost!! // TODO: RACE CONDITION HERE (HOW?)

    private val _state = MutableStateFlow(
        LobbyState(
            players = listOf(gameRepository.gameData.value.currentPlayer!!),
        )
    )
    val state = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.GetPlayerInfo::class.simpleName!!,
                GameState.StartNewRound::class.simpleName!!,
                GameState.DisplayCategoryAndWord::class.simpleName!!,
                GameState.Transitioning::class.simpleName!!,
                GameState.StartGame::class.simpleName!!,
            )
        )
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Listening to players list changes")
            launch {
                listenToPlayersListChanges()
            }
            Log.d(TAG, "Listening to game state changes")
            gameStateHandler.handleGameStateChanges()
        }
    }

    fun onEvent(event: LobbyEvent) {
        when (event) {
            is LobbyEvent.ChooseCategoryButtonClick -> navigateToCategoriesScreen()
            is LobbyEvent.ChooseCategory -> chooseCategory(event.chosenCategory)
            is LobbyEvent.StartRound -> startRound()
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "LobbyViewModel onCleared()")
    }

    private suspend fun listenToPlayersListChanges() {
        gameRepository.gameData.collectLatest {
            val players = it.players
            _state.value = state.value.copy(
                players = players,
                chosenCategory = it.category,
                isStartRoundButtonEnabled = canStartRound(
                    players.size, it.category,
                )
            )
        }
    }

    private fun startRound() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isHost) {
                chooseRandomWord(gameRepository.gameData.value.category!!)
                gameStateHandler.lastStateHandlerListener.first { it }
            }
            _eventFlow.emit(UiEvent.NavigateTo(Routes.CategoryAndWord.route))
        }
    }

    private fun chooseRandomWord(chosenCategory: Categories) {
        val categoryOrdinal = chosenCategory.ordinal
        val wordResID = chosenCategory.wordResourceIds.random()
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                categoryOrdinal = categoryOrdinal, wordResID = wordResID
            )
        )
        gameRepository.updateGameState(
            GameState.DisplayCategoryAndWord(
                categoryOrdinal, wordResID
            )
        )
    }

    private fun chooseCategory(chosenCategory: Categories) {
        _state.value = state.value.copy(
            chosenCategory = chosenCategory,
            isStartRoundButtonEnabled = canStartRound(state.value.players.size, chosenCategory)
        )
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                categoryOrdinal = chosenCategory.ordinal
            )
        )
    }

    private fun navigateToCategoriesScreen() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.ChooseCategory.route))

        }
    }

    private fun canStartRound(numOfPlayers: Int, chosenCategory: Categories? = null): Boolean =
        (numOfPlayers >= PlayerCountLimits.MIN_PLAYERS) && (numOfPlayers <= PlayerCountLimits.MAX_PLAYERS) && (chosenCategory != null)

    companion object {
        @Suppress("UNCHECKED_CAST")
        class LobbyViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                LobbyViewModel(gameRepository, gameStateHandler) as T
        }
    }
}