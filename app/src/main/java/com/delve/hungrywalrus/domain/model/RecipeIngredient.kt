package com.delve.hungrywalrus.domain.model

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
