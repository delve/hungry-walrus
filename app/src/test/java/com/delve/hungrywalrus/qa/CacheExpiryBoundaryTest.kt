package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffBarcodeResponse
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService
import com.delve.hungrywalrus.data.repository.FoodLookupRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * QA tests for the cache expiry boundary in [FoodLookupRepositoryImpl].
 *
 * Gap filled:
 * The repository's `isCacheExpired` method uses strict greater-than:
 *   `System.currentTimeMillis() - cachedAt > CACHE_DURATION_MILLIS`  (30 days)
 *
 * The existing tests cover:
 *   - 31 days old → expired (triggers network)
 *   - 1 day old → not expired (served from cache)
 *
 * These tests add:
 *   - An entry just under 30 days old (29 days 23 hours 55 minutes) must NOT be expired.
 *   - An entry just over 30 days old (30 days + 1 minute) IS expired and triggers network.
 *
 * This verifies the contract that CACHE_DURATION_MILLIS is an exclusive upper bound:
 * items are evicted only when they are STRICTLY OLDER than 30 days.
 */
class CacheExpiryBoundaryTest {

    private lateinit var usdaApiService: UsdaApiService
    private lateinit var offApiService: OffApiService
    private lateinit var foodCacheDao: FoodCacheDao
    private lateinit var repository: FoodLookupRepositoryImpl

    @Before
    fun setUp() {
        usdaApiService = mockk()
        offApiService = mockk()
        foodCacheDao = mockk(relaxed = true)
        repository = FoodLookupRepositoryImpl(usdaApiService, offApiService, foodCacheDao)
    }

    /**
     * A cache entry stored 5 minutes short of 30 days ago must NOT be treated as expired.
     * The 5-minute margin prevents test flakiness from clock drift between test setup
     * and the repository's internal `isCacheExpired` call.
     *
     * Architecture §5.4: cache duration is 30 days. An item stored 29 days 23 hours
     * 55 minutes ago is within the window and must be served from cache.
     */
    @Test
    fun `cache entry 5 minutes short of 30 days is not expired and serves from cache`() = runTest {
        val almostThirtyDays = TimeUnit.DAYS.toMillis(30) - TimeUnit.MINUTES.toMillis(5)
        val cachedAtNearBoundary = System.currentTimeMillis() - almostThirtyDays

        val nearBoundaryEntry = FoodCacheEntity(
            cacheKey = "off:nearboundary",
            foodName = "Near Boundary Product",
            kcalPer100g = 250.0,
            proteinPer100g = 12.0,
            carbsPer100g = 35.0,
            fatPer100g = 8.0,
            source = "OFF",
            barcode = "nearboundary",
            cachedAt = cachedAtNearBoundary,
        )
        coEvery { foodCacheDao.getByBarcode("nearboundary") } returns nearBoundaryEntry

        val result = repository.lookupBarcode("nearboundary")

        // Served from cache — network not called
        assertTrue(result.isSuccess)
        assertEquals("Near Boundary Product", result.getOrNull()?.name)
        coVerify(exactly = 0) { offApiService.getProductByBarcode(any()) }
    }

    /**
     * A cache entry stored 1 minute past 30 days ago IS expired and triggers a network call.
     * Architecture §5.4: the expiry is exclusive, so items > 30 days old are evicted.
     */
    @Test
    fun `cache entry 1 minute past 30 days triggers network lookup`() = runTest {
        val justOverThirtyDays = TimeUnit.DAYS.toMillis(30) + TimeUnit.MINUTES.toMillis(1)
        val expiredCachedAt = System.currentTimeMillis() - justOverThirtyDays

        val expiredEntry = FoodCacheEntity(
            cacheKey = "off:justexpired",
            foodName = "Just Expired Product",
            kcalPer100g = 100.0,
            proteinPer100g = 5.0,
            carbsPer100g = 15.0,
            fatPer100g = 3.0,
            source = "OFF",
            barcode = "justexpired",
            cachedAt = expiredCachedAt,
        )
        coEvery { foodCacheDao.getByBarcode("justexpired") } returns expiredEntry
        // Simulate product not found on network (status = 0)
        coEvery { offApiService.getProductByBarcode("justexpired") } returns
            OffBarcodeResponse(status = 0, product = null)

        val result = repository.lookupBarcode("justexpired")

        // Cache was expired: network was called
        coVerify(exactly = 1) { offApiService.getProductByBarcode("justexpired") }
        // status=0 => null result (not found, not error)
        assertTrue(result.isSuccess)
    }

    /**
     * An entry stored exactly 29 days ago (well within the 30-day window) must also be
     * served from cache — this is a sanity check anchoring the lower end of the boundary.
     */
    @Test
    fun `cache entry 29 days old is clearly not expired`() = runTest {
        val twentyNineDays = TimeUnit.DAYS.toMillis(29)
        val recentCachedAt = System.currentTimeMillis() - twentyNineDays

        val recentEntry = FoodCacheEntity(
            cacheKey = "off:recent29",
            foodName = "Recent Product",
            kcalPer100g = 200.0,
            proteinPer100g = 10.0,
            carbsPer100g = 25.0,
            fatPer100g = 7.0,
            source = "OFF",
            barcode = "recent29",
            cachedAt = recentCachedAt,
        )
        coEvery { foodCacheDao.getByBarcode("recent29") } returns recentEntry

        val result = repository.lookupBarcode("recent29")

        // Served from cache
        assertTrue(result.isSuccess)
        assertEquals("Recent Product", result.getOrNull()?.name)
        coVerify(exactly = 0) { offApiService.getProductByBarcode(any()) }
    }
}
