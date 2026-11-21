package com.dizop.print.printer.config

enum class PaperSize(val displayName: String, val widthMm: Float, val heightMm: Float) {
    A4("A4 (210 x 297 mm)", 210f, 297f),
    A3("A3 (297 x 420 mm)", 297f, 420f),
    A5("A5 (148 x 210 mm)", 148f, 210f),
    LETTER("Letter (8.5 x 11 in)", 215.9f, 279.4f),
    LEGAL("Legal (8.5 x 14 in)", 215.9f, 355.6f),
    PHOTO_4X6("Photo 4 x 6 in", 101.6f, 152.4f)
}

enum class ColorMode(val displayName: String) {
    COLOR("Full Color"),
    MONOCHROME("Black & White (Grayscale)")
}

enum class DuplexMode(val displayName: String) {
    ONE_SIDED("Single-Sided"),
    TWO_SIDED_LONG_EDGE("2-Sided (Flip on Long Edge)"),
    TWO_SIDED_SHORT_EDGE("2-Sided (Flip on Short Edge)")
}

enum class PageScaling(val displayName: String) {
    FIT_TO_PAGE("Fit to Printable Area"),
    FILL_PAGE("Fill Page / Bleed"),
    ACTUAL_SIZE("100% Actual Size")
}

data class PrintJobConfiguration(
    val copies: Int = 1,
    val paperSize: PaperSize = PaperSize.A4,
    val colorMode: ColorMode = ColorMode.COLOR,
    val duplexMode: DuplexMode = DuplexMode.ONE_SIDED,
    val pageScaling: PageScaling = PageScaling.FIT_TO_PAGE,
    val pageRange: String = "All", // e.g., "All", "1-3", "2, 4"
    val isLandscape: Boolean = false
)
