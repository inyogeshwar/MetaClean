package com.inyogeshwar.metaclean.domain.repository

import com.inyogeshwar.metaclean.domain.model.HistoryRecord
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    fun getHistoryRecords(): Flow<List<HistoryRecord>>
    suspend fun saveHistoryRecord(record: HistoryRecord): Long
    suspend fun clearHistory()
    suspend fun deleteHistoryRecord(id: Long)
}
