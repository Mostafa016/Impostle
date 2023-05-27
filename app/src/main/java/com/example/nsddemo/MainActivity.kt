package com.example.nsddemo

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.nsddemo.Debugging.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mServiceName: String = BASE_SERVICE_NAME

    private lateinit var nsdManager: NsdManager

    private lateinit var mService: NsdServiceInfo

    private lateinit var gameCode: String

    private var isHost = false

    private val registrationListener = object : NsdManager.RegistrationListener {


        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            // Save the service name. Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service address: ${NsdServiceInfo.host} ${NsdServiceInfo.port}")
            Log.d(TAG, "onServiceRegistered: serviceName = $mServiceName")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Registration failed! Put debugging code here to determine why.
            Log.e(TAG, "onRegistrationFailed, Reason: $errorCode")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {
            // Service has been unregistered. This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
            Log.d(TAG, "onServiceUnregistered")
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Unregistration failed. Put debugging code here to determine why.
            Log.e(TAG, "onUnregistrationFailed, Reason: $errorCode")
        }
    }

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : NsdManager.DiscoveryListener {

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            Log.d(TAG, "Service discovery success: $service")
            when {
                service.serviceType != SERVICE_TYPE -> // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")

                service.serviceName == mServiceName -> // The name of the service tells the user what they'd be
                    // connecting to. It could be "Bob's Chat App".
                    Log.d(TAG, "Same machine: $mServiceName")

                service.serviceName.contains(BASE_SERVICE_NAME) -> {
                    // Not host
                    if (service.serviceName.split("_").size != 2) {
                        Log.d(TAG, "${service.serviceName} does not belong to a host")
                        return
                    }
                    // Not the host of the lobby I want to join
                    val serviceGameCode = service.serviceName.split("_")[1].lowercase()
                    if (serviceGameCode != gameCode) {
                        Log.d(TAG, "${service.serviceName} is not the host of the game I want to join with code $gameCode")
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
                if (serviceInfo.serviceName.split("_")[1].lowercase() != gameCode) {
                    Log.d(TAG, "${serviceInfo.serviceName} Host of another game $gameCode")
                    return
                }
                mService = serviceInfo
                // TODO: Save port and ip address for communication with sockets
                val port: Int = serviceInfo.port
                val host: InetAddress = serviceInfo.host
                Log.d(TAG, "Client connecting to server with: ")
                Log.d(TAG, "Port: $port")
                Log.d(TAG, "Host: $host")
                CoroutineScope(Dispatchers.IO).launch{
                    Log.d(TAG, "Started client")
                    Log.d(TAG, "Address: ${host.hostAddress!!} Port: $port")
                    Client.run(host.hostAddress!!, port)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnRegisterService).setOnClickListener {
            // TODO: Get port number from the socket thing
            gameCode = generateGameCode()
            Log.d(TAG, "gameCode: $gameCode")
            findViewById<TextView>(R.id.tvCode).text = "Code: $gameCode"
            var serverIP: String? = null
            try {
                val wm = this.getSystemService(WIFI_SERVICE) as WifiManager
                serverIP = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
                Log.d(TAG, "Device IP: $serverIP")
            }
            catch (e: Exception){
                Log.d(TAG, e.message.toString())
            }
            val serverPort: Int = Server.initServerSocket(serverIP!!)
            val tmp = CoroutineScope(Dispatchers.IO).launch {
                Log.d(TAG, "Started server")
                Server.run()
            }
            Log.d(TAG, tmp.children.toString())
            registerService(serverPort, gameCode)
        }

        findViewById<Button>(R.id.btnDiscoverAndResolveService).setOnClickListener {
            // TODO: Pass lobby "code" to it so it uniquely identifies the host
            gameCode = findViewById<EditText>(R.id.etGameCode).text.toString().lowercase()
            Log.d(TAG, "Button Clicked")
            discoverServices()
        }

        findViewById<Switch>(R.id.switchIsHost).setOnCheckedChangeListener { compoundButton, isChecked ->
            isHost = isChecked
        }

    }

    override fun onPause() {
        //tearDownNsdService()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        // registerService(connection.socket.localAddress.toJavaAddress().port, gameCode)
        //discoverServices()
    }

    override fun onDestroy() {
        //tearDownNsdService()
        // connection.socket.close()
        super.onDestroy()
    }

    private fun generateGameCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..CODE_LENGTH).map { allowedChars.random() }.joinToString("")
    }

    // NsdHelper's tearDown method
    private fun tearDownNsdService() {
        nsdManager.apply {
            unregisterService(registrationListener)
            stopServiceDiscovery(discoveryListener)
        }
    }

    private fun registerService(port: Int, gameCode: String) {
        // Create the NsdServiceInfo object, and populate it.
        val serviceInfo = NsdServiceInfo().apply {
            // The name is subject to change based on conflicts
            // with other services advertised on the same network.
            serviceName = if (isHost) mServiceName + "_$gameCode" else mServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        Log.d(TAG, "Created serviceInfo")
        try {
            nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
                // TODO: Handle registering multiple times for service and for server (Look more into
                //  coroutines and stuff)
                this.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        } catch (E: Exception) {
            Log.e(TAG, E.message.toString())
        }
        finally {

        }
        Log.d(TAG, "Created nsdManager")
    }

    private fun discoverServices() {
        Log.d(TAG, "Before discover services 1")
        try {
            nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager)
            Log.d(TAG, nsdManager.toString())
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
        Log.d(TAG, "Before discover services 2")
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    companion object {
        private const val BASE_SERVICE_NAME = "NsdChat"
        private const val SERVICE_TYPE = "_nsdchat._tcp."
        private const val CODE_LENGTH = 6
    }
}