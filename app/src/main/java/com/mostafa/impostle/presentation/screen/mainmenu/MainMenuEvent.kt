package com.mostafa.impostle.presentation.screen.mainmenu

sealed interface MainMenuEvent {
    object SettingsClick : MainMenuEvent

    object PlayerNameClick : MainMenuEvent

    data class PlayerNameDialogTextChange(
        val playerName: String,
    ) : MainMenuEvent

    data class PlayerNameDialogSave(
        val playerName: String,
    ) : MainMenuEvent

    object PlayerNameDialogCancel : MainMenuEvent

    object CreateGameClick : MainMenuEvent

    object JoinGameClick : MainMenuEvent
}
