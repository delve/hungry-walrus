package com.delve.hungrywalrus.qa

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeUiEvent
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel
import com.delve.hungrywalrus.ui.screen.createrecipe.IngredientDraft
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * QA unit tests verifying that [CreateRecipeViewModel.saveRecipe] trims leading and trailing
 * whitespace from the recipe name before persisting it.
 *
 * Gap filled:
 * The production code at `saveRecipe()` constructs the [Recipe] with
 * `name = state.recipeName.trim()`. No existing test captures the [Recipe] argument passed to
 * [RecipeRepository.saveRecipe] and asserts that its `name` field is trimmed. This is an
 * important boundary: a recipe named "  Pasta  " must be stored as "Pasta" so the display
 * and uniqueness logic never operate on whitespace-padded strings.
 *
 * Tests:
 * 1. Leading whitespace is stripped from the persisted recipe name.
 * 2. Trailing whitespace is stripped from the persisted recipe name.
 * 3. Both leading and trailing whitespace are stripped simultaneously.
 * 4. Internal whitespace (between words) is preserved — trim() only removes edges.
 * 5. A name with no whitespace is persisted unchanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeNameTrimTest {

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

    /** A single ingredient used across all save-path tests. */
    private val singleIngredient = IngredientDraft(
        name = "Pasta",
        weightG = 200.0,
        kcalPer100g = 150.0,
        proteinPer100g = 5.0,
        carbsPer100g = 30.0,
        fatPer100g = 1.5,
    )

    /**
     * Leading whitespace on the recipe name must be stripped before persisting.
     * "   Bolognese" → "Bolognese"
     */
    @Test
    fun `saveRecipe strips leading whitespace from recipe name`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("   Bolognese")
        viewModel.addIngredient(singleIngredient)

        val recipeSlot = slot<Recipe>()
        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            coVerify { recipeRepo.saveRecipe(capture(recipeSlot), any()) }
            assertEquals("Bolognese", recipeSlot.captured.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Trailing whitespace on the recipe name must be stripped before persisting.
     * "Carbonara   " → "Carbonara"
     */
    @Test
    fun `saveRecipe strips trailing whitespace from recipe name`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("Carbonara   ")
        viewModel.addIngredient(singleIngredient)

        val recipeSlot = slot<Recipe>()
        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            coVerify { recipeRepo.saveRecipe(capture(recipeSlot), any()) }
            assertEquals("Carbonara", recipeSlot.captured.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Both leading and trailing whitespace must be stripped.
     * "  Pasta Arrabiata  " → "Pasta Arrabiata"
     */
    @Test
    fun `saveRecipe strips both leading and trailing whitespace`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("  Pasta Arrabiata  ")
        viewModel.addIngredient(singleIngredient)

        val recipeSlot = slot<Recipe>()
        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            coVerify { recipeRepo.saveRecipe(capture(recipeSlot), any()) }
            assertEquals("Pasta Arrabiata", recipeSlot.captured.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Internal whitespace (spaces between words) must NOT be modified by trim().
     * "Chicken  Tikka  Masala" must be persisted as-is — trim() only removes edge whitespace.
     */
    @Test
    fun `saveRecipe preserves internal whitespace in recipe name`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("Chicken  Tikka  Masala")
        viewModel.addIngredient(singleIngredient)

        val recipeSlot = slot<Recipe>()
        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            coVerify { recipeRepo.saveRecipe(capture(recipeSlot), any()) }
            assertEquals("Chicken  Tikka  Masala", recipeSlot.captured.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * A name with no surrounding whitespace must be persisted unchanged.
     * "Stir Fry" → "Stir Fry"
     */
    @Test
    fun `saveRecipe does not alter a name with no surrounding whitespace`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("Stir Fry")
        viewModel.addIngredient(singleIngredient)

        val recipeSlot = slot<Recipe>()
        viewModel.events.test {
            viewModel.saveRecipe()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(CreateRecipeUiEvent.RecipeSaved, awaitItem())
            coVerify { recipeRepo.saveRecipe(capture(recipeSlot), any()) }
            assertEquals("Stir Fry", recipeSlot.captured.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
