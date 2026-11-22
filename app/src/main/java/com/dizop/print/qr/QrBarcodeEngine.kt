package com.dizop.print.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrBarcodeEngine {

    /**
     * Encodes QR content into a boolean 2D BitMatrix (pure JVM logic).
     */
    fun encodeQrMatrix(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): Result<BitMatrix> {
        if (content.isBlank()) return Result.failure(IllegalArgumentException("Content cannot be empty"))
        return try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            Result.success(bitMatrix)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a high-contrast QR Code Bitmap suitable for direct printing or badges on Android.
     */
    suspend fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        val matrixResult = encodeQrMatrix(content, width, height)
        if (matrixResult.isFailure) {
            return@withContext Result.failure(matrixResult.exceptionOrNull()!!)
        }

        try {
            val bitMatrix = matrixResult.getOrThrow()
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            Result.success(bmp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
