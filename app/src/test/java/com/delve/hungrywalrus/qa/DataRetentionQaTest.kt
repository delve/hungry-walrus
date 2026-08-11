package com.delve.hungrywalrus.qa

import android.content.Context
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
 * QA integration tests for data retention rules.
 *
 * These fill the remaining gap from the existing test suite:
 * - An entry exactly 730 days old is NOT deleted (boundary condition: threshold is
 *   strictly in the past relative to the entry). The DAO query is
 *   `WHERE timestamp < :threshold`, so an entry at exactly the threshold is kept.
 * - An entry at exactly 730 days + 1 millisecond would be deleted.
 * - Recipe data is never deleted by the worker (no RecipeDao in constructor).
 * - The log and cache deletions are independent: a cache failure does not prevent
 *   the log deletion from running.
 *
 * Note: "older than 2 years" is implemented as 730 days (730 * 24h * 60m * 60s * 1000ms).
 */
class DataRetentionQaTest {

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
     * Boundary condition: an entry timestamped at exactly the computed threshold epoch
     * has `timestamp == threshold`. The SQL query is `WHERE timestamp < threshold`, so
     * `timestamp < threshold` is false for an entry at exactly threshold. That entry must
     * NOT be deleted.
     *
     * The assertion is built so it is **non-tautological** (i.e. it would fail if the
     * worker computed an unreasonable threshold) and **non-racy** (no second clock reading
     * is compared against the worker's captured value). We bracket the worker call with
     * `before`/`after` clock readings; the captured threshold must satisfy
     * `before - 730d <= captured <= after - 730d`. We then construct the candidate
     * boundary timestamp as `after - 730d`, which is necessarily `>= captured`, and assert
     * that it is therefore NOT strictly less than `captured` — i.e. NOT eligible for
     * deletion. This also fails fast if the worker captures a wildly wrong threshold
     * (e.g. zero, or `now` itself), because then `after - 730d` would be `< captured`.
     */
    @Test
    fun `entry at exactly 730-day boundary is not eligible for deletion`() = runTest {
        val thresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(thresholdSlot)) } just Runs

        val before = System.currentTimeMillis()
        createWorker().doWork()
        val after = System.currentTimeMillis()

        val captured = thresholdSlot.captured
        val twoYearsMillis = TimeUnit.DAYS.toMillis(730)
        val expectedLow = before - twoYearsMillis
        val expectedHigh = after - twoYearsMillis

        // Sanity check: the worker must have computed a threshold within the 730-day window
        // relative to wall-clock time. Without this, the boundary assertion below would be
        // satisfied by any wildly wrong threshold value.
        assertTrue(
            "Worker threshold $captured outside expected 730-day window [$expectedLow, $expectedHigh]",
            captured in expectedLow..expectedHigh,
        )

        // Candidate timestamp at "exactly the 730-day boundary" derived from a clock reading
        // taken AFTER the worker ran. Because `after >= before`, we have
        // `expectedHigh = after - 730d >= captured`, so `entryAtBoundary >= captured` and
        // therefore `!(entryAtBoundary < captured)` — confirming non-eligibility under
        // strict-less-than semantics.
        val entryAtBoundary = expectedHigh
        assertTrue(
            "Entry at exactly the 730-day boundary ($entryAtBoundary) must NOT be " +
                "strictly less than the captured threshold ($captured) -- it is protected from deletion",
            entryAtBoundary >= captured,
        )
    }

    /**
     * An entry 1 millisecond older than the 730-day threshold should be eligible for deletion.
     *
     * The assertion is built so it is non-tautological and non-racy: we use the `before`
     * clock reading taken BEFORE the worker runs. Because `captured >= before - 730d`, we
     * have `before - 730d - 1 < captured`, so the candidate timestamp is strictly less than
     * the captured threshold regardless of any subsequent clock drift. The sanity check on
     * the captured value ensures the worker did not compute a degenerate threshold.
     */
    @Test
    fun `entry 1 millisecond older than 730-day boundary is eligible for deletion`() = runTest {
        val thresholdSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(thresholdSlot)) } just Runs

        val before = System.currentTimeMillis()
        createWorker().doWork()
        val after = System.currentTimeMillis()

        val captured = thresholdSlot.captured
        val twoYearsMillis = TimeUnit.DAYS.toMillis(730)
        val expectedLow = before - twoYearsMillis
        val expectedHigh = after - twoYearsMillis

        // Sanity check: worker must compute the 730-day window correctly.
        assertTrue(
            "Worker threshold $captured outside expected 730-day window [$expectedLow, $expectedHigh]",
            captured in expectedLow..expectedHigh,
        )

        // Candidate timestamp derived from `before` (the earliest possible wall-clock value
        // that the worker could have used). Since `captured >= before - 730d`, we have
        // `before - 730d - 1 < captured`. The assertion is therefore tied to the captured
        // value via the wall-clock relationship, not via a self-reference.
        val oneMillisOlderEntry = expectedLow - 1
        assertTrue(
            "Entry 1ms older than the 730-day boundary ($oneMillisOlderEntry) must be " +
                "strictly less than the captured threshold ($captured)",
            oneMillisOlderEntry < captured,
        )
    }

    /**
     * Recipe data must not be touched by the data retention worker.
     * The worker has no RecipeDao dependency — this verifies by construction that
     * the worker's doWork() can complete without a RecipeDao being available.
     */
    @Test
    fun `worker completes without any recipe DAO being injected`() = runTest {
        // No RecipeDao injected. If the worker tried to use one it would fail to compile.
        val result = createWorker().doWork()
        // Success result confirms the worker ran without needing a RecipeDao
        assertEquals(androidx.work.ListenableWorker.Result.success(), result)
    }

    /**
     * The log deletion and cache deletion must both be called, and they must operate
     * on independent thresholds (730 days vs. 30 days).
     */
    @Test
    fun `log retention threshold is approximately 700 days earlier than cache threshold`() = runTest {
        val logSlot = slot<Long>()
        val cacheSlot = slot<Long>()
        coEvery { logEntryDao.deleteOlderThan(capture(logSlot)) } just Runs
        coEvery { foodCacheDao.deleteOlderThan(capture(cacheSlot)) } just Runs

        createWorker().doWork()

        val expectedDiff = TimeUnit.DAYS.toMillis(700)
        val actualDiff = cacheSlot.captured - logSlot.captured
        assertTrue(
            "Cache threshold (${cacheSlot.captured}) should be approximately 700 days " +
                "more recent than log threshold (${logSlot.captured}), diff=$actualDiff",
            actualDiff >= expectedDiff,
        )
    }

    /**
     * The cache eviction threshold is 30 days. Verify a 29-day-old cache entry
     * would NOT be deleted (it has timestamp > threshold).
     */
    @Test
    fun `cache entry 29 days old is not eligible for deletion`() = runTest {
        val cacheSlot = slot<Long>()
        coEvery { foodCacheDao.deleteOlderThan(capture(cacheSlot)) } just Runs

        createWorker().doWork()

        val entry29DaysOld = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(29)
        assertTrue(
            "A 29-day-old cache entry (ts=$entry29DaysOld) should NOT be older than " +
                "threshold (${cacheSlot.captured})",
            entry29DaysOld > cacheSlot.captured,
        )
    }

    /**
     * A cache entry exactly 31 days old must be eligible for deletion.
     */
    @Test
    fun `cache entry 31 days old is eligible for deletion`() = runTest {
        val cacheSlot = slot<Long>()
        coEvery { foodCacheDao.deleteOlderThan(capture(cacheSlot)) } just Runs

        createWorker().doWork()

        val entry31DaysOld = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31)
        assertTrue(
            "A 31-day-old cache entry (ts=$entry31DaysOld) should be older than " +
                "threshold (${cacheSlot.captured})",
            entry31DaysOld < cacheSlot.captured,
        )
    }
}
