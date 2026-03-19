package com.delve.hungrywalrus.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.delve.hungrywalrus.`data`.local.dao.FoodCacheDao
import com.delve.hungrywalrus.`data`.local.dao.FoodCacheDao_Impl
import com.delve.hungrywalrus.`data`.local.dao.LogEntryDao
import com.delve.hungrywalrus.`data`.local.dao.LogEntryDao_Impl
import com.delve.hungrywalrus.`data`.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.`data`.local.dao.NutritionPlanDao_Impl
import com.delve.hungrywalrus.`data`.local.dao.RecipeDao
import com.delve.hungrywalrus.`data`.local.dao.RecipeDao_Impl
import com.delve.hungrywalrus.`data`.local.dao.RecipeIngredientDao
import com.delve.hungrywalrus.`data`.local.dao.RecipeIngredientDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class HungryWalrusDatabase_Impl : HungryWalrusDatabase() {
  private val _nutritionPlanDao: Lazy<NutritionPlanDao> = lazy {
    NutritionPlanDao_Impl(this)
  }

  private val _logEntryDao: Lazy<LogEntryDao> = lazy {
    LogEntryDao_Impl(this)
  }

  private val _recipeDao: Lazy<RecipeDao> = lazy {
    RecipeDao_Impl(this)
  }

  private val _recipeIngredientDao: Lazy<RecipeIngredientDao> = lazy {
    RecipeIngredientDao_Impl(this)
  }

  private val _foodCacheDao: Lazy<FoodCacheDao> = lazy {
    FoodCacheDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "fcdfa0b4490df7974d0e4160893c394f", "e958f0a9fc2bbe145316059fbc68779e") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `nutrition_plan` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `kcalTarget` INTEGER NOT NULL, `proteinTargetG` REAL NOT NULL, `carbsTargetG` REAL NOT NULL, `fatTargetG` REAL NOT NULL, `effectiveFrom` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `log_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `foodName` TEXT NOT NULL, `kcal` REAL NOT NULL, `proteinG` REAL NOT NULL, `carbsG` REAL NOT NULL, `fatG` REAL NOT NULL, `timestamp` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recipe` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `totalWeightG` REAL NOT NULL, `totalKcal` REAL NOT NULL, `totalProteinG` REAL NOT NULL, `totalCarbsG` REAL NOT NULL, `totalFatG` REAL NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recipe_ingredient` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recipeId` INTEGER NOT NULL, `foodName` TEXT NOT NULL, `weightG` REAL NOT NULL, `kcalPer100g` REAL NOT NULL, `proteinPer100g` REAL NOT NULL, `carbsPer100g` REAL NOT NULL, `fatPer100g` REAL NOT NULL, FOREIGN KEY(`recipeId`) REFERENCES `recipe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_recipe_ingredient_recipeId` ON `recipe_ingredient` (`recipeId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `food_cache` (`cacheKey` TEXT NOT NULL, `foodName` TEXT NOT NULL, `kcalPer100g` REAL, `proteinPer100g` REAL, `carbsPer100g` REAL, `fatPer100g` REAL, `source` TEXT NOT NULL, `barcode` TEXT, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`cacheKey`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'fcdfa0b4490df7974d0e4160893c394f')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `nutrition_plan`")
        connection.execSQL("DROP TABLE IF EXISTS `log_entry`")
        connection.execSQL("DROP TABLE IF EXISTS `recipe`")
        connection.execSQL("DROP TABLE IF EXISTS `recipe_ingredient`")
        connection.execSQL("DROP TABLE IF EXISTS `food_cache`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsNutritionPlan: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNutritionPlan.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionPlan.put("kcalTarget", TableInfo.Column("kcalTarget", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionPlan.put("proteinTargetG", TableInfo.Column("proteinTargetG", "REAL", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionPlan.put("carbsTargetG", TableInfo.Column("carbsTargetG", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionPlan.put("fatTargetG", TableInfo.Column("fatTargetG", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionPlan.put("effectiveFrom", TableInfo.Column("effectiveFrom", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNutritionPlan: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNutritionPlan: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNutritionPlan: TableInfo = TableInfo("nutrition_plan", _columnsNutritionPlan,
            _foreignKeysNutritionPlan, _indicesNutritionPlan)
        val _existingNutritionPlan: TableInfo = read(connection, "nutrition_plan")
        if (!_infoNutritionPlan.equals(_existingNutritionPlan)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |nutrition_plan(com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity).
              | Expected:
              |""".trimMargin() + _infoNutritionPlan + """
              |
              | Found:
              |""".trimMargin() + _existingNutritionPlan)
        }
        val _columnsLogEntry: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsLogEntry.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLogEntry.put("foodName", TableInfo.Column("foodName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLogEntry.put("kcal", TableInfo.Column("kcal", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLogEntry.put("proteinG", TableInfo.Column("proteinG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLogEntry.put("carbsG", TableInfo.Column("carbsG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLogEntry.put("fatG", TableInfo.Column("fatG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsLogEntry.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysLogEntry: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesLogEntry: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoLogEntry: TableInfo = TableInfo("log_entry", _columnsLogEntry,
            _foreignKeysLogEntry, _indicesLogEntry)
        val _existingLogEntry: TableInfo = read(connection, "log_entry")
        if (!_infoLogEntry.equals(_existingLogEntry)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |log_entry(com.delve.hungrywalrus.data.local.entity.LogEntryEntity).
              | Expected:
              |""".trimMargin() + _infoLogEntry + """
              |
              | Found:
              |""".trimMargin() + _existingLogEntry)
        }
        val _columnsRecipe: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecipe.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("totalWeightG", TableInfo.Column("totalWeightG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("totalKcal", TableInfo.Column("totalKcal", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("totalProteinG", TableInfo.Column("totalProteinG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("totalCarbsG", TableInfo.Column("totalCarbsG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("totalFatG", TableInfo.Column("totalFatG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipe.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecipe: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRecipe: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRecipe: TableInfo = TableInfo("recipe", _columnsRecipe, _foreignKeysRecipe,
            _indicesRecipe)
        val _existingRecipe: TableInfo = read(connection, "recipe")
        if (!_infoRecipe.equals(_existingRecipe)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recipe(com.delve.hungrywalrus.data.local.entity.RecipeEntity).
              | Expected:
              |""".trimMargin() + _infoRecipe + """
              |
              | Found:
              |""".trimMargin() + _existingRecipe)
        }
        val _columnsRecipeIngredient: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecipeIngredient.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("recipeId", TableInfo.Column("recipeId", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("foodName", TableInfo.Column("foodName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("weightG", TableInfo.Column("weightG", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("kcalPer100g", TableInfo.Column("kcalPer100g", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("proteinPer100g", TableInfo.Column("proteinPer100g", "REAL",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("carbsPer100g", TableInfo.Column("carbsPer100g", "REAL", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipeIngredient.put("fatPer100g", TableInfo.Column("fatPer100g", "REAL", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecipeIngredient: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysRecipeIngredient.add(TableInfo.ForeignKey("recipe", "CASCADE", "NO ACTION",
            listOf("recipeId"), listOf("id")))
        val _indicesRecipeIngredient: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesRecipeIngredient.add(TableInfo.Index("index_recipe_ingredient_recipeId", false,
            listOf("recipeId"), listOf("ASC")))
        val _infoRecipeIngredient: TableInfo = TableInfo("recipe_ingredient",
            _columnsRecipeIngredient, _foreignKeysRecipeIngredient, _indicesRecipeIngredient)
        val _existingRecipeIngredient: TableInfo = read(connection, "recipe_ingredient")
        if (!_infoRecipeIngredient.equals(_existingRecipeIngredient)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recipe_ingredient(com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity).
              | Expected:
              |""".trimMargin() + _infoRecipeIngredient + """
              |
              | Found:
              |""".trimMargin() + _existingRecipeIngredient)
        }
        val _columnsFoodCache: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFoodCache.put("cacheKey", TableInfo.Column("cacheKey", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("foodName", TableInfo.Column("foodName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("kcalPer100g", TableInfo.Column("kcalPer100g", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("proteinPer100g", TableInfo.Column("proteinPer100g", "REAL", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("carbsPer100g", TableInfo.Column("carbsPer100g", "REAL", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("fatPer100g", TableInfo.Column("fatPer100g", "REAL", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("source", TableInfo.Column("source", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("barcode", TableInfo.Column("barcode", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFoodCache.put("cachedAt", TableInfo.Column("cachedAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFoodCache: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFoodCache: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFoodCache: TableInfo = TableInfo("food_cache", _columnsFoodCache,
            _foreignKeysFoodCache, _indicesFoodCache)
        val _existingFoodCache: TableInfo = read(connection, "food_cache")
        if (!_infoFoodCache.equals(_existingFoodCache)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |food_cache(com.delve.hungrywalrus.data.local.entity.FoodCacheEntity).
              | Expected:
              |""".trimMargin() + _infoFoodCache + """
              |
              | Found:
              |""".trimMargin() + _existingFoodCache)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "nutrition_plan", "log_entry",
        "recipe", "recipe_ingredient", "food_cache")
  }

  public override fun clearAllTables() {
    super.performClear(true, "nutrition_plan", "log_entry", "recipe", "recipe_ingredient",
        "food_cache")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(NutritionPlanDao::class, NutritionPlanDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(LogEntryDao::class, LogEntryDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RecipeDao::class, RecipeDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(RecipeIngredientDao::class,
        RecipeIngredientDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(FoodCacheDao::class, FoodCacheDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun nutritionPlanDao(): NutritionPlanDao = _nutritionPlanDao.value

  public override fun logEntryDao(): LogEntryDao = _logEntryDao.value

  public override fun recipeDao(): RecipeDao = _recipeDao.value

  public override fun recipeIngredientDao(): RecipeIngredientDao = _recipeIngredientDao.value

  public override fun foodCacheDao(): FoodCacheDao = _foodCacheDao.value
}
