package com.dizop.print

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dizop.print.network.DualNetworkRouter
import com.dizop.print.printer.UniversalPrintEngine
import com.dizop.print.printer.UniversalPrinterDiscovery
import com.dizop.print.ui.screens.MainDashboardScreen
import com.dizop.print.ui.theme.DizopTheme
import com.dizop.print.update.InAppUpdateManager

class MainActivity : ComponentActivity() {

    private lateinit var dualNetworkRouter: DualNetworkRouter
    private lateinit var printerDiscovery: UniversalPrinterDiscovery
    private lateinit var printEngine: UniversalPrintEngine
    private lateinit var updateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Dual Network Router to prevent SIM Mobile Data from blocking Printer Wi-Fi
        dualNetworkRouter = DualNetworkRouter(this).apply {
            bindToLocalPrinterNetwork()
        }

        printerDiscovery = UniversalPrinterDiscovery(this, dualNetworkRouter)
        printEngine = UniversalPrintEngine(this, dualNetworkRouter)
        updateManager = InAppUpdateManager(this)

        // Instant discovery start
        printerDiscovery.startDiscovery()

        setContent {
            DizopTheme {
                MainDashboardScreen(
                    discovery = printerDiscovery,
                    printEngine = printEngine,
                    updateManager = updateManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        printerDiscovery.stopDiscovery()
        dualNetworkRouter.release()
    }
}
