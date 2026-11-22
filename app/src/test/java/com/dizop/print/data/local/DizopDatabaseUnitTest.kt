package com.dizop.print.data.local

import org.junit.Assert.*
import org.junit.Test

class DizopDatabaseUnitTest {

    @Test
    fun testPrintHistoryRecordDataIntegrity() {
        val record = PrintHistoryRecord(
            id = 1L,
            title = "Annual Financial Report.pdf",
            printerName = "HP LaserJet M404dn",
            timestamp = 1700000000000L,
            status = "Sent to Printer"
        )

        assertEquals(1L, record.id)
        assertEquals("Annual Financial Report.pdf", record.title)
        assertEquals("HP LaserJet M404dn", record.printerName)
        assertEquals(1700000000000L, record.timestamp)
        assertEquals("Sent to Printer", record.status)
    }

    @Test
    fun testSavedPrinterRecordDataIntegrity() {
        val printer = SavedPrinterRecord(
            id = 10L,
            name = "Office Color Laser",
            host = "192.168.1.150",
            port = 631,
            protocol = "IPP"
        )

        assertEquals(10L, printer.id)
        assertEquals("Office Color Laser", printer.name)
        assertEquals("192.168.1.150", printer.host)
        assertEquals(631, printer.port)
        assertEquals("IPP", printer.protocol)
    }
}
