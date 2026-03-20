package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateFoodDataUseCaseTest {

    private val useCase = ValidateFoodDataUseCase()

    private fun completeResult(): FoodSearchResult = FoodSearchResult(
        id = "test-1",
        name = "Test Food",
        source = FoodSource.USDA,
        kcalPer100g = 250.0,
        proteinPer100g = 20.0,
        carbsPer100g = 30.0,
        fatPer100g = 10.0,
        missingFields = emptySet(),
    )

    private fun resultWithMissing(
        kcal: Double? = 250.0,
        protein: Double? = 20.0,
        carbs: Double? = 30.0,
        fat: Double? = 10.0,
    ): FoodSearchResult {
        val missing = buildSet {
            if (kcal == null) add(NutritionField.KCAL)
            if (protein == null) add(NutritionField.PROTEIN)
            if (carbs == null) add(NutritionField.CARBS)
            if (fat == null) add(NutritionField.FAT)
        }
        return FoodSearchResult(
            id = "test-1",
            name = "Test Food",
            source = FoodSource.USDA,
            kcalPer100g = kcal,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            fatPer100g = fat,
            missingFields = missing,
        )
    }

    @Test
    fun `isComplete returns true when missingFields is empty`() {
        assertTrue(useCase.isComplete(completeResult()))
    }

    @Test
    fun `isComplete returns false when missingFields is non-empty`() {
        val result = resultWithMissing(kcal = null)
        assertFalse(useCase.isComplete(result))
    }

    @Test
    fun `applyOverrides fills missing kcal`() {
        val result = resultWithMissing(kcal = null)
        val updated = useCase.applyOverrides(result, kcalPer100g = 300.0)
        assertEquals(300.0, updated.kcalPer100g!!, 0.001)
        assertTrue(updated.missingFields.isEmpty())
    }

    @Test
    fun `applyOverrides fills multiple missing fields`() {
        val result = resultWithMissing(protein = null, carbs = null)
        val updated = useCase.applyOverrides(result, proteinPer100g = 25.0, carbsPer100g = 35.0)
        assertEquals(25.0, updated.proteinPer100g!!, 0.001)
        assertEquals(35.0, updated.carbsPer100g!!, 0.001)
        assertTrue(updated.missingFields.isEmpty())
    }

    @Test
    fun `applyOverrides does not replace existing value with null`() {
        val result = completeResult()
        val updated = useCase.applyOverrides(result, kcalPer100g = null)
        assertEquals(250.0, updated.kcalPer100g!!, 0.001)
    }

    @Test
    fun `applyOverrides re-derives missingFields after override`() {
        val result = resultWithMissing(kcal = null, fat = null)
        val updated = useCase.applyOverrides(result, kcalPer100g = 300.0)
        assertEquals(setOf(NutritionField.FAT), updated.missingFields)
    }
}
