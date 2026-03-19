package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.LogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Query("SELECT * FROM log_entry WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    fun getEntriesForDate(startOfDay: Long, endOfDay: Long): Flow<List<LogEntryEntity>>

    @Query("SELECT * FROM log_entry WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getEntriesForRange(start: Long, end: Long): Flow<List<LogEntryEntity>>

    @Insert
    suspend fun insert(entry: LogEntryEntity)

    @Query("DELETE FROM log_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM log_entry WHERE timestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
