package com.delve.hungrywalrus.ui.screen.summaries

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummariesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logRepo: LogEntryRepository
    private lateinit var planRepo: NutritionPlanRepository
    private val computeSummaryUseCase = ComputeRollingSummaryUseCase()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logRepo = mockk()
        planRepo = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads 7 day summary with no plan as NoPlan state`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            val state = awaitItem()
            assertTrue(state is SummariesUiState.NoPlan)
            val noPlan = state as SummariesUiState.NoPlan
            assertEquals(SummaryTab.Day7, noPlan.selectedTab)
            assertEquals(7, noPlan.summary.periodDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads summary with plan as Content state`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0,
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns plan

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            val state = awaitItem()
            assertTrue(state is SummariesUiState.Content)
            val content = state as SummariesUiState.Content
            assertNotNull(content.summary.totalTarget)
            assertEquals(7, content.summary.periodDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectTab switches to 28 day and reloads`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            // Current state from init
            val initial = awaitItem()

            viewModel.selectTab(SummaryTab.Day28)
            // Should get Loading then new state
            val loading = awaitItem()
            assertEquals(SummariesUiState.Loading, loading)

            val state = awaitItem()
            assertTrue(state is SummariesUiState.NoPlan)
            assertEquals(SummaryTab.Day28, (state as SummariesUiState.NoPlan).selectedTab)
            assertEquals(28, state.summary.periodDays)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dailyAverage is total intake divided by period days`() = runTest {
        val entries = listOf(
            LogEntry(foodName = "A", kcal = 280.0, proteinG = 14.0, carbsG = 35.0, fatG = 7.0, timestamp = 0L),
            LogEntry(foodName = "B", kcal = 280.0, proteinG = 14.0, carbsG = 35.0, fatG = 7.0, timestamp = 1L),
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(entries)
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            val state = awaitItem() as SummariesUiState.NoPlan
            // total 560 kcal / 7 days = 80 kcal/day
            assertEquals(80.0, state.summary.dailyAverage.kcal, 0.001)
            // total 28g protein / 7 days = 4g/day
            assertEquals(4.0, state.summary.dailyAverage.proteinG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
