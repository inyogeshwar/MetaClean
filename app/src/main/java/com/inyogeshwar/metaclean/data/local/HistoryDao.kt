package com.inyogeshwar.metaclean.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM processing_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: HistoryEntity): Long

    @Query("DELETE FROM processing_history")
    suspend fun clearAllHistory(): Int

    @Query("DELETE FROM processing_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long): Int
}
