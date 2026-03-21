package com.delve.hungrywalrus.ui.screen.recipes

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.Recipe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recipeRepo: RecipeRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        recipeRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        every { recipeRepo.getAllRecipes() } returns flowOf(emptyList())
        val viewModel = RecipeListViewModel(recipeRepo)

        viewModel.uiState.test {
            assertEquals(RecipeListUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Content with recipes`() = runTest {
        val recipes = listOf(
            Recipe(id = 1, name = "Test", totalWeightG = 100.0, totalKcal = 200.0,
                totalProteinG = 20.0, totalCarbsG = 30.0, totalFatG = 5.0,
                createdAt = 0, updatedAt = 0),
        )
        every { recipeRepo.getAllRecipes() } returns flowOf(recipes)

        val viewModel = RecipeListViewModel(recipeRepo)

        viewModel.uiState.test {
            assertEquals(RecipeListUiState.Loading, awaitItem())
            val content = awaitItem() as RecipeListUiState.Content
            assertEquals(1, content.recipes.size)
            assertEquals("Test", content.recipes[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteRecipe calls repository`() = runTest {
        every { recipeRepo.getAllRecipes() } returns flowOf(emptyList())
        val viewModel = RecipeListViewModel(recipeRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteRecipe(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { recipeRepo.deleteRecipe(1L) }
    }
}
