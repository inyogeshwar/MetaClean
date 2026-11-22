package com.inyogeshwar.metaclean.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        ProcessingStatus.PENDING -> Triple(SurfaceContainerDark, TextSecondaryDark, "Pending")
        ProcessingStatus.INSPECTING -> Triple(AccentPrimary.copy(alpha = 0.2f), AccentPrimary, "Inspecting")
        ProcessingStatus.PROCESSING -> Triple(AccentSecondary.copy(alpha = 0.2f), AccentSecondary, "Processing")
        ProcessingStatus.VERIFYING -> Triple(AccentTertiary.copy(alpha = 0.2f), AccentTertiary, "Verifying")
        ProcessingStatus.COMPLETED -> Triple(SuccessGreen.copy(alpha = 0.2f), SuccessGreen, "Cleaned")
        ProcessingStatus.FAILED -> Triple(DangerRed.copy(alpha = 0.2f), DangerRed, "Failed")
        ProcessingStatus.CANCELLED -> Triple(WarningAmber.copy(alpha = 0.2f), WarningAmber, "Cancelled")
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
