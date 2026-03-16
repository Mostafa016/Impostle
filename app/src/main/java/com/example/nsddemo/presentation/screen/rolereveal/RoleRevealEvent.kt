package com.example.nsddemo.presentation.screen.rolereveal

sealed interface RoleRevealEvent {
    data object ConfirmRoleReveal : RoleRevealEvent
}
