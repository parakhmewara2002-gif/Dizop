package com.dizop.print.printer

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Log
import com.dizop.print.data.DiscoveredPrinter
import com.dizop.print.data.PrinterProtocol
import com.dizop.print.printer.config.PrintJobConfiguration
import com.dizop.print.network.DualNetworkRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.InetSocketAddress

class UniversalPrintEngine(
    private val context: Context,
    private val networkRouter: DualNetworkRouter? = null
) {

    private val ippClient = com.dizop.print.printer.protocol.ipp.IppClient()

    /**
     * Universal print dispatcher:
     * Dispatches either via Direct TCP Socket (Raw JetDirect Port 9100),
     * direct RFC 8011 IPP 2.0 binary envelope (Port 631), USB Host transfer, or Android System Spooler.
     * Uses DualNetworkRouter to ensure Wi-Fi printer sockets are never misrouted to SIM Mobile Data.
     */
    suspend fun printDocument(
        printer: DiscoveredPrinter?,
        file: File,
        jobName: String = "Dizop Document",
        config: PrintJobConfiguration? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (printer != null && printer.id.startsWith("usb_")) {
                // USB OTG Print
                val usbManager = com.dizop.print.usb.UsbPrinterManager(context)
                usbManager.scanForUsbPrinters()
                return@withContext usbManager.printOverUsb(file)
            } else if (printer != null && printer.protocol == PrinterProtocol.RAW_JETDIRECT) {
                sendRawStream(printer.hostAddress, printer.port, file)
                Result.success(true)
            } else if (printer != null && printer.protocol == PrinterProtocol.IPP) {
                // Real RFC 8011 IPP 2.0 execution with dynamic endpoint and print options
                val port = if (printer.port > 0) printer.port else 631
                val resourcePath = printer.resourcePath.ifBlank { "ipp/print" }
                val ippResult = ippClient.printDocument(
                    printerHost = printer.hostAddress,
                    printerPort = port,
                    resourcePath = resourcePath,
                    pdfFile = file,
                    jobName = jobName,
                    config = config,
                    network = networkRouter?.printerWifiNetwork?.value
                )
                // STRICT P0 FIX: Never automatically blast the file to raw port 9100 on IPP failure!
                // Report explicit failure back to caller to prevent duplicate pages.
                return@withContext ippResult
            } else {
                withContext(Dispatchers.Main) {
                    launchSystemPrintJob(file, jobName, config)
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("DizopPrintEngine", "Print error", e)
            Result.failure(e)
        }
    }

    private fun sendRawStream(host: String, port: Int, file: File) {
        // Explicitly bind socket to Wi-Fi so SIM data does not intercept it
        val socket = networkRouter?.createBoundSocket() ?: java.net.Socket()
        socket.use { s ->
            s.connect(InetSocketAddress(host, port), 5000)
            s.soTimeout = 10000
            val out: OutputStream = s.getOutputStream()
            FileInputStream(file).use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }
            out.flush()
        }
    }

    private fun launchSystemPrintJob(file: File, jobName: String, config: PrintJobConfiguration? = null) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = PdfPrintDocumentAdapter(file, jobName)
        val mediaSize = when (config?.paperSize) {
            com.dizop.print.printer.config.PaperSize.LETTER -> PrintAttributes.MediaSize.NA_LETTER
            com.dizop.print.printer.config.PaperSize.LEGAL -> PrintAttributes.MediaSize.NA_LEGAL
            com.dizop.print.printer.config.PaperSize.A3 -> PrintAttributes.MediaSize.ISO_A3
            com.dizop.print.printer.config.PaperSize.A5 -> PrintAttributes.MediaSize.ISO_A5
            else -> PrintAttributes.MediaSize.ISO_A4
        }
        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setColorMode(if (config?.colorMode == com.dizop.print.printer.config.ColorMode.MONOCHROME) PrintAttributes.COLOR_MODE_MONOCHROME else PrintAttributes.COLOR_MODE_COLOR)
            .build()
        printManager.print(jobName, printAdapter, printAttributes)
    }
}
