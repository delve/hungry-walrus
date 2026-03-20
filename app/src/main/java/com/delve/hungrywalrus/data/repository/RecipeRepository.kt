package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeIngredient
import com.delve.hungrywalrus.domain.model.RecipeWithIngredients
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun getAllRecipes(): Flow<List<Recipe>>
    fun getRecipeWithIngredients(id: Long): Flow<RecipeWithIngredients?>
    suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>): Long
    suspend fun updateRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>)
    suspend fun deleteRecipe(id: Long)
}
