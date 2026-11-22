package com.inyogeshwar.metaclean.domain.model

data class HistoryRecord(
    val id: Long = 0,
    val timestamp: Long,
    val formattedDate: String,
    val totalFiles: Int,
    val successCount: Int,
    val failureCount: Int,
    val processingType: String,
    val removedTagsSummary: String,
    val bytesSaved: Long
)
