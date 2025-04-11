package com.example.nsddemo.data.local.network

import android.content.Context
import android.net.ConnectivityManager
import java.net.Inet4Address

class WifiHelper(private val context: Context) {
    private val connectivityManager by lazy {
        context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
    }

    val ipAddress: String?
        get() {
            val activeNetwork = connectivityManager.activeNetwork
                ?: return null // Check if there is an active network
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                ?: return null // Get its properties

            // Iterate through the link addresses to find an IPv4 address
            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    return address.hostAddress
                }
            }
            return null
        }
}