package com.delve.hungrywalrus.di

import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.FoodLookupRepositoryImpl
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepositoryImpl
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepositoryImpl
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.data.repository.RecipeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNutritionPlanRepository(
        impl: NutritionPlanRepositoryImpl,
    ): NutritionPlanRepository

    @Binds
    @Singleton
    abstract fun bindLogEntryRepository(
        impl: LogEntryRepositoryImpl,
    ): LogEntryRepository

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(
        impl: RecipeRepositoryImpl,
    ): RecipeRepository

    @Binds
    @Singleton
    abstract fun bindFoodLookupRepository(
        impl: FoodLookupRepositoryImpl,
    ): FoodLookupRepository
}
