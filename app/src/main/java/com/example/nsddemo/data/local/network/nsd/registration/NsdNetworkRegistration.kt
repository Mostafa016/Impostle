package com.example.nsddemo.data.local.network.nsd.registration

import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.RegistrationListener
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.example.nsddemo.core.util.Debugging
import com.example.nsddemo.data.util.NSDConstants
import com.example.nsddemo.data.util.NSDConstants.nsdErrorCodeToString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class NsdNetworkRegistration
    @Inject
    constructor(
        private val nsdManager: NsdManager,
    ) : NetworkRegistration {
        private val _registrationState =
            MutableStateFlow<NsdRegistrationState>(NsdRegistrationState.Idle)
        override val registrationState: StateFlow<NsdRegistrationState> =
            _registrationState.asStateFlow()

        private var actualServiceName = NSDConstants.BASE_SERVICE_NAME

        private val registrationListener: RegistrationListener =
            object : RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    // Save the service name. Android may have changed it in order to
                    // resolve a conflict, so update the name you initially requested
                    // with the name Android actually used.
                    actualServiceName = serviceInfo.serviceName
                    _registrationState.value = (NsdRegistrationState.Registered(actualServiceName))
                    Log.d(Debugging.TAG, "Service address: ${serviceInfo.host} ${serviceInfo.port}")
                    Log.d(Debugging.TAG, "onServiceRegistered: actualServiceName = $actualServiceName")
                }

                override fun onRegistrationFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    val errorString = nsdErrorCodeToString(errorCode)
                    _registrationState.value = (NsdRegistrationState.Failed(errorString))
                    Log.e(Debugging.TAG, "onRegistrationFailed, Reason: $errorString")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    // Service has been unregistered. This only happens when you call
                    // NsdManager.unregisterService() and pass in this listener.
                    _registrationState.value =
                        (NsdRegistrationState.UnRegistered(serviceInfo.serviceName))
                    Log.d(Debugging.TAG, "onServiceUnregistered")
                }

                override fun onUnregistrationFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    val errorString = nsdErrorCodeToString(errorCode)
                    _registrationState.value = (NsdRegistrationState.Failed(errorString))
                    Log.e(Debugging.TAG, "onUnregistrationFailed, Reason: $errorString")
                }
            }

        override fun registerService(
            gameCode: String,
            port: Int,
        ) {
            val registrationState = _registrationState.value
            if (registrationState is NsdRegistrationState.Registering ||
                registrationState is NsdRegistrationState.Registered
            ) {
                Log.d(Debugging.TAG, "Ignoring registerService call. Current state: $registrationState")
                return
            }
            _registrationState.value = NsdRegistrationState.Registering
            val serviceInfo =
                NsdServiceInfo().apply {
                    // The name is subject to change based on conflicts
                    // with other services advertised on the same network.
                    serviceName = actualServiceName + "_$gameCode"
                    serviceType = NSDConstants.SERVICE_TYPE
                    setPort(port)
                }
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener,
            )
        }

        override fun unregisterService() {
            val registrationState = _registrationState.value
            if (registrationState !is NsdRegistrationState.Registered) {
                Log.d(
                    Debugging.TAG,
                    "Ignoring unregisterService call. Current state: $registrationState",
                )
                return
            }
            _registrationState.value = NsdRegistrationState.UnRegistering
            actualServiceName = NSDConstants.BASE_SERVICE_NAME
            nsdManager.unregisterService(registrationListener)
        }
    }
