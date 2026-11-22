package com.inyogeshwar.metaclean.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processing_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val totalFiles: Int,
    val successCount: Int,
    val failureCount: Int,
    val processingType: String,
    val removedTagsSummary: String,
    val bytesSaved: Long
)
