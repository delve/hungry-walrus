package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Additional edge-case tests for [ComputeRollingSummaryUseCase] covering requirements not tested
 * in the developer's ComputeRollingSummaryUseCaseTest.
 */
class ComputeRollingSummaryUseCaseEdgeCaseTest {

    private val useCase = ComputeRollingSummaryUseCase()

    private val start7 = LocalDate.of(2026, 3, 14)
    private val end7 = LocalDate.of(2026, 3, 20) // 7-day window

    private val uniformPlan = NutritionPlan(
        id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
        carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
    )

    private fun makePlan(kcal: Int) = NutritionPlan(
        id = 1, kcalTarget = kcal, proteinTargetG = 100.0,
        carbsTargetG = 200.0, fatTargetG = 50.0, effectiveFrom = 0L,
    )

    private fun makeEntry(kcal: Double, protein: Double = 10.0, carbs: Double = 20.0, fat: Double = 5.0) =
        LogEntry(foodName = "Food", kcal = kcal, proteinG = protein, carbsG = carbs, fatG = fat, timestamp = 0L)

    // --- Daily progress aggregation ---

    @Test
    fun `total intake sums all entries regardless of date field on entry`() {
        // The use case does not filter entries by date — the ViewModel is responsible for passing only entries within the [start, end] window.
        val entries = (1..7).map { makeEntry(kcal = 300.0, protein = 30.0, carbs = 40.0, fat = 10.0) }
        val plans = (0..6).associate { start7.plusDays(it.toLong()) to uniformPlan }
        val result = useCase(entries, plans, start7, end7)
        assertEquals(2100.0, result.totalIntake.kcal, 0.001)
        assertEquals(210.0, result.totalIntake.proteinG, 0.001)
        assertEquals(280.0, result.totalIntake.carbsG, 0.001)
        assertEquals(70.0, result.totalIntake.fatG, 0.001)
    }

    @Test
    fun `dailyAverage is zero for all nutrients when entries list is empty`() {
        val plans = (0..6).associate { start7.plusDays(it.toLong()) to uniformPlan }
        val result = useCase(emptyList(), plans, start7, end7)
        assertEquals(0.0, result.dailyAverage.kcal, 0.001)
        assertEquals(0.0, result.dailyAverage.proteinG, 0.001)
        assertEquals(0.0, result.dailyAverage.carbsG, 0.001)
        assertEquals(0.0, result.dailyAverage.fatG, 0.001)
    }

    // --- Rolling 7-day and 28-day cumulative targets ---

    @Test
    fun `28-day window accumulates 28 days of plan targets`() {
        val start28 = LocalDate.of(2026, 2, 21)
        val end28 = LocalDate.of(2026, 3, 20)
        val plans = (0..27).associate { start28.plusDays(it.toLong()) to makePlan(2000) }
        val result = useCase(emptyList(), plans, start28, end28)
        assertEquals(28, result.periodDays)
        assertNotNull(result.totalTarget)
        assertEquals(28 * 2000.0, result.totalTarget!!.kcal, 0.001)
    }

    @Test
    fun `totalTarget is null for 28-day window when any day is missing a plan`() {
        val start28 = LocalDate.of(2026, 2, 21)
        val end28 = LocalDate.of(2026, 3, 20)
        // Provide plans for only 27 of 28 days
        val plans = (0..26).associate { start28.plusDays(it.toLong()) to makePlan(2000) }
        val result = useCase(emptyList(), plans, start28, end28)
        assertNull(result.totalTarget)
    }

    // --- Plan changes mid-period ---

    @Test
    fun `mid-period plan change accumulates different targets for each day`() {
        // Days 0-3: planA (1500 kcal/day), Days 4-6: planB (2500 kcal/day)
        val planA = makePlan(1500)
        val planB = makePlan(2500)
        val plans = mapOf(
            start7 to planA,
            start7.plusDays(1) to planA,
            start7.plusDays(2) to planA,
            start7.plusDays(3) to planA,
            start7.plusDays(4) to planB,
            start7.plusDays(5) to planB,
            start7.plusDays(6) to planB,
        )
        val result = useCase(emptyList(), plans, start7, end7)
        assertNotNull(result.totalTarget)
        // 4 * 1500 + 3 * 2500 = 6000 + 7500 = 13500
        assertEquals(13_500.0, result.totalTarget!!.kcal, 0.001)
    }

    // --- Period length ---

    @Test
    fun `periodDays for 28-day window is exactly 28`() {
        val start28 = LocalDate.of(2026, 2, 21)
        val end28 = LocalDate.of(2026, 3, 20)
        val result = useCase(emptyList(), emptyMap(), start28, end28)
        assertEquals(28, result.periodDays)
    }

    // --- Very large values ---

    @Test
    fun `very large entry values are accumulated without overflow`() {
        // Entries with very large kcal (e.g. industrial/bulk quantity entry)
        val entries = listOf(
            makeEntry(kcal = 100_000.0, protein = 5_000.0, carbs = 10_000.0, fat = 2_000.0),
            makeEntry(kcal = 100_000.0, protein = 5_000.0, carbs = 10_000.0, fat = 2_000.0),
        )
        val result = useCase(entries, emptyMap(), start7, end7)
        assertEquals(200_000.0, result.totalIntake.kcal, 0.001)
        assertEquals(10_000.0, result.totalIntake.proteinG, 0.001)
    }
}
