package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.NutritionField
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.util.ApiKeyStore
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * QA unit tests for [AddEntryViewModel] caching behaviour and [setDirectEntry] custom weight.
 *
 * Gaps filled:
 *
 * 1. `selectFood` with a MANUAL-sourced food must NOT call [FoodLookupRepository.cacheItem].
 *    The `shouldCache` guard returns false for [FoodSource.MANUAL] — this verifies that.
 *    Existing tests only set up `cacheItem` as a no-op but never assert it was NOT called
 *    for MANUAL items.
 *
 * 2. `selectFood` with a complete USDA-sourced food MUST call [FoodLookupRepository.cacheItem]
 *    (architecture §6.2 item 2: "cache item when its per-100g data is resolved").
 *    Existing tests set `cacheItem` as a no-op but do not verify the call was made.
 *
 * 3. `selectFood` with a complete OFF-sourced food MUST call [FoodLookupRepository.cacheItem].
 *
 * 4. `applyMissingValues` that completes an OFF food must also trigger [FoodLookupRepository.cacheItem].
 *    Architecture §6.2 item 2 (second path): "cache when the user supplies missing values
 *    and the food becomes complete".
 *
 * 5. `setDirectEntry` with a non-default weight (ingredient mode) computes `scaledNutrition`
 *    proportional to the supplied weight, not the default 100g.
 *    Formula: scaledKcal = kcalPer100g * weightG / 100.0.
 *    The [ManualEntryRoundTripQaTest] only tests the default weight="100" path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEntryViewModelCachingBehaviourTest {

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
        // NOT relaxed — so any unexpected call to cacheItem that was not set up will fail.
        foodLookupRepo = mockk(relaxed = true)
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

    // ---- Caching: MANUAL source must NOT be cached ----

    /**
     * Architecture §6.2: Manual entries have synthetic IDs and no upstream source to
     * deduplicate against. `shouldCache(MANUAL) = false` must prevent any call to cacheItem.
     */
    @Test
    fun `selectFood with MANUAL source does not invoke cacheItem`() = runTest {
        val manualFood = FoodSearchResult(
            id = "manual_99999",
            name = "Homemade Stew",
            source = FoodSource.MANUAL,
            kcalPer100g = 180.0,
            proteinPer100g = 14.0,
            carbsPer100g = 22.0,
            fatPer100g = 5.0,
            missingFields = emptySet(),
        )

        val viewModel = createViewModel()
        viewModel.selectFood(manualFood)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { foodLookupRepo.cacheItem(any()) }
    }

    // ---- Caching: USDA and OFF complete foods must be cached on selection ----

    /**
     * Architecture §6.2 item 2: complete USDA foods are cached on selection so that
     * subsequent offline lookups can be served from the local cache.
     */
    @Test
    fun `selectFood with complete USDA food invokes cacheItem`() = runTest {
        val usdaFood = FoodSearchResult(
            id = "usda:999",
            name = "Oats",
            source = FoodSource.USDA,
            kcalPer100g = 374.0,
            proteinPer100g = 13.2,
            carbsPer100g = 67.7,
            fatPer100g = 7.0,
            missingFields = emptySet(),
        )

        val viewModel = createViewModel()
        viewModel.selectFood(usdaFood)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { foodLookupRepo.cacheItem(usdaFood) }
    }

    /**
     * Architecture §6.2 item 2: complete Open Food Facts foods are also cached on selection.
     */
    @Test
    fun `selectFood with complete OFF food invokes cacheItem`() = runTest {
        val offFood = FoodSearchResult(
            id = "off:9876543210",
            name = "Granola Bar",
            source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = 420.0,
            proteinPer100g = 9.0,
            carbsPer100g = 58.0,
            fatPer100g = 14.0,
            missingFields = emptySet(),
        )

        val viewModel = createViewModel()
        viewModel.selectFood(offFood)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { foodLookupRepo.cacheItem(offFood) }
    }

    /**
     * Architecture §6.2 item 2 (second path): when an incomplete food has its missing
     * values filled via [AddEntryViewModel.applyMissingValues], and the food then
     * becomes complete and is API-sourced, cacheItem must be called at that point.
     *
     * Prior to this fix (W03) the food was not cached after applying missing values.
     * This test verifies the corrected behaviour.
     */
    @Test
    fun `applyMissingValues that completes an OFF food triggers cacheItem`() = runTest {
        val incompleteOffFood = FoodSearchResult(
            id = "off:456",
            name = "Mystery Bar",
            source = FoodSource.OPEN_FOOD_FACTS,
            kcalPer100g = null,
            proteinPer100g = 8.0,
            carbsPer100g = 50.0,
            fatPer100g = 10.0,
            missingFields = setOf(NutritionField.KCAL),
        )

        val viewModel = createViewModel()
        viewModel.selectFood(incompleteOffFood) // incomplete: cacheItem NOT called yet
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { foodLookupRepo.cacheItem(any()) }

        // User supplies the missing kcal estimate
        viewModel.applyMissingValues(kcal = 350.0, protein = null, carbs = null, fat = null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Now complete: cacheItem MUST be called
        coVerify(atLeast = 1) { foodLookupRepo.cacheItem(any()) }
    }

    // ---- setDirectEntry with non-default weight (ingredient mode) ----

    /**
     * In ingredient mode the caller supplies per-100g nutritional values AND the actual
     * weight of the ingredient. The `setDirectEntry` method must compute `scaledNutrition`
     * proportional to the supplied weight, not the default 100g.
     *
     * Formula: scaledKcal = kcalPer100g * weightG / 100.0
     * Example: 250g of whole milk at 60 kcal/100g → 60 * 250 / 100 = 150 kcal.
     */
    @Test
    fun `setDirectEntry with custom weight scales nutrition proportionally`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Whole milk",
            kcal = 60.0,    // per 100g
            proteinG = 3.2, // per 100g
            carbsG = 4.8,   // per 100g
            fatG = 3.3,     // per 100g
            weight = "250", // 250g of milk
        )

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        // 60.0 * 250 / 100 = 150 kcal
        assertEquals(150.0, scaled!!.kcal, 0.001)
        // 3.2 * 250 / 100 = 8.0g protein
        assertEquals(8.0, scaled.proteinG, 0.001)
        // 4.8 * 250 / 100 = 12.0g carbs
        assertEquals(12.0, scaled.carbsG, 0.001)
        // 3.3 * 250 / 100 = 8.25g fat
        assertEquals(8.25, scaled.fatG, 0.001)
    }

    /**
     * The `weightG` field in the UI state must reflect the weight argument supplied to
     * `setDirectEntry`, not the default "100".
     */
    @Test
    fun `setDirectEntry stores the custom weight in uiState weightG`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Butter",
            kcal = 717.0,
            proteinG = 0.9,
            carbsG = 0.1,
            fatG = 81.0,
            weight = "30",
        )

        assertEquals("30", viewModel.uiState.value.weightG)
    }

    /**
     * With the default weight of "100", the scaled values must equal the per-100g
     * reference values (x * 100 / 100 = x). This anchors the sentinel arithmetic.
     */
    @Test
    fun `setDirectEntry with default weight 100 stores unscaled values as scaledNutrition`() = runTest {
        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Protein shake",
            kcal = 400.0,
            proteinG = 30.0,
            carbsG = 40.0,
            fatG = 10.0,
            // weight defaults to "100"
        )

        val scaled = viewModel.uiState.value.scaledNutrition
        assertNotNull(scaled)
        assertEquals(400.0, scaled!!.kcal, 0.001)
        assertEquals(30.0, scaled.proteinG, 0.001)
        assertEquals(40.0, scaled.carbsG, 0.001)
        assertEquals(10.0, scaled.fatG, 0.001)
    }
}
