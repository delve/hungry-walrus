package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeIngredientDao {

    @Query("SELECT * FROM recipe_ingredient WHERE recipeId = :recipeId")
    fun getByRecipeId(recipeId: Long): Flow<List<RecipeIngredientEntity>>

    @Insert
    suspend fun insertAll(ingredients: List<RecipeIngredientEntity>)

    @Query("DELETE FROM recipe_ingredient WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)
}
