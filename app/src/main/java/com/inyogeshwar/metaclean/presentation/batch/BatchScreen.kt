package com.inyogeshwar.metaclean.presentation.batch

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.inyogeshwar.metaclean.core.ui.components.*
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

    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                viewModel.exportZip(
                    outputStream = outputStream,
                    onProgress = { _, _ -> },
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
        // iOS Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Batch Queue",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (completedCount > 0) {
                    IconButton(
                        onClick = { createZipLauncher.launch("metaclean_${System.currentTimeMillis()}.zip") },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iOSCardElevated)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = "ZIP", tint = iOSSystemBlue, modifier = Modifier.size(18.dp))
                    }
                }
                if (mediaQueue.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearQueue() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iOSCardElevated)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = iOSSystemRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Subhead Controls Bar
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
                        onCheckedChange = { viewModel.selectAll(it) },
                        colors = CheckboxDefaults.colors(checkedColor = iOSSystemBlue)
                    )
                    Text(
                        text = "$selectedCount of ${mediaQueue.size} selected",
                        fontSize = 13.sp,
                        color = iOSTextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (completedCount > 0) {
                        TextButton(onClick = { viewModel.clearCompleted() }) {
                            Text("Clear Cleaned", fontSize = 12.sp, color = iOSSystemBlue)
                        }
                    }
                    if (failedCount > 0) {
                        TextButton(onClick = { viewModel.retryFailed() }) {
                            Text("Retry Failed", fontSize = 12.sp, color = iOSSystemOrange)
                        }
                    }
                }
            }
        }

        // Queue List
        if (mediaQueue.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Layers,
                title = "Queue is Empty",
                description = "Select single or multiple photos from Home to batch clean metadata and AI labels.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(mediaQueue, key = { it.id }) { item ->
                    IOSBatchItemCard(
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

        // Sticky Bottom Cupertino Bar
        if (mediaQueue.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(iOSCardDark)
                    .padding(16.dp)
            ) {
                Column {
                    if (isProcessing) {
                        val active = mediaQueue.find { it.status == ProcessingStatus.PROCESSING || it.status == ProcessingStatus.INSPECTING }
                        Text(
                            text = "Scrubbing: ${active?.fileName ?: "batch in progress..."}",
                            fontSize = 12.sp,
                            color = iOSSystemTeal,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { active?.progress ?: 0.5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = iOSSystemBlue,
                            trackColor = iOSCardElevated
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iOSSystemRed)
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { showCleanOptionsDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iOSCardElevated)
                            ) {
                                Text("Options", color = iOSTextPrimary, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = { viewModel.startBatchProcessing() },
                                enabled = selectedCount > 0,
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                            ) {
                                Text("Scrub Selected ($selectedCount)", fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IOSBatchItemCard(
    item: MediaItem,
    onToggleSelect: () -> Unit,
    onInspect: () -> Unit,
    onVerify: () -> Unit,
    onRemove: () -> Unit
) {
    IOSGroupedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = iOSSystemBlue)
            )

            Spacer(modifier = Modifier.width(8.dp))

            AsyncImage(
                model = item.cleanedUri ?: item.originalUri,
                contentDescription = item.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onInspect() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSTextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "${item.formattedSize} • ${item.mimeType.substringAfter("/")}",
                    fontSize = 12.sp,
                    color = iOSTextSecondary
                )
            }

            StatusBadge(status = item.status)

            IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = iOSTextTertiary, modifier = Modifier.size(16.dp))
            }
        }

        if (item.status == ProcessingStatus.COMPLETED) {
            IOSDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onVerify() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View Verification Audit", fontSize = 12.sp, color = iOSSystemGreen, fontWeight = FontWeight.Medium)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = iOSSystemGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}
