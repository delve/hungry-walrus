package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import com.delve.hungrywalrus.domain.model.NutritionPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class NutritionPlanRepositoryImpl @Inject constructor(
    private val dao: NutritionPlanDao,
) : NutritionPlanRepository {

    override fun getCurrentPlan(): Flow<NutritionPlan?> {
        return dao.getCurrentPlan(System.currentTimeMillis()).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getPlanForDate(date: LocalDate): NutritionPlan? {
        val millis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        return dao.getPlanForDate(millis)?.toDomain()
    }

    override suspend fun savePlan(kcal: Int, proteinG: Double, carbsG: Double, fatG: Double) {
        val entity = NutritionPlanEntity(
            kcalTarget = kcal,
            proteinTargetG = proteinG,
            carbsTargetG = carbsG,
            fatTargetG = fatG,
            effectiveFrom = System.currentTimeMillis(),
        )
        dao.insert(entity)
    }

    private fun NutritionPlanEntity.toDomain(): NutritionPlan {
        return NutritionPlan(
            id = id,
            kcalTarget = kcalTarget,
            proteinTargetG = proteinTargetG,
            carbsTargetG = carbsTargetG,
            fatTargetG = fatTargetG,
            effectiveFrom = effectiveFrom,
        )
    }
}
