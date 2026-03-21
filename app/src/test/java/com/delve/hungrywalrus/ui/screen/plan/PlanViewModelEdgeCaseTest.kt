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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Additional edge-case unit tests for [PlanViewModel] covering validation and event edge cases
 * not fully covered by PlanViewModelTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlanViewModelEdgeCaseTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var planRepo: NutritionPlanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = mockk(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `savePlan rejects zero kcal`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.savePlan("0", "150", "250", "65")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is PlanUiState.ValidationError)
        val errors = (state as PlanUiState.ValidationError).errors
        assertTrue("kcal" in errors)
    }

    @Test
    fun `savePlan rejects negative kcal`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.savePlan("-100", "150", "250", "65")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is PlanUiState.ValidationError)
    }

    @Test
    fun `savePlan rejects empty string for all fields`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.savePlan("", "", "", "")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is PlanUiState.ValidationError)
        val errors = (state as PlanUiState.ValidationError).errors
        assertEquals(4, errors.size)
    }

    @Test
    fun `savePlan rejects non-numeric input for macros`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.savePlan("2000", "abc", "def", "xyz")
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is PlanUiState.ValidationError)
        val errors = (state as PlanUiState.ValidationError).errors
        assertTrue("protein" in errors)
        assertTrue("carbs" in errors)
        assertTrue("fat" in errors)
    }

    @Test
    fun `savePlan accepts zero for protein, carbs, and fat (edge of valid range)`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.savePlan("2000", "0", "0", "0")
        testDispatcher.scheduler.advanceUntilIdle()
        // Zero is a valid (if unusual) plan — should not produce a validation error
        val state = viewModel.uiState.value
        assertTrue(
            "Expected Saved state but got $state",
            state is PlanUiState.Saved,
        )
    }

    @Test
    fun `savePlan calls repository with correct parsed values`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.savePlan("2000", "150.5", "250.0", "65.25")
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { planRepo.savePlan(2000, 150.5, 250.0, 65.25) }
    }

    @Test
    fun `savePlan emits PlanSaved event after successful save`() = runTest {
        val viewModel = PlanViewModel(planRepo)

        viewModel.events.test {
            viewModel.savePlan("2000", "150", "250", "65")
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(PlanUiEvent.PlanSaved, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state is Loading before getCurrentPlan emits`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.uiState.test {
            assertEquals(PlanUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Content state reflects current plan from repository`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 1800, proteinTargetG = 120.0,
            carbsTargetG = 200.0, fatTargetG = 60.0, effectiveFrom = 0L,
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
    fun `Content state plan is null when no plan configured`() = runTest {
        val viewModel = PlanViewModel(planRepo)
        viewModel.uiState.test {
            assertEquals(PlanUiState.Loading, awaitItem())
            val content = awaitItem() as PlanUiState.Content
            assertNull(content.plan)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
