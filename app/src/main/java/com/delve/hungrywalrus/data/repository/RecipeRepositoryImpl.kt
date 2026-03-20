package com.delve.hungrywalrus.data.repository

import com.delve.hungrywalrus.data.local.dao.RecipeDao
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao
import com.delve.hungrywalrus.data.local.entity.RecipeEntity
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeIngredient
import com.delve.hungrywalrus.domain.model.RecipeWithIngredients
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecipeRepositoryImpl @Inject constructor(
    private val recipeDao: RecipeDao,
    private val ingredientDao: RecipeIngredientDao,
) : RecipeRepository {

    override fun getAllRecipes(): Flow<List<Recipe>> {
        return recipeDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecipeWithIngredients(id: Long): Flow<RecipeWithIngredients?> {
        return recipeDao.getById(id).map { relation ->
            relation?.let {
                RecipeWithIngredients(
                    recipe = it.recipe.toDomain(),
                    ingredients = it.ingredients.map { ingredient -> ingredient.toDomain() },
                )
            }
        }
    }

    override suspend fun saveRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>) {
        val recipeId = recipeDao.insert(recipe.toEntity())
        val ingredientEntities = ingredients.map { it.toEntity(recipeId) }
        ingredientDao.insertAll(ingredientEntities)
    }

    override suspend fun updateRecipe(recipe: Recipe, ingredients: List<RecipeIngredient>) {
        recipeDao.update(recipe.toEntity())
        ingredientDao.deleteByRecipeId(recipe.id)
        val ingredientEntities = ingredients.map { it.toEntity(recipe.id) }
        ingredientDao.insertAll(ingredientEntities)
    }

    override suspend fun deleteRecipe(id: Long) {
        recipeDao.deleteById(id)
    }

    private fun RecipeEntity.toDomain(): Recipe {
        return Recipe(
            id = id,
            name = name,
            totalWeightG = totalWeightG,
            totalKcal = totalKcal,
            totalProteinG = totalProteinG,
            totalCarbsG = totalCarbsG,
            totalFatG = totalFatG,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun Recipe.toEntity(): RecipeEntity {
        return RecipeEntity(
            id = id,
            name = name,
            totalWeightG = totalWeightG,
            totalKcal = totalKcal,
            totalProteinG = totalProteinG,
            totalCarbsG = totalCarbsG,
            totalFatG = totalFatG,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun RecipeIngredientEntity.toDomain(): RecipeIngredient {
        return RecipeIngredient(
            id = id,
            recipeId = recipeId,
            foodName = foodName,
            weightG = weightG,
            kcalPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
        )
    }

    private fun RecipeIngredient.toEntity(overrideRecipeId: Long): RecipeIngredientEntity {
        return RecipeIngredientEntity(
            id = 0, // Always insert as new when saving/updating
            recipeId = overrideRecipeId,
            foodName = foodName,
            weightG = weightG,
            kcalPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
        )
    }
}
