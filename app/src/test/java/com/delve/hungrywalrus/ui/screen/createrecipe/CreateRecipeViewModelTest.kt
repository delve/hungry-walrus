package com.delve.hungrywalrus.ui.screen.createrecipe

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeIngredient
import com.delve.hungrywalrus.domain.model.RecipeWithIngredients
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecipeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recipeRepo: RecipeRepository
    private val scaleUseCase = ScaleNutritionUseCase()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        recipeRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CreateRecipeViewModel(
        recipeRepo, scaleUseCase, SavedStateHandle(),
    )

    @Test
    fun `initial state is create mode with empty data`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertEquals("", state.recipeName)
        assertTrue(state.ingredients.isEmpty())
        assertFalse(state.isEditMode)
        assertEquals(0.0, state.liveTotals.kcal, 0.001)
    }

    @Test
    fun `setRecipeName updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("Test Recipe")
        assertEquals("Test Recipe", viewModel.uiState.value.recipeName)
    }

    @Test
    fun `addIngredient updates ingredients and recomputes totals`() = runTest {
        val viewModel = createViewModel()
        val ingredient = IngredientDraft(
            name = "Chicken",
            weightG = 200.0,
            kcalPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatPer100g = 3.6,
        )

        viewModel.addIngredient(ingredient)

        val state = viewModel.uiState.value
        assertEquals(1, state.ingredients.size)
        assertEquals(200.0, state.totalWeightG, 0.001)
        // 200g * 165/100 = 330 kcal
        assertEquals(330.0, state.liveTotals.kcal, 0.001)
        // 200g * 31/100 = 62g protein
        assertEquals(62.0, state.liveTotals.proteinG, 0.001)
    }

    @Test
    fun `removeIngredient updates ingredients and recomputes totals`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(
            IngredientDraft("A", 100.0, 100.0, 10.0, 20.0, 5.0),
        )
        viewModel.addIngredient(
            IngredientDraft("B", 100.0, 200.0, 20.0, 30.0, 10.0),
        )
        assertEquals(2, viewModel.uiState.value.ingredients.size)

        viewModel.removeIngredient(0)

        assertEquals(1, viewModel.uiState.value.ingredients.size)
        assertEquals("B", viewModel.uiState.value.ingredients[0].name)
        assertEquals(200.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
    }

    @Test
    fun `saveRecipe calls repository and emits event`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("My Recipe")
        viewModel.addIngredient(
            IngredientDraft("Item", 100.0, 100.0, 10.0, 20.0, 5.0),
        )

        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            coVerify { recipeRepo.saveRecipe(any(), any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveRecipe does nothing when name empty`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(
            IngredientDraft("Item", 100.0, 100.0, 10.0, 20.0, 5.0),
        )

        viewModel.saveRecipe()
        testDispatcher.scheduler.advanceUntilIdle()

        // No event emitted, not saving
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saveRecipe does nothing when no ingredients`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("Recipe")

        viewModel.saveRecipe()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `edit mode loads existing recipe and preserves originalCreatedAt`() = runTest {
        val createdAt = 1_700_000_000_000L
        val recipe = Recipe(
            id = 1, name = "Bolognese", totalWeightG = 300.0,
            totalKcal = 450.0, totalProteinG = 30.0, totalCarbsG = 40.0, totalFatG = 15.0,
            createdAt = createdAt, updatedAt = createdAt,
        )
        val ingredient = RecipeIngredient(
            recipeId = 1, foodName = "Beef mince", weightG = 200.0,
            kcalPer100g = 200.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 10.0,
        )
        every { recipeRepo.getRecipeWithIngredients(1) } returns flowOf(
            RecipeWithIngredients(recipe, listOf(ingredient)),
        )

        val viewModel = CreateRecipeViewModel(
            recipeRepo, scaleUseCase, SavedStateHandle(mapOf("id" to 1L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertEquals("Bolognese", state.recipeName)
        assertEquals(1, state.ingredients.size)
        assertEquals("Beef mince", state.ingredients[0].name)
        assertEquals(createdAt, state.originalCreatedAt)
        assertFalse(state.isLoading)
    }

    @Test
    fun `isDirty is false initially in create mode and true after setRecipeName`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isDirty)

        viewModel.setRecipeName("Test")
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun `isDirty is false after loading edit mode and true after first user mutation`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Bolognese", totalWeightG = 300.0,
            totalKcal = 450.0, totalProteinG = 30.0, totalCarbsG = 40.0, totalFatG = 15.0,
            createdAt = 0L, updatedAt = 0L,
        )
        val ingredient = RecipeIngredient(
            recipeId = 1, foodName = "Beef mince", weightG = 200.0,
            kcalPer100g = 200.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 10.0,
        )
        every { recipeRepo.getRecipeWithIngredients(1) } returns flowOf(
            RecipeWithIngredients(recipe, listOf(ingredient)),
        )

        val viewModel = CreateRecipeViewModel(
            recipeRepo, scaleUseCase, SavedStateHandle(mapOf("id" to 1L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Loaded with existing data but no user change yet — isDirty must be false
        assertFalse(viewModel.uiState.value.isDirty)

        // User renames the recipe — now dirty
        viewModel.setRecipeName("Bolognese Variant")
        assertTrue(viewModel.uiState.value.isDirty)
    }

    @Test
    fun `isDirty becomes true when addIngredient or removeIngredient is called`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isDirty)

        viewModel.addIngredient(IngredientDraft("Pasta", 100.0, 150.0, 5.0, 30.0, 1.0))
        assertTrue(viewModel.uiState.value.isDirty)

        // Create a second viewModel to test removeIngredient independently
        val viewModel2 = createViewModel()
        viewModel2.addIngredient(IngredientDraft("Item", 100.0, 100.0, 10.0, 20.0, 5.0))
        // Reset by creating fresh — removeIngredient on a create-mode vm with one item
        // We test just the removeIngredient path sets isDirty = true (it was already true here,
        // but the copy always includes isDirty = true in removeIngredient).
        viewModel2.removeIngredient(0)
        assertTrue(viewModel2.uiState.value.isDirty)
    }

    @Test
    fun `saveRecipe in edit mode calls updateRecipe with original createdAt`() = runTest {
        val createdAt = 1_700_000_000_000L
        val recipe = Recipe(
            id = 1, name = "Pasta", totalWeightG = 200.0,
            totalKcal = 300.0, totalProteinG = 10.0, totalCarbsG = 50.0, totalFatG = 5.0,
            createdAt = createdAt, updatedAt = createdAt,
        )
        every { recipeRepo.getRecipeWithIngredients(1) } returns flowOf(
            RecipeWithIngredients(recipe, emptyList()),
        )

        val viewModel = CreateRecipeViewModel(
            recipeRepo, scaleUseCase, SavedStateHandle(mapOf("id" to 1L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addIngredient(IngredientDraft("Pasta", 200.0, 150.0, 5.0, 25.0, 2.5))

        val recipeSlot = slot<Recipe>()
        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            // updateRecipe called (not saveRecipe), confirming edit path.
            // Capture and assert original createdAt is threaded through to the saved Recipe.
            coVerify { recipeRepo.updateRecipe(capture(recipeSlot), any()) }
            assertEquals(createdAt, recipeSlot.captured.createdAt)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
