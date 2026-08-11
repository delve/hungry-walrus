package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import org.junit.Test

/**
 * QA unit tests for [ScaleNutritionUseCase] guard clauses that are NOT covered by the
 * existing [ScaleNutritionUseCaseEdgeCaseTest].
 *
 * Gaps filled:
 * - Negative [weightG] throws [IllegalArgumentException] (the existing edge-case test only
 *   tests negative per-100g fields, not the weight itself).
 * - `scaleRecipePortion` with `totalWeightG == 0.0` throws [IllegalArgumentException]
 *   (guard `require(recipe.totalWeightG > 0.0)`).
 * - `scaleRecipePortion` with negative `portionWeightG` throws [IllegalArgumentException]
 *   (guard `require(portionWeightG >= 0.0)`).
 */
class ScaleNutritionGuardTest {

    private val useCase = ScaleNutritionUseCase()

    // --- invoke: negative weightG guard ---

    /**
     * Spec: weight must be >= 0. Negative weights are physically impossible.
     * The guard `require(weightG >= 0.0)` must throw for any negative weight.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `negative weightG throws IllegalArgumentException`() {
        useCase(
            kcalPer100g = 100.0,
            proteinPer100g = 10.0,
            carbsPer100g = 20.0,
            fatPer100g = 5.0,
            weightG = -1.0,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `weightG of minus 0_1 throws IllegalArgumentException`() {
        useCase(
            kcalPer100g = 200.0,
            proteinPer100g = 15.0,
            carbsPer100g = 30.0,
            fatPer100g = 8.0,
            weightG = -0.1,
        )
    }

    // --- scaleRecipePortion: zero totalWeightG guard ---

    /**
     * A recipe with zero total weight would produce a division-by-zero error.
     * The guard `require(recipe.totalWeightG > 0.0)` must throw.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `scaleRecipePortion with zero totalWeightG throws IllegalArgumentException`() {
        val recipe = Recipe(
            id = 1,
            name = "Empty Recipe",
            totalWeightG = 0.0,
            totalKcal = 500.0,
            totalProteinG = 30.0,
            totalCarbsG = 60.0,
            totalFatG = 15.0,
            createdAt = 0,
            updatedAt = 0,
        )
        useCase.scaleRecipePortion(recipe, portionWeightG = 100.0)
    }

    // --- scaleRecipePortion: negative portionWeightG guard ---

    /**
     * Negative portion weights are physically impossible.
     * The guard `require(portionWeightG >= 0.0)` must throw.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `scaleRecipePortion with negative portionWeightG throws IllegalArgumentException`() {
        val recipe = Recipe(
            id = 1,
            name = "Stew",
            totalWeightG = 600.0,
            totalKcal = 900.0,
            totalProteinG = 50.0,
            totalCarbsG = 80.0,
            totalFatG = 30.0,
            createdAt = 0,
            updatedAt = 0,
        )
        useCase.scaleRecipePortion(recipe, portionWeightG = -50.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scaleRecipePortion with minus 0_001 portionWeightG throws IllegalArgumentException`() {
        val recipe = Recipe(
            id = 1,
            name = "Sauce",
            totalWeightG = 200.0,
            totalKcal = 400.0,
            totalProteinG = 20.0,
            totalCarbsG = 40.0,
            totalFatG = 16.0,
            createdAt = 0,
            updatedAt = 0,
        )
        useCase.scaleRecipePortion(recipe, portionWeightG = -0.001)
    }
}
