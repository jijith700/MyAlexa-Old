package com.jijith.alexa.service.managersimpl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import com.amazon.aace.network.NetworkInfoProvider.NetworkStatus
import com.jijith.alexa.service.interfaces.managers.NetworkManager
import timber.log.Timber

class NetworkMangerImpl(private var context: Context, private var networkConnection: NetworkConnection) :
    NetworkManager {

    private var wifiManager: WifiManager? = null
    private var connectivityManager: ConnectivityManager? = null
    private var receiver: NetworkChangeReceiver? = null
    private var status = NetworkStatus.UNKNOWN
    private var rssi = RSSIStatus.INVALID_RSSI.ordinal

    init {
        // Note: >=API 24 should use NetworkCallback to receive network change updates
        // instead of CONNECTIVITY_ACTION
        receiver = NetworkChangeReceiver()
        context.registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    inner class NetworkChangeReceiver : BroadcastReceiver() {

        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            if (connectivityManager != null) {
                updateNetworkStatus()
                Timber.d("Network status changed. STATUS: %s, RSSI: %s", status, rssi)
            }
        }
    }

    private fun updateNetworkStatus() {
        val activeNetwork = connectivityManager?.getActiveNetworkInfo()
        rssi = wifiManager?.getConnectionInfo()?.getRssi()!!
        if (activeNetwork != null) {
            val state = activeNetwork.state
            status = when (state) {
                NetworkInfo.State.CONNECTED -> NetworkStatus.CONNECTED
                NetworkInfo.State.CONNECTING -> NetworkStatus.CONNECTING
                NetworkInfo.State.DISCONNECTING -> NetworkStatus.DISCONNECTING
                NetworkInfo.State.DISCONNECTED, NetworkInfo.State.SUSPENDED -> NetworkStatus.DISCONNECTED
                NetworkInfo.State.UNKNOWN -> NetworkStatus.UNKNOWN
                else -> NetworkStatus.UNKNOWN
            }
            networkConnection?.onNetworkConnectionChange(status, rssi)
        } else {
            status = NetworkStatus.UNKNOWN
            networkConnection?.onNetworkConnectionChange(status, rssi)
        }
    }

    fun unregister() {
        context.unregisterReceiver(receiver)
    }

    interface NetworkConnection {
        fun onNetworkConnectionChange(status: NetworkStatus, rssi: Int)
    }

    enum class RSSIStatus(private val rssi: Int) {
        INVALID_RSSI(-127), MIN_RSSI(-126), MAX_RSSI(200);
    }
}