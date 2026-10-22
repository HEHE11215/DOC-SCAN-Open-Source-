package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.domain.model.OcrBlockItem
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PdfPageSize
import com.example.domain.model.PdfQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.min

data class PdfPageInput(
    val bitmap: Bitmap,
    val ocrBlocks: List<OcrBlockItem> = emptyList(),
    val fullOcrText: String = ""
)

object PdfEngine {

    /**
     * Generates a multi-page PDF with configurable Page Size (A4, Letter, Original),
     * Compression Quality, Watermark, Page Numbers, Signature overlay, and
     * an embedded Searchable OCR text layer.
     */
    suspend fun generatePdfFromPages(
        context: Context,
        outputFileName: String,
        pages: List<PdfPageInput>,
        pageSize: PdfPageSize = PdfPageSize.A4,
        quality: PdfQuality = PdfQuality.HIGH,
        isSearchable: Boolean = true,
        watermarkText: String = "",
        watermarkOpacity: Int = 38,
        pageNumberFormat: PageNumberFormat? = null,
        pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_RIGHT,
        signatureBitmap: Bitmap? = null,
        signaturePageIndex: Int = -1,
        signatureNormX: Float = 0.62f,
        signatureNormY: Float = 0.78f,
        signatureScale: Float = 0.28f
    ): File = withContext(Dispatchers.IO) {
        val pdfDir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val cleanName = outputFileName.replace(Regex("[^a-zA-Z0-9_\\-.]"), "_").removeSuffix(".pdf")
        val outFile = File(pdfDir, "${cleanName}_${System.currentTimeMillis()}.pdf")

        val pdfDocument = PdfDocument()
        try {
            pages.forEachIndexed { index, pageInput ->
                val compressedBmp = compressAndScaleBitmap(pageInput.bitmap, quality)
                val pageW = if (pageSize == PdfPageSize.ORIGINAL) compressedBmp.width else pageSize.widthPt
                val pageH = if (pageSize == PdfPageSize.ORIGINAL) compressedBmp.height else pageSize.heightPt

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val margin = if (pageSize == PdfPageSize.ORIGINAL) 0f else 18f
                val availW = pageW - margin * 2f
                val availH = pageH - margin * 2f
                val scale = min(availW / compressedBmp.width.toFloat(), availH / compressedBmp.height.toFloat())
                val drawW = compressedBmp.width * scale
                val drawH = compressedBmp.height * scale
                val left = (pageW - drawW) / 2f
                val top = (pageH - drawH) / 2f
                val destRect = RectF(left, top, left + drawW, top + drawH)

                val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(compressedBmp, null, destRect, bmpPaint)

                // Invisible Searchable OCR Text Layer
                if (isSearchable) {
                    val ocrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        // Alpha 1 embeds real selectable text glyphs in the PDF stream without visually obscuring the scan
                        color = Color.argb(1, 0, 0, 0)
                        style = Paint.Style.FILL
                    }
                    if (pageInput.ocrBlocks.isNotEmpty()) {
                        for (block in pageInput.ocrBlocks) {
                            val bx = left + block.leftRatio * drawW
                            val by = top + block.bottomRatio * drawH
                            val bh = ((block.bottomRatio - block.topRatio) * drawH).coerceIn(8f, 36f)
                            ocrPaint.textSize = bh * 0.72f
                            canvas.drawText(block.text.replace("\n", " "), bx, by, ocrPaint)
                        }
                    } else if (pageInput.fullOcrText.isNotBlank()) {
                        ocrPaint.textSize = 9f
                        pageInput.fullOcrText.lines().take(70).forEachIndexed { lineIdx, line ->
                            canvas.drawText(line, left + 8f, top + 16f + lineIdx * 11f, ocrPaint)
                        }
                    }
                }

                // Optional Signature Overlay
                if (signatureBitmap != null && (signaturePageIndex == -1 || signaturePageIndex == index)) {
                    val sigW = pageW * signatureScale
                    val sigRatio = signatureBitmap.height.toFloat() / signatureBitmap.width.coerceAtLeast(1)
                    val sigH = sigW * sigRatio
                    val sx = (signatureNormX * (pageW - sigW)).coerceIn(8f, pageW - sigW - 8f)
                    val sy = (signatureNormY * (pageH - sigH)).coerceIn(8f, pageH - sigH - 8f)
                    val sigRect = RectF(sx, sy, sx + sigW, sy + sigH)
                    canvas.drawBitmap(signatureBitmap, null, sigRect, bmpPaint)
                }

                // Optional Diagonal Watermark
                if (watermarkText.isNotBlank()) {
                    val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb(watermarkOpacity.coerceIn(15, 180), 30, 62, 98)
                        textSize = (pageW * 0.065f).coerceIn(22f, 54f)
                        isFakeBoldText = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.save()
                    canvas.rotate(-32f, pageW / 2f, pageH / 2f)
                    canvas.drawText(watermarkText, pageW / 2f, pageH / 2f, wmPaint)
                    canvas.restore()
                }

                // Optional Page Numbers
                if (pageNumberFormat != null) {
                    val total = pages.size
                    val numStr = when (pageNumberFormat) {
                        PageNumberFormat.PAGE_X_OF_Y -> "Page ${index + 1} of $total"
                        PageNumberFormat.SLASH -> "${index + 1} / $total"
                        PageNumberFormat.DASHED -> "- ${index + 1} -"
                    }
                    val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#334155")
                        textSize = 11f
                        isFakeBoldText = true
                    }
                    when (pageNumberPosition) {
                        PageNumberPosition.BOTTOM_RIGHT -> {
                            numPaint.textAlign = Paint.Align.RIGHT
                            canvas.drawText(numStr, pageW - 22f, pageH - 14f, numPaint)
                        }
                        PageNumberPosition.BOTTOM_CENTER -> {
                            numPaint.textAlign = Paint.Align.CENTER
                            canvas.drawText(numStr, pageW / 2f, pageH - 14f, numPaint)
                        }
                        PageNumberPosition.TOP_RIGHT -> {
                            numPaint.textAlign = Paint.Align.RIGHT
                            canvas.drawText(numStr, pageW - 22f, 22f, numPaint)
                        }
                    }
                }

                pdfDocument.finishPage(page)
            }

            FileOutputStream(outFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
        outFile
    }

    private fun compressAndScaleBitmap(source: Bitmap, quality: PdfQuality): Bitmap {
        val maxDim = quality.maxDimension
        val scaled = if (source.width > maxDim || source.height > maxDim) {
            val ratio = min(maxDim.toFloat() / source.width, maxDim.toFloat() / source.height)
            val nw = (source.width * ratio).toInt().coerceAtLeast(100)
            val nh = (source.height * ratio).toInt().coerceAtLeast(100)
            Bitmap.createScaledBitmap(source, nw, nh, true)
        } else {
            source
        }
        if (quality == PdfQuality.MAX) return scaled
        val bos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, bos)
        val bytes = bos.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: scaled
    }

    /**
     * Renders all pages (or selected indices) of an existing PDF file into high-resolution Bitmaps via Android's PdfRenderer.
     */
    suspend fun renderPdfPages(
        pdfFile: File,
        scaleFactor: Float = 2.0f,
        maxPages: Int = 100
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext emptyList()
        val result = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val count = min(renderer.pageCount, maxPages)
                for (i in 0 until count) {
                    renderer.openPage(i).use { page ->
                        val w = (page.width * scaleFactor).toInt().coerceIn(200, 2400)
                        val h = (page.height * scaleFactor).toInt().coerceIn(200, 3200)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        result.add(bmp)
                    }
                }
            }
        }
        result
    }

    suspend fun getPdfPageCount(pdfFile: File): Int = withContext(Dispatchers.IO) {
        if (!pdfFile.exists()) return@withContext 0
        runCatching {
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            } ?: 0
        }.getOrDefault(0)
    }

    /**
     * Imports an external PDF Uri into internal storage and renders its pages.
     */
    suspend fun importExternalPdfUri(context: Context, uri: Uri): Pair<File, List<Bitmap>>? = withContext(Dispatchers.IO) {
        try {
            val pdfDir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val importedFile = File(pdfDir, "imported_${System.currentTimeMillis()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(importedFile).use { output ->
                    input.copyTo(output)
                }
            }
            val bitmaps = renderPdfPages(importedFile, scaleFactor = 1.8f)
            if (bitmaps.isEmpty()) return@withContext null
            importedFile to bitmaps
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Merges multiple PDF files into a single unified PDF.
     */
    suspend fun mergePdfFiles(
        context: Context,
        sourceFiles: List<File>,
        mergedTitle: String,
        quality: PdfQuality = PdfQuality.HIGH
    ): File = withContext(Dispatchers.IO) {
        val allPages = mutableListOf<PdfPageInput>()
        for (file in sourceFiles) {
            val pages = renderPdfPages(file, scaleFactor = 1.8f)
            pages.forEach { bmp ->
                allPages.add(PdfPageInput(bitmap = bmp))
            }
        }
        generatePdfFromPages(
            context = context,
            outputFileName = mergedTitle,
            pages = allPages,
            pageSize = PdfPageSize.A4,
            quality = quality,
            isSearchable = false
        )
    }

    /**
     * Splits a PDF file into a subset of pages (1-indexed inclusive range).
     */
    suspend fun splitPdfRange(
        context: Context,
        sourceFile: File,
        outputTitle: String,
        startPageOneBased: Int,
        endPageOneBased: Int,
        quality: PdfQuality = PdfQuality.HIGH
    ): File = withContext(Dispatchers.IO) {
        val allBitmaps = renderPdfPages(sourceFile, scaleFactor = 1.9f)
        val safeStart = (startPageOneBased - 1).coerceIn(0, (allBitmaps.size - 1).coerceAtLeast(0))
        val safeEnd = (endPageOneBased - 1).coerceIn(safeStart, (allBitmaps.size - 1).coerceAtLeast(0))
        val subBitmaps = if (allBitmaps.isNotEmpty()) {
            allBitmaps.subList(safeStart, safeEnd + 1)
        } else {
            emptyList()
        }
        generatePdfFromPages(
            context = context,
            outputFileName = outputTitle,
            pages = subBitmaps.map { PdfPageInput(it) },
            pageSize = PdfPageSize.A4,
            quality = quality
        )
    }

    /**
     * Exports all pages of a PDF as individual JPG or PNG image files in cache/export directory.
     */
    suspend fun exportPdfToImages(
        context: Context,
        pdfFile: File,
        baseName: String,
        asPng: Boolean = false
    ): List<File> = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exported_images").apply { mkdirs() }
        val bitmaps = renderPdfPages(pdfFile, scaleFactor = 2.0f)
        val ext = if (asPng) "png" else "jpg"
        val format = if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        bitmaps.mapIndexed { idx, bmp ->
            val file = File(exportDir, "${baseName}_page_${idx + 1}.$ext")
            FileOutputStream(file).use { out ->
                bmp.compress(format, 95, out)
            }
            file
        }
    }

    fun hashPassword(password: String): String {
        if (password.isBlank()) return ""
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
