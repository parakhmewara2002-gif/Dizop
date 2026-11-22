package com.dizop.print.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

enum class DocScannerFilter(val displayName: String, val description: String) {
    ORIGINAL("Original", "Natural untouched photo"),
    MAGIC_COLOR("Magic Color", "Auto contrast & text enhancement"),
    DOCUMENT_BW("B&W Doc", "Crisp photocopy black and white"),
    GRAYSCALE("Grayscale", "Smooth professional gray tones"),
    VIBRANT("Vibrant", "Boosted saturation for charts & graphics")
}

object DocumentFilterEngine {

    /**
     * Applies calibrated document matrix filters matching professional scanner outputs.
     */
    fun applyFilter(source: Bitmap, filter: DocScannerFilter): Bitmap {
        if (filter == DocScannerFilter.ORIGINAL) return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val colorMatrix = when (filter) {
            DocScannerFilter.ORIGINAL -> ColorMatrix()

            DocScannerFilter.MAGIC_COLOR -> {
                // High contrast with background whitening & black ink preservation
                ColorMatrix().apply {
                    set(
                        floatArrayOf(
                            1.65f, 0f, 0f, 0f, -45f,
                            0f, 1.65f, 0f, 0f, -45f,
                            0f, 0f, 1.65f, 0f, -45f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
            }

            DocScannerFilter.DOCUMENT_BW -> {
                // Grayscale luminance conversion with aggressive text threshold
                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
                val thresholdMatrix = ColorMatrix(
                    floatArrayOf(
                        2.8f, 0f, 0f, 0f, -160f,
                        0f, 2.8f, 0f, 0f, -160f,
                        0f, 0f, 2.8f, 0f, -160f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                grayMatrix.postConcat(thresholdMatrix)
                grayMatrix
            }

            DocScannerFilter.GRAYSCALE -> {
                ColorMatrix().apply {
                    setSaturation(0f)
                    val contrastMatrix = ColorMatrix(
                        floatArrayOf(
                            1.2f, 0f, 0f, 0f, -20f,
                            0f, 1.2f, 0f, 0f, -20f,
                            0f, 0f, 1.2f, 0f, -20f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    postConcat(contrastMatrix)
                }
            }

            DocScannerFilter.VIBRANT -> {
                ColorMatrix().apply {
                    setSaturation(1.4f)
                    val contrastMatrix = ColorMatrix(
                        floatArrayOf(
                            1.15f, 0f, 0f, 0f, -10f,
                            0f, 1.15f, 0f, 0f, -10f,
                            0f, 0f, 1.15f, 0f, -10f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    postConcat(contrastMatrix)
                }
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }
}
