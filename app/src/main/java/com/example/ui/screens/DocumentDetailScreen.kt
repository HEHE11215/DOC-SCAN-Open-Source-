package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import android.speech.tts.TextToSpeech
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedPageEntity
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PdfQuality
import com.example.domain.model.ScanFilter
import com.example.ui.theme.DynamicRgbChip
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.LuminousCyan
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.utils.ImageProcessor
import com.example.utils.OcrAndBarcodeEngine
import com.example.utils.PdfEngine
import com.example.utils.ShareExportHelper
import kotlinx.coroutines.launch
import java.io.File

import androidx.compose.material.icons.filled.EditNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    viewModel: DocuScanViewModel,
    onBack: () -> Unit,
    onOpenDirectPdfEditor: (Int) -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val doc by viewModel.documentRepository.observeDocumentById(documentId)
        .collectAsStateWithLifecycle(initialValue = null)
    val pages by viewModel.documentRepository.observePagesForDocument(documentId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val unlockedIds by viewModel.unlockedDocIds.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showStampDialog by remember { mutableStateOf(false) }
    var showPasswordManageDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }

    var editableOcrText by remember(doc?.ocrText) { mutableStateOf(doc?.ocrText ?: "") }

    // Android Text-To-Speech (Read Aloud) Engine
    var isSpeaking by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = java.util.Locale.getDefault()
            }
        }
        ttsEngine = engine
        onDispose {
            runCatching {
                engine?.stop()
                engine?.shutdown()
            }
        }
    }

    fun toggleReadAloud() {
        val engine = ttsEngine ?: return
        if (isSpeaking) {
            engine.stop()
            isSpeaking = false
            viewModel.showStatus("Stopped voice reader")
        } else {
            val textToRead = editableOcrText.ifBlank {
                doc?.ocrText?.ifBlank { "" } ?: ""
            }
            if (textToRead.isBlank()) {
                viewModel.showStatus("No OCR text yet. Tap 'Scan OCR' first to read aloud.")
                selectedTab = 1
                return
            }
            engine.setSpeechRate(speechRate)
            engine.speak(textToRead.take(3900), TextToSpeech.QUEUE_FLUSH, null, "doc_tts_${doc?.id}")
            isSpeaking = true
            viewModel.showStatus("Reading document aloud (${speechRate}x)...")
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        val currentDoc = doc ?: return@rememberLauncherForActivityResult
        if (destUri != null) {
            val ok = ShareExportHelper.copyFileToUri(context, File(currentDoc.pdfFilePath), destUri)
            viewModel.showStatus(if (ok) "PDF exported to device storage" else "Failed to export PDF")
        }
    }

    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { destUri ->
        if (destUri != null) {
            val ok = ShareExportHelper.writeTextToUri(context, editableOcrText, destUri)
            viewModel.showStatus(if (ok) "TXT exported to device storage" else "Failed to export TXT")
        }
    }

    val currentDoc = doc
    if (currentDoc == null) {
        GlassScreenBackground {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val isLocked = currentDoc.isPasswordProtected && (currentDoc.id !in unlockedIds)

    GlassScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Frosted Glass Top Bar
            FrostedGlassCard(
                cornerRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentDoc.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentDoc.pageCount}p • ${ShareExportHelper.formatFileSize(currentDoc.fileSizeBytes)} • ${currentDoc.pageSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!isLocked) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DynamicRgbChip(
                                text = "Edit PDF",
                                onClick = { onOpenDirectPdfEditor(0) },
                                icon = Icons.Default.EditNote,
                                badgeText = "RGB",
                                testTag = "detail_direct_edit_pdf_button"
                            )
                            IconButton(
                                onClick = {
                                    viewModel.shareDocumentPageAsPicture(currentDoc, 0)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("share_picture_button")
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Share Picture (Status)",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    ShareExportHelper.sharePdfFile(
                                        context = context,
                                        pdfFile = File(currentDoc.pdfFilePath),
                                        title = currentDoc.title
                                    )
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("share_pdf_button")
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share PDF",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    savePdfLauncher.launch("${currentDoc.title.replace(" ", "_")}.pdf")
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("export_pdf_saf_button")
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Save PDF to Device",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FrostedGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Password Protected PDF",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enter the password configured for \"${currentDoc.title}\" to view, edit, or export its pages.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = {
                                    passwordInput = it
                                    passwordError = null
                                },
                                label = { Text("Document Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("unlock_pdf_password_input")
                            )
                            if (passwordError != null) {
                                Text(
                                    text = passwordError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = {
                                    val ok = viewModel.verifyDocumentPassword(currentDoc, passwordInput)
                                    if (!ok) {
                                        passwordError = "Incorrect password. Please try again."
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("unlock_pdf_submit_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock PDF")
                            }
                        }
                    }
                }
            } else {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("PDF & Pages (${pages.size})") },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        modifier = Modifier.testTag("detail_tab_pdf")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("OCR Text") },
                        icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                        modifier = Modifier.testTag("detail_tab_ocr")
                    )
                }

                when (selectedTab) {
                    0 -> PdfPagesViewerTab(
                        doc = currentDoc,
                        pages = pages,
                        viewModel = viewModel,
                        isSpeaking = isSpeaking,
                        onOpenDirectPdfEditor = onOpenDirectPdfEditor,
                        onToggleReadAloud = { toggleReadAloud() },
                        onOpenStampDialog = { showStampDialog = true },
                        onOpenWatermarkDialog = { showWatermarkDialog = true },
                        onOpenSignatureDialog = { showSignatureDialog = true },
                        onOpenPasswordDialog = { showPasswordManageDialog = true },
                        onOpenCompressDialog = { showCompressDialog = true }
                    )

                    1 -> OcrTextDetailTab(
                        doc = currentDoc,
                        editableText = editableOcrText,
                        isSpeaking = isSpeaking,
                        speechRate = speechRate,
                        onSpeechRateChange = { newRate ->
                            speechRate = newRate
                            ttsEngine?.setSpeechRate(newRate)
                        },
                        onToggleReadAloud = { toggleReadAloud() },
                        onTextChange = { editableOcrText = it },
                        onRunOcrAllPages = {
                            scope.launch {
                                val settings = viewModel.settings.value
                                val sb = StringBuilder()
                                pages.forEachIndexed { idx, pageEntity ->
                                    val bmp = ImageProcessor.loadBitmapFromPath(pageEntity.processedImagePath)
                                    if (bmp != null) {
                                        val res = OcrAndBarcodeEngine.recognizeText(bmp, settings.defaultOcrScript)
                                        viewModel.documentRepository.updatePageOcr(
                                            docId = currentDoc.id,
                                            page = pageEntity,
                                            ocrText = res.fullText,
                                            blocksJson = OcrAndBarcodeEngine.encodeBlocksToJson(res.blocks)
                                        )
                                        if (res.fullText.isNotBlank()) {
                                            if (sb.isNotEmpty()) sb.append("\n\n")
                                            sb.append("--- Page ${idx + 1} ---\n").append(res.fullText.trim())
                                        }
                                    }
                                }
                                if (sb.isNotEmpty()) {
                                    editableOcrText = sb.toString()
                                }
                                viewModel.showStatus("Completed OCR across ${pages.size} page(s)")
                            }
                        },
                        onSaveText = {
                            scope.launch {
                                viewModel.documentRepository.updateDocumentMetadata(
                                    currentDoc.copy(ocrText = editableOcrText)
                                )
                                viewModel.showStatus("Saved updated OCR text")
                            }
                        },
                        onCopyText = {
                            clipboard.setText(AnnotatedString(editableOcrText))
                            viewModel.showStatus("Copied OCR text to clipboard")
                        },
                        onShareTxt = {
                            val txtFile = ShareExportHelper.exportTextToCacheFile(
                                context = context,
                                text = editableOcrText,
                                fileNameWithoutExt = currentDoc.title.replace(" ", "_")
                            )
                            ShareExportHelper.shareTextFile(context, txtFile, currentDoc.title)
                        },
                        onSaveTxtSaf = {
                            saveTxtLauncher.launch("${currentDoc.title.replace(" ", "_")}.txt")
                        },
                        onCopyEntity = { kind, entityText ->
                            clipboard.setText(AnnotatedString(entityText))
                            viewModel.showStatus("Copied $kind: \"$entityText\"")
                        }
                    )
                }
            }
        }
    }

    if (showWatermarkDialog) {
        WatermarkAndNumberingDialog(
            initialWatermark = currentDoc.watermarkText,
            onDismiss = { showWatermarkDialog = false },
            onConfirm = { watermark, format, pos ->
                showWatermarkDialog = false
                viewModel.applyWatermarkOrPageNumbers(
                    docId = currentDoc.id,
                    watermarkText = watermark,
                    pageNumberFormat = format,
                    pageNumberPosition = pos
                )
            }
        )
    }

    if (showStampDialog) {
        OfficialStampDialog(
            pageCount = pages.size.coerceAtLeast(1),
            onDismiss = { showStampDialog = false },
            onStampImposed = { stampText, subText, inkColor, targetPageIdx, normX, normY, scale ->
                showStampDialog = false
                viewModel.applyOfficialStampToDocument(
                    docId = currentDoc.id,
                    stampText = stampText,
                    subText = subText,
                    inkColor = inkColor,
                    pageIndex = targetPageIdx,
                    normX = normX,
                    normY = normY,
                    scale = scale
                )
            }
        )
    }

    if (showSignatureDialog) {
        var firstPageBmp by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(pages) {
            firstPageBmp = pages.firstOrNull()?.let { ImageProcessor.loadBitmapFromPath(it.processedImagePath) }
        }
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            pagePreviewBitmap = firstPageBmp,
            pageCount = pages.size.coerceAtLeast(1),
            onSignatureImposed = { signatureBmp, targetPageIdx, normX, normY, scale ->
                showSignatureDialog = false
                viewModel.applySignatureToDocument(
                    docId = currentDoc.id,
                    signatureBitmap = signatureBmp,
                    pageIndex = targetPageIdx,
                    normX = normX,
                    normY = normY,
                    scale = scale
                )
            }
        )
    }

    if (showPasswordManageDialog) {
        PasswordManageDialog(
            isCurrentlyProtected = currentDoc.isPasswordProtected,
            onDismiss = { showPasswordManageDialog = false },
            onSavePassword = { newPass ->
                showPasswordManageDialog = false
                viewModel.setDocumentPassword(currentDoc, newPass)
            }
        )
    }

    if (showCompressDialog) {
        CompressPdfDialog(
            currentSizeBytes = currentDoc.fileSizeBytes,
            onDismiss = { showCompressDialog = false },
            onCompress = { quality ->
                showCompressDialog = false
                viewModel.compressDocument(currentDoc.id, quality)
            }
        )
    }
}

@Composable
private fun PdfPagesViewerTab(
    doc: com.example.data.local.ScannedDocumentEntity,
    pages: List<ScannedPageEntity>,
    viewModel: DocuScanViewModel,
    isSpeaking: Boolean,
    onOpenDirectPdfEditor: (Int) -> Unit,
    onToggleReadAloud: () -> Unit,
    onOpenStampDialog: () -> Unit,
    onOpenWatermarkDialog: () -> Unit,
    onOpenSignatureDialog: () -> Unit,
    onOpenPasswordDialog: () -> Unit,
    onOpenCompressDialog: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var renderedBitmaps by remember(doc.pdfFilePath, doc.updatedAt, pages.size) {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    LaunchedEffect(doc.pdfFilePath, doc.updatedAt, pages) {
        val pdfFile = File(doc.pdfFilePath)
        val list = PdfEngine.renderPdfPages(pdfFile, scaleFactor = 1.9f).toMutableList()
        if (list.isEmpty()) {
            for (p in pages) {
                val bmp = ImageProcessor.loadBitmapFromPath(p.processedImagePath)
                if (bmp != null) list.add(bmp)
            }
        }
        renderedBitmaps = list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Action Strip with Direct PDF Editor (FREE), Share Picture (Status), Clear Scan, Convert to B&W, Sign, Stamp, etc.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onOpenDirectPdfEditor(0) },
                label = { Text("Direct PDF Editor (FREE)", fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("doc_direct_pdf_editor_chip")
            )
            AssistChip(
                onClick = { viewModel.shareDocumentPageAsPicture(doc, 0) },
                label = { Text("Share Picture (Status)") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_share_picture_chip")
            )
            AssistChip(
                onClick = { viewModel.convertDocumentFilter(doc.id, ScanFilter.CLEAR) },
                label = { Text("Clear Scan") },
                leadingIcon = { Icon(Icons.Default.Contrast, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_convert_clear_button")
            )
            AssistChip(
                onClick = { viewModel.convertDocumentFilter(doc.id, ScanFilter.BW) },
                label = { Text("Convert to B&W") },
                leadingIcon = { Icon(Icons.Default.Contrast, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_convert_bw_button")
            )
            AssistChip(
                onClick = { viewModel.convertDocumentFilter(doc.id, ScanFilter.MAGIC_COLOR) },
                label = { Text("Magic Color") },
                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            AssistChip(
                onClick = onOpenSignatureDialog,
                label = { Text("Sign PDF") },
                leadingIcon = { Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_sign_button")
            )
            AssistChip(
                onClick = onOpenStampDialog,
                label = { Text("Stamp PDF") },
                leadingIcon = { Icon(Icons.Default.Approval, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_stamp_button")
            )
            AssistChip(
                onClick = onToggleReadAloud,
                label = { Text(if (isSpeaking) "Stop Audio" else "Read Aloud") },
                leadingIcon = {
                    Icon(
                        if (isSpeaking) Icons.Default.StopCircle else Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("doc_read_aloud_button")
            )
            AssistChip(
                onClick = onOpenWatermarkDialog,
                label = { Text("Watermark & #") },
                leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_watermark_button")
            )
            AssistChip(
                onClick = onOpenCompressDialog,
                label = { Text("Compress") },
                leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_compress_button")
            )
            AssistChip(
                onClick = onOpenPasswordDialog,
                label = { Text(if (doc.isPasswordProtected) "Password (On)" else "Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_password_button")
            )
            AssistChip(
                onClick = { viewModel.exportDocumentAsImages(doc, asPng = false) },
                label = { Text("Export JPGs") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("doc_export_jpg_button")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 3f)
                        if (zoomScale > 1f) {
                            panOffset += pan
                        } else {
                            panOffset = Offset.Zero
                        }
                    }
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(renderedBitmaps) { index, bitmap ->
                val pageEntity = pages.getOrNull(index)
                FrostedGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        },
                    cornerRadius = 18.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Page ${index + 1} of ${renderedBitmaps.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            if (pageEntity != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Surface(
                                        onClick = { onOpenDirectPdfEditor(index) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.padding(end = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.EditNote,
                                                contentDescription = "Direct Edit Page",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Edit Page",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            ShareExportHelper.shareBitmapAsJpg(
                                                context = context,
                                                bitmap = bitmap,
                                                fileNameWithoutExt = "${doc.title}_Page_${index + 1}"
                                            )
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = "Share Page as Picture",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                scope.launch {
                                                    val reordered = pages.toMutableList()
                                                    val item = reordered.removeAt(index)
                                                    reordered.add(index - 1, item)
                                                    viewModel.documentRepository.reorderOrModifyPages(doc.id, reordered)
                                                    viewModel.showStatus("Moved Page ${index + 1} up")
                                                }
                                            }
                                        },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Page Up")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < pages.lastIndex) {
                                                scope.launch {
                                                    val reordered = pages.toMutableList()
                                                    val item = reordered.removeAt(index)
                                                    reordered.add(index + 1, item)
                                                    viewModel.documentRepository.reorderOrModifyPages(doc.id, reordered)
                                                    viewModel.showStatus("Moved Page ${index + 1} down")
                                                }
                                            }
                                        },
                                        enabled = index < pages.lastIndex
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Page Down")
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                viewModel.documentRepository.rotateSinglePage(doc.id, pageEntity.id)
                                                viewModel.showStatus("Rotated Page ${index + 1}")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Rotate Page 90°")
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                val pageBmp = ImageProcessor.loadBitmapFromPath(pageEntity.processedImagePath)
                                                    ?: bitmap
                                                val res = OcrAndBarcodeEngine.recognizeText(pageBmp, settings.defaultOcrScript)
                                                viewModel.documentRepository.updatePageOcr(
                                                    docId = doc.id,
                                                    page = pageEntity,
                                                    ocrText = res.fullText,
                                                    blocksJson = OcrAndBarcodeEngine.encodeBlocksToJson(res.blocks)
                                                )
                                                viewModel.showStatus("OCR updated for Page ${index + 1}")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.TextFields, contentDescription = "Run OCR on Page")
                                    }
                                    if (pages.size > 1) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val remaining = pages.filterIndexed { idx, _ -> idx != index }
                                                    viewModel.documentRepository.reorderOrModifyPages(doc.id, remaining)
                                                    viewModel.showStatus("Removed Page ${index + 1}")
                                                }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete Page",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Rendered PDF Page ${index + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrTextDetailTab(
    doc: com.example.data.local.ScannedDocumentEntity,
    editableText: String,
    isSpeaking: Boolean,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    onToggleReadAloud: () -> Unit,
    onTextChange: (String) -> Unit,
    onRunOcrAllPages: () -> Unit,
    onSaveText: () -> Unit,
    onCopyText: () -> Unit,
    onCopyEntity: (String, String) -> Unit,
    onShareTxt: () -> Unit,
    onSaveTxtSaf: () -> Unit
) {
    // Smart Document Insights & Entity Extraction
    val wordCount = remember(editableText) {
        if (editableText.isBlank()) 0 else editableText.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(editableText) { editableText.length }
    val readingTimeMin = remember(wordCount) { (wordCount / 180f).coerceAtLeast(if (wordCount > 0) 0.5f else 0f) }

    val extractedEntities = remember(editableText) {
        if (editableText.isBlank()) emptyList()
        else {
            val results = mutableListOf<Pair<String, String>>()
            Regex("(?:\\$|€|£|₹|USD|EUR)\\s?\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2})?").findAll(editableText).take(3).forEach {
                results.add("Amount" to it.value.trim())
            }
            Regex("\\b(?:\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})\\b").findAll(editableText).take(2).forEach {
                results.add("Date" to it.value.trim())
            }
            Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").findAll(editableText).take(2).forEach {
                results.add("Email" to it.value.trim())
            }
            results.distinctBy { it.second }.take(6)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FrostedGlassCard(
            cornerRadius = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassActionButton(
                        text = "Scan OCR",
                        icon = Icons.Default.TextFields,
                        isPrimary = true,
                        height = 40.dp,
                        onClick = onRunOcrAllPages,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_detail_run_button")
                    )
                    GlassActionButton(
                        text = if (isSpeaking) "Stop Voice" else "Read Aloud",
                        icon = if (isSpeaking) Icons.Default.StopCircle else Icons.Default.RecordVoiceOver,
                        isPrimary = isSpeaking,
                        height = 40.dp,
                        onClick = onToggleReadAloud,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_read_aloud_button")
                    )
                    GlassActionButton(
                        text = "Copy",
                        icon = Icons.Default.ContentCopy,
                        height = 40.dp,
                        onClick = onCopyText,
                        modifier = Modifier
                            .weight(0.85f)
                            .testTag("ocr_copy_button")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassActionButton(
                        text = "Share TXT",
                        icon = Icons.Default.Share,
                        height = 38.dp,
                        onClick = onShareTxt,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_share_txt_button")
                    )
                    GlassActionButton(
                        text = "Export",
                        icon = Icons.Default.Download,
                        height = 38.dp,
                        onClick = onSaveTxtSaf,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_export_txt_button")
                    )
                    // Voice Speed Selector Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(0.75f to "0.75x", 1.0f to "1x", 1.25f to "1.25x", 1.5f to "1.5x").forEach { (rate, label) ->
                            val selected = kotlin.math.abs(speechRate - rate) < 0.05f
                            Surface(
                                onClick = { onSpeechRateChange(rate) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Smart Document Insights & Extracted Entities Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = "$wordCount words • $charCount chars • ${if (readingTimeMin < 1f && wordCount > 0) "<1 min" else "${readingTimeMin.toInt()} min"} read",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    extractedEntities.forEach { (kind, value) ->
                        AssistChip(
                            onClick = { onCopyEntity(kind, value) },
                            label = {
                                Text(
                                    text = "$kind: $value",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = kind,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = editableText,
            onValueChange = onTextChange,
            label = {
                Text(
                    "OCR Text (${doc.title})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = {
                Text(
                    "No OCR text extracted yet. Tap 'Scan OCR' above to extract text from all pages.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("ocr_detail_text_input")
        )

        GlassActionButton(
            text = "Save Edited OCR Text",
            icon = Icons.Default.Save,
            isPrimary = true,
            height = 46.dp,
            onClick = onSaveText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .testTag("ocr_save_changes_button")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatermarkAndNumberingDialog(
    initialWatermark: String,
    onDismiss: () -> Unit,
    onConfirm: (String, PageNumberFormat?, PageNumberPosition) -> Unit
) {
    var watermark by remember { mutableStateOf(initialWatermark) }
    var enablePageNumbers by remember { mutableStateOf(true) }
    var selectedFormat by remember { mutableStateOf(PageNumberFormat.PAGE_X_OF_Y) }
    var selectedPosition by remember { mutableStateOf(PageNumberPosition.BOTTOM_RIGHT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Watermark & Page Numbers") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = watermark,
                    onValueChange = { watermark = it },
                    label = { Text("Diagonal Watermark Text (leave blank for none)") },
                    placeholder = { Text("CONFIDENTIAL / DRAFT") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Page Number Format", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !enablePageNumbers,
                        onClick = { enablePageNumbers = false },
                        label = { Text("None") }
                    )
                    PageNumberFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = enablePageNumbers && selectedFormat == fmt,
                            onClick = {
                                enablePageNumbers = true
                                selectedFormat = fmt
                            },
                            label = { Text(fmt.label) }
                        )
                    }
                }
                if (enablePageNumbers) {
                    Text("Position", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PageNumberPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = selectedPosition == pos,
                                onClick = { selectedPosition = pos },
                                label = { Text(pos.label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        watermark.trim(),
                        if (enablePageNumbers) selectedFormat else null,
                        selectedPosition
                    )
                }
            ) {
                Text("Apply to PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    pagePreviewBitmap: Bitmap? = null,
    pageCount: Int = 1,
    initialPageIndex: Int = -1,
    onSignatureImposed: ((signatureBitmap: Bitmap, targetPageIndex: Int, normX: Float, normY: Float, scale: Float) -> Unit)? = null,
    onSignatureCaptured: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Normalized strokes (x and y in 0f..1f relative to the signature pad box)
    val normStrokes = remember { mutableStateListOf<List<Offset>>() }
    var currentNormStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var importedSignatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedInkColor by remember { mutableIntStateOf(AndroidColor.rgb(15, 36, 96)) }

    // Interactive Imposition Coordinates on the Document Page (0f..1f)
    var sigNormX by remember { mutableFloatStateOf(0.62f) }
    var sigNormY by remember { mutableFloatStateOf(0.78f) }
    var sigScale by remember { mutableFloatStateOf(0.32f) }
    var targetPageIndex by remember { mutableIntStateOf(initialPageIndex) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageProcessor.loadBitmapFromUri(context, uri, maxDim = 900)
                if (bmp != null) {
                    importedSignatureBitmap = ImageProcessor.makeSignatureTransparentIfWhite(bmp)
                    normStrokes.clear()
                    currentNormStroke = emptyList()
                }
            }
        }
    }

    val hasSignature = normStrokes.isNotEmpty() || importedSignatureBitmap != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sign & Impose on Page",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Draw your signature or import an image:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    importedSignatureBitmap = null
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    currentNormStroke = listOf(
                                        Offset(
                                            (offset.x / w).coerceIn(0f, 1f),
                                            (offset.y / h).coerceIn(0f, 1f)
                                        )
                                    )
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    currentNormStroke = currentNormStroke + Offset(
                                        (change.position.x / w).coerceIn(0f, 1f),
                                        (change.position.y / h).coerceIn(0f, 1f)
                                    )
                                },
                                onDragEnd = {
                                    if (currentNormStroke.size > 1) {
                                        normStrokes.add(currentNormStroke)
                                    }
                                    currentNormStroke = emptyList()
                                }
                            )
                        }
                ) {
                    val importedBmp = importedSignatureBitmap
                    if (importedBmp != null) {
                        Image(
                            bitmap = importedBmp.asImageBitmap(),
                            contentDescription = "Imported Signature",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                        )
                    } else {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Subtle signature baseline guide
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(w * 0.08f, h * 0.78f),
                                end = Offset(w * 0.92f, h * 0.78f),
                                strokeWidth = 2f
                            )
                            val allStrokes = normStrokes + listOf(currentNormStroke)
                            for (pts in allStrokes) {
                                if (pts.size > 1) {
                                    val path = Path().apply {
                                        moveTo(pts.first().x * w, pts.first().y * h)
                                        for (i in 1 until pts.size) {
                                            lineTo(pts[i].x * w, pts[i].y * h)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(selectedInkColor),
                                        style = Stroke(width = 6.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            AndroidColor.rgb(15, 36, 96) to "Navy",
                            AndroidColor.BLACK to "Black",
                            AndroidColor.rgb(10, 102, 204) to "Blue",
                            AndroidColor.rgb(180, 24, 24) to "Red"
                        ).forEach { (col, _) ->
                            Surface(
                                shape = CircleShape,
                                color = Color(col),
                                modifier = Modifier
                                    .size(26.dp)
                                    .border(
                                        width = if (selectedInkColor == col) 2.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                onClick = { selectedInkColor = col }
                            ) {}
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                normStrokes.clear()
                                currentNormStroke = emptyList()
                                importedSignatureBitmap = null
                            }
                        ) {
                            Text("Clear")
                        }
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Import")
                        }
                    }
                }

                // Step 2: Interactive Page Imposition Preview
                Text(
                    text = "2. Where to Impose Signature (Drag on Page):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Quick Position Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Triple("Bottom Right", 0.62f, 0.78f),
                        Triple("Bottom Left", 0.08f, 0.78f),
                        Triple("Bottom Center", 0.35f, 0.78f),
                        Triple("Center", 0.35f, 0.45f),
                        Triple("Top Right", 0.62f, 0.12f)
                    ).forEach { (label, px, py) ->
                        val isPresetSelected = kotlin.math.abs(sigNormX - px) < 0.06f && kotlin.math.abs(sigNormY - py) < 0.06f
                        FilterChip(
                            selected = isPresetSelected,
                            onClick = {
                                sigNormX = px
                                sigNormY = py
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Interactive Page Preview Canvas where user drags where the signature is imposed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .pointerInput(sigScale) {
                            detectTapGestures { tap ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val boxW = w * sigScale
                                val boxH = boxW * 0.4f
                                val maxL = (w - boxW).coerceAtLeast(1f)
                                val maxT = (h - boxH).coerceAtLeast(1f)
                                sigNormX = ((tap.x - boxW / 2f) / maxL).coerceIn(0f, 1f)
                                sigNormY = ((tap.y - boxH / 2f) / maxT).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(sigScale) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val boxW = w * sigScale
                                val boxH = boxW * 0.4f
                                val maxL = (w - boxW).coerceAtLeast(1f)
                                val maxT = (h - boxH).coerceAtLeast(1f)
                                sigNormX = ((change.position.x - boxW / 2f) / maxL).coerceIn(0f, 1f)
                                sigNormY = ((change.position.y - boxH / 2f) / maxT).coerceIn(0f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (pagePreviewBitmap != null) {
                        Image(
                            bitmap = pagePreviewBitmap.asImageBitmap(),
                            contentDescription = "Document Page Imposition Preview",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                    } else {
                        // Clean simulated document sheet lines when no bitmap is provided
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            val pw = size.width
                            val ph = size.height
                            for (lineIdx in 1..6) {
                                val ly = ph * (0.12f * lineIdx)
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(pw * 0.1f, ly),
                                    end = Offset(pw * (if (lineIdx == 6) 0.55f else 0.9f), ly),
                                    strokeWidth = 4f
                                )
                            }
                        }
                    }

                    // Live Signature Imposition Overlay Box
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val boxW = w * sigScale
                        val boxH = boxW * 0.40f
                        val maxL = (w - boxW).coerceAtLeast(1f)
                        val maxT = (h - boxH).coerceAtLeast(1f)
                        val left = sigNormX.coerceIn(0f, 1f) * maxL
                        val top = sigNormY.coerceIn(0f, 1f) * maxT

                        drawRoundRect(
                            color = LuminousCyan.copy(alpha = 0.22f),
                            topLeft = Offset(left, top),
                            size = Size(boxW, boxH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            color = ElectricTeal,
                            topLeft = Offset(left, top),
                            size = Size(boxW, boxH),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )

                        // Render live drawn strokes inside the target imposition box!
                        val allStrokes = normStrokes + listOf(currentNormStroke)
                        for (pts in allStrokes) {
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(left + pts.first().x * boxW, top + pts.first().y * boxH)
                                    for (i in 1 until pts.size) {
                                        lineTo(left + pts[i].x * boxW, top + pts[i].y * boxH)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(selectedInkColor),
                                    style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }

                // Signature Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Size: ${(sigScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(68.dp)
                    )
                    Slider(
                        value = sigScale,
                        onValueChange = { sigScale = it },
                        valueRange = 0.18f..0.52f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Target Page Selector if document has multiple pages
                if (pageCount > 1) {
                    Text(
                        text = "Apply to Page:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = targetPageIndex == -1,
                            onClick = { targetPageIndex = -1 },
                            label = { Text("All Pages") }
                        )
                        for (idx in 0 until pageCount) {
                            FilterChip(
                                selected = targetPageIndex == idx,
                                onClick = { targetPageIndex = idx },
                                label = { Text("Page ${idx + 1}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalBmp = importedSignatureBitmap ?: run {
                        val outW = 800
                        val outH = 320
                        val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                        val canvas = AndroidCanvas(bmp)
                        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                            color = selectedInkColor
                            style = AndroidPaint.Style.STROKE
                            strokeWidth = 9f
                            strokeCap = AndroidPaint.Cap.ROUND
                            strokeJoin = AndroidPaint.Join.ROUND
                        }
                        for (pts in normStrokes) {
                            for (i in 0 until pts.size - 1) {
                                canvas.drawLine(
                                    pts[i].x * outW,
                                    pts[i].y * outH,
                                    pts[i + 1].x * outW,
                                    pts[i + 1].y * outH,
                                    paint
                                )
                            }
                        }
                        bmp
                    }
                    if (onSignatureImposed != null) {
                        onSignatureImposed(finalBmp, targetPageIndex, sigNormX, sigNormY, sigScale)
                    } else {
                        onSignatureCaptured(finalBmp)
                    }
                },
                enabled = hasSignature,
                modifier = Modifier.testTag("confirm_impose_signature_button")
            ) {
                Text("Impose Signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PasswordManageDialog(
    isCurrentlyProtected: Boolean,
    onDismiss: () -> Unit,
    onSavePassword: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCurrentlyProtected) "Manage or Remove Password" else "Add PDF Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isCurrentlyProtected) {
                        "Enter a new password to update protection, or leave empty and tap Remove Password."
                    } else {
                        "Set a password required to open, view, or export this PDF inside DocuScan."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSavePassword(password.trim()) }) {
                Text(if (password.isBlank() && isCurrentlyProtected) "Remove Password" else "Save Password")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CompressPdfDialog(
    currentSizeBytes: Long,
    onDismiss: () -> Unit,
    onCompress: (PdfQuality) -> Unit
) {
    var selectedQuality by remember { mutableStateOf(PdfQuality.LOW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compress PDF Size") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Current size: ${ShareExportHelper.formatFileSize(currentSizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                PdfQuality.entries.forEach { quality ->
                    FilterChip(
                        selected = selectedQuality == quality,
                        onClick = { selectedQuality = quality },
                        label = { Text("${quality.label} (JPEG ${quality.jpegQuality}%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCompress(selectedQuality) }) {
                Text("Rebuild & Compress")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfficialStampDialog(
    onDismiss: () -> Unit,
    pagePreviewBitmap: Bitmap? = null,
    pageCount: Int = 1,
    initialPageIndex: Int = -1,
    onStampImposed: (stampText: String, subText: String, colorArgb: Int, targetPageIndex: Int, normX: Float, normY: Float, scale: Float) -> Unit
) {
    val todayDate = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    val presets = remember {
        listOf(
            Triple("APPROVED", AndroidColor.rgb(16, 142, 72), "DATE: $todayDate"),
            Triple("PAID", AndroidColor.rgb(14, 116, 204), "PAID ON $todayDate"),
            Triple("CONFIDENTIAL", AndroidColor.rgb(200, 30, 45), "DO NOT DISTRIBUTE"),
            Triple("URGENT", AndroidColor.rgb(217, 70, 15), "PRIORITY ACTION"),
            Triple("FINAL", AndroidColor.rgb(99, 60, 212), "VERIFIED $todayDate"),
            Triple("VOID", AndroidColor.rgb(190, 24, 24), "CANCELLED $todayDate")
        )
    }

    var stampText by remember { mutableStateOf("APPROVED") }
    var subText by remember { mutableStateOf("DATE: $todayDate") }
    var selectedColor by remember { mutableIntStateOf(AndroidColor.rgb(16, 142, 72)) }
    var stampNormX by remember { mutableFloatStateOf(0.58f) }
    var stampNormY by remember { mutableFloatStateOf(0.14f) }
    var stampScale by remember { mutableFloatStateOf(0.36f) }
    var targetPageIndex by remember { mutableIntStateOf(initialPageIndex) }

    val liveStampPreviewBitmap = remember(stampText, subText, selectedColor) {
        ImageProcessor.createOfficialStampBitmap(
            stampText = stampText.ifBlank { "APPROVED" },
            subText = subText,
            inkColor = selectedColor
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Official Rubber Stamp Maker",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Choose Preset or Customize Stamp:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, col, defaultSub) ->
                        FilterChip(
                            selected = stampText.equals(label, ignoreCase = true),
                            onClick = {
                                stampText = label
                                selectedColor = col
                                subText = defaultSub
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = stampText,
                    onValueChange = { stampText = it.take(18) },
                    label = { Text("Main Stamp Banner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subText,
                    onValueChange = { subText = it.take(28) },
                    label = { Text("Sub-banner (Date / Department)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Ink Color Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ink Color:", style = MaterialTheme.typography.labelMedium)
                    listOf(
                        AndroidColor.rgb(16, 142, 72),
                        AndroidColor.rgb(200, 30, 45),
                        AndroidColor.rgb(14, 116, 204),
                        AndroidColor.rgb(217, 70, 15),
                        AndroidColor.rgb(99, 60, 212)
                    ).forEach { col ->
                        Surface(
                            shape = CircleShape,
                            color = Color(col),
                            modifier = Modifier
                                .size(28.dp)
                                .border(
                                    width = if (selectedColor == col) 2.5.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                ),
                            onClick = { selectedColor = col }
                        ) {}
                    }
                }

                Text(
                    text = "2. Drag Stamp on Page Preview:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE2E8F0))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .pointerInput(stampScale) {
                            detectTapGestures { tap ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val boxW = w * stampScale
                                val boxH = boxW * 0.42f
                                val maxL = (w - boxW).coerceAtLeast(1f)
                                val maxT = (h - boxH).coerceAtLeast(1f)
                                stampNormX = ((tap.x - boxW / 2f) / maxL).coerceIn(0f, 1f)
                                stampNormY = ((tap.y - boxH / 2f) / maxT).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(stampScale) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val boxW = w * stampScale
                                val boxH = boxW * 0.42f
                                val maxL = (w - boxW).coerceAtLeast(1f)
                                val maxT = (h - boxH).coerceAtLeast(1f)
                                stampNormX = ((change.position.x - boxW / 2f) / maxL).coerceIn(0f, 1f)
                                stampNormY = ((change.position.y - boxH / 2f) / maxT).coerceIn(0f, 1f)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (pagePreviewBitmap != null) {
                        Image(
                            bitmap = pagePreviewBitmap.asImageBitmap(),
                            contentDescription = "Stamp Imposition Preview",
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        )
                    } else {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            val pw = size.width
                            val ph = size.height
                            for (lineIdx in 1..6) {
                                val ly = ph * (0.12f * lineIdx)
                                drawLine(
                                    color = Color(0xFFE2E8F0),
                                    start = Offset(pw * 0.1f, ly),
                                    end = Offset(pw * 0.9f, ly),
                                    strokeWidth = 4f
                                )
                            }
                        }
                    }

                    // Live Stamp Preview Box
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    ) {
                        val boxW = maxWidth * stampScale
                        val boxH = boxW * 0.42f
                        val maxL = (maxWidth - boxW).coerceAtLeast(1.dp)
                        val maxT = (maxHeight - boxH).coerceAtLeast(1.dp)
                        Image(
                            bitmap = liveStampPreviewBitmap.asImageBitmap(),
                            contentDescription = "Stamp Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = boxW, height = boxH)
                                .offset(
                                    x = maxL * stampNormX.coerceIn(0f, 1f),
                                    y = maxT * stampNormY.coerceIn(0f, 1f)
                                )
                        )
                    }
                }

                // Size slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Size: ${(stampScale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(68.dp)
                    )
                    Slider(
                        value = stampScale,
                        onValueChange = { stampScale = it },
                        valueRange = 0.22f..0.58f,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (pageCount > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = targetPageIndex == -1,
                            onClick = { targetPageIndex = -1 },
                            label = { Text("All Pages") }
                        )
                        for (idx in 0 until pageCount) {
                            FilterChip(
                                selected = targetPageIndex == idx,
                                onClick = { targetPageIndex = idx },
                                label = { Text("Page ${idx + 1}") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStampImposed(
                        stampText.ifBlank { "APPROVED" },
                        subText,
                        selectedColor,
                        targetPageIndex,
                        stampNormX,
                        stampNormY,
                        stampScale
                    )
                },
                modifier = Modifier.testTag("confirm_impose_stamp_button")
            ) {
                Text("Stamp PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

