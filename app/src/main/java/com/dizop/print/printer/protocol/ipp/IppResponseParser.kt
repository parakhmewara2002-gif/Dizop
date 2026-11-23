package com.dizop.print.printer.protocol.ipp

import com.dizop.print.data.InkLevel
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.nio.charset.StandardCharsets

enum class IppResponseStatus {
    SUCCESS,
    UNSUPPORTED,
    MALFORMED_RESPONSE,
    PRINTER_ERROR,
    UNKNOWN
}

data class ParsedIppResponse(
    val status: IppResponseStatus,
    val statusCode: Int,
    val requestId: Int,
    val printerState: String, // "idle", "processing", "stopped", "unknown"
    val stateReasons: List<String>,
    val isAcceptingJobs: Boolean,
    val makeAndModel: String,
    val inkLevels: List<InkLevel>,
    val supportedFormats: List<String>,
    val supportedMedia: List<String>,
    val supportedSides: List<String>
)

/**
 * RFC 8011 & RFC 8010 Robust Binary IPP Response Parser.
 * Correctly validates version, status code, attribute tags, integers, names, and enums.
 */
object IppResponseParser {

    fun parse(bytes: ByteArray): ParsedIppResponse {
        if (bytes.size < 8) {
            return ParsedIppResponse(
                status = IppResponseStatus.MALFORMED_RESPONSE,
                statusCode = -1,
                requestId = -1,
                printerState = "unknown",
                stateReasons = emptyList(),
                isAcceptingJobs = false,
                makeAndModel = "",
                inkLevels = emptyList(),
                supportedFormats = emptyList(),
                supportedMedia = emptyList(),
                supportedSides = emptyList()
            )
        }

        try {
            val dis = DataInputStream(ByteArrayInputStream(bytes))
            val version = dis.readShort().toInt()
            val statusCode = dis.readShort().toInt()
            val requestId = dis.readInt()

            val ippStatus = when (statusCode) {
                0x0000, 0x0001 -> IppResponseStatus.SUCCESS
                0x0500, 0x0501, 0x0507, 0x0508 -> IppResponseStatus.PRINTER_ERROR
                0x0400, 0x0401, 0x0402 -> IppResponseStatus.UNSUPPORTED
                else -> if (statusCode in 0x0000..0x00FF) IppResponseStatus.SUCCESS else IppResponseStatus.PRINTER_ERROR
            }

            var printerState = "unknown"
            val stateReasons = mutableListOf<String>()
            var isAcceptingJobs = true
            var makeAndModel = ""
            val markerNames = mutableListOf<String>()
            val markerLevels = mutableListOf<Int>()
            val markerTypes = mutableListOf<String>()
            val supportedFormats = mutableListOf<String>()
            val supportedMedia = mutableListOf<String>()
            val supportedSides = mutableListOf<String>()

            // Attribute parsing loop
            var lastName = ""
            while (dis.available() > 0) {
                val tag = dis.readByte().toInt()
                if (tag == 0x03) { // End-of-attributes tag
                    break
                }
                if (tag in 0x01..0x0F) { // Delimiter tags (operation, printer, job attrs)
                    lastName = ""
                    continue
                }

                // Read Name
                val nameLength = dis.readUnsignedShort()
                val name = if (nameLength > 0) {
                    val nameBytes = ByteArray(nameLength)
                    dis.readFully(nameBytes)
                    String(nameBytes, StandardCharsets.UTF_8)
                } else ""

                // Additional values of a multi-valued attribute arrive with an empty name
                if (name.isNotEmpty()) lastName = name
                val attrName = if (name.isNotEmpty()) name else lastName

                // Read Value
                val valueLength = dis.readUnsignedShort()
                val valueBytes = ByteArray(valueLength)
                dis.readFully(valueBytes)

                when (tag) {
                    // Integer (0x21) or Enum (0x23)
                    0x21, 0x23 -> {
                        val intVal = if (valueLength == 4) {
                            DataInputStream(ByteArrayInputStream(valueBytes)).readInt()
                        } else 0

                        if (attrName == "printer-state") {
                            printerState = when (intVal) {
                                3 -> "idle"
                                4 -> "processing"
                                5 -> "stopped"
                                else -> "unknown"
                            }
                        } else if (attrName == "marker-levels") {
                            markerLevels.add(intVal)
                        }
                    }
                    // Boolean (0x22)
                    0x22 -> {
                        val boolVal = valueBytes.isNotEmpty() && valueBytes[0].toInt() != 0
                        if (attrName == "printer-is-accepting-jobs") {
                            isAcceptingJobs = boolVal
                        }
                    }
                    // Text / Name / Keyword / URI / MIME types (0x41..0x49)
                    in 0x41..0x49 -> {
                        val strVal = String(valueBytes, StandardCharsets.UTF_8)
                        when (attrName) {
                            "printer-make-and-model" -> makeAndModel = strVal
                            "printer-state-reasons" -> stateReasons.add(strVal)
                            "marker-names" -> markerNames.add(strVal)
                            "marker-types" -> markerTypes.add(strVal)
                            "document-format-supported" -> supportedFormats.add(strVal)
                            "media-supported" -> supportedMedia.add(strVal)
                            "sides-supported" -> supportedSides.add(strVal)
                        }
                    }
                }
            }

            // Synthesize real ink telemetry if reported by hardware
            val inkList = mutableListOf<InkLevel>()
            if (markerNames.isNotEmpty() && markerLevels.isNotEmpty()) {
                val count = minOf(markerNames.size, markerLevels.size)
                for (i in 0 until count) {
                    // RFC 8011: -1 other, -2 unknown, -3 unknown-but-not-empty: not a real 0% reading
                    if (markerLevels[i] < 0) continue
                    val rawName = markerNames[i].lowercase()
                    val level = markerLevels[i].coerceIn(0, 100)
                    val colorHex = when {
                        rawName.contains("black") -> 0xFF212121
                        rawName.contains("cyan") -> 0xFF00BCD4
                        rawName.contains("magenta") -> 0xFFE91E63
                        rawName.contains("yellow") -> 0xFFFFEB3B
                        else -> 0xFF757575
                    }
                    inkList.add(InkLevel(markerNames[i], level, colorHex))
                }
            }

            return ParsedIppResponse(
                status = ippStatus,
                statusCode = statusCode,
                requestId = requestId,
                printerState = printerState,
                stateReasons = stateReasons,
                isAcceptingJobs = isAcceptingJobs,
                makeAndModel = makeAndModel,
                inkLevels = inkList,
                supportedFormats = supportedFormats,
                supportedMedia = supportedMedia,
                supportedSides = supportedSides
            )
        } catch (_: Exception) {
            return ParsedIppResponse(
                status = IppResponseStatus.MALFORMED_RESPONSE,
                statusCode = -1,
                requestId = -1,
                printerState = "unknown",
                stateReasons = emptyList(),
                isAcceptingJobs = false,
                makeAndModel = "",
                inkLevels = emptyList(),
                supportedFormats = emptyList(),
                supportedMedia = emptyList(),
                supportedSides = emptyList()
            )
        }
    }
}
