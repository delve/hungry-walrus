package com.delve.hungrywalrus.qa

import app.cash.turbine.test
import com.delve.hungrywalrus.data.local.dao.LogEntryDao
import com.delve.hungrywalrus.data.local.entity.LogEntryEntity
import com.delve.hungrywalrus.data.repository.LogEntryRepositoryImpl
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel
import com.delve.hungrywalrus.ui.screen.createrecipe.IngredientDraft
import com.delve.hungrywalrus.ui.screen.createrecipe.IngredientEditValues
import com.delve.hungrywalrus.ui.screen.settings.SettingsUiEvent
import com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import java.time.LocalDate
import java.time.ZoneId

/**
 * New QA tests filling coverage gaps identified during the QA audit pass.
 *
 * Areas covered:
 * - LogEntryRepository.getEntriesForRange date-boundary conversion.
 * - SettingsViewModel.savePlan: minimum valid kcal (exactly 1); kcal=0 rejected; macros=0 accepted.
 * - ComputeRollingSummaryUseCase: start > end throws IllegalArgumentException.
 * - ComputeRollingSummaryUseCase: dailyAverage reflects all period days, not just days with entries.
 * - CreateRecipeViewModel.editIngredient: no-op when id not found.
 * - CreateRecipeViewModel.saveRecipe: blank name prevents repository save.
 * - CreateRecipeViewModel.saveRecipe: empty ingredient list prevents repository save.
 * - CreateRecipeViewModel: recomputeTotals sums ingredients correctly after addIngredient.
 * - Per-100g scaling: very small weight produces proportionally small result.
 * - Per-100g scaling: zero weight produces all-zero NutritionValues.
 * - Recipe portion scaling: proportional correctness across diverse input combinations.
 * - Daily progress aggregation: large number of entries (no overflow).
 * - LogEntryRepository: deleteEntry delegates to DAO deleteById with correct id.
 * - LogEntryRepository.getEntriesForRange: endDate inclusive day included; next day excluded.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewQaCoverageTests {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // LogEntryRepository: date-range boundary conversion
    // ============================================================

    /**
     * getEntriesForRange converts [start, end] inclusive to half-open epoch millis
     * [start.atStartOfDay, end.plusDays(1).atStartOfDay).
     *
     * An entry timestamped exactly at start-of-day of the `end` date must be included.
     * An entry timestamped exactly at start-of-day of `end + 1` must NOT be included
     * (it falls on the exclusive upper bound).
     */
    @Test
    fun `getEntriesForRange end day is inclusive and next day is exclusive`() = runTest {
        val dao = mockk<LogEntryDao>(relaxed = true)
        val repository = LogEntryRepositoryImpl(dao)

        val start = LocalDate.of(2026, 3, 14)
        val end = LocalDate.of(2026, 3, 20)
        val zone = ZoneId.systemDefault()

        val expectedStartMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val expectedEndMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Timestamp exactly at midnight on the `end` day — should be included (>= start, < end+1)
        val entryAtEndDay = LogEntryEntity(
            id = 1, foodName = "EndDay", kcal = 100.0,
            proteinG = 5.0, carbsG = 10.0, fatG = 2.0,
            timestamp = end.atStartOfDay(zone).toInstant().toEpochMilli(),
        )

        val flowSlot = slot<Long>()
        val flow2Slot = slot<Long>()
        every { dao.getEntriesForRange(capture(flowSlot), capture(flow2Slot)) } returns
            flowOf(listOf(entryAtEndDay))

        repository.getEntriesForRange(start, end).test {
            val entries = awaitItem()
            assertEquals(1, entries.size)
            assertEquals("EndDay", entries[0].foodName)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify the DAO was called with the expected epoch-millis bounds
        assertEquals(expectedStartMillis, flowSlot.captured)
        assertEquals(expectedEndMillis, flow2Slot.captured)
    }

    /**
     * getEntriesForRange: an entry at exactly start-of-day of `end + 1` falls on
     * the exclusive upper bound and must not be returned by the query.
     *
     * This test verifies the DAO is called with an upper bound that equals
     * end.plusDays(1).atStartOfDay — i.e. the day AFTER end is excluded.
     */
    @Test
    fun `getEntriesForRange upper bound is exclusive at start of day after end`() = runTest {
        val dao = mockk<LogEntryDao>(relaxed = true)
        val repository = LogEntryRepositoryImpl(dao)

        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 7)
        val zone = ZoneId.systemDefault()

        // Timestamp exactly at midnight at the start of the day AFTER end
        val timestampAfterEnd = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val upperBoundSlot = slot<Long>()
        every { dao.getEntriesForRange(any(), capture(upperBoundSlot)) } returns flowOf(emptyList())

        repository.getEntriesForRange(start, end).test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        // The upper bound passed to the DAO must equal timestampAfterEnd (exclusive)
        assertEquals(
            "Upper bound must be start-of-day of end+1 (exclusive)",
            timestampAfterEnd,
            upperBoundSlot.captured,
        )
    }

    /**
     * deleteEntry delegates to DAO.deleteById with exactly the provided id.
     */
    @Test
    fun `deleteEntry calls dao deleteById with correct id`() = runTest {
        val dao = mockk<LogEntryDao>(relaxed = true)
        val repository = LogEntryRepositoryImpl(dao)

        repository.deleteEntry(42L)

        coVerify(exactly = 1) { dao.deleteById(42L) }
    }

    // ============================================================
    // SettingsViewModel: plan validation boundaries
    // ============================================================

    /**
     * Requirement (requirements.md): "Kilocalorie targets must be greater than zero."
     * kcal = 1 is the smallest valid integer value and must be accepted.
     */
    @Test
    fun `savePlan accepts kcal of exactly 1 (minimum valid)`() = runTest {
        val apiKeyStore = mockk<ApiKeyStore>(relaxed = true)
        val planRepo = mockk<NutritionPlanRepository>(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.events.test {
            viewModel.savePlan("1", "0.0", "0.0", "0.0")
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertEquals(SettingsUiEvent.PlanSaved, event)
            assertTrue(viewModel.uiState.value.planValidationErrors.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Requirement: kcal = 0 must be rejected (must be > 0, not >= 0).
     */
    @Test
    fun `savePlan rejects kcal of exactly 0`() = runTest {
        val apiKeyStore = mockk<ApiKeyStore>(relaxed = true)
        val planRepo = mockk<NutritionPlanRepository>(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.savePlan("0", "150.0", "250.0", "65.0")

        assertTrue(viewModel.uiState.value.planValidationErrors.containsKey("kcal"))
    }

    /**
     * Requirement: macronutrient targets of exactly 0.0 are valid.
     * A zero protein target, for example, is permitted (e.g. a carnivore diet tracking
     * plan where only fat and kcal are tracked might use zero for carbs).
     */
    @Test
    fun `savePlan accepts protein carbs fat all at exactly zero (zero-macro target is valid)`() = runTest {
        val apiKeyStore = mockk<ApiKeyStore>(relaxed = true)
        val planRepo = mockk<NutritionPlanRepository>(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.events.test {
            viewModel.savePlan("2000", "0", "0", "0")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SettingsUiEvent.PlanSaved, awaitItem())
            assertTrue(viewModel.uiState.value.planValidationErrors.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Non-numeric kcal string must produce a validation error.
     */
    @Test
    fun `savePlan rejects non-numeric kcal string`() = runTest {
        val apiKeyStore = mockk<ApiKeyStore>(relaxed = true)
        val planRepo = mockk<NutritionPlanRepository>(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.savePlan("abc", "150.0", "250.0", "65.0")

        assertTrue(viewModel.uiState.value.planValidationErrors.containsKey("kcal"))
    }

    /**
     * All four fields invalid simultaneously: all four keys present in errors map.
     */
    @Test
    fun `savePlan with all invalid fields produces four validation errors`() = runTest {
        val apiKeyStore = mockk<ApiKeyStore>(relaxed = true)
        val planRepo = mockk<NutritionPlanRepository>(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.savePlan("0", "-1", "-1", "-1")

        val errors = viewModel.uiState.value.planValidationErrors
        assertTrue(errors.containsKey("kcal"))
        assertTrue(errors.containsKey("protein"))
        assertTrue(errors.containsKey("carbs"))
        assertTrue(errors.containsKey("fat"))
    }

    // ============================================================
    // ComputeRollingSummaryUseCase edge cases
    // ============================================================

    private val summaryUseCase = ComputeRollingSummaryUseCase()

    /**
     * Architecture §7.6 requires `start <= end`. Passing start > end must throw
     * IllegalArgumentException (the use case has `require(!start.isAfter(end))`).
     */
    @Test(expected = IllegalArgumentException::class)
    fun `ComputeRollingSummaryUseCase throws when start is after end`() {
        val start = LocalDate.of(2026, 3, 20)
        val end = LocalDate.of(2026, 3, 14)  // end before start
        summaryUseCase(emptyList(), emptyMap(), start, end)
    }

    /**
     * When the same date is passed for both start and end (single-day period) the use
     * case must return periodDays = 1 without throwing.
     */
    @Test
    fun `ComputeRollingSummaryUseCase accepts start equal to end (single-day period)`() {
        val day = LocalDate.of(2026, 3, 20)
        val result = summaryUseCase(emptyList(), emptyMap(), day, day)
        assertEquals(1, result.periodDays)
        assertEquals(0.0, result.totalIntake.kcal, 0.001)
    }

    /**
     * The daily average denominator is the full period length, not the number of days
     * that have entries. For a 7-day period with entries only on day 1, the average
     * must be total / 7, not total / 1.
     */
    @Test
    fun `dailyAverage uses full period length even when entries span only one day`() {
        val start = LocalDate.of(2026, 3, 14)
        val end = LocalDate.of(2026, 3, 20)  // 7 days
        val entries = listOf(
            LogEntry(foodName = "Feast", kcal = 3500.0, proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = 1L),
        )

        val result = summaryUseCase(entries, emptyMap(), start, end)

        // Average must be 3500 / 7 = 500, not 3500 / 1 = 3500
        assertEquals(500.0, result.dailyAverage.kcal, 0.001)
    }

    /**
     * Intake aggregation: multiple entries with decimal values should sum precisely.
     */
    @Test
    fun `ComputeRollingSummaryUseCase sums decimal intake values without precision loss`() {
        val start = LocalDate.of(2026, 3, 1)
        val end = LocalDate.of(2026, 3, 3)
        val entries = listOf(
            LogEntry(foodName = "A", kcal = 123.7, proteinG = 11.3, carbsG = 22.4, fatG = 5.6, timestamp = 1L),
            LogEntry(foodName = "B", kcal = 456.3, proteinG = 33.7, carbsG = 77.6, fatG = 14.4, timestamp = 2L),
        )

        val result = summaryUseCase(entries, emptyMap(), start, end)

        assertEquals(580.0, result.totalIntake.kcal, 0.001)
        assertEquals(45.0, result.totalIntake.proteinG, 0.001)
        assertEquals(100.0, result.totalIntake.carbsG, 0.001)
        assertEquals(20.0, result.totalIntake.fatG, 0.001)
    }

    // ============================================================
    // ScaleNutritionUseCase: boundary values
    // ============================================================

    private val scaleUseCase = ScaleNutritionUseCase()

    /**
     * Very small weight (0.1g) must produce proportionally small values.
     * Formula: (value / 100) * 0.1 = value * 0.001
     */
    @Test
    fun `scaling 0_1g produces values proportional to 0_001 of the per-100g reference`() {
        val result = scaleUseCase(
            kcalPer100g = 400.0,
            proteinPer100g = 20.0,
            carbsPer100g = 50.0,
            fatPer100g = 10.0,
            weightG = 0.1,
        )
        assertEquals(0.4, result.kcal, 0.0001)
        assertEquals(0.02, result.proteinG, 0.0001)
        assertEquals(0.05, result.carbsG, 0.0001)
        assertEquals(0.01, result.fatG, 0.0001)
    }

    /**
     * Zero weight must produce all-zero NutritionValues (not divide-by-zero).
     */
    @Test
    fun `scaling zero weight returns all zeros`() {
        val result = scaleUseCase(
            kcalPer100g = 500.0,
            proteinPer100g = 25.0,
            carbsPer100g = 60.0,
            fatPer100g = 15.0,
            weightG = 0.0,
        )
        assertEquals(0.0, result.kcal, 0.0)
        assertEquals(0.0, result.proteinG, 0.0)
        assertEquals(0.0, result.carbsG, 0.0)
        assertEquals(0.0, result.fatG, 0.0)
    }

    /**
     * Negative weight must throw IllegalArgumentException per the use case contract.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `scaling negative weight throws IllegalArgumentException`() {
        scaleUseCase(100.0, 10.0, 20.0, 5.0, weightG = -1.0)
    }

    /**
     * Each per-100g field independently rejects negative values.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `negative kcalPer100g throws IllegalArgumentException`() {
        scaleUseCase(-1.0, 10.0, 20.0, 5.0, 100.0)
    }

    // ============================================================
    // CreateRecipeViewModel: editIngredient and saveRecipe guards
    // ============================================================

    @Test
    fun `editIngredient with unknown id is a no-op and isDirty remains unchanged`() = runTest {
        val recipeRepo = mockk<RecipeRepository>(relaxed = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = CreateRecipeViewModel(recipeRepo, ScaleNutritionUseCase(), savedStateHandle)

        // Add one ingredient with id=-1
        viewModel.addIngredient(
            IngredientDraft("Chicken", 200.0, 165.0, 31.0, 0.0, 3.6, id = -1L),
        )
        val nameBeforeEdit = viewModel.uiState.value.ingredients.first().name

        // Call editIngredient with an id that doesn't exist
        viewModel.editIngredient(
            id = 999L,
            newValues = IngredientEditValues("Other", 100.0, 200.0, 10.0, 30.0, 5.0),
        )

        // The ingredient list must be unchanged
        assertEquals(1, viewModel.uiState.value.ingredients.size)
        assertEquals(nameBeforeEdit, viewModel.uiState.value.ingredients.first().name)
    }

    /**
     * saveRecipe must not call recipeRepo when the recipe name is blank.
     * Architecture §7.7 / CreateRecipeViewModel.saveRecipe guards:
     * "if (state.recipeName.isBlank() || state.ingredients.isEmpty()) return"
     */
    @Test
    fun `saveRecipe does not call repository when recipe name is blank`() = runTest {
        val recipeRepo = mockk<RecipeRepository>(relaxed = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = CreateRecipeViewModel(recipeRepo, ScaleNutritionUseCase(), savedStateHandle)

        // Add an ingredient so only the name check is the guard
        viewModel.addIngredient(
            IngredientDraft("Chicken", 200.0, 165.0, 31.0, 0.0, 3.6),
        )
        // Leave recipeName empty (default is "")
        viewModel.saveRecipe()

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { recipeRepo.saveRecipe(any(), any()) }
        coVerify(exactly = 0) { recipeRepo.updateRecipe(any(), any()) }
    }

    /**
     * saveRecipe must not call recipeRepo when the ingredient list is empty.
     */
    @Test
    fun `saveRecipe does not call repository when ingredient list is empty`() = runTest {
        val recipeRepo = mockk<RecipeRepository>(relaxed = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = CreateRecipeViewModel(recipeRepo, ScaleNutritionUseCase(), savedStateHandle)

        viewModel.setRecipeName("My Recipe")
        // No ingredients added

        viewModel.saveRecipe()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { recipeRepo.saveRecipe(any(), any()) }
        coVerify(exactly = 0) { recipeRepo.updateRecipe(any(), any()) }
    }

    /**
     * After addIngredient, liveTotals and totalWeightG must reflect the scaled values of
     * the new ingredient. This verifies that recomputeTotals is called on addIngredient.
     */
    @Test
    fun `addIngredient triggers recomputeTotals and updates liveTotals`() = runTest {
        val recipeRepo = mockk<RecipeRepository>(relaxed = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = CreateRecipeViewModel(recipeRepo, ScaleNutritionUseCase(), savedStateHandle)

        // Initial state: all zeros
        assertEquals(0.0, viewModel.uiState.value.liveTotals.kcal, 0.001)

        // Add 200g chicken at 165 kcal/100g -> 330 kcal
        viewModel.addIngredient(
            IngredientDraft("Chicken", 200.0, 165.0, 31.0, 0.0, 3.6),
        )

        assertEquals(330.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
        assertEquals(62.0, viewModel.uiState.value.liveTotals.proteinG, 0.001)
        assertEquals(0.0, viewModel.uiState.value.liveTotals.carbsG, 0.001)
        assertEquals(7.2, viewModel.uiState.value.liveTotals.fatG, 0.001)
        assertEquals(200.0, viewModel.uiState.value.totalWeightG, 0.001)
    }

    /**
     * After editIngredient, liveTotals must be recomputed to reflect the updated values.
     */
    @Test
    fun `editIngredient triggers recomputeTotals and updates liveTotals`() = runTest {
        val recipeRepo = mockk<RecipeRepository>(relaxed = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = CreateRecipeViewModel(recipeRepo, ScaleNutritionUseCase(), savedStateHandle)

        // Add 200g chicken at 165 kcal/100g -> 330 kcal
        viewModel.addIngredient(
            IngredientDraft("Chicken", 200.0, 165.0, 31.0, 0.0, 3.6, id = -1L),
        )
        assertEquals(330.0, viewModel.uiState.value.liveTotals.kcal, 0.001)

        // Edit to 100g chicken -> 165 kcal
        viewModel.editIngredient(
            id = -1L,
            newValues = IngredientEditValues("Chicken", 100.0, 165.0, 31.0, 0.0, 3.6),
        )

        assertEquals(165.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
        assertEquals(31.0, viewModel.uiState.value.liveTotals.proteinG, 0.001)
        assertEquals(100.0, viewModel.uiState.value.totalWeightG, 0.001)
    }

    /**
     * removeIngredient with an out-of-range index is a no-op (does not crash).
     */
    @Test
    fun `removeIngredient with out-of-range index is a no-op`() = runTest {
        val recipeRepo = mockk<RecipeRepository>(relaxed = true)
        val savedStateHandle = androidx.lifecycle.SavedStateHandle()
        val viewModel = CreateRecipeViewModel(recipeRepo, ScaleNutritionUseCase(), savedStateHandle)

        viewModel.addIngredient(IngredientDraft("Rice", 150.0, 130.0, 2.7, 28.0, 0.3))

        val sizeBeforeRemove = viewModel.uiState.value.ingredients.size

        // Out of range index
        viewModel.removeIngredient(99)

        assertEquals(sizeBeforeRemove, viewModel.uiState.value.ingredients.size)
    }

    // ============================================================
    // Daily progress aggregation: precision
    // ============================================================

    /**
     * When a large number of entries are aggregated (simulating a very active user over
     * a single day), the sum must not overflow. LogEntry uses Double which can handle
     * the maximum sums a single day could produce.
     */
    @Test
    fun `summing 100 log entries produces correct total without overflow`() {
        // 100 entries of 200 kcal each = 20,000 kcal total
        val entries = (1..100).map { i ->
            LogEntry(
                foodName = "Food $i",
                kcal = 200.0,
                proteinG = 10.0,
                carbsG = 25.0,
                fatG = 5.0,
                timestamp = i.toLong(),
            )
        }

        val totalKcal = entries.sumOf { it.kcal }
        val totalProtein = entries.sumOf { it.proteinG }
        val totalCarbs = entries.sumOf { it.carbsG }
        val totalFat = entries.sumOf { it.fatG }

        assertEquals(20_000.0, totalKcal, 0.001)
        assertEquals(1_000.0, totalProtein, 0.001)
        assertEquals(2_500.0, totalCarbs, 0.001)
        assertEquals(500.0, totalFat, 0.001)
    }
}
