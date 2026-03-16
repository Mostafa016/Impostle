package com.example.nsddemo.data.local.network.nsd.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NetworkDiscovery {
    val discoveryProcessState: StateFlow<NsdDiscoveryState>
    val discoveredServiceEvent: Flow<NsdDiscoveryEvent> // Flow<> to support multiple services

    fun startDiscovery(targetGameCode: String)

    fun stopDiscovery()
}
