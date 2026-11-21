package com.dizop.print.printer.protocol.ipp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IppProtocolUnitTest {

    @Test
    fun testPrintJobHeaderEncoding() {
        val bytes = IppPacketEncoder.createPrintJobHeader(
            printerUri = "ipp://192.168.1.50:631/ipp/print",
            jobName = "Test Job",
            userName = "TestUser",
            documentFormat = "application/pdf",
            copies = 2,
            paperSizeKeyword = "iso_a4_210x297mm",
            duplexKeyword = "two-sided-long-edge",
            colorModeKeyword = "color",
            requestId = 42
        )

        assertNotNull(bytes)
        assertTrue(bytes.size > 20)
        // Check IPP version 2.0 (0x02, 0x00)
        assertEquals(0x02.toByte(), bytes[0])
        assertEquals(0x00.toByte(), bytes[1])
        // Check operation Print-Job (0x00, 0x02)
        assertEquals(0x00.toByte(), bytes[2])
        assertEquals(0x02.toByte(), bytes[3])
    }

    @Test
    fun testMalformedIppResponseHandling() {
        val corruptedBytes = byteArrayOf(0x02, 0x00, 0x00) // too short
        val parsed = IppResponseParser.parse(corruptedBytes)

        assertEquals(IppResponseStatus.MALFORMED_RESPONSE, parsed.status)
        assertEquals("unknown", parsed.printerState)
        assertEquals(0, parsed.inkLevels.size)
    }

    @Test
    fun testSuccessfulIppResponseParsing() {
        // Construct minimal valid IPP successful response frame:
        // Version (2.0) = 0x02, 0x00
        // Status (0x0000 = successful-ok) = 0x00, 0x00
        // Request ID (1) = 0x00, 0x00, 0x00, 0x01
        // End of attributes = 0x03
        val validBytes = byteArrayOf(
            0x02, 0x00,
            0x00, 0x00,
            0x00, 0x00, 0x00, 0x01,
            0x03
        )
        val parsed = IppResponseParser.parse(validBytes)

        assertEquals(IppResponseStatus.SUCCESS, parsed.status)
        assertEquals(0, parsed.statusCode)
        assertEquals(1, parsed.requestId)
    }
}
