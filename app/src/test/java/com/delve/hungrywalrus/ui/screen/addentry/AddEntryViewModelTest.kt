package com.delve.hungrywalrus.ui.screen.addentry

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionField
import com.delve.hungrywalrus.domain.model.NutritionValues
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logRepo: LogEntryRepository
    private lateinit var foodLookupRepo: FoodLookupRepository
    private lateinit var recipeRepo: RecipeRepository
    private lateinit var scaleUseCase: ScaleNutritionUseCase
    private lateinit var validateUseCase: ValidateFoodDataUseCase
    private lateinit var apiKeyStore: ApiKeyStore

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logRepo = mockk(relaxed = true)
        foodLookupRepo = mockk()
        recipeRepo = mockk()
        scaleUseCase = ScaleNutritionUseCase()
        validateUseCase = ValidateFoodDataUseCase()
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

    @Test
    fun `initial state has empty search and no USDA key`() = runTest {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(SearchState.Idle, state.searchState)
        assertFalse(state.hasUsdaKey)
        assertNull(state.selectedFood)
    }

    @Test
    fun `selectFood stores food and returns false when complete`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Chicken", source = FoodSource.USDA,
            kcalPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6,
            missingFields = emptySet(),
        )

        val needsMissing = viewModel.selectFood(food)

        assertFalse(needsMissing)
        assertEquals(food, viewModel.uiState.value.selectedFood)
        assertEquals("Chicken", viewModel.uiState.value.foodName)
    }

    @Test
    fun `selectFood returns true when food has missing values`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Unknown Food", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null, proteinPer100g = 10.0, carbsPer100g = 20.0, fatPer100g = 5.0,
            missingFields = setOf(NutritionField.KCAL),
        )

        val needsMissing = viewModel.selectFood(food)

        assertTrue(needsMissing)
    }

    @Test
    fun `setWeight computes scaled nutrition for food`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Rice", source = FoodSource.MANUAL,
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
    fun `setWeight with invalid value clears scaled nutrition`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Rice", source = FoodSource.MANUAL,
            kcalPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("200") // sets scaled
        viewModel.setWeight("abc") // invalid

        assertNull(viewModel.uiState.value.scaledNutrition)
    }

    @Test
    fun `saveEntry creates log entry and emits event`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Banana", source = FoodSource.MANUAL,
            kcalPer100g = 89.0, proteinPer100g = 1.1, carbsPer100g = 22.8, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("100")

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEntryUiEvent.EntrySaved)
            coVerify { logRepo.addEntry(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchUsda updates state with results`() = runTest {
        val results = listOf(
            FoodSearchResult(
                id = "1", name = "Apple", source = FoodSource.USDA,
                kcalPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 14.0, fatPer100g = 0.2,
                missingFields = emptySet(),
            ),
        )
        coEvery { foodLookupRepo.searchUsda("apple") } returns Result.success(results)

        val viewModel = createViewModel()
        viewModel.searchUsda("apple")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Results, state.searchState)
        assertEquals(1, state.searchResults.size)
        assertEquals("Apple", state.searchResults[0].name)
    }

    @Test
    fun `searchUsda with blank query is ignored`() = runTest {
        val viewModel = createViewModel()
        viewModel.searchUsda("")

        assertEquals(SearchState.Idle, viewModel.uiState.value.searchState)
    }

    @Test
    fun `searchUsda failure sets error state`() = runTest {
        coEvery { foodLookupRepo.searchUsda("test") } returns Result.failure(RuntimeException("Network error"))

        val viewModel = createViewModel()
        viewModel.searchUsda("test")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Error, state.searchState)
        assertEquals("Network error", state.searchErrorMessage)
    }

    @Test
    fun `searchOff updates state with results`() = runTest {
        val results = listOf(
            FoodSearchResult(
                id = "off1", name = "Nutella", source = FoodSource.OPEN_FOOD_FACTS,
                kcalPer100g = 539.0, proteinPer100g = 6.3, carbsPer100g = 57.5, fatPer100g = 30.9,
                missingFields = emptySet(),
            ),
        )
        coEvery { foodLookupRepo.searchOpenFoodFacts("nutella") } returns Result.success(results)

        val viewModel = createViewModel()
        viewModel.searchOff("nutella")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Results, state.searchState)
        assertEquals("Nutella", state.searchResults[0].name)
    }

    @Test
    fun `searchOff with no results sets NoResults`() = runTest {
        coEvery { foodLookupRepo.searchOpenFoodFacts("xyz") } returns Result.success(emptyList())

        val viewModel = createViewModel()
        viewModel.searchOff("xyz")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SearchState.NoResults, viewModel.uiState.value.searchState)
    }

    @Test
    fun `updateSearchQuery updates the query string`() = runTest {
        val viewModel = createViewModel()
        viewModel.updateSearchQuery("chicken")
        assertEquals("chicken", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `lookupBarcode emits BarcodeResult found on success`() = runTest {
        val food = FoodSearchResult(
            id = "off1", name = "Cola", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = 42.0, proteinPer100g = 0.0, carbsPer100g = 10.6, fatPer100g = 0.0,
            missingFields = emptySet(),
        )
        coEvery { foodLookupRepo.lookupBarcode("5449000000996") } returns Result.success(food)

        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.lookupBarcode("5449000000996")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEntryUiEvent.BarcodeResult)
            assertTrue((event as AddEntryUiEvent.BarcodeResult).found)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lookupBarcode emits BarcodeResult not found when null`() = runTest {
        coEvery { foodLookupRepo.lookupBarcode("0000000000000") } returns Result.success(null)

        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.lookupBarcode("0000000000000")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEntryUiEvent.BarcodeResult)
            assertFalse((event as AddEntryUiEvent.BarcodeResult).found)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lookupBarcode emits BarcodeResult with isError on failure`() = runTest {
        coEvery { foodLookupRepo.lookupBarcode("fail") } returns Result.failure(RuntimeException("Timeout"))

        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.lookupBarcode("fail")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEntryUiEvent.BarcodeResult)
            assertTrue((event as AddEntryUiEvent.BarcodeResult).isError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectRecipe sets recipe source state`() = runTest {
        val recipe = Recipe(
            id = 1, name = "My Pasta", totalWeightG = 500.0,
            totalKcal = 600.0, totalProteinG = 20.0, totalCarbsG = 80.0, totalFatG = 15.0,
            createdAt = 0, updatedAt = 0,
        )
        val viewModel = createViewModel()
        viewModel.selectRecipe(recipe)

        val state = viewModel.uiState.value
        assertTrue(state.isRecipeSource)
        assertEquals(recipe, state.selectedRecipe)
        assertEquals("My Pasta", state.foodName)
    }

    @Test
    fun `setWeight scales recipe portion correctly`() = runTest {
        val recipe = Recipe(
            id = 1, name = "My Pasta", totalWeightG = 500.0,
            totalKcal = 600.0, totalProteinG = 20.0, totalCarbsG = 80.0, totalFatG = 15.0,
            createdAt = 0, updatedAt = 0,
        )
        val viewModel = createViewModel()
        viewModel.selectRecipe(recipe)
        viewModel.setWeight("250")

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        // 250/500 = 0.5 portion
        assertEquals(300.0, scaled!!.kcal, 0.001)
        assertEquals(10.0, scaled.proteinG, 0.001)
    }

    @Test
    fun `applyMissingValues updates food with overrides`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Food", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null, proteinPer100g = 10.0, carbsPer100g = 20.0, fatPer100g = 5.0,
            missingFields = setOf(NutritionField.KCAL),
        )
        viewModel.selectFood(food)

        viewModel.applyMissingValues(kcal = 200.0, protein = null, carbs = null, fat = null)

        val updated = viewModel.uiState.value.selectedFood
        assertNotNull(updated)
        assertEquals(200.0, updated!!.kcalPer100g!!, 0.001)
    }

    @Test
    fun `getIngredientData returns null when no food selected`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.getIngredientData())
    }

    @Test
    fun `setIngredientMode updates state`() = runTest {
        val viewModel = createViewModel()
        viewModel.setIngredientMode(true)
        assertTrue(viewModel.uiState.value.ingredientMode)
        viewModel.setIngredientMode(false)
        assertFalse(viewModel.uiState.value.ingredientMode)
    }

    @Test
    fun `saveApiKey stores key and updates hasUsdaKey state`() = runTest {
        every { apiKeyStore.saveApiKey(any()) } just Runs
        every { apiKeyStore.hasApiKey() } returns true
        val viewModel = createViewModel()

        viewModel.saveApiKey("test-key-123")

        verify { apiKeyStore.saveApiKey("test-key-123") }
        assertTrue(viewModel.uiState.value.hasUsdaKey)
    }

    @Test
    fun `saveApiKey ignores blank key`() = runTest {
        val viewModel = createViewModel()
        viewModel.saveApiKey("   ")
        verify(exactly = 0) { apiKeyStore.saveApiKey(any()) }
    }

    @Test
    fun `events Channel delivers EntrySaved to exactly one collector`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Test", source = FoodSource.MANUAL,
            kcalPer100g = 100.0, proteinPer100g = 5.0, carbsPer100g = 10.0, fatPer100g = 2.0,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("100")

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event is AddEntryUiEvent.EntrySaved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDirectEntry stores nutrition values without weight step`() = runTest {
        val viewModel = createViewModel()

        viewModel.setDirectEntry(
            name = "Porridge",
            kcal = 320.0,
            proteinG = 10.0,
            carbsG = 58.0,
            fatG = 5.0,
        )

        val state = viewModel.uiState.value
        assertEquals("Porridge", state.foodName)
        assertNotNull(state.scaledNutrition)
        assertEquals(320.0, state.scaledNutrition!!.kcal, 0.001)
        assertEquals(10.0, state.scaledNutrition!!.proteinG, 0.001)
        assertEquals(58.0, state.scaledNutrition!!.carbsG, 0.001)
        assertEquals(5.0, state.scaledNutrition!!.fatG, 0.001)
        // default weight "100" means scaling is identity (x * 100/100 = x)
        assertEquals("100", state.weightG)
    }

    @Test
    fun `setDirectEntry with explicit weight sets weightG and scales nutrition`() = runTest {
        val viewModel = createViewModel()

        viewModel.setDirectEntry(
            name = "Oats",
            kcal = 380.0,
            proteinG = 13.0,
            carbsG = 67.0,
            fatG = 7.0,
            weight = "50",
        )

        val state = viewModel.uiState.value
        assertEquals("50", state.weightG)
        assertNotNull(state.scaledNutrition)
        // 380 * 50 / 100 = 190
        assertEquals(190.0, state.scaledNutrition!!.kcal, 0.001)
        assertEquals(6.5, state.scaledNutrition!!.proteinG, 0.001)
        assertEquals(33.5, state.scaledNutrition!!.carbsG, 0.001)
        assertEquals(3.5, state.scaledNutrition!!.fatG, 0.001)
    }

    @Test
    fun `setDirectEntry in ingredient mode with weight - getIngredientData uses supplied weight`() = runTest {
        val viewModel = createViewModel()

        viewModel.setDirectEntry(
            name = "Butter",
            kcal = 717.0,
            proteinG = 0.9,
            carbsG = 0.1,
            fatG = 81.0,
            weight = "30",
        )

        val data = viewModel.getIngredientData()

        assertNotNull(data)
        assertEquals("Butter", data!!.name)
        assertEquals(30.0, data.weightG, 0.001)
        // per-100g values are the entered values
        assertEquals(717.0, data.kcalPer100g, 0.001)
        assertEquals(81.0, data.fatPer100g, 0.001)
    }

    @Test
    fun `setDirectEntry allows saveEntry immediately without weight step`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Cheese",
            kcal = 400.0,
            proteinG = 25.0,
            carbsG = 0.5,
            fatG = 33.0,
        )

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is AddEntryUiEvent.EntrySaved)
            coVerify { logRepo.addEntry(any()) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setDirectEntry getIngredientData returns 100g ingredient for ingredient mode`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Butter",
            kcal = 717.0,
            proteinG = 0.9,
            carbsG = 0.1,
            fatG = 81.0,
        )

        val data = viewModel.getIngredientData()

        assertNotNull(data)
        assertEquals("Butter", data!!.name)
        // Weight sentinel is 100g; per-100g values equal entered values
        assertEquals(100.0, data.weightG, 0.001)
        assertEquals(717.0, data.kcalPer100g, 0.001)
        assertEquals(81.0, data.fatPer100g, 0.001)
    }

    @Test
    fun `recipes initial state is Loading before repository emits`() = runTest {
        // Use a flow that never emits to keep the Loading state
        every { recipeRepo.getAllRecipes() } returns kotlinx.coroutines.flow.flow { }

        val viewModel = createViewModel()

        assertTrue(viewModel.recipes.value is RecipesState.Loading)
    }

    @Test
    fun `recipes transitions to Loaded after repository emits`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Pasta", totalWeightG = 400.0,
            totalKcal = 800.0, totalProteinG = 40.0, totalCarbsG = 100.0, totalFatG = 20.0,
            createdAt = 0, updatedAt = 0,
        )
        every { recipeRepo.getAllRecipes() } returns flowOf(listOf(recipe))

        val viewModel = createViewModel()
        // WhileSubscribed only starts upstream when there is a collector; subscribe first
        val job = launch { viewModel.recipes.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.recipes.value
        assertTrue(state is RecipesState.Loaded)
        assertEquals(1, (state as RecipesState.Loaded).recipes.size)
        assertEquals("Pasta", state.recipes[0].name)
        job.cancel()
    }

    @Test
    fun `resetState clears all state and resets ingredientMode`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Test", source = FoodSource.MANUAL,
            kcalPer100g = 100.0, proteinPer100g = 5.0, carbsPer100g = 10.0, fatPer100g = 2.0,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("100")
        viewModel.setIngredientMode(true)

        viewModel.resetState()

        val state = viewModel.uiState.value
        assertNull(state.selectedFood)
        assertEquals("", state.weightG)
        assertNull(state.scaledNutrition)
        assertFalse(state.ingredientMode)
    }

    // W07: Tests for decimal weight handling in setWeight (used by +/- buttons after roundToInt)

    @Test
    fun `setWeight with decimal string computes correct scaled nutrition`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Rice", source = FoodSource.MANUAL,
            kcalPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)

        viewModel.setWeight("100.5")

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        // 130 * 100.5 / 100 = 130.65
        assertEquals(130.65, scaled!!.kcal, 0.001)
        assertEquals("100.5", viewModel.uiState.value.weightG)
    }

    @Test
    fun `setWeight with rounded integer after decimal preserves scaled nutrition`() = runTest {
        // Simulates the W07 fix: user types "100.5", presses +, which rounds to 101 and adds 1 -> "102"
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Rice", source = FoodSource.MANUAL,
            kcalPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)

        // User types a decimal value
        viewModel.setWeight("100.5")
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        // Simulate the +/- button logic: toDoubleOrNull()?.roundToInt() then +1
        // Kotlin roundToInt() uses Math.round() which rounds 0.5 up, so 100.5 -> 101
        val current = "100.5".toDoubleOrNull()?.roundToInt() ?: 0
        assertEquals(101, current)
        viewModel.setWeight((current + 1).toString())

        val state = viewModel.uiState.value
        assertEquals("102", state.weightG)
        val scaled = state.scaledNutrition
        assertNotNull(scaled)
        // 130 * 102 / 100 = 132.6
        assertEquals(132.6, scaled!!.kcal, 0.001)
    }

    @Test
    fun `setWeight with rounded integer from decimal decrement works correctly`() = runTest {
        // Simulates the W07 fix: user types "150.7", presses -, rounds to 151, subtracts 1 -> "150"
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Rice", source = FoodSource.MANUAL,
            kcalPer100g = 130.0, proteinPer100g = 2.7, carbsPer100g = 28.0, fatPer100g = 0.3,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)

        // User types a decimal value
        viewModel.setWeight("150.7")
        assertNotNull(viewModel.uiState.value.scaledNutrition)

        // Simulate the - button logic: toDoubleOrNull()?.roundToInt() then -1
        val current = "150.7".toDoubleOrNull()?.roundToInt() ?: 0
        assertEquals(151, current) // 150.7 rounds to 151
        assertTrue(current > 1) // guard check
        viewModel.setWeight((current - 1).toString())

        val state = viewModel.uiState.value
        assertEquals("150", state.weightG)
        val scaled = state.scaledNutrition
        assertNotNull(scaled)
        // 130 * 150 / 100 = 195
        assertEquals(195.0, scaled!!.kcal, 0.001)
    }
}
