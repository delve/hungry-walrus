package com.delve.hungrywalrus.ui.screen.dailyprogress

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Additional edge-case tests for [DailyProgressViewModel] covering
 * aggregation correctness and sort order per the spec.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyProgressViewModelEdgeCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: NutritionPlanRepository
    private lateinit var logRepo: LogEntryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = mockk()
        logRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `entries are sorted descending by timestamp`() = runTest {
        val old = LogEntry(id = 1, foodName = "Breakfast", kcal = 400.0, proteinG = 20.0, carbsG = 60.0, fatG = 10.0, timestamp = 100L)
        val newer = LogEntry(id = 2, foodName = "Lunch", kcal = 600.0, proteinG = 30.0, carbsG = 80.0, fatG = 20.0, timestamp = 200L)
        val newest = LogEntry(id = 3, foodName = "Dinner", kcal = 700.0, proteinG = 40.0, carbsG = 90.0, fatG = 25.0, timestamp = 300L)

        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(listOf(old, newer, newest))

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(3, content.entries.size)
            assertEquals(300L, content.entries[0].timestamp) // newest first
            assertEquals(200L, content.entries[1].timestamp)
            assertEquals(100L, content.entries[2].timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totals are zero when no entries logged`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(0.0, content.totalKcal, 0.001)
            assertEquals(0.0, content.totalProteinG, 0.001)
            assertEquals(0.0, content.totalCarbsG, 0.001)
            assertEquals(0.0, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totals aggregate all four macronutrients correctly across multiple entries`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )
        val entries = listOf(
            LogEntry(id = 1, foodName = "A", kcal = 100.0, proteinG = 10.0, carbsG = 15.0, fatG = 3.0, timestamp = 1L),
            LogEntry(id = 2, foodName = "B", kcal = 200.0, proteinG = 20.0, carbsG = 25.0, fatG = 6.0, timestamp = 2L),
            LogEntry(id = 3, foodName = "C", kcal = 300.0, proteinG = 30.0, carbsG = 35.0, fatG = 9.0, timestamp = 3L),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(plan)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(entries)

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(600.0, content.totalKcal, 0.001)
            assertEquals(60.0, content.totalProteinG, 0.001)
            assertEquals(75.0, content.totalCarbsG, 0.001)
            assertEquals(18.0, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single entry totals match that entry exactly`() = runTest {
        val entry = LogEntry(id = 1, foodName = "Chicken", kcal = 165.0, proteinG = 31.0, carbsG = 0.0, fatG = 3.6, timestamp = 1L)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(listOf(entry))

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(165.0, content.totalKcal, 0.001)
            assertEquals(31.0, content.totalProteinG, 0.001)
            assertEquals(0.0, content.totalCarbsG, 0.001)
            assertEquals(3.6, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state is Content not Error when plan is null`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue(state is DailyProgressUiState.Content)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
