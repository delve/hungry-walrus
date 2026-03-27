package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.data.remote.openfoodfacts.OffNutriments
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffProduct
import com.delve.hungrywalrus.data.remote.openfoodfacts.OffResponseMapper
import com.delve.hungrywalrus.data.remote.usda.UsdaFood
import com.delve.hungrywalrus.data.remote.usda.UsdaNutrient
import com.delve.hungrywalrus.data.remote.usda.UsdaResponseMapper
import com.delve.hungrywalrus.domain.model.NutritionField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QA unit tests for edge cases in [UsdaResponseMapper] and [OffResponseMapper]
 * that are not covered by the existing mapper test files.
 *
 * Gaps targeted:
 * - Duplicate nutrient IDs in a USDA response (last-wins via associateBy).
 * - USDA food with null foodNutrients list is handled safely.
 * - OFF product with null productName AND empty code produces a name from code.
 * - OFF product with null productName and non-empty code uses code as name.
 * - mapFoods with an empty list returns an empty list (not null).
 * - mapProducts with an empty list returns an empty list.
 */
class ResponseMapperEdgeCasesQaTest {

    // --- UsdaResponseMapper edge cases ---

    /**
     * Requirement: USDA nutrient IDs are used to look up values via associateBy.
     * When a USDA response contains duplicate entries for the same nutrient ID,
     * Kotlin's associateBy takes the last value for a duplicate key.
     * This test confirms that behaviour: the last entry wins.
     */
    @Test
    fun `duplicate nutrient IDs in USDA response - last value wins`() {
        val food = UsdaFood(
            fdcId = 1001L,
            description = "Duplicate nutrient food",
            foodNutrients = listOf(
                UsdaNutrient(nutrientId = 1008, value = 100.0),  // first kcal entry
                UsdaNutrient(nutrientId = 1008, value = 150.0),  // second kcal entry (last wins)
                UsdaNutrient(nutrientId = 1003, value = 20.0),
                UsdaNutrient(nutrientId = 1005, value = 30.0),
                UsdaNutrient(nutrientId = 1004, value = 5.0),
            ),
        )

        val result = UsdaResponseMapper.mapFood(food)

        // Last kcal entry (150.0) wins via associateBy
        assertEquals(150.0, result.kcalPer100g!!, 0.001)
        assertEquals(20.0, result.proteinPer100g!!, 0.001)
        assertTrue(result.missingFields.isEmpty())
    }

    /**
     * USDA food with an empty foodNutrients list should produce all-null fields
     * and all four missing fields. The default empty list is already handled but
     * we explicitly verify the contract.
     */
    @Test
    fun `USDA food with empty foodNutrients list has all fields missing`() {
        val food = UsdaFood(
            fdcId = 9999L,
            description = "No nutrients",
            foodNutrients = emptyList(),
        )

        val result = UsdaResponseMapper.mapFood(food)

        assertEquals("usda:9999", result.id)
        assertEquals("No nutrients", result.name)
        assertEquals(null, result.kcalPer100g)
        assertEquals(null, result.proteinPer100g)
        assertEquals(null, result.carbsPer100g)
        assertEquals(null, result.fatPer100g)
        assertEquals(
            setOf(NutritionField.KCAL, NutritionField.PROTEIN, NutritionField.CARBS, NutritionField.FAT),
            result.missingFields,
        )
    }

    /**
     * An unknown nutrient ID should not affect the mapped result — only the four
     * recognised IDs (1008, 1003, 1005, 1004) are extracted. All others are ignored.
     */
    @Test
    fun `USDA food with only unknown nutrient IDs produces all-null result`() {
        val food = UsdaFood(
            fdcId = 5555L,
            description = "Unknown nutrient food",
            foodNutrients = listOf(
                UsdaNutrient(nutrientId = 9999, value = 999.0),  // Unknown ID
                UsdaNutrient(nutrientId = 1234, value = 50.0),   // Unknown ID
            ),
        )

        val result = UsdaResponseMapper.mapFood(food)

        assertEquals(null, result.kcalPer100g)
        assertEquals(null, result.proteinPer100g)
        assertEquals(null, result.carbsPer100g)
        assertEquals(null, result.fatPer100g)
        assertEquals(4, result.missingFields.size)
    }

    /**
     * mapFoods with an empty list should return an empty list, not null or throw.
     */
    @Test
    fun `mapFoods with empty list returns empty list`() {
        val result = UsdaResponseMapper.mapFoods(emptyList())
        assertTrue(result.isEmpty())
    }

    // --- OffResponseMapper edge cases ---

    /**
     * Spec / OffResponseMapper: when productName is null or blank, the product code is used
     * as the display name. This test verifies a non-empty product code is used correctly.
     */
    @Test
    fun `OFF product with null productName uses product code as display name`() {
        val product = OffProduct(
            code = "5060339300001",
            productName = null,
            nutriments = OffNutriments(
                energyKcal100g = 200.0,
                proteins100g = 10.0,
                carbohydrates100g = 25.0,
                fat100g = 8.0,
            ),
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals("5060339300001", result.name)
        assertEquals("off:5060339300001", result.id)
    }

    /**
     * When productName is blank (not null, but whitespace-only), the product code
     * should be used as the display name.
     */
    @Test
    fun `OFF product with blank productName uses product code as display name`() {
        val product = OffProduct(
            code = "7622300489724",
            productName = "   ",  // blank, not null
            nutriments = OffNutriments(
                energyKcal100g = 350.0,
                proteins100g = 5.0,
                carbohydrates100g = 60.0,
                fat100g = 12.0,
            ),
        )

        val result = OffResponseMapper.mapProduct(product)

        // The mapper uses: if productName.isNullOrBlank() -> product.code
        assertEquals("7622300489724", result.name)
    }

    /**
     * OFF product with null productName AND empty code string ("") uses empty string as name.
     * This is the minimum defensive behaviour: the name will be "" rather than crashing.
     * The UI is responsible for handling an empty name gracefully.
     */
    @Test
    fun `OFF product with null productName and empty code produces empty string name`() {
        val product = OffProduct(
            code = "",
            productName = null,
            nutriments = null,
        )

        val result = OffResponseMapper.mapProduct(product)

        // productName is null -> use code which is "" -> name is ""
        assertEquals("", result.name)
        assertEquals("off:", result.id)
        // null nutriments -> all missing
        assertEquals(
            setOf(NutritionField.KCAL, NutritionField.PROTEIN, NutritionField.CARBS, NutritionField.FAT),
            result.missingFields,
        )
    }

    /**
     * OFF product with null nutriments should produce all-null per-100g values and
     * all four fields in missingFields.
     */
    @Test
    fun `OFF product with null nutriments has all fields missing`() {
        val product = OffProduct(
            code = "1234567890123",
            productName = "No nutrients product",
            nutriments = null,
        )

        val result = OffResponseMapper.mapProduct(product)

        assertEquals("No nutrients product", result.name)
        assertEquals(null, result.kcalPer100g)
        assertEquals(null, result.proteinPer100g)
        assertEquals(null, result.carbsPer100g)
        assertEquals(null, result.fatPer100g)
        assertEquals(
            setOf(NutritionField.KCAL, NutritionField.PROTEIN, NutritionField.CARBS, NutritionField.FAT),
            result.missingFields,
        )
    }

    /**
     * mapProducts with an empty list should return an empty list.
     */
    @Test
    fun `mapProducts with empty list returns empty list`() {
        val result = OffResponseMapper.mapProducts(emptyList())
        assertTrue(result.isEmpty())
    }
}
