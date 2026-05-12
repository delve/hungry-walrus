package com.delve.hungrywalrus.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * In-memory Room database tests for [FoodCacheDao].
 *
 * Covers:
 *  - `get(cacheKey)` composite-key lookup (Section 5.3, new method added per architecture)
 *  - `getByBarcode(barcode)` barcode lookup
 *  - `insert` with OnConflictStrategy.REPLACE semantics for stale-entry refresh
 *  - `deleteOlderThan` cache eviction with strict less-than semantics
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FoodCacheDaoTest {

    private lateinit var database: HungryWalrusDatabase
    private lateinit var dao: FoodCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HungryWalrusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.foodCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(
        cacheKey: String,
        name: String = "Some food",
        barcode: String? = null,
        cachedAt: Long = 1000L,
    ): FoodCacheEntity = FoodCacheEntity(
        cacheKey = cacheKey,
        foodName = name,
        kcalPer100g = 100.0,
        proteinPer100g = 5.0,
        carbsPer100g = 15.0,
        fatPer100g = 3.0,
        source = "USDA",
        barcode = barcode,
        cachedAt = cachedAt,
    )

    @Test
    fun `get returns null when cache is empty`() = runTest {
        assertNull(dao.get("usda:1234"))
    }

    @Test
    fun `get returns the entry matching the composite cache key`() = runTest {
        dao.insert(entity(cacheKey = "usda:1234", name = "USDA food"))
        dao.insert(entity(cacheKey = "off:5678", name = "OFF food"))

        val usdaResult = dao.get("usda:1234")
        val offResult = dao.get("off:5678")

        assertEquals("USDA food", usdaResult?.foodName)
        assertEquals("OFF food", offResult?.foodName)
    }

    @Test
    fun `get returns null when no entry matches the key`() = runTest {
        dao.insert(entity(cacheKey = "usda:1234"))

        assertNull(dao.get("usda:9999"))
    }

    @Test
    fun `getByBarcode returns null when no entry has the barcode`() = runTest {
        dao.insert(entity(cacheKey = "off:111", barcode = null))

        assertNull(dao.getByBarcode("9999999"))
    }

    @Test
    fun `getByBarcode returns the entry with the matching barcode`() = runTest {
        dao.insert(entity(cacheKey = "off:111", barcode = "1111111111111", name = "Product A"))
        dao.insert(entity(cacheKey = "off:222", barcode = "2222222222222", name = "Product B"))

        val result = dao.getByBarcode("1111111111111")

        assertEquals("Product A", result?.foodName)
    }

    @Test
    fun `insert with REPLACE strategy overwrites an existing row with the same cacheKey`() = runTest {
        dao.insert(entity(cacheKey = "usda:1", name = "Old name", cachedAt = 1000L))
        dao.insert(entity(cacheKey = "usda:1", name = "New name", cachedAt = 2000L))

        val result = dao.get("usda:1")

        assertEquals("New name", result?.foodName)
        assertEquals(2000L, result?.cachedAt)
    }

    @Test
    fun `deleteOlderThan removes entries strictly older than the threshold`() = runTest {
        dao.insert(entity(cacheKey = "a", cachedAt = 1000L))
        dao.insert(entity(cacheKey = "b", cachedAt = 2000L))
        dao.insert(entity(cacheKey = "c", cachedAt = 3000L))

        dao.deleteOlderThan(2000L)

        assertNull(dao.get("a"))
        // Entry at exactly the threshold is preserved (cachedAt < threshold semantics).
        assertEquals(2000L, dao.get("b")?.cachedAt)
        assertEquals(3000L, dao.get("c")?.cachedAt)
    }
}
