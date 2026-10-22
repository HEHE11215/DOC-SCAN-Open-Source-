package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.NormalizedQuad
import com.example.domain.model.ScanFilter
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.ui.viewmodel.ScannerCaptureMode
import com.example.utils.ImageProcessor
import kotlin.math.hypot

@Composable
fun CameraAndEdgeScreen(
    viewModel: DocuScanViewModel,
    onBack: () -> Unit,
    onProceedToEnhance: () -> Unit
) {
    val session by viewModel.scanSession.collectAsState()

    BackHandler {
        if (session.isCroppingStage) {
            viewModel.cancelCropStage()
        } else {
            onBack()
        }
    }

    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onMultipleGalleryUrisSelected(
                uris = uris,
                onReadyForCrop = {},
                onReadyForEnhance = onProceedToEnhance
            )
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !session.isCroppingStage) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    GlassScreenBackground {
        if (session.isCroppingStage && session.pendingRawBitmap != null) {
            PerspectiveCropStudio(
                bitmap = session.pendingRawBitmap!!,
                quad = session.detectedQuad,
                selectedFilter = session.selectedCropFilter,
                onFilterChanged = { viewModel.setCropStageFilter(it) },
                onQuadChanged = { viewModel.updateQuad(it) },
                onAutoDetectAgain = {
                    viewModel.onRawBitmapCaptured(session.pendingRawBitmap!!)
                },
                onFullPageQuad = { viewModel.resetQuadToFullPage() },
                onInsetQuad = { viewModel.setInsetDocumentQuad() },
                onCancel = {
                    if (session.isEditingExistingPageCrop) {
                        viewModel.cancelCropStage()
                        onProceedToEnhance()
                    } else {
                        viewModel.cancelCropStage()
                    }
                },
                onConfirmWarp = {
                    viewModel.confirmPerspectiveCrop(onNavigateToEnhance = onProceedToEnhance)
                }
            )
        } else {
            CameraCaptureView(
                hasCameraPermission = hasCameraPermission,
                captureMode = session.captureMode,
                selectedFilter = session.selectedCropFilter,
                onFilterSelected = { viewModel.setCropStageFilter(it) },
                capturedPageCount = session.pages.size,
                idCardFrontCaptured = session.idCardFrontBitmap != null,
                scannedBarcodes = session.scannedBarcodes,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onModeSelected = { viewModel.setScannerCaptureMode(it) },
                onBitmapCaptured = { bmp -> viewModel.onRawBitmapCaptured(bmp) },
                onOpenGallery = {
                    galleryPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onUseSampleDemoPage = {
                    viewModel.onRawBitmapCaptured(ImageProcessor.generateSampleInvoiceBitmap())
                },
                onFinishBatch = onProceedToEnhance,
                onBack = onBack
            )
        }

        if (session.isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                FrostedGlassCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = session.processingMessage.ifBlank { "Processing document..." },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraCaptureView(
    hasCameraPermission: Boolean,
    captureMode: ScannerCaptureMode,
    selectedFilter: ScanFilter,
    onFilterSelected: (ScanFilter) -> Unit,
    capturedPageCount: Int,
    idCardFrontCaptured: Boolean,
    scannedBarcodes: List<com.example.domain.model.ScannedBarcodeItem>,
    onRequestCameraPermission: () -> Unit,
    onModeSelected: (ScannerCaptureMode) -> Unit,
    onBitmapCaptured: (Bitmap) -> Unit,
    onOpenGallery: () -> Unit,
    onUseSampleDemoPage: () -> Unit,
    onFinishBatch: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_AUTO) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }
    var focusTapOffset by remember { mutableStateOf<Offset?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(flashMode)
            .build()
    }

    LaunchedEffect(flashMode) {
        imageCapture.flashMode = flashMode
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Glass Top Bar
        FrostedGlassCard(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("camera_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = captureMode.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (captureMode == ScannerCaptureMode.ID_CARD) {
                            if (!idCardFrontCaptured) "Step 1/2: Scan FRONT Side" else "Step 2/2: Scan BACK Side"
                        } else {
                            "Default Filter: ${selectedFilter.label}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier.testTag("camera_flash_toggle")
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    }
                    Icon(icon, contentDescription = "Toggle Flash", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Viewfinder Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
        ) {
            if (hasCameraPermission) {
                val previewView = remember { PreviewView(context) }
                LaunchedEffect(previewView) {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    providerFuture.addListener({
                        runCatching {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            provider.unbindAll()
                            cameraRef = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }
                    }, ContextCompat.getMainExecutor(context))
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                focusTapOffset = offset
                                val cam = cameraRef ?: return@detectTapGestures
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(offset.x, offset.y)
                                val action = FocusMeteringAction.Builder(point).build()
                                cam.cameraControl.startFocusAndMetering(action)
                            }
                        }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera Permission Needed",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant camera access or use Gallery / Sample Page below to test B&W scanning and manual edge crop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(onClick = onRequestCameraPermission) {
                        Text("Allow Camera")
                    }
                }
            }

            // Document guide frame overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padX = size.width * 0.09f
                val padY = size.height * 0.10f
                val frameW = size.width - padX * 2f
                val frameH = size.height - padY * 2f
                drawRoundRect(
                    color = LuminousCyan.copy(alpha = 0.75f),
                    topLeft = Offset(padX, padY),
                    size = androidx.compose.ui.geometry.Size(frameW, frameH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                    style = Stroke(width = 3.5f)
                )

                focusTapOffset?.let { tap ->
                    drawCircle(
                        color = LuminousCyan,
                        radius = 32f,
                        center = tap,
                        style = Stroke(width = 3f)
                    )
                }
            }

            if (captureMode == ScannerCaptureMode.QR_BARCODE && scannedBarcodes.isNotEmpty()) {
                FrostedGlassCard(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    cornerRadius = 18.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Detected QR / Barcodes (${scannedBarcodes.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        scannedBarcodes.forEach { code ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${code.formatLabel} • ${code.typeLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = code.displayValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Barcode", code.rawValue))
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                                if (code.rawValue.startsWith("http", ignoreCase = true)) {
                                    IconButton(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(code.rawValue)))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open URL")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Scan Filter Strip (B&W Sharp, Auto Enhance, Magic Color, Grayscale, Shadow Removal, Original)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }

        // Capture Mode Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScannerCaptureMode.entries.forEach { mode ->
                FilterChip(
                    selected = captureMode == mode,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.label) }
                )
            }
        }

        // Bottom Glass Shutter & Gallery Controls (3 balanced columns)
        FrostedGlassCard(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassActionButton(
                    text = "Gallery",
                    icon = Icons.Default.PhotoLibrary,
                    isPrimary = false,
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("camera_gallery_button")
                )

                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue)
                        .clickable {
                            if (hasCameraPermission) {
                                imageCapture.takePicture(
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val bmp = image.toBitmap()
                                            val rot = image.imageInfo.rotationDegrees
                                            image.close()
                                            val oriented = ImageProcessor.rotateBitmap(bmp, rot)
                                            onBitmapCaptured(oriented)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            onUseSampleDemoPage()
                                        }
                                    }
                                )
                            } else {
                                onUseSampleDemoPage()
                            }
                        }
                        .testTag("camera_shutter_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Capture Scan",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (capturedPageCount > 0) {
                    GlassActionButton(
                        text = "Next ($capturedPageCount)",
                        icon = Icons.Default.Check,
                        isPrimary = true,
                        onClick = onFinishBatch,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_finish_batch_button")
                    )
                } else {
                    GlassActionButton(
                        text = "Sample",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        isPrimary = false,
                        onClick = onUseSampleDemoPage,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("camera_sample_page_button")
                    )
                }
            }
        }
    }
}

/**
 * Interactive 4-Corner & 4-Edge Perspective Crop Studio with smooth, continuous drag gestures
 * and direct B&W / Magic Color filter selection.
 */
@Composable
private fun PerspectiveCropStudio(
    bitmap: Bitmap,
    quad: NormalizedQuad,
    selectedFilter: ScanFilter,
    onFilterChanged: (ScanFilter) -> Unit,
    onQuadChanged: (NormalizedQuad) -> Unit,
    onAutoDetectAgain: () -> Unit,
    onFullPageQuad: () -> Unit,
    onInsetQuad: () -> Unit,
    onCancel: () -> Unit,
    onConfirmWarp: () -> Unit
) {
    // CRITICAL: rememberUpdatedState prevents pointerInput from restarting & cancelling mid-drag!
    val currentQuad by rememberUpdatedState(quad)
    val currentOnQuadChanged by rememberUpdatedState(onQuadChanged)
    var activeHandleIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Glass Top Header
        FrostedGlassCard(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Crop & Filter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onFullPageQuad) {
                    Icon(
                        Icons.Default.CropFree,
                        contentDescription = "Full Page",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Interactive Image + Smooth Draggable 4-Corner & 4-Edge Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured page for edge detection",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val q = currentQuad
                                val tl = Offset(q.topLeft.x * w, q.topLeft.y * h)
                                val tr = Offset(q.topRight.x * w, q.topRight.y * h)
                                val br = Offset(q.bottomRight.x * w, q.bottomRight.y * h)
                                val bl = Offset(q.bottomLeft.x * w, q.bottomLeft.y * h)
                                val handles = listOf(
                                    tl,
                                    tr,
                                    br,
                                    bl,
                                    Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f),
                                    Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f),
                                    Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f),
                                    Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
                                )
                                activeHandleIndex = handles.indices.minByOrNull { idx ->
                                    hypot(
                                        (handles[idx].x - offset.x).toDouble(),
                                        (handles[idx].y - offset.y).toDouble()
                                    )
                                } ?: -1
                            },
                            onDragEnd = { activeHandleIndex = -1 },
                            onDragCancel = { activeHandleIndex = -1 },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = size.width.toFloat().coerceAtLeast(1f)
                                val h = size.height.toFloat().coerceAtLeast(1f)
                                val dx = dragAmount.x / w
                                val dy = dragAmount.y / h
                                val q = currentQuad

                                val updated = when (activeHandleIndex) {
                                    0 -> q.copy(
                                        topLeft = PointF(
                                            (q.topLeft.x + dx).coerceIn(0.01f, 0.92f),
                                            (q.topLeft.y + dy).coerceIn(0.01f, 0.92f)
                                        )
                                    )
                                    1 -> q.copy(
                                        topRight = PointF(
                                            (q.topRight.x + dx).coerceIn(0.08f, 0.99f),
                                            (q.topRight.y + dy).coerceIn(0.01f, 0.92f)
                                        )
                                    )
                                    2 -> q.copy(
                                        bottomRight = PointF(
                                            (q.bottomRight.x + dx).coerceIn(0.08f, 0.99f),
                                            (q.bottomRight.y + dy).coerceIn(0.08f, 0.99f)
                                        )
                                    )
                                    3 -> q.copy(
                                        bottomLeft = PointF(
                                            (q.bottomLeft.x + dx).coerceIn(0.01f, 0.92f),
                                            (q.bottomLeft.y + dy).coerceIn(0.08f, 0.99f)
                                        )
                                    )
                                    4 -> q.copy(
                                        topLeft = PointF(q.topLeft.x, (q.topLeft.y + dy).coerceIn(0.01f, 0.90f)),
                                        topRight = PointF(q.topRight.x, (q.topRight.y + dy).coerceIn(0.01f, 0.90f))
                                    )
                                    5 -> q.copy(
                                        topRight = PointF((q.topRight.x + dx).coerceIn(0.10f, 0.99f), q.topRight.y),
                                        bottomRight = PointF((q.bottomRight.x + dx).coerceIn(0.10f, 0.99f), q.bottomRight.y)
                                    )
                                    6 -> q.copy(
                                        bottomLeft = PointF(q.bottomLeft.x, (q.bottomLeft.y + dy).coerceIn(0.10f, 0.99f)),
                                        bottomRight = PointF(q.bottomRight.x, (q.bottomRight.y + dy).coerceIn(0.10f, 0.99f))
                                    )
                                    7 -> q.copy(
                                        topLeft = PointF((q.topLeft.x + dx).coerceIn(0.01f, 0.90f), q.topLeft.y),
                                        bottomLeft = PointF((q.bottomLeft.x + dx).coerceIn(0.01f, 0.90f), q.bottomLeft.y)
                                    )
                                    else -> q
                                }
                                currentOnQuadChanged(updated)
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val tl = Offset(quad.topLeft.x * w, quad.topLeft.y * h)
                val tr = Offset(quad.topRight.x * w, quad.topRight.y * h)
                val br = Offset(quad.bottomRight.x * w, quad.bottomRight.y * h)
                val bl = Offset(quad.bottomLeft.x * w, quad.bottomLeft.y * h)

                val polyPath = Path().apply {
                    moveTo(tl.x, tl.y)
                    lineTo(tr.x, tr.y)
                    lineTo(br.x, br.y)
                    lineTo(bl.x, bl.y)
                    close()
                }

                drawPath(
                    path = polyPath,
                    color = LuminousCyan.copy(alpha = 0.18f)
                )
                drawPath(
                    path = polyPath,
                    color = LuminousCyan,
                    style = Stroke(width = 5f)
                )

                listOf(tl, tr, br, bl).forEachIndexed { idx, pt ->
                    val isDragging = activeHandleIndex == idx
                    drawCircle(
                        color = Color.White,
                        radius = if (isDragging) 36f else 28f,
                        center = pt
                    )
                    drawCircle(
                        color = RoyalBlue,
                        radius = if (isDragging) 26f else 20f,
                        center = pt
                    )
                }

                val midPoints = listOf(
                    Offset((tl.x + tr.x) / 2f, (tl.y + tr.y) / 2f),
                    Offset((tr.x + br.x) / 2f, (tr.y + br.y) / 2f),
                    Offset((bl.x + br.x) / 2f, (bl.y + br.y) / 2f),
                    Offset((tl.x + bl.x) / 2f, (tl.y + bl.y) / 2f)
                )
                midPoints.forEachIndexed { idx, pt ->
                    val isDragging = activeHandleIndex == (idx + 4)
                    drawCircle(
                        color = Color.White,
                        radius = if (isDragging) 24f else 18f,
                        center = pt
                    )
                    drawCircle(
                        color = LuminousCyan,
                        radius = if (isDragging) 16f else 12f,
                        center = pt
                    )
                }
            }
        }

        // Bottom Frosted Glass Panel with 3 Aligned Edge Buttons + Filter Chips + Confirm Button
        FrostedGlassCard(
            cornerRadius = 24.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassActionButton(
                        text = "Auto",
                        icon = Icons.Default.AutoFixHigh,
                        height = 42.dp,
                        onClick = onAutoDetectAgain,
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionButton(
                        text = "Center",
                        icon = Icons.Default.FilterCenterFocus,
                        height = 42.dp,
                        onClick = onInsetQuad,
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionButton(
                        text = "Full",
                        icon = Icons.Default.CropFree,
                        height = 42.dp,
                        onClick = onFullPageQuad,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChanged(filter) },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("crop_filter_${filter.name.lowercase()}")
                        )
                    }
                }

                GlassActionButton(
                    text = "Apply (${selectedFilter.label})",
                    icon = Icons.Default.Check,
                    isPrimary = true,
                    height = 48.dp,
                    onClick = onConfirmWarp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_perspective_crop_button")
                )
            }
        }
    }
}
