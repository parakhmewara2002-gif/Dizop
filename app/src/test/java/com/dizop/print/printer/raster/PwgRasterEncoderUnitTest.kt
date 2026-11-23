package com.dizop.print.printer.raster

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class PwgRasterEncoderUnitTest {

    private fun decode(encoded: ByteArray, width: Int): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (out.size() < width) {
            val c = encoded[i++].toInt() and 0xFF
            if (c <= 127) {
                val px = encoded[i++]
                repeat(c + 1) { out.write(px.toInt()) }
            } else {
                val count = 257 - c
                out.write(encoded, i, count)
                i += count
            }
        }
        return out.toByteArray()
    }

    @Test
    fun lineRleRoundTrips() {
        val line = ByteArray(1000) { idx ->
            when {
                idx < 300 -> 0xFF.toByte()                    // long white run (> 128 -> split)
                idx < 400 -> (idx * 7).toByte()               // noisy literal region
                idx < 410 -> 0x00                              // short black run
                else -> if (idx % 2 == 0) 0x10 else 0x20       // alternating pixels
            }
        }
        val out = ByteArrayOutputStream()
        PwgRasterEncoder.encodeLine(line, out)
        assertArrayEquals(line, decode(out.toByteArray(), line.size))
    }

    @Test
    fun headerLayout() {
        val h = PwgRasterEncoder.buildHeader(2480, 3508, 300, 595, 842, 1)
        assertEquals(1796, h.size)
        assertEquals("PwgRaster", String(h, 0, 9, Charsets.US_ASCII))
        val b = java.nio.ByteBuffer.wrap(h)
        assertEquals(2480, b.getInt(372))
        assertEquals(3508, b.getInt(376))
        assertEquals(8, b.getInt(388))
        assertEquals(2480, b.getInt(392))
        assertEquals(18, b.getInt(400))
    }
}
