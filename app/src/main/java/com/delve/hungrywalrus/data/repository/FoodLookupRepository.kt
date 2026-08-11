package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.FoodSearchResult

interface FoodLookupRepository {
    suspend fun searchUsda(query: String): Result<List<FoodSearchResult>>
    suspend fun searchOpenFoodFacts(query: String): Result<List<FoodSearchResult>>
    suspend fun lookupBarcode(barcode: String): Result<FoodSearchResult?>

    /**
     * Cache a food item that the user has selected from a text search result.
     *
     * Architecture §6.2 item 2 specifies that individual food items from text searches
     * are not pre-cached; they are cached "when the user selects a specific item and its
     * per-100g data is resolved". Item 3 mandates that the cache write uses
     * [androidx.room.OnConflictStrategy.REPLACE] semantics.
     *
     * The caller (typically the AddEntry / FoodSearch flow in the UI layer) invokes this
     * method once the user has confirmed a search-result selection so that subsequent
     * lookups of the same `cacheKey` can be served from the local cache for up to 30 days
     * (architecture §5.2 cache duration).
     *
     * The cached entry's `barcode` is null because text-search results are not associated
     * with a scanned barcode. Barcode lookups continue to populate the `barcode` column
     * via the internal write path inside [lookupBarcode].
     */
    suspend fun cacheItem(result: FoodSearchResult)
}
