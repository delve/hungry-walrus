package com.delve.hungrywalrus.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for repository bindings.
 *
 * TODO: Bind the following repository interfaces to their implementations:
 * - NutritionPlanRepository
 * - LogEntryRepository
 * - RecipeRepository
 * - FoodLookupRepository
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // TODO: Implement in repository session
}
