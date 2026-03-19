package com.delve.hungrywalrus.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.delve.hungrywalrus.`data`.local.entity.RecipeIngredientEntity
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

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

  public override fun getByRecipeId(recipeId: Long): Flow<List<RecipeIngredientEntity>> {
    val _sql: String = "SELECT * FROM recipe_ingredient WHERE recipeId = ?"
    return createFlow(__db, false, arrayOf("recipe_ingredient")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, recipeId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfRecipeId: Int = getColumnIndexOrThrow(_stmt, "recipeId")
        val _columnIndexOfFoodName: Int = getColumnIndexOrThrow(_stmt, "foodName")
        val _columnIndexOfWeightG: Int = getColumnIndexOrThrow(_stmt, "weightG")
        val _columnIndexOfKcalPer100g: Int = getColumnIndexOrThrow(_stmt, "kcalPer100g")
        val _columnIndexOfProteinPer100g: Int = getColumnIndexOrThrow(_stmt, "proteinPer100g")
        val _columnIndexOfCarbsPer100g: Int = getColumnIndexOrThrow(_stmt, "carbsPer100g")
        val _columnIndexOfFatPer100g: Int = getColumnIndexOrThrow(_stmt, "fatPer100g")
        val _result: MutableList<RecipeIngredientEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecipeIngredientEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpRecipeId: Long
          _tmpRecipeId = _stmt.getLong(_columnIndexOfRecipeId)
          val _tmpFoodName: String
          _tmpFoodName = _stmt.getText(_columnIndexOfFoodName)
          val _tmpWeightG: Double
          _tmpWeightG = _stmt.getDouble(_columnIndexOfWeightG)
          val _tmpKcalPer100g: Double
          _tmpKcalPer100g = _stmt.getDouble(_columnIndexOfKcalPer100g)
          val _tmpProteinPer100g: Double
          _tmpProteinPer100g = _stmt.getDouble(_columnIndexOfProteinPer100g)
          val _tmpCarbsPer100g: Double
          _tmpCarbsPer100g = _stmt.getDouble(_columnIndexOfCarbsPer100g)
          val _tmpFatPer100g: Double
          _tmpFatPer100g = _stmt.getDouble(_columnIndexOfFatPer100g)
          _item =
              RecipeIngredientEntity(_tmpId,_tmpRecipeId,_tmpFoodName,_tmpWeightG,_tmpKcalPer100g,_tmpProteinPer100g,_tmpCarbsPer100g,_tmpFatPer100g)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
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
