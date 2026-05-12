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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * In-memory Room database tests for [RecipeDao].
 *
 * Verifies:
 *  - `getAll` returns recipes ordered by most-recently-updated first.
 *  - `getById` returns the @Relation bundle with ingredients.
 *  - `insert` returns the auto-generated id.
 *  - `update` mutates the existing row in place.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecipeDaoTest {

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

    private fun recipe(name: String, updatedAt: Long): RecipeEntity = RecipeEntity(
        name = name,
        totalWeightG = 100.0,
        totalKcal = 200.0,
        totalProteinG = 10.0,
        totalCarbsG = 20.0,
        totalFatG = 5.0,
        createdAt = 1000L,
        updatedAt = updatedAt,
    )

    @Test
    fun `getAll returns recipes ordered by updatedAt descending`() = runTest {
        recipeDao.insert(recipe("Old", updatedAt = 1000L))
        recipeDao.insert(recipe("Newest", updatedAt = 3000L))
        recipeDao.insert(recipe("Middle", updatedAt = 2000L))

        recipeDao.getAll().test {
            val names = awaitItem().map { it.name }
            assertEquals(listOf("Newest", "Middle", "Old"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert returns the auto-generated id`() = runTest {
        val id = recipeDao.insert(recipe("First", updatedAt = 1000L))
        assertEquals(1L, id)
        val id2 = recipeDao.insert(recipe("Second", updatedAt = 2000L))
        assertEquals(2L, id2)
    }

    @Test
    fun `getById returns null when recipe does not exist`() = runTest {
        recipeDao.getById(9999L).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getById returns the recipe with all its ingredients`() = runTest {
        val recipeId = recipeDao.insert(recipe("Recipe with ingredients", updatedAt = 1000L))
        ingredientDao.insertAll(
            listOf(
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    foodName = "Flour",
                    weightG = 100.0,
                    kcalPer100g = 364.0,
                    proteinPer100g = 10.0,
                    carbsPer100g = 76.0,
                    fatPer100g = 1.0,
                ),
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    foodName = "Sugar",
                    weightG = 50.0,
                    kcalPer100g = 387.0,
                    proteinPer100g = 0.0,
                    carbsPer100g = 100.0,
                    fatPer100g = 0.0,
                ),
            ),
        )

        recipeDao.getById(recipeId).test {
            val relation = awaitItem()
            assertNotNull(relation)
            assertEquals("Recipe with ingredients", relation!!.recipe.name)
            assertEquals(2, relation.ingredients.size)
            assertEquals(setOf("Flour", "Sugar"), relation.ingredients.map { it.foodName }.toSet())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `update modifies an existing recipe in place`() = runTest {
        val id = recipeDao.insert(recipe("Original", updatedAt = 1000L))

        val updated = recipe("Renamed", updatedAt = 5000L).copy(id = id, totalKcal = 999.0)
        recipeDao.update(updated)

        recipeDao.getById(id).test {
            val relation = awaitItem()!!
            assertEquals("Renamed", relation.recipe.name)
            assertEquals(999.0, relation.recipe.totalKcal, 0.001)
            assertEquals(5000L, relation.recipe.updatedAt)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteById removes the recipe row`() = runTest {
        val id = recipeDao.insert(recipe("Doomed", updatedAt = 1000L))

        recipeDao.deleteById(id)

        recipeDao.getById(id).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
