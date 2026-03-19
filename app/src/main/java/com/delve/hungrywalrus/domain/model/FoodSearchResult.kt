package com.delve.hungrywalrus.domain.model

/**
 * Domain model for a food item from an API search or manual entry.
 * Nutritional values are per 100g. Nullable fields indicate missing API data.
 * The [missingFields] set is derived: any field that is null is included.
 */
data class FoodSearchResult(
    val id: String,
    val name: String,
    val source: FoodSource,
    val kcalPer100g: Double?,
    val proteinPer100g: Double?,
    val carbsPer100g: Double?,
    val fatPer100g: Double?,
    val missingFields: Set<NutritionField>,
)
