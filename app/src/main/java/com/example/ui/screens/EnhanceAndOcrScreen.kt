package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DocumentCategory
import com.example.domain.model.OcrScript
import com.example.domain.model.PdfPageSize
import com.example.domain.model.PdfQuality
import com.example.domain.model.ScanFilter
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.LuminousCyan
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.utils.LocalHaptics
import com.example.utils.ShareExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhanceAndOcrScreen(
    viewModel: DocuScanViewModel,
    onAddMorePages: () -> Unit,
    onSavedDocument: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session by viewModel.scanSession.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val folders by viewModel.folders.collectAsState()

    val activePage = session.pages.getOrNull(session.activePageIndex)
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Enhance Filters, 1 = OCR & Text Layer
    var showAdjustSliders by remember { mutableStateOf(false) }
    var showSavePdfDialog by remember { mutableStateOf(false) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var showScriptMenu by remember { mutableStateOf(false) }
    var highlightedBlockText by remember { mutableStateOf<String?>(null) }

    val exportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && activePage != null) {
            scope.launch {
                val ok = ShareExportHelper.writeTextToSafUri(context, activePage.ocrText, uri)
                viewModel.showStatus(if (ok) "Exported OCR text to TXT file" else "TXT export failed")
            }
        }
    }

    if (activePage == null) {
        GlassScreenBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No scanned pages in current session.")
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAddMorePages) {
                    Text("Capture or Import Page")
                }
            }
        }
        return
    }

    GlassScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 6.dp)
        ) {
            // Top Frosted Glass Header
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "${session.activePageIndex + 1} / ${session.pages.size} • ${activePage.filter.label}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassActionButton(
                            text = "Add",
                            icon = Icons.Default.AddAPhoto,
                            isPrimary = false,
                            height = 38.dp,
                            onClick = onAddMorePages,
                            modifier = Modifier.testTag("add_more_pages_button")
                        )
                        GlassActionButton(
                            text = "Save",
                            icon = Icons.Default.PictureAsPdf,
                            isPrimary = true,
                            height = 38.dp,
                            onClick = { showSavePdfDialog = true },
                            modifier = Modifier.testTag("save_pdf_button")
                        )
                    }
                }
            }

            // Mode Tabs: Enhance vs OCR
            PrimaryTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Enhance") },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("OCR Text") },
                    icon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                    modifier = Modifier.testTag("ocr_tab_button")
                )
            }

            // Business Card Parsed Banner if captured in Business Card mode
            session.businessCardInfo?.let { card ->
                FrostedGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    cornerRadius = 16.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Business Card: ${card.fullName.ifBlank { "Contact" }} ${if (card.company.isNotBlank()) "• ${card.company}" else ""}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Phone: ${card.phone.ifBlank { "N/A" }} • Email: ${card.email.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Main Image Canvas with Interactive OCR Bounding Box Highlights
            Box(
                modifier = Modifier
                    .weight(if (activeTab == 0) 1f else 0.40f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, Color.White, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = activePage.enhancedBitmap.asImageBitmap(),
                    contentDescription = "Enhanced document page",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )

                if (activeTab == 1 && activePage.ocrBlocks.isNotEmpty()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activePage.ocrBlocks) {
                                detectTapGestures { tapOffset ->
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val hit = activePage.ocrBlocks.firstOrNull { block ->
                                        block.toRectF(w, h).contains(tapOffset.x, tapOffset.y)
                                    }
                                    if (hit != null) {
                                        highlightedBlockText = hit.text
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("OCR Block", hit.text))
                                        viewModel.showStatus("Copied block: \"${hit.text.take(40)}\"")
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        activePage.ocrBlocks.forEach { block ->
                            val rect = block.toRectF(w, h)
                            drawRoundRect(
                                color = LuminousCyan.copy(alpha = 0.22f),
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width(), rect.height()),
                                cornerRadius = CornerRadius(6f, 6f)
                            )
                            drawRoundRect(
                                color = ElectricTeal,
                                topLeft = Offset(rect.left, rect.top),
                                size = Size(rect.width(), rect.height()),
                                cornerRadius = CornerRadius(6f, 6f),
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }
            }

            // Multi-page thumbnail strip if > 1 page
            if (session.pages.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    session.pages.forEachIndexed { idx, draft ->
                        val isSelected = idx == session.activePageIndex
                        Box(
                            modifier = Modifier
                                .size(50.dp, 64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.selectActiveDraftPage(idx) }
                        ) {
                            Image(
                                bitmap = draft.enhancedBitmap.asImageBitmap(),
                                contentDescription = "Page ${idx + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Bottom Control Panel based on Active Tab
            if (activeTab == 0) {
                EnhanceControlsPanel(
                    activeFilter = activePage.filter,
                    brightness = activePage.brightness,
                    contrast = activePage.contrast,
                    showSliders = showAdjustSliders,
                    canDeletePage = session.pages.size > 1,
                    onFilterSelected = { filter ->
                        viewModel.applyFilterAndAdjustmentsToActivePage(filter = filter)
                    },
                    onManualCropEdges = {
                        viewModel.reCropActiveDraftPage(onNavigateToCrop = onAddMorePages)
                    },
                    onToggleSliders = { showAdjustSliders = !showAdjustSliders },
                    onBrightnessContrastChange = { b, c ->
                        viewModel.applyFilterAndAdjustmentsToActivePage(brightness = b, contrast = c)
                    },
                    onRotate90 = {
                        viewModel.applyFilterAndAdjustmentsToActivePage(rotateDelta = 90)
                    },
                    onDeskew = {
                        viewModel.applyFilterAndAdjustmentsToActivePage(autoDeskew = true)
                    },
                    onOpenSignDialog = { showSignatureDialog = true },
                    onDeletePage = { viewModel.deleteActiveDraftPage() }
                )
            } else {
                OcrControlsPanel(
                    modifier = Modifier.weight(0.60f),
                    selectedScript = session.selectedOcrScript,
                    ocrText = activePage.ocrText,
                    highlightedBlock = highlightedBlockText,
                    showScriptMenu = showScriptMenu,
                    onToggleScriptMenu = { showScriptMenu = !showScriptMenu },
                    onSelectScript = { script ->
                        showScriptMenu = false
                        viewModel.setOcrScript(script)
                    },
                    onRunOcrPage = { viewModel.runOcrOnActivePage() },
                    onRunOcrAllPages = { viewModel.runOcrOnAllDraftPages() },
                    onOcrTextEdited = { viewModel.updateActivePageOcrText(it) },
                    onCopyText = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("OCR Text", activePage.ocrText))
                        viewModel.showStatus("Copied recognized text to clipboard")
                    },
                    onShareText = {
                        ShareExportHelper.sharePlainText(context, "Scanned OCR Text", activePage.ocrText)
                    },
                    onExportTxt = {
                        exportTxtLauncher.launch("DocuScan_OCR_${System.currentTimeMillis() / 1000}.txt")
                    }
                )
            }
        }

        if (session.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                FrostedGlassCard(cornerRadius = 22.dp) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(session.processingMessage)
                    }
                }
            }
        }
    }

    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            pagePreviewBitmap = activePage.enhancedBitmap,
            pageCount = session.pages.size.coerceAtLeast(1),
            initialPageIndex = session.activePageIndex,
            onSignatureImposed = { sigBmp, targetPageIdx, normX, normY, scale ->
                showSignatureDialog = false
                if (targetPageIdx >= 0 && targetPageIdx != session.activePageIndex) {
                    viewModel.selectActiveDraftPage(targetPageIdx)
                }
                viewModel.imposeSignatureOnActiveDraftPage(
                    signatureBitmap = sigBmp,
                    normX = normX,
                    normY = normY,
                    scale = scale,
                    applyToAllPages = (targetPageIdx == -1)
                )
            }
        )
    }

    if (showSavePdfDialog) {
        SavePdfOptionsDialog(
            defaultPageSize = settings.defaultPageSize,
            defaultQuality = settings.defaultPdfQuality,
            defaultSearchable = settings.defaultSearchablePdf,
            folders = folders.map { it.id to it.name },
            onDismiss = { showSavePdfDialog = false },
            onConfirmSave = { title, category, folderId, pageSize, quality, isSearchable, watermark, password ->
                showSavePdfDialog = false
                viewModel.saveCurrentScanAsDocument(
                    title = title,
                    category = category,
                    folderId = folderId,
                    pageSize = pageSize,
                    quality = quality,
                    isSearchable = isSearchable,
                    watermarkText = watermark,
                    password = password,
                    onSaved = onSavedDocument
                )
            }
        )
    }
}

@Composable
private fun EnhanceControlsPanel(
    activeFilter: ScanFilter,
    brightness: Float,
    contrast: Float,
    showSliders: Boolean,
    canDeletePage: Boolean,
    onFilterSelected: (ScanFilter) -> Unit,
    onManualCropEdges: () -> Unit,
    onToggleSliders: () -> Unit,
    onBrightnessContrastChange: (Float, Float) -> Unit,
    onRotate90: () -> Unit,
    onDeskew: () -> Unit,
    onOpenSignDialog: () -> Unit,
    onDeletePage: () -> Unit
) {
    val haptics = LocalHaptics.current
    FrostedGlassCard(
        cornerRadius = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val orderedFilters = listOf(
                ScanFilter.CLEAR,
                ScanFilter.BW,
                ScanFilter.AUTO,
                ScanFilter.MAGIC_COLOR,
                ScanFilter.GRAYSCALE,
                ScanFilter.NO_SHADOW,
                ScanFilter.ORIGINAL
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                orderedFilters.forEach { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = {
                            haptics.tick()
                            onFilterSelected(filter)
                        },
                        label = { Text(filter.label) },
                        modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                    )
                }
            }

            if (showSliders) {
                var localBrightness by remember(activeFilter) { androidx.compose.runtime.mutableFloatStateOf(brightness) }
                var localContrast by remember(activeFilter) { androidx.compose.runtime.mutableFloatStateOf(contrast) }
                var lastBrightnessStep by remember { mutableIntStateOf(brightness.toInt() / 8) }
                var lastContrastStep by remember { mutableIntStateOf((contrast * 10f).toInt()) }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Brightness: ${localBrightness.toInt()}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        TextButton(
                            onClick = {
                                haptics.click()
                                localBrightness = 0f
                                localContrast = 1f
                                onBrightnessContrastChange(0f, 1f)
                            }
                        ) {
                            Text("Reset")
                        }
                    }
                    Slider(
                        value = localBrightness,
                        onValueChange = { newB ->
                            val step = newB.toInt() / 8
                            if (step != lastBrightnessStep) {
                                lastBrightnessStep = step
                                haptics.tick()
                            }
                            localBrightness = newB
                            onBrightnessContrastChange(newB, localContrast)
                        },
                        valueRange = -80f..80f,
                        modifier = Modifier.testTag("brightness_slider")
                    )
                    Text(
                        "Contrast: ${"%.2f".format(localContrast)}x",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = localContrast,
                        onValueChange = { newC ->
                            val step = (newC * 10f).toInt()
                            if (step != lastContrastStep) {
                                lastContrastStep = step
                                haptics.tick()
                            }
                            localContrast = newC
                            onBrightnessContrastChange(localBrightness, newC)
                        },
                        valueRange = 0.6f..1.8f,
                        modifier = Modifier.testTag("contrast_slider")
                    )
                }
            }

            // Aligned Action Buttons (Crop, Rotate, Sign, Tune, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassActionButton(
                    text = "Crop",
                    icon = Icons.Default.Crop,
                    height = 40.dp,
                    onClick = onManualCropEdges,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("manual_crop_edges_button")
                )
                GlassActionButton(
                    text = "Rotate",
                    icon = Icons.AutoMirrored.Filled.RotateRight,
                    height = 40.dp,
                    onClick = onRotate90,
                    modifier = Modifier.weight(1f)
                )
                GlassActionButton(
                    text = "Sign",
                    icon = androidx.compose.material.icons.Icons.Default.Edit,
                    height = 40.dp,
                    onClick = onOpenSignDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("enhance_sign_page_button")
                )
                GlassActionButton(
                    text = "Tune",
                    icon = Icons.Default.Tune,
                    height = 40.dp,
                    onClick = onToggleSliders,
                    modifier = Modifier.weight(1f)
                )
                if (canDeletePage) {
                    IconButton(
                        onClick = onDeletePage,
                        modifier = Modifier.size(38.dp)
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
}

@Composable
private fun OcrControlsPanel(
    modifier: Modifier = Modifier,
    selectedScript: OcrScript,
    ocrText: String,
    highlightedBlock: String?,
    showScriptMenu: Boolean,
    onToggleScriptMenu: () -> Unit,
    onSelectScript: (OcrScript) -> Unit,
    onRunOcrPage: () -> Unit,
    onRunOcrAllPages: () -> Unit,
    onOcrTextEdited: (String) -> Unit,
    onCopyText: () -> Unit,
    onShareText: () -> Unit,
    onExportTxt: () -> Unit
) {
    FrostedGlassCard(
        cornerRadius = 24.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Script selector + Scan OCR + All Pages with comfortable spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    GlassActionButton(
                        text = selectedScript.label.substringBefore(" ("),
                        height = 42.dp,
                        onClick = onToggleScriptMenu,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = showScriptMenu,
                        onDismissRequest = onToggleScriptMenu
                    ) {
                        OcrScript.entries.forEach { script ->
                            DropdownMenuItem(
                                text = { Text(script.label) },
                                onClick = { onSelectScript(script) }
                            )
                        }
                    }
                }

                GlassActionButton(
                    text = "Scan OCR",
                    icon = Icons.Default.TextFields,
                    isPrimary = true,
                    height = 42.dp,
                    onClick = onRunOcrPage,
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("run_ocr_button")
                )

                GlassActionButton(
                    text = "All Pages",
                    height = 42.dp,
                    onClick = onRunOcrAllPages,
                    modifier = Modifier.weight(1f)
                )
            }

            if (highlightedBlock != null) {
                Text(
                    text = "Copied: \"$highlightedBlock\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricTeal,
                    maxLines = 1
                )
            }

            OutlinedTextField(
                value = ocrText,
                onValueChange = onOcrTextEdited,
                label = { Text("Recognized OCR Text") },
                placeholder = { Text("Tap 'Scan OCR' above to extract text from this page...") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("ocr_editable_text_field")
            )

            // Bottom Row: Equal-width Copy, Share, Export TXT buttons with clean margins
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassActionButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    height = 42.dp,
                    onClick = onCopyText,
                    modifier = Modifier.weight(1f)
                )
                GlassActionButton(
                    text = "Share",
                    icon = Icons.Default.Share,
                    height = 42.dp,
                    onClick = onShareText,
                    modifier = Modifier.weight(1f)
                )
                GlassActionButton(
                    text = "Export TXT",
                    icon = Icons.Default.Download,
                    height = 42.dp,
                    onClick = onExportTxt,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SavePdfOptionsDialog(
    defaultPageSize: PdfPageSize,
    defaultQuality: PdfQuality,
    defaultSearchable: Boolean,
    folders: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirmSave: (
        title: String,
        category: String,
        folderId: String,
        pageSize: PdfPageSize,
        quality: PdfQuality,
        isSearchable: Boolean,
        watermark: String,
        password: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("Scan_${System.currentTimeMillis() / 1000}") }
    var selectedCategory by remember { mutableStateOf(DocumentCategory.SCAN.label) }
    var selectedFolderId by remember { mutableStateOf("") }
    var pageSize by remember { mutableStateOf(defaultPageSize) }
    var quality by remember { mutableStateOf(defaultQuality) }
    var isSearchable by remember { mutableStateOf(defaultSearchable) }
    var watermark by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Searchable PDF") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DocumentCategory.entries.filter { it != DocumentCategory.ALL }.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.label,
                            onClick = { selectedCategory = cat.label },
                            label = { Text(cat.label) }
                        )
                    }
                }

                if (folders.isNotEmpty()) {
                    Text("Folder", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFolderId.isEmpty(),
                            onClick = { selectedFolderId = "" },
                            label = { Text("Root") }
                        )
                        folders.forEach { (id, name) ->
                            FilterChip(
                                selected = selectedFolderId == id,
                                onClick = { selectedFolderId = id },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                Text("Page Size", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfPageSize.entries.forEach { ps ->
                        FilterChip(
                            selected = pageSize == ps,
                            onClick = { pageSize = ps },
                            label = { Text(ps.name) }
                        )
                    }
                }

                Text("Compression / Quality", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PdfQuality.entries.forEach { q ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { quality = q },
                            label = { Text(q.name) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSearchable,
                        onCheckedChange = { isSearchable = it }
                    )
                    Text("Embed Searchable OCR Text Layer in PDF")
                }

                OutlinedTextField(
                    value = watermark,
                    onValueChange = { watermark = it },
                    label = { Text("Optional Watermark Text (e.g. CONFIDENTIAL)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Optional Password Protection") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSave(
                        title,
                        selectedCategory,
                        selectedFolderId,
                        pageSize,
                        quality,
                        isSearchable,
                        watermark,
                        password
                    )
                },
                modifier = Modifier.testTag("confirm_save_pdf_dialog_button")
            ) {
                Text("Generate PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
