package com.delve.hungrywalrus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_cache")
data class FoodCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val foodName: String,
    val kcalPer100g: Double?,
    val proteinPer100g: Double?,
    val carbsPer100g: Double?,
    val fatPer100g: Double?,
    val source: String,
    val barcode: String?,
    val cachedAt: Long,
)
