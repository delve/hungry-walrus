package com.delve.hungrywalrus.ui.screen.summaries

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SummariesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logRepo: LogEntryRepository
    private lateinit var planRepo: NutritionPlanRepository
    private val computeSummaryUseCase = ComputeRollingSummaryUseCase()

    // Fixed clock at 20:00 local time today. At/after the 20:00 cutoff (architecture
    // §7.6) today is included in the window, so end == today. This preserves the
    // assertion semantics these tests inherited from the pre-cutoff implementation.
    private val fixedClock: Clock = Clock.fixed(
        LocalDate.now().atTime(LocalTime.of(20, 0)).atZone(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
    )

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

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads 7 day summary with no plan as NoPlan state`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)

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

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)

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

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)
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
    fun `shows Content when plan is only set for today and fallback covers earlier days`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0,
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        // Simulate: only today has a plan (effectiveFrom = today). Earlier dates return null
        // but getPlanForDate(today) returns the plan (used as fallback for all earlier days).
        val today = LocalDate.now()
        coEvery { planRepo.getPlanForDate(any()) } returns null
        coEvery { planRepo.getPlanForDate(today) } returns plan

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            val state = awaitItem()
            // Fallback fills all 7 days with the plan → totalTarget is not null → Content state
            assertTrue(state is SummariesUiState.Content)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `summary updates reactively when new entries are logged`() = runTest {
        val entriesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<LogEntry>>(emptyList())
        every { logRepo.getEntriesForRange(any(), any()) } returns entriesFlow
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)

        viewModel.uiState.test {
            assertEquals(SummariesUiState.Loading, awaitItem())
            val initial = awaitItem() as SummariesUiState.NoPlan
            assertEquals(0.0, initial.summary.totalIntake.kcal, 0.001)

            // Simulate a new entry being logged
            entriesFlow.value = listOf(
                LogEntry(foodName = "Apple", kcal = 52.0, proteinG = 0.3, carbsG = 14.0, fatG = 0.2, timestamp = 0L),
            )

            val updated = awaitItem() as SummariesUiState.NoPlan
            assertEquals(52.0, updated.summary.totalIntake.kcal, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reloadSummary re-fetches plan data and emits fresh state`() = runTest {
        var callCount = 0
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0,
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } answers {
            callCount++
            plan
        }

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)
        testDispatcher.scheduler.advanceUntilIdle()

        val callCountAfterInit = callCount
        assertTrue("init should query plan at least once", callCountAfterInit > 0)

        viewModel.reloadSummary()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("reloadSummary should re-query plan", callCount > callCountAfterInit)
        val state = viewModel.uiState.value
        assertTrue(state is SummariesUiState.Content)
    }

    @Test
    fun `reloadSummary after selectTab reloads the 28-day tab`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTab(SummaryTab.Day28)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reloadSummary()
        testDispatcher.scheduler.advanceUntilIdle()

        val noPlan = viewModel.uiState.value as SummariesUiState.NoPlan
        assertEquals(SummaryTab.Day28, noPlan.selectedTab)
        assertEquals(28, noPlan.summary.periodDays)
    }

    @Test
    fun `buildDailyPlans queries planRepo for all dates in the 7-day period`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)
        testDispatcher.scheduler.advanceUntilIdle()

        // 7 period dates queried. With the clock fixed at 20:00 the window ends at
        // today (today is in the dates list); the fallback reuses that deferred
        // rather than issuing a second query. Total: exactly 7 calls.
        coVerify(atLeast = 7) { planRepo.getPlanForDate(any()) }
        assertTrue(viewModel.uiState.value is SummariesUiState.NoPlan)
    }

    @Test
    fun `buildDailyPlans does not issue a redundant query for today`() = runTest {
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)
        testDispatcher.scheduler.advanceUntilIdle()

        // At/after 20:00 today == end, so today is in the dates list. The fallback
        // reuses that deferred — no second query is issued. Exactly 7 calls total.
        coVerify(exactly = 7) { planRepo.getPlanForDate(any()) }
        assertTrue(viewModel.uiState.value is SummariesUiState.NoPlan)
    }

    @Test
    fun `dailyAverage is total intake divided by period days`() = runTest {
        val entries = listOf(
            LogEntry(foodName = "A", kcal = 280.0, proteinG = 14.0, carbsG = 35.0, fatG = 7.0, timestamp = 0L),
            LogEntry(foodName = "B", kcal = 280.0, proteinG = 14.0, carbsG = 35.0, fatG = 7.0, timestamp = 1L),
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(entries)
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, fixedClock)

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

    // --- 20:00 rolling-window cutoff (architecture §7.6) ---

    /**
     * Before 20:00 local time, the rolling window must end at end-of-yesterday and
     * the [SummariesUiState.WithSummary.includesToday] flag must be false.
     */
    @Test
    fun `before 20-00 endDate is yesterday and includesToday is false`() = runTest {
        val today = LocalDate.of(2026, 5, 20)
        val clockAt19_59 = Clock.fixed(
            today.atTime(19, 59).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault(),
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, clockAt19_59)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SummariesUiState.NoPlan
        assertEquals(false, state.includesToday)
        assertEquals(today.minusDays(1), state.summary.endDate)
        // 7-day window ending yesterday: start = yesterday.minusDays(6) = today.minusDays(7)
        assertEquals(today.minusDays(7), state.summary.startDate)
    }

    /**
     * At exactly 20:00 local time the rolling window must include today.
     */
    @Test
    fun `at 20-00 endDate is today and includesToday is true`() = runTest {
        val today = LocalDate.of(2026, 5, 20)
        val clockAt20_00 = Clock.fixed(
            today.atTime(20, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault(),
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, clockAt20_00)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SummariesUiState.NoPlan
        assertEquals(true, state.includesToday)
        assertEquals(today, state.summary.endDate)
        assertEquals(today.minusDays(6), state.summary.startDate)
    }

    /**
     * Well after the cutoff (20:01) behaviour matches 20:00 — today is included.
     */
    @Test
    fun `after 20-00 endDate is today and includesToday is true`() = runTest {
        val today = LocalDate.of(2026, 5, 20)
        val clockAt20_01 = Clock.fixed(
            today.atTime(20, 1).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault(),
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, clockAt20_01)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SummariesUiState.NoPlan
        assertEquals(true, state.includesToday)
        assertEquals(today, state.summary.endDate)
    }

    /**
     * Before 20:00, the 28-day window ends yesterday and spans 28 days backward.
     */
    @Test
    fun `before 20-00 28-day window ends yesterday`() = runTest {
        val today = LocalDate.of(2026, 5, 20)
        val clockAt10 = Clock.fixed(
            today.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault(),
        )
        every { logRepo.getEntriesForRange(any(), any()) } returns flowOf(emptyList())
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val viewModel = SummariesViewModel(logRepo, planRepo, computeSummaryUseCase, clockAt10)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTab(SummaryTab.Day28)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as SummariesUiState.NoPlan
        assertEquals(false, state.includesToday)
        assertEquals(today.minusDays(1), state.summary.endDate)
        assertEquals(today.minusDays(28), state.summary.startDate)
        assertEquals(28, state.summary.periodDays)
    }
}
