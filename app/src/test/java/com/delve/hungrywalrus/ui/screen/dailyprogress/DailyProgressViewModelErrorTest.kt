package com.delve.hungrywalrus.ui.screen.dailyprogress

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DailyProgressViewModelErrorTest {

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
    fun `emits Error state when planRepo throws`() = runTest {
        every { planRepo.getCurrentPlan() } returns flow { throw RuntimeException("DB error") }
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue("Expected Error state but got $error", error is DailyProgressUiState.Error)
            assertEquals("DB error", (error as DailyProgressUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error state when logRepo throws`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        every { logRepo.getEntriesForDate(any()) } returns flow { throw RuntimeException("Query failed") }

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue("Expected Error state but got $error", error is DailyProgressUiState.Error)
            assertEquals("Query failed", (error as DailyProgressUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error with fallback message when exception has no message`() = runTest {
        every { planRepo.getCurrentPlan() } returns flow { throw RuntimeException() }
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue("Expected Error state but got $error", error is DailyProgressUiState.Error)
            assertEquals(
                "Could not load data. Please restart the app.",
                (error as DailyProgressUiState.Error).message,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow completes after error emission and no further Content events arrive`() = runTest {
        // RuntimeException is not retried (only IOException is), so the error
        // propagates directly to catch and the flow terminates.
        every { planRepo.getCurrentPlan() } returns flow { throw RuntimeException("persistent failure") }
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue("Expected Error state but got $error", error is DailyProgressUiState.Error)

            // After the Error emission, the upstream flow has terminated due to
            // catch consuming the exception. Verify no further events arrive.
            advanceUntilIdle()
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retries IO errors before emitting Error`() = runTest {
        // Track how many times the plan flow is collected. IOException is retryable,
        // so the flow should be collected 1 (initial) + RETRY_COUNT (retries) = 3
        // times total before the error propagates to catch.
        var collectionCount = 0
        every { planRepo.getCurrentPlan() } returns flow {
            collectionCount++
            throw IOException("transient I/O failure")
        }
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue("Expected Error state but got $error", error is DailyProgressUiState.Error)
            // 1 initial attempt + 2 retries = 3 total collections
            assertEquals(
                1 + DailyProgressViewModel.RETRY_COUNT.toInt(),
                collectionCount,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recovers on retry when transient IO error resolves`() = runTest {
        // The plan flow throws IOException on the first attempt but succeeds on retry.
        var attempt = 0
        every { planRepo.getCurrentPlan() } returns flow {
            attempt++
            if (attempt <= 1) {
                throw IOException("transient I/O failure")
            }
            emit(null)
        }
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            // On retry the flow succeeds, so we should get Content not Error
            val content = awaitItem()
            assertTrue(
                "Expected Content after transient error recovery but got $content",
                content is DailyProgressUiState.Content,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `does not retry non-IO exceptions`() = runTest {
        // Non-IO exceptions (e.g. IllegalArgumentException) should not be retried.
        // The flow should be collected exactly once before error propagates to catch.
        var collectionCount = 0
        every { planRepo.getCurrentPlan() } returns flow {
            collectionCount++
            throw IllegalArgumentException("programming error")
        }
        every { logRepo.getEntriesForDate(any()) } returns flowOf(emptyList())

        val viewModel = DailyProgressViewModel(planRepo, logRepo)

        viewModel.uiState.test {
            assertEquals(DailyProgressUiState.Loading, awaitItem())
            val error = awaitItem()
            assertTrue("Expected Error state but got $error", error is DailyProgressUiState.Error)
            assertEquals("programming error", (error as DailyProgressUiState.Error).message)
            // Only 1 collection -- no retries for non-IO exceptions
            assertEquals(1, collectionCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
