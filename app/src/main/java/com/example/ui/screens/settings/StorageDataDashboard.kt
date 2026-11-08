package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.util.MediaCategory
import com.example.util.MediaDirectoryManager
import com.example.util.MediaFileInfo
import com.example.util.StorageSummary
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDataDashboardDialog(
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<StorageSummary?>(null) }
    var mediaFiles by remember { mutableStateOf<List<MediaFileInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedCategoryTab by remember { mutableStateOf<MediaCategory?>(null) } // null = All
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedFileIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var activePreviewFile by remember { mutableStateOf<MediaFileInfo?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearCategoryConfirmDialog by remember { mutableStateOf<MediaCategory?>(null) }

    fun refreshData() {
        scope.launch {
            isLoading = true
            summary = MediaDirectoryManager.getStorageSummary(context)
            mediaFiles = MediaDirectoryManager.getAllStoredMediaFiles(context, selectedCategoryTab)
            selectedFileIds = emptySet()
            isLoading = false
        }
    }

    LaunchedEffect(selectedCategoryTab) {
        refreshData()
    }

    val selectedFilesList = remember(selectedFileIds, mediaFiles) {
        mediaFiles.filter { it.id in selectedFileIds }
    }

    val selectedFilesTotalBytes = remember(selectedFilesList) {
        selectedFilesList.sumOf { it.sizeBytes }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Storage & Data",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Linko Media Folder Manager",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = { refreshData() },
                            modifier = Modifier.testTag("refresh_storage_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Storage",
                                tint = primaryColor
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_storage_dashboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = primaryColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Calculating Storage Footprint...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        // Storage Gauge Card
                        item {
                            StorageGaugeCard(
                                summary = summary ?: StorageSummary(0, "0 B", 0, "0 B", 0, 0, "0 B", 0, 0, "0 B", 0),
                                primaryColor = primaryColor,
                                onClearCategory = { cat -> showClearCategoryConfirmDialog = cat }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Category Selection Filter Chips
                        item {
                            Text(
                                text = "Local Media Folders (Linko/Media)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedCategoryTab == null,
                                    onClick = { selectedCategoryTab = null },
                                    label = { Text("All (${summary?.formattedTotalSize ?: "0 B"})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("tab_all_media")
                                )

                                FilterChip(
                                    selected = selectedCategoryTab == MediaCategory.PHOTOS,
                                    onClick = { selectedCategoryTab = MediaCategory.PHOTOS },
                                    label = { Text("Photos (${summary?.photosCount ?: 0})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("tab_photos_media")
                                )

                                FilterChip(
                                    selected = selectedCategoryTab == MediaCategory.VIDEOS,
                                    onClick = { selectedCategoryTab = MediaCategory.VIDEOS },
                                    label = { Text("Videos (${summary?.videosCount ?: 0})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("tab_videos_media")
                                )

                                FilterChip(
                                    selected = selectedCategoryTab == MediaCategory.DOCUMENTS,
                                    onClick = { selectedCategoryTab = MediaCategory.DOCUMENTS },
                                    label = { Text("Docs (${summary?.documentsCount ?: 0})") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = primaryColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("tab_docs_media")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Management Toolbar: Multi-select toggle & Select All
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isMultiSelectMode) "${selectedFileIds.size} Selected (${MediaDirectoryManager.formatFileSize(selectedFilesTotalBytes)})" else "Files (${mediaFiles.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMultiSelectMode) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isMultiSelectMode) {
                                        TextButton(
                                            onClick = {
                                                if (selectedFileIds.size == mediaFiles.size) {
                                                    selectedFileIds = emptySet()
                                                } else {
                                                    selectedFileIds = mediaFiles.map { it.id }.toSet()
                                                }
                                            },
                                            modifier = Modifier.testTag("select_all_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SelectAll,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (selectedFileIds.size == mediaFiles.size) "Deselect All" else "Select All", fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            isMultiSelectMode = !isMultiSelectMode
                                            if (!isMultiSelectMode) selectedFileIds = emptySet()
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("toggle_multi_select_button")
                                    ) {
                                        Text(if (isMultiSelectMode) "Done Selecting" else "Select & Manage", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Media Files Display
                        if (mediaFiles.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "0 Files. No media shared yet.",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "0 KB",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 100.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 800.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(mediaFiles, key = { it.id }) { item ->
                                        val isSelected = item.id in selectedFileIds
                                        MediaTileItem(
                                            item = item,
                                            primaryColor = primaryColor,
                                            isMultiSelectMode = isMultiSelectMode,
                                            isSelected = isSelected,
                                            onToggleSelect = {
                                                selectedFileIds = if (isSelected) {
                                                    selectedFileIds - item.id
                                                } else {
                                                    selectedFileIds + item.id
                                                }
                                            },
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    selectedFileIds = if (isSelected) {
                                                        selectedFileIds - item.id
                                                    } else {
                                                        selectedFileIds + item.id
                                                    }
                                                } else {
                                                    activePreviewFile = item
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar for Multi-Select Deletion
                AnimatedVisibility(
                    visible = isMultiSelectMode && selectedFileIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedFileIds.size} files selected",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Footprint: ${MediaDirectoryManager.formatFileSize(selectedFilesTotalBytes)}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }

                            Button(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("delete_selected_media_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Files", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Multi-Select Delete
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.FolderDelete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Selected Media Files?") },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${selectedFileIds.size} files (${MediaDirectoryManager.formatFileSize(selectedFilesTotalBytes)}) from your device storage? This will instantly free up device space.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val freedBytes = MediaDirectoryManager.deleteMediaFiles(selectedFilesList.map { it.file })
                            Toast.makeText(
                                context,
                                "Successfully deleted ${selectedFilesList.size} files (${MediaDirectoryManager.formatFileSize(freedBytes)} freed)!",
                                Toast.LENGTH_SHORT
                            ).show()
                            showDeleteConfirmDialog = false
                            isMultiSelectMode = false
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for Clearing Category
    if (showClearCategoryConfirmDialog != null) {
        val cat = showClearCategoryConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showClearCategoryConfirmDialog = null },
            title = { Text("Clear All ${cat.displayName} Cache?") },
            text = {
                Text("Are you sure you want to delete all stored ${cat.displayName.lowercase()} from Linko/Media/${cat.folderName}? This operation cannot be undone.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val freedBytes = MediaDirectoryManager.clearCategoryMedia(context, cat)
                            Toast.makeText(context, "Cleared ${cat.displayName} cache (${MediaDirectoryManager.formatFileSize(freedBytes)} freed)!", Toast.LENGTH_SHORT).show()
                            showClearCategoryConfirmDialog = null
                            refreshData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All ${cat.displayName}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCategoryConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single File Preview & Metadata Dialog
    if (activePreviewFile != null) {
        val fileInfo = activePreviewFile!!
        MediaPreviewDialog(
            fileInfo = fileInfo,
            primaryColor = primaryColor,
            onDismiss = { activePreviewFile = null },
            onDelete = {
                scope.launch {
                    val freed = MediaDirectoryManager.deleteMediaFiles(listOf(fileInfo.file))
                    Toast.makeText(context, "Deleted ${fileInfo.name} (${MediaDirectoryManager.formatFileSize(freed)} freed)", Toast.LENGTH_SHORT).show()
                    activePreviewFile = null
                    refreshData()
                }
            },
            onOpenExternal = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(fileInfo.uri, fileInfo.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun StorageGaugeCard(
    summary: StorageSummary,
    primaryColor: Color,
    onClearCategory: (MediaCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Media Storage Used",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.formattedTotalSize,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryColor
                    )
                }

                Box(
                    modifier = Modifier
                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "100% Clean Files",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-color Progress Bar
            val photosRatio = if (summary.totalBytes > 0) summary.photosBytes.toFloat() / summary.totalBytes else 0f
            val videosRatio = if (summary.totalBytes > 0) summary.videosBytes.toFloat() / summary.totalBytes else 0f
            val docsRatio = if (summary.totalBytes > 0) summary.documentsBytes.toFloat() / summary.totalBytes else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                if (photosRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(photosRatio.coerceAtLeast(0.01f))
                            .background(primaryColor)
                    )
                }
                if (videosRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(videosRatio.coerceAtLeast(0.01f))
                            .background(Color(0xFF2563EB)) // Blue
                    )
                }
                if (docsRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(docsRatio.coerceAtLeast(0.01f))
                            .background(Color(0xFF9333EA)) // Purple
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Storage Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageLegendItem(
                    color = primaryColor,
                    title = "Photos",
                    sub = "${summary.photosCount} files (${summary.formattedPhotosSize})",
                    onClear = { onClearCategory(MediaCategory.PHOTOS) }
                )
                StorageLegendItem(
                    color = Color(0xFF2563EB),
                    title = "Videos",
                    sub = "${summary.videosCount} files (${summary.formattedVideosSize})",
                    onClear = { onClearCategory(MediaCategory.VIDEOS) }
                )
                StorageLegendItem(
                    color = Color(0xFF9333EA),
                    title = "Documents",
                    sub = "${summary.documentsCount} files (${summary.formattedDocumentsSize})",
                    onClear = { onClearCategory(MediaCategory.DOCUMENTS) }
                )
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    title: String,
    sub: String,
    onClear: () -> Unit
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(sub, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MediaTileItem(
    item: MediaFileInfo,
    primaryColor: Color,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .testTag("media_tile_${item.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (item.category) {
                MediaCategory.PHOTOS -> {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.file)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MediaCategory.VIDEOS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = item.name,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(6.dp)
                        )
                    }
                }
                MediaCategory.DOCUMENTS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (item.name.endsWith(".pdf", ignoreCase = true)) Icons.Default.Description else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // CRITICAL REQUIREMENT OVERLAY: Footprint Badge directly on top of the UI thumbnail
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.formattedSize,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Multi-select Checkbox overlay
            if (isMultiSelectMode || isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clickable { onToggleSelect() }
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Select",
                        tint = if (isSelected) primaryColor else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaPreviewDialog(
    fileInfo: MediaFileInfo,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fileInfo.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                // Media Content View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (fileInfo.category) {
                        MediaCategory.PHOTOS -> {
                            AsyncImage(
                                model = fileInfo.file,
                                contentDescription = fileInfo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        MediaCategory.VIDEOS -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Video File • ${fileInfo.formattedSize}", color = Color.White, fontSize = 14.sp)
                            }
                        }
                        MediaCategory.DOCUMENTS -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = primaryColor, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(fileInfo.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Size: ${fileInfo.formattedSize}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File Metadata Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("File Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Path: Linko/Media/${fileInfo.category.folderName}/${fileInfo.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Footprint: ${fileInfo.formattedSize} (${fileInfo.sizeBytes} bytes)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Date Saved: ${fileInfo.formattedDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Clean Status: 100% Clean (No Watermarks)", fontSize = 11.sp, color = primaryColor, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenExternal,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open File")
                    }

                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = Color.White)
                    }
                }
            }
        }
    }
}
