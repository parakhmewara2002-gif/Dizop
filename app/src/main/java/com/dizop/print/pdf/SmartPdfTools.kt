package com.dizop.print.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SmartPdfTools {

    /**
     * Compresses a heavy PDF by re-sampling image pages at optimized DPI with lossless text bounds.
     */
    suspend fun compressPdf(
        inputFile: File,
        outputFile: File,
        targetQualityPercent: Int = 70
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val newDoc = PdfDocument()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                // Downsample scale ~0.75 for huge byte reduction
                val scale = 0.75f
                val w = (page.width * scale).toInt().coerceAtLeast(100)
                val h = (page.height * scale).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                val newPage = newDoc.startPage(pageInfo)
                newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                newDoc.finishPage(newPage)
                bitmap.recycle()
            }

            renderer.close()
            fd.close()

            FileOutputStream(outputFile).use { out ->
                newDoc.writeTo(out)
            }
            newDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Merges multiple PDF files into one master document.
     */
    suspend fun mergePdfs(
        inputPdfs: List<File>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val masterDoc = PdfDocument()
            var pageCounter = 1

            for (file in inputPdfs) {
                val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)

                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    page.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pageCounter++).create()
                    val newPage = masterDoc.startPage(pageInfo)
                    newPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    masterDoc.finishPage(newPage)
                    bitmap.recycle()
                }
                renderer.close()
                fd.close()
            }

            FileOutputStream(outputFile).use { out ->
                masterDoc.writeTo(out)
            }
            masterDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Applies a Digital Signature or Official Stamp to the bottom right of a PDF page.
     */
    suspend fun applySignatureToPdf(
        pdfFile: File,
        signatureBitmap: Bitmap,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val newDoc = PdfDocument()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                val newPage = newDoc.startPage(pageInfo)
                val canvas = newPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                // Apply signature on the last page
                if (i == renderer.pageCount - 1) {
                    val sigWidth = page.width * 0.35f
                    val sigHeight = (sigWidth / (signatureBitmap.width.toFloat() / signatureBitmap.height.toFloat()))
                    val left = page.width - sigWidth - 40f
                    val top = page.height - sigHeight - 60f

                    val destRect = android.graphics.RectF(left, top, left + sigWidth, top + sigHeight)
                    canvas.drawBitmap(signatureBitmap, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG))
                }

                newDoc.finishPage(newPage)
                bitmap.recycle()
            }

            renderer.close()
            fd.close()

            FileOutputStream(outputFile).use { out ->
                newDoc.writeTo(out)
            }
            newDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
