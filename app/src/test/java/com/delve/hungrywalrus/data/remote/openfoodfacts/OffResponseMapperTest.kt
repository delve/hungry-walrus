package com.delve.hungrywalrus.data.remote.openfoodfacts

import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OffResponseMapperTest {

    @Test
    fun `mapProduct with all nutrients present returns complete result`() {
        val product = OffProduct(
            code = "3017620422003",
            productName = "Nutella",
            nutriments = OffNutriments(
                energyKcal100g = 539.0,
                proteins100g = 6.3,
                carbohydrates100g = 57.5,
                fat100g = 30.9,
            ),
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals("off:3017620422003", result.id)
        assertEquals("Nutella", result.name)
        assertEquals(FoodSource.OPEN_FOOD_FACTS, result.source)
        assertEquals(539.0, result.kcalPer100g!!, 0.001)
        assertEquals(6.3, result.proteinPer100g!!, 0.001)
        assertEquals(57.5, result.carbsPer100g!!, 0.001)
        assertEquals(30.9, result.fatPer100g!!, 0.001)
        assertTrue(result.missingFields.isEmpty())
    }

    @Test
    fun `mapProduct with some missing nutrients returns nulls and populates missingFields`() {
        val product = OffProduct(
            code = "123456",
            productName = "Some product",
            nutriments = OffNutriments(
                energyKcal100g = 100.0,
                proteins100g = null,
                carbohydrates100g = 20.0,
                fat100g = null,
            ),
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals(100.0, result.kcalPer100g!!, 0.001)
        assertEquals(null, result.proteinPer100g)
        assertEquals(20.0, result.carbsPer100g!!, 0.001)
        assertEquals(null, result.fatPer100g)
        assertEquals(setOf(NutritionField.PROTEIN, NutritionField.FAT), result.missingFields)
    }

    @Test
    fun `mapProduct with all nutrients missing returns all nulls and full missingFields`() {
        val product = OffProduct(
            code = "000000",
            productName = "Empty product",
            nutriments = null,
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals("off:000000", result.id)
        assertEquals("Empty product", result.name)
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
    fun `mapProduct with null product name falls back to code`() {
        val product = OffProduct(
            code = "789012",
            productName = null,
            nutriments = null,
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals("789012", result.name)
    }

    @Test
    fun `mapProduct with blank product name falls back to code`() {
        val product = OffProduct(
            code = "789012",
            productName = "   ",
            nutriments = null,
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals("789012", result.name)
    }

    @Test
    fun `mapProducts maps list of products correctly`() {
        val products = listOf(
            OffProduct(code = "001", productName = "A", nutriments = null),
            OffProduct(code = "002", productName = "B", nutriments = null),
        )

        val results = OffResponseMapper.mapProducts(products)

        assertEquals(2, results.size)
        assertEquals("off:001", results[0].id)
        assertEquals("off:002", results[1].id)
    }
}
