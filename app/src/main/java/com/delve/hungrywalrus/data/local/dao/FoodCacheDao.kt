package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity

@Dao
interface FoodCacheDao {

    @Query("SELECT * FROM food_cache WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): FoodCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FoodCacheEntity)

    @Query("DELETE FROM food_cache WHERE cachedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
