package com.dizop.print.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Merge
import com.dizop.print.pdf.SmartPdfTools
import com.dizop.print.ui.components.DigitalSignatureSheet
import com.dizop.print.ui.components.PrintPreviewSheet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dizop.print.data.DiscoveredPrinter
import com.dizop.print.printer.UniversalPrintEngine
import com.dizop.print.printer.UniversalPrinterDiscovery
import com.dizop.print.scanner.SmartDocumentProcessor
import com.dizop.print.ui.components.DynamicIslandPill
import com.dizop.print.ui.components.IosCard
import com.dizop.print.ui.components.IosSegmentedControl
import com.dizop.print.ui.components.PrintActivityState
import com.dizop.print.ui.components.PrinterCard
import com.dizop.print.ui.theme.IosBackgroundLight
import com.dizop.print.ui.theme.IosBlue
import com.dizop.print.ui.theme.IosGreen
import com.dizop.print.ui.theme.IosIndigo
import com.dizop.print.ui.theme.IosOrange
import com.dizop.print.ui.theme.IosPurple
import com.dizop.print.ui.theme.IosRed
import com.dizop.print.ui.theme.IosTeal
import com.dizop.print.ui.theme.IosTextPrimary
import com.dizop.print.ui.theme.IosTextSecondary
import com.dizop.print.update.InAppUpdateManager
import com.dizop.print.util.HapticFeedbackHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun MainDashboardScreen(
    discovery: UniversalPrinterDiscovery,
    printEngine: UniversalPrintEngine,
    updateManager: InAppUpdateManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val printers by discovery.printers.collectAsState()
    val isScanning by discovery.isScanning.collectAsState()
    var selectedPrinter by remember { mutableStateOf<DiscoveredPrinter?>(null) }

    // Print Options
    var printColorMode by remember { mutableStateOf(0) } // 0: Color, 1: Black & White
    var dynamicIslandState by remember { mutableStateOf(PrintActivityState()) }

    // ID Card Scan State
    var idCardFrontBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isWaitingForIdBack by remember { mutableStateOf(false) }
    var showIdCardSourceDialog by remember { mutableStateOf(false) }

    // Digital Signature Sheet State
    var showSignatureDialog by remember { mutableStateOf(false) }
    var pendingPdfToSign by remember { mutableStateOf<File?>(null) }

    // Print Preview & Options Sheet State
    var showPreviewDialog by remember { mutableStateOf(false) }
    var pendingPdfToPrint by remember { mutableStateOf<File?>(null) }
    var pendingJobTitle by remember { mutableStateOf("Dizop Document") }

    // Manual Printer Dialog State
    var showManualPrinterDialog by remember { mutableStateOf(false) }
    var manualHostInput by remember { mutableStateOf("") }
    var manualPortInput by remember { mutableStateOf("631") }
    var manualNameInput by remember { mutableStateOf("") }

    // Web Page Print State
    var showWebPrintDialog by remember { mutableStateOf(false) }
    var webUrlInput by remember { mutableStateOf("https://") }

    val updateInfo by updateManager.updateState.collectAsState()
    val isDownloadingUpdate by updateManager.isDownloading.collectAsState()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            updateManager.checkForUpdates()
        }
    }

    fun triggerPrint(file: File, title: String) {
        pendingPdfToPrint = file
        pendingJobTitle = title
        showPreviewDialog = true
    }

    fun executePrintJob(file: File, title: String, config: com.dizop.print.printer.config.PrintJobConfiguration? = null) {
        coroutineScope.launch {
            HapticFeedbackHelper.performClick(context)
            dynamicIslandState = PrintActivityState(
                isPrinting = true,
                documentTitle = title,
                printerName = selectedPrinter?.name ?: "All Printers"
            )

            // Apply N-Up, Poster, Margin & Duplex Page Layout transformations if necessary
            val fileToPrint = if (config != null && (
                config.pagesPerSheet != com.dizop.print.printer.config.PagesPerSheet.ONE ||
                config.posterMode != com.dizop.print.printer.config.PosterMode.NONE ||
                config.duplexMode == com.dizop.print.printer.config.DuplexMode.MANUAL_ODD_ONLY ||
                config.duplexMode == com.dizop.print.printer.config.DuplexMode.MANUAL_EVEN_ONLY ||
                config.margins != com.dizop.print.printer.config.PageMargins.NORMAL
            )) {
                val transformedPdf = File(context.cacheDir, "dizop_transformed_layout_${System.currentTimeMillis()}.pdf")
                val success = com.dizop.print.printer.layout.PageLayoutProcessor.processPdfLayout(file, transformedPdf, config)
                if (success) transformedPdf else file
            } else {
                file
            }

            val result = printEngine.printDocument(selectedPrinter, fileToPrint, title, config)
            if (result.isSuccess) {
                HapticFeedbackHelper.performSuccess(context)
                dynamicIslandState = PrintActivityState(isComplete = true)
                delay(2400)
                dynamicIslandState = PrintActivityState()
            } else {
                dynamicIslandState = PrintActivityState()
                Toast.makeText(context, "Print error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Photo Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val processed = SmartDocumentProcessor.processDocumentScan(
                        bitmap,
                        highContrast = (printColorMode == 1),
                        removeShadows = true
                    )
                    val tempPdf = File(context.cacheDir, "dizop_photo.pdf")
                    SmartDocumentProcessor.createPdfFromBitmaps(listOf(processed), tempPdf)
                    triggerPrint(tempPdf, "HD Photo")
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // PDF Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val tempPdf = File(context.cacheDir, "dizop_document.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempPdf).use { output ->
                            input.copyTo(output)
                        }
                    }
                    pendingPdfToSign = tempPdf
                    triggerPrint(tempPdf, "PDF Document")
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // PDF Compressor Launcher
    val pdfCompressLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val original = File(context.cacheDir, "to_compress.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(original).use { output -> input.copyTo(output) }
                    }
                    val compressed = File(context.cacheDir, "compressed_doc.pdf")
                    SmartPdfTools.compressPdf(original, compressed)
                    HapticFeedbackHelper.performSuccess(context)
                    val oldSize = original.length() / 1024
                    val newSize = compressed.length() / 1024
                    Toast.makeText(context, "Compressed: ${oldSize}KB ➔ ${newSize}KB!", Toast.LENGTH_LONG).show()
                    triggerPrint(compressed, "Compressed PDF")
                } catch (e: Exception) {
                    Toast.makeText(context, "Compression failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // PDF Sign Launcher
    val pdfSignPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val doc = File(context.cacheDir, "to_sign.pdf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(doc).use { output -> input.copyTo(output) }
                    }
                    pendingPdfToSign = doc
                    showSignatureDialog = true
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not open: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Smart Camera Scan Launcher
    val cameraScanLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                try {
                    val scannedDoc = SmartDocumentProcessor.processDocumentScan(
                        bitmap,
                        highContrast = true,
                        removeShadows = true
                    )
                    val tempPdf = File(context.cacheDir, "smart_scan.pdf")
                    SmartDocumentProcessor.createPdfFromBitmaps(listOf(scannedDoc), tempPdf)
                    triggerPrint(tempPdf, "AI Smart Scan")
                } catch (e: Exception) {
                    Toast.makeText(context, "Scan error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ID Card Gallery Pick Launcher
    val idCardGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        if (idCardFrontBitmap == null) {
                            idCardFrontBitmap = bitmap
                            isWaitingForIdBack = true
                            HapticFeedbackHelper.performClick(context)
                            Toast.makeText(context, "Front side loaded from gallery! Now select or scan Back side.", Toast.LENGTH_LONG).show()
                        } else {
                            isWaitingForIdBack = false
                            val front = idCardFrontBitmap!!
                            idCardFrontBitmap = null

                            val a4Sheet = SmartDocumentProcessor.createIdCardA4Sheet(front, bitmap)
                            val tempPdf = File(context.cacheDir, "id_card_2in1.pdf")
                            SmartDocumentProcessor.createPdfFromBitmaps(listOf(a4Sheet), tempPdf)
                            triggerPrint(tempPdf, "2-in-1 ID Card")
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gallery error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ID Card Front & Back Launcher
    val idCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            coroutineScope.launch {
                if (idCardFrontBitmap == null) {
                    // Front Captured
                    idCardFrontBitmap = bitmap
                    isWaitingForIdBack = true
                    HapticFeedbackHelper.performClick(context)
                    Toast.makeText(context, "Front side captured! Now flip and scan Back side.", Toast.LENGTH_LONG).show()
                } else {
                    // Back Captured -> Merge both onto A4
                    isWaitingForIdBack = false
                    val front = idCardFrontBitmap!!
                    idCardFrontBitmap = null
                    
                    val a4Sheet = SmartDocumentProcessor.createIdCardA4Sheet(front, bitmap)
                    val tempPdf = File(context.cacheDir, "id_card_2in1.pdf")
                    SmartDocumentProcessor.createPdfFromBitmaps(listOf(a4Sheet), tempPdf)
                    triggerPrint(tempPdf, "2-in-1 ID Card")
                }
            }
        }
    }

    Scaffold(
        containerColor = IosBackgroundLight,
        topBar = {
            DynamicIslandPill(state = dynamicIslandState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // iOS Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dizop",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = IosTextPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "All Printers • Direct AI Scan • No Login",
                            fontSize = 13.sp,
                            color = IosTextSecondary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                HapticFeedbackHelper.performSelection(context)
                                if (isScanning) discovery.stopDiscovery() else discovery.startDiscovery()
                            }
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = IosBlue
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh",
                                    tint = IosBlue
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                HapticFeedbackHelper.performSelection(context)
                                coroutineScope.launch {
                                    val update = updateManager.checkForUpdates()
                                    if (update == null || !update.hasUpdate) {
                                        Toast.makeText(context, "Dizop is up to date!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "Check Update",
                                tint = IosBlue
                            )
                        }
                    }
                }
            }

            // Print Output Mode Selector (Color vs B&W)
            item {
                IosSegmentedControl(
                    items = listOf("Full HD Color", "Monochrome (B&W)"),
                    selectedIndex = printColorMode,
                    onItemSelected = { index ->
                        HapticFeedbackHelper.performSelection(context)
                        printColorMode = index
                    }
                )
            }

            // Print Preview & Configuration Modal
            if (showPreviewDialog && pendingPdfToPrint != null) {
                item {
                    PrintPreviewSheet(
                        pdfFile = pendingPdfToPrint!!,
                        onConfirmPrint = { chosenConfig ->
                            showPreviewDialog = false
                            executePrintJob(pendingPdfToPrint!!, pendingJobTitle, chosenConfig)
                        },
                        onDismiss = {
                            showPreviewDialog = false
                        }
                    )
                }
            }

            // Manual Printer Addition Sheet
            if (showManualPrinterDialog) {
                item {
                    IosCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Add Printer by IP",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosTextPrimary
                                )
                                IconButton(onClick = { showManualPrinterDialog = false }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = manualHostInput,
                                onValueChange = { manualHostInput = it },
                                label = { Text("IP Address or Hostname") },
                                placeholder = { Text("192.168.1.100") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = manualPortInput,
                                    onValueChange = { manualPortInput = it },
                                    label = { Text("Port (631 IPP / 9100 RAW)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                androidx.compose.material3.OutlinedTextField(
                                    value = manualNameInput,
                                    onValueChange = { manualNameInput = it },
                                    label = { Text("Printer Name") },
                                    placeholder = { Text("Office HP Laser") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val host = manualHostInput.trim()
                                    if (host.isNotEmpty()) {
                                        val port = manualPortInput.trim().toIntOrNull() ?: 631
                                        val name = manualNameInput.trim().ifEmpty { "Printer ($host)" }
                                        discovery.addPrinterManually(
                                            host = host,
                                            port = port,
                                            name = name,
                                            protocol = if (port == 9100) com.dizop.print.data.PrinterProtocol.RAW_JETDIRECT else com.dizop.print.data.PrinterProtocol.IPP
                                        )
                                        showManualPrinterDialog = false
                                        manualHostInput = ""
                                        Toast.makeText(context, "Added printer: $name", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
                            ) {
                                Text("Connect to Printer", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Web Page Print URL Dialog
            if (showWebPrintDialog) {
                item {
                    IosCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Print Web Page",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IosTextPrimary
                                )
                                IconButton(onClick = { showWebPrintDialog = false }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.OutlinedTextField(
                                value = webUrlInput,
                                onValueChange = { webUrlInput = it },
                                label = { Text("Web URL") },
                                placeholder = { Text("https://en.wikipedia.org") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val url = webUrlInput.trim()
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        showWebPrintDialog = false
                                        val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as android.print.PrintManager
                                        val webView = android.webkit.WebView(context)
                                        webView.webViewClient = object : android.webkit.WebViewClient() {
                                            override fun onPageFinished(view: android.webkit.WebView?, u: String?) {
                                                val printAdapter = webView.createPrintDocumentAdapter("Web Print")
                                                printManager.print("Web Document", printAdapter, android.print.PrintAttributes.Builder().build())
                                            }
                                        }
                                        webView.loadUrl(url)
                                        Toast.makeText(context, "Loading page for print...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Invalid URL (must start with http:// or https://)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IosTeal)
                            ) {
                                Text("Generate Web Print Preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Digital Signature Modal
            if (showSignatureDialog && pendingPdfToSign != null) {
                item {
                    DigitalSignatureSheet(
                        onSignatureCaptured = { signatureBitmap ->
                            showSignatureDialog = false
                            coroutineScope.launch {
                                val signedOutput = File(context.cacheDir, "signed_document.pdf")
                                SmartPdfTools.applySignatureToPdf(pendingPdfToSign!!, signatureBitmap, signedOutput)
                                HapticFeedbackHelper.performSuccess(context)
                                Toast.makeText(context, "Signature applied to PDF!", Toast.LENGTH_SHORT).show()
                                triggerPrint(signedOutput, "Signed Document")
                            }
                        },
                        onCancel = {
                            showSignatureDialog = false
                        }
                    )
                }
            }

            // ID Card Scan Status Banner
            if (isWaitingForIdBack) {
                item {
                    IosCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = IosGreen.copy(alpha = 0.12f),
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Badge,
                                    contentDescription = null,
                                    tint = IosGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Front Side Saved",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = IosTextPrimary
                                    )
                                    Text(
                                        text = "Choose how to add the Back Side (Camera or Gallery)",
                                        fontSize = 12.sp,
                                        color = IosTextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(IosGreen)
                                        .clickable {
                                            HapticFeedbackHelper.performClick(context)
                                            idCardLauncher.launch(null)
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📷 Camera Scan", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(IosBlue)
                                        .clickable {
                                            HapticFeedbackHelper.performClick(context)
                                            idCardGalleryLauncher.launch("image/*")
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🖼️ From Gallery", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // ID Card Source Selection Sheet (Camera or Gallery)
            if (showIdCardSourceDialog) {
                item {
                    IosCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "2-in-1 ID Card: Choose Front Side",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = IosTextPrimary
                                )
                                Text(
                                    text = "Cancel",
                                    color = IosBlue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { showIdCardSourceDialog = false }
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(IosGreen)
                                        .clickable {
                                            showIdCardSourceDialog = false
                                            idCardFrontBitmap = null
                                            HapticFeedbackHelper.performClick(context)
                                            idCardLauncher.launch(null)
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📷 Camera Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(IosBlue)
                                        .clickable {
                                            showIdCardSourceDialog = false
                                            idCardFrontBitmap = null
                                            HapticFeedbackHelper.performClick(context)
                                            idCardGalleryLauncher.launch("image/*")
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🖼️ Select Gallery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // In-App Update Banner
            if (updateInfo?.hasUpdate == true) {
                item {
                    IosCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = IosBlue.copy(alpha = 0.08f),
                        cornerRadius = 18.dp,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            updateManager.startUpdateDownload(
                                apkUrl = updateInfo!!.downloadUrl,
                                expectedSha256 = updateInfo!!.expectedSha256
                            )
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SystemUpdate,
                                contentDescription = "Update",
                                tint = IosBlue,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Update Available (v${updateInfo!!.latestVersionName})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = IosTextPrimary
                                )
                                Text(
                                    text = updateInfo!!.releaseNotes,
                                    fontSize = 12.sp,
                                    color = IosTextSecondary
                                )
                            }
                            if (isDownloadingUpdate) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = IosBlue)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(IosBlue)
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Install", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Action Quick-Grid
            item {
                Text(
                    text = "ACTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosTextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Print Photos",
                        subtitle = "High-Res HD",
                        icon = Icons.Rounded.Image,
                        iconBackground = IosIndigo,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            photoPickerLauncher.launch("image/*")
                        }
                    )

                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Documents",
                        subtitle = "PDF & Office",
                        icon = Icons.Rounded.PictureAsPdf,
                        iconBackground = IosOrange,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            pdfPickerLauncher.launch("application/pdf")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Smart Scan",
                        subtitle = "Auto White & Shadow Filter",
                        icon = Icons.Rounded.DocumentScanner,
                        iconBackground = IosTeal,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            cameraScanLauncher.launch(null)
                        }
                    )

                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "ID Card 2-in-1",
                        subtitle = "Front + Back on A4",
                        icon = Icons.Rounded.CreditCard,
                        iconBackground = IosGreen,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            showIdCardSourceDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Sign & Stamp",
                        subtitle = "Digital Pen on PDF",
                        icon = Icons.Rounded.Draw,
                        iconBackground = IosBlue,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            pdfSignPickerLauncher.launch("application/pdf")
                        }
                    )

                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Compress PDF",
                        subtitle = "Shrink File Size",
                        icon = Icons.Rounded.Compress,
                        iconBackground = IosOrange,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            pdfCompressLauncher.launch("application/pdf")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Print Web Page",
                        subtitle = "URL / HTML to Paper",
                        icon = Icons.Rounded.Language,
                        iconBackground = IosTeal,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            showWebPrintDialog = true
                        }
                    )

                    ActionTile(
                        modifier = Modifier.weight(1f),
                        title = "Add by IP Address",
                        subtitle = "Manual Network Setup",
                        icon = Icons.Rounded.Add,
                        iconBackground = IosPurple,
                        onClick = {
                            HapticFeedbackHelper.performClick(context)
                            showManualPrinterDialog = true
                        }
                    )
                }
            }

            // Discovered Printers Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE PRINTERS (${printers.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = IosTextSecondary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    if (isScanning) {
                        Text(
                            text = "Scanning network & USB...",
                            fontSize = 11.sp,
                            color = IosBlue
                        )
                    }
                }
            }

            if (printers.isEmpty()) {
                item {
                    IosCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Print,
                                contentDescription = null,
                                tint = IosTextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ready to Print to Any Device",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = IosTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "HP, Canon, Epson, Brother, USB OTG, AirPrint",
                                fontSize = 13.sp,
                                color = IosTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(printers, key = { it.id }) { printer ->
                    PrinterCard(
                        printer = printer,
                        isSelected = selectedPrinter?.id == printer.id,
                        onClick = {
                            HapticFeedbackHelper.performSelection(context)
                            selectedPrinter = printer
                            Toast.makeText(context, "Selected: ${printer.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


@Composable
fun ActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    onClick: () -> Unit
) {
    IosCard(
        modifier = modifier,
        onClick = onClick,
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = IosTextSecondary
            )
        }
    }
}
