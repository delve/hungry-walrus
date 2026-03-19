package com.delve.hungrywalrus.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room @Relation data class that bundles a Recipe with its ingredients.
 */
data class RecipeWithIngredients(
    @Embedded
    val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipeId",
    )
    val ingredients: List<RecipeIngredientEntity>,
)
