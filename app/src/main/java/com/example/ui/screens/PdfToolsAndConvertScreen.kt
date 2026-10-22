package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedDocumentEntity
import com.example.domain.model.AppThemeMode
import com.example.domain.model.ScanFilter
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.GlassThemeTogglePill
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.ui.viewmodel.ScannerCaptureMode
import com.example.utils.ShareExportHelper

private enum class ActivePdfToolDialog {
    NONE,
    MERGE_PDF,
    SPLIT_PDF,
    PICK_DOC_FOR_BW,
    PICK_DOC_FOR_COMPRESS,
    PICK_DOC_FOR_WATERMARK,
    PICK_DOC_FOR_SIGNATURE,
    PICK_DOC_FOR_PASSWORD,
    PICK_DOC_FOR_JPG,
    PICK_DOC_FOR_PNG,
    PICK_DOC_FOR_TXT
}

@Composable
fun PdfToolsAndConvertScreen(
    viewModel: DocuScanViewModel,
    onOpenDocument: (String) -> Unit,
    onStartSpecializedScan: (ScannerCaptureMode) -> Unit,
    onNavigateToCrop: () -> Unit,
    onNavigateToEnhance: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkGlass.current
    val allDocs by viewModel.filteredDocuments.collectAsStateWithLifecycle()

    var activeDialog by remember { mutableStateOf(ActivePdfToolDialog.NONE) }
    var targetDoc by remember { mutableStateOf<ScannedDocumentEntity?>(null) }

    var showWatermarkForTarget by remember { mutableStateOf(false) }
    var showSignatureForTarget by remember { mutableStateOf(false) }
    var showPasswordForTarget by remember { mutableStateOf(false) }
    var showCompressForTarget by remember { mutableStateOf(false) }

    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalPdf(
                uri = uri,
                title = "PDF_${System.currentTimeMillis() / 1000}",
                onImported = onOpenDocument
            )
        }
    }

    val imagesToPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 25)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.startNewScanSession(ScannerCaptureMode.STANDARD_BATCH)
            viewModel.onMultipleGalleryUrisSelected(
                uris = uris,
                onReadyForCrop = onNavigateToCrop,
                onReadyForEnhance = onNavigateToEnhance
            )
        }
    }

    GlassScreenBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Clean iOS Glass Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PDF Tools",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    GlassThemeTogglePill(
                        isDark = isDark,
                        onToggle = {
                            viewModel.setThemeMode(
                                if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                            )
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Edit & Protect",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ToolActionCard(
                                    title = "B&W PDF",
                                    subtitle = "Sharp black & white",
                                    icon = Icons.Default.Contrast,
                                    testTag = "tool_bw_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_BW }
                                )
                                ToolActionCard(
                                    title = "Merge",
                                    subtitle = "Combine PDFs",
                                    icon = Icons.Default.MergeType,
                                    testTag = "tool_merge_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.MERGE_PDF }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ToolActionCard(
                                    title = "Split",
                                    subtitle = "Extract pages",
                                    icon = Icons.Default.CallSplit,
                                    testTag = "tool_split_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.SPLIT_PDF }
                                )
                                ToolActionCard(
                                    title = "Compress",
                                    subtitle = "Smaller size",
                                    icon = Icons.Default.Compress,
                                    testTag = "tool_compress_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_COMPRESS }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ToolActionCard(
                                    title = "Sign",
                                    subtitle = "Draw signature",
                                    icon = Icons.Default.Draw,
                                    testTag = "tool_sign_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_SIGNATURE }
                                )
                                ToolActionCard(
                                    title = "Watermark",
                                    subtitle = "Text & page #",
                                    icon = Icons.Default.FormatListNumbered,
                                    testTag = "tool_watermark_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_WATERMARK }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ToolActionCard(
                                    title = "Lock PDF",
                                    subtitle = "Password lock",
                                    icon = Icons.Default.Lock,
                                    testTag = "tool_password_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_PASSWORD }
                                )
                                ToolActionCard(
                                    title = "Import PDF",
                                    subtitle = "Open & OCR",
                                    icon = Icons.Default.FileUpload,
                                    testTag = "tool_import_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { importPdfLauncher.launch(arrayOf("application/pdf")) }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Convert",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ToolActionCard(
                                    title = "Photos to PDF",
                                    subtitle = "Crop & convert",
                                    icon = Icons.Default.PhotoLibrary,
                                    testTag = "tool_images_to_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        imagesToPdfPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                                ToolActionCard(
                                    title = "PDF to JPG",
                                    subtitle = "Save as images",
                                    icon = Icons.Default.Image,
                                    testTag = "tool_pdf_to_jpg",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_JPG }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ToolActionCard(
                                    title = "PDF to PNG",
                                    subtitle = "Lossless pages",
                                    icon = Icons.Default.Image,
                                    testTag = "tool_pdf_to_png",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_PNG }
                                )
                                ToolActionCard(
                                    title = "PDF to TXT",
                                    subtitle = "Extract text",
                                    icon = Icons.Default.Description,
                                    testTag = "tool_pdf_to_txt",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_TXT }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Scanners",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SpecializedScannerBanner(
                                title = "ID Card (Front + Back)",
                                description = "Both sides on a single page",
                                icon = Icons.Default.Badge,
                                testTag = "tool_id_card_scanner",
                                onClick = { onStartSpecializedScan(ScannerCaptureMode.ID_CARD) }
                            )
                            SpecializedScannerBanner(
                                title = "Business Card",
                                description = "Auto-extract contact details",
                                icon = Icons.Default.ContactMail,
                                testTag = "tool_business_card_scanner",
                                onClick = { onStartSpecializedScan(ScannerCaptureMode.BUSINESS_CARD) }
                            )
                            SpecializedScannerBanner(
                                title = "QR & Barcode",
                                description = "Instant code & URL reader",
                                icon = Icons.Default.QrCodeScanner,
                                testTag = "tool_qr_barcode_scanner",
                                onClick = { onStartSpecializedScan(ScannerCaptureMode.QR_BARCODE) }
                            )
                        }
                        Spacer(modifier = Modifier.height(104.dp))
                    }
                }
            }
        }
    }

    when (activeDialog) {
        ActivePdfToolDialog.NONE -> Unit

        ActivePdfToolDialog.PICK_DOC_FOR_BW -> {
            PickSingleDocumentDialog(
                title = "Convert to B&W",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.convertDocumentFilter(doc.id, ScanFilter.BW)
                    onOpenDocument(doc.id)
                }
            )
        }

        ActivePdfToolDialog.MERGE_PDF -> {
            MergePdfsDialog(
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onMerge = { ids, title ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.mergeDocumentsByIds(ids, title, onComplete = onOpenDocument)
                }
            )
        }

        ActivePdfToolDialog.SPLIT_PDF -> {
            SplitPdfDialog(
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSplit = { docId, start, end, title ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.splitDocumentRange(docId, start, end, title, onComplete = onOpenDocument)
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_COMPRESS -> {
            PickSingleDocumentDialog(
                title = "Compress PDF",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    targetDoc = doc
                    showCompressForTarget = true
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_WATERMARK -> {
            PickSingleDocumentDialog(
                title = "Watermark & Page #",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    targetDoc = doc
                    showWatermarkForTarget = true
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_SIGNATURE -> {
            PickSingleDocumentDialog(
                title = "Sign PDF",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    targetDoc = doc
                    showSignatureForTarget = true
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_PASSWORD -> {
            PickSingleDocumentDialog(
                title = "Password Lock",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    targetDoc = doc
                    showPasswordForTarget = true
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_JPG -> {
            PickSingleDocumentDialog(
                title = "PDF to JPG",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.exportDocumentAsImages(doc, asPng = false)
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_PNG -> {
            PickSingleDocumentDialog(
                title = "PDF to PNG",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.exportDocumentAsImages(doc, asPng = true)
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_TXT -> {
            PickSingleDocumentDialog(
                title = "Extract to TXT",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    val txtFile = ShareExportHelper.exportTextToCacheFile(
                        context = context,
                        text = doc.ocrText.ifBlank { "Document: ${doc.title}\n(No OCR text)" },
                        fileNameWithoutExt = doc.title.replace(" ", "_")
                    )
                    ShareExportHelper.shareTextFile(context, txtFile, doc.title)
                }
            )
        }
    }

    val currentTarget = targetDoc
    if (showWatermarkForTarget && currentTarget != null) {
        WatermarkAndNumberingDialog(
            initialWatermark = currentTarget.watermarkText,
            onDismiss = { showWatermarkForTarget = false },
            onConfirm = { wm, fmt, pos ->
                showWatermarkForTarget = false
                viewModel.applyWatermarkOrPageNumbers(currentTarget.id, wm, fmt, pos)
            }
        )
    }

    if (showSignatureForTarget && currentTarget != null) {
        var previewBmp by remember(currentTarget.thumbnailPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
        androidx.compose.runtime.LaunchedEffect(currentTarget.thumbnailPath) {
            previewBmp = com.example.utils.ImageProcessor.loadBitmapFromPath(currentTarget.thumbnailPath)
        }
        SignaturePadDialog(
            onDismiss = { showSignatureForTarget = false },
            pagePreviewBitmap = previewBmp,
            pageCount = currentTarget.pageCount.coerceAtLeast(1),
            onSignatureImposed = { bmp, pageIdx, normX, normY, scale ->
                showSignatureForTarget = false
                viewModel.applySignatureToDocument(
                    docId = currentTarget.id,
                    signatureBitmap = bmp,
                    pageIndex = pageIdx,
                    normX = normX,
                    normY = normY,
                    scale = scale
                )
                onOpenDocument(currentTarget.id)
            }
        )
    }

    if (showPasswordForTarget && currentTarget != null) {
        PasswordManageDialog(
            isCurrentlyProtected = currentTarget.isPasswordProtected,
            onDismiss = { showPasswordForTarget = false },
            onSavePassword = { pass ->
                showPasswordForTarget = false
                viewModel.setDocumentPassword(currentTarget, pass)
            }
        )
    }

    if (showCompressForTarget && currentTarget != null) {
        CompressPdfDialog(
            currentSizeBytes = currentTarget.fileSizeBytes,
            onDismiss = { showCompressForTarget = false },
            onCompress = { quality ->
                showCompressForTarget = false
                viewModel.compressDocument(currentTarget.id, quality)
            }
        )
    }
}

@Composable
private fun ToolActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FrostedGlassCard(
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = modifier
            .height(66.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SpecializedScannerBanner(
    title: String,
    description: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    FrostedGlassCard(
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MergePdfsDialog(
    documents: List<ScannedDocumentEntity>,
    onDismiss: () -> Unit,
    onMerge: (List<String>, String) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<String>() }
    var mergedTitle by remember { mutableStateOf("Merged_${System.currentTimeMillis() / 1000}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge PDFs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = mergedTitle,
                    onValueChange = { mergedTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        val checked = doc.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    "${doc.pageCount}p • ${ShareExportHelper.formatFileSize(doc.fileSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onMerge(selectedIds.toList(), mergedTitle.trim()) },
                enabled = selectedIds.size >= 2
            ) {
                Text("Merge (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SplitPdfDialog(
    documents: List<ScannedDocumentEntity>,
    onDismiss: () -> Unit,
    onSplit: (String, Int, Int, String) -> Unit
) {
    var selectedDoc by remember { mutableStateOf(documents.firstOrNull()) }
    var startPageText by remember { mutableStateOf("1") }
    var endPageText by remember { mutableStateOf((documents.firstOrNull()?.pageCount ?: 1).toString()) }
    var newTitle by remember { mutableStateOf("Split_Extract") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDoc = doc
                                    startPageText = "1"
                                    endPageText = doc.pageCount.toString()
                                    newTitle = "${doc.title}_Split"
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedDoc?.id == doc.id,
                                onCheckedChange = {
                                    selectedDoc = doc
                                    startPageText = "1"
                                    endPageText = doc.pageCount.toString()
                                    newTitle = "${doc.title}_Split"
                                }
                            )
                            Text(
                                "${doc.title} (${doc.pageCount}p)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPageText,
                        onValueChange = { startPageText = it },
                        label = { Text("From") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endPageText,
                        onValueChange = { endPageText = it },
                        label = { Text("To") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("New Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val doc = selectedDoc ?: return@Button
                    val start = startPageText.toIntOrNull() ?: 1
                    val end = endPageText.toIntOrNull() ?: doc.pageCount
                    onSplit(doc.id, start, end, newTitle.trim())
                },
                enabled = selectedDoc != null
            ) {
                Text("Split")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PickSingleDocumentDialog(
    title: String,
    documents: List<ScannedDocumentEntity>,
    onDismiss: () -> Unit,
    onSelected: (ScannedDocumentEntity) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (documents.isEmpty()) {
                Text("No PDFs in library yet.")
            } else {
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        Card(
                            onClick = { onSelected(doc) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(doc.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${doc.pageCount}p • ${ShareExportHelper.formatFileSize(doc.fileSizeBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
