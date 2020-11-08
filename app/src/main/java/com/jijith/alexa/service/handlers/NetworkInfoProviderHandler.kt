package com.jijith.alexa.service.handlers

import android.content.Context
import com.amazon.aace.network.NetworkInfoProvider
import com.jijith.alexa.service.managersimpl.NetworkMangerImpl
import timber.log.Timber

class NetworkInfoProviderHandler(private var context: Context) : NetworkInfoProvider(),
    NetworkMangerImpl.NetworkConnection {

    private var status: NetworkStatus = NetworkStatus.UNKNOWN
    private var rssiStatus = NetworkMangerImpl.RSSIStatus.INVALID_RSSI.ordinal

    override fun getNetworkStatus(): NetworkStatus {
        Timber.d("status= %s", status)
        return status
    }

    override fun getWifiSignalStrength(): Int {
        Timber.d("wifiSignalStrength= %d", rssiStatus)
        return rssiStatus
    }

    override fun onNetworkConnectionChange(status: NetworkStatus, rssi: Int) {
        Timber.d("status= %s wifiSignalStrength= %d", status, rssi)
        this.status = status
        this.rssiStatus = rssi
        networkStatusChanged(status, rssi)
    }
}