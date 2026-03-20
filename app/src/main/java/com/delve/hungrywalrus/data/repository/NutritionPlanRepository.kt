package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.NutritionPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface NutritionPlanRepository {
    /**
     * Returns a Flow emitting the active nutrition plan, or null if none exists.
     *
     * **Snapshot limitation**: the "current time" used to evaluate which plan is active is captured
     * once when the Flow is first collected. Room re-executes the query on database changes, but the
     * timestamp predicate does not update. If the app session spans midnight, a plan whose
     * [effectiveFrom][com.delve.hungrywalrus.domain.model.NutritionPlan.effectiveFrom] falls after
     * the collection instant will not appear until the Flow is re-collected (e.g. on navigation).
     */
    fun getCurrentPlan(): Flow<NutritionPlan?>
    suspend fun getPlanForDate(date: LocalDate): NutritionPlan?
    suspend fun savePlan(kcal: Int, proteinG: Double, carbsG: Double, fatG: Double)
}
