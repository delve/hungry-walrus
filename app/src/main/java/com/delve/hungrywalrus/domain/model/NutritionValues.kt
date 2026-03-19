package com.delve.hungrywalrus.domain.model

/**
 * Represents a set of nutritional values (kcal and macronutrients).
 * Used throughout the app for computed totals and scaled values.
 */
data class NutritionValues(
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)
