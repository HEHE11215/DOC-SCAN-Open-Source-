package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Approval
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ScannedDocumentEntity
import com.example.domain.model.AppThemeMode
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.ScanFilter
import com.example.ui.theme.DynamicRgbActionButton
import com.example.ui.theme.DynamicRgbGlowCard
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.GlassThemeTogglePill
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.theme.ShadcnBrandLogo
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.ui.viewmodel.ScannerCaptureMode
import com.example.utils.ImageProcessor
import com.example.utils.ShareExportHelper

private enum class ActivePdfToolDialog {
    NONE,
    PICK_DOC_FOR_DIRECT_EDIT,
    PICK_DOC_FOR_SHARE_PICTURE,
    MERGE_PDF,
    SPLIT_PDF,
    TEXT_TO_PDF,
    PICK_DOC_FOR_BW,
    PICK_DOC_FOR_COMPRESS,
    PICK_DOC_FOR_WATERMARK,
    BATCH_WATERMARK_STUDIO,
    ID_PASSPORT_A4_STUDIO,
    QR_BARCODE_STUDIO,
    RECEIPT_TABLE_CSV_STUDIO,
    PICK_DOC_FOR_SIGNATURE,
    PICK_DOC_FOR_STAMP,
    PICK_DOC_FOR_PASSWORD,
    PICK_DOC_FOR_JPG,
    PICK_DOC_FOR_PNG,
    PICK_DOC_FOR_TXT
}

@Composable
fun PdfToolsAndConvertScreen(
    viewModel: DocuScanViewModel,
    onOpenDocument: (String) -> Unit,
    onOpenDirectPdfEditor: (String, Int) -> Unit = { _, _ -> },
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
    var showStampForTarget by remember { mutableStateOf(false) }
    var showPasswordForTarget by remember { mutableStateOf(false) }
    var showCompressForTarget by remember { mutableStateOf(false) }

    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importExternalPdf(
                uri = uri,
                title = "PDF_${System.currentTimeMillis() / 1000}",
                onImported = { docId ->
                    onOpenDirectPdfEditor(docId, 0)
                }
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
                // Minimalist shadcn/ui Header with Brand Logo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShadcnBrandLogo(size = 28.dp)
                        Text(
                            text = "PDF Tools & Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
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
                    // Minimalist Monochrome Flagship Card: Direct PDF Editor
                    item {
                        DynamicRgbGlowCard(
                            cornerRadius = 12.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tools_flagship_direct_pdf_editor_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = "Direct PDF Editor",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(9.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Direct PDF Editor",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "STUDIO",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Edit text, names, signatures & comments directly on the main PDF or share pages as high-res pictures.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    DynamicRgbActionButton(
                                        text = "Edit PDF Studio",
                                        icon = Icons.Default.EditNote,
                                        badgeText = null,
                                        height = 42.dp,
                                        onClick = {
                                            if (allDocs.size == 1) {
                                                onOpenDirectPdfEditor(allDocs.first().id, 0)
                                            } else {
                                                activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_DIRECT_EDIT
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.15f)
                                            .testTag("tool_direct_pdf_editor_button")
                                    )
                                    GlassActionButton(
                                        text = "Import PDF",
                                        icon = Icons.Default.FileUpload,
                                        isPrimary = false,
                                        height = 42.dp,
                                        onClick = { importPdfLauncher.launch(arrayOf("application/pdf")) },
                                        modifier = Modifier.weight(0.95f)
                                    )
                                }
                            }
                        }
                    }

                    // NEW STUDIO SUITE SECTION: 4 Requested Next-Gen Workflows
                    item {
                        Text(
                            text = "New Studio Workflows",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "ID / Passport A4",
                                    subtitle = "2-Side A4 Combiner",
                                    icon = Icons.Default.Badge,
                                    testTag = "tool_id_passport_a4_combiner",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.ID_PASSPORT_A4_STUDIO }
                                )
                                ToolActionCard(
                                    title = "QR & Barcode",
                                    subtitle = "Scan & Generate QR",
                                    icon = Icons.Default.QrCode2,
                                    testTag = "tool_qr_generator_studio",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.QR_BARCODE_STUDIO }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "Batch Watermark",
                                    subtitle = "Multi-PDF & Page #",
                                    icon = Icons.Default.FormatListNumbered,
                                    testTag = "tool_batch_watermark_numbering",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.BATCH_WATERMARK_STUDIO }
                                )
                                ToolActionCard(
                                    title = "Receipt to CSV",
                                    subtitle = "Extract Table & CSV",
                                    icon = Icons.Default.TableChart,
                                    testTag = "tool_receipt_table_csv",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.RECEIPT_TABLE_CSV_STUDIO }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Edit & Protect",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "Direct Edit PDF",
                                    subtitle = "Change Name & Text",
                                    icon = Icons.Default.EditNote,
                                    testTag = "tool_grid_direct_edit_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_DIRECT_EDIT }
                                )
                                ToolActionCard(
                                    title = "Share Picture",
                                    subtitle = "Status / WhatsApp",
                                    icon = Icons.Default.Share,
                                    testTag = "tool_share_picture_status",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_SHARE_PICTURE }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "B&W Print PDF",
                                    subtitle = "Laser black & white",
                                    icon = Icons.Default.Contrast,
                                    testTag = "tool_bw_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_BW }
                                )
                                ToolActionCard(
                                    title = "Merge",
                                    subtitle = "Combine PDFs",
                                    icon = Icons.AutoMirrored.Filled.MergeType,
                                    testTag = "tool_merge_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.MERGE_PDF }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "Split",
                                    subtitle = "Extract pages",
                                    icon = Icons.AutoMirrored.Filled.CallSplit,
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "Sign",
                                    subtitle = "Draw signature",
                                    icon = Icons.Default.Draw,
                                    testTag = "tool_sign_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_SIGNATURE }
                                )
                                ToolActionCard(
                                    title = "Stamp PDF",
                                    subtitle = "Approved / Paid",
                                    icon = Icons.Default.Approval,
                                    testTag = "tool_stamp_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_STAMP }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "Watermark",
                                    subtitle = "Text & page #",
                                    icon = Icons.Default.FormatListNumbered,
                                    testTag = "tool_watermark_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_WATERMARK }
                                )
                                ToolActionCard(
                                    title = "Lock PDF",
                                    subtitle = "Password lock",
                                    icon = Icons.Default.Lock,
                                    testTag = "tool_password_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.PICK_DOC_FOR_PASSWORD }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ToolActionCard(
                                    title = "Text to PDF",
                                    subtitle = "Notes to PDF",
                                    icon = Icons.AutoMirrored.Filled.NoteAdd,
                                    testTag = "tool_text_to_pdf",
                                    modifier = Modifier.weight(1f),
                                    onClick = { activeDialog = ActivePdfToolDialog.TEXT_TO_PDF }
                                )
                                ToolActionCard(
                                    title = "Import PDF",
                                    subtitle = "Open & Direct Edit",
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
                            text = "Convert & Export",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            text = "Specialized Scanners",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SpecializedScannerBanner(
                                title = "ID Card & Passport (Front + Back A4)",
                                description = "Compose 2 sides onto a print-ready A4 page with cut marks",
                                icon = Icons.Default.Badge,
                                testTag = "tool_id_card_scanner",
                                onClick = { activeDialog = ActivePdfToolDialog.ID_PASSPORT_A4_STUDIO }
                            )
                            SpecializedScannerBanner(
                                title = "Business Card Scanner",
                                description = "Auto-extract contact details & vCard fields",
                                icon = Icons.Default.ContactMail,
                                testTag = "tool_business_card_scanner",
                                onClick = { onStartSpecializedScan(ScannerCaptureMode.BUSINESS_CARD) }
                            )
                            SpecializedScannerBanner(
                                title = "QR & Barcode Scanner + Generator",
                                description = "Scan camera codes or generate custom QR codes for PDFs",
                                icon = Icons.Default.QrCodeScanner,
                                testTag = "tool_qr_barcode_scanner",
                                onClick = { activeDialog = ActivePdfToolDialog.QR_BARCODE_STUDIO }
                            )
                            SpecializedScannerBanner(
                                title = "Smart Receipt & Table to CSV",
                                description = "Extract structured line items, dates & totals to spreadsheet",
                                icon = Icons.Default.ReceiptLong,
                                testTag = "tool_smart_receipt_scanner",
                                onClick = { activeDialog = ActivePdfToolDialog.RECEIPT_TABLE_CSV_STUDIO }
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

        ActivePdfToolDialog.PICK_DOC_FOR_DIRECT_EDIT -> {
            PickSingleDocumentDialog(
                title = "Direct PDF Editor",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    onOpenDirectPdfEditor(doc.id, 0)
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_SHARE_PICTURE -> {
            PickSingleDocumentDialog(
                title = "Share Document as Picture",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.shareDocumentPageAsPicture(doc, 0)
                }
            )
        }

        ActivePdfToolDialog.PICK_DOC_FOR_BW -> {
            PickSingleDocumentDialog(
                title = "Convert to Crisp B&W Print",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.convertDocumentFilter(doc.id, ScanFilter.CLEAR)
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

        ActivePdfToolDialog.BATCH_WATERMARK_STUDIO -> {
            BatchWatermarkAndNumberingDialog(
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onApplyBatch = { docIds, watermarkText, format, position ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.applyBatchWatermarkAndPageNumbers(
                        docIds = docIds,
                        watermarkText = watermarkText,
                        pageNumberFormat = format,
                        pageNumberPosition = position
                    )
                }
            )
        }

        ActivePdfToolDialog.ID_PASSPORT_A4_STUDIO -> {
            IdCardPassportA4StudioDialog(
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onLaunchLiveCameraScan = {
                    activeDialog = ActivePdfToolDialog.NONE
                    onStartSpecializedScan(ScannerCaptureMode.ID_CARD)
                },
                onCreateA4Document = { frontUri, backUri, title, applyBwPrint, showCropMarks, docTypeLabel ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.createIdCardPassportA4Document(
                        frontUri = frontUri,
                        backUri = backUri,
                        title = title,
                        applyBwPrint = applyBwPrint,
                        showCropMarks = showCropMarks,
                        documentTypeLabel = docTypeLabel,
                        onCreated = onOpenDocument
                    )
                }
            )
        }

        ActivePdfToolDialog.QR_BARCODE_STUDIO -> {
            QrBarcodeGeneratorAndScannerDialog(
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onLaunchCameraScanner = {
                    activeDialog = ActivePdfToolDialog.NONE
                    onStartSpecializedScan(ScannerCaptureMode.QR_BARCODE)
                },
                onShareQrPng = { bmp, label ->
                    ShareExportHelper.shareBitmapAsPng(context, bmp, "QR_$label")
                },
                onStampQrToDocument = { docId, bmp, label ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.stampQrBitmapOnDocument(
                        docId = docId,
                        qrBitmap = bmp,
                        qrLabel = label,
                        onComplete = onOpenDocument
                    )
                }
            )
        }

        ActivePdfToolDialog.RECEIPT_TABLE_CSV_STUDIO -> {
            SmartReceiptAndTableCsvDialog(
                documents = allDocs,
                viewModel = viewModel,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onExportCsv = { csvContent, docTitle ->
                    ShareExportHelper.shareCsvContent(
                        context = context,
                        csvContent = csvContent,
                        fileNameWithoutExt = "${docTitle}_Table"
                    )
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

        ActivePdfToolDialog.PICK_DOC_FOR_STAMP -> {
            PickSingleDocumentDialog(
                title = "Stamp PDF",
                documents = allDocs,
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onSelected = { doc ->
                    activeDialog = ActivePdfToolDialog.NONE
                    targetDoc = doc
                    showStampForTarget = true
                }
            )
        }

        ActivePdfToolDialog.TEXT_TO_PDF -> {
            TextToPdfDialog(
                onDismiss = { activeDialog = ActivePdfToolDialog.NONE },
                onCreatePdf = { title, body ->
                    activeDialog = ActivePdfToolDialog.NONE
                    viewModel.createPdfFromTextNote(
                        title = title,
                        bodyText = body,
                        onCreated = onOpenDocument
                    )
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
        LaunchedEffect(currentTarget.thumbnailPath) {
            previewBmp = ImageProcessor.loadBitmapFromPath(currentTarget.thumbnailPath)
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

    if (showStampForTarget && currentTarget != null) {
        var previewBmp by remember(currentTarget.thumbnailPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
        LaunchedEffect(currentTarget.thumbnailPath) {
            previewBmp = ImageProcessor.loadBitmapFromPath(currentTarget.thumbnailPath)
        }
        OfficialStampDialog(
            onDismiss = { showStampForTarget = false },
            pagePreviewBitmap = previewBmp,
            pageCount = currentTarget.pageCount.coerceAtLeast(1),
            onStampImposed = { stampText, subText, colorArgb, pageIdx, normX, normY, scale ->
                showStampForTarget = false
                viewModel.applyOfficialStampToDocument(
                    docId = currentTarget.id,
                    stampText = stampText,
                    subText = subText,
                    inkColor = colorArgb,
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
        cornerRadius = 10.dp,
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
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
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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
        cornerRadius = 10.dp,
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
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(9.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
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

/**
 * 1. ID Card & Passport 2-Side A4 Combiner Studio Dialog
 */
@Composable
private fun IdCardPassportA4StudioDialog(
    onDismiss: () -> Unit,
    onLaunchLiveCameraScan: () -> Unit,
    onCreateA4Document: (
        frontUri: Uri?,
        backUri: Uri?,
        title: String,
        applyBwPrint: Boolean,
        showCropMarks: Boolean,
        docTypeLabel: String
    ) -> Unit
) {
    var docTitle by remember { mutableStateOf("ID_Passport_A4_${System.currentTimeMillis() / 1000}") }
    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var backUri by remember { mutableStateOf<Uri?>(null) }
    var applyBwPrint by remember { mutableStateOf(true) }
    var showCropMarks by remember { mutableStateOf(true) }
    var isPassportMode by remember { mutableStateOf(false) }

    val frontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) frontUri = uri }

    val backPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) backUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ID Card & Passport 2-Side A4 Combiner",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Compose Front + Back sides onto a single print-ready A4 sheet with corner cut guides and laser B&W print clarity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isPassportMode,
                        onClick = { isPassportMode = false },
                        label = { Text("ID / Driver Card") }
                    )
                    FilterChip(
                        selected = isPassportMode,
                        onClick = { isPassportMode = true },
                        label = { Text("Passport Spread") }
                    )
                }

                OutlinedTextField(
                    value = docTitle,
                    onValueChange = { docTitle = it },
                    label = { Text("A4 Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            frontPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_id_front_button")
                    ) {
                        Text(if (frontUri != null) "Front Selected ✓" else "1. Pick Front")
                    }
                    OutlinedButton(
                        onClick = {
                            backPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_id_back_button")
                    ) {
                        Text(if (backUri != null) "Back Selected ✓" else "2. Pick Back")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyBwPrint = !applyBwPrint }
                ) {
                    Checkbox(checked = applyBwPrint, onCheckedChange = { applyBwPrint = it })
                    Text(
                        text = "Clear Mode B&W Print Enhancement",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCropMarks = !showCropMarks }
                ) {
                    Checkbox(checked = showCropMarks, onCheckedChange = { showCropMarks = it })
                    Text(
                        text = "Draw A4 Corner Cut & Alignment Marks",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                GlassActionButton(
                    text = "Scan Front + Back with Live Camera",
                    icon = Icons.Default.Badge,
                    isPrimary = false,
                    height = 38.dp,
                    onClick = onLaunchLiveCameraScan,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = if (isPassportMode) "PASSPORT BIO & VISA (2-SIDE A4)" else "ID CARD (2-SIDE A4)"
                    onCreateA4Document(frontUri, backUri, docTitle.trim(), applyBwPrint, showCropMarks, label)
                },
                modifier = Modifier.testTag("confirm_id_passport_a4_button")
            ) {
                Text("Generate A4 PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * 2. QR & Barcode Scanner + Generator Studio Dialog
 */
@Composable
private fun QrBarcodeGeneratorAndScannerDialog(
    documents: List<ScannedDocumentEntity>,
    onDismiss: () -> Unit,
    onLaunchCameraScanner: () -> Unit,
    onShareQrPng: (android.graphics.Bitmap, String) -> Unit,
    onStampQrToDocument: (String?, android.graphics.Bitmap, String) -> Unit
) {
    var presetType by remember { mutableStateOf("URL") }
    var payloadText by remember { mutableStateOf("https://docscan.app/verify") }
    var captionLabel by remember { mutableStateOf("VERIFIED DOCUMENT") }
    var selectedDocId by remember { mutableStateOf<String?>(documents.firstOrNull()?.id) }

    val qrBitmap = remember(payloadText, captionLabel) {
        ImageProcessor.generateQrCodeBitmap(
            content = payloadText.ifBlank { "https://docscan.app" },
            sizePx = 560,
            label = captionLabel
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "QR & Barcode Studio",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "URL" to "https://docscan.app/verify",
                        "Text" to "APPROVED • REF #2026-884",
                        "Wi-Fi" to "WIFI:S:OfficeNet;T:WPA;P:SecurePass2026;;",
                        "vCard" to "BEGIN:VCARD\nFN:Alex Mercer\nTEL:+1-555-0192\nEND:VCARD"
                    ).forEach { (type, sample) ->
                        FilterChip(
                            selected = presetType == type,
                            onClick = {
                                presetType = type
                                payloadText = sample
                            },
                            label = { Text(type) }
                        )
                    }
                }

                OutlinedTextField(
                    value = payloadText,
                    onValueChange = { payloadText = it },
                    label = { Text("QR Code Payload ($presetType)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qr_payload_input")
                )

                OutlinedTextField(
                    value = captionLabel,
                    onValueChange = { captionLabel = it },
                    label = { Text("Footer Caption (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Generated QR Preview",
                        modifier = Modifier
                            .size(164.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(10.dp))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaunchCameraScanner,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scan Live")
                    }
                    OutlinedButton(
                        onClick = { onShareQrPng(qrBitmap, captionLabel.ifBlank { presetType }) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_qr_png_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PNG")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onStampQrToDocument(selectedDocId, qrBitmap, captionLabel.ifBlank { presetType })
                },
                modifier = Modifier.testTag("stamp_qr_to_pdf_button")
            ) {
                Text(if (selectedDocId != null) "Embed on PDF" else "Save QR PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

/**
 * 3. Batch Page Numbering & Custom Watermarks Studio Dialog
 */
@Composable
private fun BatchWatermarkAndNumberingDialog(
    documents: List<ScannedDocumentEntity>,
    onDismiss: () -> Unit,
    onApplyBatch: (
        docIds: List<String>,
        watermarkText: String,
        format: PageNumberFormat?,
        position: PageNumberPosition
    ) -> Unit
) {
    val selectedIds = remember {
        mutableStateListOf<String>().apply {
            addAll(documents.map { it.id })
        }
    }
    var watermarkText by remember { mutableStateOf("CONFIDENTIAL") }
    var selectedFormat by remember { mutableStateOf<PageNumberFormat?>(PageNumberFormat.PAGE_X_OF_Y) }
    var selectedPosition by remember { mutableStateOf(PageNumberPosition.BOTTOM_RIGHT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Batch Watermark & Page Numbering",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text("Custom Watermark Text (leave blank for none)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("batch_watermark_input")
                )

                Text(
                    text = "Page Number Format",
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
                        selected = selectedFormat == null,
                        onClick = { selectedFormat = null },
                        label = { Text("None") }
                    )
                    PageNumberFormat.entries.forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.label) }
                        )
                    }
                }

                Text(
                    text = "Page Number Position",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PageNumberPosition.entries.forEach { pos ->
                        FilterChip(
                            selected = selectedPosition == pos,
                            onClick = { selectedPosition = pos },
                            label = { Text(pos.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Target Documents (${selectedIds.size}/${documents.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            if (selectedIds.size == documents.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(documents.map { it.id })
                            }
                        }
                    ) {
                        Text(if (selectedIds.size == documents.size) "Clear All" else "Select All")
                    }
                }

                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        val checked = doc.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) selectedIds.remove(doc.id) else selectedIds.add(doc.id)
                                }
                            )
                            Text(
                                text = "${doc.title} (${doc.pageCount}p)",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyBatch(
                        selectedIds.toList(),
                        watermarkText.trim(),
                        selectedFormat,
                        selectedPosition
                    )
                },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier.testTag("confirm_batch_watermark_button")
            ) {
                Text("Apply to ${selectedIds.size} PDF(s)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * 4. Smart Receipt & Table Data Extractor to CSV Studio Dialog
 */
@Composable
private fun SmartReceiptAndTableCsvDialog(
    documents: List<ScannedDocumentEntity>,
    viewModel: DocuScanViewModel,
    onDismiss: () -> Unit,
    onExportCsv: (csvContent: String, docTitle: String) -> Unit
) {
    var selectedDoc by remember { mutableStateOf(documents.firstOrNull()) }
    var extractedRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var csvText by remember { mutableStateOf("") }
    var isExtracting by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDoc?.id) {
        val doc = selectedDoc ?: return@LaunchedEffect
        isExtracting = true
        viewModel.extractReceiptAndTableCsv(doc.id) { rows, csv ->
            extractedRows = rows
            csvText = csv
            isExtracting = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Smart Receipt & Table to CSV",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select a scanned receipt, invoice, or table document to extract structured rows and export as a spreadsheet (.csv).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (documents.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        documents.forEach { doc ->
                            FilterChip(
                                selected = selectedDoc?.id == doc.id,
                                onClick = { selectedDoc = doc },
                                label = {
                                    Text(
                                        text = doc.title.take(18),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (isExtracting) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Parsing OCR table & receipt rows...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(extractedRows) { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = row.getOrNull(1).orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.width(68.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = row.getOrNull(2).orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = row.getOrNull(3).orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val title = selectedDoc?.title ?: "Receipt_Table"
                    onExportCsv(csvText, title)
                },
                enabled = csvText.isNotBlank(),
                modifier = Modifier.testTag("export_receipt_csv_button")
            ) {
                Text("Export CSV (${(extractedRows.size - 1).coerceAtLeast(0)} rows)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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

@Composable
private fun TextToPdfDialog(
    onDismiss: () -> Unit,
    onCreatePdf: (title: String, body: String) -> Unit
) {
    var title by remember { mutableStateOf("Note_${System.currentTimeMillis() / 1000}") }
    var bodyText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Text / Notes to PDF Generator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Type or paste notes, meeting minutes, or agreements to generate a formatted multi-page searchable PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("PDF Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bodyText,
                    onValueChange = { bodyText = it },
                    label = { Text("Document Body Content") },
                    placeholder = { Text("Enter or paste your text here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreatePdf(title.trim().ifBlank { "Document_Note" }, bodyText) },
                enabled = bodyText.isNotBlank(),
                modifier = Modifier.testTag("confirm_text_to_pdf_button")
            ) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
