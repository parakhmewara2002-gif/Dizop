package com.dizop.print.data

enum class PrinterProtocol {
    IPP,            // Internet Printing Protocol (port 631) - Modern standard
    RAW_JETDIRECT,  // Raw 9100 port - HP, Canon, Brother, Epson
    ANDROID_SPOOLER // Android Print Service fallback
}

enum class PrinterBrand {
    HP,
    CANON,
    EPSON,
    BROTHER,
    XEROX,
    RICOH,
    GENERIC
}

data class InkLevel(
    val colorName: String,
    val levelPercentage: Int, // 0..100
    val colorHex: Long
)

data class DiscoveredPrinter(
    val id: String,
    val name: String,
    val model: String,
    val hostAddress: String,
    val port: Int = 631,
    val resourcePath: String = "ipp/print",
    val brand: PrinterBrand = PrinterBrand.GENERIC,
    val protocol: PrinterProtocol = PrinterProtocol.IPP,
    val isOnline: Boolean = true,
    val isReady: Boolean = true,
    val inkLevels: List<InkLevel> = emptyList(),
    val supportsColor: Boolean = true,
    val supportsDuplex: Boolean = true
)
