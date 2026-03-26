package com.delve.hungrywalrus.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.delve.hungrywalrus.`data`.local.entity.FoodCacheEntity
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class FoodCacheDao_Impl(
  __db: RoomDatabase,
) : FoodCacheDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfFoodCacheEntity: EntityInsertAdapter<FoodCacheEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfFoodCacheEntity = object : EntityInsertAdapter<FoodCacheEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `food_cache` (`cacheKey`,`foodName`,`kcalPer100g`,`proteinPer100g`,`carbsPer100g`,`fatPer100g`,`source`,`barcode`,`cachedAt`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FoodCacheEntity) {
        statement.bindText(1, entity.cacheKey)
        statement.bindText(2, entity.foodName)
        val _tmpKcalPer100g: Double? = entity.kcalPer100g
        if (_tmpKcalPer100g == null) {
          statement.bindNull(3)
        } else {
          statement.bindDouble(3, _tmpKcalPer100g)
        }
        val _tmpProteinPer100g: Double? = entity.proteinPer100g
        if (_tmpProteinPer100g == null) {
          statement.bindNull(4)
        } else {
          statement.bindDouble(4, _tmpProteinPer100g)
        }
        val _tmpCarbsPer100g: Double? = entity.carbsPer100g
        if (_tmpCarbsPer100g == null) {
          statement.bindNull(5)
        } else {
          statement.bindDouble(5, _tmpCarbsPer100g)
        }
        val _tmpFatPer100g: Double? = entity.fatPer100g
        if (_tmpFatPer100g == null) {
          statement.bindNull(6)
        } else {
          statement.bindDouble(6, _tmpFatPer100g)
        }
        statement.bindText(7, entity.source)
        val _tmpBarcode: String? = entity.barcode
        if (_tmpBarcode == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpBarcode)
        }
        statement.bindLong(9, entity.cachedAt)
      }
    }
  }

  public override suspend fun insert(entry: FoodCacheEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfFoodCacheEntity.insert(_connection, entry)
  }

  public override suspend fun getByBarcode(barcode: String): FoodCacheEntity? {
    val _sql: String = "SELECT * FROM food_cache WHERE barcode = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, barcode)
        val _columnIndexOfCacheKey: Int = getColumnIndexOrThrow(_stmt, "cacheKey")
        val _columnIndexOfFoodName: Int = getColumnIndexOrThrow(_stmt, "foodName")
        val _columnIndexOfKcalPer100g: Int = getColumnIndexOrThrow(_stmt, "kcalPer100g")
        val _columnIndexOfProteinPer100g: Int = getColumnIndexOrThrow(_stmt, "proteinPer100g")
        val _columnIndexOfCarbsPer100g: Int = getColumnIndexOrThrow(_stmt, "carbsPer100g")
        val _columnIndexOfFatPer100g: Int = getColumnIndexOrThrow(_stmt, "fatPer100g")
        val _columnIndexOfSource: Int = getColumnIndexOrThrow(_stmt, "source")
        val _columnIndexOfBarcode: Int = getColumnIndexOrThrow(_stmt, "barcode")
        val _columnIndexOfCachedAt: Int = getColumnIndexOrThrow(_stmt, "cachedAt")
        val _result: FoodCacheEntity?
        if (_stmt.step()) {
          val _tmpCacheKey: String
          _tmpCacheKey = _stmt.getText(_columnIndexOfCacheKey)
          val _tmpFoodName: String
          _tmpFoodName = _stmt.getText(_columnIndexOfFoodName)
          val _tmpKcalPer100g: Double?
          if (_stmt.isNull(_columnIndexOfKcalPer100g)) {
            _tmpKcalPer100g = null
          } else {
            _tmpKcalPer100g = _stmt.getDouble(_columnIndexOfKcalPer100g)
          }
          val _tmpProteinPer100g: Double?
          if (_stmt.isNull(_columnIndexOfProteinPer100g)) {
            _tmpProteinPer100g = null
          } else {
            _tmpProteinPer100g = _stmt.getDouble(_columnIndexOfProteinPer100g)
          }
          val _tmpCarbsPer100g: Double?
          if (_stmt.isNull(_columnIndexOfCarbsPer100g)) {
            _tmpCarbsPer100g = null
          } else {
            _tmpCarbsPer100g = _stmt.getDouble(_columnIndexOfCarbsPer100g)
          }
          val _tmpFatPer100g: Double?
          if (_stmt.isNull(_columnIndexOfFatPer100g)) {
            _tmpFatPer100g = null
          } else {
            _tmpFatPer100g = _stmt.getDouble(_columnIndexOfFatPer100g)
          }
          val _tmpSource: String
          _tmpSource = _stmt.getText(_columnIndexOfSource)
          val _tmpBarcode: String?
          if (_stmt.isNull(_columnIndexOfBarcode)) {
            _tmpBarcode = null
          } else {
            _tmpBarcode = _stmt.getText(_columnIndexOfBarcode)
          }
          val _tmpCachedAt: Long
          _tmpCachedAt = _stmt.getLong(_columnIndexOfCachedAt)
          _result =
              FoodCacheEntity(_tmpCacheKey,_tmpFoodName,_tmpKcalPer100g,_tmpProteinPer100g,_tmpCarbsPer100g,_tmpFatPer100g,_tmpSource,_tmpBarcode,_tmpCachedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteOlderThan(threshold: Long) {
    val _sql: String = "DELETE FROM food_cache WHERE cachedAt < ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, threshold)
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
