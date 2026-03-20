package com.delve.hungrywalrus.domain.model

/**
 * Represents a single ingredient within a recipe.
 *
 * Pre-condition: all per-100g nutrition fields ([kcalPer100g], [proteinPer100g],
 * [carbsPer100g], [fatPer100g]) must be populated before constructing this object.
 * A [com.delve.hungrywalrus.domain.model.FoodSearchResult] with missing nutrition fields
 * must have those values estimated or supplied by the caller before it can be used to
 * create a RecipeIngredient.
 */
data class RecipeIngredient(
    val id: Long = 0,
    val recipeId: Long,
    val foodName: String,
    val weightG: Double,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
)
