package com.delve.hungrywalrus.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.delve.hungrywalrus.`data`.local.entity.LogEntryEntity
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
public class LogEntryDao_Impl(
  __db: RoomDatabase,
) : LogEntryDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfLogEntryEntity: EntityInsertAdapter<LogEntryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfLogEntryEntity = object : EntityInsertAdapter<LogEntryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `log_entry` (`id`,`foodName`,`kcal`,`proteinG`,`carbsG`,`fatG`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: LogEntryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.foodName)
        statement.bindDouble(3, entity.kcal)
        statement.bindDouble(4, entity.proteinG)
        statement.bindDouble(5, entity.carbsG)
        statement.bindDouble(6, entity.fatG)
        statement.bindLong(7, entity.timestamp)
      }
    }
  }

  public override suspend fun insert(entry: LogEntryEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfLogEntryEntity.insert(_connection, entry)
  }

  public override fun getEntriesForDate(startOfDay: Long, endOfDay: Long):
      Flow<List<LogEntryEntity>> {
    val _sql: String =
        "SELECT * FROM log_entry WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("log_entry")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startOfDay)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endOfDay)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFoodName: Int = getColumnIndexOrThrow(_stmt, "foodName")
        val _columnIndexOfKcal: Int = getColumnIndexOrThrow(_stmt, "kcal")
        val _columnIndexOfProteinG: Int = getColumnIndexOrThrow(_stmt, "proteinG")
        val _columnIndexOfCarbsG: Int = getColumnIndexOrThrow(_stmt, "carbsG")
        val _columnIndexOfFatG: Int = getColumnIndexOrThrow(_stmt, "fatG")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<LogEntryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LogEntryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFoodName: String
          _tmpFoodName = _stmt.getText(_columnIndexOfFoodName)
          val _tmpKcal: Double
          _tmpKcal = _stmt.getDouble(_columnIndexOfKcal)
          val _tmpProteinG: Double
          _tmpProteinG = _stmt.getDouble(_columnIndexOfProteinG)
          val _tmpCarbsG: Double
          _tmpCarbsG = _stmt.getDouble(_columnIndexOfCarbsG)
          val _tmpFatG: Double
          _tmpFatG = _stmt.getDouble(_columnIndexOfFatG)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              LogEntryEntity(_tmpId,_tmpFoodName,_tmpKcal,_tmpProteinG,_tmpCarbsG,_tmpFatG,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getEntriesForRange(start: Long, end: Long): Flow<List<LogEntryEntity>> {
    val _sql: String =
        "SELECT * FROM log_entry WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp DESC"
    return createFlow(__db, false, arrayOf("log_entry")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, start)
        _argIndex = 2
        _stmt.bindLong(_argIndex, end)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFoodName: Int = getColumnIndexOrThrow(_stmt, "foodName")
        val _columnIndexOfKcal: Int = getColumnIndexOrThrow(_stmt, "kcal")
        val _columnIndexOfProteinG: Int = getColumnIndexOrThrow(_stmt, "proteinG")
        val _columnIndexOfCarbsG: Int = getColumnIndexOrThrow(_stmt, "carbsG")
        val _columnIndexOfFatG: Int = getColumnIndexOrThrow(_stmt, "fatG")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<LogEntryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: LogEntryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpFoodName: String
          _tmpFoodName = _stmt.getText(_columnIndexOfFoodName)
          val _tmpKcal: Double
          _tmpKcal = _stmt.getDouble(_columnIndexOfKcal)
          val _tmpProteinG: Double
          _tmpProteinG = _stmt.getDouble(_columnIndexOfProteinG)
          val _tmpCarbsG: Double
          _tmpCarbsG = _stmt.getDouble(_columnIndexOfCarbsG)
          val _tmpFatG: Double
          _tmpFatG = _stmt.getDouble(_columnIndexOfFatG)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item =
              LogEntryEntity(_tmpId,_tmpFoodName,_tmpKcal,_tmpProteinG,_tmpCarbsG,_tmpFatG,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM log_entry WHERE id = ?"
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

  public override suspend fun deleteOlderThan(threshold: Long) {
    val _sql: String = "DELETE FROM log_entry WHERE timestamp < ?"
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
