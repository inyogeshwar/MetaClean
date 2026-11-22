package com.inyogeshwar.metaclean.data.repository

import com.inyogeshwar.metaclean.data.local.HistoryDao
import com.inyogeshwar.metaclean.data.local.HistoryEntity
import com.inyogeshwar.metaclean.domain.model.HistoryRecord
import com.inyogeshwar.metaclean.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())

    override fun getHistoryRecords(): Flow<List<HistoryRecord>> {
        return historyDao.getAllHistory().map { list ->
            list.map { entity ->
                HistoryRecord(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    formattedDate = dateFormat.format(Date(entity.timestamp)),
                    totalFiles = entity.totalFiles,
                    successCount = entity.successCount,
                    failureCount = entity.failureCount,
                    processingType = entity.processingType,
                    removedTagsSummary = entity.removedTagsSummary,
                    bytesSaved = entity.bytesSaved
                )
            }
        }
    }

    override suspend fun saveHistoryRecord(record: HistoryRecord): Long {
        return historyDao.insertHistory(
            HistoryEntity(
                timestamp = record.timestamp,
                totalFiles = record.totalFiles,
                successCount = record.successCount,
                failureCount = record.failureCount,
                processingType = record.processingType,
                removedTagsSummary = record.removedTagsSummary,
                bytesSaved = record.bytesSaved
            )
        )
    }

    override suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    override suspend fun deleteHistoryRecord(id: Long) {
        historyDao.deleteHistoryById(id)
    }
}
