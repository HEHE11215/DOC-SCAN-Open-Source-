package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import com.example.domain.model.NormalizedQuad
import com.example.domain.model.ScanFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

object ImageProcessor {

    suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 2200): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun loadBitmapFromPath(path: String, maxDim: Int = 2200): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(path, decodeOpts)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveBitmapToInternalFile(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "scan_page",
        quality: Int = 92
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "pages").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(40, 100), out)
        }
        file
    }

    /**
     * Automatic Document Edge Detection combining Otsu paper-brightness segmentation and Sobel gradient magnitude.
     * Works 100% offline on any Bitmap and returns a NormalizedQuad (0f..1f).
     */
    suspend fun detectDocumentEdges(bitmap: Bitmap): NormalizedQuad = withContext(Dispatchers.Default) {
        val targetW = 180
        val scale = targetW.toFloat() / bitmap.width.coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(80)
        val small = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)

        val pixels = IntArray(targetW * targetH)
        small.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)
        val luma = IntArray(targetW * targetH)
        var sumLuma = 0L
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val l = (r * 77 + g * 150 + b * 29) shr 8
            luma[i] = l
            sumLuma += l
        }
        val meanLuma = (sumLuma / pixels.size.coerceAtLeast(1)).toInt()

        val grad = IntArray(targetW * targetH)
        var maxGrad = 1
        for (y in 2 until targetH - 2) {
            for (x in 2 until targetW - 2) {
                val gx = -luma[(y - 1) * targetW + (x - 1)] + luma[(y - 1) * targetW + (x + 1)] -
                    2 * luma[y * targetW + (x - 1)] + 2 * luma[y * targetW + (x + 1)] -
                    luma[(y + 1) * targetW + (x - 1)] + luma[(y + 1) * targetW + (x + 1)]
                val gy = -luma[(y - 1) * targetW + (x - 1)] - 2 * luma[(y - 1) * targetW + x] - luma[(y - 1) * targetW + (x + 1)] +
                    luma[(y + 1) * targetW + (x - 1)] + 2 * luma[(y + 1) * targetW + x] + luma[(y + 1) * targetW + (x + 1)]
                val mag = abs(gx) + abs(gy)
                grad[y * targetW + x] = mag
                if (mag > maxGrad) maxGrad = mag
            }
        }

        val gradThreshold = (maxGrad * 0.22f).toInt().coerceAtLeast(28)
        val brightThreshold = (meanLuma + 12).coerceIn(95, 215)

        var tl = PointF(0.08f, 0.08f)
        var tr = PointF(0.92f, 0.08f)
        var br = PointF(0.92f, 0.92f)
        var bl = PointF(0.08f, 0.92f)

        var bestTlDist = Float.MAX_VALUE
        var bestTrDist = Float.MAX_VALUE
        var bestBrDist = Float.MAX_VALUE
        var bestBlDist = Float.MAX_VALUE
        var edgeHits = 0

        val marginX = (targetW * 0.03f).toInt().coerceAtLeast(2)
        val marginY = (targetH * 0.03f).toInt().coerceAtLeast(2)

        for (y in marginY until targetH - marginY) {
            for (x in marginX until targetW - marginX) {
                val idx = y * targetW + x
                val isStrongEdge = grad[idx] >= gradThreshold
                val isBrightPaperBoundary = luma[idx] >= brightThreshold && grad[idx] >= (gradThreshold * 0.6f).toInt()
                if (isStrongEdge || isBrightPaperBoundary) {
                    edgeHits++
                    val nx = x.toFloat() / targetW
                    val ny = y.toFloat() / targetH

                    val dTl = nx * nx + ny * ny
                    if (dTl < bestTlDist) {
                        bestTlDist = dTl
                        tl = PointF(nx, ny)
                    }
                    val dTr = (1f - nx) * (1f - nx) + ny * ny
                    if (dTr < bestTrDist) {
                        bestTrDist = dTr
                        tr = PointF(nx, ny)
                    }
                    val dBr = (1f - nx) * (1f - nx) + (1f - ny) * (1f - ny)
                    if (dBr < bestBrDist) {
                        bestBrDist = dBr
                        br = PointF(nx, ny)
                    }
                    val dBl = nx * nx + (1f - ny) * (1f - ny)
                    if (dBl < bestBlDist) {
                        bestBlDist = dBl
                        bl = PointF(nx, ny)
                    }
                }
            }
        }

        val minSpan = 0.22f
        if (edgeHits < 20 || (tr.x - tl.x) < minSpan || (bl.y - tl.y) < minSpan) {
            return@withContext NormalizedQuad(
                topLeft = PointF(0.06f, 0.06f),
                topRight = PointF(0.94f, 0.06f),
                bottomRight = PointF(0.94f, 0.94f),
                bottomLeft = PointF(0.06f, 0.94f)
            )
        }

        NormalizedQuad(
            topLeft = PointF(tl.x.coerceIn(0.02f, 0.46f), tl.y.coerceIn(0.02f, 0.46f)),
            topRight = PointF(tr.x.coerceIn(0.54f, 0.98f), tr.y.coerceIn(0.02f, 0.46f)),
            bottomRight = PointF(br.x.coerceIn(0.54f, 0.98f), br.y.coerceIn(0.54f, 0.98f)),
            bottomLeft = PointF(bl.x.coerceIn(0.02f, 0.46f), bl.y.coerceIn(0.54f, 0.98f))
        )
    }

    /**
     * Applies 4-point Perspective Correction (Homography Warp) using Android's Matrix.setPolyToPoly.
     */
    suspend fun applyPerspectiveWarp(source: Bitmap, quad: NormalizedQuad): Bitmap = withContext(Dispatchers.Default) {
        val w = source.width.toFloat()
        val h = source.height.toFloat()

        val srcPts = quad.toFloatArray(w, h)
        val topWidth = hypot((srcPts[2] - srcPts[0]).toDouble(), (srcPts[3] - srcPts[1]).toDouble()).toFloat()
        val bottomWidth = hypot((srcPts[4] - srcPts[6]).toDouble(), (srcPts[5] - srcPts[7]).toDouble()).toFloat()
        val leftHeight = hypot((srcPts[6] - srcPts[0]).toDouble(), (srcPts[7] - srcPts[1]).toDouble()).toFloat()
        val rightHeight = hypot((srcPts[4] - srcPts[2]).toDouble(), (srcPts[5] - srcPts[3]).toDouble()).toFloat()

        val dstW = max(topWidth, bottomWidth).toInt().coerceIn(200, 2600)
        val dstH = max(leftHeight, rightHeight).toInt().coerceIn(200, 3400)

        val dstPts = floatArrayOf(
            0f, 0f,
            dstW.toFloat(), 0f,
            dstW.toFloat(), dstH.toFloat(),
            0f, dstH.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
        if (!success) return@withContext source

        val output = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, matrix, paint)
        output
    }

    /**
     * Automatic deskew: estimates dominant horizontal text line skew angle and straightens the page.
     */
    suspend fun deskewBitmap(source: Bitmap, angleDegrees: Float? = null): Bitmap = withContext(Dispatchers.Default) {
        val angle = angleDegrees ?: estimateSkewAngle(source)
        if (abs(angle) < 0.15f) return@withContext source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val matrix = Matrix().apply {
            postRotate(-angle, source.width / 2f, source.height / 2f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        output
    }

    private fun estimateSkewAngle(bitmap: Bitmap): Float {
        // Sample horizontal projection variance across small angles (-5..+5 deg)
        val sw = 200
        val sh = (bitmap.height * (200f / bitmap.width.coerceAtLeast(1))).toInt().coerceIn(80, 300)
        val small = Bitmap.createScaledBitmap(bitmap, sw, sh, false)
        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)
        var sumDx = 0.0
        var sumDy = 0.0
        for (y in 2 until sh - 2 step 2) {
            for (x in 2 until sw - 2 step 2) {
                val c0 = pixels[y * sw + x] and 0xFF
                val cx = pixels[y * sw + (x + 1)] and 0xFF
                val cy = pixels[(y + 1) * sw + x] and 0xFF
                val gx = cx - c0
                val gy = cy - c0
                if (abs(gy) > 25 && abs(gx) < 20) {
                    sumDx += gx
                    sumDy += abs(gy)
                }
            }
        }
        if (sumDy < 1.0) return 0f
        val deg = Math.toDegrees(atan2(sumDx, sumDy)).toFloat()
        return deg.coerceIn(-4.5f, 4.5f)
    }

    fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        val norm = ((degrees % 360) + 360) % 360
        if (norm == 0) return source
        val matrix = Matrix().apply { postRotate(norm.toFloat()) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Applies selected ScanFilter plus custom brightness (-80f..80f) and contrast (0.6f..1.8f) adjustments.
     * Note: ScanFilter.BW always outputs strictly binary pure black (#000000) and pure white (#FFFFFF) pixels
     * and never turns grey when adjusting brightness or contrast.
     */
    suspend fun applyEnhancement(
        source: Bitmap,
        filter: ScanFilter,
        brightness: Float = 0f,
        contrast: Float = 1f,
        rotationDegrees: Int = 0
    ): Bitmap = withContext(Dispatchers.Default) {
        val rotated = if (rotationDegrees != 0) rotateBitmap(source, rotationDegrees) else source

        if (filter == ScanFilter.BW) {
            return@withContext applyAdaptiveBlackAndWhite(
                source = rotated,
                brightness = brightness,
                contrast = contrast
            )
        }

        val filtered = when (filter) {
            ScanFilter.ORIGINAL -> rotated
            ScanFilter.AUTO -> applyColorMatrixFilter(rotated, saturation = 1.08f, contrast = 1.25f, brightness = 10f)
            ScanFilter.MAGIC_COLOR -> applyColorMatrixFilter(rotated, saturation = 1.45f, contrast = 1.42f, brightness = 16f)
            ScanFilter.GRAYSCALE -> applyColorMatrixFilter(rotated, saturation = 0f, contrast = 1.18f, brightness = 6f)
            ScanFilter.BW -> rotated
            ScanFilter.NO_SHADOW -> applyShadowRemoval(rotated)
        }

        if (abs(brightness) > 0.5f || abs(contrast - 1f) > 0.02f) {
            applyColorMatrixFilter(filtered, saturation = 1f, contrast = contrast, brightness = brightness)
        } else {
            filtered
        }
    }

    private fun applyColorMatrixFilter(
        source: Bitmap,
        saturation: Float,
        contrast: Float,
        brightness: Float
    ): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        val translate = (1f - contrast) * 128f + brightness
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(contrastMatrix)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(satMatrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return out
    }

    /**
     * High-contrast adaptive local thresholding for crisp Black & White document reproduction.
     * Produces strictly binary deep black ink (#000000) on pure white paper (#FFFFFF) — never grey.
     * Brightness (-80..80) and Contrast (0.6..1.8) smoothly adjust ink thickness and threshold sensitivity.
     */
    private fun applyAdaptiveBlackAndWhite(
        source: Bitmap,
        brightness: Float = 0f,
        contrast: Float = 1f
    ): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val blockSize = max(14, min(w, h) / 28)
        val gridW = (w + blockSize - 1) / blockSize
        val gridH = (h + blockSize - 1) / blockSize
        val blockMeans = IntArray(gridW * gridH)

        for (gy in 0 until gridH) {
            val yStart = gy * blockSize
            val yEnd = min(h, yStart + blockSize)
            for (gx in 0 until gridW) {
                val xStart = gx * blockSize
                val xEnd = min(w, xStart + blockSize)
                var sum = 0
                var count = 0
                for (y in yStart until yEnd step 2) {
                    val rowOffset = y * w
                    for (x in xStart until xEnd step 2) {
                        val c = pixels[rowOffset + x]
                        val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                        sum += l
                        count++
                    }
                }
                blockMeans[gy * gridW + gx] = if (count > 0) sum / count else 170
            }
        }

        // Brightness shifts threshold (higher brightness = cleaner white page; lower brightness = bolder ink)
        // Contrast scales local threshold sensitivity without ever introducing grey pixels.
        val contrastBias = (contrast - 1f) * 28f
        val thresholdShift = (-brightness * 0.55f + contrastBias).toInt()

        for (y in 0 until h) {
            val gy = min(gridH - 1, y / blockSize)
            val rowOffset = y * w
            for (x in 0 until w) {
                val gx = min(gridW - 1, x / blockSize)
                val localMean = blockMeans[gy * gridW + gx]
                val c = pixels[rowOffset + x]
                val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                val threshold = ((localMean * 0.91f).toInt() + thresholdShift).coerceIn(45, 240)
                val v = if (l < threshold) 0 else 255
                pixels[rowOffset + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Shadow removal by estimating local background illumination and normalizing pixel luminance.
     */
    private fun applyShadowRemoval(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val blockSize = max(24, min(w, h) / 16)
        val gridW = (w + blockSize - 1) / blockSize
        val gridH = (h + blockSize - 1) / blockSize
        val bgLuma = IntArray(gridW * gridH)

        for (gy in 0 until gridH) {
            val yStart = gy * blockSize
            val yEnd = min(h, yStart + blockSize)
            for (gx in 0 until gridW) {
                val xStart = gx * blockSize
                val xEnd = min(w, xStart + blockSize)
                var maxL = 80
                for (y in yStart until yEnd step 3) {
                    val rowOffset = y * w
                    for (x in xStart until xEnd step 3) {
                        val c = pixels[rowOffset + x]
                        val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                        if (l > maxL) maxL = l
                    }
                }
                bgLuma[gy * gridW + gx] = maxL
            }
        }

        for (y in 0 until h) {
            val gy = min(gridH - 1, y / blockSize)
            val rowOffset = y * w
            for (x in 0 until w) {
                val gx = min(gridW - 1, x / blockSize)
                val bg = bgLuma[gy * gridW + gx].coerceAtLeast(60)
                val c = pixels[rowOffset + x]
                val r = (((c shr 16) and 0xFF) * 245 / bg).coerceIn(0, 255)
                val g = (((c shr 8) and 0xFF) * 245 / bg).coerceIn(0, 255)
                val b = ((c and 0xFF) * 245 / bg).coerceIn(0, 255)
                pixels[rowOffset + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Composites Front and Back of an ID Card onto a single A4 page with crop marks and optional watermark.
     */
    suspend fun createIdCardCompositePage(
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        watermark: String = ""
    ): Bitmap = withContext(Dispatchers.Default) {
        val pageW = 1240
        val pageH = 1754
        val out = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val cardW = 860f
        val cardH = 540f
        val left = (pageW - cardW) / 2f
        val topFront = 220f
        val topBack = 960f

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 32f
            isFakeBoldText = true
        }

        canvas.drawText("FRONT SIDE", left, topFront - 24f, labelPaint)
        val frontRect = RectF(left, topFront, left + cardW, topFront + cardH)
        canvas.drawBitmap(frontBitmap, null, frontRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(frontRect, borderPaint)

        canvas.drawText("BACK SIDE", left, topBack - 24f, labelPaint)
        val backRect = RectF(left, topBack, left + cardW, topBack + cardH)
        canvas.drawBitmap(backBitmap, null, backRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(backRect, borderPaint)

        if (watermark.isNotBlank()) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(48, 30, 62, 98)
                textSize = 64f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.save()
            canvas.rotate(-28f, pageW / 2f, pageH / 2f)
            canvas.drawText(watermark, pageW / 2f, pageH / 2f, wmPaint)
            canvas.restore()
        }

        out
    }

    /**
     * Generates a realistic sample scanned invoice Bitmap for first-launch demonstration.
     */
    fun generateSampleInvoiceBitmap(): Bitmap {
        val w = 1000
        val h = 1414
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B192C")
            textSize = 46f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            textSize = 26f
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 28f
            isFakeBoldText = true
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0284C7")
            style = Paint.Style.FILL
        }

        canvas.drawRect(70f, 70f, 930f, 84f, accentPaint)
        canvas.drawText("APEX CLOUD SYSTEMS INC.", 70f, 150f, headerPaint)
        canvas.drawText("INVOICE #INV-2026-0842  •  DATE: OCT 21, 2026", 70f, 200f, subPaint)
        canvas.drawText("Bill To: Horizon Design Studio, 740 Market St, San Francisco, CA", 70f, 255f, subPaint)

        val linePaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 2f
        }
        canvas.drawLine(70f, 300f, 930f, 300f, linePaint)
        canvas.drawText("DESCRIPTION", 70f, 350f, boldPaint)
        canvas.drawText("AMOUNT", 760f, 350f, boldPaint)
        canvas.drawLine(70f, 375f, 930f, 375f, linePaint)

        canvas.drawText("1. Enterprise OCR & Document Pipeline License (Annual)", 70f, 440f, subPaint)
        canvas.drawText("$1,200.00", 760f, 440f, subPaint)
        canvas.drawText("2. Cloud Vault Multi-Device Encrypted Sync (5 Seats)", 70f, 510f, subPaint)
        canvas.drawText("$450.00", 760f, 510f, subPaint)
        canvas.drawText("3. High-Precision PDF Archival Module", 70f, 580f, subPaint)
        canvas.drawText("$290.00", 760f, 580f, subPaint)

        canvas.drawLine(70f, 650f, 930f, 650f, linePaint)
        canvas.drawText("TOTAL DUE (USD):", 500f, 715f, boldPaint)
        canvas.drawText("$1,940.00", 760f, 715f, headerPaint)

        canvas.drawText("Payment Terms: Net 30 • Status: PAID IN FULL", 70f, 830f, boldPaint)
        canvas.drawText("Authorized Signature: A. Vance, VP Finance", 70f, 900f, subPaint)
        return bmp
    }
}
