package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import com.delve.hungrywalrus.domain.model.NutritionPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class NutritionPlanRepositoryImpl @Inject constructor(
    private val dao: NutritionPlanDao,
) : NutritionPlanRepository {

    /**
     * Observes the most recent nutrition plan whose `effectiveFrom` is not in the future.
     *
     * The "now" snapshot is captured at flow construction time (i.e. on every collection)
     * and passed to the DAO. This honours the DAO contract documented in
     * [NutritionPlanDao.getCurrentPlan] -- the latest non-future plan -- and means that if
     * a future feature is added that schedules a plan for a future `effectiveFrom`, the
     * filter will continue to behave correctly.
     *
     * Note: Room re-runs the underlying query on every `nutrition_plan` table change, so a
     * newly saved plan is reflected immediately. The query parameter is not re-evaluated
     * on a timer; the assumption is that any plan whose `effectiveFrom` was in the future
     * at observation time will become observable either when the table changes (re-collection
     * by a downstream observer) or when the screen is re-entered. This is sufficient for v1
     * because [savePlan] always uses `System.currentTimeMillis()` for `effectiveFrom`.
     */
    override fun getCurrentPlan(): Flow<NutritionPlan?> {
        return dao.getCurrentPlan(System.currentTimeMillis()).map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * Returns the plan that was active at the start of the supplied [date].
     *
     * Architecture §17.7 calls for "the plan that was effective on each day within the
     * period" when computing rolling summaries. This implementation converts [date] to the
     * **start-of-day** epoch millis in the device's local zone and asks the DAO for the
     * plan with the latest `effectiveFrom <= start-of-day`.
     *
     * **Behavioural note:** because [savePlan] writes `effectiveFrom = System.currentTimeMillis()`
     * (wall-clock at save time), a plan saved mid-day will have
     * `effectiveFrom > start-of-day(today)` and will therefore **not** be returned by
     * `getPlanForDate(today)`. The plan in effect at the start of today (i.e. the previous
     * plan, or null) is returned instead. This gives each calendar day a single,
     * deterministic plan attribution and avoids the ambiguity of a plan that was active
     * for only part of a day. The rolling summary (§7.6) sums per-day targets across the
     * window, which is the only consumer of this method, so the start-of-day attribution
     * is internally consistent across the data layer and the ViewModel layer.
     */
    override suspend fun getPlanForDate(date: LocalDate): NutritionPlan? {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
