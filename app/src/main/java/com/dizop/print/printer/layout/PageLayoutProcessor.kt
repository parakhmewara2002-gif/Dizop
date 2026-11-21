package com.dizop.print.printer.layout

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.dizop.print.printer.config.PagesPerSheet
import com.dizop.print.printer.config.PosterMode
import com.dizop.print.printer.config.PrintJobConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * High-performance Page Layout & N-Up / Poster Transform Processor.
 * Renders transformed multi-page grids, poster splits, cut marks, and custom margins.
 */
object PageLayoutProcessor {

    /**
     * Applies layout transformations (N-Up, Poster, Page Range, Duplex filter)
     * and produces a clean transformed PDF ready for print engines.
     */
    suspend fun processPdfLayout(
        sourcePdf: File,
        destPdf: File,
        config: PrintJobConfiguration
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(sourcePdf, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val totalPages = renderer.pageCount
            if (totalPages == 0) {
                renderer.close()
                fd.close()
                return@withContext false
            }

            val doc = PdfDocument()

            // 1. Filter pages based on Duplex / Range
            val pageIndices = mutableListOf<Int>()
            for (i in 0 until totalPages) {
                val pageNum = i + 1
                val isOdd = pageNum % 2 != 0
                val include = when (config.duplexMode) {
                    com.dizop.print.printer.config.DuplexMode.MANUAL_ODD_ONLY -> isOdd
                    com.dizop.print.printer.config.DuplexMode.MANUAL_EVEN_ONLY -> !isOdd
                    else -> true
                }
                if (include) pageIndices.add(i)
            }

            // Standard target page dimensions (Points: 72 points per inch)
            // A4 = 595 x 842 points
            val targetPageWidth = (config.paperSize.widthMm * 72f / 25.4f).toInt()
            val targetPageHeight = (config.paperSize.heightMm * 72f / 25.4f).toInt()

            val marginPoints = config.margins.marginMm * 72f / 25.4f

            // Case A: Poster Mode (Splits each page into 2x2 or 3x3 separate sheets)
            if (config.posterMode != PosterMode.NONE) {
                val cols = config.posterMode.cols
                val rows = config.posterMode.rows

                for (pageIdx in pageIndices) {
                    val page = renderer.openPage(pageIdx)
                    // High-res render for poster scaling
                    val bmpWidth = page.width * 2
                    val bmpHeight = page.height * 2
                    val fullBitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    page.render(fullBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val tileW = bmpWidth / cols
                    val tileH = bmpHeight / rows

                    for (r in 0 until rows) {
                        for (c in 0 until cols) {
                            val srcRect = Rect(c * tileW, r * tileH, (c + 1) * tileW, (r + 1) * tileH)
                            val pageInfo = PdfDocument.PageInfo.Builder(targetPageWidth, targetPageHeight, doc.pages.size + 1).create()
                            val pdfPage = doc.startPage(pageInfo)
                            val canvas = pdfPage.canvas

                            // Draw poster tile filling printable area
                            val destRect = RectF(
                                marginPoints,
                                marginPoints,
                                targetPageWidth - marginPoints,
                                targetPageHeight - marginPoints
                            )
                            canvas.drawBitmap(fullBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

                            // Draw cut marks
                            val paint = Paint().apply {
                                color = Color.LTGRAY
                                strokeWidth = 1f
                                style = Paint.Style.STROKE
                            }
                            canvas.drawRect(destRect, paint)

                            doc.finishPage(pdfPage)
                        }
                    }
                    fullBitmap.recycle()
                }
            }
            // Case B: N-Up Multi-Page (1, 2, 4, 6, 9 pages per single sheet)
            else if (config.pagesPerSheet != PagesPerSheet.ONE) {
                val n = config.pagesPerSheet.count
                val (cols, rows) = when (n) {
                    2 -> Pair(1, 2)
                    4 -> Pair(2, 2)
                    6 -> Pair(2, 3)
                    9 -> Pair(3, 3)
                    else -> Pair(1, 1)
                }

                val subWidth = (targetPageWidth - 2 * marginPoints) / cols
                val subHeight = (targetPageHeight - 2 * marginPoints) / rows

                var currentSub = 0
                var currentPdfPage: PdfDocument.Page? = null
                var currentCanvas: Canvas? = null

                val borderPaint = Paint().apply {
                    color = Color.DKGRAY
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                }

                for (pageIdx in pageIndices) {
                    if (currentSub % n == 0) {
                        currentPdfPage?.let { doc.finishPage(it) }
                        val pageInfo = PdfDocument.PageInfo.Builder(targetPageWidth, targetPageHeight, doc.pages.size + 1).create()
                        currentPdfPage = doc.startPage(pageInfo)
                        currentCanvas = currentPdfPage.canvas
                    }

                    val slot = currentSub % n
                    val col = slot % cols
                    val row = slot / cols

                    val left = marginPoints + col * subWidth
                    val top = marginPoints + row * subHeight

                    val page = renderer.openPage(pageIdx)
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val destRect = RectF(left + 4f, top + 4f, left + subWidth - 4f, top + subHeight - 4f)
                    currentCanvas?.drawBitmap(bmp, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

                    if (config.printBorderLines) {
                        currentCanvas?.drawRect(destRect, borderPaint)
                    }

                    bmp.recycle()
                    currentSub++
                }

                currentPdfPage?.let { doc.finishPage(it) }
            }
            // Case C: Standard 1-to-1 page rendering with custom margins & scaling
            else {
                for (pageIdx in pageIndices) {
                    val page = renderer.openPage(pageIdx)
                    val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(targetPageWidth, targetPageHeight, doc.pages.size + 1).create()
                    val pdfPage = doc.startPage(pageInfo)
                    val canvas = pdfPage.canvas

                    val destRect = RectF(
                        marginPoints,
                        marginPoints,
                        targetPageWidth - marginPoints,
                        targetPageHeight - marginPoints
                    )
                    canvas.drawBitmap(bmp, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    doc.finishPage(pdfPage)
                    bmp.recycle()
                }
            }

            renderer.close()
            fd.close()

            FileOutputStream(destPdf).use { fos ->
                doc.writeTo(fos)
            }
            doc.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
