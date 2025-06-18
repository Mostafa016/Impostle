package com.example.nsddemo.data.local.network.nsd.resolution

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.nsddemo.core.util.Debugging
import com.example.nsddemo.data.util.NSDConstants.nsdErrorCodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


class NsdNetworkResolution @Inject constructor(private val nsdManager: NsdManager) :
    NetworkResolution {
    private val _resolutionState = MutableStateFlow<NsdResolutionState>(NsdResolutionState.Idle)
    override val resolutionState: StateFlow<NsdResolutionState> = _resolutionState.asStateFlow()

    private lateinit var gameCode: String

    override fun resolveServiceWithGameCode(serviceInfo: NsdServiceInfo, gameCode: String) {
        this.gameCode = gameCode
        _resolutionState.value = NsdResolutionState.Resolving
        nsdManager.resolveService(serviceInfo, resolveListener)
    }

    // region Resolve
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            val errorMessage = nsdErrorCodeToString(errorCode)
            _resolutionState.value = NsdResolutionState.Failed(errorMessage)
            Log.e(Debugging.TAG, "Resolve failed: $errorMessage")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            // Handle same device advertising and discovering
//            if (serviceInfo.serviceName == mServiceName) {
//                Log.d(Debugging.TAG, "Same IP.")
//                return
//            }
            // Host of another game
            val fullServiceNameSegmented = serviceInfo.serviceName.split("_")
            val resolvedServiceGameCode = fullServiceNameSegmented[1]
            if (resolvedServiceGameCode != gameCode) {
                Log.w(
                    Debugging.TAG, "${serviceInfo.serviceName} Host of another game $gameCode"
                )
                return
            }
            // Save port and ip address for communication with sockets
            _resolutionState.value = NsdResolutionState.Success(
                host = serviceInfo.host.hostAddress!!,
                port = serviceInfo.port,
            )
            Log.i(Debugging.TAG, "Resolve Succeeded: $serviceInfo")
        }
    }
    // endregion
}