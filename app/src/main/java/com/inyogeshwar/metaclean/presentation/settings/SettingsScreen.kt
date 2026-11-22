package com.inyogeshwar.metaclean.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inyogeshwar.metaclean.core.ui.components.*
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.repository.AppThemeMode
import com.inyogeshwar.metaclean.domain.repository.ExportFormat
import com.inyogeshwar.metaclean.domain.repository.SaveDestinationMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    var showNamingDialog by remember { mutableStateOf(false) }
    var tempSuffix by remember { mutableStateOf(settings.outputNamingSuffix) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    if (showNamingDialog) {
        AlertDialog(
            onDismissRequest = { showNamingDialog = false },
            title = { Text("Output File Suffix", color = MaterialTheme.colorScheme.onBackground) },
            text = {
                OutlinedTextField(
                    value = tempSuffix,
                    onValueChange = { tempSuffix = it },
                    label = { Text("Suffix (e.g. _clean)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setNamingSuffix(tempSuffix)
                        showNamingDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNamingDialog = false }) {
                    Text("Cancel", color = iOSSystemBlue)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All History?", color = MaterialTheme.colorScheme.onBackground) },
            text = { Text("This will permanently remove the log of past processing sessions from this device.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryConfirm = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSSystemRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel", color = iOSSystemBlue)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // iOS Navigation Header
        Text(
            text = "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Workflow & Save Options (User preference for instant save & destination)
            IOSSectionHeader(title = "Save & Workflow")
            IOSGroupedCard {
                var saveModeExpanded by remember { mutableStateOf(false) }
                val currentSaveModeDesc = when (settings.saveDestinationMode) {
                    SaveDestinationMode.GALLERY_ALBUM -> "Gallery (Pictures/MetaClean)"
                    SaveDestinationMode.OVERWRITE_ORIGINAL -> "Overwrite Original File"
                }

                IOSListRow(
                    title = "Save Destination",
                    value = currentSaveModeDesc,
                    icon = Icons.Default.Save,
                    iconTint = iOSSystemBlue,
                    showChevron = true,
                    onClick = { saveModeExpanded = true }
                )
                DropdownMenu(
                    expanded = saveModeExpanded,
                    onDismissRequest = { saveModeExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Save to Gallery (Recommended)", fontWeight = FontWeight.SemiBold)
                                Text("Saves to Pictures/MetaClean; leaves original safe", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            viewModel.setSaveDestinationMode(SaveDestinationMode.GALLERY_ALBUM)
                            saveModeExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Overwrite Original File", fontWeight = FontWeight.SemiBold, color = iOSSystemOrange)
                                Text("Replaces original file in place where supported", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            viewModel.setSaveDestinationMode(SaveDestinationMode.OVERWRITE_ORIGINAL)
                            saveModeExpanded = false
                        }
                    )
                }

                IOSDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto-Clean & Save on Pick",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Instantly scrubs & saves photos as soon as you choose them",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.autoSaveOnPick,
                        onCheckedChange = { viewModel.setAutoSaveOnPick(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = iOSSystemGreen
                        )
                    )
                }
            }

            // Appearance
            IOSSectionHeader(title = "Appearance")
            IOSGroupedCard {
                var expanded by remember { mutableStateOf(false) }
                IOSListRow(
                    title = "App Theme",
                    value = settings.themeMode.name,
                    icon = Icons.Default.DarkMode,
                    iconTint = iOSSystemIndigo,
                    showChevron = true,
                    onClick = { expanded = true }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(text = { Text("Dark (OLED)") }, onClick = { viewModel.setTheme(AppThemeMode.DARK); expanded = false })
                    DropdownMenuItem(text = { Text("Light") }, onClick = { viewModel.setTheme(AppThemeMode.LIGHT); expanded = false })
                    DropdownMenuItem(text = { Text("Follow System") }, onClick = { viewModel.setTheme(AppThemeMode.SYSTEM); expanded = false })
                }
            }

            // Export Options
            IOSSectionHeader(title = "Export & Output Files")
            IOSGroupedCard {
                var formatExpanded by remember { mutableStateOf(false) }
                IOSListRow(
                    title = "Output Format",
                    value = settings.exportFormat.name,
                    icon = Icons.Default.Image,
                    iconTint = iOSSystemBlue,
                    showChevron = true,
                    onClick = { formatExpanded = true }
                )
                DropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false }
                ) {
                    ExportFormat.values().forEach { fmt ->
                        DropdownMenuItem(
                            text = { Text(fmt.name) },
                            onClick = { viewModel.setExportFormat(fmt); formatExpanded = false }
                        )
                    }
                }

                IOSDivider()

                IOSListRow(
                    title = "File Name Suffix",
                    value = settings.outputNamingSuffix,
                    icon = Icons.Default.Edit,
                    iconTint = iOSSystemTeal,
                    showChevron = true,
                    onClick = {
                        tempSuffix = settings.outputNamingSuffix
                        showNamingDialog = true
                    }
                )
            }

            // Storage & Maintenance
            IOSSectionHeader(title = "Storage & Cache")
            IOSGroupedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-purge Working Cache", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                    Switch(
                        checked = settings.autoClearTempFiles,
                        onCheckedChange = { viewModel.setAutoClearTemp(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSSystemGreen)
                    )
                }

                IOSDivider()

                IOSListRow(
                    title = "Clear Cache Now",
                    icon = Icons.Default.CleaningServices,
                    iconTint = iOSSystemOrange,
                    onClick = {
                        viewModel.clearTempFilesNow(context)
                        Toast.makeText(context, "Working cache cleared", Toast.LENGTH_SHORT).show()
                    }
                )

                IOSDivider()

                IOSListRow(
                    title = "Delete History Records",
                    icon = Icons.Default.DeleteForever,
                    iconTint = iOSSystemRed,
                    onClick = { showClearHistoryConfirm = true }
                )
            }

            // Privacy & Architecture
            IOSSectionHeader(title = "About MetaClean")
            IOSGroupedCard {
                IOSListRow(
                    title = "Architecture",
                    value = "Native Kotlin + Compose",
                    icon = Icons.Default.Info,
                    iconTint = iOSSystemTeal
                )
                IOSDivider()
                IOSListRow(
                    title = "Network Access",
                    value = "Zero / 100% Offline",
                    icon = Icons.Default.Security,
                    iconTint = iOSSystemGreen
                )
                IOSDivider()
                IOSListRow(
                    title = "Version",
                    value = "1.0.0 (Release)",
                    icon = Icons.Default.Verified,
                    iconTint = iOSSystemBlue
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
