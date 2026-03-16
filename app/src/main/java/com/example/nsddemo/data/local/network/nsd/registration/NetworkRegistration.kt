package com.example.nsddemo.data.local.network.nsd.registration

import kotlinx.coroutines.flow.StateFlow

interface NetworkRegistration {
    val registrationState: StateFlow<NsdRegistrationState>

    fun registerService(
        gameCode: String,
        port: Int,
    )

    fun unregisterService()
}
