package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Additional edge-case unit tests for [ScaleNutritionUseCase] not covered by
 * ScaleNutritionUseCaseTest.
 */
class ScaleNutritionUseCaseEdgeCaseTest {

    private val useCase = ScaleNutritionUseCase()

    // --- invoke (per-100g scaling) edge cases ---

    @Test
    fun `very large weight produces proportionally large values`() {
        val result = useCase(
            kcalPer100g = 400.0,
            proteinPer100g = 10.0,
            carbsPer100g = 80.0,
            fatPer100g = 5.0,
            weightG = 10_000.0,
        )
        assertEquals(40_000.0, result.kcal, 0.001)
        assertEquals(1_000.0, result.proteinG, 0.001)
        assertEquals(8_000.0, result.carbsG, 0.001)
        assertEquals(500.0, result.fatG, 0.001)
    }

    @Test
    fun `zero per-100g reference values produce zero scaled values for any weight`() {
        val result = useCase(
            kcalPer100g = 0.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatPer100g = 0.0,
            weightG = 250.0,
        )
        assertEquals(0.0, result.kcal, 0.001)
        assertEquals(0.0, result.proteinG, 0.001)
        assertEquals(0.0, result.carbsG, 0.001)
        assertEquals(0.0, result.fatG, 0.001)
    }

    @Test
    fun `exactly 100g weight returns per-100g reference values unchanged`() {
        val result = useCase(
            kcalPer100g = 200.0,
            proteinPer100g = 15.0,
            carbsPer100g = 30.0,
            fatPer100g = 5.0,
            weightG = 100.0,
        )
        assertEquals(200.0, result.kcal, 0.001)
        assertEquals(15.0, result.proteinG, 0.001)
        assertEquals(30.0, result.carbsG, 0.001)
        assertEquals(5.0, result.fatG, 0.001)
    }

    // --- invoke: negative per-100g values are rejected ---

    @Test(expected = IllegalArgumentException::class)
    fun `negative kcalPer100g throws IllegalArgumentException`() {
        useCase(kcalPer100g = -1.0, proteinPer100g = 10.0, carbsPer100g = 20.0, fatPer100g = 5.0, weightG = 100.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative proteinPer100g throws IllegalArgumentException`() {
        useCase(kcalPer100g = 100.0, proteinPer100g = -0.1, carbsPer100g = 20.0, fatPer100g = 5.0, weightG = 100.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative carbsPer100g throws IllegalArgumentException`() {
        useCase(kcalPer100g = 100.0, proteinPer100g = 10.0, carbsPer100g = -5.0, fatPer100g = 5.0, weightG = 100.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative fatPer100g throws IllegalArgumentException`() {
        useCase(kcalPer100g = 100.0, proteinPer100g = 10.0, carbsPer100g = 20.0, fatPer100g = -0.5, weightG = 100.0)
    }

    // --- scaleRecipePortion edge cases ---

    @Test
    fun `portion weight equal to total recipe weight returns full recipe totals`() {
        val recipe = Recipe(
            id = 1, name = "Stew", totalWeightG = 600.0,
            totalKcal = 900.0, totalProteinG = 45.0, totalCarbsG = 90.0, totalFatG = 30.0,
            createdAt = 0, updatedAt = 0,
        )
        val result = useCase.scaleRecipePortion(recipe, 600.0)
        assertEquals(900.0, result.kcal, 0.001)
        assertEquals(45.0, result.proteinG, 0.001)
        assertEquals(90.0, result.carbsG, 0.001)
        assertEquals(30.0, result.fatG, 0.001)
    }

    @Test
    fun `zero portion weight returns all zero nutrition`() {
        val recipe = Recipe(
            id = 1, name = "Salad", totalWeightG = 200.0,
            totalKcal = 100.0, totalProteinG = 5.0, totalCarbsG = 10.0, totalFatG = 2.0,
            createdAt = 0, updatedAt = 0,
        )
        val result = useCase.scaleRecipePortion(recipe, 0.0)
        assertEquals(0.0, result.kcal, 0.001)
        assertEquals(0.0, result.proteinG, 0.001)
        assertEquals(0.0, result.carbsG, 0.001)
        assertEquals(0.0, result.fatG, 0.001)
    }

    @Test
    fun `portion larger than total recipe weight scales above 100 percent`() {
        // A user could theoretically enter a portion weight greater than the recipe total.
        // The use case should apply the formula without restriction.
        val recipe = Recipe(
            id = 1, name = "Pasta", totalWeightG = 300.0,
            totalKcal = 600.0, totalProteinG = 24.0, totalCarbsG = 90.0, totalFatG = 12.0,
            createdAt = 0, updatedAt = 0,
        )
        // 600g is twice the recipe total: all values should double
        val result = useCase.scaleRecipePortion(recipe, 600.0)
        assertEquals(1200.0, result.kcal, 0.001)
        assertEquals(48.0, result.proteinG, 0.001)
        assertEquals(180.0, result.carbsG, 0.001)
        assertEquals(24.0, result.fatG, 0.001)
    }

}
