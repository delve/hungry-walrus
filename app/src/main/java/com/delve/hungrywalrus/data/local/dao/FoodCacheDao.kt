package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity

@Dao
interface FoodCacheDao {

    /**
     * Cache lookup by the composite cache key.
     *
     * The key format is "usda:{fdcId}" or "off:{product_code}" (see architecture §5.2,
     * FoodCache cache key strategy). Returns null if no entry exists for the key.
     */
    @Query("SELECT * FROM food_cache WHERE cacheKey = :cacheKey")
    suspend fun get(cacheKey: String): FoodCacheEntity?

    @Query("SELECT * FROM food_cache WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): FoodCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodCacheEntity)

    @Query("DELETE FROM food_cache WHERE cachedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
