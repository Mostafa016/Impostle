package com.example.nsddemo.data.local.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.nsddemo.core.util.Debugging
import com.example.nsddemo.data.util.NSDConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.properties.Delegates

class NSDHelper(private val context: Context) {
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(AppCompatActivity.NSD_SERVICE) as NsdManager
    }
    private var mServiceName: String = NSDConstants.BASE_SERVICE_NAME

    private lateinit var gameCode: String

    // region Registration
    private val _isServiceRegistered = MutableStateFlow(false)
    val isServiceRegistered: StateFlow<Boolean> = _isServiceRegistered.asStateFlow()

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = serviceInfo.serviceName
            Log.d(Debugging.TAG, "Service address: ${serviceInfo.host} ${serviceInfo.port}")
            Log.d(Debugging.TAG, "onServiceRegistered: serviceName = $mServiceName")
            // When ending a game and starting another
            _isServiceRegistered.value = true
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(Debugging.TAG, "onRegistrationFailed, Reason: $${errorCodeToString(errorCode)}")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(Debugging.TAG, "onServiceUnregistered")
            _isServiceRegistered.value = false
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(Debugging.TAG, "onUnregistrationFailed, Reason: ${errorCodeToString(errorCode)}")
            _isServiceRegistered.value = false
        }
    }

    fun registerService(port: Int, gameCode: String) {
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = mServiceName + "_$gameCode"
            serviceType = NSDConstants.SERVICE_TYPE
            setPort(port)
        }
        Log.d(Debugging.TAG, "Created serviceInfo")
        try {
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (E: Exception) {
            Log.e(Debugging.TAG, E.message.toString())
        }
        Log.d(Debugging.TAG, "Created nsdManager")
    }

    fun unregisterService() {
        try {
            mServiceName = NSDConstants.BASE_SERVICE_NAME
            nsdManager.unregisterService(registrationListener)
        } catch (E: Exception) {
            Log.e(Debugging.TAG, E.message.toString())
        }
    }
    // endregion

    // region Discovery
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
                    // TODO: Could be used for a feature where the client says they're interested
                    //  to join a game and a list of players appears to the host to choose which one
                    //  to invite
                    // Not a host
                    if (service.serviceName.split("_").size != 2) {
                        Log.d(Debugging.TAG, "${service.serviceName} does not belong to a host")
                        return
                    }
                    // Not the host of the lobby I want to join
                    val serviceGameCode = service.serviceName.split("_").last()
                    if (serviceGameCode != gameCode) {
                        Log.d(
                            Debugging.TAG,
                            "${service.serviceName} is not the host of the game I want to join with code $gameCode"
                        )
                        return
                    }
                    // The host of the lobby I want to join
                    nsdManager.resolveService(
                        service, resolveListener
                    )
                    Log.d(Debugging.TAG, "Found app service: ${service.serviceName}")
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            // TODO: Figure out when this happens as it causes duplicate players in server
            //  when it happens from client
            Log.e(Debugging.TAG, "Service lost: $service")
            _isServiceResolved.value = false
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(Debugging.TAG, "Discovery stopped: $serviceType")
            _isServiceResolved.value = false
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(Debugging.TAG, "Discovery failed: Error code:${errorCodeToString(errorCode)}")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(Debugging.TAG, "Discovery failed: Error code:${errorCodeToString(errorCode)}")
            nsdManager.stopServiceDiscovery(this)
        }
    }

    fun discoverServiceWithGameCode(gameCode: String) {
        try {
            Log.d(Debugging.TAG, nsdManager.toString())
        } catch (e: Exception) {
            Log.e(Debugging.TAG, e.message.toString())
        }
        this.gameCode = gameCode
        nsdManager.discoverServices(
            NSDConstants.SERVICE_TYPE,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stopServiceDiscovery() {
        nsdManager.stopServiceDiscovery(discoveryListener)
    }
    // endregion

    // region Resolve
    // Used to stop discovery when the service is resolved and for starting the client
    private val _isServiceResolved = MutableStateFlow(false)
    val isServiceResolved: StateFlow<Boolean> = _isServiceResolved.asStateFlow()

    lateinit var hostIpAddress: String
    var hostPort by Delegates.notNull<Int>()

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(Debugging.TAG, "Resolve failed: ${errorCodeToString(errorCode)}")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(Debugging.TAG, "Resolve Succeeded. $serviceInfo")
            if (serviceInfo.serviceName == mServiceName) {
                Log.d(Debugging.TAG, "Same IP.")
                return
            }
            // Host of another game
            if (serviceInfo.serviceName.split("_")[1] != gameCode) {
                Log.d(
                    Debugging.TAG, "${serviceInfo.serviceName} Host of another game $gameCode"
                )
                return
            }
            // Save port and ip address for communication with sockets
            hostIpAddress = serviceInfo.host.hostAddress!!
            hostPort = serviceInfo.port
            Log.d(Debugging.TAG, "Client connecting to server with: ")
            Log.d(Debugging.TAG, "Host: $hostIpAddress")
            Log.d(Debugging.TAG, "Port: $hostPort")
            _isServiceResolved.value = true
        }
    }

    // endregion
    private companion object {
        private fun errorCodeToString(errorCode: Int): String {
            return when (errorCode) {
                NsdManager.FAILURE_ALREADY_ACTIVE -> "FAILURE_ALREADY_ACTIVE"
                NsdManager.FAILURE_INTERNAL_ERROR -> "FAILURE_INTERNAL_ERROR"
                NsdManager.FAILURE_MAX_LIMIT -> "FAILURE_MAX_LIMIT"
                else -> "Unknown Error Code"
            }
        }
    }
}