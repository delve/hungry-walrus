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

        assertEquals(SearchState.Results, viewModel.uiState.value.searchState)
        assertEquals(1, viewModel.uiState.value.searchResults.size)
        assertEquals("Apple", viewModel.uiState.value.searchResults[0].name)
    }

    @Test
    fun `searchUsda with empty results sets NoResults state`() = runTest {
        coEvery { foodLookupRepo.searchUsda("xyz") } returns Result.success(emptyList())

        val viewModel = createViewModel()
        viewModel.searchUsda("xyz")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SearchState.NoResults, viewModel.uiState.value.searchState)
    }

    @Test
    fun `searchUsda with error sets Error state`() = runTest {
        coEvery { foodLookupRepo.searchUsda("test") } returns Result.failure(Exception("API error"))

        val viewModel = createViewModel()
        viewModel.searchUsda("test")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SearchState.Error, viewModel.uiState.value.searchState)
        assertTrue(viewModel.uiState.value.searchErrorMessage.isNotEmpty())
    }

    @Test
    fun `selectRecipe stores recipe and sets recipe source`() = runTest {
        val viewModel = createViewModel()
        val recipe = Recipe(
            id = 1, name = "Stir Fry", totalWeightG = 400.0,
            totalKcal = 800.0, totalProteinG = 60.0, totalCarbsG = 80.0, totalFatG = 20.0,
            createdAt = 0, updatedAt = 0,
        )

        viewModel.selectRecipe(recipe)

        assertEquals(recipe, viewModel.uiState.value.selectedRecipe)
        assertTrue(viewModel.uiState.value.isRecipeSource)
        assertEquals("Stir Fry", viewModel.uiState.value.foodName)
    }

    @Test
    fun `setWeight computes scaled nutrition for recipe`() = runTest {
        val viewModel = createViewModel()
        val recipe = Recipe(
            id = 1, name = "Stir Fry", totalWeightG = 400.0,
            totalKcal = 800.0, totalProteinG = 60.0, totalCarbsG = 80.0, totalFatG = 20.0,
            createdAt = 0, updatedAt = 0,
        )
        viewModel.selectRecipe(recipe)
        viewModel.setWeight("200")

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        // 200g is half of 400g total, so half the values
        assertEquals(400.0, scaled!!.kcal, 0.001)
        assertEquals(30.0, scaled.proteinG, 0.001)
        assertEquals(40.0, scaled.carbsG, 0.001)
        assertEquals(10.0, scaled.fatG, 0.001)
    }

    @Test
    fun `applyMissingValues updates selected food`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Test", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null, proteinPer100g = 10.0, carbsPer100g = 20.0, fatPer100g = null,
            missingFields = setOf(NutritionField.KCAL, NutritionField.FAT),
        )
        viewModel.selectFood(food)

        viewModel.applyMissingValues(kcal = 150.0, protein = null, carbs = null, fat = 5.0)

        val updated = viewModel.uiState.value.selectedFood!!
        assertEquals(150.0, updated.kcalPer100g!!, 0.001)
        assertEquals(5.0, updated.fatPer100g!!, 0.001)
        assertTrue(updated.missingFields.isEmpty())
    }

    @Test
    fun `ingredientMode defaults to false`() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.ingredientMode)
    }

    @Test
    fun `setIngredientMode updates ingredientMode in uiState`() = runTest {
        val viewModel = createViewModel()
        viewModel.setIngredientMode(true)
        assertTrue(viewModel.uiState.value.ingredientMode)
        viewModel.setIngredientMode(false)
        assertFalse(viewModel.uiState.value.ingredientMode)
    }

    @Test
    fun `getIngredientData returns null when no food selected`() = runTest {
        val viewModel = createViewModel()
        assertNull(viewModel.getIngredientData())
    }

    @Test
    fun `getIngredientData returns ingredient data for selected food with weight`() = runTest {
        val viewModel = createViewModel()
        val food = FoodSearchResult(
            id = "1", name = "Oats", source = FoodSource.MANUAL,
            kcalPer100g = 360.0, proteinPer100g = 13.0, carbsPer100g = 60.0, fatPer100g = 7.0,
            missingFields = emptySet(),
        )
        viewModel.selectFood(food)
        viewModel.setWeight("150")

        val result = viewModel.getIngredientData()

        assertNotNull(result)
        assertEquals("Oats", result!!.name)
        assertEquals(150.0, result.weightG, 0.001)
        assertEquals(360.0, result.kcalPer100g, 0.001)
        assertEquals(13.0, result.proteinPer100g, 0.001)
    }

    @Test
    fun `getIngredientData converts recipe to per-100g equivalent`() = runTest {
        val viewModel = createViewModel()
        val recipe = Recipe(
            id = 1, name = "Stew", totalWeightG = 500.0,
            totalKcal = 750.0, totalProteinG = 40.0, totalCarbsG = 60.0, totalFatG = 25.0,
            createdAt = 0, updatedAt = 0,
        )
        viewModel.selectRecipe(recipe)
        viewModel.setWeight("250")

        val result = viewModel.getIngredientData()

        assertNotNull(result)
        // 750 kcal / 500g * 100 = 150 kcal/100g
        assertEquals(150.0, result!!.kcalPer100g, 0.001)
        assertEquals(250.0, result.weightG, 0.001)
    }

    @Test
    fun `saveApiKey stores key and updates hasUsdaKey state`() = runTest {
        every { apiKeyStore.saveApiKey(any()) } just Runs
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.hasUsdaKey) // false from setup mock

        every { apiKeyStore.hasApiKey() } returns true
        viewModel.saveApiKey("test-key")

        assertTrue(viewModel.uiState.value.hasUsdaKey)
        verify { apiKeyStore.saveApiKey("test-key") }
    }

    @Test
    fun `saveApiKey ignores blank key`() = runTest {
        val viewModel = createViewModel()
        viewModel.saveApiKey("   ")
        // saveApiKey with blank should not call apiKeyStore.saveApiKey
        verify(exactly = 0) { apiKeyStore.saveApiKey(any()) }
        assertFalse(viewModel.uiState.value.hasUsdaKey)
    }

    @Test
    fun `lookupBarcode emits BarcodeResult found=true isError=false on successful lookup`() = runTest {
        val food = FoodSearchResult(
            id = "123", name = "Oat Biscuits", source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = 450.0, proteinPer100g = 8.0, carbsPer100g = 60.0, fatPer100g = 18.0,
            missingFields = emptySet(),
        )
        coEvery { foodLookupRepo.lookupBarcode("1234567890") } returns Result.success(food)

        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.lookupBarcode("1234567890")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as AddEntryUiEvent.BarcodeResult
            assertTrue(event.found)
            assertFalse(event.isError)
            assertEquals("1234567890", event.barcode)
            assertEquals(SearchState.Idle, viewModel.uiState.value.searchState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lookupBarcode emits BarcodeResult found=false isError=false when product not in database`() = runTest {
        coEvery { foodLookupRepo.lookupBarcode("0000000000") } returns Result.success(null)

        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.lookupBarcode("0000000000")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as AddEntryUiEvent.BarcodeResult
            assertFalse(event.found)
            assertFalse(event.isError)
            assertEquals("0000000000", event.barcode)
            assertEquals(SearchState.Idle, viewModel.uiState.value.searchState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lookupBarcode emits BarcodeResult found=false isError=true and sets Error state on network failure`() = runTest {
        coEvery { foodLookupRepo.lookupBarcode("9999999999") } returns Result.failure(Exception("Network error"))

        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.lookupBarcode("9999999999")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem() as AddEntryUiEvent.BarcodeResult
            assertFalse(event.found)
            assertTrue(event.isError)
            assertEquals("9999999999", event.barcode)
            assertEquals(SearchState.Error, viewModel.uiState.value.searchState)
            assertTrue(viewModel.uiState.value.searchErrorMessage.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events Channel delivers EntrySaved to exactly one collector`() = runTest {
        val food = FoodSearchResult(
            id = "1", name = "Apple", source = FoodSource.MANUAL,
            kcalPer100g = 52.0, proteinPer100g = 0.3, carbsPer100g = 14.0, fatPer100g = 0.2,
            missingFields = emptySet(),
        )
        val viewModel = createViewModel()
        viewModel.selectFood(food)
        viewModel.setWeight("100")

        val received1 = mutableListOf<AddEntryUiEvent>()
        val received2 = mutableListOf<AddEntryUiEvent>()

        val job1 = launch { viewModel.events.collect { received1.add(it) } }
        val job2 = launch { viewModel.events.collect { received2.add(it) } }

        viewModel.saveEntry()
        testDispatcher.scheduler.advanceUntilIdle()

        job1.cancel()
        job2.cancel()

        // Channel delivers to exactly one collector — total across both must be 1
        val total = received1.count { it is AddEntryUiEvent.EntrySaved } +
            received2.count { it is AddEntryUiEvent.EntrySaved }
        assertEquals(1, total)
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
}
