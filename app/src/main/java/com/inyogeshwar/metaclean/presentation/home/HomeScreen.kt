package com.inyogeshwar.metaclean.presentation.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.inyogeshwar.metaclean.R
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.ui.components.*
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.model.MediaItem
import com.inyogeshwar.metaclean.domain.repository.AppThemeMode
import com.inyogeshwar.metaclean.domain.repository.SaveDestinationMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToInspector: () -> Unit = {},
    onNavigateToBatch: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val mediaQueue by viewModel.mediaQueue.collectAsState()
    val historyRecords by viewModel.historyRecords.collectAsState()
    val quickCleanState by viewModel.quickCleanState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var inspectingItem by remember { mutableStateOf<MediaItem?>(null) }

    // Check storage permission
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasStoragePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, requiredPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Storage access granted", Toast.LENGTH_SHORT).show()
        }
    }

    // 1-Tap Pickers
    val multiPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.quickCleanImages(uris)
        }
    }

    val singlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.quickCleanImages(listOf(it)) }
    }

    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.quickCleanImages(uris)
        }
    }

    fun triggerSelectPhotos() {
        if (!hasStoragePermission) {
            permissionLauncher.launch(requiredPermission)
        }
        multiPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
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
        // iOS Navigation Bar Header with Quick Settings Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "MetaClean Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MetaClean",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Instant Photo Privacy",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = { showSettingsSheet = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = iOSSystemBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Storage Permission Banner (Shown only if not yet granted)
        if (!hasStoragePermission) {
            IOSGroupedCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(iOSSystemOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = iOSSystemOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Photo Access Permission",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Required to save directly to Gallery",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { permissionLauncher.launch(requiredPermission) },
                        colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Circular Privacy Ring Centerpiece
        IOSCircularShieldMeter(
            totalCleaned = totalCleaned,
            totalBytesSaved = totalSaved
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hero 1-Tap Action Card
        IOSGroupedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Save Mode Switch (Save to Gallery vs Overwrite Original)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isGallery = settings.saveDestinationMode == SaveDestinationMode.GALLERY_ALBUM

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isGallery) iOSSystemBlue else Color.Transparent)
                            .clickable { viewModel.setSaveDestinationMode(SaveDestinationMode.GALLERY_ALBUM) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save to Gallery",
                            fontSize = 12.sp,
                            fontWeight = if (isGallery) FontWeight.Bold else FontWeight.Medium,
                            color = if (isGallery) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isGallery) iOSSystemOrange else Color.Transparent)
                            .clickable { viewModel.setSaveDestinationMode(SaveDestinationMode.OVERWRITE_ORIGINAL) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Overwrite Original",
                            fontSize = 12.sp,
                            fontWeight = if (!isGallery) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isGallery) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PRIMARY 1-TAP ACTION BUTTON
                Button(
                    onClick = { triggerSelectPhotos() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Select & Clean Photos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            singlePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Single Photo", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { safPickerLauncher.launch(arrayOf("image/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse Files", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (settings.saveDestinationMode == SaveDestinationMode.OVERWRITE_ORIGINAL)
                        "⚡ Selected photos will be cleaned and updated directly in place."
                    else
                        "📁 Selected photos will be cleaned and auto-saved to Gallery (Pictures/MetaClean).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Recent Cleaned Photos Strip (if any items cleaned)
        val completedItems = mediaQueue.filter { it.cleanedUri != null }
        if (completedItems.isNotEmpty()) {
            IOSSectionHeader(title = "Cleaned Photos (${completedItems.size})")
            IOSGroupedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(completedItems) { item ->
                            Column(
                                modifier = Modifier
                                    .width(84.dp)
                                    .clickable { inspectingItem = item },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Gray.copy(alpha = 0.15f))
                                ) {
                                    AsyncImage(
                                        model = item.cleanedUri ?: item.originalUri,
                                        contentDescription = item.fileName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(iOSSystemGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.fileName,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = item.formattedSize,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val galleryIntent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                context.startActivity(galleryIntent)
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = iOSSystemBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Gallery", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                        }

                        Button(
                            onClick = {
                                val uris = completedItems.mapNotNull { it.cleanedUri }
                                if (uris.isNotEmpty()) {
                                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                        type = "image/*"
                                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Cleaned Photos"))
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share All", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // AI Footprint & C2PA Detection Engine
        IOSSectionHeader(title = "AI & Privacy Shield")
        IOSGroupedCard {
            IOSListRow(
                title = "ChatGPT / DALL-E Metadata",
                value = "Stripped",
                icon = Icons.Default.AutoAwesome,
                iconTint = iOSSystemIndigo
            )
            IOSDivider()
            IOSListRow(
                title = "C2PA & JUMBF Manifests",
                value = "Erased",
                icon = Icons.Default.VerifiedUser,
                iconTint = iOSSystemGreen
            )
            IOSDivider()
            IOSListRow(
                title = "GPS & Camera Hardware",
                value = "Masked",
                icon = Icons.Default.LocationOff,
                iconTint = iOSSystemRed
            )
            IOSDivider()
            IOSListRow(
                title = "100% Offline Processing",
                value = "Zero Network",
                icon = Icons.Default.WifiOff,
                iconTint = iOSSystemTeal
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Quick Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Quick Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                IOSSectionHeader(title = "Theme")
                IOSGroupedCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = settings.themeMode == AppThemeMode.DARK,
                            onClick = { viewModel.setTheme(AppThemeMode.DARK) },
                            label = { Text("Dark OLED") }
                        )
                        FilterChip(
                            selected = settings.themeMode == AppThemeMode.LIGHT,
                            onClick = { viewModel.setTheme(AppThemeMode.LIGHT) },
                            label = { Text("Light") }
                        )
                        FilterChip(
                            selected = settings.themeMode == AppThemeMode.SYSTEM,
                            onClick = { viewModel.setTheme(AppThemeMode.SYSTEM) },
                            label = { Text("System") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                IOSSectionHeader(title = "Storage Maintenance")
                IOSGroupedCard {
                    IOSListRow(
                        title = "Clear Working Cache",
                        icon = Icons.Default.CleaningServices,
                        iconTint = iOSSystemOrange,
                        onClick = {
                            FileUtils.clearTempFiles(context)
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                            showSettingsSheet = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // In-Place Photo Details Bottom Sheet (replaces navigating to separate Inspector screen)
    inspectingItem?.let { item ->
        ModalBottomSheet(
            onDismissRequest = { inspectingItem = null },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cleaned Photo Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { inspectingItem = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = item.cleanedUri ?: item.originalUri,
                        contentDescription = item.fileName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                IOSGroupedCard {
                    IOSListRow(title = "File Name", value = item.fileName, icon = Icons.Default.Description)
                    IOSDivider()
                    IOSListRow(title = "File Size", value = item.formattedSize, icon = Icons.Default.DataUsage)
                    IOSDivider()
                    IOSListRow(title = "GPS Coordinates", value = "Removed", icon = Icons.Default.LocationOff, iconTint = iOSSystemGreen)
                    IOSDivider()
                    IOSListRow(title = "Camera Serial & Model", value = "Removed", icon = Icons.Default.CameraAlt, iconTint = iOSSystemGreen)
                    IOSDivider()
                    IOSListRow(title = "AI / C2PA Footprint", value = "Erased", icon = Icons.Default.AutoAwesome, iconTint = iOSSystemGreen)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
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
