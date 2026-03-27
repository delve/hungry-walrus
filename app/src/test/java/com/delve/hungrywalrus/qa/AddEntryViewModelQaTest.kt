package com.delve.hungrywalrus.qa

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.OfflineException
import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryUiEvent
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.ui.screen.addentry.SearchState
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.coEvery
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA unit tests for [AddEntryViewModel] filling coverage gaps:
 *
 * - lookupBarcode success (found=true) path.
 * - lookupBarcode offline (IOException) path.
 * - applyMissingValues updates selectedFood and clears missing fields.
 * - getIngredientData for a selected recipe produces correct per-100g conversion.
 * - getIngredientData returns null when weightG is not yet set.
 * - selectFood returns true when food has missing fields.
 * - selectRecipe sets isRecipeSource and clears selectedFood.
 * - resetState restores initial conditions.
 * - setWeight clears scaledNutrition when weight is invalid (e.g. negative).
 * - saveEntry does not proceed when scaledNutrition is null.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModelQaTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logRepo: LogEntryRepository
    private lateinit var foodLookupRepo: FoodLookupRepository
    private lateinit var recipeRepo: RecipeRepository
    private val scaleUseCase = ScaleNutritionUseCase()
    private val validateUseCase = ValidateFoodDataUseCase()
    private lateinit var apiKeyStore: ApiKeyStore

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logRepo = mockk(relaxed = true)
        foodLookupRepo = mockk()
        recipeRepo = mockk()
        apiKeyStore = mockk()
        every { apiKeyStore.hasApiKey() } returns false
        every { apiKeyStore.getApiKey() } returns null
        every { recipeRepo.getAllRecipes() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AddEntryViewModel(
        logRepo, foodLookupRepo, recipeRepo, scaleUseCase, validateUseCase, apiKeyStore,
    )

    private fun completeFood(name: String = "Chicken") = FoodSearchResult(
        id = "usda:1", name = name, source = FoodSource.USDA,
        kcalPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6,
        missingFields = emptySet(),
    )

    private fun incompleteFood() = FoodSearchResult(
        id = "off:2", name = "Mystery Bar", source = FoodSource.OPEN_FOOD_FACTS,
        kcalPer100g = null, proteinPer100g = 8.0, carbsPer100g = 50.0, fatPer100g = 10.0,
        missingFields = setOf(NutritionField.KCAL),
    )

    // --- selectFood ---

    @Test
    fun `selectFood returns true when food has missing fields`() = runTest {
        val viewModel = createViewModel()
        val hasMissing = viewModel.selectFood(incompleteFood())
        assertTrue(hasMissing)
    }

    @Test
    fun `selectFood clears previous recipe selection`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Soup", totalWeightG = 500.0,
            totalKcal = 250.0, totalProteinG = 15.0, totalCarbsG = 30.0, totalFatG = 8.0,
            createdAt = 0, updatedAt = 0,
        )
        val viewModel = createViewModel()
        viewModel.selectRecipe(recipe)
        assertNotNull(viewModel.uiState.value.selectedRecipe)

        viewModel.selectFood(completeFood())

        assertNull(viewModel.uiState.value.selectedRecipe)
        assertFalse(viewModel.uiState.value.isRecipeSource)
    }

    // --- selectRecipe ---

    @Test
    fun `selectRecipe sets isRecipeSource and stores recipe`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Pasta", totalWeightG = 400.0,
            totalKcal = 600.0, totalProteinG = 25.0, totalCarbsG = 80.0, totalFatG = 15.0,
            createdAt = 0, updatedAt = 0,
        )
        val viewModel = createViewModel()
        viewModel.selectRecipe(recipe)

        assertTrue(viewModel.uiState.value.isRecipeSource)
        assertEquals(recipe, viewModel.uiState.value.selectedRecipe)
        assertNull(viewModel.uiState.value.selectedFood)
        assertEquals("Pasta", viewModel.uiState.value.foodName)
    }

    // --- applyMissingValues ---

    /**
     * Spec: "If any of the four core nutritional values are missing from an API response,
     * prompt the user to provide an estimate for the missing values before the entry can be saved."
     * After the user supplies the missing value, the food should be complete.
     */
    @Test
    fun `applyMissingValues fills in missing kcal so food becomes complete`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectFood(incompleteFood())

        // Before applying: missing kcal
        assertFalse(validateUseCase.isComplete(viewModel.uiState.value.selectedFood!!))

        // User supplies kcal estimate
        viewModel.applyMissingValues(kcal = 300.0, protein = null, carbs = null, fat = null)

        // After applying: food should be complete
        val updatedFood = viewModel.uiState.value.selectedFood
        assertNotNull(updatedFood)
        assertTrue(validateUseCase.isComplete(updatedFood!!))
        assertEquals(300.0, updatedFood.kcalPer100g!!, 0.001)
    }

    @Test
    fun `applyMissingValues does nothing when no food selected`() = runTest {
        val viewModel = createViewModel()
        // No food selected
        assertNull(viewModel.uiState.value.selectedFood)

        // Should not throw
        viewModel.applyMissingValues(kcal = 200.0, protein = null, carbs = null, fat = null)

        // State unchanged
        assertNull(viewModel.uiState.value.selectedFood)
    }

    // --- getIngredientData for recipe ---

    /**
     * When a recipe is selected and a weight is provided, getIngredientData must return
     * per-100g equivalent values derived from the recipe's totals.
     * Formula: (recipeTotalValue / recipeTotalWeightG) * 100
     */
    @Test
    fun `getIngredientData for recipe returns per-100g equivalent values`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Bolognese", totalWeightG = 800.0,
            totalKcal = 1200.0, totalProteinG = 80.0, totalCarbsG = 120.0, totalFatG = 40.0,
            createdAt = 0, updatedAt = 0,
        )
        val viewModel = createViewModel()
        viewModel.selectRecipe(recipe)
        viewModel.setWeight("200")  // 200g portion

        val ingredientData = viewModel.getIngredientData()

        assertNotNull(ingredientData)
        assertEquals("Bolognese", ingredientData!!.name)
        assertEquals(200.0, ingredientData.weightG, 0.001)
        // per-100g kcal = (1200 / 800) * 100 = 150
        assertEquals(150.0, ingredientData.kcalPer100g, 0.001)
        // per-100g protein = (80 / 800) * 100 = 10
        assertEquals(10.0, ingredientData.proteinPer100g, 0.001)
        // per-100g carbs = (120 / 800) * 100 = 15
        assertEquals(15.0, ingredientData.carbsPer100g, 0.001)
        // per-100g fat = (40 / 800) * 100 = 5
        assertEquals(5.0, ingredientData.fatPer100g, 0.001)
    }

    @Test
    fun `getIngredientData returns null when no food or recipe selected`() = runTest {
        val viewModel = createViewModel()
        viewModel.setWeight("150")

        assertNull(viewModel.getIngredientData())
    }

    @Test
    fun `getIngredientData returns null when weightG is empty string`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectFood(completeFood())
        // Do not set weight

        assertNull(viewModel.getIngredientData())
    }

    // --- setWeight edge cases ---

    @Test
    fun `setWeight with zero string clears scaledNutrition`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectFood(completeFood())
        viewModel.setWeight("150")
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        viewModel.setWeight("0")

        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    @Test
    fun `setWeight with negative string clears scaledNutrition`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectFood(completeFood())
        viewModel.setWeight("100")
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        viewModel.setWeight("-50")

        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    @Test
    fun `setWeight with non-numeric string clears scaledNutrition`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectFood(completeFood())
        viewModel.setWeight("100")
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        viewModel.setWeight("abc")

        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    // --- lookupBarcode ---

    @Test
    fun `lookupBarcode found=true emits BarcodeResult with found=true`() = runTest {
        val food = completeFood("Scanned Food")
        coEvery { foodLookupRepo.lookupBarcode("12345678") } returns Result.success(food)

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.lookupBarcode("12345678")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as AddEntryUiEvent.BarcodeResult
            assertTrue(event.found)
            assertEquals("12345678", event.barcode)
            assertFalse(event.isError)
            cancelAndIgnoreRemainingEvents()
        }

        // Food is also stored in state
        assertEquals("Scanned Food", viewModel.uiState.value.foodName)
    }

    @Test
    fun `lookupBarcode not found (null result) emits BarcodeResult with found=false`() = runTest {
        coEvery { foodLookupRepo.lookupBarcode("00000000") } returns Result.success(null)

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.lookupBarcode("00000000")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as AddEntryUiEvent.BarcodeResult
            assertFalse(event.found)
            assertFalse(event.isError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lookupBarcode offline emits BarcodeResult with isError=true and sets error state`() = runTest {
        coEvery { foodLookupRepo.lookupBarcode("offline") } returns
            Result.failure(OfflineException("No network"))

        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.lookupBarcode("offline")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as AddEntryUiEvent.BarcodeResult
            assertFalse(event.found)
            assertTrue(event.isError)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(SearchState.Error, viewModel.uiState.value.searchState)
        assertTrue(viewModel.uiState.value.searchErrorMessage.contains("internet", ignoreCase = true))
    }

    // --- saveEntry guard ---

    @Test
    fun `saveEntry does nothing when scaledNutrition is null`() = runTest {
        val viewModel = createViewModel()
        // No food selected, no weight set -> scaledNutrition is null

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()

            // No event should be emitted
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- resetState ---

    @Test
    fun `resetState clears selected food and resets to initial conditions`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectFood(completeFood())
        viewModel.setWeight("200")
        assertNotNull(viewModel.uiState.value.selectedFood)
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        viewModel.resetState()

        assertNull(viewModel.uiState.value.selectedFood)
        assertNull(viewModel.uiState.value.scaledNutrition)
        assertEquals("", viewModel.uiState.value.weightG)
        assertEquals("", viewModel.uiState.value.foodName)
        assertEquals(SearchState.Idle, viewModel.uiState.value.searchState)
    }

    // --- Ingredient mode ---

    @Test
    fun `setIngredientMode updates ingredientMode flag`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.ingredientMode)

        viewModel.setIngredientMode(true)
        assertTrue(viewModel.uiState.value.ingredientMode)

        viewModel.setIngredientMode(false)
        assertFalse(viewModel.uiState.value.ingredientMode)
    }
}
