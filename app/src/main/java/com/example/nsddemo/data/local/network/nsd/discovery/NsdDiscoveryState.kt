package com.example.nsddemo.data.local.network.nsd.discovery

sealed interface NsdDiscoveryState {
    data object Idle : NsdDiscoveryState
    data class Discovering(val serviceType: String) : NsdDiscoveryState
    data class Stopped(val serviceType: String) : NsdDiscoveryState
    data class Failed(val error: String) : NsdDiscoveryState
}