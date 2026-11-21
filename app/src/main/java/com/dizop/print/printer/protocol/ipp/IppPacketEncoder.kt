package com.dizop.print.printer.protocol.ipp

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/**
 * RFC 8011 & RFC 8010 Compliant Binary IPP (Internet Printing Protocol) Packet Encoder.
 * Creates standard IPP binary frames for Get-Printer-Attributes and Print-Job.
 */
object IppPacketEncoder {

    const val IPP_VERSION_2_0 = 0x0200
    const val IPP_VERSION_1_1 = 0x0101

    // Operations
    const val OP_PRINT_JOB = 0x0002
    const val OP_GET_PRINTER_ATTRIBUTES = 0x000B

    // Tag Constants
    const val TAG_OPERATION_ATTRIBUTES = 0x01.toByte()
    const val TAG_JOB_ATTRIBUTES = 0x02.toByte()
    const val TAG_END_OF_ATTRIBUTES = 0x03.toByte()
    const val TAG_PRINTER_ATTRIBUTES = 0x04.toByte()

    // Value Tags
    const val VALUE_CHARSET = 0x47.toByte()
    const val VALUE_NATURAL_LANG = 0x48.toByte()
    const val VALUE_MIME_TYPE = 0x49.toByte()
    const val VALUE_URI = 0x45.toByte()
    const val VALUE_NAME_WITHOUT_LANG = 0x42.toByte()
    const val VALUE_KEYWORD = 0x44.toByte()

    /**
     * Builds an IPP 2.0 Get-Printer-Attributes binary packet
     */
    fun createGetPrinterAttributesPacket(
        printerUri: String,
        requestId: Int = 1
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Version 2.0
        dos.writeShort(IPP_VERSION_2_0)
        // Operation: Get-Printer-Attributes
        dos.writeShort(OP_GET_PRINTER_ATTRIBUTES)
        // Request ID
        dos.writeInt(requestId)

        // Start Operation Attributes
        dos.writeByte(TAG_OPERATION_ATTRIBUTES.toInt())

        writeAttribute(dos, VALUE_CHARSET, "attributes-charset", "utf-8")
        writeAttribute(dos, VALUE_NATURAL_LANG, "attributes-natural-language", "en-us")
        writeAttribute(dos, VALUE_URI, "printer-uri", printerUri)
        writeAttribute(dos, VALUE_KEYWORD, "requested-attributes", "marker-levels")
        writeAttribute(dos, VALUE_KEYWORD, "", "marker-names")
        writeAttribute(dos, VALUE_KEYWORD, "", "printer-state")
        writeAttribute(dos, VALUE_KEYWORD, "", "printer-state-reasons")

        // End of Attributes Tag
        dos.writeByte(TAG_END_OF_ATTRIBUTES.toInt())
        dos.flush()
        return baos.toByteArray()
    }

    /**
     * Builds an IPP 2.0 Print-Job request header with comprehensive print options:
     * - Copies
     * - Media/Paper Size (A4, Letter, Legal, etc.)
     * - Sides/Duplex (one-sided, two-sided-long-edge, two-sided-short-edge)
     * - Color Mode (color, monochrome)
     */
    fun createPrintJobHeader(
        printerUri: String,
        jobName: String = "Dizop Print Job",
        userName: String = "Mobile User",
        documentFormat: String = "application/pdf",
        copies: Int = 1,
        paperSizeKeyword: String = "iso_a4_210x297mm",
        duplexKeyword: String = "one-sided",
        colorModeKeyword: String = "color",
        requestId: Int = 2
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        dos.writeShort(IPP_VERSION_2_0)
        dos.writeShort(OP_PRINT_JOB)
        dos.writeInt(requestId)

        // 1. Operation Attributes Group
        dos.writeByte(TAG_OPERATION_ATTRIBUTES.toInt())
        writeAttribute(dos, VALUE_CHARSET, "attributes-charset", "utf-8")
        writeAttribute(dos, VALUE_NATURAL_LANG, "attributes-natural-language", "en-us")
        writeAttribute(dos, VALUE_URI, "printer-uri", printerUri)
        writeAttribute(dos, VALUE_NAME_WITHOUT_LANG, "requesting-user-name", userName)
        writeAttribute(dos, VALUE_NAME_WITHOUT_LANG, "job-name", jobName)
        writeAttribute(dos, VALUE_MIME_TYPE, "document-format", documentFormat)

        // 2. Job Template Attributes Group
        dos.writeByte(TAG_JOB_ATTRIBUTES.toInt())
        // Copies (Integer tag = 0x21)
        dos.writeByte(0x21)
        val copiesAttrName = "copies".toByteArray(StandardCharsets.UTF_8)
        dos.writeShort(copiesAttrName.size)
        dos.write(copiesAttrName)
        dos.writeShort(4) // 4 bytes for integer
        dos.writeInt(copies.coerceAtLeast(1))

        // Media / Paper Size
        writeAttribute(dos, VALUE_KEYWORD, "media", paperSizeKeyword)
        // Sides / Duplex
        writeAttribute(dos, VALUE_KEYWORD, "sides", duplexKeyword)
        // Color Mode
        writeAttribute(dos, VALUE_KEYWORD, "print-color-mode", colorModeKeyword)

        // End of Attributes Group
        dos.writeByte(TAG_END_OF_ATTRIBUTES.toInt())
        dos.flush()
        return baos.toByteArray()
    }

    private fun writeAttribute(
        dos: DataOutputStream,
        valueTag: Byte,
        name: String,
        value: String
    ) {
        val nameBytes = name.toByteArray(StandardCharsets.UTF_8)
        val valueBytes = value.toByteArray(StandardCharsets.UTF_8)

        dos.writeByte(valueTag.toInt())
        dos.writeShort(nameBytes.size)
        dos.write(nameBytes)
        dos.writeShort(valueBytes.size)
        dos.write(valueBytes)
    }
}
