package com.delve.hungrywalrus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.data.local.dao.RecipeDao
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao
import com.delve.hungrywalrus.data.local.entity.FoodCacheEntity
import com.delve.hungrywalrus.data.local.entity.LogEntryEntity
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import com.delve.hungrywalrus.data.local.entity.RecipeEntity
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity

@Database(
    entities = [
        NutritionPlanEntity::class,
        LogEntryEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        FoodCacheEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HungryWalrusDatabase : RoomDatabase() {
    abstract fun nutritionPlanDao(): NutritionPlanDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun foodCacheDao(): FoodCacheDao

    companion object {
        const val DATABASE_NAME = "hungry_walrus.db"
    }
}
