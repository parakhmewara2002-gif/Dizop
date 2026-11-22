package com.dizop.print.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AdvancedPdfEngine {

    /**
     * Adds custom diagonal Watermark across all pages of a PDF.
     */
    suspend fun addWatermark(
        inputFile: File,
        outputFile: File,
        watermarkText: String,
        watermarkColor: Int = Color.argb(45, 120, 120, 120)
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val newDoc = PdfDocument()

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = watermarkColor
                textSize = 64f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                val newPage = newDoc.startPage(pageInfo)
                val canvas = newPage.canvas

                // Draw base page
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                // Draw diagonal watermark
                canvas.save()
                canvas.rotate(-45f, bitmap.width / 2f, bitmap.height / 2f)
                canvas.drawText(watermarkText, bitmap.width / 2f, bitmap.height / 2f, textPaint)
                canvas.restore()

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
     * Adds clean page numbers at the bottom center of each page (e.g., "1 / 4").
     */
    suspend fun addPageNumbers(
        inputFile: File,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val totalPages = renderer.pageCount
            val newDoc = PdfDocument()

            val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }

            for (i in 0 until totalPages) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, i + 1).create()
                val newPage = newDoc.startPage(pageInfo)
                val canvas = newPage.canvas

                canvas.drawBitmap(bitmap, 0f, 0f, null)
                val pageString = "- ${i + 1} / $totalPages -"
                canvas.drawText(pageString, bitmap.width / 2f, bitmap.height - 30f, numberPaint)

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
