package com.delve.hungrywalrus.integration

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressUiState
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressViewModel
import com.delve.hungrywalrus.ui.screen.summaries.SummariesUiState
import com.delve.hungrywalrus.ui.screen.summaries.SummariesViewModel
import com.delve.hungrywalrus.ui.screen.summaries.SummaryTab
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests verifying the repository layer → ViewModel layer data flow.
 *
 * These tests use mocked repositories (to avoid Room instrumentation) and real use case
 * implementations to verify that:
 *  - DailyProgressViewModel correctly combines plan + entries data.
 *  - SummariesViewModel correctly wires repository data through ComputeRollingSummaryUseCase.
 *  - Data flows are correctly transformed and aggregated at the ViewModel boundary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryToViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: NutritionPlanRepository
    private lateinit var logRepo: LogEntryRepository
    private lateinit var recipeRepo: RecipeRepository
    private val scaleUseCase = ScaleNutritionUseCase()
    private val rollingSummaryUseCase = ComputeRollingSummaryUseCase()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = mockk()
        logRepo = mockk(relaxed = true)
        recipeRepo = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- DailyProgressViewModel integration ----

    @Test
    fun `DailyProgressViewModel combines plan and entries from repository into Content state`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )
        val entries = listOf(
            LogEntry(id = 1, foodName = "Chicken", kcal = 330.0, proteinG = 62.0, carbsG = 0.0, fatG = 7.2, timestamp = 1L),
            LogEntry(id = 2, foodName = "Rice", kcal = 260.0, proteinG = 5.4, carbsG = 56.0, fatG = 0.6, timestamp = 2L),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(plan)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(entries)

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(plan, content.plan)
            assertEquals(2, content.entries.size)
            assertEquals(590.0, content.totalKcal, 0.001)
            assertEquals(67.4, content.totalProteinG, 0.001)
            assertEquals(56.0, content.totalCarbsG, 0.001)
            assertEquals(7.8, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DailyProgressViewModel shows null plan and zero totals when no plan configured`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertNull(content.plan)
            assertEquals(0.0, content.totalKcal, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `DailyProgressViewModel entries are sorted descending by timestamp`() = runTest {
        val entries = listOf(
            LogEntry(id = 1, foodName = "Breakfast", kcal = 400.0, proteinG = 20.0, carbsG = 60.0, fatG = 10.0, timestamp = 1_000L),
            LogEntry(id = 2, foodName = "Lunch", kcal = 600.0, proteinG = 30.0, carbsG = 80.0, fatG = 20.0, timestamp = 5_000L),
            LogEntry(id = 3, foodName = "Dinner", kcal = 700.0, proteinG = 40.0, carbsG = 90.0, fatG = 25.0, timestamp = 9_000L),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(entries)

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            // Most recent entry should be first
            assertEquals(9_000L, content.entries[0].timestamp)
            assertEquals(5_000L, content.entries[1].timestamp)
            assertEquals(1_000L, content.entries[2].timestamp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- SummariesViewModel integration ----

    @Test
    fun `SummariesViewModel produces NoPlan state when no plan configured for any day`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, rollingSummaryUseCase)

        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue(state is SummariesUiState.NoPlan)
            val noPlan = state as SummariesUiState.NoPlan
            assertEquals(7, noPlan.summary.periodDays)
            assertNull(noPlan.summary.totalTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SummariesViewModel produces Content state with correct totalTarget when plan is fully configured`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns plan

        val viewModel = SummariesViewModel(logRepo, planRepo, rollingSummaryUseCase)

        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue(state is SummariesUiState.Content)
            val content = state as SummariesUiState.Content
            assertNotNull(content.summary.totalTarget)
            // 7 days * 2000 kcal/day = 14000
            assertEquals(14_000.0, content.summary.totalTarget!!.kcal, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SummariesViewModel calculates correct daily average for 7-day period`() = runTest {
        val entries = (1..7).map { day ->
            LogEntry(
                foodName = "Day $day",
                kcal = 1400.0, proteinG = 100.0, carbsG = 180.0, fatG = 50.0,
                timestamp = day.toLong(),
            )
        }
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(entries)
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, rollingSummaryUseCase)

        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem() as SummariesUiState.NoPlan
            // Total: 7 * 1400 = 9800 kcal. Average: 9800 / 7 = 1400
            assertEquals(1400.0, state.summary.dailyAverage.kcal, 0.001)
            assertEquals(100.0, state.summary.dailyAverage.proteinG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SummariesViewModel switches to 28-day period when tab changed`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, rollingSummaryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            awaitItem() // current state after init

            viewModel.selectTab(SummaryTab.Day28)
            awaitItem() // Loading
            val state = awaitItem() as SummariesUiState.NoPlan
            assertEquals(SummaryTab.Day28, state.selectedTab)
            assertEquals(28, state.summary.periodDays)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
