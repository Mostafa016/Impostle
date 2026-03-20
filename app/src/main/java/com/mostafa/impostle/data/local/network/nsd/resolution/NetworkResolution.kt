package com.mostafa.impostle.data.local.network.nsd.resolution

import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.StateFlow

interface NetworkResolution {
    val resolutionState: StateFlow<NsdResolutionState>

    fun resolveServiceWithGameCode(
        serviceInfo: NsdServiceInfo,
        gameCode: String,
    )
}
