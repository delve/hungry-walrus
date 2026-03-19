package com.delve.hungrywalrus.`data`.local.dao

import androidx.collection.LongSparseArray
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import com.delve.hungrywalrus.`data`.local.entity.RecipeEntity
import com.delve.hungrywalrus.`data`.local.entity.RecipeIngredientEntity
import com.delve.hungrywalrus.`data`.local.entity.RecipeWithIngredients
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RecipeDao_Impl(
  __db: RoomDatabase,
) : RecipeDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRecipeEntity: EntityInsertAdapter<RecipeEntity>

  private val __updateAdapterOfRecipeEntity: EntityDeleteOrUpdateAdapter<RecipeEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRecipeEntity = object : EntityInsertAdapter<RecipeEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `recipe` (`id`,`name`,`totalWeightG`,`totalKcal`,`totalProteinG`,`totalCarbsG`,`totalFatG`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RecipeEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.totalWeightG)
        statement.bindDouble(4, entity.totalKcal)
        statement.bindDouble(5, entity.totalProteinG)
        statement.bindDouble(6, entity.totalCarbsG)
        statement.bindDouble(7, entity.totalFatG)
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.updatedAt)
      }
    }
    this.__updateAdapterOfRecipeEntity = object : EntityDeleteOrUpdateAdapter<RecipeEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `recipe` SET `id` = ?,`name` = ?,`totalWeightG` = ?,`totalKcal` = ?,`totalProteinG` = ?,`totalCarbsG` = ?,`totalFatG` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: RecipeEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindDouble(3, entity.totalWeightG)
        statement.bindDouble(4, entity.totalKcal)
        statement.bindDouble(5, entity.totalProteinG)
        statement.bindDouble(6, entity.totalCarbsG)
        statement.bindDouble(7, entity.totalFatG)
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.updatedAt)
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun insert(recipe: RecipeEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfRecipeEntity.insertAndReturnId(_connection, recipe)
    _result
  }

  public override suspend fun update(recipe: RecipeEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfRecipeEntity.handle(_connection, recipe)
  }

  public override fun getAll(): Flow<List<RecipeEntity>> {
    val _sql: String = "SELECT * FROM recipe ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("recipe")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfTotalWeightG: Int = getColumnIndexOrThrow(_stmt, "totalWeightG")
        val _columnIndexOfTotalKcal: Int = getColumnIndexOrThrow(_stmt, "totalKcal")
        val _columnIndexOfTotalProteinG: Int = getColumnIndexOrThrow(_stmt, "totalProteinG")
        val _columnIndexOfTotalCarbsG: Int = getColumnIndexOrThrow(_stmt, "totalCarbsG")
        val _columnIndexOfTotalFatG: Int = getColumnIndexOrThrow(_stmt, "totalFatG")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<RecipeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecipeEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpTotalWeightG: Double
          _tmpTotalWeightG = _stmt.getDouble(_columnIndexOfTotalWeightG)
          val _tmpTotalKcal: Double
          _tmpTotalKcal = _stmt.getDouble(_columnIndexOfTotalKcal)
          val _tmpTotalProteinG: Double
          _tmpTotalProteinG = _stmt.getDouble(_columnIndexOfTotalProteinG)
          val _tmpTotalCarbsG: Double
          _tmpTotalCarbsG = _stmt.getDouble(_columnIndexOfTotalCarbsG)
          val _tmpTotalFatG: Double
          _tmpTotalFatG = _stmt.getDouble(_columnIndexOfTotalFatG)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item =
              RecipeEntity(_tmpId,_tmpName,_tmpTotalWeightG,_tmpTotalKcal,_tmpTotalProteinG,_tmpTotalCarbsG,_tmpTotalFatG,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getById(id: Long): Flow<RecipeWithIngredients?> {
    val _sql: String = "SELECT * FROM recipe WHERE id = ?"
    return createFlow(__db, true, arrayOf("recipe_ingredient", "recipe")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfTotalWeightG: Int = getColumnIndexOrThrow(_stmt, "totalWeightG")
        val _columnIndexOfTotalKcal: Int = getColumnIndexOrThrow(_stmt, "totalKcal")
        val _columnIndexOfTotalProteinG: Int = getColumnIndexOrThrow(_stmt, "totalProteinG")
        val _columnIndexOfTotalCarbsG: Int = getColumnIndexOrThrow(_stmt, "totalCarbsG")
        val _columnIndexOfTotalFatG: Int = getColumnIndexOrThrow(_stmt, "totalFatG")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _collectionIngredients: LongSparseArray<MutableList<RecipeIngredientEntity>> =
            LongSparseArray<MutableList<RecipeIngredientEntity>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionIngredients.containsKey(_tmpKey)) {
            _collectionIngredients.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshiprecipeIngredientAscomDelveHungrywalrusDataLocalEntityRecipeIngredientEntity(_connection,
            _collectionIngredients)
        val _result: RecipeWithIngredients?
        if (_stmt.step()) {
          val _tmpRecipe: RecipeEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpTotalWeightG: Double
          _tmpTotalWeightG = _stmt.getDouble(_columnIndexOfTotalWeightG)
          val _tmpTotalKcal: Double
          _tmpTotalKcal = _stmt.getDouble(_columnIndexOfTotalKcal)
          val _tmpTotalProteinG: Double
          _tmpTotalProteinG = _stmt.getDouble(_columnIndexOfTotalProteinG)
          val _tmpTotalCarbsG: Double
          _tmpTotalCarbsG = _stmt.getDouble(_columnIndexOfTotalCarbsG)
          val _tmpTotalFatG: Double
          _tmpTotalFatG = _stmt.getDouble(_columnIndexOfTotalFatG)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _tmpRecipe =
              RecipeEntity(_tmpId,_tmpName,_tmpTotalWeightG,_tmpTotalKcal,_tmpTotalProteinG,_tmpTotalCarbsG,_tmpTotalFatG,_tmpCreatedAt,_tmpUpdatedAt)
          val _tmpIngredientsCollection: MutableList<RecipeIngredientEntity>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpIngredientsCollection = checkNotNull(_collectionIngredients.get(_tmpKey_1))
          _result = RecipeWithIngredients(_tmpRecipe,_tmpIngredientsCollection)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM recipe WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private
      fun __fetchRelationshiprecipeIngredientAscomDelveHungrywalrusDataLocalEntityRecipeIngredientEntity(_connection: SQLiteConnection,
      _map: LongSparseArray<MutableList<RecipeIngredientEntity>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshiprecipeIngredientAscomDelveHungrywalrusDataLocalEntityRecipeIngredientEntity(_connection,
            _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`recipeId`,`foodName`,`weightG`,`kcalPer100g`,`proteinPer100g`,`carbsPer100g`,`fatPer100g` FROM `recipe_ingredient` WHERE `recipeId` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "recipeId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfRecipeId: Int = 1
      val _columnIndexOfFoodName: Int = 2
      val _columnIndexOfWeightG: Int = 3
      val _columnIndexOfKcalPer100g: Int = 4
      val _columnIndexOfProteinPer100g: Int = 5
      val _columnIndexOfCarbsPer100g: Int = 6
      val _columnIndexOfFatPer100g: Int = 7
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<RecipeIngredientEntity>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: RecipeIngredientEntity
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
          _item_1 =
              RecipeIngredientEntity(_tmpId,_tmpRecipeId,_tmpFoodName,_tmpWeightG,_tmpKcalPer100g,_tmpProteinPer100g,_tmpCarbsPer100g,_tmpFatPer100g)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
