package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.FoodSearchResult

interface FoodLookupRepository {
    suspend fun searchUsda(query: String): Result<List<FoodSearchResult>>
    suspend fun searchOpenFoodFacts(query: String): Result<List<FoodSearchResult>>
    suspend fun lookupBarcode(barcode: String): Result<FoodSearchResult?>
}
