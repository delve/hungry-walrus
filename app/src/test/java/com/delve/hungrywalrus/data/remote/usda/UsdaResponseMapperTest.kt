package com.delve.hungrywalrus.data.remote.usda

import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsdaResponseMapperTest {

    @Test
    fun `mapFood with all nutrients present returns complete result`() {
        val food = UsdaFood(
            fdcId = 12345,
            description = "Chicken breast, raw",
            foodNutrients = listOf(
                UsdaNutrient(nutrientId = 1008, value = 120.0),
                UsdaNutrient(nutrientId = 1003, value = 22.5),
                UsdaNutrient(nutrientId = 1005, value = 0.0),
                UsdaNutrient(nutrientId = 1004, value = 2.6),
            ),
        )

        val result = UsdaResponseMapper.mapFood(food)

        assertEquals("usda:12345", result.id)
        assertEquals("Chicken breast, raw", result.name)
        assertEquals(FoodSource.USDA, result.source)
        assertEquals(120.0, result.kcalPer100g!!, 0.001)
        assertEquals(22.5, result.proteinPer100g!!, 0.001)
        assertEquals(0.0, result.carbsPer100g!!, 0.001)
        assertEquals(2.6, result.fatPer100g!!, 0.001)
        assertTrue(result.missingFields.isEmpty())
    }

    @Test
    fun `mapFood with some missing nutrients returns nulls and populates missingFields`() {
        val food = UsdaFood(
            fdcId = 99999,
            description = "Mystery food",
            foodNutrients = listOf(
                UsdaNutrient(nutrientId = 1008, value = 200.0),
                UsdaNutrient(nutrientId = 1004, value = 10.0),
            ),
        )

        val result = UsdaResponseMapper.mapFood(food)

        assertEquals("usda:99999", result.id)
        assertEquals(200.0, result.kcalPer100g!!, 0.001)
        assertEquals(null, result.proteinPer100g)
        assertEquals(null, result.carbsPer100g)
        assertEquals(10.0, result.fatPer100g!!, 0.001)
        assertEquals(setOf(NutritionField.PROTEIN, NutritionField.CARBS), result.missingFields)
    }

    @Test
    fun `mapFood with all nutrients missing returns all nulls and full missingFields`() {
        val food = UsdaFood(
            fdcId = 11111,
            description = "Unknown food",
            foodNutrients = emptyList(),
        )

        val result = UsdaResponseMapper.mapFood(food)

        assertEquals("usda:11111", result.id)
        assertEquals("Unknown food", result.name)
        assertEquals(null, result.kcalPer100g)
        assertEquals(null, result.proteinPer100g)
        assertEquals(null, result.carbsPer100g)
        assertEquals(null, result.fatPer100g)
        assertEquals(
            setOf(NutritionField.KCAL, NutritionField.PROTEIN, NutritionField.CARBS, NutritionField.FAT),
            result.missingFields,
        )
    }

    @Test
    fun `mapFood with null nutrient value treats as missing`() {
        val food = UsdaFood(
            fdcId = 22222,
            description = "Partial food",
            foodNutrients = listOf(
                UsdaNutrient(nutrientId = 1008, value = null),
                UsdaNutrient(nutrientId = 1003, value = 15.0),
                UsdaNutrient(nutrientId = 1005, value = null),
                UsdaNutrient(nutrientId = 1004, value = 5.0),
            ),
        )

        val result = UsdaResponseMapper.mapFood(food)

        assertEquals(null, result.kcalPer100g)
        assertEquals(15.0, result.proteinPer100g!!, 0.001)
        assertEquals(null, result.carbsPer100g)
        assertEquals(5.0, result.fatPer100g!!, 0.001)
        assertEquals(setOf(NutritionField.KCAL, NutritionField.CARBS), result.missingFields)
    }

    @Test
    fun `mapFoods maps list of foods correctly`() {
        val foods = listOf(
            UsdaFood(fdcId = 1, description = "Apple", foodNutrients = emptyList()),
            UsdaFood(fdcId = 2, description = "Banana", foodNutrients = emptyList()),
        )

        val results = UsdaResponseMapper.mapFoods(foods)

        assertEquals(2, results.size)
        assertEquals("usda:1", results[0].id)
        assertEquals("usda:2", results[1].id)
    }
}
