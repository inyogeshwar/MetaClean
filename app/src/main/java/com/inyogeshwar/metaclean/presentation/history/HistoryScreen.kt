package com.inyogeshwar.metaclean.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.ui.components.EmptyStateView
import com.inyogeshwar.metaclean.core.ui.components.IOSDivider
import com.inyogeshwar.metaclean.core.ui.components.IOSGroupedCard
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.model.HistoryRecord

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyRecords by viewModel.historyRecords.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All History?", color = iOSTextPrimary) },
            text = { Text("This will permanently remove the log of past processing sessions from your device.", color = iOSTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = iOSSystemBlue)
                }
            },
            containerColor = iOSCardDark
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (historyRecords.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iOSCardElevated)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = iOSSystemRed, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (historyRecords.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.History,
                title = "No History Records",
                description = "Completed privacy cleaning sessions will be securely logged here on-device without storing photo contents.",
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
                items(historyRecords, key = { it.id }) { record ->
                    IOSHistoryItemCard(
                        record = record,
                        onDelete = { viewModel.deleteRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IOSHistoryItemCard(
    record: HistoryRecord,
    onDelete: () -> Unit
) {
    IOSGroupedCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(iOSSystemGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = iOSSystemGreen, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(record.processingType, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iOSTextPrimary)
                        Text(record.formattedDate, fontSize = 11.sp, color = iOSTextSecondary)
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = iOSTextTertiary, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            IOSDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Processed", fontSize = 11.sp, color = iOSTextSecondary)
                    Text("${record.totalFiles} photos (${record.successCount} ok, ${record.failureCount} err)", fontSize = 13.sp, color = iOSTextPrimary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Footprint Stripped", fontSize = 11.sp, color = iOSTextSecondary)
                    Text(FileUtils.formatFileSize(record.bytesSaved), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = iOSSystemTeal)
                }
            }

            if (record.removedTagsSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scrubbed: ${record.removedTagsSummary}",
                    fontSize = 11.sp,
                    color = iOSTextSecondary
                )
            }
        }
    }
}
