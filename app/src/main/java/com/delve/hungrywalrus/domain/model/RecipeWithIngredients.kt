package com.delve.hungrywalrus.domain.model

/**
 * Domain model combining a recipe with its ingredients.
 * Separate from the Room @Relation class in data.local.entity.
 */
data class RecipeWithIngredients(
    val recipe: Recipe,
    val ingredients: List<RecipeIngredient>,
)
