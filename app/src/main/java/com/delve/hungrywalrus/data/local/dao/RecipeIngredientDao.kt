package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeIngredientDao {

    /**
     * Observes the ingredients belonging to a given recipe.
     *
     * The primary path for fetching ingredients in the app is via
     * [RecipeDao.getById], which returns the bundled
     * [com.delve.hungrywalrus.data.local.entity.RecipeWithIngredients] @Relation.
     * This method exists as the direct accessor required by the architecture
     * (Section 5.3) for callers that only need the ingredient list (e.g. validation
     * paths, debugging, future features that don't need the parent recipe metadata).
     */
    @Query("SELECT * FROM recipe_ingredient WHERE recipeId = :recipeId")
    fun getByRecipeId(recipeId: Long): Flow<List<RecipeIngredientEntity>>

    @Insert
    suspend fun insertAll(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredient WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)
}
