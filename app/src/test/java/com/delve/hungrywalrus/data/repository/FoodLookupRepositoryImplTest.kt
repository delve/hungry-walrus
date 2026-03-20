package com.delve.hungrywalrus.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var repository: FoodLookupRepositoryImpl

    @Before
    fun setUp() {
        usdaApiService = mockk()
        offApiService = mockk()
        foodCacheDao = mockk(relaxed = true)
        connectivityManager = mockk()
        context = mockk()
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        repository = FoodLookupRepositoryImpl(usdaApiService, offApiService, foodCacheDao, context)
    }

    private fun setNetworkAvailable(available: Boolean) {
        if (available) {
            val network = mockk<Network>()
            val capabilities = mockk<NetworkCapabilities>()
            every { connectivityManager.activeNetwork } returns network
            every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
            every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        } else {
            every { connectivityManager.activeNetwork } returns null
        }
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
        setNetworkAvailable(true)
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
        setNetworkAvailable(false)

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
    fun `searchUsda returns failure with too many requests message on HTTP 429`() = runTest {
        val httpException = mockk<HttpException>()
        every { httpException.code() } returns 429
        coEvery { usdaApiService.searchFoods(query = any()) } throws httpException

        val result = repository.searchUsda("chicken")

        assertTrue(result.isFailure)
        assertEquals("Too many requests, please try again later", result.exceptionOrNull()?.message)
    }

    @Test
    fun `searchUsda caches each result after successful search`() = runTest {
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
        coVerify(exactly = 1) { foodCacheDao.insert(any()) }
    }

    // --- searchOpenFoodFacts ---

    @Test
    fun `searchOpenFoodFacts caches results after successful search`() = runTest {
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
        coVerify(exactly = 1) { foodCacheDao.insert(any()) }
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
        setNetworkAvailable(true)
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
}
