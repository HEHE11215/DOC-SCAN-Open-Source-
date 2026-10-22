package com.example.domain.model

import android.graphics.PointF
import android.graphics.RectF
import kotlinx.serialization.Serializable

@Serializable
enum class ScanFilter(val label: String) {
    CLEAR("Clear"),
    BW("B&W"),
    AUTO("Auto"),
    MAGIC_COLOR("Magic"),
    GRAYSCALE("Gray"),
    NO_SHADOW("Clean"),
    ORIGINAL("Original")
}

@Serializable
enum class PdfPageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
    ORIGINAL("Original", 0, 0)
}

@Serializable
enum class PdfQuality(val label: String, val jpegQuality: Int, val maxDimension: Int) {
    LOW("Compact", 60, 1200),
    MEDIUM("Standard", 80, 1800),
    HIGH("High", 92, 2480),
    MAX("Lossless", 100, 3200)
}

@Serializable
enum class OcrScript(val label: String, val engineName: String, val isRtl: Boolean = false) {
    LATIN("Latin (EN/ES/FR/DE)", "ML Kit", false),
    DEVANAGARI("Devanagari (Hindi)", "ML Kit", false),
    CHINESE("Chinese", "ML Kit", false),
    JAPANESE("Japanese", "ML Kit", false),
    KOREAN("Korean", "ML Kit", false),
    URDU_ARABIC("Urdu / Arabic", "RTL Engine", true)
}

@Serializable
enum class DocumentCategory(val label: String) {
    ALL("All"),
    SCAN("Scans"),
    ID_CARD("ID Cards"),
    BUSINESS_CARD("Cards"),
    RECEIPT("Receipts"),
    CONTRACT("Contracts"),
    IMPORTED("Imported")
}

@Serializable
enum class SortOption(val label: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    NAME_ASC("Name (A–Z)"),
    SIZE_DESC("Size"),
    PAGES_DESC("Pages")
}

@Serializable
enum class AppThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

data class NormalizedQuad(
    val topLeft: PointF = PointF(0.06f, 0.06f),
    val topRight: PointF = PointF(0.94f, 0.06f),
    val bottomRight: PointF = PointF(0.94f, 0.94f),
    val bottomLeft: PointF = PointF(0.06f, 0.94f)
) {
    fun toFloatArray(width: Float, height: Float): FloatArray = floatArrayOf(
        topLeft.x * width, topLeft.y * height,
        topRight.x * width, topRight.y * height,
        bottomRight.x * width, bottomRight.y * height,
        bottomLeft.x * width, bottomLeft.y * height
    )
}

@Serializable
data class OcrBlockItem(
    val text: String,
    val leftRatio: Float,
    val topRatio: Float,
    val rightRatio: Float,
    val bottomRatio: Float,
    val confidence: Float = 0.95f
) {
    fun toRectF(width: Float, height: Float): RectF = RectF(
        leftRatio * width,
        topRatio * height,
        rightRatio * width,
        bottomRatio * height
    )
}

data class OcrPageResult(
    val fullText: String,
    val blocks: List<OcrBlockItem>,
    val script: OcrScript,
    val engineUsed: String
)

data class BusinessCardInfo(
    val fullName: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val rawText: String = ""
)

data class ScannedBarcodeItem(
    val rawValue: String,
    val displayValue: String,
    val formatLabel: String,
    val typeLabel: String
)

@Serializable
enum class PageNumberPosition(val label: String) {
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_CENTER("Bottom Center"),
    TOP_RIGHT("Top Right")
}

@Serializable
enum class PageNumberFormat(val label: String) {
    PAGE_X_OF_Y("Page 1 of N"),
    SLASH("1 / N"),
    DASHED("- 1 -")
}
