package com.dizop.print.scanner

import com.dizop.print.qr.QrBarcodeEngine
import org.junit.Assert.*
import org.junit.Test

class DocScannerEnginesUnitTest {

    @Test
    fun testDocScannerFiltersEnumIntegrity() {
        val filters = DocScannerFilter.values()
        assertTrue(filters.contains(DocScannerFilter.ORIGINAL))
        assertTrue(filters.contains(DocScannerFilter.MAGIC_COLOR))
        assertTrue(filters.contains(DocScannerFilter.DOCUMENT_BW))
        assertTrue(filters.contains(DocScannerFilter.GRAYSCALE))
        assertTrue(filters.contains(DocScannerFilter.VIBRANT))
    }

    @Test
    fun testQrBarcodeEngineEncodesValidMatrix() {
        val result = QrBarcodeEngine.encodeQrMatrix("DIZOP-OFFLINE-PRINT-AUTH-CODE", 256, 256)
        assertTrue(result.isSuccess)
        val matrix = result.getOrNull()
        assertNotNull(matrix)
        assertEquals(256, matrix?.width)
        assertEquals(256, matrix?.height)
    }

    @Test
    fun testQrBarcodeEngineRejectsEmptyString() {
        val result = QrBarcodeEngine.encodeQrMatrix("")
        assertTrue(result.isFailure)
    }
}
