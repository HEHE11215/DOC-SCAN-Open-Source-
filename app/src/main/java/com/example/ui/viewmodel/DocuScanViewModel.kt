package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.GeminiDocumentAssistant
import com.example.data.local.FolderEntity
import com.example.data.local.ScannedDocumentEntity
import com.example.data.local.ScannedPageEntity
import com.example.data.preferences.SettingsRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.DocumentRepository
import com.example.data.repository.DraftScannedPage
import com.example.di.AppContainer
import com.example.domain.model.AppThemeMode
import com.example.domain.model.BusinessCardInfo
import com.example.domain.model.DocumentCategory
import com.example.domain.model.NormalizedQuad
import com.example.domain.model.OcrScript
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PdfPageSize
import com.example.domain.model.PdfQuality
import com.example.domain.model.ScanFilter
import com.example.domain.model.ScannedBarcodeItem
import com.example.domain.model.SortOption
import com.example.utils.ImageProcessor
import com.example.utils.OcrAndBarcodeEngine
import com.example.utils.PdfEngine
import com.example.utils.PdfPageInput
import com.example.utils.ShareExportHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class ScannerCaptureMode(val label: String) {
    STANDARD_BATCH("Batch Document"),
    ID_CARD("ID Card (Front + Back)"),
    BUSINESS_CARD("Business Card"),
    QR_BARCODE("QR & Barcode")
}

data class ActiveScanSessionState(
    val captureMode: ScannerCaptureMode = ScannerCaptureMode.STANDARD_BATCH,
    val pages: List<DraftScannedPage> = emptyList(),
    val activePageIndex: Int = 0,
    val pendingRawBitmap: Bitmap? = null,
    val detectedQuad: NormalizedQuad = NormalizedQuad(),
    val isCroppingStage: Boolean = false,
    val isEditingExistingPageCrop: Boolean = false,
    val selectedCropFilter: ScanFilter = ScanFilter.CLEAR,
    val idCardFrontBitmap: Bitmap? = null,
    val idCardBackBitmap: Bitmap? = null,
    val businessCardInfo: BusinessCardInfo? = null,
    val scannedBarcodes: List<ScannedBarcodeItem> = emptyList(),
    val selectedOcrScript: OcrScript = OcrScript.LATIN,
    val isProcessing: Boolean = false,
    val processingMessage: String = ""
)

data class AiAssistantState(
    val isAnalyzing: Boolean = false,
    val activeModelBadge: String = "",
    val outputText: String = "",
    val uploadedBitmap: Bitmap? = null,
    val errorMessage: String? = null
)

class DocuScanViewModel(
    private val appContext: Context,
    val documentRepository: DocumentRepository,
    val settingsRepository: SettingsRepository,
    val geminiAssistant: GeminiDocumentAssistant,
    val currentUserId: String
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserSettings())

    val folders: StateFlow<List<FolderEntity>> = documentRepository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow(DocumentCategory.ALL)
    val selectedCategory: StateFlow<DocumentCategory> = _selectedCategory.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedDocIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedDocIds: StateFlow<Set<String>> = _selectedDocIds.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _scanSession = MutableStateFlow(ActiveScanSessionState())
    val scanSession: StateFlow<ActiveScanSessionState> = _scanSession.asStateFlow()

    private val _aiState = MutableStateFlow(AiAssistantState())
    val aiState: StateFlow<AiAssistantState> = _aiState.asStateFlow()

    private val _unlockedDocIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedDocIds: StateFlow<Set<String>> = _unlockedDocIds.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    val filteredDocuments: StateFlow<List<ScannedDocumentEntity>> = combine(
        documentRepository.allDocuments,
        _searchQuery,
        _selectedCategory,
        _selectedFolderId,
        combine(_onlyFavorites, _sortOption) { fav, sort -> fav to sort }
    ) { docs, query, category, folderId, (favOnly, sort) ->
        docs.filter { doc ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                doc.title.lowercase().contains(q) ||
                    doc.ocrText.lowercase().contains(q) ||
                    doc.aiSummary.lowercase().contains(q) ||
                    doc.tagsCsv.lowercase().contains(q)
            }
            val matchesCategory = category == DocumentCategory.ALL ||
                doc.category.equals(category.label, ignoreCase = true)
            val matchesFolder = folderId == null || doc.folderId == folderId
            val matchesFav = !favOnly || doc.isFavorite
            matchesQuery && matchesCategory && matchesFolder && matchesFav
        }.let { filtered ->
            when (sort) {
                SortOption.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
                SortOption.DATE_ASC -> filtered.sortedBy { it.createdAt }
                SortOption.NAME_ASC -> filtered.sortedBy { it.title.lowercase() }
                SortOption.SIZE_DESC -> filtered.sortedByDescending { it.fileSizeBytes }
                SortOption.PAGES_DESC -> filtered.sortedByDescending { it.pageCount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            val isSignedIn = currentUserId.isNotBlank() && currentUserId != "guest"
            documentRepository.ensureStarterDataIfEmpty(autoSyncToCloud = isSignedIn)
            if (isSignedIn) {
                documentRepository.syncFromCloudToLocal()
            }
        }
    }

    fun showStatus(message: String) {
        _statusBanner.value = message
    }

    fun clearStatus() {
        _statusBanner.value = null
    }

    fun markAppUnlocked() {
        _isAppUnlocked.value = true
    }

    // --- Library Filter & Selection Actions ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: DocumentCategory) {
        _selectedCategory.value = category
    }

    fun setFolderFilter(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun toggleFavoritesOnly() {
        _onlyFavorites.update { !it }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleDocSelection(docId: String) {
        _selectedDocIds.update { current ->
            if (docId in current) current - docId else current + docId
        }
    }

    fun clearDocSelection() {
        _selectedDocIds.value = emptySet()
    }

    fun toggleFavorite(doc: ScannedDocumentEntity) {
        viewModelScope.launch {
            documentRepository.updateDocumentMetadata(doc.copy(isFavorite = !doc.isFavorite))
        }
    }

    fun renameDocument(doc: ScannedDocumentEntity, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            documentRepository.updateDocumentMetadata(doc.copy(title = newTitle.trim()))
            showStatus("Renamed to \"${newTitle.trim()}\"")
        }
    }

    fun moveDocumentToFolder(doc: ScannedDocumentEntity, folderId: String) {
        viewModelScope.launch {
            documentRepository.updateDocumentMetadata(doc.copy(folderId = folderId))
            showStatus("Moved document to folder")
        }
    }

    fun moveSelectedDocsToFolder(folderId: String) {
        val ids = _selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                val doc = documentRepository.getDocumentById(id)
                if (doc != null) {
                    documentRepository.updateDocumentMetadata(doc.copy(folderId = folderId))
                }
            }
            clearDocSelection()
            showStatus("Moved ${ids.size} document(s) to folder")
        }
    }

    fun deleteDocument(docId: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(docId)
            showStatus("Document deleted")
        }
    }

    fun deleteSelectedDocuments() {
        val ids = _selectedDocIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            documentRepository.deleteMultipleDocuments(ids)
            clearDocSelection()
            showStatus("Deleted ${ids.size} document(s)")
        }
    }

    fun createNewFolder(name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            documentRepository.createFolder(name, colorHex)
            showStatus("Folder \"${name.trim()}\" created")
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            if (_selectedFolderId.value == folderId) _selectedFolderId.value = null
            documentRepository.deleteFolder(folderId)
            showStatus("Folder removed")
        }
    }

    fun syncLibraryWithCloud() {
        viewModelScope.launch {
            val res = documentRepository.syncFromCloudToLocal()
            res.onSuccess { count ->
                showStatus("Cloud sync complete ($count new cloud item(s) pulled)")
            }.onFailure { err ->
                showStatus("Cloud sync status: ${err.localizedMessage ?: "Offline mode active"}")
            }
        }
    }

    // --- Scanner & Edge Detection Pipeline ---

    fun startNewScanSession(mode: ScannerCaptureMode = ScannerCaptureMode.STANDARD_BATCH) {
        val defaultScript = settings.value.defaultOcrScript
        _scanSession.value = ActiveScanSessionState(
            captureMode = mode,
            selectedOcrScript = defaultScript
        )
    }

    fun setScannerCaptureMode(mode: ScannerCaptureMode) {
        _scanSession.update { it.copy(captureMode = mode) }
    }

    /**
     * Called when CameraX captures a frame or user selects an image from Gallery.
     * Runs automatic Sobel edge detection and transitions into the interactive 4-corner adjustment overlay.
     */
    fun onRawBitmapCaptured(bitmap: Bitmap) {
        viewModelScope.launch {
            val mode = _scanSession.value.captureMode
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Detecting document edges...")
            }

            when (mode) {
                ScannerCaptureMode.QR_BARCODE -> {
                    val codes = OcrAndBarcodeEngine.scanBarcodes(bitmap)
                    _scanSession.update {
                        it.copy(
                            scannedBarcodes = codes,
                            pendingRawBitmap = bitmap,
                            isProcessing = false,
                            processingMessage = ""
                        )
                    }
                    if (codes.isEmpty()) {
                        showStatus("No QR or barcode found in frame. Try moving closer.")
                    } else {
                        showStatus("Detected ${codes.size} code(s)!")
                    }
                }
                else -> {
                    val quad = ImageProcessor.detectDocumentEdges(bitmap)
                    _scanSession.update {
                        it.copy(
                            pendingRawBitmap = bitmap,
                            detectedQuad = quad,
                            isCroppingStage = true,
                            isProcessing = false,
                            processingMessage = ""
                        )
                    }
                }
            }
        }
    }

    fun onMultipleGalleryUrisSelected(
        uris: List<Uri>,
        onReadyForCrop: () -> Unit = {},
        onReadyForEnhance: () -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Loading ${uris.size} image(s)...")
            }
            if (uris.size == 1) {
                val bmp = ImageProcessor.loadBitmapFromUri(appContext, uris.first())
                if (bmp != null) {
                    val quad = ImageProcessor.detectDocumentEdges(bmp)
                    _scanSession.update {
                        it.copy(
                            pendingRawBitmap = bmp,
                            detectedQuad = quad,
                            isCroppingStage = true,
                            isEditingExistingPageCrop = false,
                            isProcessing = false,
                            processingMessage = ""
                        )
                    }
                    onReadyForCrop()
                } else {
                    _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
                    showStatus("Could not decode selected image")
                }
                return@launch
            }

            // Multi-page batch import: first page goes through interactive or selected filter, all use selectedCropFilter
            val targetFilter = _scanSession.value.selectedCropFilter
            val addedPages = mutableListOf<DraftScannedPage>()
            for (uri in uris) {
                val raw = ImageProcessor.loadBitmapFromUri(appContext, uri) ?: continue
                val quad = ImageProcessor.detectDocumentEdges(raw)
                val warped = ImageProcessor.applyPerspectiveWarp(raw, quad)
                val enhanced = ImageProcessor.applyEnhancement(warped, targetFilter)
                addedPages.add(
                    DraftScannedPage(
                        originalBitmap = raw,
                        croppedBitmap = warped,
                        enhancedBitmap = enhanced,
                        filter = targetFilter
                    )
                )
            }
            _scanSession.update { state ->
                val combined = state.pages + addedPages
                state.copy(
                    pages = combined,
                    activePageIndex = (combined.size - 1).coerceAtLeast(0),
                    isCroppingStage = false,
                    isEditingExistingPageCrop = false,
                    isProcessing = false,
                    processingMessage = ""
                )
            }
            onReadyForEnhance()
        }
    }

    fun setCropStageFilter(filter: ScanFilter) {
        _scanSession.update { it.copy(selectedCropFilter = filter) }
    }

    fun updateQuad(newQuad: NormalizedQuad) {
        _scanSession.update { it.copy(detectedQuad = newQuad) }
    }

    fun resetQuadToFullPage() {
        _scanSession.update {
            it.copy(
                detectedQuad = NormalizedQuad(
                    topLeft = android.graphics.PointF(0.02f, 0.02f),
                    topRight = android.graphics.PointF(0.98f, 0.02f),
                    bottomRight = android.graphics.PointF(0.98f, 0.98f),
                    bottomLeft = android.graphics.PointF(0.02f, 0.98f)
                )
            )
        }
    }

    fun setInsetDocumentQuad() {
        _scanSession.update {
            it.copy(
                detectedQuad = NormalizedQuad(
                    topLeft = android.graphics.PointF(0.10f, 0.10f),
                    topRight = android.graphics.PointF(0.90f, 0.10f),
                    bottomRight = android.graphics.PointF(0.90f, 0.90f),
                    bottomLeft = android.graphics.PointF(0.10f, 0.90f)
                )
            )
        }
    }

    fun reCropActiveDraftPage(onNavigateToCrop: () -> Unit) {
        val state = _scanSession.value
        val activePage = state.pages.getOrNull(state.activePageIndex) ?: return
        viewModelScope.launch {
            val quad = ImageProcessor.detectDocumentEdges(activePage.originalBitmap)
            _scanSession.update {
                it.copy(
                    pendingRawBitmap = activePage.originalBitmap,
                    detectedQuad = quad,
                    selectedCropFilter = activePage.filter,
                    isCroppingStage = true,
                    isEditingExistingPageCrop = true
                )
            }
            onNavigateToCrop()
        }
    }

    fun cancelCropStage() {
        _scanSession.update {
            it.copy(
                pendingRawBitmap = null,
                isCroppingStage = false,
                isEditingExistingPageCrop = false
            )
        }
    }

    /**
     * Confirms the 4-corner perspective adjustment, warps the bitmap, applies selected filter (e.g. B&W Sharp),
     * and handles ID Card / Business Card / Standard Batch flows.
     */
    fun confirmPerspectiveCrop(onNavigateToEnhance: () -> Unit) {
        val state = _scanSession.value
        val raw = state.pendingRawBitmap ?: return
        val chosenFilter = state.selectedCropFilter
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Warping & applying ${chosenFilter.label}...")
            }
            val warped = ImageProcessor.applyPerspectiveWarp(raw, state.detectedQuad)
            val enhanced = ImageProcessor.applyEnhancement(warped, chosenFilter)

            if (state.isEditingExistingPageCrop && state.activePageIndex in state.pages.indices) {
                val existing = state.pages[state.activePageIndex]
                val updatedPage = existing.copy(
                    croppedBitmap = warped,
                    enhancedBitmap = enhanced,
                    filter = chosenFilter
                )
                _scanSession.update { current ->
                    val mutable = current.pages.toMutableList()
                    mutable[current.activePageIndex] = updatedPage
                    current.copy(
                        pages = mutable,
                        pendingRawBitmap = null,
                        isCroppingStage = false,
                        isEditingExistingPageCrop = false,
                        isProcessing = false,
                        processingMessage = ""
                    )
                }
                onNavigateToEnhance()
                return@launch
            }

            when (state.captureMode) {
                ScannerCaptureMode.ID_CARD -> {
                    if (state.idCardFrontBitmap == null) {
                        _scanSession.update {
                            it.copy(
                                idCardFrontBitmap = enhanced,
                                pendingRawBitmap = null,
                                isCroppingStage = false,
                                isProcessing = false,
                                processingMessage = ""
                            )
                        }
                        showStatus("Front side captured! Now scan or import the BACK side of the ID card.")
                    } else {
                        val composite = ImageProcessor.createIdCardCompositePage(
                            frontBitmap = state.idCardFrontBitmap,
                            backBitmap = enhanced
                        )
                        val draftPage = DraftScannedPage(
                            originalBitmap = composite,
                            croppedBitmap = composite,
                            enhancedBitmap = composite,
                            filter = chosenFilter
                        )
                        _scanSession.update {
                            it.copy(
                                idCardBackBitmap = enhanced,
                                pages = listOf(draftPage),
                                activePageIndex = 0,
                                pendingRawBitmap = null,
                                isCroppingStage = false,
                                isProcessing = false,
                                processingMessage = ""
                            )
                        }
                        onNavigateToEnhance()
                    }
                }

                ScannerCaptureMode.BUSINESS_CARD -> {
                    _scanSession.update {
                        it.copy(processingMessage = "Extracting business card contact info...")
                    }
                    val ocrRes = OcrAndBarcodeEngine.recognizeText(enhanced, OcrScript.LATIN)
                    val cardInfo = geminiAssistant.parseBusinessCardOffline(ocrRes.fullText)
                    val draftPage = DraftScannedPage(
                        originalBitmap = raw,
                        croppedBitmap = warped,
                        enhancedBitmap = enhanced,
                        filter = chosenFilter,
                        ocrText = ocrRes.fullText,
                        ocrBlocks = ocrRes.blocks
                    )
                    _scanSession.update {
                        it.copy(
                            pages = listOf(draftPage),
                            activePageIndex = 0,
                            businessCardInfo = cardInfo,
                            pendingRawBitmap = null,
                            isCroppingStage = false,
                            isProcessing = false,
                            processingMessage = ""
                        )
                    }
                    onNavigateToEnhance()
                }

                else -> {
                    val newPage = DraftScannedPage(
                        originalBitmap = raw,
                        croppedBitmap = warped,
                        enhancedBitmap = enhanced,
                        filter = chosenFilter
                    )
                    _scanSession.update { current ->
                        val updatedPages = current.pages + newPage
                        current.copy(
                            pages = updatedPages,
                            activePageIndex = updatedPages.lastIndex,
                            pendingRawBitmap = null,
                            isCroppingStage = false,
                            isProcessing = false,
                            processingMessage = ""
                        )
                    }
                    onNavigateToEnhance()
                }
            }
        }
    }

    // --- Enhancement & OCR Studio Actions ---

    private var enhancementJob: kotlinx.coroutines.Job? = null

    fun selectActiveDraftPage(index: Int) {
        _scanSession.update { state ->
            if (index in state.pages.indices) state.copy(activePageIndex = index) else state
        }
    }

    fun applyFilterAndAdjustmentsToActivePage(
        filter: ScanFilter? = null,
        brightness: Float? = null,
        contrast: Float? = null,
        rotateDelta: Int = 0,
        autoDeskew: Boolean = false
    ) {
        val state = _scanSession.value
        val pageIndex = state.activePageIndex
        val page = state.pages.getOrNull(pageIndex) ?: return
        val newFilter = filter ?: page.filter
        val newBrightness = brightness ?: page.brightness
        val newContrast = contrast ?: page.contrast
        val newRotation = (page.rotationDegrees + rotateDelta) % 360

        // Immediately reflect slider/filter values in state without blocking the UI with a modal spinner
        _scanSession.update { s ->
            val current = s.pages.getOrNull(pageIndex) ?: return@update s
            val mutable = s.pages.toMutableList()
            mutable[pageIndex] = current.copy(
                filter = newFilter,
                brightness = newBrightness,
                contrast = newContrast,
                rotationDegrees = newRotation
            )
            s.copy(pages = mutable)
        }

        enhancementJob?.cancel()
        enhancementJob = viewModelScope.launch {
            val latestPage = _scanSession.value.pages.getOrNull(pageIndex) ?: return@launch
            val baseCropped = if (autoDeskew) {
                ImageProcessor.deskewBitmap(latestPage.croppedBitmap)
            } else {
                latestPage.croppedBitmap
            }
            val resultBmp = ImageProcessor.applyEnhancement(
                source = baseCropped,
                filter = newFilter,
                brightness = newBrightness,
                contrast = newContrast,
                rotationDegrees = newRotation
            )
            _scanSession.update { s ->
                val current = s.pages.getOrNull(pageIndex) ?: return@update s
                val mutable = s.pages.toMutableList()
                mutable[pageIndex] = current.copy(
                    croppedBitmap = baseCropped,
                    enhancedBitmap = resultBmp,
                    filter = newFilter,
                    brightness = newBrightness,
                    contrast = newContrast,
                    rotationDegrees = newRotation
                )
                s.copy(pages = mutable)
            }
        }
    }

    fun deleteActiveDraftPage() {
        _scanSession.update { state ->
            if (state.pages.size <= 1) return@update state
            val mutable = state.pages.toMutableList()
            mutable.removeAt(state.activePageIndex)
            state.copy(
                pages = mutable,
                activePageIndex = state.activePageIndex.coerceAtMost(mutable.lastIndex)
            )
        }
    }

    fun setOcrScript(script: OcrScript) {
        _scanSession.update { it.copy(selectedOcrScript = script) }
    }

    fun runOcrOnActivePage() {
        val state = _scanSession.value
        val page = state.pages.getOrNull(state.activePageIndex) ?: return
        viewModelScope.launch {
            _scanSession.update {
                it.copy(
                    isProcessing = true,
                    processingMessage = "Running ${state.selectedOcrScript.engineName} OCR..."
                )
            }
            val ocrResult = OcrAndBarcodeEngine.recognizeText(page.enhancedBitmap, state.selectedOcrScript)
            val updatedPage = page.copy(
                ocrText = ocrResult.fullText,
                ocrBlocks = ocrResult.blocks
            )
            _scanSession.update { s ->
                val mutable = s.pages.toMutableList()
                if (s.activePageIndex in mutable.indices) {
                    mutable[s.activePageIndex] = updatedPage
                }
                s.copy(pages = mutable, isProcessing = false, processingMessage = "")
            }
            if (ocrResult.fullText.isBlank()) {
                showStatus("No text detected on page. Try switching OCR script or using AI Vision.")
            } else {
                showStatus("Extracted ${ocrResult.blocks.size} text block(s) via ${ocrResult.engineUsed}")
            }
        }
    }

    fun runOcrOnAllDraftPages() {
        val state = _scanSession.value
        if (state.pages.isEmpty()) return
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Running batch OCR on ${state.pages.size} page(s)...")
            }
            val updated = state.pages.map { page ->
                val res = OcrAndBarcodeEngine.recognizeText(page.enhancedBitmap, state.selectedOcrScript)
                page.copy(ocrText = res.fullText, ocrBlocks = res.blocks)
            }
            _scanSession.update {
                it.copy(pages = updated, isProcessing = false, processingMessage = "")
            }
            showStatus("Completed OCR across all ${updated.size} page(s)")
        }
    }

    fun updateActivePageOcrText(newText: String) {
        _scanSession.update { s ->
            val mutable = s.pages.toMutableList()
            val current = mutable.getOrNull(s.activePageIndex) ?: return@update s
            mutable[s.activePageIndex] = current.copy(ocrText = newText)
            s.copy(pages = mutable)
        }
    }

    /**
     * Uses gemini-4-argon Vision to transcribe handwritten/multilingual/Urdu/Arabic or complex table text directly into the active page's OCR buffer.
     */
    fun runGeminiVisionOcrOnActivePage() {
        val state = _scanSession.value
        val page = state.pages.getOrNull(state.activePageIndex) ?: return
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Analyzing page with Gemini (gemini-4-argon)...")
            }
            val res = geminiAssistant.analyzeDocumentImage(
                bitmap = page.enhancedBitmap,
                userPrompt = "Transcribe all visible text accurately (including any Latin, Arabic, Urdu, Devanagari, CJK, or handwritten script) and preserve layout structure."
            )
            res.onSuccess { text ->
                updateActivePageOcrText(text)
                showStatus("Gemini Vision transcription complete")
            }.onFailure { err ->
                showStatus(err.localizedMessage ?: "Gemini Vision failed")
            }
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
        }
    }

    /**
     * Saves the current scan session into Room + generates searchable PDF + syncs with Firestore.
     */
    fun saveCurrentScanAsDocument(
        title: String,
        category: String,
        folderId: String,
        pageSize: PdfPageSize,
        quality: PdfQuality,
        isSearchable: Boolean,
        watermarkText: String,
        password: String,
        onSaved: (String) -> Unit
    ) {
        val state = _scanSession.value
        if (state.pages.isEmpty()) return
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Building Searchable PDF & Syncing...")
            }
            val savedDoc = documentRepository.saveDraftDocument(
                title = title.ifBlank { "DocuScan_${System.currentTimeMillis() / 1000}" },
                category = category,
                folderId = folderId,
                pages = state.pages,
                pageSize = pageSize,
                quality = quality,
                isSearchable = isSearchable,
                watermarkText = watermarkText,
                password = password,
                syncToCloud = settings.value.autoCloudSync
            )
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Saved \"${savedDoc.title}\" (${savedDoc.pageCount} page(s))")
            onSaved(savedDoc.id)
        }
    }

    // --- PDF Toolkit & Convert Center Operations ---

    fun mergeDocumentsByIds(docIds: List<String>, mergedTitle: String, onComplete: (String) -> Unit) {
        if (docIds.size < 2) {
            showStatus("Select at least 2 documents to merge")
            return
        }
        viewModelScope.launch {
            _scanSession.update { it.copy(isProcessing = true, processingMessage = "Merging ${docIds.size} PDFs...") }
            val allPages = mutableListOf<DraftScannedPage>()
            for (id in docIds) {
                val pages = documentRepository.getPagesForDocument(id)
                for (p in pages) {
                    val bmp = ImageProcessor.loadBitmapFromPath(p.processedImagePath) ?: continue
                    allPages.add(
                        DraftScannedPage(
                            originalBitmap = bmp,
                            croppedBitmap = bmp,
                            enhancedBitmap = bmp,
                            ocrText = p.ocrText,
                            ocrBlocks = OcrAndBarcodeEngine.decodeBlocksFromJson(p.ocrBlocksJson)
                        )
                    )
                }
            }
            if (allPages.isEmpty()) {
                _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
                showStatus("Could not load pages to merge")
                return@launch
            }
            val mergedDoc = documentRepository.saveDraftDocument(
                title = mergedTitle.ifBlank { "Merged Document" },
                category = "Scans",
                pages = allPages,
                pageSize = settings.value.defaultPageSize,
                quality = settings.value.defaultPdfQuality,
                isSearchable = true
            )
            clearDocSelection()
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Merged ${docIds.size} PDFs into \"${mergedDoc.title}\"!")
            onComplete(mergedDoc.id)
        }
    }

    fun splitDocumentRange(
        docId: String,
        startPage: Int,
        endPage: Int,
        newTitle: String,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch {
            val pages = documentRepository.getPagesForDocument(docId)
            if (pages.isEmpty()) return@launch
            val safeStart = (startPage - 1).coerceIn(0, pages.lastIndex)
            val safeEnd = (endPage - 1).coerceIn(safeStart, pages.lastIndex)
            val subPages = pages.subList(safeStart, safeEnd + 1).mapNotNull { p ->
                val bmp = ImageProcessor.loadBitmapFromPath(p.processedImagePath) ?: return@mapNotNull null
                DraftScannedPage(
                    originalBitmap = bmp,
                    croppedBitmap = bmp,
                    enhancedBitmap = bmp,
                    ocrText = p.ocrText,
                    ocrBlocks = OcrAndBarcodeEngine.decodeBlocksFromJson(p.ocrBlocksJson)
                )
            }
            val splitDoc = documentRepository.saveDraftDocument(
                title = newTitle.ifBlank { "Split Pages $startPage-$endPage" },
                category = "Scans",
                pages = subPages
            )
            showStatus("Created split PDF \"${splitDoc.title}\" (${subPages.size} pages)")
            onComplete(splitDoc.id)
        }
    }

    fun compressDocument(docId: String, targetQuality: PdfQuality) {
        viewModelScope.launch {
            val beforeDoc = documentRepository.getDocumentById(docId) ?: return@launch
            val beforeSize = beforeDoc.fileSizeBytes
            val updated = documentRepository.rebuildDocumentPdf(docId = docId, quality = targetQuality)
            if (updated != null) {
                val afterKb = updated.fileSizeBytes / 1024
                val beforeKb = beforeSize / 1024
                showStatus("Compressed PDF: ${beforeKb}KB → ${afterKb}KB (${targetQuality.label})")
            }
        }
    }

    fun convertDocumentFilter(docId: String, filter: ScanFilter) {
        viewModelScope.launch {
            documentRepository.applyFilterToSavedDocument(docId, filter)
            showStatus("Converted PDF pages to ${filter.label}")
        }
    }

    fun applyWatermarkOrPageNumbers(
        docId: String,
        watermarkText: String,
        pageNumberFormat: PageNumberFormat?,
        pageNumberPosition: PageNumberPosition
    ) {
        viewModelScope.launch {
            documentRepository.rebuildDocumentPdf(
                docId = docId,
                watermarkText = watermarkText,
                pageNumberFormat = pageNumberFormat,
                pageNumberPosition = pageNumberPosition
            )
            showStatus("Updated PDF watermark & page numbering")
        }
    }

    /**
     * Batch applies custom watermark text and/or page numbering across one or multiple selected documents.
     */
    fun applyBatchWatermarkAndPageNumbers(
        docIds: List<String>,
        watermarkText: String,
        pageNumberFormat: PageNumberFormat?,
        pageNumberPosition: PageNumberPosition,
        onComplete: () -> Unit = {}
    ) {
        if (docIds.isEmpty()) {
            showStatus("Select at least one document for batch watermarking")
            return
        }
        viewModelScope.launch {
            _scanSession.update {
                it.copy(
                    isProcessing = true,
                    processingMessage = "Applying batch watermark & page numbers to ${docIds.size} document(s)..."
                )
            }
            for (id in docIds) {
                documentRepository.rebuildDocumentPdf(
                    docId = id,
                    watermarkText = watermarkText,
                    pageNumberFormat = pageNumberFormat,
                    pageNumberPosition = pageNumberPosition
                )
            }
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Applied watermark & page numbers to ${docIds.size} document(s)")
            onComplete()
        }
    }

    /**
     * Composes Front + Back ID Card or Passport images onto a single A4 printable sheet with optional
     * Clear B&W Print enhancement and corner cut marks, saving it as a new PDF document.
     */
    fun createIdCardPassportA4Document(
        frontUri: Uri?,
        backUri: Uri?,
        title: String,
        applyBwPrint: Boolean = true,
        showCropMarks: Boolean = true,
        documentTypeLabel: String = "ID CARD / PASSPORT (2-SIDE A4)",
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Composing 2-side A4 ID / Passport sheet...")
            }
            val frontBmp = frontUri?.let { ImageProcessor.loadBitmapFromUri(appContext, it) }
                ?: ImageProcessor.generateSampleInvoiceBitmap()
            val backBmp = backUri?.let { ImageProcessor.loadBitmapFromUri(appContext, it) }
                ?: frontBmp

            val compositeA4 = ImageProcessor.createIdCardCompositePage(
                frontBitmap = frontBmp,
                backBitmap = backBmp,
                applyBwPrint = applyBwPrint,
                showCropMarks = showCropMarks,
                documentTypeLabel = documentTypeLabel
            )
            val ocr = OcrAndBarcodeEngine.recognizeText(compositeA4, settings.value.defaultOcrScript)
            val draftPage = DraftScannedPage(
                originalBitmap = compositeA4,
                croppedBitmap = compositeA4,
                enhancedBitmap = compositeA4,
                filter = if (applyBwPrint) ScanFilter.CLEAR else ScanFilter.ORIGINAL,
                ocrText = ocr.fullText,
                ocrBlocks = ocr.blocks
            )
            val savedDoc = documentRepository.saveDraftDocument(
                title = title.ifBlank { "ID_Passport_A4_${System.currentTimeMillis() / 1000}" },
                category = DocumentCategory.ID_CARD.label,
                pages = listOf(draftPage),
                pageSize = PdfPageSize.A4,
                quality = PdfQuality.HIGH,
                isSearchable = true
            )
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Created 2-Side A4 PDF \"${savedDoc.title}\"")
            onCreated(savedDoc.id)
        }
    }

    /**
     * Stamps a generated QR / Barcode bitmap directly onto a PDF document (or creates a new QR sheet PDF if docId is null).
     */
    fun stampQrBitmapOnDocument(
        docId: String?,
        qrBitmap: Bitmap,
        qrLabel: String,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (docId != null) {
                documentRepository.rebuildDocumentPdf(
                    docId = docId,
                    signatureBitmap = qrBitmap,
                    signaturePageIndex = 0,
                    signatureNormX = 0.72f,
                    signatureNormY = 0.78f,
                    signatureScale = 0.24f
                )
                showStatus("Embedded QR code onto document")
                onComplete(docId)
            } else {
                val draft = DraftScannedPage(
                    originalBitmap = qrBitmap,
                    croppedBitmap = qrBitmap,
                    enhancedBitmap = qrBitmap,
                    filter = ScanFilter.ORIGINAL,
                    ocrText = qrLabel
                )
                val saved = documentRepository.saveDraftDocument(
                    title = "QR_${qrLabel.take(18).replace(" ", "_").ifBlank { "Code" }}",
                    category = DocumentCategory.SCAN.label,
                    pages = listOf(draft),
                    pageSize = PdfPageSize.A4,
                    quality = PdfQuality.HIGH,
                    isSearchable = true
                )
                showStatus("Saved QR code as PDF \"${saved.title}\"")
                onComplete(saved.id)
            }
        }
    }

    /**
     * Extracts structured receipt & tabular data rows from a document's pages (running OCR automatically if needed)
     * and formats them as RFC-4180 CSV.
     */
    fun extractReceiptAndTableCsv(
        docId: String,
        onResult: (List<List<String>>, String) -> Unit
    ) {
        viewModelScope.launch {
            val doc = documentRepository.getDocumentById(docId)
            val pages = documentRepository.getPagesForDocument(docId).sortedBy { it.pageIndex }
            val allBlocks = mutableListOf<com.example.domain.model.OcrBlockItem>()
            val fullTextBuilder = StringBuilder()

            for (page in pages) {
                val decoded = OcrAndBarcodeEngine.decodeBlocksFromJson(page.ocrBlocksJson)
                if (page.ocrText.isNotBlank() || decoded.isNotEmpty()) {
                    fullTextBuilder.appendLine(page.ocrText)
                    allBlocks.addAll(decoded)
                } else {
                    val bmp = ImageProcessor.loadBitmapFromPath(page.processedImagePath)
                    if (bmp != null) {
                        val ocr = OcrAndBarcodeEngine.recognizeText(bmp, settings.value.defaultOcrScript)
                        fullTextBuilder.appendLine(ocr.fullText)
                        allBlocks.addAll(ocr.blocks)
                    }
                }
            }

            val combinedText = fullTextBuilder.toString().ifBlank { doc?.ocrText.orEmpty() }
            val rows = ImageProcessor.parseReceiptAndTableToCsvRows(combinedText, allBlocks)
            val csvString = ImageProcessor.formatCsvRowsToString(rows)
            onResult(rows, csvString)
        }
    }

    fun applySignatureToDocument(
        docId: String,
        signatureBitmap: Bitmap,
        pageIndex: Int = -1,
        normX: Float = 0.65f,
        normY: Float = 0.80f,
        scale: Float = 0.30f
    ) {
        viewModelScope.launch {
            documentRepository.rebuildDocumentPdf(
                docId = docId,
                signatureBitmap = signatureBitmap,
                signaturePageIndex = pageIndex,
                signatureNormX = normX,
                signatureNormY = normY,
                signatureScale = scale
            )
            val targetLabel = if (pageIndex == -1) "all pages" else "Page ${pageIndex + 1}"
            showStatus("Imposed signature on $targetLabel")
        }
    }

    fun imposeSignatureOnActiveDraftPage(
        signatureBitmap: Bitmap,
        normX: Float = 0.65f,
        normY: Float = 0.80f,
        scale: Float = 0.30f,
        applyToAllPages: Boolean = false
    ) {
        val state = _scanSession.value
        if (state.pages.isEmpty()) return
        val targetIdx = state.activePageIndex
        viewModelScope.launch {
            val updatedPages = state.pages.mapIndexed { idx, draft ->
                if (applyToAllPages || idx == targetIdx) {
                    val signedEnhanced = ImageProcessor.imposeSignatureOnBitmap(
                        pageBitmap = draft.enhancedBitmap,
                        signatureBitmap = signatureBitmap,
                        normX = normX,
                        normY = normY,
                        scale = scale
                    )
                    val signedCropped = ImageProcessor.imposeSignatureOnBitmap(
                        pageBitmap = draft.croppedBitmap,
                        signatureBitmap = signatureBitmap,
                        normX = normX,
                        normY = normY,
                        scale = scale
                    )
                    draft.copy(
                        croppedBitmap = signedCropped,
                        enhancedBitmap = signedEnhanced
                    )
                } else {
                    draft
                }
            }
            _scanSession.update { it.copy(pages = updatedPages) }
            showStatus(
                if (applyToAllPages) "Imposed signature on all ${updatedPages.size} pages"
                else "Imposed signature on Page ${targetIdx + 1}"
            )
        }
    }

    fun applyOfficialStampToDocument(
        docId: String,
        stampText: String,
        subText: String = "",
        inkColor: Int = android.graphics.Color.rgb(210, 32, 48),
        pageIndex: Int = -1,
        normX: Float = 0.60f,
        normY: Float = 0.14f,
        scale: Float = 0.34f
    ) {
        viewModelScope.launch {
            val stampBmp = ImageProcessor.createOfficialStampBitmap(
                stampText = stampText,
                subText = subText,
                inkColor = inkColor
            )
            documentRepository.rebuildDocumentPdf(
                docId = docId,
                signatureBitmap = stampBmp,
                signaturePageIndex = pageIndex,
                signatureNormX = normX,
                signatureNormY = normY,
                signatureScale = scale
            )
            val targetLabel = if (pageIndex == -1) "all pages" else "Page ${pageIndex + 1}"
            showStatus("Stamped \"${stampText.trim().uppercase()}\" on $targetLabel")
        }
    }

    fun createPdfFromTextNote(
        title: String,
        bodyText: String,
        includeRuledLines: Boolean = false,
        onCreated: (String) -> Unit
    ) {
        if (bodyText.isBlank() && title.isBlank()) {
            showStatus("Enter note text or title to generate PDF")
            return
        }
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Formatting Text Note to PDF...")
            }
            val pageBitmaps = ImageProcessor.renderTextNotePagesToBitmaps(
                title = title,
                bodyText = bodyText,
                includeRuledLines = includeRuledLines
            )
            val drafts = pageBitmaps.mapIndexed { idx, bmp ->
                DraftScannedPage(
                    originalBitmap = bmp,
                    croppedBitmap = bmp,
                    enhancedBitmap = bmp,
                    filter = ScanFilter.ORIGINAL,
                    ocrText = if (idx == 0) bodyText else ""
                )
            }
            val savedDoc = documentRepository.saveDraftDocument(
                title = title.ifBlank { "Note_${System.currentTimeMillis() / 1000}" },
                category = DocumentCategory.SCAN.label,
                pages = drafts,
                pageSize = PdfPageSize.A4,
                quality = PdfQuality.HIGH,
                isSearchable = true
            )
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Created PDF \"${savedDoc.title}\" (${savedDoc.pageCount} page(s))")
            onCreated(savedDoc.id)
        }
    }

    fun setDocumentPassword(doc: ScannedDocumentEntity, newPassword: String) {
        viewModelScope.launch {
            val hash = PdfEngine.hashPassword(newPassword)
            documentRepository.updateDocumentMetadata(
                doc.copy(
                    isPasswordProtected = hash.isNotEmpty(),
                    passwordHash = hash
                )
            )
            if (hash.isNotEmpty()) {
                _unlockedDocIds.update { it + doc.id }
                showStatus("Password protection enabled for \"${doc.title}\"")
            } else {
                showStatus("Password removed from \"${doc.title}\"")
            }
        }
    }

    fun verifyDocumentPassword(doc: ScannedDocumentEntity, inputPassword: String): Boolean {
        val hash = PdfEngine.hashPassword(inputPassword)
        val ok = hash == doc.passwordHash
        if (ok) {
            _unlockedDocIds.update { it + doc.id }
        }
        return ok
    }

    fun importExternalPdf(uri: Uri, title: String, onImported: (String) -> Unit) {
        viewModelScope.launch {
            _scanSession.update { it.copy(isProcessing = true, processingMessage = "Importing PDF pages...") }
            val imported = PdfEngine.importExternalPdfUri(appContext, uri)
            if (imported == null) {
                _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
                showStatus("Unable to read selected PDF file")
                return@launch
            }
            val (_, bitmaps) = imported
            val drafts = bitmaps.map { bmp ->
                val clearBmp = ImageProcessor.applyEnhancement(bmp, ScanFilter.CLEAR)
                val ocr = OcrAndBarcodeEngine.recognizeText(clearBmp, settings.value.defaultOcrScript)
                DraftScannedPage(
                    originalBitmap = bmp,
                    croppedBitmap = bmp,
                    enhancedBitmap = clearBmp,
                    filter = ScanFilter.CLEAR,
                    ocrText = ocr.fullText,
                    ocrBlocks = ocr.blocks
                )
            }
            val doc = documentRepository.saveDraftDocument(
                title = title.ifBlank { "Imported PDF" },
                category = DocumentCategory.IMPORTED.label,
                pages = drafts,
                pageSize = PdfPageSize.A4,
                quality = PdfQuality.HIGH,
                isSearchable = true
            )
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Imported \"${doc.title}\" with ${drafts.size} page(s) & OCR!")
            onImported(doc.id)
        }
    }

    fun exportDocumentAsImages(doc: ScannedDocumentEntity, asPng: Boolean) {
        viewModelScope.launch {
            val pdfFile = File(doc.pdfFilePath)
            val files = PdfEngine.exportPdfToImages(
                context = appContext,
                pdfFile = pdfFile,
                baseName = doc.title.replace(" ", "_"),
                asPng = asPng
            )
            if (files.isNotEmpty()) {
                val mime = if (asPng) "image/png" else "image/jpeg"
                ShareExportHelper.shareMultipleFiles(appContext, files, mime, "Share ${files.size} Picture(s) (Status / Apps)")
            } else {
                showStatus("Could not render PDF images")
            }
        }
    }

    /**
     * 1-tap shares the document's page as a high-resolution JPG picture (e.g. for WhatsApp Status, Instagram, Chat, or Gallery).
     */
    fun shareDocumentPageAsPicture(doc: ScannedDocumentEntity, pageIndex: Int = 0) {
        viewModelScope.launch {
            val pages = documentRepository.getPagesForDocument(doc.id).sortedBy { it.pageIndex }
            val targetPath = pages.getOrNull(pageIndex)?.processedImagePath ?: doc.thumbnailPath
            val pageFile = File(targetPath)
            if (pageFile.exists()) {
                ShareExportHelper.shareImageFile(
                    context = appContext,
                    imageFile = pageFile,
                    title = "Share \"${doc.title}\" as Picture"
                )
            } else {
                exportDocumentAsImages(doc, asPng = false)
            }
        }
    }

    /**
     * Imports 1 or more images directly into a saved editable PDF document with OCR blocks
     * and opens it immediately in the Direct PDF Editor.
     */
    fun importImagesForDirectPdfEditor(
        uris: List<Uri>,
        defaultTitle: String = "Editable_Doc_${System.currentTimeMillis() / 1000}",
        onCreated: (String) -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Preparing document for Direct PDF Editor...")
            }
            val drafts = mutableListOf<DraftScannedPage>()
            for (uri in uris) {
                val rawBmp = ImageProcessor.loadBitmapFromUri(appContext, uri) ?: continue
                val ocr = OcrAndBarcodeEngine.recognizeText(rawBmp, settings.value.defaultOcrScript)
                drafts.add(
                    DraftScannedPage(
                        originalBitmap = rawBmp,
                        croppedBitmap = rawBmp,
                        enhancedBitmap = rawBmp,
                        filter = ScanFilter.ORIGINAL,
                        ocrText = ocr.fullText,
                        ocrBlocks = ocr.blocks
                    )
                )
            }
            if (drafts.isEmpty()) {
                _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
                showStatus("Could not load selected picture(s)")
                return@launch
            }
            val savedDoc = documentRepository.saveDraftDocument(
                title = defaultTitle.ifBlank { "Editable Document" },
                category = DocumentCategory.IMPORTED.label,
                pages = drafts,
                pageSize = PdfPageSize.A4,
                quality = PdfQuality.HIGH,
                isSearchable = true
            )
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            showStatus("Opened \"${savedDoc.title}\" in Direct PDF Editor!")
            onCreated(savedDoc.id)
        }
    }

    /**
     * Saves direct on-page edits (replaced names, letter text, comments, highlights, whiteout)
     * directly into the main PDF file and optionally invokes onComplete with the updated document.
     */
    fun saveDirectEditedPageToDocument(
        docId: String,
        pageIndex: Int,
        editedBitmap: Bitmap,
        newTitle: String? = null,
        updatedPageOcrText: String? = null,
        updatedBlocks: List<com.example.domain.model.OcrBlockItem>? = null,
        onComplete: (ScannedDocumentEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            _scanSession.update {
                it.copy(isProcessing = true, processingMessage = "Saving changes directly to main PDF...")
            }
            val updatedDoc = documentRepository.saveDirectEditedPage(
                docId = docId,
                pageIndex = pageIndex,
                editedBitmap = editedBitmap,
                newTitle = newTitle,
                updatedPageOcrText = updatedPageOcrText,
                updatedBlocks = updatedBlocks
            )
            _scanSession.update { it.copy(isProcessing = false, processingMessage = "") }
            if (updatedDoc != null) {
                showStatus("Saved edits directly to \"${updatedDoc.title}\".pdf!")
                onComplete(updatedDoc)
            } else {
                showStatus("Could not save changes to PDF")
            }
        }
    }

    // --- Gemini AI Intelligence Actions ---

    fun setAiUploadedBitmap(bitmap: Bitmap?) {
        _aiState.update { it.copy(uploadedBitmap = bitmap, errorMessage = null) }
    }

    /**
     * 1. Low-Latency Fast Response (gemini-3.1-flash-lite): Instant Smart Metadata & OCR Cleanup.
     */
    fun runFastLowLatencyCleanup(text: String, taskPrompt: String) {
        viewModelScope.launch {
            _aiState.update {
                it.copy(
                    isAnalyzing = true,
                    activeModelBadge = "Fast Low-Latency • gemini-3.1-flash-lite",
                    errorMessage = null
                )
            }
            val res = geminiAssistant.fastCleanOrTranslateText(text, taskPrompt)
            res.onSuccess { out ->
                _aiState.update { it.copy(isAnalyzing = false, outputText = out) }
            }.onFailure { err ->
                _aiState.update { it.copy(isAnalyzing = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    fun autoGenerateTitleAndTagsForDoc(doc: ScannedDocumentEntity) {
        viewModelScope.launch {
            _aiState.update {
                it.copy(
                    isAnalyzing = true,
                    activeModelBadge = "Instant Tagger • gemini-3.1-flash-lite",
                    errorMessage = null
                )
            }
            val res = geminiAssistant.suggestQuickMetadata(doc.ocrText.ifBlank { doc.title })
            res.onSuccess { (newTitle, category, tags) ->
                documentRepository.updateDocumentMetadata(
                    doc.copy(
                        title = newTitle,
                        category = category,
                        tagsCsv = tags.joinToString(",")
                    )
                )
                _aiState.update {
                    it.copy(
                        isAnalyzing = false,
                        outputText = "Smart Title: $newTitle\nCategory: $category\nTags: ${tags.joinToString(", ")}"
                    )
                }
                showStatus("Updated title & tags with gemini-3.1-flash-lite!")
            }.onFailure { err ->
                _aiState.update { it.copy(isAnalyzing = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    /**
     * 2. General Document Assistant (gemini-3.8-flash): Executive Summary, Action Items, Markdown Rewrite.
     */
    fun runGeneralDocumentTask(doc: ScannedDocumentEntity?, text: String, instruction: String) {
        viewModelScope.launch {
            _aiState.update {
                it.copy(
                    isAnalyzing = true,
                    activeModelBadge = "General Intelligence • gemini-3.8-flash",
                    errorMessage = null
                )
            }
            val res = geminiAssistant.summarizeDocumentGeneral(text, instruction)
            res.onSuccess { summary ->
                _aiState.update { it.copy(isAnalyzing = false, outputText = summary) }
                if (doc != null) {
                    documentRepository.updateDocumentMetadata(doc.copy(aiSummary = summary.take(2000)))
                }
            }.onFailure { err ->
                _aiState.update { it.copy(isAnalyzing = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    /**
     * 3. Analyze Images (gemini-4-argon): Direct multimodal image understanding on uploaded or scanned photos.
     */
    fun runImageAnalysisWithArgon(bitmap: Bitmap, prompt: String) {
        viewModelScope.launch {
            _aiState.update {
                it.copy(
                    isAnalyzing = true,
                    uploadedBitmap = bitmap,
                    activeModelBadge = "Vision Understanding • gemini-4-argon",
                    errorMessage = null
                )
            }
            val res = geminiAssistant.analyzeDocumentImage(bitmap, prompt)
            res.onSuccess { analysis ->
                _aiState.update { it.copy(isAnalyzing = false, outputText = analysis) }
            }.onFailure { err ->
                _aiState.update { it.copy(isAnalyzing = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    /**
     * 4. High Thinking Mode (gemini-4-argon + ThinkingLevel.HIGH, no maxOutputTokens):
     * Handles complex queries, contract audits, and financial checks.
     */
    fun runHighThinkingQuery(
        documentText: String,
        complexQuery: String,
        optionalBitmap: Bitmap? = null,
        docToUpdate: ScannedDocumentEntity? = null
    ) {
        viewModelScope.launch {
            _aiState.update {
                it.copy(
                    isAnalyzing = true,
                    activeModelBadge = "High Thinking (ThinkingLevel.HIGH) • gemini-4-argon",
                    errorMessage = null
                )
            }
            val res = geminiAssistant.deepThinkDocumentAnalysis(
                documentText = documentText,
                complexQuery = complexQuery,
                optionalBitmap = optionalBitmap
            )
            res.onSuccess { reasoningOutput ->
                _aiState.update { it.copy(isAnalyzing = false, outputText = reasoningOutput) }
                if (docToUpdate != null) {
                    documentRepository.updateDocumentMetadata(docToUpdate.copy(aiSummary = reasoningOutput.take(2000)))
                }
            }.onFailure { err ->
                _aiState.update { it.copy(isAnalyzing = false, errorMessage = err.localizedMessage) }
            }
        }
    }

    // --- Settings Updates ---

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDefaultOcrScript(script: OcrScript) {
        viewModelScope.launch { settingsRepository.setDefaultOcrScript(script) }
    }

    fun setDefaultPdfQuality(quality: PdfQuality) {
        viewModelScope.launch { settingsRepository.setDefaultPdfQuality(quality) }
    }

    fun setDefaultPageSize(size: PdfPageSize) {
        viewModelScope.launch { settingsRepository.setDefaultPageSize(size) }
    }

    fun setDefaultSearchablePdf(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDefaultSearchablePdf(enabled) }
    }

    fun setBiometricLock(enabled: Boolean, pin: String) {
        viewModelScope.launch {
            settingsRepository.setBiometricLock(enabled, pin)
            if (enabled) _isAppUnlocked.value = true
            showStatus(if (enabled) "App Lock enabled" else "App Lock disabled")
        }
    }

    fun setAutoCloudSync(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoCloudSync(enabled) }
    }

    fun toggleGridView() {
        viewModelScope.launch {
            settingsRepository.setGridView(!settings.value.isGridView)
        }
    }

    fun setGuestMode(isGuest: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGuestMode(isGuest)
        }
    }

    companion object {
        fun provideFactory(
            appContainer: AppContainer,
            appContext: Context,
            currentUserId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DocuScanViewModel(
                    appContext = appContext.applicationContext,
                    documentRepository = appContainer.documentRepository,
                    settingsRepository = appContainer.settingsRepository,
                    geminiAssistant = appContainer.geminiAssistant,
                    currentUserId = currentUserId
                )
            }
        }
    }
}
