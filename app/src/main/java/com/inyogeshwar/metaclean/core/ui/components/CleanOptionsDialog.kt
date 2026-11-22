package com.inyogeshwar.metaclean.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.inyogeshwar.metaclean.core.ui.theme.AccentPrimary
import com.inyogeshwar.metaclean.domain.model.CleanOptions

@Composable
fun CleanOptionsDialog(
    initialOptions: CleanOptions = CleanOptions.ALL_PRIVACY,
    onDismiss: () -> Unit,
    onConfirm: (CleanOptions) -> Unit
) {
    var removeGps by remember { mutableStateOf(initialOptions.removeGps) }
    var removeCamera by remember { mutableStateOf(initialOptions.removeCameraDetails) }
    var removeTime by remember { mutableStateOf(initialOptions.removeTimestamps) }
    var removeAuthor by remember { mutableStateOf(initialOptions.removeAuthorAndContact) }
    var removeThumbnails by remember { mutableStateOf(initialOptions.removeThumbnails) }
    var removeXmp by remember { mutableStateOf(initialOptions.removeXmpAndIptc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Privacy Clean Options",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select privacy attributes to sanitize from copies. Originals will never be modified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OptionSwitchRow("GPS Coordinates & Location", "Lat/Lon, altitude, location name", removeGps) {
                    removeGps = it
                }
                OptionSwitchRow("Camera & Device Model", "Make, model, lens, serial numbers", removeCamera) {
                    removeCamera = it
                }
                OptionSwitchRow("Timestamps & Dates", "Capture date, digitize time, subseconds", removeTime) {
                    removeTime = it
                }
                OptionSwitchRow("Author & Personal Info", "Artist name, copyright, comments", removeAuthor) {
                    removeAuthor = it
                }
                OptionSwitchRow("Embedded Thumbnails", "Cached preview thumbnails in EXIF", removeThumbnails) {
                    removeThumbnails = it
                }
                OptionSwitchRow("XMP & IPTC Textual Chunks", "Adobe XMP, keywords, captions", removeXmp) {
                    removeXmp = it
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        CleanOptions(
                            removeGps = removeGps,
                            removeCameraDetails = removeCamera,
                            removeTimestamps = removeTime,
                            removeAuthorAndContact = removeAuthor,
                            removeThumbnails = removeThumbnails,
                            removeXmpAndIptc = removeXmp,
                            preserveDimensions = true
                        )
                    )
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Start Scrub")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun OptionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
