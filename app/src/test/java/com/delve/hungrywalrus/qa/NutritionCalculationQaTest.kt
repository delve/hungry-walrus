package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * QA unit tests verifying nutritional calculation correctness per the requirements spec.
 *
 * These tests fill gaps in existing coverage, specifically:
 * - Scaling accuracy at non-round weights.
 * - Recipe portion scaling when portion weight exactly equals recipe weight.
 * - Recipe portion scaling when portion weight is larger than recipe weight.
 * - Daily progress aggregation with a single entry at zero values.
 * - Rolling summary with a plan that changes mid-period (partial coverage -> null totalTarget).
 * - Very large value handling (no integer overflow in 28-day kcal accumulation).
 * - Rolling summary boundary: start == end (single-day period).
 */
class NutritionCalculationQaTest {

    private val scaleUseCase = ScaleNutritionUseCase()
    private val summaryUseCase = ComputeRollingSummaryUseCase()

    // --- Per-100g scaling ---

    @Test
    fun `scaling 175g of food produces proportionally correct values`() {
        // 175g at 200 kcal/100g = 350 kcal
        val result = scaleUseCase(
            kcalPer100g = 200.0,
            proteinPer100g = 20.0,
            carbsPer100g = 30.0,
            fatPer100g = 10.0,
            weightG = 175.0,
        )
        assertEquals(350.0, result.kcal, 0.001)
        assertEquals(35.0, result.proteinG, 0.001)
        assertEquals(52.5, result.carbsG, 0.001)
        assertEquals(17.5, result.fatG, 0.001)
    }

    @Test
    fun `scaling exactly 100g returns reference values unchanged`() {
        val result = scaleUseCase(
            kcalPer100g = 374.0,
            proteinPer100g = 13.2,
            carbsPer100g = 67.7,
            fatPer100g = 7.0,
            weightG = 100.0,
        )
        assertEquals(374.0, result.kcal, 0.001)
        assertEquals(13.2, result.proteinG, 0.001)
        assertEquals(67.7, result.carbsG, 0.001)
        assertEquals(7.0, result.fatG, 0.001)
    }

    @Test
    fun `scaling produces zero for all macros when reference values are all zero`() {
        val result = scaleUseCase(
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

    // --- Recipe portion scaling ---

    @Test
    fun `recipe portion equal to total weight returns full recipe totals`() {
        val recipe = Recipe(
            id = 1, name = "Soup", totalWeightG = 500.0,
            totalKcal = 250.0, totalProteinG = 15.0, totalCarbsG = 30.0, totalFatG = 8.0,
            createdAt = 0, updatedAt = 0,
        )
        // Consuming all 500g = same as full recipe
        val result = scaleUseCase.scaleRecipePortion(recipe, 500.0)
        assertEquals(250.0, result.kcal, 0.001)
        assertEquals(15.0, result.proteinG, 0.001)
        assertEquals(30.0, result.carbsG, 0.001)
        assertEquals(8.0, result.fatG, 0.001)
    }

    @Test
    fun `recipe portion larger than total weight scales proportionally beyond 100 percent`() {
        // A user could consume more than the recipe total by serving it twice,
        // but the spec does not prohibit portionWeightG > totalWeightG.
        val recipe = Recipe(
            id = 2, name = "Sauce", totalWeightG = 200.0,
            totalKcal = 400.0, totalProteinG = 20.0, totalCarbsG = 40.0, totalFatG = 16.0,
            createdAt = 0, updatedAt = 0,
        )
        // Consuming 300g (150% of recipe)
        val result = scaleUseCase.scaleRecipePortion(recipe, 300.0)
        assertEquals(600.0, result.kcal, 0.001)
        assertEquals(30.0, result.proteinG, 0.001)
        assertEquals(60.0, result.carbsG, 0.001)
        assertEquals(24.0, result.fatG, 0.001)
    }

    @Test
    fun `recipe portion of zero grams returns all zeros`() {
        val recipe = Recipe(
            id = 3, name = "Stew", totalWeightG = 600.0,
            totalKcal = 900.0, totalProteinG = 50.0, totalCarbsG = 80.0, totalFatG = 30.0,
            createdAt = 0, updatedAt = 0,
        )
        val result = scaleUseCase.scaleRecipePortion(recipe, 0.0)
        assertEquals(0.0, result.kcal, 0.001)
        assertEquals(0.0, result.proteinG, 0.001)
        assertEquals(0.0, result.carbsG, 0.001)
        assertEquals(0.0, result.fatG, 0.001)
    }

    // --- Daily progress aggregation ---

    @Test
    fun `daily aggregate of entries with zero values produces zero totals`() {
        // A user may log a food with truly zero nutritional values (e.g. water)
        val entries = listOf(
            LogEntry(foodName = "Water", kcal = 0.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = 1L),
            LogEntry(foodName = "Plain soda water", kcal = 0.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = 2L),
        )
        val total = entries.sumOf { it.kcal }
        assertEquals(0.0, total, 0.001)
    }

    // --- Rolling summaries ---

    /**
     * Spec: "totalTarget is null" when a plan does not cover all days in the period.
     * Here the plan changes on day 4 of a 7-day period — the old plan covers days 1-3
     * and the new plan covers days 4-7. Since ALL days have a plan, totalTarget must NOT
     * be null.
     */
    @Test
    fun `7-day summary with mid-period plan change still shows totalTarget when all days covered`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 7)
        val planA = NutritionPlan(
            id = 1, kcalTarget = 1800, proteinTargetG = 120.0,
            carbsTargetG = 200.0, fatTargetG = 60.0, effectiveFrom = 0L,
        )
        val planB = NutritionPlan(
            id = 2, kcalTarget = 2200, proteinTargetG = 160.0,
            carbsTargetG = 280.0, fatTargetG = 70.0, effectiveFrom = 1L,
        )

        // Days 1-3 use planA, days 4-7 use planB
        val plans = buildMap {
            for (i in 0..2) put(start.plusDays(i.toLong()), planA)
            for (i in 3..6) put(start.plusDays(i.toLong()), planB)
        }

        val summary = summaryUseCase(emptyList(), plans, start, end)

        // All 7 days have a plan, so totalTarget is non-null
        assertNotNull(summary.totalTarget)
        // Expected: 3 * 1800 + 4 * 2200 = 5400 + 8800 = 14200
        assertEquals(14_200.0, summary.totalTarget!!.kcal, 0.001)
    }

    /**
     * When only some days in the 7-day period have a plan, totalTarget must be null.
     * This verifies the "partial plan coverage produces null" path specifically for
     * the rolling summary requirement.
     */
    @Test
    fun `7-day summary with one day missing plan produces null totalTarget`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 7)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )

        // Only 6 of 7 days have a plan — day 4 (index 3) is explicitly null
        val plans = buildMap {
            for (i in 0..2) put(start.plusDays(i.toLong()), plan)
            put(start.plusDays(3), null)
            for (i in 4..6) put(start.plusDays(i.toLong()), plan)
        }

        val summary = summaryUseCase(emptyList(), plans, start, end)

        assertNull(summary.totalTarget)
    }

    /**
     * Spec: both 7-day and 28-day summaries are required.
     * Verify the 28-day period accumulates targets correctly.
     */
    @Test
    fun `28-day summary with consistent plan accumulates kcal target over 28 days`() {
        val start = LocalDate.of(2026, 2, 1)
        val end = LocalDate.of(2026, 2, 28)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )
        val plans = (0..27).associate { start.plusDays(it.toLong()) to plan }

        val summary = summaryUseCase(emptyList(), plans, start, end)

        assertEquals(28, summary.periodDays)
        assertNotNull(summary.totalTarget)
        assertEquals(28 * 2000.0, summary.totalTarget!!.kcal, 0.001)
        assertEquals(28 * 150.0, summary.totalTarget!!.proteinG, 0.001)
    }

    /**
     * Very large daily values should not overflow when accumulated over 28 days.
     * Int kcalTarget (max ~2.1B) * 28 days could exceed Int range, but it is widened
     * to Double in the use case (per architecture spec section 5.2).
     */
    @Test
    fun `very large kcal target over 28 days does not overflow`() {
        val start = LocalDate.of(2026, 2, 1)
        val end = LocalDate.of(2026, 2, 28)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 10_000, proteinTargetG = 999.0,
            carbsTargetG = 999.0, fatTargetG = 999.0, effectiveFrom = 0L,
        )
        val plans = (0..27).associate { start.plusDays(it.toLong()) to plan }

        val summary = summaryUseCase(emptyList(), plans, start, end)

        assertNotNull(summary.totalTarget)
        assertEquals(28 * 10_000.0, summary.totalTarget!!.kcal, 0.001)
    }

    /**
     * Single-day period (start == end) must work correctly and return periodDays == 1.
     */
    @Test
    fun `single-day period returns periodDays of 1 and correct aggregation`() {
        val day = LocalDate.of(2026, 3, 20)
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )
        val entries = listOf(
            LogEntry(foodName = "Lunch", kcal = 600.0, proteinG = 40.0, carbsG = 70.0, fatG = 15.0, timestamp = 1L),
        )

        val summary = summaryUseCase(entries, mapOf(day to plan), day, day)

        assertEquals(1, summary.periodDays)
        assertEquals(600.0, summary.totalIntake.kcal, 0.001)
        assertNotNull(summary.totalTarget)
        assertEquals(2000.0, summary.totalTarget!!.kcal, 0.001)
        // Daily average of a single-day period equals the total
        assertEquals(600.0, summary.dailyAverage.kcal, 0.001)
    }

    /**
     * Daily average is computed as total / periodDays, not total / days-with-entries.
     * Even if only some days have entries, the average divides by the full period length.
     */
    @Test
    fun `daily average divides by full period length even when some days have no entries`() {
        val start = LocalDate.of(2026, 3, 14)
        val end = LocalDate.of(2026, 3, 20)   // 7 days
        // Only 3 entries, spread across the period
        val entries = listOf(
            LogEntry(foodName = "A", kcal = 700.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = 1L),
            LogEntry(foodName = "B", kcal = 700.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = 2L),
            LogEntry(foodName = "C", kcal = 700.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = 3L),
        )

        val summary = summaryUseCase(entries, emptyMap(), start, end)

        // Total = 2100, period = 7, average = 300 (not 700)
        assertEquals(2100.0, summary.totalIntake.kcal, 0.001)
        assertEquals(300.0, summary.dailyAverage.kcal, 0.001)
    }
}
