package com.inyogeshwar.metaclean.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.inyogeshwar.metaclean.core.file.FileUtils
import com.inyogeshwar.metaclean.core.ui.theme.*
import com.inyogeshwar.metaclean.domain.repository.QuickCleanResult

@Composable
fun IOSSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
fun IOSGroupedCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun IOSListRow(
    title: String,
    value: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = iOSSystemBlue,
    isSensitive: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (isSensitive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(iOSSystemRed.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "SENSITIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSSystemRed
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = iOSTextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun IOSDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 16.dp)
    )
}

@Composable
fun IOSBadge(
    text: String,
    color: Color = iOSSystemBlue,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

/**
 * iOS-Style Circular Privacy Shield & Progress Ring
 * Inspired by modern Apple health/utility designs with dynamic gradient ring and metric pills.
 */
@Composable
fun IOSCircularShieldMeter(
    totalCleaned: Int,
    totalBytesSaved: Long,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (totalCleaned > 0) 1.0f else 0.75f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "shield_progress"
    )

    IOSGroupedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Circular Ring Centerpiece
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.12f),
                        style = Stroke(width = strokeWidth)
                    )
                    // Gradient active arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                iOSSystemRed,
                                iOSSystemOrange,
                                iOSSystemGreen,
                                iOSSystemTeal,
                                iOSSystemBlue,
                                iOSSystemIndigo,
                                iOSSystemRed
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (totalCleaned > 0) "$totalCleaned" else "100%",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (totalCleaned > 0) "Photos Cleaned" else "Offline Shield",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (totalBytesSaved > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${FileUtils.formatFileSize(totalBytesSaved)} saved",
                            fontSize = 11.sp,
                            color = iOSSystemTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Three Vibrant Metric Pills (matching reference UI style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IOSMetricPill(
                    icon = Icons.Default.LocationOff,
                    value = "GPS Off",
                    color = iOSSystemRed
                )
                IOSMetricPill(
                    icon = Icons.Default.AutoAwesome,
                    value = "AI Strip",
                    color = iOSSystemTeal
                )
                IOSMetricPill(
                    icon = Icons.Default.CameraAlt,
                    value = "EXIF Clean",
                    color = iOSSystemIndigo
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Zero Location Trace • C2PA / JUMBF Erased • Hardware Masked",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun IOSMetricPill(
    icon: ImageVector,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 1-Tap Quick Clean Progress & Completion Dialog
 */
@Composable
fun IOSQuickCleanModal(
    isRunning: Boolean,
    current: Int,
    total: Int,
    result: QuickCleanResult?,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    if (!isRunning && result == null) return

    Dialog(onDismissRequest = { if (!isRunning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(iOSSystemBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = iOSSystemBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Instant Cleaning...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Scrubbing GPS, Camera & AI signatures ($current of $total)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = iOSSystemBlue,
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                } else if (result != null) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(iOSSystemGreen.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iOSSystemGreen,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Cleaned & Saved!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val destinationDesc = if (result.overwrittenCount > 0) {
                        "${result.overwrittenCount} photos updated in place"
                    } else {
                        "${result.savedToGalleryCount} photos saved directly to Gallery (Pictures/MetaClean)"
                    }

                    Text(
                        text = destinationDesc,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (result.totalBytesSaved > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stripped ${FileUtils.formatFileSize(result.totalBytesSaved)} metadata footprint",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = iOSSystemTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onShare != null && result.savedUris.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onShare,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 13.sp)
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = iOSSystemBlue)
                        ) {
                            Text("Done", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
