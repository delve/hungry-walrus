package com.delve.hungrywalrus.integration

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import com.delve.hungrywalrus.worker.DataRetentionWorker
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Integration tests for data retention rules per the requirements spec:
 * - Log entries older than 2 years (730 days) are deleted.
 * - Recipes are retained indefinitely (no deletion of recipes by the worker).
 * - Food cache entries older than 30 days are deleted.
 *
 * These tests verify the [DataRetentionWorker] enforces the correct thresholds
 * against the DAO layer.
 */
class DataRetentionIntegrationTest {

    private lateinit var logEntryDao: LogEntryDao
    private lateinit var foodCacheDao: FoodCacheDao
    private lateinit var context: Context
    private lateinit var params: WorkerParameters

    @Before
    fun setUp() {
        logEntryDao = mockk()
        foodCacheDao = mockk()
        context = mockk(relaxed = true)
        params = mockk(relaxed = true)

        coEvery { logEntryDao.deleteOlderThan(any()) } just Runs
        coEvery { foodCacheDao.deleteOlderThan(any()) } just Runs
    }

    private fun createWorker() = DataRetentionWorker(context, params, logEntryDao, foodCacheDao)

    /**
     * Requirement: "Log entries older than 2 years are automatically deleted."
     * 2 years = 730 days. The threshold passed to the DAO must correspond to
     * (current time - 730 days).
     */
    @Test
    fun `log retention threshold is exactly 730 days in the past`() = runTest {
        val thresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(thresholdSlot)) } just Runs

        val beforeCall = System.currentTimeMillis()
        createWorker().doWork()
        val afterCall = System.currentTimeMillis()

        val twoYearsMillis = TimeUnit.DAYS.toMillis(730)
        val expectedLow = beforeCall - twoYearsMillis
        val expectedHigh = afterCall - twoYearsMillis

        assertTrue(
            "Threshold ${thresholdSlot.captured} should be in range [$expectedLow, $expectedHigh]",
            thresholdSlot.captured in expectedLow..expectedHigh,
        )
    }

    /**
     * Requirement: "Recipes are retained indefinitely."
     * The worker must NOT call any recipe DAO delete method.
     */
    @Test
    fun `worker does not call any recipe deletion method`() = runTest {
        // No RecipeDao is injected into DataRetentionWorker. This test confirms the
        // worker only has access to logEntryDao and foodCacheDao, and calls only
        // deleteOlderThan on each.
        createWorker().doWork()

        // Only log and cache DAOs should be touched
        coVerify(exactly = 1) { logEntryDao.deleteOlderThan(any()) }
        coVerify(exactly = 1) { foodCacheDao.deleteOlderThan(any()) }
    }

    /**
     * Verify: log entry written just over 730 days ago would be eligible for deletion
     * (threshold is in the past, so timestamp < threshold is false for 729-day-old entries
     * and true for 731-day-old entries).
     */
    @Test
    fun `a log entry 731 days old would have timestamp older than deletion threshold`() = runTest {
        val thresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(thresholdSlot)) } just Runs

        createWorker().doWork()

        val millisIn731Days = TimeUnit.DAYS.toMillis(731)
        val entryTimestamp = System.currentTimeMillis() - millisIn731Days

        // entryTimestamp should be LESS than the threshold (older), so it would be deleted
        assertTrue(
            "A 731-day-old entry (timestamp=$entryTimestamp) should be older than threshold " +
                "(${thresholdSlot.captured})",
            entryTimestamp < thresholdSlot.captured,
        )
    }

    /**
     * Verify: a log entry exactly 729 days old would NOT be deleted (it is within the
     * 730-day retention window).
     */
    @Test
    fun `a log entry 729 days old would not be deleted`() = runTest {
        val thresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(thresholdSlot)) } just Runs

        createWorker().doWork()

        val millisIn729Days = TimeUnit.DAYS.toMillis(729)
        val entryTimestamp = System.currentTimeMillis() - millisIn729Days

        // entryTimestamp should be GREATER than the threshold (newer), so it would NOT be deleted
        assertTrue(
            "A 729-day-old entry (timestamp=$entryTimestamp) should be newer than threshold " +
                "(${thresholdSlot.captured})",
            entryTimestamp > thresholdSlot.captured,
        )
    }

    /**
     * Cache retention: food cache items older than 30 days must be deleted.
     */
    @Test
    fun `food cache retention threshold is exactly 30 days in the past`() = runTest {
        val thresholdSlot = slot<Long>()
        coEvery { foodCacheDao.deleteOlderThan(capture(thresholdSlot)) } just Runs

        val beforeCall = System.currentTimeMillis()
        createWorker().doWork()
        val afterCall = System.currentTimeMillis()

        val thirtyDaysMillis = TimeUnit.DAYS.toMillis(30)
        val expectedLow = beforeCall - thirtyDaysMillis
        val expectedHigh = afterCall - thirtyDaysMillis

        assertTrue(
            "Cache threshold ${thresholdSlot.captured} should be in range [$expectedLow, $expectedHigh]",
            thresholdSlot.captured in expectedLow..expectedHigh,
        )
    }

    @Test
    fun `doWork returns success when both DAOs succeed`() = runTest {
        assertEquals(Result.success(), createWorker().doWork())
    }

    @Test
    fun `doWork returns retry when logEntryDao throws`() = runTest {
        coEvery { logEntryDao.deleteOlderThan(any()) } throws RuntimeException("DB locked")
        assertEquals(Result.retry(), createWorker().doWork())
    }

    @Test
    fun `doWork returns retry when foodCacheDao throws`() = runTest {
        coEvery { foodCacheDao.deleteOlderThan(any()) } throws RuntimeException("DB locked")
        assertEquals(Result.retry(), createWorker().doWork())
    }
}
