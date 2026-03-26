package com.delve.hungrywalrus.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.delve.hungrywalrus.`data`.local.entity.RecipeIngredientEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RecipeIngredientDao_Impl(
  __db: RoomDatabase,
) : RecipeIngredientDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRecipeIngredientEntity: EntityInsertAdapter<RecipeIngredientEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRecipeIngredientEntity = object :
        EntityInsertAdapter<RecipeIngredientEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `recipe_ingredient` (`id`,`recipeId`,`foodName`,`weightG`,`kcalPer100g`,`proteinPer100g`,`carbsPer100g`,`fatPer100g`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RecipeIngredientEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.recipeId)
        statement.bindText(3, entity.foodName)
        statement.bindDouble(4, entity.weightG)
        statement.bindDouble(5, entity.kcalPer100g)
        statement.bindDouble(6, entity.proteinPer100g)
        statement.bindDouble(7, entity.carbsPer100g)
        statement.bindDouble(8, entity.fatPer100g)
      }
    }
  }

  public override suspend fun insertAll(ingredients: List<RecipeIngredientEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRecipeIngredientEntity.insert(_connection, ingredients)
  }

  public override suspend fun deleteByRecipeId(recipeId: Long) {
    val _sql: String = "DELETE FROM recipe_ingredient WHERE recipeId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, recipeId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
