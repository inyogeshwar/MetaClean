package com.inyogeshwar.metaclean.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.model.ProcessingStatus

@Composable
fun StatusBadge(
    status: ProcessingStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, text) = when (status) {
        ProcessingStatus.PENDING -> Triple(iOSCardElevated, iOSTextSecondary, "Pending")
        ProcessingStatus.INSPECTING -> Triple(iOSSystemBlue.copy(alpha = 0.2f), iOSSystemBlue, "Inspecting")
        ProcessingStatus.PROCESSING -> Triple(iOSSystemTeal.copy(alpha = 0.2f), iOSSystemTeal, "Scrubbing")
        ProcessingStatus.VERIFYING -> Triple(iOSSystemIndigo.copy(alpha = 0.2f), iOSSystemIndigo, "Verifying")
        ProcessingStatus.COMPLETED -> Triple(iOSSystemGreen.copy(alpha = 0.2f), iOSSystemGreen, "Cleaned")
        ProcessingStatus.FAILED -> Triple(iOSSystemRed.copy(alpha = 0.2f), iOSSystemRed, "Failed")
        ProcessingStatus.CANCELLED -> Triple(iOSSystemOrange.copy(alpha = 0.2f), iOSSystemOrange, "Cancelled")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.5.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
