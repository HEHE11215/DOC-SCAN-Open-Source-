package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.domain.model.BusinessCardInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiDocumentAssistant {

    companion object {
        // Explicit models required by user feature blocks:
        // 1. Complex tasks, Image analysis, and High Thinking: gemini-4-argon
        const val MODEL_COMPLEX_AND_VISION = "gemini-4-argon"
        // 2. General tasks: gemini-3.8-flash
        const val MODEL_GENERAL = "gemini-3.8-flash"
        // 3. Fast low-latency tasks: gemini-3.1-flash-lite
        const val MODEL_FAST_LITE = "gemini-3.1-flash-lite"

        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun Bitmap.toBase64Jpeg(quality: Int = 82): String {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun checkApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Please set your GEMINI_API_KEY in the AI Studio Secrets panel to enable Gemini AI features.")
        }
        return key
    }

    /**
     * Low-latency fast response using gemini-3.1-flash-lite:
     * Generates a smart document title, category, and up to 4 tags in milliseconds.
     */
    suspend fun suggestQuickMetadata(ocrText: String): Result<Triple<String, String, List<String>>> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = checkApiKey()
            val sample = ocrText.take(2500).ifBlank { "Scanned document page" }
            val prompt = """
                Based on this OCR text from a scanned document, respond ONLY with a JSON object containing:
                - "title": concise professional title (max 45 chars)
                - "category": one of ["Scans", "ID Cards", "Business Cards", "Receipts & Invoices", "Contracts"]
                - "tags": comma-separated list of up to 4 short lowercase tags
                
                OCR Text:
                $sample
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2f
                )
            )
            val response = service.generateContent(MODEL_FAST_LITE, apiKey, request)
            val raw = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "{}"
            val obj = json.parseToJsonElement(raw).jsonObject
            val title = obj["title"]?.jsonPrimitive?.content ?: "Scanned Document"
            val category = obj["category"]?.jsonPrimitive?.content ?: "Scans"
            val tagsStr = obj["tags"]?.jsonPrimitive?.content ?: "scan,document"
            val tags = tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(4)
            Triple(title, category, tags)
        }
    }

    /**
     * Low-latency fast OCR cleanup or translation using gemini-3.1-flash-lite.
     */
    suspend fun fastCleanOrTranslateText(
        text: String,
        targetTask: String = "Fix OCR typos, formatting, and punctuation while preserving all original meaning."
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = checkApiKey()
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = "$targetTask\n\nDocument Text:\n${text.take(12000)}")
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = 0.2f)
            )
            val response = service.generateContent(MODEL_FAST_LITE, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No output returned."
        }
    }

    /**
     * General document task using gemini-3.8-flash:
     * Summarizes, extracts action items, or formats a document into clean Markdown.
     */
    suspend fun summarizeDocumentGeneral(ocrText: String, instruction: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = checkApiKey()
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(
                                text = """
                                    You are an expert document assistant inside DocuScan.
                                    Task: $instruction
                                    
                                    Document Content:
                                    ${ocrText.take(25000)}
                                """.trimIndent()
                            )
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = 0.4f)
            )
            val response = service.generateContent(MODEL_GENERAL, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Unable to generate summary."
        }
    }

    /**
     * Image understanding using gemini-4-argon:
     * Analyzes an uploaded or scanned document photo directly (tables, handwriting, receipts, diagrams, or Urdu/Arabic/multilingual script).
     */
    suspend fun analyzeDocumentImage(
        bitmap: Bitmap,
        userPrompt: String = "Analyze this document image thoroughly. Extract all key fields, tables, dates, totals, and summarize its purpose."
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = checkApiKey()
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = userPrompt),
                            Part(
                                inlineData = InlineData(
                                    mimeType = "image/jpeg",
                                    data = bitmap.toBase64Jpeg(82)
                                )
                            )
                        )
                    )
                )
            )
            val response = service.generateContent(MODEL_COMPLEX_AND_VISION, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No image analysis returned."
        }
    }

    /**
     * High Thinking Mode using gemini-4-argon with ThinkingLevel.HIGH (thinkingLevel = "HIGH") and NO maxOutputTokens:
     * Performs deep legal/financial/contract reasoning or complex multi-page document Q&A.
     */
    suspend fun deepThinkDocumentAnalysis(
        documentText: String,
        complexQuery: String,
        optionalBitmap: Bitmap? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = checkApiKey()
            val parts = buildList {
                add(
                    Part(
                        text = """
                            Perform a deep, rigorous analysis of this document to answer the user's complex query.
                            Highlight key clauses, risks, calculations, obligations, or inconsistencies if applicable.
                            
                            User Query: $complexQuery
                            
                            Extracted Document Text:
                            ${documentText.take(35000)}
                        """.trimIndent()
                    )
                )
                if (optionalBitmap != null) {
                    add(
                        Part(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = optionalBitmap.toBase64Jpeg(80)
                            )
                        )
                    )
                }
            }
            // Note: Do not set maxOutputTokens as required by Enable High Thinking guidelines
            val request = GenerateContentRequest(
                contents = listOf(Content(parts = parts)),
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "HIGH")
                )
            )
            val response = service.generateContent(MODEL_COMPLEX_AND_VISION, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n\n")
                ?.ifBlank { null }
                ?: "No response generated."
        }
    }

    /**
     * Parses a Business Card image + OCR text into structured contact fields using gemini-4-argon (with offline regex fallback).
     */
    suspend fun extractBusinessCard(bitmap: Bitmap, ocrFallbackText: String): BusinessCardInfo = withContext(Dispatchers.IO) {
        val offlineParsed = parseBusinessCardOffline(ocrFallbackText)
        try {
            val apiKey = checkApiKey()
            val prompt = """
                Extract the contact details from this business card image and OCR text.
                Return ONLY a JSON object with keys:
                "fullName", "jobTitle", "company", "phone", "email", "website", "address".
                
                OCR Text:
                $ocrFallbackText
            """.trimIndent()
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64Jpeg(80)))
                        )
                    )
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )
            val response = service.generateContent(MODEL_COMPLEX_AND_VISION, apiKey, request)
            val raw = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: return@withContext offlineParsed
            val obj = json.parseToJsonElement(raw).jsonObject
            BusinessCardInfo(
                fullName = obj["fullName"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.fullName } ?: offlineParsed.fullName,
                jobTitle = obj["jobTitle"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.jobTitle } ?: offlineParsed.jobTitle,
                company = obj["company"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.company } ?: offlineParsed.company,
                phone = obj["phone"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.phone } ?: offlineParsed.phone,
                email = obj["email"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.email } ?: offlineParsed.email,
                website = obj["website"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.website } ?: offlineParsed.website,
                address = obj["address"]?.jsonPrimitive?.content?.ifBlank { offlineParsed.address } ?: offlineParsed.address,
                rawText = ocrFallbackText
            )
        } catch (e: Exception) {
            offlineParsed
        }
    }

    fun parseBusinessCardOffline(ocrText: String): BusinessCardInfo {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val emailRegex = Regex("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}")
        val phoneRegex = Regex("(\\+?\\d[\\d\\s\\-().]{7,}\\d)")
        val webRegex = Regex("((https?://)?(www\\.)?[a-zA-Z0-9\\-]+\\.(com|org|net|io|co|ai|edu|gov)(/\\S*)?)", RegexOption.IGNORE_CASE)

        val email = emailRegex.find(ocrText)?.value ?: ""
        val phone = phoneRegex.find(ocrText)?.value?.trim() ?: ""
        val website = webRegex.find(ocrText.replace(email, ""))?.value ?: ""

        val remainingLines = lines.filter { line ->
            !line.contains(email, ignoreCase = true).and(email.isNotEmpty()) &&
            !line.contains(phone).and(phone.isNotEmpty())
        }
        val fullName = remainingLines.getOrNull(0) ?: "Contact"
        val jobTitle = remainingLines.getOrNull(1) ?: ""
        val company = remainingLines.getOrNull(2) ?: ""
        val address = remainingLines.drop(3).joinToString(", ")

        return BusinessCardInfo(
            fullName = fullName,
            jobTitle = jobTitle,
            company = company,
            phone = phone,
            email = email,
            website = website,
            address = address,
            rawText = ocrText
        )
    }
}
