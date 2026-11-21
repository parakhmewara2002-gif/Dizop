package com.dizop.print.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SmartDocumentProcessor {

    /**
     * Enhanced Document Enhancement:
     * - Auto contrast boost
     * - White balance correction & shadow removal
     * - B&W Document optimization
     */
    suspend fun processDocumentScan(
        source: Bitmap,
        highContrast: Boolean = true,
        removeShadows: Boolean = true
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (highContrast) {
            val colorMatrix = ColorMatrix().apply {
                if (removeShadows) {
                    // Aggressive document text whitening with dark ink preservation
                    set(
                        floatArrayOf(
                            1.6f, 0f, 0f, 0f, -50f,
                            0f, 1.6f, 0f, 0f, -50f,
                            0f, 0f, 1.6f, 0f, -50f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                } else {
                    set(
                        floatArrayOf(
                            1.3f, 0f, 0f, 0f, -25f,
                            0f, 1.3f, 0f, 0f, -25f,
                            0f, 0f, 1.3f, 0f, -25f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
            }
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        output
    }

    /**
     * ID Card 2-in-1 layout creator:
     * Takes Front and Back captures and merges them onto a standard single A4 canvas.
     */
    suspend fun createIdCardA4Sheet(
        front: Bitmap,
        back: Bitmap
    ): Bitmap = withContext(Dispatchers.Default) {
        // Standard A4 at 300 DPI: 2480 x 3508
        val a4Width = 2480
        val a4Height = 3508
        val sheet = Bitmap.createBitmap(a4Width, a4Height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        // Standard ID card aspect ratio is ~85.6mm x 53.98mm (width / height ~ 1.586)
        val targetCardWidth = (a4Width * 0.70f).toInt()
        val targetCardHeight = (targetCardWidth / 1.586f).toInt()

        val leftMargin = (a4Width - targetCardWidth) / 2
        
        // Front Side
        val frontTop = (a4Height * 0.18f).toInt()
        val frontRect = RectF(
            leftMargin.toFloat(),
            frontTop.toFloat(),
            (leftMargin + targetCardWidth).toFloat(),
            (frontTop + targetCardHeight).toFloat()
        )
        canvas.drawBitmap(front, null, frontRect, paint)
        canvas.drawRoundRect(frontRect, 30f, 30f, borderPaint)

        // Back Side
        val backTop = frontTop + targetCardHeight + (a4Height * 0.12f).toInt()
        val backRect = RectF(
            leftMargin.toFloat(),
            backTop.toFloat(),
            (leftMargin + targetCardWidth).toFloat(),
            (backTop + targetCardHeight).toFloat()
        )
        canvas.drawBitmap(back, null, backRect, paint)
        canvas.drawRoundRect(backRect, 30f, 30f, borderPaint)

        sheet
    }

    /**
     * Converts a list of scanned page Bitmaps into a multi-page PDF.
     */
    suspend fun createPdfFromBitmaps(
        pages: List<Bitmap>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            pages.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                document.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
