package com.example.nsddemo.di

import com.example.nsddemo.data.local.network.nsd.discovery.NetworkDiscovery
import com.example.nsddemo.data.local.network.nsd.discovery.NsdNetworkDiscovery
import com.example.nsddemo.data.local.network.nsd.registration.NetworkRegistration
import com.example.nsddemo.data.local.network.nsd.registration.NsdNetworkRegistration
import com.example.nsddemo.data.local.network.nsd.resolution.NetworkResolution
import com.example.nsddemo.data.local.network.nsd.resolution.NsdNetworkResolution
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NsdModule {
    @Binds
    abstract fun bindNetworkDiscovery(
        nsdNetworkDiscovery: NsdNetworkDiscovery
    ): NetworkDiscovery

    @Binds
    abstract fun bindNetworkRegistration(
        nsdNetworkRegistration: NsdNetworkRegistration
    ): NetworkRegistration

    @Binds
    abstract fun bindNetworkResolution(
        nsdNetworkResolution: NsdNetworkResolution
    ): NetworkResolution
}