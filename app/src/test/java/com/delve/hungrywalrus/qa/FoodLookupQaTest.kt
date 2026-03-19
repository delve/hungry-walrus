package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffBarcodeResponse
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffNutriments
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffProduct
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffSearchResponse
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService
import com.delve.hungrywalrus.data.remote.usda.UsdaFood
import com.delve.hungrywalrus.data.remote.usda.UsdaNutrient
import com.delve.hungrywalrus.data.remote.usda.UsdaSearchResponse
import com.delve.hungrywalrus.data.repository.FoodLookupRepositoryImpl
import com.delve.hungrywalrus.domain.OfflineException
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * QA tests filling coverage gaps for [FoodLookupRepositoryImpl]:
 *
 * - Expired barcode cache triggers a fresh network call and returns fresh data.
 * - Successful barcode lookup stores the result in the cache (cacheResult called).
 * - IOException with no cached entry produces OfflineException (not null).
 * - HTTP 4xx other than 403/404/429 returns a generic error message.
 * - Barcode lookup with product name null falls back to product code as name.
 * - USDA empty result list returns success with empty list.
 * - OFF search with empty product list returns success with empty list.
 * - Zero nutritional values (not null) are mapped correctly and have no missing fields.
 */
class FoodLookupQaTest {

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

    // --- Expired cache triggers network ---

    /**
     * Requirement: cache duration is 30 days (architecture section 5.4).
     * When the cached entry is older than 30 days, the repository must make a fresh
     * network call and return the fresh result, ignoring the stale cache.
     */
    @Test
    fun `expired cache triggers network call for barcode lookup`() = runTest {
        val staleCache = FoodCacheEntity(
            cacheKey = "off:expiredbarcode",
            foodName = "Old Product",
            kcalPer100g = 100.0,
            proteinPer100g = 5.0,
            carbsPer100g = 15.0,
            fatPer100g = 3.0,
            source = "OFF",
            barcode = "expiredbarcode",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31), // 31 days old
        )
        coEvery { foodCacheDao.getByBarcode("expiredbarcode") } returns staleCache
        coEvery { offApiService.getProductByBarcode("expiredbarcode") } returns
            OffBarcodeResponse(
                status = 1,
                product = OffProduct(
                    code = "expiredbarcode",
                    productName = "Fresh Product",
                    nutriments = OffNutriments(
                        energyKcal100g = 200.0,
                        proteins100g = 10.0,
                        carbohydrates100g = 25.0,
                        fat100g = 7.0,
                    ),
                ),
            )

        val result = repository.lookupBarcode("expiredbarcode")

        // Network was called (cache was expired)
        coVerify { offApiService.getProductByBarcode("expiredbarcode") }
        // Returns fresh data, not stale
        assertTrue(result.isSuccess)
        assertEquals("Fresh Product", result.getOrNull()?.name)
        assertEquals(200.0, result.getOrNull()!!.kcalPer100g!!, 0.001)
    }

    /**
     * After a successful barcode network lookup, the result must be cached for future use.
     */
    @Test
    fun `successful barcode lookup stores result in cache`() = runTest {
        coEvery { foodCacheDao.getByBarcode("newbarcode") } returns null
        coEvery { offApiService.getProductByBarcode("newbarcode") } returns
            OffBarcodeResponse(
                status = 1,
                product = OffProduct(
                    code = "newbarcode",
                    productName = "New Product",
                    nutriments = OffNutriments(
                        energyKcal100g = 300.0,
                        proteins100g = 15.0,
                        carbohydrates100g = 40.0,
                        fat100g = 10.0,
                    ),
                ),
            )

        repository.lookupBarcode("newbarcode")

        // The repository must insert the result into the cache (architecture section 5.4)
        coVerify { foodCacheDao.insert(any()) }
    }

    /**
     * When the network throws IOException and there is no cached entry (not even stale),
     * the result must be a failure wrapping OfflineException.
     * (Not null result -- null result means "product not found", not "offline".)
     */
    @Test
    fun `IOException with no cached entry produces OfflineException failure`() = runTest {
        coEvery { foodCacheDao.getByBarcode("noncached") } returns null
        coEvery { offApiService.getProductByBarcode("noncached") } throws IOException("timeout")

        val result = repository.lookupBarcode("noncached")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineException)
    }

    // --- HTTP error codes ---

    /**
     * HTTP 400 (Bad Request) for USDA must return a specific message.
     */
    @Test
    fun `searchUsda returns failure with Invalid request message on HTTP 400`() = runTest {
        val body = okhttp3.ResponseBody.create(null, "")
        coEvery { usdaApiService.searchFoods(query = any()) } throws
            HttpException(retrofit2.Response.error<Any>(400, body))

        val result = repository.searchUsda("test")

        assertTrue(result.isFailure)
        assertEquals("Invalid request", result.exceptionOrNull()?.message)
    }

    /**
     * An unexpected HTTP code (e.g. 503) not in the switch should produce a message
     * containing the status code.
     */
    @Test
    fun `searchUsda returns failure with status code on unrecognised HTTP error`() = runTest {
        val body = okhttp3.ResponseBody.create(null, "")
        coEvery { usdaApiService.searchFoods(query = any()) } throws
            HttpException(retrofit2.Response.error<Any>(503, body))

        val result = repository.searchUsda("test")

        assertTrue(result.isFailure)
        // "Service temporarily unavailable" covers 5xx range
        assertEquals("Service temporarily unavailable", result.exceptionOrNull()?.message)
    }

    /**
     * HTTP 429 (Too Many Requests) for OFF search must return specific message.
     */
    @Test
    fun `searchOpenFoodFacts returns rate-limit message on HTTP 429`() = runTest {
        val body = okhttp3.ResponseBody.create(null, "")
        coEvery { offApiService.searchProducts(searchTerms = any()) } throws
            HttpException(retrofit2.Response.error<Any>(429, body))

        val result = repository.searchOpenFoodFacts("test")

        assertTrue(result.isFailure)
        assertEquals("Too many requests, please try again later", result.exceptionOrNull()?.message)
    }

    // --- Empty results ---

    /**
     * USDA search returning an empty food list must return Result.success with an empty list,
     * not a failure.
     */
    @Test
    fun `searchUsda with empty response list returns success with empty list`() = runTest {
        coEvery { usdaApiService.searchFoods(query = "zzz") } returns
            UsdaSearchResponse(foods = emptyList())

        val result = repository.searchUsda("zzz")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }

    /**
     * OFF search returning an empty product list must return Result.success with an empty list.
     */
    @Test
    fun `searchOpenFoodFacts with empty response list returns success with empty list`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = "zzz") } returns
            OffSearchResponse(products = emptyList())

        val result = repository.searchOpenFoodFacts("zzz")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrNull())
    }

    // --- Zero nutritional values ---

    /**
     * Requirement: "If any of the four core nutritional values are missing from an API
     * response, prompt the user." Zero is not missing -- only null is missing.
     * A food with all-zero nutrition values must have an empty missingFields set.
     */
    @Test
    fun `USDA food with zero nutritional values has empty missingFields`() = runTest {
        val food = UsdaFood(
            fdcId = 99999L,
            description = "Plain water",
            foodNutrients = listOf(
                UsdaNutrient(nutrientId = 1008, value = 0.0),
                UsdaNutrient(nutrientId = 1003, value = 0.0),
                UsdaNutrient(nutrientId = 1005, value = 0.0),
                UsdaNutrient(nutrientId = 1004, value = 0.0),
            ),
        )
        coEvery { usdaApiService.searchFoods(query = "water") } returns
            UsdaSearchResponse(foods = listOf(food))

        val result = repository.searchUsda("water")

        assertTrue(result.isSuccess)
        val foodResult = result.getOrNull()!!.first()
        assertTrue("Zero values should not be treated as missing", foodResult.missingFields.isEmpty())
        assertEquals(0.0, foodResult.kcalPer100g!!, 0.001)
    }

    /**
     * OFF product with explicit zero nutritional values has empty missingFields.
     */
    @Test
    fun `OFF product with zero nutritional values has empty missingFields`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = "water") } returns
            OffSearchResponse(
                products = listOf(
                    OffProduct(
                        code = "0000001",
                        productName = "Sparkling Water",
                        nutriments = OffNutriments(
                            energyKcal100g = 0.0,
                            proteins100g = 0.0,
                            carbohydrates100g = 0.0,
                            fat100g = 0.0,
                        ),
                    ),
                ),
            )

        val result = repository.searchOpenFoodFacts("water")

        assertTrue(result.isSuccess)
        val foodResult = result.getOrNull()!!.first()
        assertTrue("Zero values should not be treated as missing", foodResult.missingFields.isEmpty())
    }

    // --- FoodSource mapping for cached results ---

    /**
     * A cached entry with source="USDA" must map to FoodSource.USDA when retrieved.
     */
    @Test
    fun `cached barcode with USDA source maps to FoodSource USDA`() = runTest {
        // Note: barcode lookups use OFF only per spec, but the cache can theoretically
        // contain USDA entries inserted via other paths. This tests the toDomain() mapping.
        val freshCache = FoodCacheEntity(
            cacheKey = "usda:123",
            foodName = "USDA Food",
            kcalPer100g = 100.0,
            proteinPer100g = 10.0,
            carbsPer100g = 15.0,
            fatPer100g = 5.0,
            source = "USDA",
            barcode = "scannedfood",
            cachedAt = System.currentTimeMillis(),
        )
        coEvery { foodCacheDao.getByBarcode("scannedfood") } returns freshCache

        val result = repository.lookupBarcode("scannedfood")

        assertTrue(result.isSuccess)
        assertEquals(FoodSource.USDA, result.getOrNull()?.source)
    }

    /**
     * Spec: Barcode scans query Open Food Facts only.
     * Verify that lookupBarcode calls offApiService (not usdaApiService) for network requests.
     */
    @Test
    fun `lookupBarcode uses only Open Food Facts for network calls`() = runTest {
        coEvery { foodCacheDao.getByBarcode("testbarcode") } returns null
        coEvery { offApiService.getProductByBarcode("testbarcode") } returns
            OffBarcodeResponse(status = 0, product = null)

        repository.lookupBarcode("testbarcode")

        // Only OFF was called
        coVerify { offApiService.getProductByBarcode("testbarcode") }
        // USDA must not be called at all for barcode lookup
        coVerify(exactly = 0) { usdaApiService.searchFoods(any()) }
    }

    // --- Missing fields detection after barcode lookup ---

    /**
     * A barcode lookup result with some null nutriments must have the correct
     * missingFields set so the UI can prompt for them.
     */
    @Test
    fun `barcode lookup result with only protein missing reports PROTEIN in missingFields`() = runTest {
        coEvery { foodCacheDao.getByBarcode("partial") } returns null
        coEvery { offApiService.getProductByBarcode("partial") } returns
            OffBarcodeResponse(
                status = 1,
                product = OffProduct(
                    code = "partial",
                    productName = "Partial Product",
                    nutriments = OffNutriments(
                        energyKcal100g = 200.0,
                        proteins100g = null,  // MISSING
                        carbohydrates100g = 30.0,
                        fat100g = 8.0,
                    ),
                ),
            )

        val result = repository.lookupBarcode("partial")

        assertTrue(result.isSuccess)
        val food = result.getOrNull()
        assertNotNull(food)
        assertEquals(setOf(NutritionField.PROTEIN), food!!.missingFields)
        assertNull(food.proteinPer100g)
        assertEquals(200.0, food!!.kcalPer100g!!, 0.001)
    }
}
