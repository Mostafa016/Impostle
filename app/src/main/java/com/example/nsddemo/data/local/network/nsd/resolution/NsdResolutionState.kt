package com.example.nsddemo.data.local.network.nsd.resolution

sealed interface NsdResolutionState {
    data object Idle : NsdResolutionState
    data object Resolving : NsdResolutionState
    data class Success(val host: String, val port: Int) : NsdResolutionState
    data object Stopped : NsdResolutionState
    data class Failed(val error: String) : NsdResolutionState
}