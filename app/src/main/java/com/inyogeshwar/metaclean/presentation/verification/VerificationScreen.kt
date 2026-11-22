package com.inyogeshwar.metaclean.presentation.verification

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.ui.components.EmptyStateView
import com.inyogeshwar.metaclean.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val item by viewModel.item.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Audit Diff, 1 = Original, 2 = Cleaned

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(item?.mimeType ?: "image/jpeg")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportToDestination(uri) { success ->
                Toast.makeText(
                    context,
                    if (success) "Cleaned image saved successfully!" else "Failed to save file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Verification Audit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareCleanedImage(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        val originalName = item?.fileName ?: "cleaned.jpg"
                        val ext = originalName.substringAfterLast(".", "jpg")
                        val base = originalName.substringBeforeLast(".")
                        saveFileLauncher.launch("${base}_clean.$ext")
                    }) {
                        Icon(Icons.Default.SaveAlt, contentDescription = "Save to Device")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (item == null || item?.verificationResult == null) {
            EmptyStateView(
                icon = Icons.Default.VerifiedUser,
                title = "No Verification Data",
                description = "This file has not yet undergone the privacy cleaning pipeline.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            val mediaItem = item!!
            val ver = mediaItem.verificationResult!!
            val origMeta = mediaItem.originalMetadata
            val cleanMeta = mediaItem.cleanedMetadata

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Verified Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Verified Clean & Privacy Protected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ver.summaryNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Visual Before / After Comparison Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Original Copy", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = mediaItem.originalUri,
                            contentDescription = "Original",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FileUtils.formatFileSize(ver.originalSizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Cleaned Output", style = MaterialTheme.typography.labelMedium, color = AccentSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = mediaItem.cleanedUri,
                            contentDescription = "Cleaned",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = FileUtils.formatFileSize(ver.cleanedSizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentSecondary
                        )
                    }
                }

                // Quantitative Metric Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VerificationMetricCard(
                        title = "Removed Fields",
                        value = "${ver.removedFields.size}",
                        subtitle = "Personal tags stripped",
                        icon = Icons.Default.Shield,
                        tint = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )

                    VerificationMetricCard(
                        title = "Dimensions",
                        value = if (ver.dimensionsPreserved) "Preserved" else "Modified",
                        subtitle = "${ver.cleanedDimensions.first} × ${ver.cleanedDimensions.second} px",
                        icon = Icons.Default.AspectRatio,
                        tint = AccentSecondary,
                        modifier = Modifier.weight(1f)
                    )

                    VerificationMetricCard(
                        title = "Footprint Delta",
                        value = "-${ver.sizeReductionPercentage}%",
                        subtitle = FileUtils.formatFileSize(ver.sizeDifferenceBytes),
                        icon = Icons.Default.TrendingDown,
                        tint = AccentTertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Comparison Category Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = AccentPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Audit Diff", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Original Tags", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Cleaned Tags", fontSize = 12.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // Audit Diff List
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Privacy Sanitization Audit",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                if (ver.removedFields.isNotEmpty()) {
                                    Text(
                                        text = "Successfully Removed (${ver.removedFields.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ver.removedFields.forEach { field ->
                                        AuditFieldRow(field, isRemoved = true)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Retained Technical Properties (${ver.retainedFields.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                ver.retainedFields.forEach { field ->
                                    AuditFieldRow(field, isRemoved = false)
                                }

                                if (ver.unremovableFields.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Informational / Read-Only Elements (${ver.unremovableFields.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WarningAmber
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ver.unremovableFields.forEach { field ->
                                        AuditFieldRow(field, isWarning = true)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Original Tags
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Original Metadata Snapshot", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                origMeta?.exifData?.allTags?.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Cleaned Tags
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Cleaned Output Metadata Snapshot", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (cleanMeta?.exifData?.allTags?.isEmpty() == true) {
                                    Text("All personal EXIF tags completely scrubbed.", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                                } else {
                                    cleanMeta?.exifData?.allTags?.forEach { (k, v) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(k, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Export & Share Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.shareCleanedImage(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Cleaned")
                    }

                    Button(
                        onClick = {
                            val originalName = mediaItem.fileName
                            val ext = originalName.substringAfterLast(".", "jpg")
                            val base = originalName.substringBeforeLast(".")
                            saveFileLauncher.launch("${base}_clean.$ext")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Copy")
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AuditFieldRow(
    fieldName: String,
    isRemoved: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                isRemoved -> Icons.Default.CheckCircle
                isWarning -> Icons.Default.Info
                else -> Icons.Default.Done
            },
            contentDescription = null,
            tint = when {
                isRemoved -> SuccessGreen
                isWarning -> WarningAmber
                else -> AccentSecondary
            },
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = fieldName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
