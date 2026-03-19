package com.delve.hungrywalrus.di

import android.content.Context
import androidx.room.Room
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase
import com.delve.hungrywalrus.data.local.dao.FoodCacheDao
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.data.local.dao.RecipeDao
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HungryWalrusDatabase {
        return Room.databaseBuilder(
            context,
            HungryWalrusDatabase::class.java,
            HungryWalrusDatabase.DATABASE_NAME,
        ).build()
    }

    @Provides
    @Singleton
    fun provideNutritionPlanDao(database: HungryWalrusDatabase): NutritionPlanDao {
        return database.nutritionPlanDao()
    }

    @Provides
    @Singleton
    fun provideLogEntryDao(database: HungryWalrusDatabase): LogEntryDao {
        return database.logEntryDao()
    }

    @Provides
    @Singleton
    fun provideRecipeDao(database: HungryWalrusDatabase): RecipeDao {
        return database.recipeDao()
    }

    @Provides
    @Singleton
    fun provideRecipeIngredientDao(database: HungryWalrusDatabase): RecipeIngredientDao {
        return database.recipeIngredientDao()
    }

    @Provides
    @Singleton
    fun provideFoodCacheDao(database: HungryWalrusDatabase): FoodCacheDao {
        return database.foodCacheDao()
    }
}
