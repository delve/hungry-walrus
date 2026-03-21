package com.delve.hungrywalrus.integration

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
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Integration tests verifying the data layer → domain layer interaction for food lookup,
 * including:
 * - API client behaviour with the repository (USDA and Open Food Facts).
 * - Caching: fresh cache returns without network call; expired cache triggers network.
 * - Error handling: IOException, HttpException (4xx, 5xx), unexpected exceptions.
 * - Missing fields in API responses produce correct missingFields set and trigger
 *   the validate use case to report the food as incomplete.
 * - Negative and zero value handling.
 */
class FoodLookupIntegrationTest {

    private lateinit var usdaApiService: UsdaApiService
    private lateinit var offApiService: OffApiService
    private lateinit var foodCacheDao: FoodCacheDao
    private lateinit var repository: FoodLookupRepositoryImpl
    private val validateUseCase = ValidateFoodDataUseCase()

    @Before
    fun setUp() {
        usdaApiService = mockk()
        offApiService = mockk()
        foodCacheDao = mockk(relaxed = true)
        repository = FoodLookupRepositoryImpl(usdaApiService, offApiService, foodCacheDao)
    }

    private fun usdaFood(
        id: Long = 12345L,
        name: String = "Chicken Breast",
        kcal: Double? = 165.0,
        protein: Double? = 31.0,
        carbs: Double? = 0.0,
        fat: Double? = 3.6,
    ) = UsdaFood(
        fdcId = id,
        description = name,
        foodNutrients = buildList {
            if (kcal != null) add(UsdaNutrient(nutrientId = 1008, value = kcal))
            if (protein != null) add(UsdaNutrient(nutrientId = 1003, value = protein))
            if (carbs != null) add(UsdaNutrient(nutrientId = 1005, value = carbs))
            if (fat != null) add(UsdaNutrient(nutrientId = 1004, value = fat))
        },
    )

    private fun offProduct(
        code: String = "1234567890",
        name: String? = "Oat Biscuits",
        kcal: Double? = 450.0,
        protein: Double? = 8.0,
        carbs: Double? = 60.0,
        fat: Double? = 18.0,
    ) = OffProduct(
        code = code,
        productName = name,
        nutriments = OffNutriments(
            energyKcal100g = kcal,
            proteins100g = protein,
            carbohydrates100g = carbs,
            fat100g = fat,
        ),
    )

    // ---- USDA search integration ----

    @Test
    fun `searchUsda returns mapped FoodSearchResult with correct nutrient values`() = runTest {
        coEvery { usdaApiService.searchFoods(query = "chicken") } returns
            UsdaSearchResponse(foods = listOf(usdaFood()))

        val result = repository.searchUsda("chicken")

        assertTrue(result.isSuccess)
        val foods = result.getOrNull()!!
        assertEquals(1, foods.size)
        val food = foods[0]
        assertEquals("Chicken Breast", food.name)
        assertEquals("usda:12345", food.id)
        assertEquals(FoodSource.USDA, food.source)
        assertEquals(165.0, food.kcalPer100g!!, 0.001)
        assertEquals(31.0, food.proteinPer100g!!, 0.001)
        assertEquals(0.0, food.carbsPer100g!!, 0.001)
        assertEquals(3.6, food.fatPer100g!!, 0.001)
        assertTrue(food.missingFields.isEmpty())
    }

    @Test
    fun `searchUsda result with missing kcal has KCAL in missingFields and is not complete`() = runTest {
        coEvery { usdaApiService.searchFoods(query = "mystery food") } returns
            UsdaSearchResponse(foods = listOf(usdaFood(kcal = null)))

        val result = repository.searchUsda("mystery food")
        assertTrue(result.isSuccess)
        val food = result.getOrNull()!!.first()

        assertTrue(NutritionField.KCAL in food.missingFields)
        assertNull(food.kcalPer100g)
        assertFalse(validateUseCase.isComplete(food))
    }

    @Test
    fun `searchUsda returns OfflineException on IOException`() = runTest {
        coEvery { usdaApiService.searchFoods(query = any()) } throws IOException("timeout")

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineException)
    }

    @Test
    fun `searchUsda returns failure with Invalid API key message on HTTP 403`() = runTest {
        val response = okhttp3.ResponseBody.create(null, "")
        coEvery { usdaApiService.searchFoods(query = any()) } throws
            HttpException(retrofit2.Response.error<Any>(403, response))

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Invalid API key", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchUsda returns failure with Too many requests message on HTTP 429`() = runTest {
        val response = okhttp3.ResponseBody.create(null, "")
        coEvery { usdaApiService.searchFoods(query = any()) } throws
            HttpException(retrofit2.Response.error<Any>(429, response))

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Too many requests, please try again later", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchUsda returns failure on unexpected exception`() = runTest {
        coEvery { usdaApiService.searchFoods(query = any()) } throws RuntimeException("unexpected")

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Could not read food data", result.exceptionOrNull()?.message)
    }

    // ---- Open Food Facts search integration ----

    @Test
    fun `searchOpenFoodFacts returns mapped results`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = "biscuits") } returns
            OffSearchResponse(products = listOf(offProduct()))

        val result = repository.searchOpenFoodFacts("biscuits")

        assertTrue(result.isSuccess)
        val foods = result.getOrNull()!!
        assertEquals(1, foods.size)
        val food = foods[0]
        assertEquals("Oat Biscuits", food.name)
        assertEquals(FoodSource.OPEN_FOOD_FACTS, food.source)
        assertEquals(450.0, food.kcalPer100g!!, 0.001)
    }

    @Test
    fun `searchOpenFoodFacts result with all null nutriments has all four missing fields`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = "unknown") } returns
            OffSearchResponse(
                products = listOf(
                    OffProduct(
                        code = "999",
                        productName = "No-data product",
                        nutriments = null,
                    ),
                ),
            )

        val result = repository.searchOpenFoodFacts("unknown")
        assertTrue(result.isSuccess)
        val food = result.getOrNull()!!.first()

        assertEquals(4, food.missingFields.size)
        assertFalse(validateUseCase.isComplete(food))
    }

    @Test
    fun `searchOpenFoodFacts returns OfflineException on IOException`() = runTest {
        coEvery { offApiService.searchProducts(searchTerms = any()) } throws IOException("offline")

        val result = repository.searchOpenFoodFacts("oats")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineException)
    }

    // ---- Barcode lookup integration ----

    @Test
    fun `lookupBarcode uses cached result when cache is fresh`() = runTest {
        val freshCache = FoodCacheEntity(
            cacheKey = "off:9876543210",
            foodName = "Granola Bar",
            kcalPer100g = 420.0,
            proteinPer100g = 9.0,
            carbsPer100g = 58.0,
            fatPer100g = 14.0,
            source = "OFF",
            barcode = "9876543210",
            cachedAt = System.currentTimeMillis(),
        )
        coEvery { foodCacheDao.getByBarcode("9876543210") } returns freshCache

        val result = repository.lookupBarcode("9876543210")

        assertTrue(result.isSuccess)
        val food = result.getOrNull()
        assertNotNull(food)
        assertEquals("Granola Bar", food!!.name)
        assertEquals(FoodSource.OPEN_FOOD_FACTS, food.source)
        assertTrue(food.missingFields.isEmpty())
    }

    @Test
    fun `lookupBarcode returns null when product not found (status 0)`() = runTest {
        coEvery { foodCacheDao.getByBarcode(any()) } returns null
        coEvery { offApiService.getProductByBarcode("0000000000") } returns
            OffBarcodeResponse(status = 0, product = null)

        val result = repository.lookupBarcode("0000000000")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `lookupBarcode returns stale cache on IOException (offline)`() = runTest {
        val staleCache = FoodCacheEntity(
            cacheKey = "off:expired",
            foodName = "Old Product",
            kcalPer100g = 200.0,
            proteinPer100g = 10.0,
            carbsPer100g = 25.0,
            fatPer100g = 8.0,
            source = "OFF",
            barcode = "expired",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(35),
        )
        coEvery { foodCacheDao.getByBarcode("expired") } returns staleCache
        coEvery { offApiService.getProductByBarcode("expired") } throws IOException("offline")

        val result = repository.lookupBarcode("expired")

        assertTrue(result.isSuccess)
        assertEquals("Old Product", result.getOrNull()?.name)
    }

    @Test
    fun `lookupBarcode returns failure on HttpException 500 even when stale cache exists`() = runTest {
        val staleCache = FoodCacheEntity(
            cacheKey = "off:broken",
            foodName = "Stale Product",
            kcalPer100g = 100.0,
            proteinPer100g = 5.0,
            carbsPer100g = 15.0,
            fatPer100g = 3.0,
            source = "OFF",
            barcode = "broken",
            cachedAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(35),
        )
        coEvery { foodCacheDao.getByBarcode("broken") } returns staleCache
        val response = okhttp3.ResponseBody.create(null, "")
        coEvery { offApiService.getProductByBarcode("broken") } throws
            HttpException(retrofit2.Response.error<Any>(500, response))

        val result = repository.lookupBarcode("broken")

        // Per fix session 04: HttpException should NOT fall back to stale cache
        assertTrue(result.isFailure)
        assertEquals("Service temporarily unavailable", result.exceptionOrNull()?.message)
    }

    @Test
    fun `lookupBarcode returns success null on HTTP 404`() = runTest {
        coEvery { foodCacheDao.getByBarcode(any()) } returns null
        val response = okhttp3.ResponseBody.create(null, "")
        coEvery { offApiService.getProductByBarcode(any()) } throws
            HttpException(retrofit2.Response.error<Any>(404, response))

        val result = repository.lookupBarcode("notfound")

        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `lookupBarcode result with missing nutriments has all four fields in missingFields`() = runTest {
        coEvery { foodCacheDao.getByBarcode("no_nutrients") } returns null
        coEvery { offApiService.getProductByBarcode("no_nutrients") } returns
            OffBarcodeResponse(
                status = 1,
                product = OffProduct(
                    code = "no_nutrients",
                    productName = "No Nutrients Product",
                    nutriments = null,
                ),
            )

        val result = repository.lookupBarcode("no_nutrients")

        assertTrue(result.isSuccess)
        val food = result.getOrNull()
        assertNotNull(food)
        assertEquals(4, food!!.missingFields.size)
        assertFalse(validateUseCase.isComplete(food))
    }

    @Test
    fun `complete barcode lookup result satisfies validateUseCase isComplete`() = runTest {
        coEvery { foodCacheDao.getByBarcode("complete") } returns null
        coEvery { offApiService.getProductByBarcode("complete") } returns
            OffBarcodeResponse(status = 1, product = offProduct(code = "complete"))

        val result = repository.lookupBarcode("complete")

        assertTrue(result.isSuccess)
        val food = result.getOrNull()
        assertNotNull(food)
        assertTrue(validateUseCase.isComplete(food!!))
    }
}
