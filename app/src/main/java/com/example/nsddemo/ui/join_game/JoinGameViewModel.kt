package com.example.nsddemo.ui.join_game

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nsddemo.network.Client
import com.example.nsddemo.Debugging
import com.example.nsddemo.NSDConstants
import com.example.nsddemo.ui.GameViewModel
import com.example.nsddemo.ui.category_and_word.ChooseCategoryViewModel
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
            Log.d(Debugging.TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(Debugging.TAG, "Service discovery success: $service")
            when {
                service.serviceType != NSDConstants.SERVICE_TYPE -> // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(Debugging.TAG, "Unknown Service Type: ${service.serviceType}")

                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(Debugging.TAG, "Same machine: $mServiceName")

                service.serviceName.contains(NSDConstants.BASE_SERVICE_NAME) -> {
                    // Not host
                    if (service.serviceName.split("_").size != 2) {
                        Log.d(Debugging.TAG, "${service.serviceName} does not belong to a host")
                        return
                    }
                    // Not the host of the lobby I want to join
                    val serviceGameCode = service.serviceName.split("_")[1].lowercase()
                    if (serviceGameCode != gameCode) {
                        Log.d(
                            Debugging.TAG,
                            "${service.serviceName} is not the host of the game I want to join with code $gameCode"
                        )
                        return
                    }
                    nsdManager.resolveService(
                        service, resolveListener
                    )
                    Log.d(Debugging.TAG, "Found app service: ${service.serviceName}")
                }
            }
        }

        private val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                // Called when the resolve fails. Use the error code to debug.
                Log.e(Debugging.TAG, "Resolve failed: $errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.e(Debugging.TAG, "Resolve Succeeded. $serviceInfo")

                if (serviceInfo.serviceName == mServiceName) {
                    Log.d(Debugging.TAG, "Same IP.")
                    return
                }
                // Host of another game
                if (serviceInfo.serviceName.split("_")[1].lowercase() != gameCode) {
                    Log.d(
                        Debugging.TAG, "${serviceInfo.serviceName} Host of another game $gameCode"
                    )
                    return
                }
                // Save port and ip address for communication with sockets
                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host
                Log.d(Debugging.TAG, "Client connecting to server with: ")
                Log.d(Debugging.TAG, "Port: $port")
                Log.d(Debugging.TAG, "Host: $host")
                val clientJob = gameViewModel.viewModelScope.launch(Dispatchers.IO) {
                    Log.d(Debugging.TAG, "Started client")
                    Log.d(Debugging.TAG, "Address: ${host.hostAddress!!} Port: $port")
                    Client.run(host.hostAddress!!, port, gameViewModel::handleServerMessages)
                }
                gameViewModel.setClientJob(clientJob)
                //_hasJoinedGame.value = true
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            // TODO: Figure out when this happens as it causes duplicate players in server
            // when it happens from client
            Log.e(Debugging.TAG, "Service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(Debugging.TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(Debugging.TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(Debugging.TAG, "Discovery failed: Error code:$errorCode")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private fun discoverServices() {
        Log.d(Debugging.TAG, "Before discover services 1")
        try {
            Log.d(Debugging.TAG, nsdManager.toString())
        } catch (e: Exception) {
            Log.d(Debugging.TAG, e.message.toString())
        }
        Log.d(Debugging.TAG, "Before discover services 2")
        nsdManager.discoverServices(
            NSDConstants.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun onDiscoverAndResolveServicesClick() {
        gameCode = gameCodeTextFieldState.value.lowercase()
        Log.d(Debugging.TAG, "Game Code: $gameCode")
        discoverServices()
    }

    fun onGameCodeTextFieldValueChange(text: String) {
        _gameCodeTextFieldState.value = text
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