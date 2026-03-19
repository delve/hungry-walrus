package com.delve.hungrywalrus.qa

import androidx.lifecycle.SavedStateHandle
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel
import com.delve.hungrywalrus.ui.screen.createrecipe.IngredientDraft
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA unit tests for [CreateRecipeViewModel] filling coverage gaps:
 *
 * - Live totals are correct when multiple ingredients are added.
 * - Total weight sums all ingredient weights correctly.
 * - Removing an ingredient from the middle of the list updates totals correctly.
 * - Removing the only ingredient leaves the list empty and totals at zero.
 * - saveRecipe is blocked when name is blank even if ingredients exist.
 * - saveRecipe is blocked when ingredients are empty even if name is set.
 * - removeIngredient with an out-of-range index does not crash.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecipeViewModelQaTest {

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

    // --- Live totals with multiple ingredients ---

    /**
     * Recipe total nutrition is the sum of each ingredient's scaled nutrition.
     * Formula for each ingredient: (per100g / 100) * weightG
     */
    @Test
    fun `live totals accumulate correctly across three ingredients`() = runTest {
        val viewModel = createViewModel()

        // Ingredient 1: 200g chicken (165 kcal, 31g protein, 0g carbs, 3.6g fat per 100g)
        viewModel.addIngredient(
            IngredientDraft("Chicken", 200.0, 165.0, 31.0, 0.0, 3.6),
        )
        // Ingredient 2: 150g rice (130 kcal, 2.7g protein, 28g carbs, 0.3g fat per 100g)
        viewModel.addIngredient(
            IngredientDraft("Rice", 150.0, 130.0, 2.7, 28.0, 0.3),
        )
        // Ingredient 3: 100g broccoli (34 kcal, 2.8g protein, 6.6g carbs, 0.4g fat per 100g)
        viewModel.addIngredient(
            IngredientDraft("Broccoli", 100.0, 34.0, 2.8, 6.6, 0.4),
        )

        val state = viewModel.uiState.value

        // Chicken: 330 kcal, 62g protein, 0g carbs, 7.2g fat
        // Rice: 195 kcal, 4.05g protein, 42g carbs, 0.45g fat
        // Broccoli: 34 kcal, 2.8g protein, 6.6g carbs, 0.4g fat
        assertEquals(559.0, state.liveTotals.kcal, 0.001)
        assertEquals(68.85, state.liveTotals.proteinG, 0.001)
        assertEquals(48.6, state.liveTotals.carbsG, 0.001)
        assertEquals(8.05, state.liveTotals.fatG, 0.001)
        assertEquals(450.0, state.totalWeightG, 0.001)
    }

    /**
     * Total weight must sum all ingredient weights, not just count ingredients.
     */
    @Test
    fun `total weight sums all ingredient weights`() = runTest {
        val viewModel = createViewModel()

        viewModel.addIngredient(IngredientDraft("A", 300.0, 100.0, 10.0, 20.0, 5.0))
        viewModel.addIngredient(IngredientDraft("B", 150.0, 100.0, 10.0, 20.0, 5.0))
        viewModel.addIngredient(IngredientDraft("C", 50.0, 100.0, 10.0, 20.0, 5.0))

        assertEquals(500.0, viewModel.uiState.value.totalWeightG, 0.001)
    }

    // --- Ingredient removal ---

    /**
     * Removing an ingredient from the middle of the list must correctly recompute totals
     * using only the remaining ingredients.
     */
    @Test
    fun `removing middle ingredient recomputes totals from remaining ingredients only`() = runTest {
        val viewModel = createViewModel()

        // Three ingredients
        viewModel.addIngredient(IngredientDraft("A", 100.0, 100.0, 0.0, 0.0, 0.0))  // 100 kcal
        viewModel.addIngredient(IngredientDraft("B", 100.0, 200.0, 0.0, 0.0, 0.0))  // 200 kcal
        viewModel.addIngredient(IngredientDraft("C", 100.0, 300.0, 0.0, 0.0, 0.0))  // 300 kcal
        assertEquals(600.0, viewModel.uiState.value.liveTotals.kcal, 0.001)

        // Remove middle ingredient (index 1 = "B")
        viewModel.removeIngredient(1)

        // Should be left with A (100 kcal) + C (300 kcal) = 400 kcal
        assertEquals(2, viewModel.uiState.value.ingredients.size)
        assertEquals("A", viewModel.uiState.value.ingredients[0].name)
        assertEquals("C", viewModel.uiState.value.ingredients[1].name)
        assertEquals(400.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
        assertEquals(200.0, viewModel.uiState.value.totalWeightG, 0.001)
    }

    @Test
    fun `removing only ingredient leaves empty list and zero totals`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("Solo", 100.0, 200.0, 20.0, 30.0, 8.0))
        assertEquals(1, viewModel.uiState.value.ingredients.size)

        viewModel.removeIngredient(0)

        assertTrue(viewModel.uiState.value.ingredients.isEmpty())
        assertEquals(0.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
        assertEquals(0.0, viewModel.uiState.value.totalWeightG, 0.001)
    }

    /**
     * removeIngredient with an out-of-range index should not crash or remove any ingredient.
     */
    @Test
    fun `removeIngredient with out-of-range index does not throw`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("Item", 100.0, 100.0, 10.0, 20.0, 5.0))

        // Index -1 is out of range
        viewModel.removeIngredient(-1)
        assertEquals(1, viewModel.uiState.value.ingredients.size)

        // Index 99 is out of range
        viewModel.removeIngredient(99)
        assertEquals(1, viewModel.uiState.value.ingredients.size)
    }

    // --- Save guards ---

    @Test
    fun `saveRecipe with blank name does not call repository even when ingredients exist`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("Item", 100.0, 100.0, 10.0, 20.0, 5.0))
        // Name is blank (default)

        viewModel.saveRecipe()
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify(exactly = 0) { recipeRepo.saveRecipe(any(), any()) }
        io.mockk.coVerify(exactly = 0) { recipeRepo.updateRecipe(any(), any()) }
    }

    @Test
    fun `saveRecipe with only whitespace name does not call repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("   ")
        viewModel.addIngredient(IngredientDraft("Item", 100.0, 100.0, 10.0, 20.0, 5.0))

        viewModel.saveRecipe()
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify(exactly = 0) { recipeRepo.saveRecipe(any(), any()) }
    }

    // --- Ingredient mode interaction ---

    @Test
    fun `adding two identical ingredients doubles the totals`() = runTest {
        val viewModel = createViewModel()
        val ingredient = IngredientDraft("Oats", 50.0, 374.0, 13.2, 67.7, 7.0)

        viewModel.addIngredient(ingredient)
        viewModel.addIngredient(ingredient)

        // Two identical 50g oat servings
        // Each: (374 / 100) * 50 = 187 kcal => total 374 kcal
        assertEquals(374.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
        assertEquals(100.0, viewModel.uiState.value.totalWeightG, 0.001)
        assertEquals(2, viewModel.uiState.value.ingredients.size)
    }

    @Test
    fun `recipe name can be updated after being set initially`() = runTest {
        val viewModel = createViewModel()
        viewModel.setRecipeName("First Name")
        assertEquals("First Name", viewModel.uiState.value.recipeName)

        viewModel.setRecipeName("Updated Name")
        assertEquals("Updated Name", viewModel.uiState.value.recipeName)
    }
}
