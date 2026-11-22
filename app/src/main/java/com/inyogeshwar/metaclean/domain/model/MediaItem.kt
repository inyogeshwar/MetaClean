package com.inyogeshwar.metaclean.domain.model

import android.net.Uri
import com.inyogeshwar.metaclean.data.model.ImageMetadata

data class MediaItem(
    val id: String,
    val originalUri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val formattedSize: String,
    val mimeType: String,
    val width: Int = 0,
    val height: Int = 0,
    val status: ProcessingStatus = ProcessingStatus.PENDING,
    val progress: Float = 0f,
    val originalMetadata: ImageMetadata? = null,
    val cleanedUri: Uri? = null,
    val cleanedMetadata: ImageMetadata? = null,
    val verificationResult: VerificationResult? = null,
    val errorMessage: String? = null,
    val isSelected: Boolean = true
)
