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
    private val activeListeners = java.util.concurrent.CopyOnWriteArrayList<NsdManager.DiscoveryListener>()

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

        // One DiscoveryListener per DNS-SD service type. All resolves go through a single
        // serialized queue, because overlapping NsdManager.resolveService() calls fail with
        // FAILURE_ALREADY_ACTIVE and the printer is then silently never resolved.
        val serviceTypes = listOf("_ipp._tcp", "_printer._tcp", "_pdl-datastream._tcp")
        serviceTypes.forEach { type ->
            val listener = createListener(type)
            activeListeners.add(listener)
            try {
                nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (e: Exception) {
                Log.e("DizopDiscovery", "Error starting discovery for $type", e)
                activeListeners.remove(listener)
            }
        }
        if (activeListeners.isEmpty()) _isScanning.value = false
    }

    private fun createListener(serviceType: String) = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(type: String?, errorCode: Int) {
            Log.e("DizopDiscovery", "Start discovery failed for $serviceType: $errorCode")
            activeListeners.remove(this)
            if (activeListeners.isEmpty()) _isScanning.value = false
        }

        override fun onStopDiscoveryFailed(type: String?, errorCode: Int) {
            activeListeners.remove(this)
        }

        override fun onDiscoveryStarted(type: String?) {
            Log.i("DizopDiscovery", "Discovery started for $type")
        }

        override fun onDiscoveryStopped(type: String?) {
            activeListeners.remove(this)
            if (activeListeners.isEmpty()) _isScanning.value = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.i("DizopDiscovery", "Service found: ${serviceInfo.serviceName} ($serviceType)")
            enqueueResolve(serviceInfo, serviceType)
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            // Printers are stored with the raw mDNS service name in `model`
            // (`name` has underscores replaced), so match on that.
            _printers.update { list -> list.filterNot { it.model == serviceInfo.serviceName } }
        }
    }

    private val resolveQueue = java.util.ArrayDeque<Pair<NsdServiceInfo, String>>()
    private var resolving = false

    @Synchronized
    private fun enqueueResolve(info: NsdServiceInfo, serviceType: String) {
        resolveQueue.add(info to serviceType)
        if (!resolving) resolveNext()
    }

    @Synchronized
    private fun resolveNext() {
        val queued = resolveQueue.poll()
        if (queued == null) {
            resolving = false
            return
        }
        val next = queued.first
        val serviceType = queued.second
        resolving = true
        try {
            nsdManager.resolveService(next, object : NsdManager.ResolveListener {
                override fun onResolveFailed(info: NsdServiceInfo?, errorCode: Int) {
                    Log.w("DizopDiscovery", "Resolve failed: $errorCode")
                    finishResolve()
                }

                override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                    try {
                        handleResolvedPrinter(resolvedInfo, serviceType)
                    } finally {
                        finishResolve()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("DizopDiscovery", "Resolve exception", e)
            finishResolve()
        }
    }

    @Synchronized
    private fun finishResolve() {
        resolving = false
        resolveNext()
    }

    private val ippProbeClient = com.dizop.print.printer.protocol.ipp.IppClient()

    /**
     * The advertised port says nothing about the protocol: _printer._tcp is LPD (port 515),
     * _pdl-datastream._tcp is raw JetDirect (9100) and only _ipp._tcp is IPP. Treating every
     * non-9100 port as IPP sent IPP/HTTP POSTs to the LPD port ("unexpected end of stream").
     */
    private fun handleResolvedPrinter(info: NsdServiceInfo, serviceType: String) {
        val host = info.host?.hostAddress ?: return
        val name = info.serviceName
        val brand = detectBrand(name)

        when (serviceType) {
            "_ipp._tcp" -> {
                // Parse dynamic mDNS TXT record attributes if present
                var resourcePath = "ipp/print"
                try {
                    val rpBytes = info.attributes?.get("rp")
                    if (rpBytes != null && rpBytes.isNotEmpty()) {
                        resourcePath = String(rpBytes, java.nio.charset.StandardCharsets.UTF_8).trimStart('/')
                    }
                } catch (_: Exception) {}
                upsertPrinter(
                    buildPrinter(host, info.port, name, brand, PrinterProtocol.IPP, resourcePath)
                )
            }
            "_pdl-datastream._tcp" -> {
                // Raw JetDirect is only a fallback; an IPP entry for the same host always wins
                upsertPrinter(
                    buildPrinter(host, info.port, name, brand, PrinterProtocol.RAW_JETDIRECT, "")
                )
                probeIpp(host, name, brand)
            }
            else -> {
                // _printer._tcp = LPD/LPR, which this app cannot speak. Most such printers also
                // accept IPP on 631, so probe for it instead of listing an unusable LPD port.
                probeIpp(host, name, brand)
            }
        }
    }

    private fun buildPrinter(
        host: String,
        port: Int,
        name: String,
        brand: PrinterBrand,
        protocol: PrinterProtocol,
        resourcePath: String
    ) = DiscoveredPrinter(
        id = "$host:$port",
        name = name.replace("_", " "),
        model = name,
        hostAddress = host,
        port = port,
        resourcePath = resourcePath,
        brand = brand,
        protocol = protocol,
        isOnline = true,
        isReady = true,
        // Accurate telemetry: empty list means not reported / unknown (never fake)
        inkLevels = emptyList()
    )

    /** Verifies IPP on the standard port 631 off the resolve thread; adds the printer only on a real reply. */
    private fun probeIpp(host: String, name: String, brand: PrinterBrand) {
        Thread {
            try {
                val reply = ippProbeClient.queryPrinterAttributes(
                    printerHost = host,
                    printerPort = 631,
                    resourcePath = "ipp/print",
                    network = networkRouter?.printerWifiNetwork?.value
                )
                if (reply.status == com.dizop.print.printer.protocol.ipp.IppResponseStatus.SUCCESS) {
                    // Never replace an IPP entry that mDNS already advertised (it carries the right port/rp)
                    upsertPrinter(
                        buildPrinter(host, 631, name, brand, PrinterProtocol.IPP, "ipp/print"),
                        replaceSameRank = false
                    )
                }
            } catch (e: Exception) {
                Log.d("DizopDiscovery", "IPP probe failed for $host: ${e.message}")
            }
        }.start()
    }

    private fun rank(p: PrinterProtocol) = when (p) {
        PrinterProtocol.IPP -> 2
        PrinterProtocol.RAW_JETDIRECT -> 1
        else -> 0
    }

    /**
     * Atomic, host-based upsert. NSD resolve callbacks arrive on binder threads and the same
     * physical printer is advertised under several services/ports (_ipp, _printer, _pdl-datastream),
     * so a read-modify-write on id ("host:port") produced duplicate cards for one printer.
     * Protocol priority per host: IPP over raw 9100, unless [forceReplace] is set.
     */
    private fun upsertPrinter(
        printer: DiscoveredPrinter,
        atFront: Boolean = false,
        forceReplace: Boolean = false,
        replaceSameRank: Boolean = true
    ) {
        _printers.update { current ->
            val idx = current.indexOfFirst {
                it.id == printer.id ||
                    (it.hostAddress.isNotBlank() && it.hostAddress == printer.hostAddress)
            }
            if (idx >= 0) {
                val existing = current[idx]
                val keepExisting = !forceReplace && (
                    rank(existing.protocol) > rank(printer.protocol) ||
                        (rank(existing.protocol) == rank(printer.protocol) && !replaceSameRank)
                    )
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
            activeListeners.toList().forEach { l ->
                try {
                    nsdManager.stopServiceDiscovery(l)
                } catch (e: Exception) {
                    Log.e("DizopDiscovery", "Stop error", e)
                }
            }
            activeListeners.clear()
        } finally {
            synchronized(this) {
                resolveQueue.clear()
                resolving = false
            }
            _isScanning.value = false
            wifiDirectManager.stopDiscovery()
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        }
    }
}
