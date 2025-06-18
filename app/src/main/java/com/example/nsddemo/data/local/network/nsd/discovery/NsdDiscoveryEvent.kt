package com.example.nsddemo.data.local.network.nsd.discovery

import android.net.nsd.NsdServiceInfo

sealed class NsdDiscoveryEvent(open val serviceInfo: NsdServiceInfo) {
    data class Found(override val serviceInfo: NsdServiceInfo) : NsdDiscoveryEvent(serviceInfo)
    data class Lost(override val serviceInfo: NsdServiceInfo) : NsdDiscoveryEvent(serviceInfo)
}