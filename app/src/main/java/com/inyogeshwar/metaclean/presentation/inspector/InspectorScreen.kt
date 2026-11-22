package com.inyogeshwar.metaclean.presentation.inspector

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
            title = { Text("Raw Metadata JSON Export") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = jsonText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(jsonText))
                        showJsonDialog = false
                    }
                ) {
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, jsonText)
                        type = "application/json"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Metadata JSON"))
                }) {
                    Text("Share")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Metadata Inspector",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Open Image")
                }
                if (metadata != null) {
                    IconButton(onClick = { showJsonDialog = true }) {
                        Icon(Icons.Default.DataObject, contentDescription = "Export JSON")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        // Queue Horizontal Selector if multiple images exist
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
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectItem(item.id) },
                        label = {
                            Text(
                                text = item.fileName.take(18) + if (item.fileName.length > 18) "..." else "",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            StatusBadge(status = item.status)
                        }
                    )
                }
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
        }

        if (selectedItem == null || metadata == null) {
            EmptyStateView(
                icon = Icons.Default.Search,
                title = "No Image Selected",
                description = "Choose an image to inspect its EXIF, GPS location, camera specifications, IPTC, XMP and file signatures.",
                buttonText = "Select Image",
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Image Preview & Quick Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.originalUri,
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${metadata.fileInfo.width} × ${metadata.fileInfo.height} px • ${metadata.fileInfo.megapixels} MP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.formattedSize} • ${metadata.fileInfo.formatDescription}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentSecondary
                            )
                        }

                        StatusBadge(status = item.status)
                    }
                }

                // Privacy Warning Banner if GPS or Personal fields are found
                if (metadata.exifData.gpsCoordinateString != null || metadata.exifData.artist != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.12f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Privacy Sensitive Metadata Found",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DangerRed
                                )
                                Text(
                                    text = "This file contains embedded personal location or identity tags that could expose your privacy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 1. File Information Section
                MetadataSectionCard(
                    title = "File Information",
                    icon = Icons.Default.Info,
                    itemCount = 7,
                    initiallyExpanded = true,
                    badgeText = metadata.fileInfo.formatDescription
                ) {
                    MetadataItemRow("File Name", metadata.fileInfo.fileName)
                    MetadataItemRow("File Size", metadata.fileInfo.formattedFileSize)
                    MetadataItemRow("MIME Type", metadata.fileInfo.mimeType)
                    MetadataItemRow("Dimensions", "${metadata.fileInfo.width} × ${metadata.fileInfo.height} px")
                    MetadataItemRow("Resolution", "${metadata.fileInfo.megapixels} Megapixels")
                    MetadataItemRow("Image Orientation", metadata.fileInfo.orientation)
                    MetadataItemRow("Container Format", metadata.fileInfo.formatDescription)
                }

                // 2. GPS Location
                MetadataSectionCard(
                    title = "GPS & Location",
                    icon = Icons.Default.LocationOn,
                    itemCount = if (metadata.exifData.gpsCoordinateString != null) 3 else 0,
                    initiallyExpanded = metadata.exifData.gpsCoordinateString != null,
                    badgeText = if (metadata.exifData.gpsCoordinateString != null) "FOUND" else "CLEAN",
                    badgeColor = if (metadata.exifData.gpsCoordinateString != null) DangerRed else SuccessGreen
                ) {
                    if (metadata.exifData.gpsCoordinateString != null) {
                        MetadataItemRow("Coordinates", metadata.exifData.gpsCoordinateString, isSensitive = true, highlight = true)
                        MetadataItemRow("Latitude", metadata.exifData.gpsLatitude?.toString(), isSensitive = true)
                        MetadataItemRow("Longitude", metadata.exifData.gpsLongitude?.toString(), isSensitive = true)
                        metadata.exifData.gpsAltitude?.let {
                            MetadataItemRow("Altitude", "%.1f meters".format(it), isSensitive = true)
                        }
                    } else {
                        Text(
                            text = "No GPS coordinates or geographical location tags found in this file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. Camera & EXIF
                MetadataSectionCard(
                    title = "Camera & EXIF Hardware",
                    icon = Icons.Default.PhotoCamera,
                    itemCount = metadata.exifData.allTags.size,
                    initiallyExpanded = false,
                    badgeText = if (metadata.exifData.hasExif) "EXIF 2.3+" else "NONE"
                ) {
                    MetadataItemRow("Camera Make", metadata.exifData.cameraMake, isSensitive = true)
                    MetadataItemRow("Camera Model", metadata.exifData.cameraModel, isSensitive = true)
                    MetadataItemRow("Lens Model", metadata.exifData.lensModel)
                    MetadataItemRow("Software / Tool", metadata.exifData.software, isSensitive = true)
                    MetadataItemRow("Original Date/Time", metadata.exifData.dateTimeOriginal, isSensitive = true)
                    MetadataItemRow("Digitized Date/Time", metadata.exifData.dateTimeDigitized, isSensitive = true)
                    MetadataItemRow("ISO Sensitivity", metadata.exifData.iso)
                    MetadataItemRow("Aperture", metadata.exifData.fNumber)
                    MetadataItemRow("Shutter Speed", metadata.exifData.exposureTime)
                    MetadataItemRow("Focal Length", metadata.exifData.focalLength)
                    MetadataItemRow("35mm Equivalent", metadata.exifData.focalLengthIn35mm)
                    MetadataItemRow("Flash", metadata.exifData.flash)
                    MetadataItemRow("White Balance", metadata.exifData.whiteBalance)
                    MetadataItemRow("Metering Mode", metadata.exifData.meteringMode)
                    MetadataItemRow("Artist / Author", metadata.exifData.artist, isSensitive = true)
                    MetadataItemRow("Copyright", metadata.exifData.copyright, isSensitive = true)
                    MetadataItemRow("User Comment", metadata.exifData.userComment, isSensitive = true)
                    MetadataItemRow("Embedded Thumbnail", if (metadata.exifData.hasThumbnail) "Yes (Present)" else "None")
                }

                // 4. IPTC & Authorship
                MetadataSectionCard(
                    title = "IPTC & Authorship",
                    icon = Icons.Default.Person,
                    itemCount = if (metadata.iptcData.hasIptc) 3 else 0,
                    initiallyExpanded = false
                ) {
                    if (metadata.iptcData.hasIptc) {
                        MetadataItemRow("Creator", metadata.iptcData.creator, isSensitive = true)
                        MetadataItemRow("Copyright", metadata.iptcData.copyrightNotice, isSensitive = true)
                        MetadataItemRow("Caption / Headline", metadata.iptcData.caption ?: metadata.iptcData.headline)
                        MetadataItemRow("Contact", metadata.iptcData.contactInfo, isSensitive = true)
                    } else {
                        Text(
                            text = "No standard IPTC authorship blocks detected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 5. Adobe XMP
                MetadataSectionCard(
                    title = "Adobe XMP Properties",
                    icon = Icons.Default.Description,
                    itemCount = metadata.xmpData.properties.size,
                    initiallyExpanded = false
                ) {
                    if (metadata.xmpData.hasXmp) {
                        MetadataItemRow("Namespaces", metadata.xmpData.namespaces.joinToString(", "))
                        metadata.xmpData.properties.forEach { (k, v) ->
                            MetadataItemRow(k, v)
                        }
                    } else {
                        Text(
                            text = "No embedded Adobe XMP XML packet found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 6. PNG Chunks (If PNG)
                if (metadata.pngData != null) {
                    MetadataSectionCard(
                        title = "PNG Chunks Structure",
                        icon = Icons.Default.GridOn,
                        itemCount = metadata.pngData.chunks.size,
                        initiallyExpanded = false,
                        badgeText = "${metadata.pngData.chunks.size} CHUNKS"
                    ) {
                        MetadataItemRow("Color Type", metadata.pngData.colorType)
                        MetadataItemRow("Bit Depth", metadata.pngData.bitDepth?.let { "$it bits/channel" })
                        MetadataItemRow("Interlace", metadata.pngData.interlaceMethod)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Chunk Breakdown:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        metadata.pngData.chunks.forEach { chunk ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = chunk.type,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = if (chunk.isCritical) AccentPrimary else AccentSecondary
                                )
                                Text(
                                    text = "${chunk.length} bytes • ${chunk.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // 7. ICC Color Profile
                MetadataSectionCard(
                    title = "ICC Color Profile",
                    icon = Icons.Default.Palette,
                    itemCount = if (metadata.iccData?.hasProfile == true) 2 else 0,
                    initiallyExpanded = false
                ) {
                    if (metadata.iccData?.hasProfile == true) {
                        MetadataItemRow("Profile Description", metadata.iccData.profileName)
                        MetadataItemRow("Color Space", metadata.iccData.colorSpace)
                        MetadataItemRow("Profile Size", "${metadata.iccData.profileSize} bytes")
                    } else {
                        Text(
                            text = "Standard sRGB / No custom ICC profile embedded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 8. Content Credentials & AI Provenance (Read-Only)
                MetadataSectionCard(
                    title = "Content Credentials & AI Signals",
                    icon = Icons.Default.AutoAwesome,
                    itemCount = metadata.provenanceData.aiSignals.size,
                    initiallyExpanded = metadata.provenanceData.hasProvenance,
                    badgeText = "READ-ONLY",
                    badgeColor = AccentTertiary
                ) {
                    Text(
                        text = metadata.provenanceData.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataItemRow("C2PA Detected", if (metadata.provenanceData.isC2paDetected) "Yes (Manifest Found)" else "No")
                    MetadataItemRow("JUMBF Box Detected", if (metadata.provenanceData.isJumbfDetected) "Yes" else "No")
                    MetadataItemRow("Detected Generator", metadata.provenanceData.generatorSoftware)
                    if (metadata.provenanceData.aiSignals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Detected Signals:",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentTertiary
                        )
                        metadata.provenanceData.aiSignals.forEach { signal ->
                            Text(
                                text = "• $signal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 9. Technical Hashes & Hex Viewer
                HexViewerCard(technicalData = metadata.technicalData)

                Spacer(modifier = Modifier.height(12.dp))

                // Action Bar: Clean or Verify
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCleanOptionsDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clean This Image")
                    }

                    if (item.status == ProcessingStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = { onNavigateToVerification(item.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Output")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
