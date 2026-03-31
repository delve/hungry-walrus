package com.delve.hungrywalrus.ui.screen.dailyprogress

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyProgressDateRefreshTest {

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
    fun `queries entries for the date returned by todayProvider`() = runTest {
        val fixedDate = LocalDate.of(2026, 3, 28)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())
        every { logRepo.getEntriesForDate(fixedDate) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)
        viewModel.todayProvider = { fixedDate }
        viewModel.refreshDate()

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertTrue(content.entries.isEmpty())
            assertEquals(fixedDate, content.displayDate)
            cancelAndIgnoreRemainingEvents()
        }

        verify { logRepo.getEntriesForDate(fixedDate) }
    }

    @Test
    fun `refreshDate switches to new date and re-queries entries`() = runTest {
        val yesterday = LocalDate.of(2026, 3, 27)
        val today = LocalDate.of(2026, 3, 28)

        val yesterdayEntries = listOf(
            LogEntry(id = 1, foodName = "Old food", kcal = 100.0, proteinG = 10.0, carbsG = 15.0, fatG = 3.0, timestamp = 1L),
        )
        val todayEntries = listOf(
            LogEntry(id = 2, foodName = "New food", kcal = 200.0, proteinG = 20.0, carbsG = 25.0, fatG = 6.0, timestamp = 2L),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())
        every { logRepo.getEntriesForDate(yesterday) } returns flowOf(yesterdayEntries)
        every { logRepo.getEntriesForDate(today) } returns flowOf(todayEntries)

        var currentDate = yesterday
        val viewModel = DailyProgressViewModel(planRepo, logRepo)
        viewModel.todayProvider = { currentDate }
        viewModel.refreshDate()

        viewModel.uiState.test {
            awaitItem() // Loading
            val content1 = awaitItem() as DailyProgressUiState.Content
            assertEquals(1, content1.entries.size)
            assertEquals("Old food", content1.entries[0].foodName)
            assertEquals(100.0, content1.totalKcal, 0.001)
            assertEquals(yesterday, content1.displayDate)

            currentDate = today
            viewModel.refreshDate()

            val content2 = awaitItem() as DailyProgressUiState.Content
            assertEquals(1, content2.entries.size)
            assertEquals("New food", content2.entries[0].foodName)
            assertEquals(200.0, content2.totalKcal, 0.001)
            assertEquals(today, content2.displayDate)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshDate is no-op when date has not changed`() = runTest {
        val fixedDate = LocalDate.of(2026, 3, 28)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())
        every { logRepo.getEntriesForDate(fixedDate) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)
        viewModel.todayProvider = { fixedDate }
        viewModel.refreshDate()

        viewModel.uiState.test {
            awaitItem() // Loading
            awaitItem() // Content

            viewModel.refreshDate()
            // Advance coroutines to ensure any pending emissions would have fired.
            advanceUntilIdle()

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totals update correctly after date change`() = runTest {
        val day1 = LocalDate.of(2026, 3, 27)
        val day2 = LocalDate.of(2026, 3, 28)

        val day1Entries = listOf(
            LogEntry(id = 1, foodName = "A", kcal = 100.0, proteinG = 10.0, carbsG = 15.0, fatG = 3.0, timestamp = 1L),
            LogEntry(id = 2, foodName = "B", kcal = 200.0, proteinG = 20.0, carbsG = 25.0, fatG = 6.0, timestamp = 2L),
        )
        val day2Entries = listOf(
            LogEntry(id = 3, foodName = "C", kcal = 50.0, proteinG = 5.0, carbsG = 8.0, fatG = 1.0, timestamp = 3L),
        )

        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())
        every { logRepo.getEntriesForDate(day1) } returns flowOf(day1Entries)
        every { logRepo.getEntriesForDate(day2) } returns flowOf(day2Entries)

        var currentDate = day1
        val viewModel = DailyProgressViewModel(planRepo, logRepo)
        viewModel.todayProvider = { currentDate }
        viewModel.refreshDate()

        viewModel.uiState.test {
            awaitItem() // Loading
            val content1 = awaitItem() as DailyProgressUiState.Content
            assertEquals(300.0, content1.totalKcal, 0.001)
            assertEquals(30.0, content1.totalProteinG, 0.001)
            assertEquals(40.0, content1.totalCarbsG, 0.001)
            assertEquals(9.0, content1.totalFatG, 0.001)

            currentDate = day2
            viewModel.refreshDate()

            val content2 = awaitItem() as DailyProgressUiState.Content
            assertEquals(50.0, content2.totalKcal, 0.001)
            assertEquals(5.0, content2.totalProteinG, 0.001)
            assertEquals(8.0, content2.totalCarbsG, 0.001)
            assertEquals(1.0, content2.totalFatG, 0.001)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `displayDate in Content matches the todayProvider date`() = runTest {
        val fixedDate = LocalDate.of(2026, 6, 15)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)
        viewModel.todayProvider = { fixedDate }
        viewModel.refreshDate()

        viewModel.uiState.test {
            awaitItem() // Loading
            val content = awaitItem() as DailyProgressUiState.Content
            assertEquals(fixedDate, content.displayDate)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
