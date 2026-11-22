package com.inyogeshwar.metaclean.presentation.inspector

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.inyogeshwar.metaclean.core.ui.components.*
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.model.ProcessingStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    onNavigateToVerification: (String) -> Unit,
    viewModel: InspectorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val mediaQueue by viewModel.mediaQueue.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()

    var showCleanOptionsDialog by remember { mutableStateOf(false) }
    var showJsonDialog by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.onSingleImageSelected(it) }
    }

    if (showCleanOptionsDialog) {
        CleanOptionsDialog(
            onDismiss = { showCleanOptionsDialog = false },
            onConfirm = { options ->
                showCleanOptionsDialog = false
                viewModel.cleanCurrentImage(options)
            }
        )
    }

    val metadata = selectedItem?.originalMetadata

    if (showJsonDialog && metadata != null) {
        val jsonText = viewModel.generateMetadataJson(metadata)
        AlertDialog(
            onDismissRequest = { showJsonDialog = false },
            title = { Text("Metadata JSON Export", color = iOSTextPrimary) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(iOSCardElevated, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonText,
                        color = iOSTextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(jsonText))
                        showJsonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                ) {
                    Text("Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, jsonText)
                        type = "application/json"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share JSON"))
                }) {
                    Text("Share", color = iOSSystemBlue)
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
        // iOS Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inspector",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (metadata != null) {
                    IconButton(
                        onClick = { showJsonDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iOSCardElevated)
                    ) {
                        Icon(Icons.Default.DataObject, contentDescription = "JSON", tint = iOSTextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                IconButton(
                    onClick = {
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iOSSystemBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Open", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Horizontal Image Carousel Selector
        if (mediaQueue.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mediaQueue.forEach { item ->
                    val isSelected = item.id == selectedItem?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) iOSSystemBlue else iOSCardDark)
                            .clickable { viewModel.selectItem(item.id) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = item.fileName.take(16) + if (item.fileName.length > 16) "..." else "",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else iOSTextSecondary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (selectedItem == null || metadata == null) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "No Photo Inspected",
                description = "Choose an image to inspect EXIF tags, GPS coordinates, camera hardware, and C2PA / JUMBF AI footprints.",
                buttonText = "Select Photo",
                onButtonClick = {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            val item = selectedItem!!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Image Hero Preview Card
                IOSGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.originalUri,
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = iOSTextPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${metadata.fileInfo.width} × ${metadata.fileInfo.height} px • ${metadata.fileInfo.megapixels} MP",
                                fontSize = 13.sp,
                                color = iOSTextSecondary
                            )
                            Text(
                                text = "${item.formattedSize} • ${metadata.fileInfo.formatDescription}",
                                fontSize = 12.sp,
                                color = iOSSystemTeal
                            )
                        }

                        StatusBadge(status = item.status)
                    }
                }

                // AI Footprint & C2PA Status (Inspired by DeleteFootprints.ai from z paste.txt)
                val prov = metadata.provenanceData
                val hasAi = prov.hasProvenance || prov.aiSignals.isNotEmpty()
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
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (hasAi) iOSSystemOrange.copy(alpha = 0.2f) else iOSSystemGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (hasAi) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (hasAi) iOSSystemOrange else iOSSystemGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (hasAi) "AI Footprints Found" else "No AI Signals Detected",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasAi) iOSSystemOrange else iOSSystemGreen
                                )
                            }

                            IOSBadge(
                                text = if (hasAi) "DETECTED" else "CLEAN",
                                color = if (hasAi) iOSSystemOrange else iOSSystemGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hasAi) "Hidden C2PA, JUMBF, or AI generator metadata markers were identified."
                            else "No C2PA manifests or known AI generator footprints were found in the file headers.",
                            fontSize = 12.sp,
                            color = iOSTextSecondary
                        )

                        if (prov.generatorSoftware != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("AI Model / Source: ", fontSize = 13.sp, color = iOSTextSecondary)
                                Text(prov.generatorSoftware!!, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = iOSSystemIndigo)
                            }
                        }

                        if (prov.aiSignals.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            prov.aiSignals.forEach { signal ->
                                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("• ", color = iOSSystemTeal, fontWeight = FontWeight.Bold)
                                    Text(signal, fontSize = 12.sp, color = iOSTextPrimary)
                                }
                            }
                        }
                    }
                }

                // GPS Location Section
                IOSSectionHeader(title = "Location & GPS Privacy")
                IOSGroupedCard {
                    val gps = metadata.exifData.gpsCoordinateString
                    if (gps != null) {
                        IOSListRow(
                            title = "Coordinates",
                            value = gps,
                            icon = Icons.Default.LocationOn,
                            iconTint = iOSSystemRed,
                            isSensitive = true
                        )
                        IOSDivider()
                        IOSListRow(
                            title = "Latitude",
                            value = metadata.exifData.gpsLatitude?.toString(),
                            icon = Icons.Default.Explore,
                            iconTint = iOSSystemRed,
                            isSensitive = true
                        )
                        IOSDivider()
                        IOSListRow(
                            title = "Longitude",
                            value = metadata.exifData.gpsLongitude?.toString(),
                            icon = Icons.Default.Explore,
                            iconTint = iOSSystemRed,
                            isSensitive = true
                        )
                        metadata.exifData.gpsAltitude?.let {
                            IOSDivider()
                            IOSListRow(
                                title = "Altitude",
                                value = "%.1f m".format(it),
                                icon = Icons.Default.Terrain,
                                iconTint = iOSSystemOrange,
                                isSensitive = true
                            )
                        }
                    } else {
                        IOSListRow(
                            title = "GPS Location",
                            value = "None (Clean)",
                            icon = Icons.Default.LocationOff,
                            iconTint = iOSSystemGreen
                        )
                    }
                }

                // Camera Hardware & Capture
                IOSSectionHeader(title = "Camera & Capture Hardware")
                IOSGroupedCard {
                    IOSListRow(title = "Camera Make", value = metadata.exifData.cameraMake ?: "Unknown", icon = Icons.Default.PhotoCamera, iconTint = iOSSystemBlue, isSensitive = true)
                    IOSDivider()
                    IOSListRow(title = "Camera Model", value = metadata.exifData.cameraModel ?: "Unknown", icon = Icons.Default.CameraAlt, iconTint = iOSSystemBlue, isSensitive = true)
                    metadata.exifData.lensModel?.let {
                        IOSDivider()
                        IOSListRow(title = "Lens", value = it, icon = Icons.Default.Camera, iconTint = iOSSystemIndigo)
                    }
                    metadata.exifData.dateTimeOriginal?.let {
                        IOSDivider()
                        IOSListRow(title = "Captured On", value = it, icon = Icons.Default.Schedule, iconTint = iOSSystemOrange, isSensitive = true)
                    }
                    metadata.exifData.iso?.let {
                        IOSDivider()
                        IOSListRow(title = "ISO", value = it, icon = Icons.Default.ShutterSpeed, iconTint = iOSSystemTeal)
                    }
                    metadata.exifData.fNumber?.let {
                        IOSDivider()
                        IOSListRow(title = "Aperture", value = it, icon = Icons.Default.Camera, iconTint = iOSSystemIndigo)
                    }
                    metadata.exifData.artist?.let {
                        IOSDivider()
                        IOSListRow(title = "Author / Artist", value = it, icon = Icons.Default.Person, iconTint = iOSSystemRed, isSensitive = true)
                    }
                }

                // PNG Chunks Breakdown (if PNG)
                if (metadata.pngData != null) {
                    IOSSectionHeader(title = "PNG Chunk Structure (${metadata.pngData.chunks.size} chunks)")
                    IOSGroupedCard {
                        IOSListRow(title = "Color Type", value = metadata.pngData.colorType ?: "RGB", icon = Icons.Default.Palette, iconTint = iOSSystemTeal)
                        IOSDivider()
                        IOSListRow(title = "Bit Depth", value = "${metadata.pngData.bitDepth ?: 8} bits", icon = Icons.Default.Layers, iconTint = iOSSystemIndigo)

                        metadata.pngData.chunks.take(8).forEach { chunk ->
                            IOSDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(chunk.type, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (chunk.isCritical) iOSSystemBlue else iOSSystemTeal)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(chunk.description, fontSize = 11.sp, color = iOSTextSecondary, maxLines = 1)
                                }
                                Text("${chunk.length} B", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = iOSTextTertiary)
                            }
                        }
                    }
                }

                // Technical Checksums Card
                IOSSectionHeader(title = "Digital Checksums & Hashes")
                IOSGroupedCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        HashItem("SHA-256", metadata.technicalData.sha256) {
                            clipboard.setText(AnnotatedString(metadata.technicalData.sha256))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HashItem("MD5", metadata.technicalData.md5) {
                            clipboard.setText(AnnotatedString(metadata.technicalData.md5))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HashItem("Signature", metadata.technicalData.magicSignature) {
                            clipboard.setText(AnnotatedString(metadata.technicalData.magicSignature))
                        }
                    }
                }

                // Action Bar
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCleanOptionsDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clean This Photo", fontWeight = FontWeight.SemiBold)
                    }

                    if (item.status == ProcessingStatus.COMPLETED) {
                        Button(
                            onClick = { onNavigateToVerification(item.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = iOSSystemGreen)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Audit Report", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun HashItem(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = iOSTextSecondary)
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = iOSTextPrimary)
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = iOSSystemBlue, modifier = Modifier.size(16.dp))
        }
    }
}
