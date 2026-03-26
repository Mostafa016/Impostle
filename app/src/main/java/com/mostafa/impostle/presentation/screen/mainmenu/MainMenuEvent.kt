package com.mostafa.impostle.presentation.screen.mainmenu

import com.mostafa.impostle.domain.model.AppPermission

sealed interface MainMenuEvent {
    data object SettingsClick : MainMenuEvent

    data object PlayerNameClick : MainMenuEvent

    data class PlayerNameDialogTextChange(
        val playerName: String,
    ) : MainMenuEvent

    data class PlayerNameDialogSave(
        val playerName: String,
    ) : MainMenuEvent

    data object PlayerNameDialogCancel : MainMenuEvent

    data object CreateGameClick : MainMenuEvent

    data object JoinGameClick : MainMenuEvent

    data class PermissionRequested(
        val permission: AppPermission,
    ) : MainMenuEvent
}
