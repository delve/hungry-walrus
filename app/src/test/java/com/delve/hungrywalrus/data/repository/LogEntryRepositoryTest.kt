package com.delve.hungrywalrus.data.repository

import app.cash.turbine.test
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import com.delve.hungrywalrus.data.local.entity.LogEntryEntity
import com.delve.hungrywalrus.domain.model.LogEntry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class LogEntryRepositoryTest {

    private lateinit var dao: LogEntryDao
    private lateinit var repository: LogEntryRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = LogEntryRepositoryImpl(dao)
    }

    @Test
    fun `getEntriesForDate converts date to correct epoch millis range`() = runTest {
        val date = LocalDate.of(2026, 3, 20)
        val expectedStart = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val expectedEnd = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        val entity = LogEntryEntity(
            id = 1,
            foodName = "Apple",
            kcal = 52.0,
            proteinG = 0.3,
            carbsG = 14.0,
            fatG = 0.2,
            timestamp = expectedStart + 3600000L,
        )
        every { dao.getEntriesForDate(expectedStart, expectedEnd) } returns flowOf(listOf(entity))

        repository.getEntriesForDate(date).test {
            val entries = awaitItem()
            assertEquals(1, entries.size)
            assertEquals("Apple", entries[0].foodName)
            assertEquals(52.0, entries[0].kcal, 0.001)
            awaitComplete()
        }
    }

    @Test
    fun `getEntriesForDate returns empty list when no entries exist`() = runTest {
        val date = LocalDate.of(2026, 1, 1)
        every { dao.getEntriesForDate(any(), any()) } returns flowOf(emptyList())

        repository.getEntriesForDate(date).test {
            assertEquals(emptyList<LogEntry>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getEntriesForRange converts dates to correct epoch millis`() = runTest {
        val start = LocalDate.of(2026, 3, 14)
        val end = LocalDate.of(2026, 3, 20)
        val expectedStart = start.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val expectedEnd = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

        every { dao.getEntriesForRange(expectedStart, expectedEnd) } returns flowOf(emptyList())

        repository.getEntriesForRange(start, end).test {
            assertEquals(emptyList<LogEntry>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `addEntry maps domain model to entity and inserts`() = runTest {
        val entitySlot = slot<LogEntryEntity>()
        coEvery { dao.insert(capture(entitySlot)) } returns Unit

        val entry = LogEntry(
            id = 0,
            foodName = "Banana",
            kcal = 89.0,
            proteinG = 1.1,
            carbsG = 22.8,
            fatG = 0.3,
            timestamp = 1710892800000L,
        )

        repository.addEntry(entry)

        val captured = entitySlot.captured
        assertEquals("Banana", captured.foodName)
        assertEquals(89.0, captured.kcal, 0.001)
        assertEquals(1.1, captured.proteinG, 0.001)
        assertEquals(22.8, captured.carbsG, 0.001)
        assertEquals(0.3, captured.fatG, 0.001)
        assertEquals(1710892800000L, captured.timestamp)
    }

    @Test
    fun `deleteEntry calls dao deleteById with correct id`() = runTest {
        coEvery { dao.deleteById(42L) } returns Unit

        repository.deleteEntry(42L)

        coVerify { dao.deleteById(42L) }
    }

}
