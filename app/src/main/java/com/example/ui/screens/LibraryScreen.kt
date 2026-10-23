package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.FolderEntity
import com.example.data.local.ScannedDocumentEntity
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DocumentCategory
import com.example.domain.model.SortOption
import com.example.ui.theme.FrostedGlassCard
import com.example.ui.theme.GlassActionButton
import com.example.ui.theme.GlassScreenBackground
import com.example.ui.theme.GlassThemeTogglePill
import com.example.ui.theme.LocalIsDarkGlass
import com.example.ui.theme.RoyalBlue
import com.example.ui.viewmodel.DocuScanViewModel
import com.example.ui.viewmodel.ScannerCaptureMode
import com.example.utils.LocalHaptics
import com.example.utils.ShareExportHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: DocuScanViewModel,
    onLaunchScanner: (ScannerCaptureMode) -> Unit,
    onPickGalleryImages: () -> Unit,
    onOpenDocument: (String) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHaptics.current
    val isDark = LocalIsDarkGlass.current
    val documents by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val selectedDocIds by viewModel.selectedDocIds.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }

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
                // Multi-Select Action Bar when items are selected
                AnimatedVisibility(visible = selectedDocIds.isNotEmpty()) {
                    FrostedGlassCard(
                        cornerRadius = 18.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { viewModel.clearDocSelection() },
                                    modifier = Modifier.testTag("clear_selection_button")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear selection")
                                }
                                Text(
                                    text = "${selectedDocIds.size} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (selectedDocIds.size >= 2) {
                                    IconButton(
                                        onClick = { showMergeDialog = true },
                                        modifier = Modifier.testTag("batch_merge_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.MergeType, contentDescription = "Merge PDFs")
                                    }
                                }
                                IconButton(
                                    onClick = { showMoveFolderDialog = true },
                                    modifier = Modifier.testTag("batch_move_folder_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to folder")
                                }
                                IconButton(
                                    onClick = {
                                        val files = documents.filter { it.id in selectedDocIds }
                                            .map { File(it.pdfFilePath) }
                                        ShareExportHelper.shareMultipleFiles(
                                            context,
                                            files,
                                            "application/pdf",
                                            "Share PDFs"
                                        )
                                    },
                                    modifier = Modifier.testTag("batch_share_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share selected")
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSelectedDocuments() },
                                    modifier = Modifier.testTag("batch_delete_button")
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete selected",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                // Clean iOS Glass Top Header with 1-Tap Dark/Light Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Doc Scan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GlassThemeTogglePill(
                            isDark = isDark,
                            onToggle = {
                                viewModel.setThemeMode(
                                    if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                                )
                            },
                            modifier = Modifier.testTag("header_theme_toggle")
                        )
                        IconButton(
                            onClick = {
                                haptics.tick()
                                viewModel.toggleFavoritesOnly()
                            },
                            modifier = Modifier.testTag("favorites_filter_button")
                        ) {
                            Icon(
                                imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Favorites",
                                tint = if (onlyFavorites) Color(0xFFFF9F0A) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                haptics.tick()
                                viewModel.toggleGridView()
                            },
                            modifier = Modifier.testTag("grid_list_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (settings.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle grid or list view"
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Adaptive 2x2 iOS Glass Action Panel (Strictly aligned on all screen sizes)
                FrostedGlassCard(
                    cornerRadius = 22.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassActionButton(
                                text = "Scan",
                                icon = Icons.Default.CameraAlt,
                                isPrimary = true,
                                onClick = { onLaunchScanner(ScannerCaptureMode.STANDARD_BATCH) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_camera_scan_button")
                            )
                            GlassActionButton(
                                text = "Import",
                                icon = Icons.Default.PhotoLibrary,
                                isPrimary = false,
                                onClick = onPickGalleryImages,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("hero_import_images_button")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassActionButton(
                                text = "ID Card",
                                icon = Icons.Default.Badge,
                                isPrimary = false,
                                onClick = { onLaunchScanner(ScannerCaptureMode.ID_CARD) },
                                modifier = Modifier.weight(1f)
                            )
                            GlassActionButton(
                                text = "QR Code",
                                icon = Icons.Default.QrCodeScanner,
                                isPrimary = false,
                                onClick = { onLaunchScanner(ScannerCaptureMode.QR_BARCODE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Full-Width Single-Line iOS Glass Search Bar
                val searchBg = if (isDark) Color(0xFF1A2942).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.82f)
                val searchBorder = if (isDark) Color.White.copy(alpha = 0.36f) else Color.White
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = "Search PDFs, OCR text & tags...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = searchBg,
                        unfocusedContainerColor = searchBg,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = searchBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("library_search_input")
                )

                // Folders & Categories Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFolderId == null && selectedCategory == DocumentCategory.ALL,
                        onClick = {
                            viewModel.setFolderFilter(null)
                            viewModel.setCategory(DocumentCategory.ALL)
                        },
                        label = { Text("All") }
                    )
                    folders.forEach { folder ->
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = {
                                viewModel.setFolderFilter(
                                    if (selectedFolderId == folder.id) null else folder.id
                                )
                            },
                            label = { Text(folder.name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = runCatching { Color(android.graphics.Color.parseColor(folder.colorHex)) }
                                        .getOrDefault(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        )
                    }
                    DocumentCategory.entries.filter { it != DocumentCategory.ALL }.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category.label) }
                        )
                    }
                    AssistChip(
                        onClick = { showNewFolderDialog = true },
                        label = { Text("+ Folder") },
                        leadingIcon = {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.testTag("new_folder_chip")
                    )
                }

                // Document List / Grid
                if (documents.isEmpty()) {
                    EmptyLibraryPlaceholder(
                        onStartScan = { onLaunchScanner(ScannerCaptureMode.STANDARD_BATCH) },
                        onImportGallery = onPickGalleryImages
                    )
                } else if (settings.isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 128.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            DocumentGridCard(
                                doc = doc,
                                isSelected = doc.id in selectedDocIds,
                                onClick = {
                                    haptics.click()
                                    if (selectedDocIds.isNotEmpty()) {
                                        viewModel.toggleDocSelection(doc.id)
                                    } else {
                                        onOpenDocument(doc.id)
                                    }
                                },
                                onLongClick = {
                                    haptics.heavy()
                                    viewModel.toggleDocSelection(doc.id)
                                },
                                onToggleFavorite = {
                                    haptics.tick()
                                    viewModel.toggleFavorite(doc)
                                },
                                onRename = { newTitle -> viewModel.renameDocument(doc, newTitle) },
                                onDelete = { viewModel.deleteDocument(doc.id) },
                                onShare = {
                                    ShareExportHelper.sharePdfFile(
                                        context = context,
                                        pdfFile = File(doc.pdfFilePath),
                                        title = doc.title
                                    )
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 128.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(documents, key = { it.id }) { doc ->
                            DocumentListRowCard(
                                doc = doc,
                                isSelected = doc.id in selectedDocIds,
                                onClick = {
                                    haptics.click()
                                    if (selectedDocIds.isNotEmpty()) {
                                        viewModel.toggleDocSelection(doc.id)
                                    } else {
                                        onOpenDocument(doc.id)
                                    }
                                },
                                onLongClick = {
                                    haptics.heavy()
                                    viewModel.toggleDocSelection(doc.id)
                                },
                                onToggleFavorite = {
                                    haptics.tick()
                                    viewModel.toggleFavorite(doc)
                                },
                                onRename = { newTitle -> viewModel.renameDocument(doc, newTitle) },
                                onDelete = { viewModel.deleteDocument(doc.id) },
                                onShare = {
                                    ShareExportHelper.sharePdfFile(
                                        context = context,
                                        pdfFile = File(doc.pdfFilePath),
                                        title = doc.title
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3D Glowing RGB Floating Camera Button above the LiquidGlassBottomBar
            com.example.ui.theme.Glowing3dRgbCameraOrb(
                onClick = {
                    onLaunchScanner(ScannerCaptureMode.STANDARD_BATCH)
                },
                size = 60.dp,
                iconSize = 27.dp,
                icon = Icons.Default.CameraAlt,
                contentDescription = "Scan Document",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 84.dp)
                    .testTag("fab_scan_document")
            )
        }
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { name, colorHex ->
                showNewFolderDialog = false
                viewModel.createNewFolder(name, colorHex)
            }
        )
    }

    if (showMoveFolderDialog) {
        MoveToFolderDialog(
            folders = folders,
            onDismiss = { showMoveFolderDialog = false },
            onSelectFolder = { folderId ->
                showMoveFolderDialog = false
                viewModel.moveSelectedDocsToFolder(folderId)
            }
        )
    }

    if (showMergeDialog) {
        var mergedName by remember { mutableStateOf("Merged_${System.currentTimeMillis() / 1000}") }
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge ${selectedDocIds.size} PDFs") },
            text = {
                OutlinedTextField(
                    value = mergedName,
                    onValueChange = { mergedName = it },
                    label = { Text("PDF Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMergeDialog = false
                        viewModel.mergeDocumentsByIds(
                            docIds = selectedDocIds.toList(),
                            mergedTitle = mergedName,
                            onComplete = onOpenDocument
                        )
                    }
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentGridCard(
    doc: ScannedDocumentEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    FrostedGlassCard(
        cornerRadius = 20.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("doc_card_${doc.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.White)
            ) {
                val context = LocalContext.current
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                )
                if (doc.thumbnailPath.isNotBlank()) {
                    val imageRequest = remember(doc.thumbnailPath) {
                        ImageRequest.Builder(context)
                            .data(File(doc.thumbnailPath))
                            .size(280)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = doc.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Page count pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xCC0F172A),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${doc.pageCount}p • ${ShareExportHelper.formatFileSize(doc.fileSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (doc.isPasswordProtected) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC0F172A),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = doc.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (doc.isFavorite) "Unfavorite" else "Favorite") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onToggleFavorite()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
                Text(
                    text = dateFormat.format(Date(doc.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }

    if (showRenameDialog) {
        RenameDocumentDialog(
            currentTitle = doc.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                onRename(newName)
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentListRowCard(
    doc: ScannedDocumentEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    FrostedGlassCard(
        cornerRadius = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp, 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                val context = LocalContext.current
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                if (doc.thumbnailPath.isNotBlank()) {
                    val imageRequest = remember(doc.thumbnailPath) {
                        ImageRequest.Builder(context)
                            .data(File(doc.thumbnailPath))
                            .size(160)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = doc.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.pageCount}p • ${ShareExportHelper.formatFileSize(doc.fileSizeBytes)} • ${dateFormat.format(Date(doc.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (doc.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (doc.isFavorite) Color(0xFFFF9F0A) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showRenameDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        RenameDocumentDialog(
            currentTitle = doc.title,
            onDismiss = { showRenameDialog = false },
            onConfirm = { newName ->
                showRenameDialog = false
                onRename(newName)
            }
        )
    }
}

@Composable
private fun EmptyLibraryPlaceholder(
    onStartScan: () -> Unit,
    onImportGallery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FrostedGlassCard(
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "No PDFs Yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassActionButton(
                        text = "Scan",
                        icon = Icons.Default.CameraAlt,
                        isPrimary = true,
                        onClick = onStartScan,
                        modifier = Modifier.weight(1f)
                    )
                    GlassActionButton(
                        text = "Import",
                        icon = Icons.Default.PhotoLibrary,
                        isPrimary = false,
                        onClick = onImportGallery,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDocumentDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(title) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NewFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName, "#007AFF") },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MoveToFolderDialog(
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to Folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                folders.forEach { folder ->
                    OutlinedButton(
                        onClick = { onSelectFolder(folder.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(folder.name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
