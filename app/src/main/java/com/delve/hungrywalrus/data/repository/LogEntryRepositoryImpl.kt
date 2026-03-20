package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import com.delve.hungrywalrus.data.local.entity.LogEntryEntity
import com.delve.hungrywalrus.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class LogEntryRepositoryImpl @Inject constructor(
    private val dao: LogEntryDao,
) : LogEntryRepository {

    override fun getEntriesForDate(date: LocalDate): Flow<List<LogEntry>> {
        val startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return dao.getEntriesForDate(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEntriesForRange(start: LocalDate, end: LocalDate): Flow<List<LogEntry>> {
        val startMillis = start.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return dao.getEntriesForRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addEntry(entry: LogEntry) {
        dao.insert(entry.toEntity())
    }

    override suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    private fun LogEntryEntity.toDomain(): LogEntry {
        return LogEntry(
            id = id,
            foodName = foodName,
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            timestamp = timestamp,
        )
    }

    private fun LogEntry.toEntity(): LogEntryEntity {
        return LogEntryEntity(
            id = id,
            foodName = foodName,
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            timestamp = timestamp,
        )
    }
}
