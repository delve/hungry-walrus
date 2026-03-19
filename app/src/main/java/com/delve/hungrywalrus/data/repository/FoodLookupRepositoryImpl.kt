package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffApiService
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffResponseMapper
import com.delve.hungrywalrus.data.remote.usda.UsdaApiService
import com.delve.hungrywalrus.data.remote.usda.UsdaResponseMapper
import com.delve.hungrywalrus.domain.OfflineException
import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FoodLookupRepositoryImpl @Inject constructor(
    private val usdaApiService: UsdaApiService,
    private val offApiService: OffApiService,
    private val foodCacheDao: FoodCacheDao,
) : FoodLookupRepository {

    companion object {
        private val CACHE_DURATION_MILLIS = TimeUnit.DAYS.toMillis(30)
    }

    override suspend fun searchUsda(query: String): Result<List<FoodSearchResult>> {
        return try {
            val response = usdaApiService.searchFoods(query = query)
            val results = UsdaResponseMapper.mapFoods(response.foods)
            Result.success(results)
        } catch (e: IOException) {
            Result.failure(OfflineException("Network error: unable to reach USDA service"))
        } catch (e: HttpException) {
            Result.failure(Exception(mapHttpError(e.code())))
        } catch (e: Exception) {
            Result.failure(Exception("Could not read food data"))
        }
    }

    override suspend fun searchOpenFoodFacts(query: String): Result<List<FoodSearchResult>> {
        return try {
            val response = offApiService.searchProducts(searchTerms = query)
            val results = OffResponseMapper.mapProducts(response.products)
            Result.success(results)
        } catch (e: IOException) {
            Result.failure(OfflineException("Network error: unable to reach Open Food Facts service"))
        } catch (e: HttpException) {
            Result.failure(Exception(mapHttpError(e.code())))
        } catch (e: Exception) {
            Result.failure(Exception("Could not read food data"))
        }
    }

    override suspend fun lookupBarcode(barcode: String): Result<FoodSearchResult?> {
        // Check cache first
        val cached = foodCacheDao.getByBarcode(barcode)
        if (cached != null && !isCacheExpired(cached.cachedAt)) {
            return Result.success(cached.toDomain())
        }

        return try {
            val response = offApiService.getProductByBarcode(barcode)
            if (response.status == 0 || response.product == null) {
                Result.success(null)
            } else {
                val result = OffResponseMapper.mapProduct(response.product)
                cacheResult(result, barcode)
                Result.success(result)
            }
        } catch (e: IOException) {
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(OfflineException("Network error: unable to reach Open Food Facts service"))
            }
        } catch (e: HttpException) {
            if (e.code() == 404) {
                Result.success(null)
            } else {
                Result.failure(Exception(mapHttpError(e.code())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not read food data"))
        }
    }

    private fun isCacheExpired(cachedAt: Long): Boolean {
        return System.currentTimeMillis() - cachedAt > CACHE_DURATION_MILLIS
    }

    private suspend fun cacheResult(result: FoodSearchResult, barcode: String? = null) {
        val entity = FoodCacheEntity(
            cacheKey = result.id,
            foodName = result.name,
            kcalPer100g = result.kcalPer100g,
            proteinPer100g = result.proteinPer100g,
            carbsPer100g = result.carbsPer100g,
            fatPer100g = result.fatPer100g,
            source = when (result.source) {
                FoodSource.USDA -> "USDA"
                FoodSource.OPEN_FOOD_FACTS -> "OFF"
                FoodSource.MANUAL -> "MANUAL"
            },
            barcode = barcode,
            cachedAt = System.currentTimeMillis(),
        )
        foodCacheDao.insert(entity)
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            400 -> "Invalid request"
            403 -> "Invalid API key"
            429 -> "Too many requests, please try again later"
            in 500..599 -> "Service temporarily unavailable"
            else -> "Unexpected error (HTTP $code)"
        }
    }

    private fun FoodCacheEntity.toDomain(): FoodSearchResult {
        val missingFields = buildSet {
            if (kcalPer100g == null) add(NutritionField.KCAL)
            if (proteinPer100g == null) add(NutritionField.PROTEIN)
            if (carbsPer100g == null) add(NutritionField.CARBS)
            if (fatPer100g == null) add(NutritionField.FAT)
        }
        return FoodSearchResult(
            id = cacheKey,
            name = foodName,
            source = when (source) {
                "USDA" -> FoodSource.USDA
                "OFF" -> FoodSource.OPEN_FOOD_FACTS
                else -> FoodSource.MANUAL
            },
            kcalPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            missingFields = missingFields,
        )
    }
}
