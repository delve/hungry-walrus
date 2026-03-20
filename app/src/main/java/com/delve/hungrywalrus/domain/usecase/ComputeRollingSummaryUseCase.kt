package com.delve.hungrywalrus.domain.usecase

import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.NutritionValues
import com.delve.hungrywalrus.domain.model.RollingSummary
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Computes a [RollingSummary] for a date range given a list of log entries and per-day plan targets.
 *
 * The caller (ViewModel) is responsible for fetching:
 * - [entries]: all [LogEntry] records whose timestamps fall within [start]..[end].
 * - [dailyPlans]: a map from each date in the period to the [NutritionPlan] that was active on
 *   that day, or null if no plan was configured for that day.
 *
 * This use case is a pure function with no I/O or Android dependencies.
 */
class ComputeRollingSummaryUseCase @Inject constructor() {

    operator fun invoke(
        entries: List<LogEntry>,
        dailyPlans: Map<LocalDate, NutritionPlan?>,
        start: LocalDate,
        end: LocalDate,
    ): RollingSummary {
        val periodDays = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

        // Sum intake
        val totalKcal = entries.sumOf { it.kcal }
        val totalProtein = entries.sumOf { it.proteinG }
        val totalCarbs = entries.sumOf { it.carbsG }
        val totalFat = entries.sumOf { it.fatG }
        val totalIntake = NutritionValues(
            kcal = totalKcal,
            proteinG = totalProtein,
            carbsG = totalCarbs,
            fatG = totalFat,
        )

        // Sum targets day by day
        var hasAnyPlan = false
        var targetKcal = 0.0
        var targetProtein = 0.0
        var targetCarbs = 0.0
        var targetFat = 0.0

        var date = start
        while (!date.isAfter(end)) {
            val plan = dailyPlans[date]
            if (plan != null) {
                hasAnyPlan = true
                targetKcal += plan.kcalTarget
                targetProtein += plan.proteinTargetG
                targetCarbs += plan.carbsTargetG
                targetFat += plan.fatTargetG
            }
            date = date.plusDays(1)
        }

        val totalTarget = if (hasAnyPlan) {
            NutritionValues(
                kcal = targetKcal,
                proteinG = targetProtein,
                carbsG = targetCarbs,
                fatG = targetFat,
            )
        } else null

        val dailyAverage = NutritionValues(
            kcal = totalKcal / periodDays,
            proteinG = totalProtein / periodDays,
            carbsG = totalCarbs / periodDays,
            fatG = totalFat / periodDays,
        )

        return RollingSummary(
            periodDays = periodDays,
            startDate = start,
            endDate = end,
            totalIntake = totalIntake,
            totalTarget = totalTarget,
            dailyAverage = dailyAverage,
        )
    }
}
