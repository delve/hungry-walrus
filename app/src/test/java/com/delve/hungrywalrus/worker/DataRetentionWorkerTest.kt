package com.delve.hungrywalrus.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
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
 * Unit tests for [DataRetentionWorker] verifying:
 * - Log entries older than 730 days (2 years) are deleted.
 * - Food cache entries older than 30 days are deleted.
 * - doWork returns [Result.success] on success.
 * - doWork returns [Result.retry] when a DAO throws.
 */
class DataRetentionWorkerTest {

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

    @Test
    fun `doWork returns success when both DAOs succeed`() = runTest {
        val worker = createWorker()
        val result = worker.doWork()
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork passes log retention threshold of approximately 730 days ago`() = runTest {
        val logThresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(logThresholdSlot)) } just Runs

        val before = System.currentTimeMillis()
        createWorker().doWork()
        val after = System.currentTimeMillis()

        val expectedLow = before - TimeUnit.DAYS.toMillis(730)
        val expectedHigh = after - TimeUnit.DAYS.toMillis(730)
        val captured = logThresholdSlot.captured
        assertTrue(
            "Log retention threshold $captured should be ~730 days ago, " +
                "expected range [$expectedLow, $expectedHigh]",
            captured in expectedLow..expectedHigh,
        )
    }

    @Test
    fun `doWork passes cache retention threshold of approximately 30 days ago`() = runTest {
        val cacheThresholdSlot = slot<Long>()
        coEvery { foodCacheDao.deleteOlderThan(capture(cacheThresholdSlot)) } just Runs

        val before = System.currentTimeMillis()
        createWorker().doWork()
        val after = System.currentTimeMillis()

        val expectedLow = before - TimeUnit.DAYS.toMillis(30)
        val expectedHigh = after - TimeUnit.DAYS.toMillis(30)
        val captured = cacheThresholdSlot.captured
        assertTrue(
            "Cache retention threshold $captured should be ~30 days ago, " +
                "expected range [$expectedLow, $expectedHigh]",
            captured in expectedLow..expectedHigh,
        )
    }

    @Test
    fun `doWork calls logEntryDao deleteOlderThan exactly once`() = runTest {
        createWorker().doWork()
        coVerify(exactly = 1) { logEntryDao.deleteOlderThan(any()) }
    }

    @Test
    fun `doWork calls foodCacheDao deleteOlderThan exactly once`() = runTest {
        createWorker().doWork()
        coVerify(exactly = 1) { foodCacheDao.deleteOlderThan(any()) }
    }

    @Test
    fun `doWork returns retry when logEntryDao throws`() = runTest {
        coEvery { logEntryDao.deleteOlderThan(any()) } throws RuntimeException("DB error")
        val result = createWorker().doWork()
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `doWork returns retry when foodCacheDao throws`() = runTest {
        coEvery { foodCacheDao.deleteOlderThan(any()) } throws RuntimeException("DB error")
        val result = createWorker().doWork()
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `log retention threshold is greater than cache retention threshold`() = runTest {
        val logThresholdSlot = slot<Long>()
        val cacheThresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(logThresholdSlot)) } just Runs
        coEvery { foodCacheDao.deleteOlderThan(capture(cacheThresholdSlot)) } just Runs

        createWorker().doWork()

        // Log threshold is further in the past (smaller millis value) than cache threshold
        assertTrue(
            "Log threshold (${logThresholdSlot.captured}) should be earlier than " +
                "cache threshold (${cacheThresholdSlot.captured})",
            logThresholdSlot.captured < cacheThresholdSlot.captured,
        )
    }
}
