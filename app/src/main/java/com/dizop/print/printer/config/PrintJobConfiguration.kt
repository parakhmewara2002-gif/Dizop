package com.dizop.print.printer.config

enum class PaperSize(val displayName: String, val widthMm: Float, val heightMm: Float) {
    A4("A4 (210 x 297 mm)", 210f, 297f),
    A3("A3 (297 x 420 mm)", 297f, 420f),
    A5("A5 (148 x 210 mm)", 148f, 210f),
    LETTER("Letter (8.5 x 11 in)", 215.9f, 279.4f),
    LEGAL("Legal (8.5 x 14 in)", 215.9f, 355.6f),
    PHOTO_4X6("Photo 4 x 6 in (10x15 cm)", 101.6f, 152.4f),
    PHOTO_5X7("Photo 5 x 7 in (13x18 cm)", 127f, 177.8f),
    PHOTO_PASSPORT("Passport (35 x 45 mm)", 35f, 45f),
    PHOTO_2X2("ID / Visa (2 x 2 in / 51x51 mm)", 50.8f, 50.8f)
}

enum class ColorMode(val displayName: String) {
    COLOR("Full Color"),
    MONOCHROME("Black & White (Grayscale)")
}

enum class DuplexMode(val displayName: String) {
    ONE_SIDED("Single-Sided"),
    TWO_SIDED_LONG_EDGE("2-Sided (Flip Long Edge)"),
    TWO_SIDED_SHORT_EDGE("2-Sided (Flip Short Edge)"),
    MANUAL_ODD_ONLY("Manual Duplex: Front Sides (Odd)"),
    MANUAL_EVEN_ONLY("Manual Duplex: Back Sides (Even)")
}

enum class PageScaling(val displayName: String) {
    FIT_TO_PAGE("Fit to Printable Area"),
    FILL_PAGE("Crop / Fill Page (Bleed)"),
    ACTUAL_SIZE("100% Actual Size")
}

enum class PagesPerSheet(val count: Int, val displayName: String) {
    ONE(1, "1 per sheet"),
    TWO(2, "2 per sheet"),
    FOUR(4, "4 per sheet"),
    SIX(6, "6 per sheet"),
    NINE(9, "9 per sheet")
}

enum class PosterMode(val displayName: String, val cols: Int, val rows: Int) {
    NONE("Normal Page", 1, 1),
    POSTER_2X2("Poster 2x2 (4 sheets)", 2, 2),
    POSTER_3X3("Poster 3x3 (9 sheets)", 3, 3)
}

enum class PageMargins(val displayName: String, val marginMm: Float) {
    NORMAL("Normal", 10f),
    NARROW("Narrow", 5f),
    NONE("None (Borderless)", 0f),
    WIDE("Wide", 20f)
}

enum class PagePosition(val displayName: String) {
    CENTER("Center"),
    TOP_LEFT("Top Left"),
    TOP_MIDDLE("Top Middle"),
    BOTTOM_MIDDLE("Bottom Middle")
}

data class PrintJobConfiguration(
    val copies: Int = 1,
    val paperSize: PaperSize = PaperSize.A4,
    val colorMode: ColorMode = ColorMode.COLOR,
    val duplexMode: DuplexMode = DuplexMode.ONE_SIDED,
    val pageScaling: PageScaling = PageScaling.FIT_TO_PAGE,
    val pagesPerSheet: PagesPerSheet = PagesPerSheet.ONE,
    val posterMode: PosterMode = PosterMode.NONE,
    val margins: PageMargins = PageMargins.NORMAL,
    val position: PagePosition = PagePosition.CENTER,
    val printBorderLines: Boolean = false,
    val collateCopies: Boolean = true,
    val pageRange: String = "All", // e.g. "All", "1-3", "1, 3, 5"
    val isLandscape: Boolean = false
)
