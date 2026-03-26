package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.NutritionPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface NutritionPlanRepository {
    /**
     * Returns a Flow emitting the currently active nutrition plan, or null if none has been saved.
     *
     * The implementation queries for the most recent plan whose [effectiveFrom][com.delve.hungrywalrus.domain.model.NutritionPlan.effectiveFrom]
     * is at or before [Long.MAX_VALUE], which matches every plan ever inserted. Plans are always
     * saved with `effectiveFrom = System.currentTimeMillis()`, so no future-dated plan can exist.
     * Room re-executes the underlying query on every table change, so any update to the plan table
     * is reflected in the Flow immediately without re-collection.
     */
    fun getCurrentPlan(): Flow<NutritionPlan?>
    suspend fun getPlanForDate(date: LocalDate): NutritionPlan?
    suspend fun savePlan(kcal: Int, proteinG: Double, carbsG: Double, fatG: Double)
}
