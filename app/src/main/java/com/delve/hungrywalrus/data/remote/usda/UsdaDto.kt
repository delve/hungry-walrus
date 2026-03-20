package com.delve.hungrywalrus.data.remote.usda

import kotlinx.serialization.Serializable

@Serializable
data class UsdaSearchResponse(
    val foods: List<UsdaFood> = emptyList(),
)

@Serializable
data class UsdaFood(
    val fdcId: Long,
    val description: String,
    val foodNutrients: List<UsdaNutrient> = emptyList(),
)

@Serializable
data class UsdaNutrient(
    val nutrientId: Int,
    val value: Double? = null,
)
