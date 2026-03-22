package com.delve.hungrywalrus.ui.screen.settings

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiKeyStore: ApiKeyStore
    private lateinit var planRepo: NutritionPlanRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiKeyStore = mockk(relaxed = true)
        planRepo = mockk(relaxed = true)
        every { planRepo.getCurrentPlan() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- API key tests ---

    @Test
    fun `initial state reflects no key`() = runTest {
        every { apiKeyStore.getApiKey() } returns null

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        assertFalse(viewModel.uiState.value.hasKey)
        assertEquals("", viewModel.uiState.value.keyMasked)
    }

    @Test
    fun `initial state reflects stored key`() = runTest {
        every { apiKeyStore.getApiKey() } returns "my-secret-api-key-12345"

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        assertTrue(viewModel.uiState.value.hasKey)
        assertTrue(viewModel.uiState.value.keyMasked.contains("****"))
    }

    @Test
    fun `saveKey stores key and emits event`() = runTest {
        every { apiKeyStore.getApiKey() } returns null andThen "test-key-1234"

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.events.test {
            viewModel.saveKey("test-key-1234")

            val event = awaitItem()
            assertEquals(SettingsUiEvent.KeySaved, event)
            verify { apiKeyStore.saveApiKey("test-key-1234") }
            assertTrue(viewModel.uiState.value.hasKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearKey removes key and emits event`() = runTest {
        every { apiKeyStore.getApiKey() } returns "existing-key" andThen null

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        assertTrue(viewModel.uiState.value.hasKey)

        viewModel.events.test {
            viewModel.clearKey()

            val event = awaitItem()
            assertEquals(SettingsUiEvent.KeyCleared, event)
            verify { apiKeyStore.clearApiKey() }
            assertFalse(viewModel.uiState.value.hasKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveKey with blank string does nothing`() = runTest {
        every { apiKeyStore.getApiKey() } returns null

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.saveKey("   ")

        verify(exactly = 0) { apiKeyStore.saveApiKey(any()) }
    }

    // --- Plan tests ---

    @Test
    fun `initial plan state is loading before getCurrentPlan emits`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.uiState.test {
            assertTrue(awaitItem().planLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentPlan is null when no plan configured`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.planLoading)
        assertNull(viewModel.uiState.value.currentPlan)
    }

    @Test
    fun `currentPlan reflects plan from repository`() = runTest {
        val plan = NutritionPlan(
            id = 1, kcalTarget = 2000, proteinTargetG = 150.0,
            carbsTargetG = 250.0, fatTargetG = 65.0, effectiveFrom = 0L,
        )
        every { planRepo.getCurrentPlan() } returns flowOf(plan)

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(plan, viewModel.uiState.value.currentPlan)
        assertFalse(viewModel.uiState.value.planLoading)
    }

    @Test
    fun `savePlan with valid input calls repository and emits PlanSaved event`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.savePlan("2000", "150.0", "250.0", "65.0")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SettingsUiEvent.PlanSaved, awaitItem())
            coVerify { planRepo.savePlan(2000, 150.0, 250.0, 65.0) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savePlan with invalid input sets planValidationErrors`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.savePlan("", "abc", "250.0", "65.0")

        val errors = viewModel.uiState.value.planValidationErrors
        assertTrue(errors.containsKey("kcal"))
        assertTrue(errors.containsKey("protein"))
        assertFalse(errors.containsKey("carbs"))
        assertFalse(errors.containsKey("fat"))
    }

    @Test
    fun `savePlan rejects zero kcal`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.savePlan("0", "150", "250", "65")

        val errors = viewModel.uiState.value.planValidationErrors
        assertTrue(errors.containsKey("kcal"))
    }

    @Test
    fun `savePlan rejects negative kcal`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.savePlan("-100", "150", "250", "65")

        assertTrue(viewModel.uiState.value.planValidationErrors.containsKey("kcal"))
    }

    @Test
    fun `savePlan accepts zero for macros`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.savePlan("2000", "0", "0", "0")
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(SettingsUiEvent.PlanSaved, awaitItem())
            assertTrue(viewModel.uiState.value.planValidationErrors.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `savePlan rejects empty string for all fields`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.savePlan("", "", "", "")

        val errors = viewModel.uiState.value.planValidationErrors
        assertEquals(4, errors.size)
    }

    @Test
    fun `savePlan rejects non-numeric input for macros`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        viewModel.savePlan("2000", "abc", "def", "xyz")

        val errors = viewModel.uiState.value.planValidationErrors
        assertTrue(errors.containsKey("protein"))
        assertTrue(errors.containsKey("carbs"))
        assertTrue(errors.containsKey("fat"))
        assertFalse(errors.containsKey("kcal"))
    }

    @Test
    fun `savePlan clears validation errors on next valid save`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.savePlan("", "", "", "")
        assertTrue(viewModel.uiState.value.planValidationErrors.isNotEmpty())

        viewModel.events.test {
            viewModel.savePlan("2000", "150.0", "250.0", "65.0")
            testDispatcher.scheduler.advanceUntilIdle()

            awaitItem() // PlanSaved event
            assertTrue(viewModel.uiState.value.planValidationErrors.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
