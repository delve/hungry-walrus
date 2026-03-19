package com.delve.hungrywalrus.`data`.local.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.delve.hungrywalrus.`data`.local.entity.NutritionPlanEntity
import javax.`annotation`.processing.Generated
import kotlin.Double
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class NutritionPlanDao_Impl(
  __db: RoomDatabase,
) : NutritionPlanDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfNutritionPlanEntity: EntityInsertAdapter<NutritionPlanEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfNutritionPlanEntity = object : EntityInsertAdapter<NutritionPlanEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `nutrition_plan` (`id`,`kcalTarget`,`proteinTargetG`,`carbsTargetG`,`fatTargetG`,`effectiveFrom`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NutritionPlanEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.kcalTarget.toLong())
        statement.bindDouble(3, entity.proteinTargetG)
        statement.bindDouble(4, entity.carbsTargetG)
        statement.bindDouble(5, entity.fatTargetG)
        statement.bindLong(6, entity.effectiveFrom)
      }
    }
  }

  public override suspend fun insert(plan: NutritionPlanEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __insertAdapterOfNutritionPlanEntity.insert(_connection, plan)
  }

  public override fun getCurrentPlan(now: Long): Flow<NutritionPlanEntity?> {
    val _sql: String =
        "SELECT * FROM nutrition_plan WHERE effectiveFrom <= ? ORDER BY effectiveFrom DESC LIMIT 1"
    return createFlow(__db, false, arrayOf("nutrition_plan")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, now)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfKcalTarget: Int = getColumnIndexOrThrow(_stmt, "kcalTarget")
        val _columnIndexOfProteinTargetG: Int = getColumnIndexOrThrow(_stmt, "proteinTargetG")
        val _columnIndexOfCarbsTargetG: Int = getColumnIndexOrThrow(_stmt, "carbsTargetG")
        val _columnIndexOfFatTargetG: Int = getColumnIndexOrThrow(_stmt, "fatTargetG")
        val _columnIndexOfEffectiveFrom: Int = getColumnIndexOrThrow(_stmt, "effectiveFrom")
        val _result: NutritionPlanEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpKcalTarget: Int
          _tmpKcalTarget = _stmt.getLong(_columnIndexOfKcalTarget).toInt()
          val _tmpProteinTargetG: Double
          _tmpProteinTargetG = _stmt.getDouble(_columnIndexOfProteinTargetG)
          val _tmpCarbsTargetG: Double
          _tmpCarbsTargetG = _stmt.getDouble(_columnIndexOfCarbsTargetG)
          val _tmpFatTargetG: Double
          _tmpFatTargetG = _stmt.getDouble(_columnIndexOfFatTargetG)
          val _tmpEffectiveFrom: Long
          _tmpEffectiveFrom = _stmt.getLong(_columnIndexOfEffectiveFrom)
          _result =
              NutritionPlanEntity(_tmpId,_tmpKcalTarget,_tmpProteinTargetG,_tmpCarbsTargetG,_tmpFatTargetG,_tmpEffectiveFrom)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPlanForDate(date: Long): NutritionPlanEntity? {
    val _sql: String =
        "SELECT * FROM nutrition_plan WHERE effectiveFrom <= ? ORDER BY effectiveFrom DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, date)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfKcalTarget: Int = getColumnIndexOrThrow(_stmt, "kcalTarget")
        val _columnIndexOfProteinTargetG: Int = getColumnIndexOrThrow(_stmt, "proteinTargetG")
        val _columnIndexOfCarbsTargetG: Int = getColumnIndexOrThrow(_stmt, "carbsTargetG")
        val _columnIndexOfFatTargetG: Int = getColumnIndexOrThrow(_stmt, "fatTargetG")
        val _columnIndexOfEffectiveFrom: Int = getColumnIndexOrThrow(_stmt, "effectiveFrom")
        val _result: NutritionPlanEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpKcalTarget: Int
          _tmpKcalTarget = _stmt.getLong(_columnIndexOfKcalTarget).toInt()
          val _tmpProteinTargetG: Double
          _tmpProteinTargetG = _stmt.getDouble(_columnIndexOfProteinTargetG)
          val _tmpCarbsTargetG: Double
          _tmpCarbsTargetG = _stmt.getDouble(_columnIndexOfCarbsTargetG)
          val _tmpFatTargetG: Double
          _tmpFatTargetG = _stmt.getDouble(_columnIndexOfFatTargetG)
          val _tmpEffectiveFrom: Long
          _tmpEffectiveFrom = _stmt.getLong(_columnIndexOfEffectiveFrom)
          _result =
              NutritionPlanEntity(_tmpId,_tmpKcalTarget,_tmpProteinTargetG,_tmpCarbsTargetG,_tmpFatTargetG,_tmpEffectiveFrom)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
