package com.dizop.print.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dizop.print.printer.config.ColorMode
import com.dizop.print.printer.config.DuplexMode
import com.dizop.print.printer.config.PaperSize
import com.dizop.print.printer.config.PrintJobConfiguration
import com.dizop.print.ui.theme.IosBlue
import com.dizop.print.ui.theme.IosCardBorder
import com.dizop.print.ui.theme.IosCardLight
import com.dizop.print.ui.theme.IosTextPrimary
import com.dizop.print.ui.theme.IosTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Premium iOS-style Interactive Print Preview & Configuration Sheet
 */
@Composable
fun PrintPreviewSheet(
    pdfFile: File,
    onConfirmPrint: (PrintJobConfiguration) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(PrintJobConfiguration()) }
    var currentPageIndex by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pdfFile, currentPageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                totalPages = renderer.pageCount.coerceAtLeast(1)
                val safeIndex = currentPageIndex.coerceIn(0, totalPages - 1)
                val page = renderer.openPage(safeIndex)

                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                fd.close()
                previewBitmap = bmp
            } catch (e: Exception) {
                // PDF render fallback
            }
        }
    }

    IosCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Print Options & Preview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosTextPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close", tint = IosTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Document Preview Canvas with Page Navigation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE5E5EA))
                    .border(0.5.dp, IosCardBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Page Preview",
                        modifier = Modifier
                            .padding(12.dp)
                            .shadow(4.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White),
                        contentScale = ContentScale.Fit
                    )
                }

                // Page Switcher Controls
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.NavigateBefore, contentDescription = "Prev", tint = Color.White)
                        }

                        Text(
                            text = "${currentPageIndex + 1} of $totalPages",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.NavigateNext, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Copies", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosTextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E5EA))
                            .clickable { if (config.copies > 1) config = config.copy(copies = config.copies - 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${config.copies}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E5EA))
                            .clickable { config = config.copy(copies = config.copies + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color Mode Switcher
            IosSegmentedControl(
                items = listOf("Color", "Grayscale (B&W)"),
                selectedIndex = if (config.colorMode == ColorMode.COLOR) 0 else 1,
                onItemSelected = { idx ->
                    config = config.copy(colorMode = if (idx == 0) ColorMode.COLOR else ColorMode.MONOCHROME)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Paper Size Switcher
            IosSegmentedControl(
                items = listOf("A4", "Letter", "Legal", "4x6"),
                selectedIndex = when (config.paperSize) {
                    PaperSize.A4 -> 0
                    PaperSize.LETTER -> 1
                    PaperSize.LEGAL -> 2
                    PaperSize.PHOTO_4X6 -> 3
                    else -> 0
                },
                onItemSelected = { idx ->
                    val size = when (idx) {
                        0 -> PaperSize.A4
                        1 -> PaperSize.LETTER
                        2 -> PaperSize.LEGAL
                        3 -> PaperSize.PHOTO_4X6
                        else -> PaperSize.A4
                    }
                    config = config.copy(paperSize = size)
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = { onConfirmPrint(config) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print Document (${config.copies} Copies)", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
