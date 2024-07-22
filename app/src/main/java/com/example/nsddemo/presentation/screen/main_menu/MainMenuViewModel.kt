package com.example.nsddemo.presentation.screen.main_menu

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.core.util.Debugging.TAG
import com.example.nsddemo.core.util.GameConstants
import com.example.nsddemo.core.util.GameState
import com.example.nsddemo.data.repository.GameRepository
import com.example.nsddemo.domain.model.Player
import com.example.nsddemo.presentation.util.GameStateHandler
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainMenuViewModel(
    private val gameRepository: GameRepository,
    private val gameStateHandler: GameStateHandler,
) : ViewModel() {
    private val _state = MutableStateFlow(gameRepository.gameData.value.currentPlayer?.let {
        MainMenuState(
            playerNameTextFieldText = it.name, playerName = it.name
        )
    } ?: MainMenuState())
    val state: StateFlow<MainMenuState> = _state.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    // Used to determine the action to make after clicking on one of the menu options
    // (Create Game, Join Game)
    private var onPlayerNameSave: () -> Unit = {}

    init {
        gameRepository.setAllowedStates(
            setOf(
                GameState.StartGame::class.simpleName!!, GameState.Transitioning::class.simpleName!!
            )
        )
        gameRepository.playerName?.let {
            gameRepository.updateGameData(
                gameRepository.gameData.value.copy(
                    currentPlayer = Player(
                        name = it, color = GameConstants.DEFAULT_PLAYER_COLOR
                    )
                )
            )
            _state.value = state.value.copy(
                playerName = it, playerNameTextFieldText = it
            )
        }
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "MainMenuViewModel.onCleared() called")
    }


    fun onEvent(event: MainMenuEvent) {
        when (event) {
            MainMenuEvent.SettingsClick -> {
                onSettingsClick()
            }

            MainMenuEvent.PlayerNameClick -> {
                showPlayerNameDialogWithoutSaveAction()
            }

            is MainMenuEvent.PlayerNameDialogTextChange -> {
                onPlayerNameDialogTextChange(event.playerName)
            }

            is MainMenuEvent.PlayerNameDialogSave -> {
                onPlayerNameDialogSave()
            }

            MainMenuEvent.PlayerNameDialogCancel -> {
                onCancelPlayerNameDialog()
            }

            MainMenuEvent.CreateGameClick -> {
                onCreateGameClick()
            }

            MainMenuEvent.JoinGameClick -> {
                showPlayerNameDialogOrJoinGame()
            }

        }
    }

    private fun onSettingsClick() {
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.NavigateTo(Routes.Settings.route, isPopInclusive = false))
        }
    }

    private fun showPlayerNameDialogWithoutSaveAction() {
        _state.value = state.value.copy(
            isPlayerNameDialogVisible = true
        )
    }

    private fun showPlayerNameDialogWithSaveAction(onPlayerNameSave: () -> Unit) {
        _state.value = state.value.copy(
            isPlayerNameDialogVisible = true
        )
        this.onPlayerNameSave = onPlayerNameSave
    }

    private fun onPlayerNameDialogTextChange(playerName: String) {
        _state.value = state.value.copy(
            playerNameTextFieldText = playerName
        )
    }

    private fun onPlayerNameDialogSave() {
        val playerName = state.value.playerNameTextFieldText
        gameRepository.playerName = playerName
        _state.value = state.value.copy(
            playerName = playerName, isPlayerNameDialogVisible = false
        )
        onPlayerNameSave()
    }

    private fun onCancelPlayerNameDialog() {
        _state.value = state.value.copy(
            isPlayerNameDialogVisible = false
        )
    }

    private fun onCreateGameClick() {
        if (state.value.playerName == null) {
            showPlayerNameDialogWithSaveAction {
                createGame()
                // TODO: IDEA: Create a separate ViewModel scoped to the screens where
                //  the game is created and joined (The service should still be registered)
                //  and do like you did with JoinGameViewModel but with CreateGameViewModel.
                //  This will make GameViewModel only responsible for calling
                //  Client.run() and Server.run() and living as long as the Activity.
                //  IDEA A: NSD or should have it's own repository and be injected into the
                //  ViewModels that would use it.
                //  IDEA B: Create Server and Client repositories each will handle
                //  the following (for now): Registration/Discovery (NSD),
                //  communication with sockets (Ktor),
            }
        } else {
            createGame()
        }
    }

    private fun createGame() {
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                isHost = true, gameCode = generateGameCode()
            )
        )
        val gameData = gameRepository.gameData.value
        Log.d(TAG, "Game code: ${gameData.gameCode}, isHost: ${gameData.isHost}")
        viewModelScope.launch(Dispatchers.IO) {
            gameStateHandler.handleGameStateChanges()
        }
        viewModelScope.launch {
            // TODO: Find a better way to handle this if this appears to take a long time in testing
            gameStateHandler.lastStateHandlerListener.first { it }
            _eventFlow.emit(
                UiEvent.NavigateTo(
                    destination = Routes.GameSession.route,
                    dynamicStartRoute = Routes.CreateGame.route,
                    graphWithDynamicDestinationRoute = Routes.GameSession.route,
                )
            )
        }
    }

    private fun showPlayerNameDialogOrJoinGame() {
        if (state.value.playerName == null) {
            showPlayerNameDialogWithSaveAction { onJoinGame() }
        } else {
            onJoinGame()
        }
    }

    private fun onJoinGame() {
        gameRepository.updateGameData(
            gameRepository.gameData.value.copy(
                isHost = false
            )
        )
        viewModelScope.launch {
            _eventFlow.emit(
                UiEvent.NavigateTo(
                    destination = Routes.GameSession.route,
                    dynamicStartRoute = Routes.JoinGameSession.route,
                    graphWithDynamicDestinationRoute = Routes.GameSession.route
                )
            )
        }
    }

    private fun generateGameCode(): String {
        return (1..GameConstants.CODE_LENGTH).map { GameConstants.CODE_ALLOWED_CHARACTERS.random() }
            .joinToString("")
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class MainMenuViewModelFactory(
            private val gameRepository: GameRepository,
            private val gameStateHandler: GameStateHandler,
        ) : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainMenuViewModel(gameRepository, gameStateHandler) as T
        }
    }
}