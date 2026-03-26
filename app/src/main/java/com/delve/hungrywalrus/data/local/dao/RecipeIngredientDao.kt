package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity

@Dao
interface RecipeIngredientDao {

    @Insert
    suspend fun insertAll(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredient WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)
}
