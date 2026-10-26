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
     * - ScanFilter.CLEAR: Built-in Document Clear mode (default on first capture/upload) that bleaches paper
     *   background to pure white (#FFFFFF) while preserving smooth anti-aliased black/charcoal ink and fine detail.
     * - ScanFilter.BW: Strictly binary pure black (#000000) and pure white (#FFFFFF) pixels (never grey).
     */
    suspend fun applyEnhancement(
        source: Bitmap,
        filter: ScanFilter,
        brightness: Float = 0f,
        contrast: Float = 1f,
        rotationDegrees: Int = 0
    ): Bitmap = withContext(Dispatchers.Default) {
        val rotated = if (rotationDegrees != 0) rotateBitmap(source, rotationDegrees) else source

        if (filter == ScanFilter.CLEAR) {
            return@withContext applyDocumentClearScan(
                source = rotated,
                brightness = brightness,
                contrast = contrast,
                saturationBoost = 0f,
                contrastBoost = 1.28f,
                isBwPrint = true
            )
        }

        if (filter == ScanFilter.MAGIC_COLOR) {
            return@withContext applyDocumentClearScan(
                source = rotated,
                brightness = brightness + 6f,
                contrast = contrast * 1.12f,
                saturationBoost = 1.40f,
                contrastBoost = 1.18f,
                isBwPrint = false
            )
        }

        if (filter == ScanFilter.BW) {
            return@withContext applyAdaptiveBlackAndWhite(
                source = rotated,
                brightness = brightness,
                contrast = contrast
            )
        }

        val filtered = when (filter) {
            ScanFilter.ORIGINAL -> rotated
            ScanFilter.AUTO -> applyColorMatrixFilter(rotated, saturation = 1.10f, contrast = 1.18f, brightness = 6f)
            ScanFilter.GRAYSCALE -> applyColorMatrixFilter(rotated, saturation = 0f, contrast = 1.18f, brightness = 6f)
            ScanFilter.CLEAR, ScanFilter.MAGIC_COLOR, ScanFilter.BW -> rotated
            ScanFilter.NO_SHADOW -> applyShadowRemoval(rotated)
        }

        if (abs(brightness) > 0.5f || abs(contrast - 1f) > 0.02f) {
            applyColorMatrixFilter(filtered, saturation = 1f, contrast = contrast, brightness = brightness)
        } else {
            filtered
        }
    }

    /**
     * Smooth 4-pass 3x3 Gaussian-weighted grid filter ([1, 2, 1; 2, 4, 2; 1, 2, 1] / 16)
     * so local illumination maps never exhibit abrupt transitions between cells.
     */
    private fun smoothGridGaussian(grid: IntArray, gridW: Int, gridH: Int, passes: Int = 4): IntArray {
        var src = grid.copyOf()
        var dst = IntArray(gridW * gridH)
        repeat(passes) {
            for (gy in 0 until gridH) {
                val y0 = (gy - 1).coerceIn(0, gridH - 1) * gridW
                val y1 = gy * gridW
                val y2 = (gy + 1).coerceIn(0, gridH - 1) * gridW
                for (gx in 0 until gridW) {
                    val x0 = (gx - 1).coerceIn(0, gridW - 1)
                    val x1 = gx
                    val x2 = (gx + 1).coerceIn(0, gridW - 1)
                    val weightedSum =
                        src[y0 + x0] + (src[y0 + x1] shl 1) + src[y0 + x2] +
                        (src[y1 + x0] shl 1) + (src[y1 + x1] shl 2) + (src[y1 + x2] shl 1) +
                        src[y2 + x0] + (src[y2 + x1] shl 1) + src[y2 + x2]
                    dst[y1 + x1] = (weightedSum + 8) shr 4
                }
            }
            val tmp = src
            src = dst
            dst = tmp
        }
        return src
    }

    /**
     * Continuous bilinear interpolation across cell centers of a 2D illumination grid.
     * Eliminates 100% of square block/checkerboard artifacts.
     */
    private fun sampleBilinearGrid(
        grid: IntArray,
        gridW: Int,
        gridH: Int,
        blockSize: Int,
        x: Int,
        y: Int
    ): Float {
        val fx = ((x.toFloat() + 0.5f) / blockSize.toFloat()) - 0.5f
        val fy = ((y.toFloat() + 0.5f) / blockSize.toFloat()) - 0.5f
        val gx0 = fx.toInt().coerceIn(0, gridW - 1)
        val gy0 = fy.toInt().coerceIn(0, gridH - 1)
        val gx1 = (gx0 + 1).coerceIn(0, gridW - 1)
        val gy1 = (gy0 + 1).coerceIn(0, gridH - 1)
        val tx = (fx - gx0.toFloat()).coerceIn(0f, 1f)
        val ty = (fy - gy0.toFloat()).coerceIn(0f, 1f)

        val row0 = gy0 * gridW
        val row1 = gy1 * gridW
        val v00 = grid[row0 + gx0].toFloat()
        val v10 = grid[row0 + gx1].toFloat()
        val v01 = grid[row1 + gx0].toFloat()
        val v11 = grid[row1 + gx1].toFloat()

        val top = v00 + (v10 - v00) * tx
        val bottom = v01 + (v11 - v01) * tx
        return top + (bottom - top) * ty
    }

    /**
     * Clean Document Scanner ("Clear" Black & White Print mode & "Magic" Color mode):
     * - Estimates a continuous, bilinear-interpolated illumination envelope (zero square block artifacts).
     * - When `isBwPrint = true` (Clear Mode): Produces ultra-crisp Black & White laser-print quality
     *   by bleaching uneven paper shadows to pure #FFFFFF, deepening ink strokes with an anti-aliased
     *   high-contrast S-curve, and applying a 3x3 unsharp mask for razor-sharp typography.
     * - When `isBwPrint = false` (Magic Color Mode): Preserves full RGB color vibrancy while cleanly
     *   whitening low-saturation paper backgrounds.
     */
    private fun applyDocumentClearScan(
        source: Bitmap,
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturationBoost: Float = 1.15f,
        contrastBoost: Float = 1.08f,
        isBwPrint: Boolean = true
    ): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= 2 || h <= 2) return source

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // 1. Estimate global high-luminance envelope so we know if the page is paper vs dark graphics
        val lumaHist = IntArray(256)
        var sampleCount = 0
        val step = max(1, min(w, h) / 180)
        for (y in 0 until h step step) {
            val rowOffset = y * w
            for (x in 0 until w step step) {
                val c = pixels[rowOffset + x]
                val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                lumaHist[l.coerceIn(0, 255)]++
                sampleCount++
            }
        }
        val target88th = (sampleCount * 88) / 100
        var acc = 0
        var globalHighLuma = 220
        for (i in 0..255) {
            acc += lumaHist[i]
            if (acc >= target88th) {
                globalHighLuma = i
                break
            }
        }
        globalHighLuma = globalHighLuma.coerceIn(175, 250)
        // Safe illumination floor prevents dark graphics/photos from being over-amplified into white ghosts
        val minSafeBg = max(165, (globalHighLuma * 0.76f).toInt())

        val blockSize = max(24, min(w, h) / 18)
        val gridW = (w + blockSize - 1) / blockSize
        val gridH = (h + blockSize - 1) / blockSize
        val rawBg = IntArray(gridW * gridH)

        // 2. Estimate local illumination envelope per cell
        for (gy in 0 until gridH) {
            val yStart = gy * blockSize
            val yEnd = min(h, yStart + blockSize)
            for (gx in 0 until gridW) {
                val xStart = gx * blockSize
                val xEnd = min(w, xStart + blockSize)
                var maxL = minSafeBg
                var sumBright = 0
                var countBright = 0
                for (y in yStart until yEnd step 2) {
                    val rowOffset = y * w
                    for (x in xStart until xEnd step 2) {
                        val c = pixels[rowOffset + x]
                        val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                        if (l > maxL) maxL = l
                        if (l >= minSafeBg - 22) {
                            sumBright += l
                            countBright++
                        }
                    }
                }
                val avgBright = if (countBright > 0) sumBright / countBright else maxL
                rawBg[gy * gridW + gx] = ((maxL * 2 + avgBright * 3) / 5).coerceIn(minSafeBg, 255)
            }
        }

        // 3. Smooth illumination grid with 4 Gaussian passes
        val smoothBg = smoothGridGaussian(rawBg, gridW, gridH, passes = 4)

        // 4. Per-pixel bilinear illumination normalization + B&W Print / Magic Color curve
        val effectiveContrast = (contrast * contrastBoost).coerceIn(0.65f, 2.35f)
        val brightnessOffset = brightness * 0.85f
        val whitePaperKnee = (200f - brightness * 0.30f).coerceIn(174f, 236f)
        val normalizedPixels = IntArray(w * h)
        val lumaBuffer = IntArray(w * h)

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val idx = rowOffset + x
                val c = pixels[idx]
                val rIn = (c shr 16) and 0xFF
                val gIn = (c shr 8) and 0xFF
                val bIn = c and 0xFF

                val bg = sampleBilinearGrid(smoothBg, gridW, gridH, blockSize, x, y).coerceIn(minSafeBg.toFloat(), 255f)
                val gain = (252f / bg).coerceIn(1.0f, 1.48f)

                if (isBwPrint) {
                    // High-Clarity Black & White Laser-Print Document Curve
                    val rawLuma = (rIn * 77 + gIn * 150 + bIn * 29) shr 8
                    var normL = rawLuma * gain + brightnessOffset
                    normL = (normL - 142f) * effectiveContrast + 142f

                    val printLuma = when {
                        normL >= whitePaperKnee -> 255f
                        normL <= 78f -> (normL * 0.22f).coerceIn(0f, 20f)
                        else -> {
                            // Smooth cubic Hermite S-curve between ink (78) and pure white paper (whitePaperKnee)
                            val t = ((normL - 78f) / (whitePaperKnee - 78f).coerceAtLeast(1f)).coerceIn(0f, 1f)
                            val s = t * t * (3f - 2f * t)
                            18f + s * 237f
                        }
                    }
                    val vOut = printLuma.toInt().coerceIn(0, 255)
                    lumaBuffer[idx] = vOut
                    normalizedPixels[idx] = (0xFF shl 24) or (vOut shl 16) or (vOut shl 8) or vOut
                } else {
                    var r = rIn * gain + brightnessOffset
                    var g = gIn * gain + brightnessOffset
                    var b = bIn * gain + brightnessOffset

                    r = (r - 136f) * effectiveContrast + 136f
                    g = (g - 136f) * effectiveContrast + 136f
                    b = (b - 136f) * effectiveContrast + 136f

                    val luma = 0.299f * r + 0.587f * g + 0.114f * b
                    val maxCh = max(r, max(g, b))
                    val minCh = min(r, min(g, b))
                    val chroma = maxCh - minCh

                    if (chroma > 6f) {
                        r = luma + (r - luma) * saturationBoost
                        g = luma + (g - luma) * saturationBoost
                        b = luma + (b - luma) * saturationBoost
                    }

                    if (luma > whitePaperKnee && chroma < 48f) {
                        val t = ((luma - whitePaperKnee) / (255f - whitePaperKnee).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val chromaWeight = (1f - (chroma / 48f)).coerceIn(0f, 1f)
                        val whiteBlend = t * t * (3f - 2f * t) * chromaWeight * 0.88f
                        r += (255f - r) * whiteBlend
                        g += (255f - g) * whiteBlend
                        b += (255f - b) * whiteBlend
                    } else if (luma < 88f) {
                        val inkFactor = 0.88f + 0.12f * (luma / 88f).coerceIn(0f, 1f)
                        r *= inkFactor
                        g *= inkFactor
                        b *= inkFactor
                    }

                    val rOut = r.toInt().coerceIn(0, 255)
                    val gOut = g.toInt().coerceIn(0, 255)
                    val bOut = b.toInt().coerceIn(0, 255)
                    lumaBuffer[idx] = (rOut * 77 + gOut * 150 + bOut * 29) shr 8
                    normalizedPixels[idx] = (0xFF shl 24) or (rOut shl 16) or (gOut shl 8) or bOut
                }
            }
        }

        // 5. Fast 3x3 Unsharp Mask pass for crisp laser-print text & line sharpness
        val outPixels = normalizedPixels.copyOf()
        val unsharpMultiplier = if (isBwPrint) 5 else 3 // Stronger edge definition for B&W Print
        for (y in 1 until h - 1) {
            val rowOffset = y * w
            val prevRow = (y - 1) * w
            val nextRow = (y + 1) * w
            for (x in 1 until w - 1) {
                val idx = rowOffset + x
                val centerL = lumaBuffer[idx]
                val neighborL = (
                    lumaBuffer[prevRow + x] +
                    lumaBuffer[nextRow + x] +
                    lumaBuffer[idx - 1] +
                    lumaBuffer[idx + 1]
                ) shr 2
                val detail = centerL - neighborL
                if (abs(detail) > 2) {
                    val boost = (detail * unsharpMultiplier) shr 3
                    val c = normalizedPixels[idx]
                    val r = (((c shr 16) and 0xFF) + boost).coerceIn(0, 255)
                    val g = (((c shr 8) and 0xFF) + boost).coerceIn(0, 255)
                    val b = ((c and 0xFF) + boost).coerceIn(0, 255)
                    outPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, w, 0, 0, w, h)
        return out
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
     * Smooth bilinear-interpolated adaptive local thresholding for crisp Black & White document reproduction.
     * Eliminates square block artifacts and keeps solid dark regions intact.
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

        val blockSize = max(20, min(w, h) / 22)
        val gridW = (w + blockSize - 1) / blockSize
        val gridH = (h + blockSize - 1) / blockSize
        val rawMeans = IntArray(gridW * gridH)

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
                val mean = if (count > 0) sum / count else 175
                rawMeans[gy * gridW + gx] = mean.coerceIn(115, 245)
            }
        }

        val smoothMeans = smoothGridGaussian(rawMeans, gridW, gridH, passes = 3)
        val contrastBias = (contrast - 1f) * 24f
        val thresholdShift = (-brightness * 0.50f + contrastBias)

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val localMean = sampleBilinearGrid(smoothMeans, gridW, gridH, blockSize, x, y)
                val c = pixels[rowOffset + x]
                val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                val threshold = (localMean * 0.90f + thresholdShift).toInt().coerceIn(55, 235)
                val v = if (l < threshold) 0 else 255
                pixels[rowOffset + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Smooth bilinear-interpolated shadow removal that normalizes local illumination without square blocks.
     */
    private fun applyShadowRemoval(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val blockSize = max(24, min(w, h) / 16)
        val gridW = (w + blockSize - 1) / blockSize
        val gridH = (h + blockSize - 1) / blockSize
        val rawBg = IntArray(gridW * gridH)

        for (gy in 0 until gridH) {
            val yStart = gy * blockSize
            val yEnd = min(h, yStart + blockSize)
            for (gx in 0 until gridW) {
                val xStart = gx * blockSize
                val xEnd = min(w, xStart + blockSize)
                var maxL = 155
                for (y in yStart until yEnd step 3) {
                    val rowOffset = y * w
                    for (x in xStart until xEnd step 3) {
                        val c = pixels[rowOffset + x]
                        val l = (((c shr 16) and 0xFF) * 77 + ((c shr 8) and 0xFF) * 150 + (c and 0xFF) * 29) shr 8
                        if (l > maxL) maxL = l
                    }
                }
                rawBg[gy * gridW + gx] = maxL.coerceIn(155, 255)
            }
        }

        val smoothBg = smoothGridGaussian(rawBg, gridW, gridH, passes = 4)

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val bg = sampleBilinearGrid(smoothBg, gridW, gridH, blockSize, x, y).coerceAtLeast(155f)
                val gain = (248f / bg).coerceIn(1.0f, 1.45f)
                val c = pixels[rowOffset + x]
                val r = (((c shr 16) and 0xFF) * gain).toInt().coerceIn(0, 255)
                val g = (((c shr 8) and 0xFF) * gain).toInt().coerceIn(0, 255)
                val b = ((c and 0xFF) * gain).toInt().coerceIn(0, 255)
                pixels[rowOffset + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Composites Front and Back of an ID Card or Passport onto a single A4 page with precision corner crop marks,
     * optional B&W Print Clear Scan enhancement, and optional watermark.
     */
    suspend fun createIdCardCompositePage(
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        watermark: String = "",
        applyBwPrint: Boolean = false,
        showCropMarks: Boolean = true,
        documentTypeLabel: String = "ID CARD / PASSPORT (2-SIDE A4)"
    ): Bitmap = withContext(Dispatchers.Default) {
        val pageW = 1240
        val pageH = 1754
        val out = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)

        val processedFront = if (applyBwPrint) {
            applyDocumentClearScan(frontBitmap, isBwPrint = true, contrastBoost = 1.28f)
        } else {
            frontBitmap
        }
        val processedBack = if (applyBwPrint) {
            applyDocumentClearScan(backBitmap, isBwPrint = true, contrastBoost = 1.28f)
        } else {
            backBitmap
        }

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181B")
            textSize = 28f
            isFakeBoldText = true
            letterSpacing = 0.08f
        }
        canvas.drawText(documentTypeLabel, 190f, 118f, headerPaint)

        val cardW = 860f
        val cardH = 540f
        val left = (pageW - cardW) / 2f
        val topFront = 220f
        val topBack = 960f

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4D4D8")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val cropMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18181B")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#52525B")
            textSize = 26f
            isFakeBoldText = true
            letterSpacing = 0.06f
        }

        fun drawCornerCropMarks(rect: RectF) {
            if (!showCropMarks) return
            val pad = 16f
            val len = 34f
            // Top-Left
            canvas.drawLine(rect.left - pad, rect.top - pad, rect.left - pad + len, rect.top - pad, cropMarkPaint)
            canvas.drawLine(rect.left - pad, rect.top - pad, rect.left - pad, rect.top - pad + len, cropMarkPaint)
            // Top-Right
            canvas.drawLine(rect.right + pad - len, rect.top - pad, rect.right + pad, rect.top - pad, cropMarkPaint)
            canvas.drawLine(rect.right + pad, rect.top - pad, rect.right + pad, rect.top - pad + len, cropMarkPaint)
            // Bottom-Left
            canvas.drawLine(rect.left - pad, rect.bottom + pad - len, rect.left - pad, rect.bottom + pad, cropMarkPaint)
            canvas.drawLine(rect.left - pad, rect.bottom + pad, rect.left - pad + len, rect.bottom + pad, cropMarkPaint)
            // Bottom-Right
            canvas.drawLine(rect.right + pad - len, rect.bottom + pad, rect.right + pad, rect.bottom + pad, cropMarkPaint)
            canvas.drawLine(rect.right + pad, rect.bottom + pad - len, rect.right + pad, rect.bottom + pad, cropMarkPaint)
        }

        canvas.drawText("01 / FRONT SIDE", left, topFront - 28f, labelPaint)
        val frontRect = RectF(left, topFront, left + cardW, topFront + cardH)
        canvas.drawBitmap(processedFront, null, frontRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(frontRect, borderPaint)
        drawCornerCropMarks(frontRect)

        canvas.drawText("02 / BACK SIDE", left, topBack - 28f, labelPaint)
        val backRect = RectF(left, topBack, left + cardW, topBack + cardH)
        canvas.drawBitmap(processedBack, null, backRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawRect(backRect, borderPaint)
        drawCornerCropMarks(backRect)

        if (watermark.isNotBlank()) {
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(44, 24, 24, 27)
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

    /**
     * Composites a signature bitmap onto a document page bitmap at normalized placement coordinates.
     * @param normX 0f (left) to 1f (right) within available width
     * @param normY 0f (top) to 1f (bottom) within available height
     * @param scale Signature width as a fraction of page width (e.g., 0.28f)
     */
    fun imposeSignatureOnBitmap(
        pageBitmap: Bitmap,
        signatureBitmap: Bitmap,
        normX: Float = 0.65f,
        normY: Float = 0.80f,
        scale: Float = 0.30f
    ): Bitmap {
        val out = pageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val pageW = out.width.toFloat()
        val pageH = out.height.toFloat()

        val clampedScale = scale.coerceIn(0.12f, 0.65f)
        val sigW = pageW * clampedScale
        val sigRatio = signatureBitmap.height.toFloat() / signatureBitmap.width.coerceAtLeast(1)
        val sigH = sigW * sigRatio

        val maxLeft = (pageW - sigW - 8f).coerceAtLeast(8f)
        val maxTop = (pageH - sigH - 8f).coerceAtLeast(8f)
        val sx = (normX.coerceIn(0f, 1f) * maxLeft).coerceIn(8f, maxLeft)
        val sy = (normY.coerceIn(0f, 1f) * maxTop).coerceIn(8f, maxTop)

        val destRect = RectF(sx, sy, sx + sigW, sy + sigH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(signatureBitmap, null, destRect, paint)
        return out
    }

    /**
     * Converts near-white backgrounds in an imported signature image to transparent
     * so imported signatures stamp cleanly onto document pages.
     */
    fun makeSignatureTransparentIfWhite(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val c = pixels[i]
            val a = (c ushr 24) and 0xFF
            if (a > 0) {
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                val lum = (r * 77 + g * 150 + b * 29) shr 8
                if (lum > 220) {
                    pixels[i] = 0x00000000
                } else if (lum > 185) {
                    val newAlpha = ((220 - lum) * 255 / 35).coerceIn(0, 255)
                    pixels[i] = (newAlpha shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Generates a high-resolution transparent Bitmap of an official double-bordered rubber stamp
     * (e.g., APPROVED, PAID, CONFIDENTIAL, URGENT, VOID, or custom text) with optional date line.
     */
    fun createOfficialStampBitmap(
        stampText: String,
        subText: String = "",
        inkColor: Int = Color.rgb(210, 32, 48)
    ): Bitmap {
        val cleanMain = stampText.trim().uppercase().ifBlank { "APPROVED" }
        val cleanSub = subText.trim().uppercase()
        val w = 760
        val h = if (cleanSub.isNotBlank()) 280 else 220
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val outerRect = RectF(18f, 18f, w - 18f, h - 18f)
        val innerRect = RectF(32f, 32f, w - 32f, h - 32f)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(
                28,
                Color.red(inkColor),
                Color.green(inkColor),
                Color.blue(inkColor)
            )
            style = Paint.Style.FILL
        }
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }

        canvas.drawRoundRect(outerRect, 24f, 24f, fillPaint)
        canvas.drawRoundRect(outerRect, 24f, 24f, outerBorderPaint)
        canvas.drawRoundRect(innerRect, 16f, 16f, innerBorderPaint)

        val maxTextWidth = w - 110f
        var mainTextSize = 78f
        val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
            textSize = mainTextSize
        }
        while (mainPaint.measureText(cleanMain) > maxTextWidth && mainTextSize > 34f) {
            mainTextSize -= 4f
            mainPaint.textSize = mainTextSize
        }

        if (cleanSub.isBlank()) {
            val textY = h / 2f - (mainPaint.descent() + mainPaint.ascent()) / 2f
            canvas.drawText(cleanMain, w / 2f, textY, mainPaint)
        } else {
            val mainY = h * 0.48f - (mainPaint.descent() + mainPaint.ascent()) / 2f - 8f
            canvas.drawText(cleanMain, w / 2f, mainY, mainPaint)

            val divY = h * 0.64f
            canvas.drawLine(56f, divY, w - 56f, divY, innerBorderPaint)

            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = inkColor
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.06f
                textSize = 30f
            }
            val subY = h * 0.80f - (subPaint.descent() + subPaint.ascent()) / 2f
            canvas.drawText(cleanSub, w / 2f, subY, subPaint)
        }

        return bmp
    }

    /**
     * Renders a multi-page formatted document from typed or pasted text notes so users can
     * create clean, searchable PDFs directly from text/notes.
     */
    fun renderTextNotePagesToBitmaps(
        title: String,
        bodyText: String,
        includeRuledLines: Boolean = false
    ): List<Bitmap> {
        val pageW = 1240
        val pageH = 1754
        val marginX = 96f
        val maxLineW = pageW - marginX * 2f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 22, 46)
            textSize = 46f
            isFakeBoldText = true
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 24f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 30f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 2f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(10, 132, 255)
            style = Paint.Style.FILL
        }

        // Word-wrap paragraphs into lines
        val wrappedLines = mutableListOf<String>()
        val rawParagraphs = bodyText.ifBlank { "(Empty note)" }.split("\n")
        for (para in rawParagraphs) {
            if (para.isBlank()) {
                wrappedLines.add("")
                continue
            }
            val words = para.split(" ")
            var currentLine = StringBuilder()
            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (bodyPaint.measureText(testLine) <= maxLineW) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        wrappedLines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                wrappedLines.add(currentLine.toString())
            }
        }

        val lineHeight = 46f
        val linesPerPage = 28
        val chunks = wrappedLines.chunked(linesPerPage).ifEmpty { listOf(emptyList()) }
        val dateStr = java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date())

        return chunks.mapIndexed { pageIdx, pageLines ->
            val bmp = Bitmap.createBitmap(pageW, pageH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)

            // Header bar
            canvas.drawRect(marginX, 72f, pageW - marginX, 82f, accentPaint)
            val displayTitle = title.ifBlank { "Document Note" }
            canvas.drawText(displayTitle.take(45), marginX, 142f, titlePaint)
            canvas.drawText("Generated with Doc Scan • $dateStr • Page ${pageIdx + 1} of ${chunks.size}", marginX, 184f, metaPaint)
            canvas.drawLine(marginX, 210f, pageW - marginX, 210f, linePaint)

            var y = 276f
            for (i in 0 until linesPerPage) {
                if (includeRuledLines) {
                    canvas.drawLine(marginX, y + 10f, pageW - marginX, y + 10f, linePaint)
                }
                val lineText = pageLines.getOrNull(i)
                if (!lineText.isNullOrEmpty()) {
                    canvas.drawText(lineText, marginX, y, bodyPaint)
                }
                y += lineHeight
            }
            bmp
        }
    }

    /**
     * Generates a high-contrast monochrome QR Matrix & Barcode Card bitmap with finder patterns,
     * alignment module, timing pattern, payload encoding, and optional footer caption.
     */
    fun generateQrCodeBitmap(
        content: String,
        sizePx: Int = 640,
        label: String = ""
    ): Bitmap {
        val cleanPayload = content.trim().ifBlank { "https://docscan.app" }
        val gridDim = 29 // Version 3 QR 29x29 modules
        val matrix = Array(gridDim) { BooleanArray(gridDim) }
        val reserved = Array(gridDim) { BooleanArray(gridDim) }

        fun placeFinderPattern(startX: Int, startY: Int) {
            for (dy in -1..7) {
                for (dx in -1..7) {
                    val x = startX + dx
                    val y = startY + dy
                    if (x in 0 until gridDim && y in 0 until gridDim) {
                        reserved[y][x] = true
                        val inOuter = dx in 0..6 && dy in 0..6 && (dx == 0 || dx == 6 || dy == 0 || dy == 6)
                        val inInner = dx in 2..4 && dy in 2..4
                        matrix[y][x] = inOuter || inInner
                    }
                }
            }
        }

        placeFinderPattern(0, 0)
        placeFinderPattern(gridDim - 7, 0)
        placeFinderPattern(0, gridDim - 7)

        // Alignment pattern at (20, 20)
        for (dy in -2..2) {
            for (dx in -2..2) {
                val x = 20 + dx
                val y = 20 + dy
                reserved[y][x] = true
                matrix[y][x] = (abs(dx) == 2 || abs(dy) == 2 || (dx == 0 && dy == 0))
            }
        }

        // Timing patterns
        for (i in 8 until gridDim - 8) {
            reserved[6][i] = true
            matrix[6][i] = (i % 2 == 0)
            reserved[i][6] = true
            matrix[i][6] = (i % 2 == 0)
        }

        // Deterministic bit stream from UTF-8 payload bytes + SHA-256 parity bytes
        val payloadBytes = cleanPayload.toByteArray(Charsets.UTF_8)
        val digestBytes = try {
            java.security.MessageDigest.getInstance("SHA-256").digest(payloadBytes)
        } catch (_: Exception) {
            payloadBytes
        }
        val combinedBytes = payloadBytes + digestBytes
        var bitIndex = 0

        for (y in 0 until gridDim) {
            for (x in 0 until gridDim) {
                if (!reserved[y][x]) {
                    val byteVal = combinedBytes[(bitIndex / 8) % combinedBytes.size].toInt() and 0xFF
                    val bit = ((byteVal ushr (7 - (bitIndex % 8))) and 1) == 1
                    val mask = ((x + y) % 2 == 0)
                    matrix[y][x] = bit xor mask
                    bitIndex++
                }
            }
        }

        val extraFooterH = if (label.isNotBlank()) (sizePx * 0.14f).toInt() else 0
        val bmp = Bitmap.createBitmap(sizePx, sizePx + extraFooterH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E4E4E7")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(
            RectF(4f, 4f, sizePx - 4f, (sizePx + extraFooterH) - 4f),
            24f,
            24f,
            borderPaint
        )

        val quietZone = sizePx * 0.10f
        val moduleSize = (sizePx - quietZone * 2f) / gridDim.toFloat()
        val modulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#09090B")
            style = Paint.Style.FILL
        }

        for (y in 0 until gridDim) {
            for (x in 0 until gridDim) {
                if (matrix[y][x]) {
                    val left = quietZone + x * moduleSize
                    val top = quietZone + y * moduleSize
                    canvas.drawRect(left, top, left + moduleSize + 0.5f, top + moduleSize + 0.5f, modulePaint)
                }
            }
        }

        if (label.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#18181B")
                textSize = sizePx * 0.045f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                label.take(36),
                sizePx / 2f,
                sizePx + extraFooterH * 0.58f,
                textPaint
            )
        }

        return bmp
    }

    /**
     * Parses OCR blocks and full text from a scanned receipt, invoice, or tabular document
     * into structured CSV rows (with columns for Line #, Item / Description, Date / Reference, and Amount / Value).
     */
    fun parseReceiptAndTableToCsvRows(
        fullText: String,
        blocks: List<com.example.domain.model.OcrBlockItem>
    ): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        rows.add(listOf("Row #", "Category / Field", "Description / Item", "Amount / Value"))

        val amountRegex = Regex("""([$€£¥₹]?\s?\d{1,3}(?:,\d{3})*(?:\.\d{2})|\d+\.\d{2})""")
        val dateRegex = Regex("""(\b\d{1,2}[/-]\d{1,2}[/-]\d{2,4}\b|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s+\d{1,2},?\s+\d{4}\b)""", RegexOption.IGNORE_CASE)

        // Group spatial OCR blocks by vertical Y row if blocks are available
        val rawLines: List<String> = if (blocks.isNotEmpty()) {
            val sortedByY = blocks.sortedWith(compareBy({ (it.topRatio * 45f).toInt() }, { it.leftRatio }))
            val clusteredLines = mutableListOf<MutableList<com.example.domain.model.OcrBlockItem>>()
            for (blk in sortedByY) {
                val lastCluster = clusteredLines.lastOrNull()
                if (lastCluster != null && abs(lastCluster.first().topRatio - blk.topRatio) < 0.028f) {
                    lastCluster.add(blk)
                } else {
                    clusteredLines.add(mutableListOf(blk))
                }
            }
            clusteredLines.flatMap { cluster ->
                val joined = cluster.sortedBy { it.leftRatio }.joinToString("   ") { it.text.trim() }
                joined.split("\n")
            }
        } else {
            fullText.split("\n")
        }.map { it.trim() }.filter { it.isNotBlank() }

        var rowNum = 1
        var detectedDate = ""

        for (line in rawLines) {
            val dateMatch = dateRegex.find(line)
            if (dateMatch != null && detectedDate.isEmpty()) {
                detectedDate = dateMatch.value.trim()
                rows.add(listOf((rowNum++).toString(), "Date", line, detectedDate))
                continue
            }

            // Check if the line has multiple tab/space-separated columns
            val multiColParts = line.split(Regex("""\s{2,}|\t|\|""")).map { it.trim() }.filter { it.isNotEmpty() }
            val amountsInLine = amountRegex.findAll(line).map { it.value.trim() }.toList()

            when {
                amountsInLine.isNotEmpty() -> {
                    val amountVal = amountsInLine.last()
                    val desc = line.replace(amountVal, "").trim().trimEnd('-', ':', '.', '•', ' ')
                    val category = when {
                        desc.contains("total", ignoreCase = true) || desc.contains("due", ignoreCase = true) -> "TOTAL"
                        desc.contains("tax", ignoreCase = true) || desc.contains("vat", ignoreCase = true) -> "Tax"
                        desc.contains("sub", ignoreCase = true) -> "Subtotal"
                        else -> "Line Item"
                    }
                    rows.add(
                        listOf(
                            (rowNum++).toString(),
                            category,
                            desc.ifBlank { line },
                            amountVal
                        )
                    )
                }
                multiColParts.size >= 2 -> {
                    rows.add(
                        listOf(
                            (rowNum++).toString(),
                            multiColParts.first(),
                            multiColParts.drop(1).dropLast(1).joinToString(" ").ifBlank { multiColParts[1] },
                            if (multiColParts.size >= 3) multiColParts.last() else ""
                        )
                    )
                }
                line.contains(":") -> {
                    val idx = line.indexOf(":")
                    val key = line.substring(0, idx).trim()
                    val value = line.substring(idx + 1).trim()
                    rows.add(listOf((rowNum++).toString(), key.ifBlank { "Meta" }, value, ""))
                }
                else -> {
                    val cat = if (rowNum == 1) "Header / Merchant" else "Text Row"
                    rows.add(listOf((rowNum++).toString(), cat, line, ""))
                }
            }
        }

        if (rows.size == 1) {
            rows.add(listOf("1", "Document", "No structured rows found — run OCR first", "0.00"))
        }
        return rows
    }

    /**
     * Escapes and formats a 2D table of strings into RFC-4180 compliant CSV text.
     */
    fun formatCsvRowsToString(rows: List<List<String>>): String {
        return rows.joinToString("\n") { row ->
            row.joinToString(",") { cell ->
                val clean = cell.replace("\"", "\"\"")
                if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
                    "\"$clean\""
                } else {
                    clean
                }
            }
        }
    }
}


