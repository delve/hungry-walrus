package com.delve.hungrywalrus.integration

import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Integration tests verifying that the use case chain produces nutritional values consistent
 * with the requirements specification:
 *
 *  - Scaling from per-100g reference values to user-entered weights.
 *  - Proportional calculation from recipe total weight to portion weight.
 *  - Daily progress aggregation.
 *  - Rolling 7-day and 28-day cumulative summaries.
 *  - Missing field prompting lifecycle (validate → apply overrides → entry ready).
 */
class NutritionCalculationIntegrationTest {

    private val scaleUseCase = ScaleNutritionUseCase()
    private val validateUseCase = ValidateFoodDataUseCase()
    private val rollingSummaryUseCase = ComputeRollingSummaryUseCase()

    // --- Per-100g scaling to user-entered weight ---

    @Test
    fun `scale 200g chicken breast from 100g reference gives correct values`() {
        // Per 100g: 165 kcal, 31g protein, 0g carbs, 3.6g fat
        val result = scaleUseCase(
            kcalPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
            weightG = 200.0,
        )
        assertEquals(330.0, result.kcal, 0.001)
        assertEquals(62.0, result.proteinG, 0.001)
        assertEquals(0.0, result.carbsG, 0.001)
        assertEquals(7.2, result.fatG, 0.001)
    }

    @Test
    fun `scale 50g oats from 100g reference gives half the reference values`() {
        val result = scaleUseCase(
            kcalPer100g = 374.0,
            proteinPer100g = 13.2,
            carbsPer100g = 67.7,
            fatPer100g = 7.0,
            weightG = 50.0,
        )
        assertEquals(187.0, result.kcal, 0.001)
        assertEquals(6.6, result.proteinG, 0.001)
        assertEquals(33.85, result.carbsG, 0.001)
        assertEquals(3.5, result.fatG, 0.001)
    }

    @Test
    fun `scale 0g of any food returns all zeros`() {
        val result = scaleUseCase(
            kcalPer100g = 500.0,
            proteinPer100g = 25.0,
            carbsPer100g = 50.0,
            fatPer100g = 20.0,
            weightG = 0.0,
        )
        assertEquals(0.0, result.kcal, 0.001)
        assertEquals(0.0, result.proteinG, 0.001)
        assertEquals(0.0, result.carbsG, 0.001)
        assertEquals(0.0, result.fatG, 0.001)
    }

    // --- Recipe portion scaling ---

    @Test
    fun `50 percent of recipe gives half of recipe totals`() {
        val recipe = Recipe(
            id = 1, name = "Bolognese", totalWeightG = 800.0,
            totalKcal = 1200.0, totalProteinG = 80.0, totalCarbsG = 120.0, totalFatG = 40.0,
            createdAt = 0, updatedAt = 0,
        )
        val result = scaleUseCase.scaleRecipePortion(recipe, 400.0)
        assertEquals(600.0, result.kcal, 0.001)
        assertEquals(40.0, result.proteinG, 0.001)
        assertEquals(60.0, result.carbsG, 0.001)
        assertEquals(20.0, result.fatG, 0.001)
    }

    @Test
    fun `25 percent of recipe gives quarter of recipe totals`() {
        val recipe = Recipe(
            id = 1, name = "Curry", totalWeightG = 1000.0,
            totalKcal = 2000.0, totalProteinG = 100.0, totalCarbsG = 200.0, totalFatG = 80.0,
            createdAt = 0, updatedAt = 0,
        )
        val result = scaleUseCase.scaleRecipePortion(recipe, 250.0)
        assertEquals(500.0, result.kcal, 0.001)
        assertEquals(25.0, result.proteinG, 0.001)
        assertEquals(50.0, result.carbsG, 0.001)
        assertEquals(20.0, result.fatG, 0.001)
    }

    // --- Missing field lifecycle: validate → override → complete ---

    @Test
    fun `food with missing kcal is not complete until override supplied`() {
        val food = FoodSearchResult(
            id = "off:123", name = "Mystery Bar", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null, proteinPer100g = 8.0, carbsPer100g = 50.0, fatPer100g = 10.0,
            missingFields = setOf(com.delve.hungrywalrus.domain.model.NutritionField.KCAL),
        )

        // Before override: not complete
        assertTrue(!validateUseCase.isComplete(food))

        // User supplies kcal estimate
        val updated = validateUseCase.applyOverrides(food, kcalPer100g = 300.0)

        // After override: complete and kcal set
        assertTrue(validateUseCase.isComplete(updated))
        assertEquals(300.0, updated.kcalPer100g!!, 0.001)
    }

    @Test
    fun `food with all four missing fields requires all four overrides`() {
        val food = FoodSearchResult(
            id = "usda:999", name = "Unknown Fruit", source = FoodSource.USDA,
            kcalPer100g = null, proteinPer100g = null, carbsPer100g = null, fatPer100g = null,
            missingFields = setOf(
                com.delve.hungrywalrus.domain.model.NutritionField.KCAL,
                com.delve.hungrywalrus.domain.model.NutritionField.PROTEIN,
                com.delve.hungrywalrus.domain.model.NutritionField.CARBS,
                com.delve.hungrywalrus.domain.model.NutritionField.FAT,
            ),
        )

        val step1 = validateUseCase.applyOverrides(food, kcalPer100g = 50.0)
        assertTrue(!validateUseCase.isComplete(step1))

        val step2 = validateUseCase.applyOverrides(step1, proteinPer100g = 1.0)
        assertTrue(!validateUseCase.isComplete(step2))

        val step3 = validateUseCase.applyOverrides(step2, carbsPer100g = 12.0)
        assertTrue(!validateUseCase.isComplete(step3))

        val step4 = validateUseCase.applyOverrides(step3, fatPer100g = 0.2)
        assertTrue(validateUseCase.isComplete(step4))
    }

    // --- Daily progress aggregation ---

    @Test
    fun `daily totals computed by summing individual scaled entries`() {
        // Simulate three log entries with known values — these are already scaled at log time
        val entries = listOf(
            LogEntry(foodName = "Chicken", kcal = 330.0, proteinG = 62.0, carbsG = 0.0, fatG = 7.2, timestamp = 1L),
            LogEntry(foodName = "Rice", kcal = 260.0, proteinG = 5.4, carbsG = 56.0, fatG = 0.6, timestamp = 2L),
            LogEntry(foodName = "Broccoli", kcal = 52.5, proteinG = 5.25, carbsG = 7.5, fatG = 0.75, timestamp = 3L),
        )

        val totalKcal = entries.sumOf { it.kcal }
        val totalProtein = entries.sumOf { it.proteinG }
        val totalCarbs = entries.sumOf { it.carbsG }
        val totalFat = entries.sumOf { it.fatG }

        assertEquals(642.5, totalKcal, 0.001)
        assertEquals(72.65, totalProtein, 0.001)
        assertEquals(63.5, totalCarbs, 0.001)
        assertEquals(8.55, totalFat, 0.001)
    }

    // --- Rolling 7-day summary ---

    @Test
    fun `7-day summary correctly accumulates intake over 7 days`() {
        val start = LocalDate.of(2026, 3, 14)
        val end = LocalDate.of(2026, 3, 20)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )

        // One entry per day for 7 days: 500 kcal, 30g protein, 60g carbs, 15g fat each
        val entries = (0..6).map {
            LogEntry(
                foodName = "Day ${it + 1} food",
                kcal = 500.0, proteinG = 30.0, carbsG = 60.0, fatG = 15.0,
                timestamp = it.toLong(),
            )
        }

        val plans = (0..6).associate { start.plusDays(it.toLong()) to plan }
        val summary = rollingSummaryUseCase(entries, plans, start, end)

        assertEquals(7, summary.periodDays)
        assertEquals(3500.0, summary.totalIntake.kcal, 0.001)
        assertEquals(210.0, summary.totalIntake.proteinG, 0.001)
        assertEquals(420.0, summary.totalIntake.carbsG, 0.001)
        assertEquals(105.0, summary.totalIntake.fatG, 0.001)

        // Daily average = total / 7
        assertEquals(500.0, summary.dailyAverage.kcal, 0.001)
        assertEquals(30.0, summary.dailyAverage.proteinG, 0.001)

        // Target: 7 * 2000 kcal = 14000
        assertNotNull(summary.totalTarget)
        assertEquals(14_000.0, summary.totalTarget!!.kcal, 0.001)
    }

    @Test
    fun `28-day summary correctly accumulates intake and targets`() {
        val start = LocalDate.of(2026, 2, 21)
        val end = LocalDate.of(2026, 3, 20)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )

        val entries = (0..27).map {
            LogEntry(
                foodName = "Food $it",
                kcal = 1800.0, proteinG = 130.0, carbsG = 220.0, fatG = 60.0,
                timestamp = it.toLong(),
            )
        }

        val plans = (0..27).associate { start.plusDays(it.toLong()) to plan }
        val summary = rollingSummaryUseCase(entries, plans, start, end)

        assertEquals(28, summary.periodDays)
        assertEquals(28 * 1800.0, summary.totalIntake.kcal, 0.001)
        assertNotNull(summary.totalTarget)
        assertEquals(28 * 2000.0, summary.totalTarget!!.kcal, 0.001)
    }

    @Test
    fun `totalTarget is null in 7-day summary when plan missing for one day`() {
        val start = LocalDate.of(2026, 3, 14)
        val end = LocalDate.of(2026, 3, 20)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )

        // Only 6 of 7 days have a plan
        val plans = (0..5).associate { start.plusDays(it.toLong()) to plan }
        val summary = rollingSummaryUseCase(emptyList(), plans, start, end)

        assertNull(summary.totalTarget)
    }

    // --- Verify that scale + sum produces same result as direct calculation ---

    @Test
    fun `scaling two ingredients and summing matches manually computed totals`() {
        // Ingredient 1: 200g chicken (165 kcal, 31g protein, 0g carbs, 3.6g fat per 100g)
        val chicken = scaleUseCase(165.0, 31.0, 0.0, 3.6, 200.0)
        // Ingredient 2: 150g rice (130 kcal, 2.7g protein, 28g carbs, 0.3g fat per 100g)
        val rice = scaleUseCase(130.0, 2.7, 28.0, 0.3, 150.0)

        val totalKcal = chicken.kcal + rice.kcal
        val totalProtein = chicken.proteinG + rice.proteinG
        val totalCarbs = chicken.carbsG + rice.carbsG
        val totalFat = chicken.fatG + rice.fatG

        // chicken: 330 kcal, 62g protein, 0g carbs, 7.2g fat
        // rice: 195 kcal, 4.05g protein, 42g carbs, 0.45g fat
        assertEquals(525.0, totalKcal, 0.001)
        assertEquals(66.05, totalProtein, 0.001)
        assertEquals(42.0, totalCarbs, 0.001)
        assertEquals(7.65, totalFat, 0.001)
    }
}
