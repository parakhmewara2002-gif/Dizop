package com.dizop.print.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.dizop.print.printer.config.PageMargins
import com.dizop.print.printer.config.PagesPerSheet
import com.dizop.print.printer.config.PaperSize
import com.dizop.print.printer.config.PosterMode
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
 * Premium iOS-style Interactive Print Preview & Universal Configuration Sheet
 * Supporting N-Up Multi-Page, Poster Mode, Duplex, Custom Paper & Photo Sizes, Margins.
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
    val sheetScroll = rememberScrollState()

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
                // PDF render error
            }
        }
    }

    IosCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScroll)
        ) {
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
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = IosTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PDF Live Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(IosCardLight)
                    .border(1.dp, IosCardBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                previewBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Page Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(12.dp)
                            .shadow(6.dp, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                    )
                } ?: run {
                    Text("Rendering Preview...", color = IosTextSecondary, fontSize = 14.sp)
                }

                // Page Navigation Pill
                if (totalPages > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(14.dp))

            // Copies Counter
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
            Text("Color Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IosTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            IosSegmentedControl(
                items = listOf("Color", "Grayscale (B&W)"),
                selectedIndex = if (config.colorMode == ColorMode.COLOR) 0 else 1,
                onItemSelected = { idx ->
                    config = config.copy(colorMode = if (idx == 0) ColorMode.COLOR else ColorMode.MONOCHROME)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Page (N-Up Layout)
            Text("Pages Per Sheet (Multi-Page)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IosTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PagesPerSheet.values().forEach { nUp ->
                    FilterChip(
                        selected = config.pagesPerSheet == nUp,
                        onClick = { config = config.copy(pagesPerSheet = nUp, posterMode = PosterMode.NONE) },
                        label = { Text(nUp.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IosBlue.copy(alpha = 0.15f),
                            selectedLabelColor = IosBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Poster Printing Options
            Text("Poster Mode (Split Across Sheets)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IosTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PosterMode.values().forEach { poster ->
                    FilterChip(
                        selected = config.posterMode == poster,
                        onClick = { config = config.copy(posterMode = poster, pagesPerSheet = PagesPerSheet.ONE) },
                        label = { Text(poster.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IosBlue.copy(alpha = 0.15f),
                            selectedLabelColor = IosBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Paper & Photo Sizes
            Text("Paper & Photo Size", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IosTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperSize.values().forEach { size ->
                    FilterChip(
                        selected = config.paperSize == size,
                        onClick = { config = config.copy(paperSize = size) },
                        label = { Text(size.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IosBlue.copy(alpha = 0.15f),
                            selectedLabelColor = IosBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duplex & 2-Sided Printing
            Text("Double-Sided (Duplex)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IosTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DuplexMode.values().forEach { duplex ->
                    FilterChip(
                        selected = config.duplexMode == duplex,
                        onClick = { config = config.copy(duplexMode = duplex) },
                        label = { Text(duplex.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IosBlue.copy(alpha = 0.15f),
                            selectedLabelColor = IosBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Margins Selection
            Text("Page Margins", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = IosTextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PageMargins.values().forEach { margin ->
                    FilterChip(
                        selected = config.margins == margin,
                        onClick = { config = config.copy(margins = margin) },
                        label = { Text(margin.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IosBlue.copy(alpha = 0.15f),
                            selectedLabelColor = IosBlue
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onConfirmPrint(config) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosBlue)
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Print with Options (${config.copies} Copies)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
