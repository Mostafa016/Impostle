package com.mostafa.impostle.presentation.screen.rolereveal

sealed interface RoleRevealEvent {
    data object ConfirmRoleReveal : RoleRevealEvent
}
