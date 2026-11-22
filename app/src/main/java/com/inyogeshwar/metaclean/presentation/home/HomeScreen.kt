package com.inyogeshwar.metaclean.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inyogeshwar.metaclean.R
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.ui.components.*
import com.inyogeshwar.metaclean.core.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToInspector: () -> Unit,
    onNavigateToBatch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val mediaQueue by viewModel.mediaQueue.collectAsState()
    val historyRecords by viewModel.historyRecords.collectAsState()

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
            onNavigateToBatch()
        }
    }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelected(uris)
            onNavigateToBatch()
        }
    }

    val totalCleaned = historyRecords.sumOf { it.successCount }
    val totalSaved = historyRecords.sumOf { it.bytesSaved }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackgroundDark)
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
                    color = iOSTextPrimary
                )
                Text(
                    text = "Privacy & Metadata Toolkit",
                    fontSize = 13.sp,
                    color = iOSTextSecondary
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

        // Hero Selection Card (Cupertino Action Center)
        IOSGroupedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(iOSSystemBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = iOSSystemBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Clean Photos & AI Labels",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Strip GPS, camera hardware specs, and C2PA / JUMBF AI footprints from photos before sharing.",
                    fontSize = 13.sp,
                    color = iOSTextSecondary,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSCardElevated)
                    ) {
                        Text("Single Photo", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary)
                    }

                    Button(
                        onClick = {
                            multiplePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Multiple", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { safPickerLauncher.launch(arrayOf("image/*")) }
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = iOSSystemBlue, modifier = Modifier.size(16.dp))
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

        // Statistics Inset Group
        IOSSectionHeader(title = "Statistics & History")
        IOSGroupedCard {
            IOSListRow(
                title = "Total Photos Scrubbed",
                value = "$totalCleaned",
                icon = Icons.Default.CheckCircle,
                iconTint = iOSSystemGreen
            )
            IOSDivider()
            IOSListRow(
                title = "Stripped Footprint",
                value = FileUtils.formatFileSize(totalSaved),
                icon = Icons.Default.DataUsage,
                iconTint = iOSSystemTeal
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
                title = "Originals Never Overwritten",
                value = "Isolated Copies",
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
}
