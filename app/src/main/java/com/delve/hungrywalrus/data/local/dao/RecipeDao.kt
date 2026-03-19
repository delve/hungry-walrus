package com.delve.hungrywalrus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.delve.hungrywalrus.data.local.entity.RecipeEntity
import com.delve.hungrywalrus.data.local.entity.RecipeWithIngredients
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipe ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<RecipeEntity>>

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :id")
    fun getById(id: Long): Flow<RecipeWithIngredients?>

    @Insert
    suspend fun insert(recipe: RecipeEntity): Long

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Query("DELETE FROM recipe WHERE id = :id")
    suspend fun deleteById(id: Long)
}
