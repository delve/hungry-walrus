package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface LogEntryRepository {
    fun getEntriesForDate(date: LocalDate): Flow<List<LogEntry>>
    fun getEntriesForRange(start: LocalDate, end: LocalDate): Flow<List<LogEntry>>
    suspend fun addEntry(entry: LogEntry)
    suspend fun deleteEntry(id: Long)
}
