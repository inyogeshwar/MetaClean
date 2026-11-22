package com.inyogeshwar.metaclean.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inyogeshwar.metaclean.core.ui.theme.*
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
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iOSSystemBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        tint = iOSSystemBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Scrubbing Options",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSTextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select metadata attributes to strip from copies. Originals will never be modified.",
                    fontSize = 12.sp,
                    color = iOSTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                IOSSwitchRow("GPS & Geolocation", "Latitude, longitude, altitude", removeGps) { removeGps = it }
                IOSSwitchRow("Camera Hardware", "Make, model, lens, serial", removeCamera) { removeCamera = it }
                IOSSwitchRow("Timestamps & Dates", "Capture date, digitize time", removeTime) { removeTime = it }
                IOSSwitchRow("Author & Personal", "Artist name, copyright, comments", removeAuthor) { removeAuthor = it }
                IOSSwitchRow("Cached Thumbnails", "Embedded preview thumbnails", removeThumbnails) { removeThumbnails = it }
                IOSSwitchRow("XMP, IPTC & AI Markers", "C2PA, Adobe tags, keywords", removeXmp) { removeXmp = it }
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
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
            ) {
                Text("Start Scrub", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = iOSSystemBlue)
            }
        },
        containerColor = iOSCardDark,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun IOSSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary)
            Text(subtitle, fontSize = 11.sp, color = iOSTextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = iOSSystemGreen)
        )
    }
}
