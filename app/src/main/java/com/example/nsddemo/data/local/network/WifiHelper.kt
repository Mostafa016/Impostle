package com.example.nsddemo.data.local.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter

class WifiHelper(private val context: Context) {
    private val connectivityManager by lazy {
        context.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
    }

    val ipAddress: String?
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.let {
                    val wifiTransportInfo = it.transportInfo as WifiInfo
                    Formatter.formatIpAddress(wifiTransportInfo.ipAddress)
                }
            } else {
                (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).let {
                    try {
                        val wifiInfo = it.connectionInfo
                        Formatter.formatIpAddress(wifiInfo.ipAddress)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }
}