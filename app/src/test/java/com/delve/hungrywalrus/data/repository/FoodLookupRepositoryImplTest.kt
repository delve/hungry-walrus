package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffBarcodeResponse
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffNutriments
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffProduct
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffSearchResponse
import com.delve.hungrywalrus.domain.OfflineException
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService
import com.delve.hungrywalrus.data.remote.usda.UsdaFood
import com.delve.hungrywalrus.data.remote.usda.UsdaNutrient
import com.delve.hungrywalrus.data.remote.usda.UsdaSearchResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

class FoodLookupRepositoryImplTest {

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

    private fun freshCacheEntity(barcode: String = "1234567890") = FoodCacheEntity(
        cacheKey = "off:$barcode",
        foodName = "Cached Product",
        kcalPer100g = 100.0,
        proteinPer100g = 5.0,
        carbsPer100g = 15.0,
        fatPer100g = 3.0,
        source = "OFF",
        barcode = barcode,
        cachedAt = System.currentTimeMillis(),
    )

    // --- lookupBarcode ---

    @Test
    fun `lookupBarcode returns cached result without network call when cache is fresh`() = runTest {
        coEvery { foodCacheDao.getByBarcode("1234567890") } returns freshCacheEntity()

        val result = repository.lookupBarcode("1234567890")

        assertTrue(result.isSuccess)
        assertEquals("Cached Product", result.getOrNull()?.name)
        coVerify(exactly = 0) { offApiService.getProductByBarcode(any()) }
    }

    @Test
    fun `lookupBarcode returns success null when API returns status 0`() = runTest {
        coEvery { foodCacheDao.getByBarcode("0000000000") } returns null
        coEvery { offApiService.getProductByBarcode("0000000000") } returns OffBarcodeResponse(status = 0, product = null)

        val result = repository.lookupBarcode("0000000000")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `lookupBarcode returns expired cache when device is offline`() = runTest {
        val expiredEntity = FoodCacheEntity(
            cacheKey = "off:expired",
            foodName = "Stale Product",
            kcalPer100g = 200.0,
            proteinPer100g = 10.0,
            carbsPer100g = 25.0,
            fatPer100g = 8.0,
            source = "OFF",
            barcode = "expired",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31),
        )
        coEvery { foodCacheDao.getByBarcode("expired") } returns expiredEntity
        coEvery { offApiService.getProductByBarcode("expired") } throws IOException("offline")

        val result = repository.lookupBarcode("expired")

        assertTrue(result.isSuccess)
        assertEquals("Stale Product", result.getOrNull()?.name)
    }

    // --- searchUsda ---

    @Test
    fun `searchUsda returns failure with OfflineException on IOException`() = runTest {
        coEvery { usdaApiService.searchFoods(query = any()) } throws IOException("Network unreachable")

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineException)
    }

    @Test
    fun `searchUsda returns failure with invalid API key message on HTTP 403`() = runTest {
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 403
        coEvery { usdaApiService.searchFoods(query = any()) } throws httpException

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Invalid API key", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchUsda returns failure with generic message on unexpected Exception`() = runTest {
        coEvery { usdaApiService.searchFoods(query = any()) } throws RuntimeException("unexpected")

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Could not read food data", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchUsda returns failure with too many requests message on HTTP 429`() = runTest {
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 429
        coEvery { usdaApiService.searchFoods(query = any()) } throws httpException

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Too many requests, please try again later", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchUsda returns mapped results without caching`() = runTest {
        val response = UsdaSearchResponse(
            foods = listOf(
                UsdaFood(
                    fdcId = 123L,
                    description = "Chicken breast",
                    foodNutrients = listOf(
                        UsdaNutrient(nutrientId = 1008, value = 165.0),
                        UsdaNutrient(nutrientId = 1003, value = 31.0),
                        UsdaNutrient(nutrientId = 1005, value = 0.0),
                        UsdaNutrient(nutrientId = 1004, value = 3.6),
                    ),
                ),
            ),
        )
        coEvery { usdaApiService.searchFoods(query = "chicken") } returns response

        val result = repository.searchUsda("chicken")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        coVerify(exactly = 0) { foodCacheDao.insert(any()) }
    }

    // --- searchOpenFoodFacts ---

    @Test
    fun `searchOpenFoodFacts returns mapped results without caching`() = runTest {
        val response = OffSearchResponse(
            products = listOf(
                OffProduct(
                    code = "111",
                    productName = "Test Biscuit",
                    nutriments = OffNutriments(
                        energyKcal100g = 450.0,
                        proteins100g = 6.0,
                        carbohydrates100g = 70.0,
                        fat100g = 18.0,
                    ),
                ),
            ),
        )
        coEvery { offApiService.searchProducts(searchTerms = "biscuit") } returns response

        val result = repository.searchOpenFoodFacts("biscuit")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        coVerify(exactly = 0) { foodCacheDao.insert(any()) }
    }

    @Test
    fun `searchOpenFoodFacts returns failure with OfflineException on IOException`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = any()) } throws IOException()

        val result = repository.searchOpenFoodFacts("biscuit")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineException)
    }

    // --- isCacheExpired (via lookupBarcode) ---

    @Test
    fun `cache older than 30 days is treated as expired and network is attempted`() = runTest {
        val expiredEntity = freshCacheEntity("old").copy(
            cacheKey = "off:old",
            barcode = "old",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31),
        )
        coEvery { foodCacheDao.getByBarcode("old") } returns expiredEntity
        coEvery { offApiService.getProductByBarcode("old") } returns OffBarcodeResponse(
            status = 1,
            product = OffProduct(code = "old", productName = "Fresh Result", nutriments = null),
        )

        val result = repository.lookupBarcode("old")

        assertTrue(result.isSuccess)
        assertEquals("Fresh Result", result.getOrNull()?.name)
        coVerify(exactly = 1) { offApiService.getProductByBarcode("old") }
    }

    @Test
    fun `cache within 30 days is not expired and network is not called`() = runTest {
        val recentEntity = freshCacheEntity("recent").copy(
            cacheKey = "off:recent",
            barcode = "recent",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
        )
        coEvery { foodCacheDao.getByBarcode("recent") } returns recentEntity

        val result = repository.lookupBarcode("recent")

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { offApiService.getProductByBarcode(any()) }
    }

    // --- searchOpenFoodFacts additional error paths ---

    @Test
    fun `searchOpenFoodFacts returns failure with mapped message on HttpException`() = runTest {
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 429
        coEvery { offApiService.searchProducts(searchTerms = any()) } throws httpException

        val result = repository.searchOpenFoodFacts("biscuit")

        assertTrue(result.isFailure)
        assertEquals("Too many requests, please try again later", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchOpenFoodFacts returns failure with generic message on unexpected Exception`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = any()) } throws RuntimeException("unexpected")

        val result = repository.searchOpenFoodFacts("biscuit")

        assertTrue(result.isFailure)
        assertEquals("Could not read food data", result.exceptionOrNull()?.message)
    }

    // --- lookupBarcode additional paths ---

    @Test
    fun `lookupBarcode returns failure on HttpException when no cache`() = runTest {
        coEvery { foodCacheDao.getByBarcode("9999") } returns null
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 500
        coEvery { offApiService.getProductByBarcode("9999") } throws httpException

        val result = repository.lookupBarcode("9999")

        assertTrue(result.isFailure)
        assertEquals("Service temporarily unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `lookupBarcode returns failure on HttpException when cache is expired, not stale cache`() = runTest {
        val expiredEntity = FoodCacheEntity(
            cacheKey = "off:expired2",
            foodName = "Stale Product",
            kcalPer100g = 200.0,
            proteinPer100g = 10.0,
            carbsPer100g = 25.0,
            fatPer100g = 8.0,
            source = "OFF",
            barcode = "expired2",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31),
        )
        coEvery { foodCacheDao.getByBarcode("expired2") } returns expiredEntity
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 500
        coEvery { offApiService.getProductByBarcode("expired2") } throws httpException

        val result = repository.lookupBarcode("expired2")

        assertTrue(result.isFailure)
        assertEquals("Service temporarily unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `lookupBarcode returns success null on HTTP 404 even when cache is expired`() = runTest {
        val expiredEntity = FoodCacheEntity(
            cacheKey = "off:gone",
            foodName = "Old Product",
            kcalPer100g = 150.0,
            proteinPer100g = 5.0,
            carbsPer100g = 20.0,
            fatPer100g = 4.0,
            source = "OFF",
            barcode = "gone",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31),
        )
        coEvery { foodCacheDao.getByBarcode("gone") } returns expiredEntity
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 404
        coEvery { offApiService.getProductByBarcode("gone") } throws httpException

        val result = repository.lookupBarcode("gone")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `lookupBarcode populates missingFields when API response has null nutriments`() = runTest {
        coEvery { foodCacheDao.getByBarcode("5555") } returns null
        coEvery { offApiService.getProductByBarcode("5555") } returns OffBarcodeResponse(
            status = 1,
            product = OffProduct(code = "5555", productName = "Incomplete Food", nutriments = null),
        )

        val result = repository.lookupBarcode("5555")

        assertTrue(result.isSuccess)
        val food = result.getOrNull()!!
        assertEquals(4, food.missingFields.size)
    }

    @Test
    fun `lookupBarcode returns failure on unexpected Exception even when cache is expired`() = runTest {
        val expiredEntity = FoodCacheEntity(
            cacheKey = "off:expired3",
            foodName = "Stale Product",
            kcalPer100g = 200.0,
            proteinPer100g = 10.0,
            carbsPer100g = 25.0,
            fatPer100g = 8.0,
            source = "OFF",
            barcode = "expired3",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31),
        )
        coEvery { foodCacheDao.getByBarcode("expired3") } returns expiredEntity
        coEvery { offApiService.getProductByBarcode("expired3") } throws RuntimeException("serialisation error")

        val result = repository.lookupBarcode("expired3")

        assertTrue(result.isFailure)
        assertEquals("Could not read food data", result.exceptionOrNull()?.message)
    }

    // --- cacheResult entity content ---

    @Test
    fun `lookupBarcode caches entity with source OFF and barcode set`() = runTest {
        coEvery { foodCacheDao.getByBarcode("7777") } returns null
        coEvery { offApiService.getProductByBarcode("7777") } returns OffBarcodeResponse(
            status = 1,
            product = OffProduct(
                code = "7777",
                productName = "Test Product",
                nutriments = OffNutriments(
                    energyKcal100g = 200.0,
                    proteins100g = 10.0,
                    carbohydrates100g = 30.0,
                    fat100g = 5.0,
                ),
            ),
        )
        val entitySlot = slot<FoodCacheEntity>()
        coEvery { foodCacheDao.insert(capture(entitySlot)) } returns Unit

        repository.lookupBarcode("7777")

        assertEquals("OFF", entitySlot.captured.source)
        assertEquals("7777", entitySlot.captured.barcode)
    }
}
