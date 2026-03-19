package com.delve.hungrywalrus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodSearchResultTest {

    @Test
    fun `FoodSearchResult with all values has empty missingFields`() {
        val result = FoodSearchResult(
            id = "usda:123",
            name = "Chicken breast",
            source = FoodSource.USDA,
            kcalPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
            missingFields = emptySet(),
        )
        assertTrue(result.missingFields.isEmpty())
    }

    @Test
    fun `FoodSearchResult missing values reflected in missingFields`() {
        val result = FoodSearchResult(
            id = "off:123",
            name = "Some product",
            source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null,
            proteinPer100g = 10.0,
            carbsPer100g = null,
            fatPer100g = 5.0,
            missingFields = setOf(NutritionField.KCAL, NutritionField.CARBS),
        )
        assertEquals(2, result.missingFields.size)
        assertTrue(result.missingFields.contains(NutritionField.KCAL))
        assertTrue(result.missingFields.contains(NutritionField.CARBS))
    }

    @Test
    fun `FoodSource enum has three values`() {
        assertEquals(3, FoodSource.entries.size)
        assertEquals(FoodSource.USDA, FoodSource.valueOf("USDA"))
        assertEquals(FoodSource.OPEN_FOOD_FACTS, FoodSource.valueOf("OPEN_FOOD_FACTS"))
        assertEquals(FoodSource.MANUAL, FoodSource.valueOf("MANUAL"))
    }

    @Test
    fun `NutritionField enum has four values`() {
        assertEquals(4, NutritionField.entries.size)
    }
}
