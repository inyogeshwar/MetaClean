package com.inyogeshwar.metaclean.presentation.verification

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import com.inyogeshwar.metaclean.core.ui.components.IOSBadge
import com.inyogeshwar.metaclean.core.ui.components.IOSDivider
import com.inyogeshwar.metaclean.core.ui.components.IOSGroupedCard
import com.inyogeshwar.metaclean.core.ui.components.IOSSectionHeader
import com.inyogeshwar.metaclean.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: VerificationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val item by viewModel.item.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

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
                title = { Text("Audit Report", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = iOSSystemBlue)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareCleanedImage(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = iOSSystemBlue)
                    }
                    IconButton(onClick = {
                        val originalName = item?.fileName ?: "cleaned.jpg"
                        val ext = originalName.substringAfterLast(".", "jpg")
                        val base = originalName.substringBeforeLast(".")
                        saveFileLauncher.launch("${base}_clean.$ext")
                    }) {
                        Icon(Icons.Default.SaveAlt, contentDescription = "Save", tint = iOSSystemBlue)
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Status Pill Card
                IOSGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(iOSSystemGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Verified Clean & Private",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSSystemGreen
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ver.summaryNotes,
                                fontSize = 12.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }
                }

                // Visual Before & After Comparison
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Original", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = mediaItem.originalUri,
                            contentDescription = "Original",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FileUtils.formatFileSize(ver.originalSizeBytes),
                            fontSize = 12.sp,
                            color = iOSTextSecondary
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Cleaned Output", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSSystemTeal)
                        Spacer(modifier = Modifier.height(6.dp))
                        AsyncImage(
                            model = mediaItem.cleanedUri,
                            contentDescription = "Cleaned",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FileUtils.formatFileSize(ver.cleanedSizeBytes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSSystemTeal
                        )
                    }
                }

                // Metrics Triple Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IOSMetricCard(
                        title = "Removed",
                        value = "${ver.removedFields.size} fields",
                        subtitle = "Personal tags",
                        icon = Icons.Default.Shield,
                        tint = iOSSystemGreen,
                        modifier = Modifier.weight(1f)
                    )

                    IOSMetricCard(
                        title = "Dimensions",
                        value = if (ver.dimensionsPreserved) "Preserved" else "Modified",
                        subtitle = "${ver.cleanedDimensions.first}×${ver.cleanedDimensions.second}",
                        icon = Icons.Default.AspectRatio,
                        tint = iOSSystemBlue,
                        modifier = Modifier.weight(1f)
                    )

                    IOSMetricCard(
                        title = "Stripped",
                        value = "-${ver.sizeReductionPercentage}%",
                        subtitle = FileUtils.formatFileSize(ver.sizeDifferenceBytes),
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        tint = iOSSystemIndigo,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Segmented Tab Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(iOSCardDark)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("Audit Diff", "Original Tags", "Cleaned Tags")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) iOSCardElevated else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) iOSTextPrimary else iOSTextSecondary
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        IOSGroupedCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Sanitization Checklist", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iOSTextPrimary)
                                Spacer(modifier = Modifier.height(10.dp))

                                if (ver.removedFields.isNotEmpty()) {
                                    Text("Successfully Removed (${ver.removedFields.size}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = iOSSystemGreen)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    ver.removedFields.forEach { f ->
                                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = iOSSystemGreen, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(f, fontSize = 12.sp, color = iOSTextPrimary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Retained Properties (${ver.retainedFields.size}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = iOSSystemTeal)
                                Spacer(modifier = Modifier.height(6.dp))
                                ver.retainedFields.forEach { f ->
                                    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Done, contentDescription = null, tint = iOSSystemTeal, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(f, fontSize = 12.sp, color = iOSTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        IOSGroupedCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Original Metadata", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iOSTextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                origMeta?.exifData?.allTags?.forEach { (k, v) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(k, fontSize = 12.sp, color = iOSTextSecondary)
                                        Text(v, fontSize = 12.sp, color = iOSTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        IOSGroupedCard {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Cleaned Output Metadata", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = iOSTextPrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (cleanMeta?.exifData?.allTags?.isEmpty() == true) {
                                    Text("All personal EXIF tags completely scrubbed.", fontSize = 12.sp, color = iOSSystemGreen)
                                } else {
                                    cleanMeta?.exifData?.allTags?.forEach { (k, v) ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(k, fontSize = 12.sp, color = iOSTextSecondary)
                                            Text(v, fontSize = 12.sp, color = iOSTextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.shareCleanedImage(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSCardElevated)
                    ) {
                        Text("Share Cleaned", color = iOSTextPrimary, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            val originalName = mediaItem.fileName
                            val ext = originalName.substringAfterLast(".", "jpg")
                            val base = originalName.substringBeforeLast(".")
                            saveFileLauncher.launch("${base}_clean.$ext")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                    ) {
                        Text("Save Copy", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun IOSMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    IOSGroupedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = iOSTextPrimary)
            Text(subtitle, fontSize = 10.sp, color = iOSTextSecondary)
        }
    }
}
