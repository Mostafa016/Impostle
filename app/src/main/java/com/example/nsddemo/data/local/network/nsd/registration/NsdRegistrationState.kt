package com.example.nsddemo.data.local.network.nsd.registration

sealed interface NsdRegistrationState {
    data object Idle : NsdRegistrationState
    data object Registering : NsdRegistrationState
    data class Registered(val serviceName: String) : NsdRegistrationState
    data class UnRegistered(val serviceName: String) : NsdRegistrationState
    data class Failed(val error: String) : NsdRegistrationState
}