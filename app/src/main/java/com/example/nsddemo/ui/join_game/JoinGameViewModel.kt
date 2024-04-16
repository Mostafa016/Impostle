package com.example.nsddemo.ui.join_game

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.Debugging.TAG
import com.example.nsddemo.GameConstants
import com.example.nsddemo.NSDConstants
import com.example.nsddemo.network.Client
import com.example.nsddemo.ui.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress

class JoinGameViewModel(val gameViewModel: GameViewModel, val nsdManager: NsdManager) :
    ViewModel() {
    private val _gameCodeTextFieldState = mutableStateOf("")
    val gameCodeTextFieldState: State<String> = _gameCodeTextFieldState
    lateinit var gameCode: String
    private var mServiceName: String = NSDConstants.BASE_SERVICE_NAME

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: $service")
            when {
                service.serviceType != NSDConstants.SERVICE_TYPE -> // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")

                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: $mServiceName")

                service.serviceName.contains(NSDConstants.BASE_SERVICE_NAME) -> {
                    // Not host
                    if (service.serviceName.split("_").size != 2) {
                        Log.d(TAG, "${service.serviceName} does not belong to a host")
                        return
                    }
                    // Not the host of the lobby I want to join
                    val serviceGameCode = service.serviceName.split("_")[1]
                    if (serviceGameCode != gameCode) {
                        Log.d(
                            TAG,
                            "${service.serviceName} is not the host of the game I want to join with code $gameCode"
                        )
                        return
                    }
                    nsdManager.resolveService(
                        service, resolveListener
                    )
                    Log.d(TAG, "Found app service: ${service.serviceName}")
                }
            }
        }

        private val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(TAG, "Resolve Succeeded. $serviceInfo")

                if (serviceInfo.serviceName == mServiceName) {
                    Log.d(TAG, "Same IP.")
                    return
                }
                // Host of another game
                if (serviceInfo.serviceName.split("_")[1] != gameCode) {
                    Log.d(
                        TAG, "${serviceInfo.serviceName} Host of another game $gameCode"
                    )
                    return
                }
                // Save port and ip address for communication with sockets
                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host
                Log.d(TAG, "Client connecting to server with: ")
                Log.d(TAG, "Port: $port")
                Log.d(TAG, "Host: $host")
                val clientJob = gameViewModel.viewModelScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "Started client")
                    Log.d(TAG, "Address: ${host.hostAddress!!} Port: $port")
                    Client.run(
                        host.hostAddress!!,
                        port,
                        gameViewModel::handleServerMessages
                    )
                }
                gameViewModel.setClientJob(clientJob)
                stopSearchingForGame()
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            // TODO: Figure out when this happens as it causes duplicate players in server
            // when it happens from client
            Log.e(TAG, "Service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun discoverServices() {
        Log.d(TAG, "Before discover services 1")
        try {
            Log.d(TAG, nsdManager.toString())
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
        Log.d(TAG, "Before discover services 2")
        nsdManager.discoverServices(
            NSDConstants.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stopSearchingForGame() {
        nsdManager.stopServiceDiscovery(discoveryListener)
        _gameCodeTextFieldState.value = ""
    }
    fun onDiscoverAndResolveServicesClick() {
        gameCode = gameCodeTextFieldState.value
        Log.d(TAG, "Game Code: $gameCode")
        discoverServices()
    }

    fun onGameCodeTextFieldValueChange(text: String) {
        if (text.length <= GameConstants.CODE_LENGTH) {
            _gameCodeTextFieldState.value = text.uppercase()
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        class JoinGameViewModelFactory(
            private val gameViewModel: GameViewModel,
            private val nsdManager: NsdManager
        ) :
            ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                JoinGameViewModel(gameViewModel, nsdManager) as T
        }
    }
}