package com.dizop.print.printer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.dizop.print.data.DiscoveredPrinter
import com.dizop.print.data.InkLevel
import com.dizop.print.data.PrinterBrand
import com.dizop.print.data.PrinterProtocol
import com.dizop.print.network.DualNetworkRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UniversalPrinterDiscovery(
    private val context: Context,
    private val networkRouter: DualNetworkRouter? = null
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val wifiDirectManager = com.dizop.print.wifidirect.WifiDirectPrinterManager(context)
    val usbPrinterManager = com.dizop.print.usb.UsbPrinterManager(context)

    private val _printers = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val printers: StateFlow<List<DiscoveredPrinter>> = _printers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var multicastLock: WifiManager.MulticastLock? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        // 1. Start Wi-Fi Direct (P2P) printer scanning
        try {
            wifiDirectManager.startWifiDirectDiscovery { directPrinter ->
                val list = _printers.value.toMutableList()
                val idx = list.indexOfFirst { it.id == directPrinter.id }
                if (idx >= 0) {
                    list[idx] = directPrinter
                } else {
                    list.add(directPrinter)
                }
                _printers.value = list
            }
        } catch (e: Exception) {
            Log.w("DizopDiscovery", "Wi-Fi Direct discovery init", e)
        }

        // 2. Scan for USB OTG connected printers
        try {
            usbPrinterManager.scanForUsbPrinters()
            usbPrinterManager.connectedUsbPrinter.value?.let { usbPrinter ->
                val list = _printers.value.toMutableList()
                val idx = list.indexOfFirst { it.id == usbPrinter.id }
                if (idx >= 0) {
                    list[idx] = usbPrinter
                } else {
                    list.add(0, usbPrinter)
                }
                _printers.value = list
            }
        } catch (e: Exception) {
            Log.w("DizopDiscovery", "USB scan error", e)
        }

        // Acquire Wi-Fi Multicast Lock so Android doesn't drop mDNS packets when SIM data is on
        try {
            multicastLock = wifiManager?.createMulticastLock("DizopMdnsLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            Log.w("DizopDiscovery", "Could not acquire MulticastLock", e)
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("DizopDiscovery", "Start discovery failed: $errorCode")
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.i("DizopDiscovery", "Discovery started for $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                _isScanning.value = false
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.i("DizopDiscovery", "Service found: ${serviceInfo.serviceName}")
                try {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                            Log.w("DizopDiscovery", "Resolve failed: $errorCode")
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            handleResolvedPrinter(resolvedInfo)
                        }
                    })
                } catch (e: Exception) {
                    Log.e("DizopDiscovery", "Resolve exception", e)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                val current = _printers.value.toMutableList()
                current.removeAll { it.name == serviceInfo.serviceName }
                _printers.value = current
            }
        }

        try {
            nsdManager.discoverServices("_ipp._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("DizopDiscovery", "Error starting IPP discovery", e)
            _isScanning.value = false
        }

        // Additional service types for legacy & alternate network printer protocols
        try {
            val rawListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(s: String?, err: Int) {}
                override fun onStopDiscoveryFailed(s: String?, err: Int) {}
                override fun onDiscoveryStarted(s: String?) {}
                override fun onDiscoveryStopped(s: String?) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    try {
                        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(i: NsdServiceInfo?, err: Int) {}
                            override fun onServiceResolved(res: NsdServiceInfo) {
                                handleResolvedPrinter(res)
                            }
                        })
                    } catch (_: Exception) {}
                }
                override fun onServiceLost(info: NsdServiceInfo) {}
            }
            nsdManager.discoverServices("_printer._tcp", NsdManager.PROTOCOL_DNS_SD, rawListener)
        } catch (_: Exception) {}

        try {
            val pdlListener = object : NsdManager.DiscoveryListener {
                override fun onStartDiscoveryFailed(s: String?, err: Int) {}
                override fun onStopDiscoveryFailed(s: String?, err: Int) {}
                override fun onDiscoveryStarted(s: String?) {}
                override fun onDiscoveryStopped(s: String?) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    try {
                        nsdManager.resolveService(info, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(i: NsdServiceInfo?, err: Int) {}
                            override fun onServiceResolved(res: NsdServiceInfo) {
                                handleResolvedPrinter(res)
                            }
                        })
                    } catch (_: Exception) {}
                }
                override fun onServiceLost(info: NsdServiceInfo) {}
            }
            nsdManager.discoverServices("_pdl-datastream._tcp", NsdManager.PROTOCOL_DNS_SD, pdlListener)
        } catch (_: Exception) {}
    }

    private fun handleResolvedPrinter(info: NsdServiceInfo) {
        val host = info.host?.hostAddress ?: return
        val port = info.port
        val name = info.serviceName
        val brand = detectBrand(name)

        // Parse dynamic mDNS TXT record attributes if present
        var resourcePath = "ipp/print"
        try {
            val attributes = info.attributes
            if (attributes != null && attributes.containsKey("rp")) {
                val rpBytes = attributes["rp"]
                if (rpBytes != null && rpBytes.isNotEmpty()) {
                    resourcePath = String(rpBytes, java.nio.charset.StandardCharsets.UTF_8).trimStart('/')
                }
            }
        } catch (_: Exception) {}

        val printer = DiscoveredPrinter(
            id = "$host:$port",
            name = name.replace("_", " "),
            model = name,
            hostAddress = host,
            port = port,
            resourcePath = resourcePath,
            brand = brand,
            protocol = if (port == 9100) PrinterProtocol.RAW_JETDIRECT else PrinterProtocol.IPP,
            isOnline = true,
            isReady = true,
            // Accurate telemetry: empty list means not reported / unknown (never fake)
            inkLevels = emptyList()
        )

        upsertPrinter(printer)
    }

    /**
     * Atomic, host-based upsert. NSD resolve callbacks arrive on binder threads and the same
     * physical printer is advertised under several services/ports (_ipp, _printer, _pdl-datastream),
     * so a read-modify-write on id ("host:port") produced duplicate cards for one printer.
     * An IPP entry is preferred over a raw 9100 entry for the same host unless [forceReplace] is set.
     */
    private fun upsertPrinter(printer: DiscoveredPrinter, atFront: Boolean = false, forceReplace: Boolean = false) {
        _printers.update { current ->
            val idx = current.indexOfFirst {
                it.id == printer.id ||
                    (it.hostAddress.isNotBlank() && it.hostAddress == printer.hostAddress)
            }
            if (idx >= 0) {
                val existing = current[idx]
                val keepExisting = !forceReplace &&
                    existing.protocol == PrinterProtocol.IPP &&
                    printer.protocol != PrinterProtocol.IPP
                val merged = if (keepExisting) existing else printer
                current.toMutableList().also { it[idx] = merged }
            } else if (atFront) {
                listOf(printer) + current
            } else {
                current + printer
            }
        }
    }

    private fun detectBrand(name: String): PrinterBrand {
        val lower = name.lowercase()
        return when {
            lower.contains("hp") || lower.contains("deskjet") || lower.contains("laserjet") || lower.contains("envy") || lower.contains("smart tank") -> PrinterBrand.HP
            lower.contains("canon") || lower.contains("pixma") || lower.contains("imageclass") -> PrinterBrand.CANON
            lower.contains("epson") || lower.contains("ecotank") || lower.contains("workforce") -> PrinterBrand.EPSON
            lower.contains("brother") || lower.contains("hl-") || lower.contains("mfc-") -> PrinterBrand.BROTHER
            lower.contains("xerox") -> PrinterBrand.XEROX
            lower.contains("ricoh") -> PrinterBrand.RICOH
            else -> PrinterBrand.GENERIC
        }
    }

    fun addPrinterManually(
        host: String,
        port: Int = 631,
        name: String = "Manual IP Printer",
        resourcePath: String = "ipp/print",
        protocol: PrinterProtocol = PrinterProtocol.IPP
    ) {
        val printer = DiscoveredPrinter(
            id = "$host:$port",
            name = name,
            model = "Direct IP Printer ($host)",
            hostAddress = host,
            port = port,
            resourcePath = resourcePath.trimStart('/'),
            brand = detectBrand(name),
            protocol = protocol,
            isOnline = true,
            isReady = true,
            inkLevels = emptyList()
        )
        upsertPrinter(printer, atFront = true, forceReplace = true)
    }

    fun stopDiscovery() {
        if (!_isScanning.value) return
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e("DizopDiscovery", "Stop error", e)
        } finally {
            _isScanning.value = false
            wifiDirectManager.stopDiscovery()
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        }
    }
}
