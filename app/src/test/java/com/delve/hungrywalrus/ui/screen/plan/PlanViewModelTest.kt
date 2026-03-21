package com.delve.hungrywalrus.ui.screen.plan

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: NutritionPlanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        val viewModel = PlanViewModel(planRepo)

        viewModel.uiState.test {
            assertEquals(PlanUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loads existing plan into Content state`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = System.currentTimeMillis(),
        )
        every { planRepo.getCurrentPlan() } returns flowOf(plan)

        val viewModel = PlanViewModel(planRepo)

        viewModel.uiState.test {
            assertEquals(PlanUiState.Loading, awaitItem())
            val content = awaitItem() as PlanUiState.Content
            assertEquals(plan, content.plan)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savePlan with invalid input emits ValidationError`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        val viewModel = PlanViewModel(planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.savePlan("", "abc", "250.0", "65.0")

        val state = viewModel.uiState.value
        assertTrue(state is PlanUiState.ValidationError)
        val errors = (state as PlanUiState.ValidationError).errors
        assertTrue(errors.containsKey("kcal"))
        assertTrue(errors.containsKey("protein"))
    }

    @Test
    fun `savePlan with valid input calls repository and emits event`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        val viewModel = PlanViewModel(planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.savePlan("2000", "150.0", "250.0", "65.0")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(PlanUiEvent.PlanSaved, awaitItem())
            coVerify { planRepo.savePlan(2000, 150.0, 250.0, 65.0) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savePlan rejects zero kcal but accepts zero macros`() = runTest {
        every { planRepo.getCurrentPlan() } returns flowOf(null)
        val viewModel = PlanViewModel(planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.savePlan("0", "0.0", "0.0", "0.0")

        val state = viewModel.uiState.value
        assertTrue(state is PlanUiState.ValidationError)
        val errors = (state as PlanUiState.ValidationError).errors
        assertTrue(errors.containsKey("kcal"))
        assertTrue(!errors.containsKey("protein"))
        assertTrue(!errors.containsKey("carbs"))
        assertTrue(!errors.containsKey("fat"))
    }
}
