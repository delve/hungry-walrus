package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.NutritionPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface NutritionPlanRepository {
    fun getCurrentPlan(): Flow<NutritionPlan?>
    suspend fun getPlanForDate(date: LocalDate): NutritionPlan?
    suspend fun savePlan(kcal: Int, proteinG: Double, carbsG: Double, fatG: Double)
}
