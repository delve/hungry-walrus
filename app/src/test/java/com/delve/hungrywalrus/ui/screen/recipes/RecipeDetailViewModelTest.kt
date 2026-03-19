package com.delve.hungrywalrus.ui.screen.recipes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeWithIngredients
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
class RecipeDetailViewModelTest {

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
        every { recipeRepo.getRecipeWithIngredients(1L) } returns flowOf(null)
        val savedState = SavedStateHandle(mapOf("id" to 1L))
        val viewModel = RecipeDetailViewModel(recipeRepo, savedState)

        viewModel.uiState.test {
            assertEquals(RecipeDetailUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Content when recipe found`() = runTest {
        val recipe = Recipe(id = 1, name = "Test", totalWeightG = 100.0, totalKcal = 200.0,
            totalProteinG = 20.0, totalCarbsG = 30.0, totalFatG = 5.0,
            createdAt = 0, updatedAt = 0)
        val rwi = RecipeWithIngredients(recipe, emptyList())
        every { recipeRepo.getRecipeWithIngredients(1L) } returns flowOf(rwi)

        val savedState = SavedStateHandle(mapOf("id" to 1L))
        val viewModel = RecipeDetailViewModel(recipeRepo, savedState)

        viewModel.uiState.test {
            assertEquals(RecipeDetailUiState.Loading, awaitItem())
            val content = awaitItem() as RecipeDetailUiState.Content
            assertEquals("Test", content.recipeWithIngredients.recipe.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits NotFound when recipe is null`() = runTest {
        every { recipeRepo.getRecipeWithIngredients(99L) } returns flowOf(null)

        val savedState = SavedStateHandle(mapOf("id" to 99L))
        val viewModel = RecipeDetailViewModel(recipeRepo, savedState)

        viewModel.uiState.test {
            assertEquals(RecipeDetailUiState.Loading, awaitItem())
            assertEquals(RecipeDetailUiState.NotFound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteRecipe calls repository and emits event`() = runTest {
        every { recipeRepo.getRecipeWithIngredients(1L) } returns flowOf(null)
        val savedState = SavedStateHandle(mapOf("id" to 1L))
        val viewModel = RecipeDetailViewModel(recipeRepo, savedState)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.deleteRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(RecipeDetailUiEvent.RecipeDeleted, awaitItem())
            coVerify { recipeRepo.deleteRecipe(1L) }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
