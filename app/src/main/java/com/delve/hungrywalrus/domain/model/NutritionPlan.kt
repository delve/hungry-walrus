package com.delve.hungrywalrus.domain.model

data class NutritionPlan(
    val id: Long = 0,
    val kcalTarget: Int,
    val proteinTargetG: Double,
    val carbsTargetG: Double,
    val fatTargetG: Double,
    val effectiveFrom: Long,
)
