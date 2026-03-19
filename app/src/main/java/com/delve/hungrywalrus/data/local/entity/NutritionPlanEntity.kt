package com.delve.hungrywalrus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_plan")
data class NutritionPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kcalTarget: Int,
    val proteinTargetG: Double,
    val carbsTargetG: Double,
    val fatTargetG: Double,
    val effectiveFrom: Long,
)
