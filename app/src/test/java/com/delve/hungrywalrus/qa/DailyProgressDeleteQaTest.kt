package com.delve.hungrywalrus.qa

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressUiState
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA tests for the DailyProgressViewModel delete-entry path.
 *
 * The previous QA report identified this as a gap:
 * "DailyProgressViewModel — delete entry path. LogEntryRepository.deleteEntry is present
 *  in the interface and DAO but no ViewModel-level test verifies that deleting an entry
 *  causes the daily progress totals to update reactively."
 *
 * These tests verify:
 * - deleteEntry delegates to logRepo.deleteEntry with the correct id.
 * - The reactive Flow causes totals to update after an entry is removed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyProgressDeleteQaTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: NutritionPlanRepository
    private lateinit var logRepo: LogEntryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = mockk()
        logRepo = mockk(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeEntry(id: Long, kcal: Double) = LogEntry(
        id = id, foodName = "Food$id", kcal = kcal,
        proteinG = 0.0, carbsG = 0.0, fatG = 0.0, timestamp = id,
    )

    /**
     * deleteEntry(id) must call logRepo.deleteEntry with the exact same id.
     * Spec: "Log entries can be deleted from the daily progress view."
     */
    @Test
    fun `deleteEntry delegates to repository with correct id`() = runTest {
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.deleteEntry(42L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { logRepo.deleteEntry(42L) }
    }

    /**
     * When the entry list changes reactively (the Flow emits new data after deletion),
     * the Content state must reflect the updated totals.
     *
     * This simulates the reactive update: we use a MutableStateFlow for the entries
     * to model the Room DAO returning different results after deletion.
     */
    @Test
    fun `after entry deleted reactive flow emits updated totals`() = runTest {
        val twoEntries = listOf(makeEntry(1L, 400.0), makeEntry(2L, 300.0))
        val oneEntry = listOf(makeEntry(1L, 400.0))

        // Simulate the reactive DB flow: starts with 2 entries, then updates to 1
        val entriesFlow = MutableStateFlow(twoEntries)
        every { logRepo.getEntriesForDate(any()) } returns entriesFlow

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading

            // First content: both entries present
            val firstContent = awaitItem() as DailyProgressUiState.Content
            assertEquals(700.0, firstContent.totalKcal, 0.001)
            assertEquals(2, firstContent.entries.size)

            // Simulate database removing the second entry reactively
            entriesFlow.value = oneEntry

            // Second content: only one entry
            val secondContent = awaitItem() as DailyProgressUiState.Content
            assertEquals(400.0, secondContent.totalKcal, 0.001)
            assertEquals(1, secondContent.entries.size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Deleting all entries must produce a Content state with zero totals.
     * Spec: "Shows a running total of kilocalories, protein, carbohydrates, and fat
     *  consumed so far today." — zero is correct when no entries exist.
     */
    @Test
    fun `deleting all entries produces zero totals in Content state`() = runTest {
        val entriesFlow = MutableStateFlow(listOf(makeEntry(1L, 500.0)))
        every { logRepo.getEntriesForDate(any()) } returns entriesFlow

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading

            val firstContent = awaitItem() as DailyProgressUiState.Content
            assertEquals(500.0, firstContent.totalKcal, 0.001)

            // Remove the last entry
            entriesFlow.value = emptyList()

            val emptyContent = awaitItem() as DailyProgressUiState.Content
            assertEquals(0.0, emptyContent.totalKcal, 0.001)
            assertEquals(0.0, emptyContent.totalProteinG, 0.001)
            assertEquals(0.0, emptyContent.totalCarbsG, 0.001)
            assertEquals(0.0, emptyContent.totalFatG, 0.001)
            assertTrue(emptyContent.entries.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * DailyProgressViewModel.deleteEntry is a fire-and-forget operation.
     * The ViewModel does not expose a separate loading state for deletion; the
     * reactive flow handles the update. Verify calling deleteEntry does not crash.
     */
    @Test
    fun `deleteEntry can be called multiple times without error`() = runTest {
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        // Should not throw
        viewModel.deleteEntry(1L)
        viewModel.deleteEntry(2L)
        viewModel.deleteEntry(3L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 3) { logRepo.deleteEntry(any()) }
    }
}
