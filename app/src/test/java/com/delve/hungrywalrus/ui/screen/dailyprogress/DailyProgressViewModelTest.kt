package com.delve.hungrywalrus.ui.screen.dailyprogress

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyProgressViewModelTest {

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
    fun `initial state is Loading`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Content with totals when data available`() = runTest {
        val plan = NutritionPlan(
            id = 1,
            kcalTarget = 2000,
            proteinTargetG = 150.0,
            carbsTargetG = 250.0,
            fatTargetG = 65.0,
            effectiveFrom = System.currentTimeMillis(),
        )
        val entries = listOf(
            LogEntry(id = 1, foodName = "Chicken", kcal = 300.0, proteinG = 30.0, carbsG = 0.0, fatG = 5.0, timestamp = System.currentTimeMillis()),
            LogEntry(id = 2, foodName = "Rice", kcal = 200.0, proteinG = 4.0, carbsG = 45.0, fatG = 1.0, timestamp = System.currentTimeMillis()),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(plan)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(entries)

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(plan, content.plan)
            assertEquals(2, content.entries.size)
            assertEquals(500.0, content.totalKcal, 0.001)
            assertEquals(34.0, content.totalProteinG, 0.001)
            assertEquals(45.0, content.totalCarbsG, 0.001)
            assertEquals(6.0, content.totalFatG, 0.001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Content with null plan when no plan configured`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val content = awaitItem() as DailyProgressUiState.Content
            assertNull(content.plan)
            assertTrue(content.entries.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteEntry calls repository`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteEntry(42L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { logRepo.deleteEntry(42L) }
    }
}
