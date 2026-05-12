package com.delve.hungrywalrus.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase
import com.delve.hungrywalrus.data.local.entity.RecipeEntity
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * In-memory Room database tests for [RecipeIngredientDao] and its CASCADE delete relation
 * with [RecipeDao].
 *
 * Covers:
 *  - `getByRecipeId` (newly added per architecture Section 5.3)
 *  - `insertAll` batch insert
 *  - `deleteByRecipeId` direct deletion
 *  - CASCADE delete from `RecipeEntity` -> `RecipeIngredientEntity`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeIngredientDaoTest {

    private lateinit var database: HungryWalrusDatabase
    private lateinit var recipeDao: RecipeDao
    private lateinit var ingredientDao: RecipeIngredientDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HungryWalrusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recipeDao = database.recipeDao()
        ingredientDao = database.recipeIngredientDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun newRecipe(name: String = "Test recipe"): RecipeEntity = RecipeEntity(
        name = name,
        totalWeightG = 100.0,
        totalKcal = 200.0,
        totalProteinG = 10.0,
        totalCarbsG = 20.0,
        totalFatG = 5.0,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private fun newIngredient(recipeId: Long, name: String): RecipeIngredientEntity =
        RecipeIngredientEntity(
            recipeId = recipeId,
            foodName = name,
            weightG = 50.0,
            kcalPer100g = 100.0,
            proteinPer100g = 5.0,
            carbsPer100g = 10.0,
            fatPer100g = 3.0,
        )

    @Test
    fun `getByRecipeId returns ingredients belonging to that recipe only`() = runTest {
        val recipeA = recipeDao.insert(newRecipe("Recipe A"))
        val recipeB = recipeDao.insert(newRecipe("Recipe B"))

        ingredientDao.insertAll(
            listOf(
                newIngredient(recipeA, "A-flour"),
                newIngredient(recipeA, "A-sugar"),
                newIngredient(recipeB, "B-eggs"),
            ),
        )

        ingredientDao.getByRecipeId(recipeA).test {
            val ingredients = awaitItem()
            assertEquals(setOf("A-flour", "A-sugar"), ingredients.map { it.foodName }.toSet())
            cancelAndIgnoreRemainingEvents()
        }

        ingredientDao.getByRecipeId(recipeB).test {
            val ingredients = awaitItem()
            assertEquals(setOf("B-eggs"), ingredients.map { it.foodName }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getByRecipeId returns empty list when recipe has no ingredients`() = runTest {
        val recipeId = recipeDao.insert(newRecipe())

        ingredientDao.getByRecipeId(recipeId).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteByRecipeId removes only ingredients for that recipe`() = runTest {
        val recipeA = recipeDao.insert(newRecipe("Recipe A"))
        val recipeB = recipeDao.insert(newRecipe("Recipe B"))
        ingredientDao.insertAll(
            listOf(
                newIngredient(recipeA, "A1"),
                newIngredient(recipeB, "B1"),
            ),
        )

        ingredientDao.deleteByRecipeId(recipeA)

        ingredientDao.getByRecipeId(recipeA).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
        ingredientDao.getByRecipeId(recipeB).test {
            val remaining = awaitItem()
            assertEquals(setOf("B1"), remaining.map { it.foodName }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting recipe cascades to its ingredients`() = runTest {
        val recipeId = recipeDao.insert(newRecipe("Cascade recipe"))
        ingredientDao.insertAll(
            listOf(
                newIngredient(recipeId, "Ing1"),
                newIngredient(recipeId, "Ing2"),
            ),
        )

        recipeDao.deleteById(recipeId)

        ingredientDao.getByRecipeId(recipeId).test {
            assertTrue(
                "Recipe deletion must cascade to its ingredients (architecture Section 5.2)",
                awaitItem().isEmpty(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}
