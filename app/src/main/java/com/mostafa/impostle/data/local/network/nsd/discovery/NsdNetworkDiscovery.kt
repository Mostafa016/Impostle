package com.mostafa.impostle.data.local.network.nsd.discovery

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.mostafa.impostle.core.util.Debugging
import com.mostafa.impostle.data.util.NSDConstants
import com.mostafa.impostle.data.util.NSDConstants.nsdErrorCodeToString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class NsdNetworkDiscovery
    @Inject
    constructor(
        private val nsdManager: NsdManager,
    ) : NetworkDiscovery {
        private val _discoveryProcessState =
            MutableStateFlow<NsdDiscoveryState>(NsdDiscoveryState.Idle)
        override val discoveryProcessState: StateFlow<NsdDiscoveryState> =
            _discoveryProcessState.asStateFlow()

        private val _discoveredServiceEvent =
            MutableSharedFlow<NsdDiscoveryEvent>(replay = 16, extraBufferCapacity = 64)
        override val discoveredServiceEvent: Flow<NsdDiscoveryEvent> =
            _discoveredServiceEvent

        private lateinit var gameCode: String

        override fun startDiscovery(targetGameCode: String) {
            gameCode = targetGameCode
            nsdManager.discoverServices(
                NSDConstants.SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener,
            )
        }

        override fun stopDiscovery() {
            val discoveryProcessState = discoveryProcessState.value
            if (discoveryProcessState is NsdDiscoveryState.Stopping || discoveryProcessState is NsdDiscoveryState.Stopped) return
            _discoveryProcessState.value = NsdDiscoveryState.Stopping
            nsdManager.stopServiceDiscovery(discoveryListener)
        }

        // region Discovery Listener
        private val discoveryListener =
            object : NsdManager.DiscoveryListener {
                // Called as soon as service discovery begins.
                override fun onDiscoveryStarted(serviceType: String) {
                    Log.d(Debugging.TAG, "Service discovery started for: $serviceType")
                    _discoveryProcessState.value = NsdDiscoveryState.Discovering(serviceType)
                }

                override fun onServiceFound(service: NsdServiceInfo) {
                    // A service was found! Do something with it.
                    if (!isCurrentGameService(service)) {
                        return
                    }
                    Log.d(Debugging.TAG, "Service discovery success: $service")
                    _discoveredServiceEvent.tryEmit(NsdDiscoveryEvent.Found(service))
                }

                override fun onServiceLost(service: NsdServiceInfo) {
                    // When the network service is no longer available.
                    // Internal bookkeeping code goes here.
                    // TODO: Figure out when this happens as it causes duplicate players in server
                    //  when it happens from client
                    if (!isCurrentGameService(service)) {
                        return
                    }
                    Log.e(Debugging.TAG, "Lost Service: ${service.serviceName}")
                    _discoveredServiceEvent.tryEmit(NsdDiscoveryEvent.Lost(service))
                }

                override fun onDiscoveryStopped(serviceType: String) {
                    Log.d(Debugging.TAG, "Discovery stopped: $serviceType")
                    _discoveryProcessState.value = NsdDiscoveryState.Stopped(serviceType)
                }

                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) {
                    val errorMessage = nsdErrorCodeToString(errorCode)
                    Log.e(Debugging.TAG, "Discovery failed: Error code:$errorMessage")
                    _discoveryProcessState.value =
                        NsdDiscoveryState.Failed("Discovery failed: Error code:$errorMessage")
                }

                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) {
                    val errorMessage = nsdErrorCodeToString(errorCode)
                    Log.e(Debugging.TAG, "Stop discovery failed: Error code:$errorMessage")
                    _discoveryProcessState.value =
                        NsdDiscoveryState.Failed("Stop discovery failed: Error code:$errorMessage")
                }
            }

        private fun isCurrentGameService(service: NsdServiceInfo): Boolean {
            if (service.serviceType != NSDConstants.SERVICE_TYPE) {
                // Service type is the string containing the protocol and
                // transport layer for this service.
                Log.d(Debugging.TAG, "Unknown Service Type: ${service.serviceType}")
                return false
            } else if (service.serviceName.contains(NSDConstants.BASE_SERVICE_NAME)) {
                // Could be used for a feature where the client says they're interested
                // to join a game and a list of players appears to the host to choose which one
                // to invite

                // Not a host
                val fullServiceNameSegmented = service.serviceName.split("_")
                if (fullServiceNameSegmented.size != 2) {
                    Log.d(Debugging.TAG, "${service.serviceName} does not belong to a host")
                    return false
                }

                // Not the host of the lobby I want to join
                val serviceGameCode = fullServiceNameSegmented.last()
                if (serviceGameCode != gameCode) {
                    Log.d(
                        Debugging.TAG,
                        "${service.serviceName} is not the host of the game I want to join with code $gameCode",
                    )
                    return false
                }

                // Correct host found
                Log.d(
                    Debugging.TAG,
                    "Found service with code ($gameCode): ${service.serviceName}",
                )
                return true
            }
            return false
        }

        // endregion
    }
