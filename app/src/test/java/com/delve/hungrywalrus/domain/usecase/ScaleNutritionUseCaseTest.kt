package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.Recipe
import org.junit.Assert.assertEquals
import org.junit.Test

class ScaleNutritionUseCaseTest {

    private val useCase = ScaleNutritionUseCase()

    @Test
    fun `invoke scales per-100g values by weight`() {
        val result = useCase(
            kcalPer100g = 250.0,
            proteinPer100g = 20.0,
            carbsPer100g = 30.0,
            fatPer100g = 10.0,
            weightG = 200.0,
        )
        assertEquals(500.0, result.kcal, 0.001)
        assertEquals(40.0, result.proteinG, 0.001)
        assertEquals(60.0, result.carbsG, 0.001)
        assertEquals(20.0, result.fatG, 0.001)
    }

    @Test
    fun `invoke returns zero values for zero weight`() {
        val result = useCase(
            kcalPer100g = 250.0,
            proteinPer100g = 20.0,
            carbsPer100g = 30.0,
            fatPer100g = 10.0,
            weightG = 0.0,
        )
        assertEquals(0.0, result.kcal, 0.001)
        assertEquals(0.0, result.proteinG, 0.001)
        assertEquals(0.0, result.carbsG, 0.001)
        assertEquals(0.0, result.fatG, 0.001)
    }

    @Test
    fun `scaleRecipePortion scales recipe correctly`() {
        val recipe = Recipe(
            id = 1,
            name = "Test Recipe",
            totalWeightG = 500.0,
            totalKcal = 1000.0,
            totalProteinG = 80.0,
            totalCarbsG = 120.0,
            totalFatG = 40.0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val result = useCase.scaleRecipePortion(recipe, portionWeightG = 250.0)
        assertEquals(500.0, result.kcal, 0.001)
        assertEquals(40.0, result.proteinG, 0.001)
        assertEquals(60.0, result.carbsG, 0.001)
        assertEquals(20.0, result.fatG, 0.001)
    }

    @Test
    fun `scaleRecipePortion with full recipe weight returns total values`() {
        val recipe = Recipe(
            id = 1,
            name = "Test Recipe",
            totalWeightG = 500.0,
            totalKcal = 1000.0,
            totalProteinG = 80.0,
            totalCarbsG = 120.0,
            totalFatG = 40.0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        val result = useCase.scaleRecipePortion(recipe, portionWeightG = 500.0)
        assertEquals(1000.0, result.kcal, 0.001)
        assertEquals(80.0, result.proteinG, 0.001)
        assertEquals(120.0, result.carbsG, 0.001)
        assertEquals(40.0, result.fatG, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scaleRecipePortion throws for zero recipe weight`() {
        val recipe = Recipe(
            id = 1,
            name = "Test Recipe",
            totalWeightG = 0.0,
            totalKcal = 1000.0,
            totalProteinG = 80.0,
            totalCarbsG = 120.0,
            totalFatG = 40.0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        useCase.scaleRecipePortion(recipe, portionWeightG = 250.0)
    }
}
