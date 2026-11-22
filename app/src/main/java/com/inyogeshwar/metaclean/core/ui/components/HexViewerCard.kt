package com.inyogeshwar.metaclean.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inyogeshwar.metaclean.core.ui.theme.AccentSecondary
import com.inyogeshwar.metaclean.core.ui.theme.SuccessGreen
import com.inyogeshwar.metaclean.data.model.TechnicalData

@Composable
fun HexViewerCard(
    technicalData: TechnicalData,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    var copiedHash by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Technical File Hashes",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HashRow("SHA-256", technicalData.sha256, copiedHash == "SHA-256") {
                clipboard.setText(AnnotatedString(technicalData.sha256))
                copiedHash = "SHA-256"
            }
            HashRow("SHA-1", technicalData.sha1, copiedHash == "SHA-1") {
                clipboard.setText(AnnotatedString(technicalData.sha1))
                copiedHash = "SHA-1"
            }
            HashRow("MD5", technicalData.md5, copiedHash == "MD5") {
                clipboard.setText(AnnotatedString(technicalData.md5))
                copiedHash = "MD5"
            }
            HashRow("CRC32", technicalData.crc32, copiedHash == "CRC32") {
                clipboard.setText(AnnotatedString(technicalData.crc32))
                copiedHash = "CRC32"
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "File Header Signature: ${technicalData.magicSignature}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentSecondary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hexadecimal Inspection (First 256 bytes):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = technicalData.hexDumpPreview,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun HashRow(
    algorithm: String,
    hashValue: String,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = algorithm,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = hashValue,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy $algorithm",
                tint = if (isCopied) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
