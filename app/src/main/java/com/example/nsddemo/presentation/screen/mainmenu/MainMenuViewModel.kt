package com.example.nsddemo.presentation.screen.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.di.IoDispatcher
import com.example.nsddemo.domain.repository.SettingsRepository
import com.example.nsddemo.presentation.util.Routes
import com.example.nsddemo.presentation.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainMenuViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _state = MutableStateFlow(MainMenuState())
        val state: StateFlow<MainMenuState> = _state.asStateFlow()

        private val _eventFlow = MutableSharedFlow<UiEvent>()
        val eventFlow = _eventFlow.asSharedFlow()

        // Callback to execute after player name is saved
        private var onPlayerNameSave: () -> Unit = {}

        init {
            loadPlayerName()
        }

        private fun loadPlayerName() {
            viewModelScope.launch(ioDispatcher) {
                val settings = settingsRepository.userSettings.first()
                _state.value =
                    _state.value.copy(
                        playerName = settings.playerName,
                        playerNameTextFieldText = settings.playerName ?: "",
                    )
            }
        }

        fun onEvent(event: MainMenuEvent) {
            when (event) {
                MainMenuEvent.SettingsClick -> {
                    navigateTo(Routes.Settings.route, popPrevious = false)
                }

                MainMenuEvent.PlayerNameClick -> {
                    showPlayerNameDialogWithoutSaveAction()
                }

                is MainMenuEvent.PlayerNameDialogTextChange -> {
                    _state.value = state.value.copy(playerNameTextFieldText = event.playerName)
                }

                is MainMenuEvent.PlayerNameDialogSave -> {
                    onPlayerNameDialogSave()
                }

                MainMenuEvent.PlayerNameDialogCancel -> {
                    _state.value = state.value.copy(isPlayerNameDialogVisible = false)
                }

                MainMenuEvent.CreateGameClick -> {
                    onCreateGameClick()
                }

                MainMenuEvent.JoinGameClick -> {
                    showPlayerNameDialogOrJoinGame()
                }
            }
        }

        private fun showPlayerNameDialogWithoutSaveAction() {
            _state.value = state.value.copy(isPlayerNameDialogVisible = true)
            onPlayerNameSave = {}
        }

        private fun showPlayerNameDialogWithSaveAction(action: () -> Unit) {
            _state.value = state.value.copy(isPlayerNameDialogVisible = true)
            onPlayerNameSave = action
        }

        private fun onPlayerNameDialogSave() {
            val playerName = state.value.playerNameTextFieldText
            viewModelScope.launch {
                withContext(ioDispatcher) {
                    settingsRepository.setPlayerName(playerName)
                }
                _state.value =
                    state.value.copy(
                        playerName = playerName,
                        isPlayerNameDialogVisible = false,
                    )
                // Execute the deferred action (Create or Join) on Main Thread
                launch {
                    onPlayerNameSave()
                }
            }
        }

        private fun onCreateGameClick() {
            if (state.value.playerName == null) {
                showPlayerNameDialogWithSaveAction { navigateToCreateGame() }
            } else {
                navigateToCreateGame()
            }
        }

        private fun showPlayerNameDialogOrJoinGame() {
            if (state.value.playerName == null) {
                showPlayerNameDialogWithSaveAction { navigateToJoinGame() }
            } else {
                navigateToJoinGame()
            }
        }

        private fun navigateToCreateGame() {
            navigateTo(
                destination = Routes.CreateGameLoading.route,
            )
        }

        private fun navigateToJoinGame() {
            navigateTo(
                destination = Routes.JoinGameGraph.route,
            )
        }

        private fun navigateTo(
            destination: String,
            popPrevious: Boolean = true,
        ) {
            viewModelScope.launch {
                _eventFlow.emit(
                    UiEvent.NavigateTo(
                        destination = destination,
                        popPrevious = popPrevious,
                    ),
                )
            }
        }
    }
