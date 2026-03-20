package com.delve.hungrywalrus.domain.model

data class Recipe(
    val id: Long = 0,
    val name: String,
    val totalWeightG: Double,
    val totalKcal: Double,
    val totalProteinG: Double,
    val totalCarbsG: Double,
    val totalFatG: Double,
    val createdAt: Long,
    val updatedAt: Long,
)
