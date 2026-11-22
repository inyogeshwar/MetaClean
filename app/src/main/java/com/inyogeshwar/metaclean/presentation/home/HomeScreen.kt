package com.inyogeshwar.metaclean.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inyogeshwar.metaclean.R
import com.inyogeshwar.metaclean.core.ui.components.*
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.repository.SaveDestinationMode

@Composable
fun HomeScreen(
    onNavigateToInspector: () -> Unit,
    onNavigateToBatch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val mediaQueue by viewModel.mediaQueue.collectAsState()
    val historyRecords by viewModel.historyRecords.collectAsState()
    val quickCleanState by viewModel.quickCleanState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // 1-Tap Quick Clean Picker (Directly scrubs & auto-saves)
    val instantCleanPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.quickCleanImages(uris)
        }
    }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImagesSelected(listOf(it)) }
    }

    val multiplePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
            if (!settings.autoSaveOnPick) {
                onNavigateToBatch()
            }
        }
    }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
            if (!settings.autoSaveOnPick) {
                onNavigateToBatch()
            }
        }
    }

    val totalCleaned = historyRecords.sumOf { it.successCount }
    val totalSaved = historyRecords.sumOf { it.bytesSaved }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        // iOS Large Title Navigation Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MetaClean",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Privacy & Metadata Toolkit",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "MetaClean Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Circular Privacy Shield & Metric Ring (iOS Health/Utility Centerpiece)
        IOSCircularShieldMeter(
            totalCleaned = totalCleaned,
            totalBytesSaved = totalSaved
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hero 1-Tap Action Card (Cupertino 1-Tap Clean Center)
        IOSGroupedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(iOSSystemBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = iOSSystemBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "1-Tap Clean & Auto-Save",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                val saveTargetText = if (settings.saveDestinationMode == SaveDestinationMode.OVERWRITE_ORIGINAL) {
                    "Select photos to instantly scrub GPS, camera details & AI labels and update the original image directly."
                } else {
                    "Select photos to instantly scrub GPS, camera details & AI labels and auto-save directly to your Gallery (Pictures/MetaClean)."
                }

                Text(
                    text = saveTargetText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Primary 1-Tap Instant Clean Button
                Button(
                    onClick = {
                        instantCleanPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Instant Clean & Save",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Options Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            singlePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            "Single Photo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Button(
                        onClick = {
                            multiplePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = iOSSystemBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Select Multi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                TextButton(
                    onClick = { safPickerLauncher.launch(arrayOf("image/*")) }
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = iOSSystemBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse Storage Files (SAF)", fontSize = 13.sp, color = iOSSystemBlue)
                }
            }
        }

        // Active Queue Quick Access Banner
        if (mediaQueue.isNotEmpty()) {
            IOSSectionHeader(title = "Processing Queue")
            IOSGroupedCard {
                IOSListRow(
                    title = "Queue (${mediaQueue.size} items ready)",
                    value = "${mediaQueue.count { it.status == com.inyogeshwar.metaclean.domain.model.ProcessingStatus.COMPLETED }} cleaned",
                    icon = Icons.Default.Layers,
                    iconTint = iOSSystemBlue,
                    showChevron = true,
                    onClick = onNavigateToBatch
                )
            }
        }

        // AI Footprint & C2PA Detection Highlights
        IOSSectionHeader(title = "AI Footprint & C2PA Detection")
        IOSGroupedCard {
            IOSListRow(
                title = "ChatGPT / DALL-E Markers",
                value = "Detect & Strip",
                icon = Icons.Default.AutoAwesome,
                iconTint = iOSSystemIndigo
            )
            IOSDivider()
            IOSListRow(
                title = "Midjourney & Stable Diffusion",
                value = "Detected",
                icon = Icons.Default.Psychology,
                iconTint = iOSSystemTeal
            )
            IOSDivider()
            IOSListRow(
                title = "C2PA & JUMBF Manifests",
                value = "Inspected",
                icon = Icons.Default.VerifiedUser,
                iconTint = iOSSystemGreen
            )
        }

        // Format Support Inset Group
        IOSSectionHeader(title = "Format Engine")
        IOSGroupedCard {
            IOSListRow(
                title = "JPEG / JPG",
                value = "Lossless EXIF Scrub",
                icon = Icons.Default.Image,
                iconTint = iOSSystemBlue
            )
            IOSDivider()
            IOSListRow(
                title = "PNG",
                value = "Chunk-Level Filter",
                icon = Icons.Default.GridOn,
                iconTint = iOSSystemIndigo
            )
            IOSDivider()
            IOSListRow(
                title = "WebP",
                value = "Lossless Scrub",
                icon = Icons.Default.FilterVintage,
                iconTint = iOSSystemOrange
            )
        }

        // Privacy Guarantees Group
        IOSSectionHeader(title = "Security Guarantees")
        IOSGroupedCard {
            IOSListRow(
                title = "100% Offline Processing",
                value = "Zero Network",
                icon = Icons.Default.WifiOff,
                iconTint = iOSSystemGreen
            )
            IOSDivider()
            IOSListRow(
                title = "Safe Auto-Save",
                value = if (settings.saveDestinationMode == SaveDestinationMode.OVERWRITE_ORIGINAL) "Overwrite In-Place" else "Saved to Gallery",
                icon = Icons.Default.Lock,
                iconTint = iOSSystemBlue
            )
            IOSDivider()
            IOSListRow(
                title = "No Analytics or Telemetry",
                value = "Private",
                icon = Icons.Default.Shield,
                iconTint = iOSSystemTeal
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // iOS Quick Clean Progress / Completion Dialog
    IOSQuickCleanModal(
        isRunning = quickCleanState.isRunning,
        current = quickCleanState.current,
        total = quickCleanState.total,
        result = quickCleanState.result,
        onDismiss = { viewModel.dismissQuickCleanResult() },
        onShare = {
            val uris = quickCleanState.result?.savedUris ?: emptyList()
            if (uris.isNotEmpty()) {
                val shareIntent = if (uris.size == 1) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, uris.first())
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else {
                    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "image/*"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Cleaned Images"))
            }
        }
    )
}
