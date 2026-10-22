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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedPageEntity
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PdfQuality
import com.example.domain.model.ScanFilter
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.utils.ImageProcessor
import com.example.utils.OcrAndBarcodeEngine
import com.example.utils.PdfEngine
import com.example.utils.ShareExportHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentId: String,
    viewModel: DocuScanViewModel,
    onBack: () -> Unit
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
    var showPasswordManageDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }

    var editableOcrText by remember(doc?.ocrText) { mutableStateOf(doc?.ocrText ?: "") }

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
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentDoc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${currentDoc.pageCount} page(s) • ${ShareExportHelper.formatFileSize(currentDoc.fileSizeBytes)} • ${currentDoc.pageSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!isLocked) {
                        Row {
                            IconButton(
                                onClick = {
                                    ShareExportHelper.sharePdfFile(
                                        context = context,
                                        pdfFile = File(currentDoc.pdfFilePath),
                                        title = currentDoc.title
                                    )
                                },
                                modifier = Modifier.testTag("share_pdf_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share PDF")
                            }
                            IconButton(
                                onClick = {
                                    savePdfLauncher.launch("${currentDoc.title.replace(" ", "_")}.pdf")
                                },
                                modifier = Modifier.testTag("export_pdf_saf_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Save PDF to Device")
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
                        onOpenWatermarkDialog = { showWatermarkDialog = true },
                        onOpenSignatureDialog = { showSignatureDialog = true },
                        onOpenPasswordDialog = { showPasswordManageDialog = true },
                        onOpenCompressDialog = { showCompressDialog = true }
                    )

                    1 -> OcrTextDetailTab(
                        doc = currentDoc,
                        editableText = editableOcrText,
                        onTextChange = { editableOcrText = it },
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

    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onSignatureCaptured = { signatureBmp ->
                showSignatureDialog = false
                viewModel.applySignatureToDocument(
                    docId = currentDoc.id,
                    signatureBitmap = signatureBmp,
                    pageIndex = -1
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
    onOpenWatermarkDialog: () -> Unit,
    onOpenSignatureDialog: () -> Unit,
    onOpenPasswordDialog: () -> Unit,
    onOpenCompressDialog: () -> Unit
) {
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
        // Horizontal Action Strip with 1-tap Convert to B&W, Sign, Watermark, Compress, Password
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Page ${index + 1} of ${renderedBitmaps.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (pageEntity != null) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
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

@Composable
private fun OcrTextDetailTab(
    doc: com.example.data.local.ScannedDocumentEntity,
    editableText: String,
    onTextChange: (String) -> Unit,
    onSaveText: () -> Unit,
    onCopyText: () -> Unit,
    onShareTxt: () -> Unit,
    onSaveTxtSaf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onCopyText,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ocr_copy_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy")
            }
            FilledTonalButton(
                onClick = onShareTxt,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ocr_share_txt_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share .TXT")
            }
            FilledTonalButton(
                onClick = onSaveTxtSaf,
                modifier = Modifier
                    .weight(1f)
                    .testTag("ocr_export_txt_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export")
            }
        }

        OutlinedTextField(
            value = editableText,
            onValueChange = onTextChange,
            label = { Text("Searchable OCR Text Layer (${doc.title})") },
            placeholder = { Text("No OCR text extracted yet. Tap the OCR icon on any page to extract text.") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("ocr_detail_text_input")
        )

        Button(
            onClick = onSaveText,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ocr_save_changes_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Edited OCR Text")
        }
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

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSignatureCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var selectedInkColor by remember { mutableIntStateOf(AndroidColor.rgb(15, 36, 96)) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageProcessor.loadBitmapFromUri(context, uri, maxDim = 800)
                if (bmp != null) {
                    onSignatureCaptured(bmp)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Signature (Draw or Import)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Draw your signature below or import a signature image.",
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke = currentStroke + change.position
                                },
                                onDragEnd = {
                                    if (currentStroke.size > 1) {
                                        strokes.add(currentStroke)
                                    }
                                    currentStroke = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val allStrokes = strokes + listOf(currentStroke)
                        for (pts in allStrokes) {
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    for (i in 1 until pts.size) {
                                        lineTo(pts[i].x, pts[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(selectedInkColor),
                                    style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
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
                            AndroidColor.rgb(180, 24, 24) to "Red"
                        ).forEach { (col, _) ->
                            Surface(
                                shape = CircleShape,
                                color = Color(col),
                                modifier = Modifier
                                    .size(28.dp)
                                    .border(
                                        width = if (selectedInkColor == col) 2.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ),
                                onClick = { selectedInkColor = col }
                            ) {}
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            strokes.clear()
                            currentStroke = emptyList()
                        }) {
                            Text("Clear")
                        }
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Text("Import")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bmp = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
                    val canvas = AndroidCanvas(bmp)
                    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                        color = selectedInkColor
                        style = AndroidPaint.Style.STROKE
                        strokeWidth = 7f
                        strokeCap = AndroidPaint.Cap.ROUND
                        strokeJoin = AndroidPaint.Join.ROUND
                    }
                    for (pts in strokes) {
                        for (i in 0 until pts.size - 1) {
                            canvas.drawLine(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y, paint)
                        }
                    }
                    onSignatureCaptured(bmp)
                },
                enabled = strokes.isNotEmpty()
            ) {
                Text("Stamp Signature")
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
