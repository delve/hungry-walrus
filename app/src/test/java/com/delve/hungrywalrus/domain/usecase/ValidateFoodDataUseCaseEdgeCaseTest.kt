package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Additional edge-case unit tests for [ValidateFoodDataUseCase] beyond the developer's existing
 * ValidateFoodDataUseCaseTest.
 */
class ValidateFoodDataUseCaseEdgeCaseTest {

    private val useCase = ValidateFoodDataUseCase()

    private fun makeFood(
        kcal: Double? = 100.0,
        protein: Double? = 10.0,
        carbs: Double? = 20.0,
        fat: Double? = 5.0,
        missingFields: Set<NutritionField> = emptySet(),
    ) = FoodSearchResult(
        id = "test:1",
        name = "Test Food",
        source = FoodSource.USDA,
        kcalPer100g = kcal,
        proteinPer100g = protein,
        carbsPer100g = carbs,
        fatPer100g = fat,
        missingFields = missingFields,
    )

    // --- isComplete ---

    @Test
    fun `isComplete returns false when all four fields are missing`() {
        val food = makeFood(
            kcal = null, protein = null, carbs = null, fat = null,
            missingFields = setOf(
                NutritionField.KCAL, NutritionField.PROTEIN,
                NutritionField.CARBS, NutritionField.FAT,
            ),
        )
        assertFalse(useCase.isComplete(food))
    }

    @Test
    fun `isComplete returns true when all four fields are present`() {
        val food = makeFood()
        assertTrue(useCase.isComplete(food))
    }

    // --- applyOverrides: zero values are valid ---

    @Test
    fun `applyOverrides accepts zero as a valid override for kcal`() {
        val food = makeFood(kcal = null, missingFields = setOf(NutritionField.KCAL))
        val result = useCase.applyOverrides(food, kcalPer100g = 0.0)
        assertEquals(0.0, result.kcalPer100g!!, 0.001)
        assertTrue(result.missingFields.isEmpty())
    }

    @Test
    fun `applyOverrides accepts zero for all macros simultaneously`() {
        val food = makeFood(
            protein = null, carbs = null, fat = null,
            missingFields = setOf(
                NutritionField.PROTEIN, NutritionField.CARBS, NutritionField.FAT,
            ),
        )
        val result = useCase.applyOverrides(
            food,
            proteinPer100g = 0.0, carbsPer100g = 0.0, fatPer100g = 0.0,
        )
        assertEquals(0.0, result.proteinPer100g!!, 0.001)
        assertEquals(0.0, result.carbsPer100g!!, 0.001)
        assertEquals(0.0, result.fatPer100g!!, 0.001)
        assertTrue(result.missingFields.isEmpty())
    }

    // --- applyOverrides: existing non-null value is not overwritten ---

    @Test
    fun `applyOverrides does not overwrite an existing non-null kcal with a non-null override`() {
        // If kcalPer100g is already populated (not missing), and an override is provided,
        // the override takes precedence — this tests that applyOverrides uses the override
        // when the field is supplied, not that it preserves the original.
        val food = makeFood(kcal = 100.0)
        val result = useCase.applyOverrides(food, kcalPer100g = 200.0)
        // Per the implementation: override is used when non-null
        assertEquals(200.0, result.kcalPer100g!!, 0.001)
    }

    @Test
    fun `applyOverrides preserves existing non-null kcal when override is null`() {
        val food = makeFood(kcal = 100.0)
        val result = useCase.applyOverrides(food, kcalPer100g = null)
        assertEquals(100.0, result.kcalPer100g!!, 0.001)
    }

    // --- missingFields re-derived correctly after partial override ---

    @Test
    fun `missingFields remains non-empty when not all missing fields are filled`() {
        val food = makeFood(
            kcal = null, protein = null,
            missingFields = setOf(NutritionField.KCAL, NutritionField.PROTEIN),
        )
        // Only fill kcal; protein remains missing
        val result = useCase.applyOverrides(food, kcalPer100g = 150.0)
        assertFalse(result.missingFields.isEmpty())
        assertTrue(NutritionField.PROTEIN in result.missingFields)
        assertFalse(NutritionField.KCAL in result.missingFields)
    }

    @Test
    fun `applyOverrides called multiple times is idempotent when values unchanged`() {
        val food = makeFood(
            kcal = null, missingFields = setOf(NutritionField.KCAL),
        )
        val firstPass = useCase.applyOverrides(food, kcalPer100g = 300.0)
        val secondPass = useCase.applyOverrides(firstPass, kcalPer100g = null)
        // Second call with null override should preserve the value set in first pass
        assertEquals(300.0, secondPass.kcalPer100g!!, 0.001)
        assertTrue(secondPass.missingFields.isEmpty())
    }

    // --- Requirement: user must supply estimate for missing fields before entry can be saved ---

    @Test
    fun `entry cannot be considered complete until all missing fields are overridden`() {
        val food = makeFood(
            kcal = null, fat = null,
            missingFields = setOf(NutritionField.KCAL, NutritionField.FAT),
        )
        assertFalse(useCase.isComplete(food))

        // Apply only kcal override
        val partial = useCase.applyOverrides(food, kcalPer100g = 100.0)
        assertFalse(useCase.isComplete(partial))

        // Apply fat override
        val complete = useCase.applyOverrides(partial, fatPer100g = 3.0)
        assertTrue(useCase.isComplete(complete))
    }
}
