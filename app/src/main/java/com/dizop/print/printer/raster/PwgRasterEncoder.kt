package com.dizop.print.printer.raster

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Converts a PDF into a PWG Raster stream (PWG 5102.4, "image/pwg-raster"), 8-bit sGray, for
 * printers that do not accept application/pdf (e.g. HP Laser MFP 1188w, IPP error 0x040a).
 * Pages are rendered in small horizontal bands so memory stays small at any resolution.
 */
object PwgRasterEncoder {

    private const val HEADER_SIZE = 1796
    private const val BAND_ROWS = 128

    fun encodePdfToFile(pdf: File, out: File, dpi: Int = 300) {
        val pfd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(pfd).use { renderer ->
            BufferedOutputStream(FileOutputStream(out), 1 shl 16).use { os ->
                os.write("RaS2".toByteArray(Charsets.US_ASCII))
                for (pageIndex in 0 until renderer.pageCount) {
                    val page = renderer.openPage(pageIndex)
                    try {
                        writePage(os, page, dpi, renderer.pageCount)
                    } finally {
                        page.close()
                    }
                }
            }
        }
    }

    private fun writePage(os: OutputStream, page: PdfRenderer.Page, dpi: Int, totalPages: Int) {
        val widthPx = max(1, (page.width * dpi / 72f).roundToInt())
        val heightPx = max(1, (page.height * dpi / 72f).roundToInt())
        os.write(buildHeader(widthPx, heightPx, dpi, page.width, page.height, totalPages))

        val bitmap = Bitmap.createBitmap(widthPx, BAND_ROWS, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(widthPx * BAND_ROWS)
        val scaleX = widthPx / page.width.toFloat()
        val scaleY = heightPx / page.height.toFloat()

        var pending: ByteArray? = null
        var pendingCount = 0
        fun flush() {
            val line = pending ?: return
            os.write(pendingCount - 1) // line-repeat count: 0 = line appears once
            encodeLine(line, os)
        }

        try {
            var y = 0
            while (y < heightPx) {
                val rows = min(BAND_ROWS, heightPx - y)
                bitmap.eraseColor(Color.WHITE)
                val m = Matrix().apply {
                    setScale(scaleX, scaleY)
                    postTranslate(0f, -y.toFloat())
                }
                page.render(bitmap, null, m, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                bitmap.getPixels(pixels, 0, widthPx, 0, 0, widthPx, rows)
                for (r in 0 until rows) {
                    val gray = ByteArray(widthPx)
                    val base = r * widthPx
                    for (x in 0 until widthPx) {
                        val p = pixels[base + x]
                        gray[x] = ((((p shr 16) and 0xFF) * 77 + ((p shr 8) and 0xFF) * 151 + (p and 0xFF) * 28) shr 8).toByte()
                    }
                    if (pending != null && pendingCount < 256 && java.util.Arrays.equals(pending, gray)) {
                        pendingCount++
                    } else {
                        flush()
                        pending = gray
                        pendingCount = 1
                    }
                }
                y += rows
            }
            flush()
        } finally {
            bitmap.recycle()
        }
    }

    /** 1796-byte big-endian PWG raster page header (same layout as cups_page_header2_t). */
    internal fun buildHeader(widthPx: Int, heightPx: Int, dpi: Int, pageWidthPt: Int, pageHeightPt: Int, totalPages: Int): ByteArray {
        val b = ByteBuffer.allocate(HEADER_SIZE) // big-endian by default
        val sig = "PwgRaster".toByteArray(Charsets.US_ASCII)
        for (i in sig.indices) b.put(i, sig[i])
        b.putInt(272, 0)                // Duplex
        b.putInt(276, dpi); b.putInt(280, dpi) // HWResolution
        b.putInt(340, 1)                // NumCopies
        b.putInt(344, 0)                // Orientation
        b.putInt(352, pageWidthPt); b.putInt(356, pageHeightPt) // PageSize (points)
        b.putInt(368, 0)                // Tumble
        b.putInt(372, widthPx); b.putInt(376, heightPx)
        b.putInt(384, 8)                // BitsPerColor
        b.putInt(388, 8)                // BitsPerPixel
        b.putInt(392, widthPx)          // BytesPerLine
        b.putInt(396, 0)                // ColorOrder: chunky
        b.putInt(400, 18)               // ColorSpace: sGray
        b.putInt(420, 1)                // NumColors
        b.putInt(452, totalPages)       // TotalPageCount
        b.putInt(456, 1); b.putInt(460, 1) // Cross/Feed transform
        b.putInt(464, 0); b.putInt(468, 0); b.putInt(472, widthPx); b.putInt(476, heightPx) // ImageBox
        return b.array()
    }

    /** PWG raster line RLE for 1-byte pixels: 0..127 = repeat pixel n+1 times, 129..255 = 257-n literal pixels. */
    internal fun encodeLine(line: ByteArray, out: OutputStream) {
        val n = line.size
        var i = 0
        while (i < n) {
            var run = 1
            while (i + run < n && run < 128 && line[i + run] == line[i]) run++
            if (run >= 2) {
                out.write(run - 1)
                out.write(line[i].toInt() and 0xFF)
                i += run
            } else {
                var j = i + 1
                while (j < n && (j - i) < 128 && !(j + 1 < n && line[j] == line[j + 1])) j++
                val count = j - i
                if (count == 1) {
                    out.write(0)
                    out.write(line[i].toInt() and 0xFF)
                } else {
                    out.write(257 - count)
                    out.write(line, i, count)
                }
                i = j
            }
        }
    }
}
