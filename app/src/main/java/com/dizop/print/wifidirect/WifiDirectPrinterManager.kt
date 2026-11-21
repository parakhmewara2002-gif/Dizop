package com.dizop.print.wifidirect

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.dizop.print.data.DiscoveredPrinter
import com.dizop.print.data.InkLevel
import com.dizop.print.data.PrinterBrand
import com.dizop.print.data.PrinterProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WifiDirectPrinterDevice(
    val deviceName: String,
    val deviceAddress: String,
    val status: String,
    val isPrinter: Boolean
)

class WifiDirectPrinterManager(private val context: Context) {

    private val p2pManager: WifiP2pManager? =
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _p2pPrinters = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val p2pPrinters: StateFlow<List<WifiP2pDevice>> = _p2pPrinters.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo: StateFlow<WifiP2pInfo?> = _connectionInfo.asStateFlow()

    private var p2pReceiver: BroadcastReceiver? = null

    init {
        channel = p2pManager?.initialize(context, Looper.getMainLooper(), null)
    }

    @SuppressLint("MissingPermission")
    fun startWifiDirectDiscovery(onPrinterFound: (DiscoveredPrinter) -> Unit) {
        if (p2pManager == null || channel == null) return

        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        p2pReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        p2pManager.requestPeers(channel) { peers: WifiP2pDeviceList? ->
                            val devices = peers?.deviceList?.toList() ?: emptyList()
                            Log.i("DizopWifiDirect", "Peers found: ${devices.size}")
                            
                            val printerPeers = devices.filter { isPrinterDevice(it) }
                            _p2pPrinters.value = printerPeers

                            printerPeers.forEach { dev ->
                                val brand = detectBrand(dev.deviceName)
                                val printer = DiscoveredPrinter(
                                    id = "p2p_${dev.deviceAddress}",
                                    name = "⚡ Direct: ${dev.deviceName}",
                                    model = dev.deviceName,
                                    hostAddress = "192.168.49.1", // Standard Wi-Fi Direct Group Owner IP
                                    port = 9100, // Standard Wi-Fi Direct Print port
                                    brand = brand,
                                    protocol = PrinterProtocol.RAW_JETDIRECT,
                                    isOnline = true,
                                    isReady = true,
                                    inkLevels = emptyList() // Truthful telemetry: not reported over raw P2P
                                )
                                onPrinterFound(printer)
                            }
                        }
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        p2pManager.requestConnectionInfo(channel) { info ->
                            _connectionInfo.value = info
                            if (info?.groupFormed == true) {
                                val groupOwnerIp = info.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
                                Log.i("DizopWifiDirect", "P2P Connected! Group Owner IP: $groupOwnerIp")
                                
                                // Update discovered printer IP with real groupOwnerAddress
                                val updated = _p2pPrinters.value.map { dev ->
                                    val brand = detectBrand(dev.deviceName)
                                    DiscoveredPrinter(
                                        id = "p2p_${dev.deviceAddress}",
                                        name = "⚡ Direct: ${dev.deviceName}",
                                        model = dev.deviceName,
                                        hostAddress = groupOwnerIp,
                                        port = 9100,
                                        brand = brand,
                                        protocol = PrinterProtocol.RAW_JETDIRECT,
                                        isOnline = true,
                                        isReady = true,
                                        inkLevels = emptyList()
                                    )
                                }
                                updated.forEach { onPrinterFound(it) }
                            }
                        }
                    }
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(p2pReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(p2pReceiver, intentFilter)
            }
        } catch (e: Exception) {
            Log.e("DizopWifiDirect", "Register receiver failed", e)
        }

        _isDiscovering.value = true
        p2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i("DizopWifiDirect", "Wi-Fi Direct peer discovery initiated successfully")
            }

            override fun onFailure(reasonCode: Int) {
                Log.w("DizopWifiDirect", "Wi-Fi Direct peer discovery failed code: $reasonCode")
                _isDiscovering.value = false
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToPrinter(device: WifiP2pDevice, onSuccess: () -> Unit, onFailure: (Int) -> Unit) {
        if (p2pManager == null || channel == null) return
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        p2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                onSuccess()
            }

            override fun onFailure(reason: Int) {
                onFailure(reason)
            }
        })
    }

    private fun isPrinterDevice(dev: WifiP2pDevice): Boolean {
        val name = dev.deviceName.lowercase()
        // Primary printer type categorization or keyword matching
        return name.contains("direct") ||
                name.contains("hp") ||
                name.contains("epson") ||
                name.contains("canon") ||
                name.contains("brother") ||
                name.contains("printer") ||
                name.contains("deskjet") ||
                name.contains("laserjet") ||
                name.contains("pixma") ||
                dev.primaryDeviceType?.startsWith("6-") == true // Wi-Fi Alliance category 6: Printer
    }

    private fun detectBrand(name: String): PrinterBrand {
        val lower = name.lowercase()
        return when {
            lower.contains("hp") || lower.contains("deskjet") || lower.contains("laserjet") -> PrinterBrand.HP
            lower.contains("canon") || lower.contains("pixma") -> PrinterBrand.CANON
            lower.contains("epson") || lower.contains("ecotank") -> PrinterBrand.EPSON
            lower.contains("brother") -> PrinterBrand.BROTHER
            else -> PrinterBrand.GENERIC
        }
    }

    fun stopDiscovery() {
        _isDiscovering.value = false
        p2pReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        p2pReceiver = null
        p2pManager?.stopPeerDiscovery(channel, null)
    }
}
