package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.utils.ImageProcessor
import com.example.utils.ShareExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GeminiAiStudioScreen(
    viewModel: DocuScanViewModel
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val documents by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()

    var selectedDocIndex by remember { mutableIntStateOf(0) }
    val activeDoc = documents.getOrNull(selectedDocIndex)

    var customPrompt by remember {
        mutableStateOf("Analyze this document thoroughly: summarize key clauses, verify any numbers or totals, and highlight action items.")
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageProcessor.loadBitmapFromUri(context, uri, maxDim = 1800)
                if (bmp != null) {
                    viewModel.setAiUploadedBitmap(bmp)
                    viewModel.showStatus("Loaded photo for Gemini Vision (gemini-4-argon)")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gemini Document AI Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vision (gemini-4-argon) • High Thinking • Fast Lite (gemini-3.1-flash-lite)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document & Image Source Selector Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Select Source Document or Upload Photo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (documents.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            documents.take(6).forEachIndexed { idx, doc ->
                                FilterChip(
                                    selected = selectedDocIndex == idx,
                                    onClick = { selectedDocIndex = idx },
                                    label = { Text(doc.title, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_upload_image_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo for Vision")
                        }

                        if (activeDoc != null) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val bmp = ImageProcessor.loadBitmapFromPath(activeDoc.thumbnailPath)
                                        if (bmp != null) {
                                            viewModel.setAiUploadedBitmap(bmp)
                                            viewModel.showStatus("Loaded page image from \"${activeDoc.title}\"")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ImageSearch, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use Doc Thumbnail")
                            }
                        }
                    }

                    if (aiState.uploadedBitmap != null) {
                        Image(
                            bitmap = aiState.uploadedBitmap!!.asImageBitmap(),
                            contentDescription = "Uploaded image for Gemini Vision",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            // Custom Prompt Input
            OutlinedTextField(
                value = customPrompt,
                onValueChange = { customPrompt = it },
                label = { Text("Prompt / Task Instruction") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_studio_prompt_input"),
                minLines = 2,
                maxLines = 4
            )

            // 4 Model Mode Buttons
            Text(
                text = "2. Choose Gemini Model Engine",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Mode A: Analyze Image with gemini-4-argon
                Button(
                    onClick = {
                        scope.launch {
                            val bmp = aiState.uploadedBitmap
                                ?: activeDoc?.thumbnailPath?.let { ImageProcessor.loadBitmapFromPath(it) }
                            if (bmp != null) {
                                viewModel.runImageAnalysisWithArgon(bmp, customPrompt)
                            } else {
                                viewModel.showStatus("Please upload a photo or select a document with a page image first.")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_run_argon_vision_button")
                ) {
                    Icon(Icons.Default.ImageSearch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyze Image & Layout (gemini-4-argon)")
                }

                // Mode B: High Thinking Mode with gemini-4-argon + ThinkingLevel.HIGH
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            val docText = activeDoc?.ocrText?.ifBlank { activeDoc.title } ?: customPrompt
                            val bmp = aiState.uploadedBitmap
                                ?: activeDoc?.thumbnailPath?.let { ImageProcessor.loadBitmapFromPath(it) }
                            viewModel.runHighThinkingQuery(
                                documentText = docText,
                                complexQuery = customPrompt,
                                optionalBitmap = bmp,
                                docToUpdate = activeDoc
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_run_high_thinking_button")
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Deep Reasoning Mode (gemini-4-argon • HIGH Thinking)")
                }

                // Mode C: General Document Task with gemini-3.8-flash
                FilledTonalButton(
                    onClick = {
                        val docText = activeDoc?.ocrText?.ifBlank { activeDoc.title } ?: customPrompt
                        viewModel.runGeneralDocumentTask(
                            doc = activeDoc,
                            text = docText,
                            instruction = customPrompt
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_run_general_flash_button")
                ) {
                    Icon(Icons.Default.Summarize, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executive Summary & Rewrite (gemini-3.8-flash)")
                }

                // Mode D: Fast Low-Latency with gemini-3.1-flash-lite
                FilledTonalButton(
                    onClick = {
                        val docText = activeDoc?.ocrText?.ifBlank { activeDoc.title } ?: customPrompt
                        viewModel.runFastLowLatencyCleanup(
                            text = docText,
                            taskPrompt = "Clean up OCR artifacts, organize into clear sections, and suggest 4 tags."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_run_fast_lite_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Instant OCR Cleanup & Tags (gemini-3.1-flash-lite)")
                }
            }

            if (aiState.isAnalyzing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                        Column {
                            Text(
                                text = "Running Gemini Analysis...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = aiState.activeModelBadge,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            if (aiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = aiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            if (aiState.outputText.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        text = aiState.activeModelBadge.ifBlank { "Gemini Output" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(aiState.outputText))
                                        viewModel.showStatus("Copied AI response")
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy AI Output")
                                }
                                IconButton(
                                    onClick = {
                                        ShareExportHelper.sharePlainText(
                                            context = context,
                                            title = "DocuScan Gemini Analysis",
                                            text = aiState.outputText
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share AI Output")
                                }
                            }
                        }

                        Text(
                            text = aiState.outputText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
