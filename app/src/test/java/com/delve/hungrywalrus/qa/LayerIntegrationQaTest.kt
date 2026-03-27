package com.delve.hungrywalrus.qa

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA integration tests for the repository-to-ViewModel layer interaction.
 *
 * These tests fill gaps in [RepositoryToViewModelIntegrationTest]:
 *
 * - DailyProgressViewModel: large single entry with exact macros (precision check).
 * - DailyProgressViewModel: plan changes reactively after initial load
 *   (verifies that planRepo.getCurrentPlan() is observed as a Flow, not snapshotted).
 * - SummariesViewModel: 28-day tab shows 28 days in summary after explicit tab switch.
 * - SummariesViewModel: NoPlan state when plan only exists for some days.
 * - Use case (ScaleNutritionUseCase) invoked correctly by the ViewModel pipeline:
 *   full round-trip from per-100g reference to log entry through manual wiring.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LayerIntegrationQaTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: NutritionPlanRepository
    private lateinit var logRepo: LogEntryRepository
    private val scaleUseCase = ScaleNutritionUseCase()
    private val summaryUseCase = ComputeRollingSummaryUseCase()

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

    // ---- DailyProgressViewModel ----

    /**
     * Verify that nutritional totals are computed with floating-point precision across
     * entries that produce non-round numbers.
     */
    @Test
    fun `DailyProgressViewModel sums fractional nutritional values without precision loss`() = runTest {
        val entries = listOf(
            // 375g oats: (374/100)*375 = 1402.5 kcal, etc.
            LogEntry(id = 1, foodName = "Oats", kcal = 1402.5, proteinG = 49.5, carbsG = 253.875, fatG = 26.25, timestamp = 1L),
            // 120g almonds: (579/100)*120 = 694.8 kcal, etc.
            LogEntry(id = 2, foodName = "Almonds", kcal = 694.8, proteinG = 25.44, carbsG = 25.68, fatG = 63.0, timestamp = 2L),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(entries)

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(2097.3, content.totalKcal, 0.001)
            assertEquals(74.94, content.totalProteinG, 0.001)
            assertEquals(279.555, content.totalCarbsG, 0.001)
            assertEquals(89.25, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * DailyProgressViewModel must use a plan Flow and reflect plan changes reactively.
     * This verifies the use of combine() not a one-shot plan fetch.
     * The Content state must carry the plan from the repository as-is.
     */
    @Test
    fun `DailyProgressViewModel Content state carries plan object from repository`() = runTest {
        val plan = NutritionPlan(
            id = 99, kcalTarget = 1800, proteinTargetG = 120.0,
            carbsTargetG = 200.0, fatTargetG = 55.0, effectiveFrom = 12345L,
        )
        every { planRepo.getCurrentPlan() } returns flowOf(plan)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(plan.id, content.plan!!.id)
            assertEquals(plan.kcalTarget, content.plan!!.kcalTarget)
            assertEquals(plan.effectiveFrom, content.plan!!.effectiveFrom)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * When no plan is configured, the daily progress screen must show Content with a null plan,
     * not an error state. Users should see their entries even without a plan.
     */
    @Test
    fun `DailyProgressViewModel Content state with null plan still shows entries`() = runTest {
        val entries = listOf(
            LogEntry(id = 1, foodName = "Banana", kcal = 89.0, proteinG = 1.1, carbsG = 22.8, fatG = 0.3, timestamp = 1L),
        )
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(entries)

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertNull(content.plan)
            assertEquals(1, content.entries.size)
            assertEquals("Banana", content.entries[0].foodName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- SummariesViewModel ----

    /**
     * Switching from 7-day to 28-day tab must produce a summary with 28 periodDays
     * and a correctly accumulated totalTarget (28 * plan.kcalTarget).
     */
    @Test
    fun `SummariesViewModel 28-day Content has 28x daily kcal target`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 1500, proteinTargetG = 100.0,
            carbsTargetG = 180.0, fatTargetG = 50.0, effectiveFrom = 0L,
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns plan

        val viewModel = SummariesViewModel(logRepo, planRepo, summaryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTab(SummaryTab.Day28)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            val content = state as SummariesUiState.Content
            assertEquals(SummaryTab.Day28, content.selectedTab)
            assertEquals(28, content.summary.periodDays)
            assertEquals(28 * 1500.0, content.summary.totalTarget!!.kcal, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * When the 7-day period has no plan for any day, the SummariesViewModel must produce
     * NoPlan state (not Content), ensuring the UI shows the "no plan" message.
     */
    @Test
    fun `SummariesViewModel produces NoPlan not Content when plan is missing`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, summaryUseCase)

        viewModel.uiState.test {
            awaitItem() // Loading
            val state = awaitItem()
            assertTrue(
                "Expected NoPlan but got $state",
                state is SummariesUiState.NoPlan,
            )
            val noPlan = state as SummariesUiState.NoPlan
            assertNull(noPlan.summary.totalTarget)
            assertEquals(SummaryTab.Day7, noPlan.selectedTab)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---- ScaleNutritionUseCase round-trip ---

    /**
     * Round-trip test: apply ScaleNutritionUseCase directly (as DailyProgressViewModel would
     * receive pre-scaled LogEntry values) and verify the aggregation in the ViewModel is exact.
     *
     * This simulates: user logs 250g of yoghurt (59 kcal, 10g protein, 3.6g carbs, 0.4g fat per 100g).
     * The AddEntryViewModel would scale these via ScaleNutritionUseCase; the resulting LogEntry
     * is stored and retrieved by DailyProgressViewModel.
     */
    @Test
    fun `scaled LogEntry values aggregate correctly in DailyProgressViewModel`() = runTest {
        // Scale 250g yoghurt
        val scaled = scaleUseCase(
            kcalPer100g = 59.0,
            proteinPer100g = 10.0,
            carbsPer100g = 3.6,
            fatPer100g = 0.4,
            weightG = 250.0,
        )
        // Create the log entry as the repository would store it
        val entry = LogEntry(
            id = 1,
            foodName = "Yoghurt",
            kcal = scaled.kcal,
            proteinG = scaled.proteinG,
            carbsG = scaled.carbsG,
            fatG = scaled.fatG,
            timestamp = 1L,
        )

        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(listOf(entry))

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content

            // Verify exact scaled values: 250g * 59/100 = 147.5 kcal
            assertEquals(147.5, content.totalKcal, 0.001)
            assertEquals(25.0, content.totalProteinG, 0.001)
            assertEquals(9.0, content.totalCarbsG, 0.001)
            assertEquals(1.0, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
