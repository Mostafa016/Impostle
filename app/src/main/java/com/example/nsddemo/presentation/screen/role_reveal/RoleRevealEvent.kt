package com.example.nsddemo.presentation.screen.role_reveal

sealed interface RoleRevealEvent {
    data object ConfirmRoleReveal : RoleRevealEvent
}