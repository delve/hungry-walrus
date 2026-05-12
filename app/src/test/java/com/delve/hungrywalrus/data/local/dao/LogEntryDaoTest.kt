package com.delve.hungrywalrus.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase
import com.delve.hungrywalrus.data.local.entity.LogEntryEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

/**
 * In-memory Room database tests for [LogEntryDao].
 *
 * The architecture (Section 19, Revision 1 item 13) recommends these for verifying
 * SQL query correctness, particularly day-boundary queries. Day-boundary queries
 * use a half-open `[startOfDay, endOfDay)` range, so an entry at exactly the start
 * of the next day must NOT be included in the previous day's results.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LogEntryDaoTest {

    private lateinit var database: HungryWalrusDatabase
    private lateinit var dao: LogEntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HungryWalrusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.logEntryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun millisAtStartOfDay(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun entry(timestamp: Long, name: String = "Apple"): LogEntryEntity =
        LogEntryEntity(
            foodName = name,
            kcal = 52.0,
            proteinG = 0.3,
            carbsG = 14.0,
            fatG = 0.2,
            timestamp = timestamp,
        )

    @Test
    fun `getEntriesForDate excludes entry at exactly the end-of-day boundary`() = runTest {
        val target = LocalDate.of(2026, 3, 15)
        val startOfDay = millisAtStartOfDay(target)
        val endOfDay = millisAtStartOfDay(target.plusDays(1))

        dao.insert(entry(startOfDay, "inside-start"))
        dao.insert(entry(endOfDay - 1, "inside-last-millis"))
        dao.insert(entry(endOfDay, "outside-next-day"))

        dao.getEntriesForDate(startOfDay, endOfDay).test {
            val items = awaitItem()
            val names = items.map { it.foodName }.toSet()
            assertEquals(setOf("inside-start", "inside-last-millis"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getEntriesForDate excludes entry from previous day at exactly one millisecond before start`() = runTest {
        val target = LocalDate.of(2026, 3, 15)
        val startOfDay = millisAtStartOfDay(target)
        val endOfDay = millisAtStartOfDay(target.plusDays(1))

        dao.insert(entry(startOfDay - 1, "yesterday-last-millis"))
        dao.insert(entry(startOfDay, "today-first-millis"))

        dao.getEntriesForDate(startOfDay, endOfDay).test {
            val items = awaitItem()
            val names = items.map { it.foodName }.toSet()
            assertEquals(setOf("today-first-millis"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getEntriesForDate returns entries in descending timestamp order`() = runTest {
        val target = LocalDate.of(2026, 3, 15)
        val startOfDay = millisAtStartOfDay(target)
        val endOfDay = millisAtStartOfDay(target.plusDays(1))

        dao.insert(entry(startOfDay + 1000L, "early"))
        dao.insert(entry(startOfDay + 5000L, "late"))
        dao.insert(entry(startOfDay + 3000L, "middle"))

        dao.getEntriesForDate(startOfDay, endOfDay).test {
            val items = awaitItem()
            assertEquals(listOf("late", "middle", "early"), items.map { it.foodName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getEntriesForRange spans multiple days inclusively at start and exclusively at end`() = runTest {
        val day1 = LocalDate.of(2026, 3, 15)
        val day2 = day1.plusDays(1)
        val day3 = day1.plusDays(2)

        dao.insert(entry(millisAtStartOfDay(day1) + 1000L, "day1"))
        dao.insert(entry(millisAtStartOfDay(day2) + 1000L, "day2"))
        dao.insert(entry(millisAtStartOfDay(day3), "day3-boundary"))

        // [day1 start, day3 start) should include day1 and day2 entries only.
        dao.getEntriesForRange(millisAtStartOfDay(day1), millisAtStartOfDay(day3)).test {
            val items = awaitItem()
            val names = items.map { it.foodName }.toSet()
            assertEquals(setOf("day1", "day2"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteById removes only the specified entry`() = runTest {
        dao.insert(entry(1000L, "keep1"))
        dao.insert(entry(2000L, "delete-me"))
        dao.insert(entry(3000L, "keep2"))

        val all = mutableListOf<LogEntryEntity>()
        dao.getEntriesForRange(0L, Long.MAX_VALUE).test {
            all.addAll(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val targetId = all.first { it.foodName == "delete-me" }.id

        dao.deleteById(targetId)

        dao.getEntriesForRange(0L, Long.MAX_VALUE).test {
            val remaining = awaitItem().map { it.foodName }.toSet()
            assertEquals(setOf("keep1", "keep2"), remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteOlderThan removes entries strictly older than the threshold`() = runTest {
        dao.insert(entry(1000L, "very-old"))
        dao.insert(entry(2000L, "threshold-boundary"))
        dao.insert(entry(3000L, "new"))

        // Threshold is 2000 -> only timestamps strictly less than 2000 are deleted.
        dao.deleteOlderThan(2000L)

        dao.getEntriesForRange(0L, Long.MAX_VALUE).test {
            val remaining = awaitItem().map { it.foodName }.toSet()
            assertTrue(
                "Entry at threshold value 2000 must NOT be deleted (strict less-than semantics)",
                "threshold-boundary" in remaining,
            )
            assertTrue("threshold-boundary" in remaining && "new" in remaining)
            assertEquals(setOf("threshold-boundary", "new"), remaining)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `auto-generated id is unique per insertion`() = runTest {
        dao.insert(entry(1000L, "a"))
        dao.insert(entry(2000L, "b"))

        dao.getEntriesForRange(0L, Long.MAX_VALUE).test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals(2, items.map { it.id }.toSet().size)
            assertTrue(items.all { it.id > 0L })
            cancelAndIgnoreRemainingEvents()
        }
    }
}
