package com.dizop.print.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.dizop.print.data.DiscoveredPrinter
import com.dizop.print.data.InkLevel
import com.dizop.print.data.PrinterBrand
import com.dizop.print.data.PrinterProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class UsbPrinterManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ACTION_USB_PERMISSION = "com.dizop.print.USB_PERMISSION"

    private val _connectedUsbPrinter = MutableStateFlow<DiscoveredPrinter?>(null)
    val connectedUsbPrinter: StateFlow<DiscoveredPrinter?> = _connectedUsbPrinter.asStateFlow()

    private var activeDevice: UsbDevice? = null

    fun scanForUsbPrinters() {
        val deviceList = usbManager.deviceList
        for ((_, device) in deviceList) {
            if (isPrinterDevice(device)) {
                activeDevice = device
                val brand = detectBrand(device.productName ?: "")
                val printer = DiscoveredPrinter(
                    id = "usb_${device.deviceId}",
                    name = "🔌 USB: ${device.productName ?: "USB Printer"}",
                    model = device.productName ?: "Generic USB Printer",
                    hostAddress = "USB_DIRECT",
                    port = 0,
                    brand = brand,
                    protocol = PrinterProtocol.RAW_JETDIRECT,
                    isOnline = true,
                    isReady = true,
                    inkLevels = emptyList() // Truthful telemetry: not reported over basic USB bulk
                )
                _connectedUsbPrinter.value = printer
                return
            }
        }
        _connectedUsbPrinter.value = null
    }

    private fun isPrinterDevice(device: UsbDevice): Boolean {
        // USB Class 7 is Printer (UsbConstants.USB_CLASS_PRINTER = 7)
        if (device.deviceClass == 7) return true
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            if (intf.interfaceClass == 7) return true
        }
        val name = (device.productName ?: "").lowercase()
        return name.contains("printer") || name.contains("laserjet") || name.contains("deskjet") || name.contains("canon") || name.contains("epson")
    }

    private fun detectBrand(name: String): PrinterBrand {
        val lower = name.lowercase()
        return when {
            lower.contains("hp") || lower.contains("laserjet") || lower.contains("deskjet") -> PrinterBrand.HP
            lower.contains("canon") || lower.contains("lbp") -> PrinterBrand.CANON
            lower.contains("epson") -> PrinterBrand.EPSON
            lower.contains("brother") -> PrinterBrand.BROTHER
            else -> PrinterBrand.GENERIC
        }
    }

    suspend fun printOverUsb(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        val device = activeDevice ?: return@withContext Result.failure(Exception("No USB printer connected"))
        if (!usbManager.hasPermission(device)) {
            requestUsbPermission(device)
            return@withContext Result.failure(Exception("USB Permission required. Please grant permission and retry."))
        }

        try {
            var printInterface: UsbInterface? = null
            var outEndpoint: UsbEndpoint? = null

            for (i in 0 until device.interfaceCount) {
                val intf = device.getInterface(i)
                if (intf.interfaceClass == 7 || intf.endpointCount > 0) {
                    for (e in 0 until intf.endpointCount) {
                        val ep = intf.getEndpoint(e)
                        if (ep.direction == UsbConstants.USB_DIR_OUT) {
                            printInterface = intf
                            outEndpoint = ep
                            break
                        }
                    }
                }
                if (outEndpoint != null) break
            }

            if (printInterface == null || outEndpoint == null) {
                return@withContext Result.failure(Exception("No writeable endpoint found on USB printer"))
            }

            val connection: UsbDeviceConnection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("Could not open USB connection"))

            connection.claimInterface(printInterface, true)

            FileInputStream(file).use { input ->
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val transferred = connection.bulkTransfer(outEndpoint, buffer, bytesRead, 5000)
                    if (transferred < 0) {
                        connection.releaseInterface(printInterface)
                        connection.close()
                        return@withContext Result.failure(Exception("USB bulkTransfer failed with return code $transferred"))
                    } else if (transferred < bytesRead) {
                        // Partial transfer handling
                        val remaining = bytesRead - transferred
                        val retryTransfer = connection.bulkTransfer(outEndpoint, buffer.copyOfRange(transferred, bytesRead), remaining, 5000)
                        if (retryTransfer < 0) {
                            connection.releaseInterface(printInterface)
                            connection.close()
                            return@withContext Result.failure(Exception("USB partial transfer retry failed"))
                        }
                    }
                }
            }

            connection.releaseInterface(printInterface)
            connection.close()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("DizopUsb", "USB print error", e)
            Result.failure(e)
        }
    }

    private fun requestUsbPermission(device: UsbDevice) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            flags
        )
        usbManager.requestPermission(device, permissionIntent)
    }
}
