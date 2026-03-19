package com.delve.hungrywalrus.domain.model

import java.time.LocalDate

/**
 * Computed summary for a rolling period (7-day or 28-day).
 *
 * [totalTarget] is null when one or more days in the period have no active nutrition plan.
 * It is non-null only when every day in the period has a plan entry.
 * [dailyAverage] is the period total intake divided by [periodDays].
 */
data class RollingSummary(
    val periodDays: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalIntake: NutritionValues,
    val totalTarget: NutritionValues?,
    val dailyAverage: NutritionValues,
)
