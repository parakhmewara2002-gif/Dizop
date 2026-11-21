package com.dizop.print.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.Socket

/**
 * Dual-Network Multi-Path Router:
 * Solves the classic Android issue where Mobile Data (SIM) is ON without Internet on Wi-Fi,
 * causing Android to route all socket and mDNS packets over LTE/5G instead of Local Wi-Fi (No-Internet Printer LAN).
 */
class DualNetworkRouter(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _printerWifiNetwork = MutableStateFlow<Network?>(null)
    val printerWifiNetwork: StateFlow<Network?> = _printerWifiNetwork.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun bindToLocalPrinterNetwork() {
        if (networkCallback != null) return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i("DizopNetwork", "Printer Wi-Fi network detected! Binding socket routing...")
                _printerWifiNetwork.value = network
                
                try {
                    connectivityManager.bindProcessToNetwork(network)
                } catch (e: Exception) {
                    Log.w("DizopNetwork", "bindProcessToNetwork info", e)
                }
            }

            override fun onLost(network: Network) {
                if (_printerWifiNetwork.value == network) {
                    _printerWifiNetwork.value = null
                    try {
                        connectivityManager.bindProcessToNetwork(null)
                    } catch (_: Exception) {}
                }
            }
        }

        try {
            // registerNetworkCallback does NOT require CHANGE_NETWORK_STATE or root on Android 10+
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e("DizopNetwork", "Error registering network callback", e)
        }
    }

    /**
     * Creates a socket explicitly bound to the Printer's Wi-Fi network interface,
     * completely bypassing Mobile Data (SIM internet).
     */
    fun createBoundSocket(): Socket {
        val wifiNetwork = _printerWifiNetwork.value
        return if (wifiNetwork != null) {
            val socket = Socket()
            wifiNetwork.bindSocket(socket)
            socket
        } else {
            Socket()
        }
    }

    fun release() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
        }
        networkCallback = null
        try {
            connectivityManager.bindProcessToNetwork(null)
        } catch (_: Exception) {}
    }
}
