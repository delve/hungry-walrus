package com.delve.hungrywalrus.data.repository

import androidx.room.withTransaction
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase
import com.delve.hungrywalrus.data.local.dao.RecipeDao
import com.delve.hungrywalrus.data.local.dao.RecipeIngredientDao
import com.delve.hungrywalrus.data.local.entity.RecipeEntity
import com.delve.hungrywalrus.data.local.entity.RecipeIngredientEntity
import com.delve.hungrywalrus.data.local.entity.RecipeWithIngredients
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeIngredient
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RecipeRepositoryImplTest {

    private lateinit var database: HungryWalrusDatabase
    private lateinit var recipeDao: RecipeDao
    private lateinit var ingredientDao: RecipeIngredientDao
    private lateinit var repository: RecipeRepositoryImpl

    @Before
    fun setUp() {
        database = mockk()
        recipeDao = mockk(relaxed = true)
        ingredientDao = mockk(relaxed = true)

        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { database.withTransaction<Any>(any()) } coAnswers {
            @Suppress("UNCHECKED_CAST")
            (secondArg<suspend () -> Any>()).invoke()
        }

        repository = RecipeRepositoryImpl(database, recipeDao, ingredientDao)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    private fun sampleRecipe(id: Long = 0) = Recipe(
        id = id,
        name = "Test Recipe",
        totalWeightG = 300.0,
        totalKcal = 600.0,
        totalProteinG = 30.0,
        totalCarbsG = 80.0,
        totalFatG = 15.0,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private fun sampleIngredient(recipeId: Long = 0) = RecipeIngredient(
        id = 0,
        recipeId = recipeId,
        foodName = "Oats",
        weightG = 100.0,
        kcalPer100g = 389.0,
        proteinPer100g = 17.0,
        carbsPer100g = 66.0,
        fatPer100g = 7.0,
    )

    // --- saveRecipe ---

    @Test
    fun `saveRecipe uses DAO-generated ID for ingredient inserts, not recipe id field`() = runTest {
        val generatedId = 42L
        coEvery { recipeDao.insert(any()) } returns generatedId
        val ingredientSlot = slot<List<RecipeIngredientEntity>>()
        coEvery { ingredientDao.insertAll(capture(ingredientSlot)) } returns Unit

        repository.saveRecipe(sampleRecipe(id = 0), listOf(sampleIngredient(recipeId = 0)))

        assertEquals(1, ingredientSlot.captured.size)
        assertEquals(generatedId, ingredientSlot.captured[0].recipeId)
    }

    @Test
    fun `saveRecipe returns the DAO-generated recipe ID`() = runTest {
        coEvery { recipeDao.insert(any()) } returns 99L

        val returnedId = repository.saveRecipe(sampleRecipe(), listOf(sampleIngredient()))

        assertEquals(99L, returnedId)
    }

    // --- updateRecipe ---

    @Test
    fun `updateRecipe deletes old ingredients before inserting new ones`() = runTest {
        val recipe = sampleRecipe(id = 5L)
        val newIngredient = sampleIngredient(recipeId = 5L)

        repository.updateRecipe(recipe, listOf(newIngredient))

        coVerifyOrder {
            recipeDao.update(any())
            ingredientDao.deleteByRecipeId(5L)
            ingredientDao.insertAll(any())
        }
    }

    @Test
    fun `updateRecipe inserts new ingredients with the recipe id`() = runTest {
        val recipe = sampleRecipe(id = 7L)
        val ingredientSlot = slot<List<RecipeIngredientEntity>>()
        coEvery { ingredientDao.insertAll(capture(ingredientSlot)) } returns Unit

        repository.updateRecipe(recipe, listOf(sampleIngredient()))

        assertEquals(1, ingredientSlot.captured.size)
        assertEquals(7L, ingredientSlot.captured[0].recipeId)
    }

    // --- getAllRecipes ---

    @Test
    fun `getAllRecipes returns mapped domain list from DAO`() = runTest {
        val entities = listOf(
            RecipeEntity(
                id = 1L,
                name = "Oat Porridge",
                totalWeightG = 250.0,
                totalKcal = 300.0,
                totalProteinG = 12.0,
                totalCarbsG = 50.0,
                totalFatG = 6.0,
                createdAt = 1000L,
                updatedAt = 2000L,
            ),
        )
        coEvery { recipeDao.getAll() } returns flowOf(entities)

        repository.getAllRecipes().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Oat Porridge", result[0].name)
            assertEquals(1L, result[0].id)
            awaitComplete()
        }
    }

    // --- deleteRecipe ---

    @Test
    fun `deleteRecipe calls DAO with correct id`() = runTest {
        repository.deleteRecipe(42L)

        coVerify(exactly = 1) { recipeDao.deleteById(42L) }
    }

    // --- getRecipeWithIngredients ---

    @Test
    fun `getRecipeWithIngredients returns null in flow when recipe does not exist`() = runTest {
        coEvery { recipeDao.getById(999L) } returns flowOf(null)

        repository.getRecipeWithIngredients(999L).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getRecipeWithIngredients returns mapped domain model when recipe exists`() = runTest {
        val entity = RecipeWithIngredients(
            recipe = RecipeEntity(
                id = 1L,
                name = "Test Recipe",
                totalWeightG = 300.0,
                totalKcal = 600.0,
                totalProteinG = 30.0,
                totalCarbsG = 80.0,
                totalFatG = 15.0,
                createdAt = 1000L,
                updatedAt = 1000L,
            ),
            ingredients = listOf(
                RecipeIngredientEntity(
                    id = 10L,
                    recipeId = 1L,
                    foodName = "Oats",
                    weightG = 100.0,
                    kcalPer100g = 389.0,
                    proteinPer100g = 17.0,
                    carbsPer100g = 66.0,
                    fatPer100g = 7.0,
                ),
            ),
        )
        coEvery { recipeDao.getById(1L) } returns flowOf(entity)

        repository.getRecipeWithIngredients(1L).test {
            val result = awaitItem()!!
            assertEquals("Test Recipe", result.recipe.name)
            assertEquals(1, result.ingredients.size)
            assertEquals("Oats", result.ingredients[0].foodName)
            awaitComplete()
        }
    }
}
