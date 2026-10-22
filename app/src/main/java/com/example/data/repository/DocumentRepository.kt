package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.DocumentDao
import com.example.data.local.FolderEntity
import com.example.data.local.ScannedDocumentEntity
import com.example.data.local.ScannedPageEntity
import com.example.data.remote.CloudDocument
import com.example.data.remote.CloudFolder
import com.example.data.remote.CloudSyncRepository
import com.example.domain.model.OcrBlockItem
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PdfPageSize
import com.example.domain.model.PdfQuality
import com.example.domain.model.ScanFilter
import com.example.utils.ImageProcessor
import com.example.utils.OcrAndBarcodeEngine
import com.example.utils.PdfEngine
import com.example.utils.PdfPageInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class DraftScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val originalBitmap: Bitmap,
    val croppedBitmap: Bitmap,
    val enhancedBitmap: Bitmap,
    val filter: ScanFilter = ScanFilter.BW,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val rotationDegrees: Int = 0,
    val ocrText: String = "",
    val ocrBlocks: List<OcrBlockItem> = emptyList()
)

class DocumentRepository(
    private val context: Context,
    private val documentDao: DocumentDao,
    private val cloudSyncRepository: CloudSyncRepository
) {
    val allDocuments: Flow<List<ScannedDocumentEntity>> = documentDao.observeAllDocuments()
    val allFolders: Flow<List<FolderEntity>> = documentDao.observeAllFolders()

    fun observeDocumentById(docId: String): Flow<ScannedDocumentEntity?> = documentDao.observeDocumentById(docId)
    fun observePagesForDocument(docId: String): Flow<List<ScannedPageEntity>> = documentDao.observePagesForDocument(docId)
    fun observeCloudDocuments(userId: String): Flow<List<CloudDocument>> = cloudSyncRepository.observeDocuments(userId)
    fun observeCloudFolders(userId: String): Flow<List<CloudFolder>> = cloudSyncRepository.observeFolders(userId)

    suspend fun getDocumentById(docId: String): ScannedDocumentEntity? = documentDao.getDocumentById(docId)
    suspend fun getPagesForDocument(docId: String): List<ScannedPageEntity> = documentDao.getPagesForDocument(docId)

    /**
     * Seeds default folders and a sample multi-feature invoice scan on first launch if library is empty.
     */
    suspend fun ensureStarterDataIfEmpty(autoSyncToCloud: Boolean = true) = withContext(Dispatchers.IO) {
        val currentFolders = documentDao.observeAllFolders().first()
        if (currentFolders.isEmpty()) {
            val defaultFolders = listOf(
                FolderEntity(id = "folder_receipts", name = "Receipts & Tax", colorHex = "#0EA5E9"),
                FolderEntity(id = "folder_contracts", name = "Legal & Contracts", colorHex = "#6366F1"),
                FolderEntity(id = "folder_ids", name = "ID & Cards", colorHex = "#10B981")
            )
            defaultFolders.forEach { folder ->
                documentDao.upsertFolder(folder)
                if (autoSyncToCloud) {
                    cloudSyncRepository.upsertFolder(folder.id, folder.name, folder.colorHex)
                }
            }
        }

        val currentDocs = documentDao.observeAllDocuments().first()
        if (currentDocs.isEmpty()) {
            val sampleBmp = ImageProcessor.generateSampleInvoiceBitmap()
            val sampleOcr = """
                APEX CLOUD SYSTEMS INC.
                INVOICE #INV-2026-0842 • DATE: OCT 21, 2026
                Bill To: Horizon Design Studio, 740 Market St, San Francisco, CA
                1. Enterprise OCR & Document Pipeline License (Annual) - $1,200.00
                2. Cloud Vault Multi-Device Encrypted Sync (5 Seats) - $450.00
                3. High-Precision PDF Archival Module - $290.00
                TOTAL DUE (USD): $1,940.00
                Payment Terms: Net 30 • Status: PAID IN FULL
                Authorized Signature: A. Vance, VP Finance
            """.trimIndent()
            val sampleBlocks = listOf(
                OcrBlockItem("APEX CLOUD SYSTEMS INC.", 0.07f, 0.08f, 0.68f, 0.12f),
                OcrBlockItem("INVOICE #INV-2026-0842 • DATE: OCT 21, 2026", 0.07f, 0.13f, 0.72f, 0.16f),
                OcrBlockItem("TOTAL DUE (USD): $1,940.00", 0.50f, 0.48f, 0.88f, 0.53f),
                OcrBlockItem("Status: PAID IN FULL", 0.07f, 0.57f, 0.55f, 0.61f)
            )
            val draft = DraftScannedPage(
                originalBitmap = sampleBmp,
                croppedBitmap = sampleBmp,
                enhancedBitmap = sampleBmp,
                filter = ScanFilter.BW,
                ocrText = sampleOcr,
                ocrBlocks = sampleBlocks
            )
            saveDraftDocument(
                title = "Apex Cloud Invoice #0842",
                category = "Receipts & Invoices",
                folderId = "folder_receipts",
                pages = listOf(draft),
                pageSize = PdfPageSize.A4,
                quality = PdfQuality.HIGH,
                isSearchable = true,
                aiSummary = "Paid annual invoice #INV-2026-0842 from Apex Cloud Systems Inc. to Horizon Design Studio totaling $1,940.00.",
                tags = listOf("invoice", "paid", "cloud", "tax"),
                syncToCloud = autoSyncToCloud
            )
        }
    }

    /**
     * Saves a multi-page scanned document to internal files, generates a searchable PDF, records in Room,
     * and syncs metadata + OCR + AI summary to Cloud Firestore.
     */
    suspend fun saveDraftDocument(
        existingDocId: String? = null,
        title: String,
        category: String = "Scans",
        folderId: String = "",
        pages: List<DraftScannedPage>,
        pageSize: PdfPageSize = PdfPageSize.A4,
        quality: PdfQuality = PdfQuality.HIGH,
        isSearchable: Boolean = true,
        watermarkText: String = "",
        pageNumberFormat: PageNumberFormat? = null,
        pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_RIGHT,
        signatureBitmap: Bitmap? = null,
        aiSummary: String = "",
        tags: List<String> = emptyList(),
        password: String = "",
        syncToCloud: Boolean = true
    ): ScannedDocumentEntity = withContext(Dispatchers.IO) {
        val docId = existingDocId ?: "doc_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val now = System.currentTimeMillis()

        val pageEntities = mutableListOf<ScannedPageEntity>()
        val pdfInputs = mutableListOf<PdfPageInput>()

        pages.forEachIndexed { index, draft ->
            val origFile = ImageProcessor.saveBitmapToInternalFile(context, draft.originalBitmap, "${docId}_orig_$index", 90)
            val procFile = ImageProcessor.saveBitmapToInternalFile(context, draft.enhancedBitmap, "${docId}_proc_$index", quality.jpegQuality)

            pageEntities.add(
                ScannedPageEntity(
                    id = draft.id,
                    documentId = docId,
                    pageIndex = index,
                    originalImagePath = origFile.absolutePath,
                    processedImagePath = procFile.absolutePath,
                    filterName = draft.filter.name,
                    brightness = draft.brightness,
                    contrast = draft.contrast,
                    rotationDegrees = draft.rotationDegrees,
                    ocrText = draft.ocrText,
                    ocrBlocksJson = OcrAndBarcodeEngine.encodeBlocksToJson(draft.ocrBlocks),
                    createdAt = now
                )
            )
            pdfInputs.add(
                PdfPageInput(
                    bitmap = draft.enhancedBitmap,
                    ocrBlocks = draft.ocrBlocks,
                    fullOcrText = draft.ocrText
                )
            )
        }

        val pdfFile = PdfEngine.generatePdfFromPages(
            context = context,
            outputFileName = title.ifBlank { "DocuScan_$docId" },
            pages = pdfInputs,
            pageSize = pageSize,
            quality = quality,
            isSearchable = isSearchable,
            watermarkText = watermarkText,
            pageNumberFormat = pageNumberFormat,
            pageNumberPosition = pageNumberPosition,
            signatureBitmap = signatureBitmap
        )

        val combinedOcr = pages.mapIndexedNotNull { idx, p ->
            if (p.ocrText.isBlank()) null else "--- Page ${idx + 1} ---\n${p.ocrText.trim()}"
        }.joinToString("\n\n")

        val passHash = PdfEngine.hashPassword(password)
        val thumbPath = pageEntities.firstOrNull()?.processedImagePath ?: ""

        var synced = false
        if (syncToCloud) {
            val cloudResult = cloudSyncRepository.upsertDocument(
                docId = docId,
                title = title.ifBlank { "Scanned Document" },
                folderId = folderId,
                category = category,
                pageCount = pages.size.coerceAtLeast(1),
                fileSizeBytes = pdfFile.length(),
                ocrText = combinedOcr,
                aiSummary = aiSummary,
                tags = tags,
                isFavorite = false,
                isPasswordProtected = passHash.isNotEmpty(),
                pageSize = pageSize.name
            )
            synced = cloudResult.isSuccess
        }

        val docEntity = ScannedDocumentEntity(
            id = docId,
            title = title.ifBlank { "Scanned Document" },
            category = category,
            folderId = folderId,
            pdfFilePath = pdfFile.absolutePath,
            thumbnailPath = thumbPath,
            pageCount = pages.size.coerceAtLeast(1),
            fileSizeBytes = pdfFile.length(),
            ocrText = combinedOcr,
            aiSummary = aiSummary,
            tagsCsv = tags.joinToString(","),
            pageSize = pageSize.name,
            quality = quality.name,
            isSearchablePdf = isSearchable,
            isFavorite = false,
            isPasswordProtected = passHash.isNotEmpty(),
            passwordHash = passHash,
            watermarkText = watermarkText,
            isCloudSynced = synced,
            createdAt = now,
            updatedAt = now
        )

        documentDao.saveDocumentWithPages(docEntity, pageEntities)
        docEntity
    }

    suspend fun updateDocumentMetadata(
        doc: ScannedDocumentEntity,
        syncToCloud: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val updated = doc.copy(updatedAt = System.currentTimeMillis())
        documentDao.updateDocument(updated)
        if (syncToCloud) {
            val res = cloudSyncRepository.upsertDocument(
                docId = updated.id,
                title = updated.title,
                folderId = updated.folderId,
                category = updated.category,
                pageCount = updated.pageCount,
                fileSizeBytes = updated.fileSizeBytes,
                ocrText = updated.ocrText,
                aiSummary = updated.aiSummary,
                tags = updated.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                isFavorite = updated.isFavorite,
                isPasswordProtected = updated.isPasswordProtected,
                pageSize = updated.pageSize
            )
            if (res.isSuccess && !updated.isCloudSynced) {
                documentDao.updateDocument(updated.copy(isCloudSynced = true))
            }
        }
    }

    /**
     * Rebuilds a document's PDF after page reordering, rotation, deletion, watermarking, signature, or compression.
     */
    suspend fun rebuildDocumentPdf(
        docId: String,
        newTitle: String? = null,
        quality: PdfQuality? = null,
        pageSize: PdfPageSize? = null,
        watermarkText: String? = null,
        pageNumberFormat: PageNumberFormat? = null,
        pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_RIGHT,
        signatureBitmap: Bitmap? = null,
        signaturePageIndex: Int = -1
    ): ScannedDocumentEntity? = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(docId) ?: return@withContext null
        val pages = documentDao.getPagesForDocument(docId).sortedBy { it.pageIndex }
        if (pages.isEmpty()) return@withContext doc

        val inputs = pages.mapNotNull { pageEntity ->
            val bmp = ImageProcessor.loadBitmapFromPath(pageEntity.processedImagePath) ?: return@mapNotNull null
            PdfPageInput(
                bitmap = bmp,
                ocrBlocks = OcrAndBarcodeEngine.decodeBlocksFromJson(pageEntity.ocrBlocksJson),
                fullOcrText = pageEntity.ocrText
            )
        }
        val targetQuality = quality ?: runCatching { PdfQuality.valueOf(doc.quality) }.getOrDefault(PdfQuality.HIGH)
        val targetPageSize = pageSize ?: runCatching { PdfPageSize.valueOf(doc.pageSize) }.getOrDefault(PdfPageSize.A4)
        val targetTitle = newTitle ?: doc.title
        val targetWatermark = watermarkText ?: doc.watermarkText

        val newPdf = PdfEngine.generatePdfFromPages(
            context = context,
            outputFileName = targetTitle,
            pages = inputs,
            pageSize = targetPageSize,
            quality = targetQuality,
            isSearchable = doc.isSearchablePdf,
            watermarkText = targetWatermark,
            pageNumberFormat = pageNumberFormat,
            pageNumberPosition = pageNumberPosition,
            signatureBitmap = signatureBitmap,
            signaturePageIndex = signaturePageIndex
        )

        val combinedOcr = pages.mapIndexedNotNull { idx, p ->
            if (p.ocrText.isBlank()) null else "--- Page ${idx + 1} ---\n${p.ocrText.trim()}"
        }.joinToString("\n\n")

        val updatedDoc = doc.copy(
            title = targetTitle,
            pdfFilePath = newPdf.absolutePath,
            thumbnailPath = pages.first().processedImagePath,
            pageCount = pages.size,
            fileSizeBytes = newPdf.length(),
            ocrText = combinedOcr,
            quality = targetQuality.name,
            pageSize = targetPageSize.name,
            watermarkText = targetWatermark,
            updatedAt = System.currentTimeMillis()
        )
        updateDocumentMetadata(updatedDoc)
        updatedDoc
    }

    suspend fun reorderOrModifyPages(
        docId: String,
        updatedPages: List<ScannedPageEntity>
    ) = withContext(Dispatchers.IO) {
        val reindexed = updatedPages.mapIndexed { idx, p -> p.copy(pageIndex = idx) }
        documentDao.deletePagesForDocument(docId)
        documentDao.upsertPages(reindexed)
        rebuildDocumentPdf(docId)
    }

    suspend fun rotateSinglePage(docId: String, pageId: String) = withContext(Dispatchers.IO) {
        val page = documentDao.getPageById(pageId) ?: return@withContext
        val bmp = ImageProcessor.loadBitmapFromPath(page.processedImagePath) ?: return@withContext
        val rotatedBmp = ImageProcessor.rotateBitmap(bmp, 90)
        val newFile = ImageProcessor.saveBitmapToInternalFile(context, rotatedBmp, "${docId}_rot_${page.pageIndex}", 92)
        documentDao.upsertPage(
            page.copy(
                processedImagePath = newFile.absolutePath,
                rotationDegrees = (page.rotationDegrees + 90) % 360
            )
        )
        rebuildDocumentPdf(docId)
    }

    suspend fun updatePageOcr(
        docId: String,
        page: ScannedPageEntity,
        ocrText: String,
        blocksJson: String
    ) = withContext(Dispatchers.IO) {
        documentDao.upsertPage(
            page.copy(
                ocrText = ocrText,
                ocrBlocksJson = blocksJson
            )
        )
        rebuildDocumentPdf(docId)
    }

    suspend fun applyFilterToSavedDocument(
        docId: String,
        targetFilter: ScanFilter
    ) = withContext(Dispatchers.IO) {
        val pages = documentDao.getPagesForDocument(docId).sortedBy { it.pageIndex }
        for (page in pages) {
            val baseBmp = ImageProcessor.loadBitmapFromPath(page.originalImagePath)
                ?: ImageProcessor.loadBitmapFromPath(page.processedImagePath)
                ?: continue
            val enhanced = ImageProcessor.applyEnhancement(
                source = baseBmp,
                filter = targetFilter,
                brightness = page.brightness,
                contrast = page.contrast,
                rotationDegrees = page.rotationDegrees
            )
            val newFile = ImageProcessor.saveBitmapToInternalFile(
                context = context,
                bitmap = enhanced,
                prefix = "${docId}_${targetFilter.name.lowercase()}_${page.pageIndex}",
                quality = 92
            )
            documentDao.upsertPage(
                page.copy(
                    processedImagePath = newFile.absolutePath,
                    filterName = targetFilter.name
                )
            )
        }
        rebuildDocumentPdf(docId)
    }

    suspend fun deleteDocument(docId: String) = withContext(Dispatchers.IO) {
        val doc = documentDao.getDocumentById(docId)
        val pages = documentDao.getPagesForDocument(docId)
        pages.forEach {
            runCatching { File(it.originalImagePath).delete() }
            runCatching { File(it.processedImagePath).delete() }
        }
        doc?.let { runCatching { File(it.pdfFilePath).delete() } }
        documentDao.deleteDocumentById(docId)
        cloudSyncRepository.deleteDocument(docId)
    }

    suspend fun deleteMultipleDocuments(docIds: List<String>) = withContext(Dispatchers.IO) {
        docIds.forEach { deleteDocument(it) }
    }

    suspend fun createFolder(name: String, colorHex: String = "#2563EB"): FolderEntity = withContext(Dispatchers.IO) {
        val id = "folder_${UUID.randomUUID().toString().take(8)}"
        val folder = FolderEntity(id = id, name = name.trim(), colorHex = colorHex)
        documentDao.upsertFolder(folder)
        cloudSyncRepository.upsertFolder(id, folder.name, folder.colorHex)
        folder
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        documentDao.clearFolderFromDocuments(folderId)
        documentDao.deleteFolderById(folderId)
        cloudSyncRepository.deleteFolder(folderId)
    }

    /**
     * Pulls any cloud-only documents/folders from Firestore into local Room database so multi-device sync is bidirectional.
     */
    suspend fun syncFromCloudToLocal(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val cloudDocs = cloudSyncRepository.getUserDocuments().getOrThrow()
            var importedCount = 0
            for (cDoc in cloudDocs) {
                val local = documentDao.getDocumentById(cDoc.id)
                if (local == null) {
                    val bmp = ImageProcessor.generateSampleInvoiceBitmap()
                    val draft = DraftScannedPage(
                        originalBitmap = bmp,
                        croppedBitmap = bmp,
                        enhancedBitmap = bmp,
                        ocrText = cDoc.ocrText
                    )
                    saveDraftDocument(
                        existingDocId = cDoc.id,
                        title = cDoc.title,
                        category = cDoc.category,
                        folderId = cDoc.folderId,
                        pages = listOf(draft),
                        pageSize = runCatching { PdfPageSize.valueOf(cDoc.pageSize) }.getOrDefault(PdfPageSize.A4),
                        aiSummary = cDoc.aiSummary,
                        tags = cDoc.tags,
                        syncToCloud = false
                    )
                    importedCount++
                }
            }
            importedCount
        }
    }
}
