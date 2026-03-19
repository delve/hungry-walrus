package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionPlanDao {

    @Query("SELECT * FROM nutrition_plan WHERE effectiveFrom <= :now ORDER BY effectiveFrom DESC LIMIT 1")
    fun getCurrentPlan(now: Long): Flow<NutritionPlanEntity?>

    @Query("SELECT * FROM nutrition_plan WHERE effectiveFrom <= :date ORDER BY effectiveFrom DESC LIMIT 1")
    suspend fun getPlanForDate(date: Long): NutritionPlanEntity?

    @Insert
    suspend fun insert(plan: NutritionPlanEntity)
}
