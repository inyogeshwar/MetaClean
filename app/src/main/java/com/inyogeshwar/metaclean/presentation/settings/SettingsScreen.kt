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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
            title = { Text("Output Filename Suffix") },
            text = {
                OutlinedTextField(
                    value = tempSuffix,
                    onValueChange = { tempSuffix = it },
                    label = { Text("Suffix (e.g. _clean, _scrubbed)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setNamingSuffix(tempSuffix)
                    showNamingDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNamingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All History?") },
            text = { Text("This will delete all saved processing session statistics from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryConfirm = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Settings & Privacy",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            SettingsGroup(title = "Appearance & Interface") {
                SettingRow(
                    title = "App Theme",
                    subtitle = when (settings.themeMode) {
                        AppThemeMode.DARK -> "Dark (Default privacy darkroom aesthetic)"
                        AppThemeMode.LIGHT -> "Light"
                        AppThemeMode.SYSTEM -> "Follow Android System"
                    },
                    icon = Icons.Default.DarkMode
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(settings.themeMode.name, color = AccentPrimary)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Dark (Recommended)") },
                                onClick = {
                                    viewModel.setTheme(AppThemeMode.DARK)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Light") },
                                onClick = {
                                    viewModel.setTheme(AppThemeMode.LIGHT)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Follow System") },
                                onClick = {
                                    viewModel.setTheme(AppThemeMode.SYSTEM)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Export Defaults
            SettingsGroup(title = "Export & File Naming") {
                SettingRow(
                    title = "Default Output Format",
                    subtitle = when (settings.exportFormat) {
                        ExportFormat.ORIGINAL -> "Preserve original file format"
                        ExportFormat.JPEG -> "Convert to high-quality JPEG"
                        ExportFormat.PNG -> "Convert to lossless PNG"
                        ExportFormat.WEBP -> "Convert to WebP"
                    },
                    icon = Icons.Default.Image
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text(settings.exportFormat.name, color = AccentPrimary)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            ExportFormat.values().forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text(fmt.name) },
                                    onClick = {
                                        viewModel.setExportFormat(fmt)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

                SettingRow(
                    title = "Output Filename Suffix",
                    subtitle = "Appended to cleaned copies: \"${settings.outputNamingSuffix}\"",
                    icon = Icons.Default.Edit
                ) {
                    TextButton(onClick = {
                        tempSuffix = settings.outputNamingSuffix
                        showNamingDialog = true
                    }) {
                        Text("Edit", color = AccentPrimary)
                    }
                }
            }

            // Workflow Preferences
            SettingsGroup(title = "Workflow & Storage") {
                SettingSwitchRow(
                    title = "Confirm Before Batch Processing",
                    subtitle = "Show options dialog before executing batch clean",
                    checked = settings.confirmBeforeProcessing,
                    onCheckedChange = { viewModel.setConfirmBeforeProcessing(it) }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

                SettingSwitchRow(
                    title = "Auto-clear Cache on Queue Clear",
                    subtitle = "Delete temporary processed working files automatically",
                    checked = settings.autoClearTempFiles,
                    onCheckedChange = { viewModel.setAutoClearTemp(it) }
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

                SettingRow(
                    title = "Purge Working Cache Now",
                    subtitle = "Free temporary storage used during processing",
                    icon = Icons.Default.CleaningServices
                ) {
                    TextButton(onClick = {
                        viewModel.clearTempFilesNow(context)
                        Toast.makeText(context, "Temporary files deleted", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Clear", color = WarningAmber)
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)

                SettingRow(
                    title = "Clear Processing History",
                    subtitle = "Delete all Room database session records",
                    icon = Icons.Default.DeleteForever
                ) {
                    TextButton(onClick = { showClearHistoryConfirm = true }) {
                        Text("Delete", color = DangerRed)
                    }
                }
            }

            // Honest Technical Limitations
            SettingsGroup(title = "Processing Integrity & Limitations") {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Honest Privacy Disclosures", style = MaterialTheme.typography.titleSmall)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    DisclosureItem("EXIF & GPS: 100% removed from JPEG and WebP copies.")
                    DisclosureItem("PNG: Lossless chunk filtering strips textual comments (tEXt/zTXt/iTXt) and eXIf chunks while leaving raw image data untouched.")
                    DisclosureItem("AI Provenance & C2PA: Treated strictly as read-only / informational. MetaClean does not bypass or evade AI provenance labels.")
                    DisclosureItem("Copy Safety: The original file is NEVER modified or overwritten.")
                }
            }

            // About MetaClean
            SettingsGroup(title = "About MetaClean") {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("MetaClean v1.0.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Native Android Image Metadata Privacy Toolkit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Built with Kotlin, Jetpack Compose, Material 3, and Clean Architecture. Completely offline, zero telemetry, zero permissions required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = AccentPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
        action()
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DisclosureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = AccentSecondary, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
