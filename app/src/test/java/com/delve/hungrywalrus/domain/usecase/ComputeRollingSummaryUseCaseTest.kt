package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ComputeRollingSummaryUseCaseTest {

    private val useCase = ComputeRollingSummaryUseCase()

    private val start = LocalDate.of(2026, 3, 14)
    private val end = LocalDate.of(2026, 3, 20)
    private val ts = System.currentTimeMillis()

    private val planA = NutritionPlan(
        id = 1,
        kcalTarget = 2000,
        proteinTargetG = 150.0,
        carbsTargetG = 200.0,
        fatTargetG = 65.0,
        effectiveFrom = ts,
    )

    private val planB = NutritionPlan(
        id = 2,
        kcalTarget = 2500,
        proteinTargetG = 180.0,
        carbsTargetG = 250.0,
        fatTargetG = 80.0,
        effectiveFrom = ts,
    )

    private fun entry(kcal: Double, protein: Double, carbs: Double, fat: Double) = LogEntry(
        foodName = "Test",
        kcal = kcal,
        proteinG = protein,
        carbsG = carbs,
        fatG = fat,
        timestamp = ts,
    )

    @Test
    fun `computes correct total intake from entries`() {
        val entries = listOf(
            entry(500.0, 30.0, 60.0, 20.0),
            entry(300.0, 20.0, 40.0, 10.0),
            entry(200.0, 10.0, 20.0, 5.0),
        )
        val result = useCase(entries, emptyMap(), start, end)
        assertEquals(1000.0, result.totalIntake.kcal, 0.001)
        assertEquals(60.0, result.totalIntake.proteinG, 0.001)
        assertEquals(120.0, result.totalIntake.carbsG, 0.001)
        assertEquals(35.0, result.totalIntake.fatG, 0.001)
    }

    @Test
    fun `computes correct daily average`() {
        val entries = listOf(
            entry(500.0, 30.0, 60.0, 20.0),
            entry(300.0, 20.0, 40.0, 10.0),
            entry(200.0, 10.0, 20.0, 5.0),
        )
        val result = useCase(entries, emptyMap(), start, end)
        // total / 7 days
        assertEquals(1000.0 / 7, result.dailyAverage.kcal, 0.001)
        assertEquals(60.0 / 7, result.dailyAverage.proteinG, 0.001)
        assertEquals(120.0 / 7, result.dailyAverage.carbsG, 0.001)
        assertEquals(35.0 / 7, result.dailyAverage.fatG, 0.001)
    }

    @Test
    fun `returns null target when dailyPlans has no non-null values`() {
        val plans = mapOf<LocalDate, NutritionPlan?>(
            start to null,
            start.plusDays(1) to null,
        )
        val result = useCase(emptyList(), plans, start, end)
        assertNull(result.totalTarget)
    }

    @Test
    fun `sums per-day targets when plan exists for every day`() {
        val plans = (0L..6L).associate { start.plusDays(it) to planA }
        val result = useCase(emptyList(), plans, start, end)
        assertNotNull(result.totalTarget)
        assertEquals(2000.0 * 7, result.totalTarget!!.kcal, 0.001)
        assertEquals(150.0 * 7, result.totalTarget!!.proteinG, 0.001)
        assertEquals(200.0 * 7, result.totalTarget!!.carbsG, 0.001)
        assertEquals(65.0 * 7, result.totalTarget!!.fatG, 0.001)
    }

    @Test
    fun `handles plan changes mid-period`() {
        val plans = buildMap<LocalDate, NutritionPlan?> {
            // Days 1-3: planA
            for (i in 0L..2L) put(start.plusDays(i), planA)
            // Days 4-7: planB
            for (i in 3L..6L) put(start.plusDays(i), planB)
        }
        val result = useCase(emptyList(), plans, start, end)
        assertNotNull(result.totalTarget)
        assertEquals(2000.0 * 3 + 2500.0 * 4, result.totalTarget!!.kcal, 0.001)
        assertEquals(150.0 * 3 + 180.0 * 4, result.totalTarget!!.proteinG, 0.001)
        assertEquals(200.0 * 3 + 250.0 * 4, result.totalTarget!!.carbsG, 0.001)
        assertEquals(65.0 * 3 + 80.0 * 4, result.totalTarget!!.fatG, 0.001)
    }

    @Test
    fun `returns zero intake when no entries`() {
        val result = useCase(emptyList(), emptyMap(), start, end)
        assertEquals(0.0, result.totalIntake.kcal, 0.001)
        assertEquals(0.0, result.totalIntake.proteinG, 0.001)
        assertEquals(0.0, result.totalIntake.carbsG, 0.001)
        assertEquals(0.0, result.totalIntake.fatG, 0.001)
    }

    @Test
    fun `periodDays is inclusive of start and end`() {
        val result = useCase(emptyList(), emptyMap(), start, end)
        assertEquals(7, result.periodDays)
    }

    @Test
    fun `startDate and endDate are preserved in result`() {
        val result = useCase(emptyList(), emptyMap(), start, end)
        assertEquals(start, result.startDate)
        assertEquals(end, result.endDate)
    }
}
