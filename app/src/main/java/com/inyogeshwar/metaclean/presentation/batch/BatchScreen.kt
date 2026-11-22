package com.inyogeshwar.metaclean.presentation.batch

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.inyogeshwar.metaclean.core.ui.components.CleanOptionsDialog
import com.inyogeshwar.metaclean.core.ui.components.EmptyStateView
import com.inyogeshwar.metaclean.core.ui.components.StatusBadge
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.model.ProcessingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onNavigateToInspector: (String) -> Unit,
    onNavigateToVerification: (String) -> Unit,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val mediaQueue by viewModel.mediaQueue.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var showCleanOptionsDialog by remember { mutableStateOf(false) }

    // Export ZIP Launcher (SAF Create Document)
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                viewModel.exportZip(
                    outputStream = outputStream,
                    onProgress = { current, total -> },
                    onComplete = { success ->
                        Toast.makeText(
                            context,
                            if (success) "ZIP archive exported successfully!" else "Failed to export ZIP",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    if (showCleanOptionsDialog) {
        CleanOptionsDialog(
            onDismiss = { showCleanOptionsDialog = false },
            onConfirm = { options ->
                showCleanOptionsDialog = false
                viewModel.startBatchProcessing(options)
            }
        )
    }

    val selectedCount = mediaQueue.count { it.isSelected }
    val completedCount = mediaQueue.count { it.status == ProcessingStatus.COMPLETED }
    val failedCount = mediaQueue.count { it.status == ProcessingStatus.FAILED }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Batch Queue (${mediaQueue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                if (completedCount > 0) {
                    IconButton(
                        onClick = {
                            createZipLauncher.launch("metaclean_export_${System.currentTimeMillis()}.zip")
                        }
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "Export ZIP")
                    }
                }
                if (mediaQueue.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearQueue() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Queue")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Queue Controls Bar
        if (mediaQueue.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedCount == mediaQueue.size && mediaQueue.isNotEmpty(),
                        onCheckedChange = { viewModel.selectAll(it) }
                    )
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (completedCount > 0) {
                        TextButton(onClick = { viewModel.clearCompleted() }) {
                            Text("Clear Cleaned", fontSize = 12.sp)
                        }
                    }
                    if (failedCount > 0) {
                        TextButton(onClick = { viewModel.retryFailed() }) {
                            Text("Retry Failed", fontSize = 12.sp, color = WarningAmber)
                        }
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
        }

        // Queue List
        if (mediaQueue.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Layers,
                title = "Queue is Empty",
                description = "No images added for processing yet. Go to Home to select single or multiple photos.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mediaQueue, key = { it.id }) { item ->
                    BatchItemCard(
                        item = item,
                        onToggleSelect = { viewModel.toggleSelection(item.id) },
                        onInspect = {
                            viewModel.selectItemForInspection(item.id)
                            onNavigateToInspector(item.id)
                        },
                        onVerify = { onNavigateToVerification(item.id) },
                        onRemove = { viewModel.removeItem(item.id) }
                    )
                }
            }
        }

        // Sticky Bottom Processing Action Bar
        if (mediaQueue.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isProcessing) {
                        val activeItem = mediaQueue.find { it.status == ProcessingStatus.PROCESSING || it.status == ProcessingStatus.INSPECTING }
                        Text(
                            text = "Cleaning: ${activeItem?.fileName ?: "batch in progress..."}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentSecondary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { activeItem?.progress ?: 0.5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentPrimary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isProcessing) {
                            Button(
                                onClick = { viewModel.cancelProcessing() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel Processing")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showCleanOptionsDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Options")
                            }

                            Button(
                                onClick = { viewModel.startBatchProcessing() },
                                enabled = selectedCount > 0,
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clean Selected ($selectedCount)")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchItemCard(
    item: MediaItem,
    onToggleSelect: () -> Unit,
    onInspect: () -> Unit,
    onVerify: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                AsyncImage(
                    model = item.cleanedUri ?: item.originalUri,
                    contentDescription = item.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onInspect() }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.formattedSize} • ${item.mimeType.substringAfter("/")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = item.status)

                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Progress bar if active
            if (item.status == ProcessingStatus.PROCESSING || item.status == ProcessingStatus.INSPECTING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentPrimary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }

            // Error message if failed
            if (item.errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed,
                    fontSize = 11.sp
                )
            }

            // Verification shortcut if completed
            if (item.status == ProcessingStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onVerify,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp), tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Verification Audit", fontSize = 11.sp, color = SuccessGreen)
                    }
                }
            }
        }
    }
}
