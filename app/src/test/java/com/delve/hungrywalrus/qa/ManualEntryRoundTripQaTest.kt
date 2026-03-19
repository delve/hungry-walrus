package com.delve.hungrywalrus.qa

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryUiEvent
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA integration tests for the manual entry round-trip:
 * setDirectEntry -> saveEntry -> LogEntry values.
 *
 * The previous QA report identified this as the highest-priority gap:
 * "The integration path from manual entry through to saveEntry and the resulting
 *  LogEntry values has no end-to-end test verifying the sentinel arithmetic is correct
 *  (i.e. that entering kcal=600 produces a log entry with kcal=600.0)."
 *
 * The sentinel pattern is: the user enters consumed values, which are stored in the
 * FoodSearchResult per-100g fields. weight defaults to "100". Scaling:
 *   scaledKcal = kcal * 100 / 100 = kcal (unchanged).
 * So entering 400 kcal must produce a LogEntry.kcal of exactly 400.0.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualEntryRoundTripQaTest {

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

    /**
     * Spec: "For manual entries, the user enters nutritional values directly as consumed.
     * No weight input is required and no scaling is performed."
     *
     * After setDirectEntry(kcal=600, protein=40, carbs=80, fat=15), saveEntry must produce
     * a LogEntry with exactly those values — no sentinel scaling must alter them.
     */
    @Test
    fun `manual entry setDirectEntry then saveEntry produces LogEntry with exact entered values`() = runTest {
        val capturedEntry = slot<LogEntry>()
        coEvery { logRepo.addEntry(capture(capturedEntry)) } returns Unit

        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Chicken salad",
            kcal = 600.0,
            proteinG = 40.0,
            carbsG = 20.0,
            fatG = 25.0,
        )

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Expected EntrySaved event", event is AddEntryUiEvent.EntrySaved)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify the stored LogEntry has exactly the values the user entered
        assertEquals("Chicken salad", capturedEntry.captured.foodName)
        assertEquals(600.0, capturedEntry.captured.kcal, 0.001)
        assertEquals(40.0, capturedEntry.captured.proteinG, 0.001)
        assertEquals(20.0, capturedEntry.captured.carbsG, 0.001)
        assertEquals(25.0, capturedEntry.captured.fatG, 0.001)
    }

    /**
     * Manual entry with zero kcal (e.g. plain water, no nutritional value).
     * Zero is a valid entered value; the resulting LogEntry must also be zero.
     */
    @Test
    fun `manual entry with all-zero values produces LogEntry with all-zero nutritional values`() = runTest {
        val capturedEntry = slot<LogEntry>()
        coEvery { logRepo.addEntry(capture(capturedEntry)) } returns Unit

        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Water",
            kcal = 0.0,
            proteinG = 0.0,
            carbsG = 0.0,
            fatG = 0.0,
        )

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // EntrySaved
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0.0, capturedEntry.captured.kcal, 0.001)
        assertEquals(0.0, capturedEntry.captured.proteinG, 0.001)
        assertEquals(0.0, capturedEntry.captured.carbsG, 0.001)
        assertEquals(0.0, capturedEntry.captured.fatG, 0.001)
    }

    /**
     * Manual entry with fractional values (e.g. 12.7 kcal) must be stored with
     * full Double precision. The sentinel arithmetic (x * 100 / 100 = x) must not
     * introduce rounding errors for values with decimal components.
     */
    @Test
    fun `manual entry with fractional values preserves full precision in LogEntry`() = runTest {
        val capturedEntry = slot<LogEntry>()
        coEvery { logRepo.addEntry(capture(capturedEntry)) } returns Unit

        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Custom food",
            kcal = 123.7,
            proteinG = 8.3,
            carbsG = 15.2,
            fatG = 3.9,
        )

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // EntrySaved
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(123.7, capturedEntry.captured.kcal, 0.001)
        assertEquals(8.3, capturedEntry.captured.proteinG, 0.001)
        assertEquals(15.2, capturedEntry.captured.carbsG, 0.001)
        assertEquals(3.9, capturedEntry.captured.fatG, 0.001)
    }

    /**
     * Manual entry food name is preserved exactly in the LogEntry.
     * The food name appears in the log entry item and must not be altered.
     */
    @Test
    fun `manual entry food name is stored verbatim in LogEntry`() = runTest {
        val capturedEntry = slot<LogEntry>()
        coEvery { logRepo.addEntry(capture(capturedEntry)) } returns Unit

        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "Homemade beef stew",
            kcal = 350.0,
            proteinG = 30.0,
            carbsG = 20.0,
            fatG = 10.0,
        )

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // EntrySaved
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("Homemade beef stew", capturedEntry.captured.foodName)
    }

    /**
     * When the food name is blank, saveEntry falls back to "Unknown food".
     * This defensive behaviour from AddEntryViewModel.saveEntry is verified here.
     */
    @Test
    fun `manual entry with blank name stores Unknown food in LogEntry`() = runTest {
        val capturedEntry = slot<LogEntry>()
        coEvery { logRepo.addEntry(capture(capturedEntry)) } returns Unit

        val viewModel = createViewModel()
        viewModel.setDirectEntry(
            name = "   ",  // blank name
            kcal = 200.0,
            proteinG = 10.0,
            carbsG = 30.0,
            fatG = 5.0,
        )

        viewModel.events.test {
            viewModel.saveEntry()
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem() // EntrySaved
            cancelAndIgnoreRemainingEvents()
        }

        // Architecture spec: foodName.ifBlank { "Unknown food" }
        assertEquals("Unknown food", capturedEntry.captured.foodName)
    }
}
