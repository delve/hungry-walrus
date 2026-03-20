package com.delve.hungrywalrus.domain.model

import java.time.LocalDate

/**
 * Computed summary for a rolling period (7-day or 28-day).
 *
 * [totalTarget] is null when no nutrition plan was configured for any day in the period.
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
