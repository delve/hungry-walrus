package com.delve.hungrywalrus.integration

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for the AddEntryViewModel connecting to real use case implementations.
 *
 * Tests verify:
 * - Per-100g scaling wired through the ViewModel using real ScaleNutritionUseCase.
 * - Recipe portion scaling wired through the ViewModel.
 * - Missing-field validation lifecycle through ValidateFoodDataUseCase.
 * - LogEntry created with correctly scaled values when saveEntry is called.
 * - Zero and invalid weight handling.
 * - Negative value rejection (weight <= 0 produces no scaled nutrition).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logRepo: LogEntryRepository
    private lateinit var foodLookupRepo: FoodLookupRepository
    private lateinit var recipeRepo: RecipeRepository
    private lateinit var apiKeyStore: ApiKeyStore

    // Real use cases — not mocked
    private val scaleUseCase = ScaleNutritionUseCase()
    private val validateUseCase = ValidateFoodDataUseCase()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logRepo = mockk(relaxed = true)
        foodLookupRepo = mockk()
        recipeRepo = mockk()
        apiKeyStore = mockk()
        every { apiKeyStore.hasApiKey() } returns true
        every { apiKeyStore.getApiKey() } returns "test-key"
        every { recipeRepo.getAllRecipes() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AddEntryViewModel(
        logRepo, foodLookupRepo, recipeRepo, scaleUseCase, validateUseCase, apiKeyStore,
    )

    // ---- Per-100g scaling end-to-end ----

    @Test
    fun `setWeight with 200g produces correctly scaled nutrition for API food`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:12345", name = "Brown Rice", source = FoodSource.USDA,
            kcalPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("200")

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        assertEquals(260.0, scaled!!.kcal, 0.001)
        assertEquals(5.4, scaled.proteinG, 0.001)
        assertEquals(56.0, scaled.carbsG, 0.001)
        assertEquals(0.6, scaled.fatG, 0.001)
    }

    @Test
    fun `setWeight with 100g returns values equal to per-100g reference`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:99", name = "Oats", source = FoodSource.USDA,
            kcalPer100g = 374.0, proteinPer100g = 13.2, carbsPer100g = 67.7, fatPer100g = 7.0,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("100")

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        assertEquals(374.0, scaled!!.kcal, 0.001)
        assertEquals(13.2, scaled.proteinG, 0.001)
    }

    @Test
    fun `setWeight with zero does not compute scaled nutrition`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:1", name = "Apple", source = FoodSource.USDA,
            kcalPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 14.0, fatPer100g = 0.2,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("0")

        // Weight of 0 should clear scaled nutrition (not allowed per setWeight logic)
        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    @Test
    fun `setWeight with negative value does not produce scaled nutrition`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:1", name = "Apple", source = FoodSource.USDA,
            kcalPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 14.0, fatPer100g = 0.2,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("-50")

        // Negative weight should be treated as invalid, scaled nutrition should be null
        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    @Test
    fun `setWeight with non-numeric string clears scaled nutrition`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:1", name = "Banana", source = FoodSource.USDA,
            kcalPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 22.8, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("150")
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        viewModel.setWeight("not-a-number")
        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    // ---- Recipe portion scaling end-to-end ----

    @Test
    fun `setWeight for recipe computes proportional nutrition based on recipe totals`() = runTest {
        val viewModel = createViewModel()
        val recipe = Recipe(
            id = 1, name = "Pasta Bolognese", totalWeightG = 600.0,
            totalKcal = 1200.0, totalProteinG = 60.0, totalCarbsG = 120.0, totalFatG = 40.0,
            createdAt = 0, updatedAt = 0,
        )
        viewModel.selectRecipe(recipe)
        viewModel.setWeight("300") // 50% of recipe

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        assertEquals(600.0, scaled!!.kcal, 0.001)
        assertEquals(30.0, scaled.proteinG, 0.001)
        assertEquals(60.0, scaled.carbsG, 0.001)
        assertEquals(20.0, scaled.fatG, 0.001)
    }

    // ---- Missing value lifecycle: validate → apply overrides → can save ----

    @Test
    fun `food with missing kcal cannot produce scaled nutrition until override applied`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "off:123", name = "Mystery Bar", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null, proteinPer100g = 8.0, carbsPer100g = 50.0, fatPer100g = 10.0,
            missingFields = setOf(NutritionField.KCAL),
        )
        val needsMissing = viewModel.selectFood(food)
        assertTrue(needsMissing)

        // Before override: setWeight cannot compute scaled nutrition (kcal is null)
        viewModel.setWeight("100")
        assertNull(viewModel.uiState.value.scaledNutrition)

        // Apply missing kcal override
        viewModel.applyMissingValues(kcal = 300.0, protein = null, carbs = null, fat = null)

        // After override: setWeight should now produce scaled nutrition
        viewModel.setWeight("100")
        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        assertEquals(300.0, scaled!!.kcal, 0.001)
        assertEquals(8.0, scaled.proteinG, 0.001)
    }

    // ---- saveEntry creates correct LogEntry ----

    @Test
    fun `saveEntry creates LogEntry with correctly scaled values from food`() = runTest {
        val entrySlot = slot<com.delve.hungrywalrus.domain.model.LogEntry>()
        coEvery { logRepo.addEntry(capture(entrySlot)) } returns Unit

        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:42", name = "Salmon", source = FoodSource.USDA,
            kcalPer100g = 208.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 13.0,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("150") // 150g of salmon

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() // EntrySaved event
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { logRepo.addEntry(any()) }
        val captured = entrySlot.captured
        assertEquals("Salmon", captured.foodName)
        assertEquals(312.0, captured.kcal, 0.001) // 208 * 1.5
        assertEquals(30.0, captured.proteinG, 0.001) // 20 * 1.5
        assertEquals(0.0, captured.carbsG, 0.001)
        assertEquals(19.5, captured.fatG, 0.001) // 13 * 1.5
    }

    @Test
    fun `saveEntry creates LogEntry with recipe-scaled values`() = runTest {
        val entrySlot = slot<com.delve.hungrywalrus.domain.model.LogEntry>()
        coEvery { logRepo.addEntry(capture(entrySlot)) } returns Unit

        val viewModel = createViewModel()
        val recipe = Recipe(
            id = 1, name = "Veggie Stir Fry", totalWeightG = 500.0,
            totalKcal = 750.0, totalProteinG = 30.0, totalCarbsG = 90.0, totalFatG = 25.0,
            createdAt = 0, updatedAt = 0,
        )
        viewModel.selectRecipe(recipe)
        viewModel.setWeight("250") // 50% of recipe

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() // EntrySaved event
            cancelAndIgnoreRemainingEvents()
        }

        val captured = entrySlot.captured
        assertEquals("Veggie Stir Fry", captured.foodName)
        assertEquals(375.0, captured.kcal, 0.001) // 750 * 0.5
        assertEquals(15.0, captured.proteinG, 0.001) // 30 * 0.5
        assertEquals(45.0, captured.carbsG, 0.001)
        assertEquals(12.5, captured.fatG, 0.001)
    }

    // ---- searchOff integration ----

    @Test
    fun `searchOff with results updates state to Results`() = runTest {
        val results = listOf(
            FoodSearchResult(
                id = "off:123", name = "Chocolate Bar", source = FoodSource.OPEN_FOOD_FACTS,
                kcalPer100g = 530.0, proteinPer100g = 6.0, carbsPer100g = 60.0, fatPer100g = 28.0,
                missingFields = emptySet(),
            ),
        )
        coEvery { foodLookupRepo.searchOpenFoodFacts("chocolate") } returns Result.success(results)

        val viewModel = createViewModel()
        viewModel.searchOff("chocolate")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SearchState.Results, viewModel.uiState.value.searchState)
        assertEquals(1, viewModel.uiState.value.searchResults.size)
        assertEquals("Chocolate Bar", viewModel.uiState.value.searchResults[0].name)
    }

    @Test
    fun `searchOff with no results sets NoResults state`() = runTest {
        coEvery { foodLookupRepo.searchOpenFoodFacts(any()) } returns Result.success(emptyList())

        val viewModel = createViewModel()
        viewModel.searchOff("zxzxzx")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SearchState.NoResults, viewModel.uiState.value.searchState)
    }

    // ---- Very large values ----

    @Test
    fun `setWeight with very large value does not crash and produces large scaled result`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "usda:1", name = "Pure Sugar", source = FoodSource.USDA,
            kcalPer100g = 400.0, proteinPer100g = 0.0, carbsPer100g = 100.0, fatPer100g = 0.0,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("100000") // 100kg — extreme but should not crash

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        assertEquals(400_000.0, scaled!!.kcal, 0.001)
        assertEquals(100_000.0, scaled.carbsG, 0.001)
    }
}
