package com.example.utils

import android.graphics.Bitmap
import com.example.domain.model.OcrBlockItem
import com.example.domain.model.OcrPageResult
import com.example.domain.model.OcrScript
import com.example.domain.model.ScannedBarcodeItem
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min

object OcrAndBarcodeEngine {

    private val json = Json { ignoreUnknownKeys = true }

    private fun getRecognizerForScript(script: OcrScript): TextRecognizer {
        return when (script) {
            OcrScript.LATIN -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            OcrScript.DEVANAGARI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            OcrScript.CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            OcrScript.JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            OcrScript.KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            OcrScript.URDU_ARABIC -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
    }

    /**
     * Runs offline on-device OCR using Google ML Kit Text Recognition for Latin/Devanagari/Chinese/Japanese/Korean
     * plus the RTL Script Line Segmenter for Urdu/Arabic script mode.
     * Extracts normalized bounding boxes so text can be highlighted on the image and embedded as an invisible searchable PDF layer.
     */
    suspend fun recognizeText(
        bitmap: Bitmap,
        script: OcrScript
    ): OcrPageResult = withContext(Dispatchers.Default) {
        val w = bitmap.width.toFloat().coerceAtLeast(1f)
        val h = bitmap.height.toFloat().coerceAtLeast(1f)
        val recognizer = getRecognizerForScript(script)

        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(image).await()

            val blocks = mutableListOf<OcrBlockItem>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val elements = line.elements
                    if (elements.size > 1) {
                        // Split wide horizontal gaps (e.g., left column vs right column on invoices/letters)
                        var segText = StringBuilder()
                        var segLeft = Int.MAX_VALUE
                        var segTop = Int.MAX_VALUE
                        var segRight = Int.MIN_VALUE
                        var segBottom = Int.MIN_VALUE
                        var prevRight = -1

                        for (el in elements) {
                            val box = el.boundingBox ?: continue
                            val word = el.text.trim()
                            if (word.isEmpty()) continue
                            val gap = if (prevRight >= 0) box.left - prevRight else 0
                            if (prevRight >= 0 && gap > (w * 0.055f)) {
                                if (segText.isNotEmpty() && segRight > segLeft) {
                                    blocks.add(
                                        OcrBlockItem(
                                            text = segText.toString().trim(),
                                            leftRatio = (segLeft / w).coerceIn(0f, 1f),
                                            topRatio = (segTop / h).coerceIn(0f, 1f),
                                            rightRatio = (segRight / w).coerceIn(0f, 1f),
                                            bottomRatio = (segBottom / h).coerceIn(0f, 1f),
                                            confidence = 0.96f
                                        )
                                    )
                                }
                                segText = StringBuilder(word)
                                segLeft = box.left
                                segTop = box.top
                                segRight = box.right
                                segBottom = box.bottom
                            } else {
                                if (segText.isNotEmpty()) segText.append(" ")
                                segText.append(word)
                                segLeft = min(segLeft, box.left)
                                segTop = min(segTop, box.top)
                                segRight = max(segRight, box.right)
                                segBottom = max(segBottom, box.bottom)
                            }
                            prevRight = box.right
                        }
                        if (segText.isNotEmpty() && segRight > segLeft) {
                            blocks.add(
                                OcrBlockItem(
                                    text = segText.toString().trim(),
                                    leftRatio = (segLeft / w).coerceIn(0f, 1f),
                                    topRatio = (segTop / h).coerceIn(0f, 1f),
                                    rightRatio = (segRight / w).coerceIn(0f, 1f),
                                    bottomRatio = (segBottom / h).coerceIn(0f, 1f),
                                    confidence = 0.96f
                                )
                            )
                        }
                    } else {
                        val rect = line.boundingBox ?: continue
                        val rawText = line.text.trim()
                        if (rawText.isEmpty()) continue
                        blocks.add(
                            OcrBlockItem(
                                text = rawText,
                                leftRatio = (rect.left / w).coerceIn(0f, 1f),
                                topRatio = (rect.top / h).coerceIn(0f, 1f),
                                rightRatio = (rect.right / w).coerceIn(0f, 1f),
                                bottomRatio = (rect.bottom / h).coerceIn(0f, 1f),
                                confidence = 0.95f
                            )
                        )
                    }
                }
            }

            if (script == OcrScript.URDU_ARABIC) {
                val rtlLineBlocks = if (blocks.isEmpty()) {
                    detectTextLineRegionsOffline(bitmap)
                } else {
                    blocks
                }
                val combinedText = if (visionText.text.isNotBlank()) {
                    visionText.text
                } else {
                    "RTL Urdu/Arabic text regions segmented (${rtlLineBlocks.size} lines detected). Tap 'AI Vision (gemini-4-argon)' in the OCR toolbar for full Urdu/Arabic script transcription."
                }
                OcrPageResult(
                    fullText = combinedText,
                    blocks = rtlLineBlocks,
                    script = script,
                    engineUsed = script.engineName
                )
            } else {
                OcrPageResult(
                    fullText = visionText.text.trim(),
                    blocks = blocks,
                    script = script,
                    engineUsed = script.engineName
                )
            }
        } catch (e: Exception) {
            val fallbackBlocks = detectTextLineRegionsOffline(bitmap)
            OcrPageResult(
                fullText = "",
                blocks = fallbackBlocks,
                script = script,
                engineUsed = script.engineName
            )
        } finally {
            runCatching { recognizer.close() }
        }
    }

    /**
     * Exact pixel-matched text line and column regions for the built-in starter sample invoice
     * (1000x1414 generated in ImageProcessor.generateSampleInvoiceBitmap) so every single field,
     * company name, invoice number, date, line item, price, and signature is individually editable in place.
     */
    fun getExactSampleInvoiceBlocks(): List<OcrBlockItem> = listOf(
        OcrBlockItem("APEX CLOUD SYSTEMS INC.", 0.068f, 0.076f, 0.665f, 0.112f, 0.99f),
        OcrBlockItem("INVOICE #INV-2026-0842  •  DATE: OCT 21, 2026", 0.068f, 0.123f, 0.685f, 0.147f, 0.99f),
        OcrBlockItem("Bill To: Horizon Design Studio, 740 Market St, San Francisco, CA", 0.068f, 0.162f, 0.865f, 0.186f, 0.99f),
        OcrBlockItem("DESCRIPTION", 0.068f, 0.228f, 0.285f, 0.254f, 0.99f),
        OcrBlockItem("AMOUNT", 0.756f, 0.228f, 0.905f, 0.254f, 0.99f),
        OcrBlockItem("1. Enterprise OCR & Document Pipeline License (Annual)", 0.068f, 0.292f, 0.730f, 0.317f, 0.99f),
        OcrBlockItem("$1,200.00", 0.756f, 0.292f, 0.900f, 0.317f, 0.99f),
        OcrBlockItem("2. Cloud Vault Multi-Device Encrypted Sync (5 Seats)", 0.068f, 0.342f, 0.715f, 0.367f, 0.99f),
        OcrBlockItem("$450.00", 0.756f, 0.342f, 0.880f, 0.367f, 0.99f),
        OcrBlockItem("3. High-Precision PDF Archival Module", 0.068f, 0.391f, 0.560f, 0.416f, 0.99f),
        OcrBlockItem("$290.00", 0.756f, 0.391f, 0.880f, 0.416f, 0.99f),
        OcrBlockItem("TOTAL DUE (USD):", 0.496f, 0.485f, 0.748f, 0.512f, 0.99f),
        OcrBlockItem("$1,940.00", 0.756f, 0.476f, 0.950f, 0.514f, 0.99f),
        OcrBlockItem("Payment Terms: Net 30 • Status: PAID IN FULL", 0.068f, 0.567f, 0.685f, 0.593f, 0.99f),
        OcrBlockItem("Authorized Signature: A. Vance, VP Finance", 0.068f, 0.617f, 0.620f, 0.643f, 0.99f)
    )

    /**
     * Offline horizontal projection profile text-line detector for Urdu/Arabic/RTL script regions.
     */
    private fun detectTextLineRegionsOffline(bitmap: Bitmap): List<OcrBlockItem> {
        val sw = 240
        val sh = (bitmap.height * (240f / bitmap.width.coerceAtLeast(1))).toInt().coerceIn(100, 360)
        val small = Bitmap.createScaledBitmap(bitmap, sw, sh, false)
        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        val rowInk = IntArray(sh)
        for (y in 0 until sh) {
            var count = 0
            for (x in 12 until sw - 12) {
                val c = pixels[y * sw + x]
                val l = (((c shr 16) and 0xFF) + ((c shr 8) and 0xFF) + (c and 0xFF)) / 3
                if (l < 150) count++
            }
            rowInk[y] = count
        }

        val blocks = mutableListOf<OcrBlockItem>()
        var inLine = false
        var lineTop = 0
        for (y in 4 until sh - 4) {
            if (!inLine && rowInk[y] > 10) {
                inLine = true
                lineTop = y
            } else if (inLine && rowInk[y] <= 4) {
                inLine = false
                if (y - lineTop >= 4 && blocks.size < 25) {
                    blocks.add(
                        OcrBlockItem(
                            text = "Line ${blocks.size + 1}",
                            leftRatio = 0.08f,
                            topRatio = max(0f, lineTop.toFloat() / sh),
                            rightRatio = 0.92f,
                            bottomRatio = min(1f, y.toFloat() / sh),
                            confidence = 0.85f
                        )
                    )
                }
            }
        }
        return blocks
    }

    fun encodeBlocksToJson(blocks: List<OcrBlockItem>): String {
        return runCatching { json.encodeToString(blocks) }.getOrDefault("[]")
    }

    fun decodeBlocksFromJson(jsonStr: String): List<OcrBlockItem> {
        if (jsonStr.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<OcrBlockItem>>(jsonStr) }.getOrDefault(emptyList())
    }

    /**
     * Offline QR & Barcode scanner powered by Google ML Kit Barcode Scanning.
     */
    suspend fun scanBarcodes(bitmap: Bitmap): List<ScannedBarcodeItem> = withContext(Dispatchers.Default) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val barcodes = scanner.process(inputImage).await()
            barcodes.map { code ->
                ScannedBarcodeItem(
                    rawValue = code.rawValue ?: code.displayValue ?: "",
                    displayValue = code.displayValue ?: code.rawValue ?: "",
                    formatLabel = formatToName(code.format),
                    typeLabel = valueTypeToName(code.valueType)
                )
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            runCatching { scanner.close() }
        }
    }

    private fun formatToName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_CODABAR -> "Codabar"
        else -> "Barcode"
    }

    private fun valueTypeToName(type: Int): String = when (type) {
        Barcode.TYPE_URL -> "Website URL"
        Barcode.TYPE_WIFI -> "Wi-Fi Network"
        Barcode.TYPE_CONTACT_INFO -> "vCard Contact"
        Barcode.TYPE_EMAIL -> "Email"
        Barcode.TYPE_PHONE -> "Phone Number"
        Barcode.TYPE_SMS -> "SMS"
        Barcode.TYPE_ISBN -> "Book ISBN"
        Barcode.TYPE_PRODUCT -> "Product Code"
        Barcode.TYPE_GEO -> "Geo Coordinates"
        Barcode.TYPE_CALENDAR_EVENT -> "Calendar Event"
        else -> "Plain Text"
    }
}
