package com.dizop.print.printer.protocol.ipp

import android.net.Network
import android.util.Log
import com.dizop.print.data.InkLevel
import com.dizop.print.printer.config.ColorMode
import com.dizop.print.printer.config.DuplexMode
import com.dizop.print.printer.config.PaperSize
import com.dizop.print.printer.config.PrintJobConfiguration
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.SequenceInputStream
import java.util.concurrent.TimeUnit

data class IppPrinterAttributes(
    val state: String = "idle",
    val stateReasons: List<String> = emptyList(),
    val inkLevels: List<InkLevel> = emptyList()
)

class IppClient(private val baseClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()
) {

    private val IPP_MEDIA_TYPE = "application/ipp".toMediaType()

    private fun getClientForNetwork(network: Network?): OkHttpClient {
        return if (network != null) {
            baseClient.newBuilder()
                .socketFactory(network.socketFactory)
                .build()
        } else {
            baseClient
        }
    }

    /**
     * Executes an RFC 8011 IPP Print-Job operation over HTTP/1.1 POST
     * using the dynamically discovered printer endpoint path and user configuration.
     * Optionally binds to a specific Wi-Fi Network so mobile data never intercepts it.
     */
    fun printDocument(
        printerHost: String,
        printerPort: Int,
        resourcePath: String,
        pdfFile: File,
        jobName: String,
        config: PrintJobConfiguration? = null,
        network: Network? = null
    ): Result<Boolean> {
        return try {
            val client = getClientForNetwork(network)
            val cleanPath = resourcePath.trimStart('/')
            val printerUri = "ipp://$printerHost:$printerPort/$cleanPath"
            val httpUrl = "http://$printerHost:$printerPort/$cleanPath"

            val paperKeyword = when (config?.paperSize) {
                PaperSize.LETTER -> "na_letter_8.5x11in"
                PaperSize.LEGAL -> "na_legal_8.5x14in"
                PaperSize.A3 -> "iso_a3_297x420mm"
                PaperSize.A5 -> "iso_a5_148x210mm"
                PaperSize.PHOTO_4X6 -> "na_index-4x6_4x6in"
                PaperSize.PHOTO_5X7 -> "na_5x7_5x7in"
                PaperSize.PHOTO_PASSPORT -> "om_passport_35x45mm"
                PaperSize.PHOTO_2X2 -> "om_id_51x51mm"
                else -> "iso_a4_210x297mm"
            }

            val duplexKeyword = when (config?.duplexMode) {
                DuplexMode.TWO_SIDED_LONG_EDGE -> "two-sided-long-edge"
                DuplexMode.TWO_SIDED_SHORT_EDGE -> "two-sided-short-edge"
                DuplexMode.MANUAL_ODD_ONLY, DuplexMode.MANUAL_EVEN_ONLY -> "one-sided"
                else -> "one-sided"
            }

            val colorKeyword = when (config?.colorMode) {
                ColorMode.MONOCHROME -> "monochrome"
                else -> "color"
            }

            val headerBytes = IppPacketEncoder.createPrintJobHeader(
                printerUri = printerUri,
                jobName = jobName,
                documentFormat = "application/pdf",
                copies = config?.copies ?: 1,
                paperSizeKeyword = paperKeyword,
                duplexKeyword = duplexKeyword,
                colorModeKeyword = colorKeyword
            )

            val combinedStream = SequenceInputStream(
                ByteArrayInputStream(headerBytes),
                FileInputStream(pdfFile)
            )

            val totalLength = headerBytes.size.toLong() + pdfFile.length()

            val requestBody = object : okhttp3.RequestBody() {
                override fun contentType() = IPP_MEDIA_TYPE
                override fun contentLength() = totalLength
                override fun writeTo(sink: okio.BufferedSink) {
                    val buffer = ByteArray(8192)
                    var read: Int
                    combinedStream.use { stream ->
                        while (stream.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                        }
                    }
                }
            }

            val request = Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .addHeader("User-Agent", "Dizop/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.size >= 8) {
                    val parsed = IppResponseParser.parse(bytes)
                    if (parsed.status == IppResponseStatus.SUCCESS) {
                        return Result.success(true)
                    } else {
                        return Result.failure(Exception("IPP Error (0x${Integer.toHexString(parsed.statusCode)}): ${parsed.stateReasons.joinToString()}"))
                    }
                }
                return Result.failure(Exception("Empty or invalid IPP response payload from printer"))
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Log.e("IppClient", "IPP Print error", e)
            Result.failure(e)
        }
    }

    /**
     * Queries real printer status, capabilities, and supply levels via RFC 8011 Get-Printer-Attributes
     */
    fun queryPrinterAttributes(
        printerHost: String,
        printerPort: Int,
        resourcePath: String = "ipp/print",
        network: Network? = null
    ): ParsedIppResponse {
        return try {
            val client = getClientForNetwork(network)
            val cleanPath = resourcePath.trimStart('/')
            val printerUri = "ipp://$printerHost:$printerPort/$cleanPath"
            val httpUrl = "http://$printerHost:$printerPort/$cleanPath"

            val packet = IppPacketEncoder.createGetPrinterAttributesPacket(printerUri)
            val request = Request.Builder()
                .url(httpUrl)
                .post(packet.toRequestBody(IPP_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    IppResponseParser.parse(bytes)
                } else {
                    ParsedIppResponse(
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
            } else {
                ParsedIppResponse(
                    status = IppResponseStatus.PRINTER_ERROR,
                    statusCode = response.code,
                    requestId = -1,
                    printerState = "unknown",
                    stateReasons = listOf(response.message),
                    isAcceptingJobs = false,
                    makeAndModel = "",
                    inkLevels = emptyList(),
                    supportedFormats = emptyList(),
                    supportedMedia = emptyList(),
                    supportedSides = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.d("IppClient", "Query attributes unavailable for $printerHost: ${e.message}")
            ParsedIppResponse(
                status = IppResponseStatus.UNKNOWN,
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
