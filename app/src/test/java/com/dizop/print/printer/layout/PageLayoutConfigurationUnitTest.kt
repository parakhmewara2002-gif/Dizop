package com.dizop.print.printer.layout

import com.dizop.print.printer.config.*
import org.junit.Assert.*
import org.junit.Test

class PageLayoutConfigurationUnitTest {

    @Test
    fun testDefaultPrintJobConfiguration() {
        val config = PrintJobConfiguration()
        assertEquals(1, config.copies)
        assertEquals(ColorMode.COLOR, config.colorMode)
        assertEquals(PaperSize.A4, config.paperSize)
        assertEquals(DuplexMode.ONE_SIDED, config.duplexMode)
        assertEquals(PagesPerSheet.ONE, config.pagesPerSheet)
        assertEquals(PosterMode.NONE, config.posterMode)
        assertEquals(PageMargins.NORMAL, config.margins)
    }

    @Test
    fun testNUpCalculations() {
        assertEquals(1, PagesPerSheet.ONE.count)
        assertEquals(2, PagesPerSheet.TWO.count)
        assertEquals(4, PagesPerSheet.FOUR.count)
        assertEquals(6, PagesPerSheet.SIX.count)
        assertEquals(9, PagesPerSheet.NINE.count)
    }

    @Test
    fun testPosterModeGridDimensions() {
        assertEquals(2, PosterMode.POSTER_2X2.cols)
        assertEquals(2, PosterMode.POSTER_2X2.rows)
        assertEquals(4, PosterMode.POSTER_2X2.cols * PosterMode.POSTER_2X2.rows)

        assertEquals(3, PosterMode.POSTER_3X3.cols)
        assertEquals(3, PosterMode.POSTER_3X3.rows)
        assertEquals(9, PosterMode.POSTER_3X3.cols * PosterMode.POSTER_3X3.rows)
    }

    @Test
    fun testPaperSizeDimensionsMm() {
        assertEquals(210f, PaperSize.A4.widthMm)
        assertEquals(297f, PaperSize.A4.heightMm)

        assertEquals(101.6f, PaperSize.PHOTO_4X6.widthMm)
        assertEquals(152.4f, PaperSize.PHOTO_4X6.heightMm)
    }
}
