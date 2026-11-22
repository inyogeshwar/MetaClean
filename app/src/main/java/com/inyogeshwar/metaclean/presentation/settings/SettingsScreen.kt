package com.inyogeshwar.metaclean.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.inyogeshwar.metaclean.core.ui.components.*
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.repository.AppThemeMode
import com.inyogeshwar.metaclean.domain.repository.ExportFormat

@OptIn(ExperimentalMaterial3Api::class)
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
            title = { Text("Output Filename Suffix", color = iOSTextPrimary) },
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
            containerColor = iOSCardDark
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All History?", color = iOSTextPrimary) },
            text = { Text("This will permanently remove the log of past processing sessions from this device.", color = iOSTextSecondary) },
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
            containerColor = iOSCardDark
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackgroundDark)
    ) {
        // iOS Navigation Header
        Text(
            text = "Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = iOSTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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

            // Workflow & Storage
            IOSSectionHeader(title = "Storage & Automation")
            IOSGroupedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Confirm Before Scrubbing", fontSize = 15.sp, color = iOSTextPrimary)
                    Switch(
                        checked = settings.confirmBeforeProcessing,
                        onCheckedChange = { viewModel.setConfirmBeforeProcessing(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSSystemGreen)
                    )
                }

                IOSDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-purge Working Cache", fontSize = 15.sp, color = iOSTextPrimary)
                    Switch(
                        checked = settings.autoClearTempFiles,
                        onCheckedChange = { viewModel.setAutoClearTemp(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSSystemGreen)
                    )
                }

                IOSDivider()

                IOSListRow(
                    title = "Clear Cache Now",
                    value = "Free Space",
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
                    value = "Purge",
                    icon = Icons.Default.DeleteForever,
                    iconTint = iOSSystemRed,
                    onClick = { showClearHistoryConfirm = true }
                )
            }

            // AI Footprint & Disclosures
            IOSSectionHeader(title = "Honest Disclosures")
            IOSGroupedCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("• EXIF & Location: 100% removed from JPEG and WebP.", fontSize = 12.sp, color = iOSTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• PNG: Lossless filtering strips textual and EXIF chunks without re-encoding pixels.", fontSize = 12.sp, color = iOSTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• AI Provenance & C2PA: Informational & read-only detection.", fontSize = 12.sp, color = iOSTextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Safety: Original files are never modified.", fontSize = 12.sp, color = iOSTextSecondary)
                }
            }

            // About
            IOSSectionHeader(title = "About")
            IOSGroupedCard {
                IOSListRow(title = "App Version", value = "1.0.0 (Native)", icon = Icons.Default.Info, iconTint = iOSSystemBlue)
                IOSDivider()
                IOSListRow(title = "Engine", value = "Kotlin • Jetpack Compose", icon = Icons.Default.Code, iconTint = iOSSystemIndigo)
                IOSDivider()
                IOSListRow(title = "Offline Security", value = "100% On-Device", icon = Icons.Default.Shield, iconTint = iOSSystemGreen)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
