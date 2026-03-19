package com.delve.hungrywalrus.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker that handles data retention:
 * 1. Deletes LogEntry rows older than 2 years (730 days).
 * 2. Deletes FoodCache rows older than 30 days.
 *
 * Runs every 24 hours with a 1-hour initial delay.
 * Enqueued from [com.delve.hungrywalrus.HungryWalrusApp.onCreate].
 */
@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val logEntryDao: LogEntryDao,
    private val foodCacheDao: FoodCacheDao,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()

            // Delete log entries older than 730 days (2 years)
            val logRetentionThreshold = now - LOG_RETENTION_DAYS * MILLIS_PER_DAY
            logEntryDao.deleteOlderThan(logRetentionThreshold)

            // Delete cached food data older than 30 days
            val cacheRetentionThreshold = now - CACHE_RETENTION_DAYS * MILLIS_PER_DAY
            foodCacheDao.deleteOlderThan(cacheRetentionThreshold)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val LOG_RETENTION_DAYS = 730L
        private const val CACHE_RETENTION_DAYS = 30L
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}
