package com.delve.hungrywalrus.qa

import app.cash.turbine.test
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.ui.screen.settings.SettingsUiEvent
import com.delve.hungrywalrus.ui.screen.settings.SettingsViewModel
import com.delve.hungrywalrus.util.ApiKeyStore
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * QA unit tests for [SettingsViewModel] covering gaps in the existing test suite:
 *
 * - maskKey behaviour for short keys (8 chars or fewer always shows "****" with no suffix).
 * - savePlan rejects negative macronutrient values (spec: macros must be >= 0).
 * - savePlan rejects negative fat value specifically.
 * - saveKey trims leading/trailing whitespace before storing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelQaTest {

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

    // --- maskKey edge cases ---

    /**
     * Spec: The masked key is shown in Settings so users can confirm a key is saved.
     * Keys with 8 or fewer characters should render as "****" (no suffix visible).
     */
    @Test
    fun `short API key (8 chars) is fully masked as four asterisks`() = runTest {
        // 8-character key -- maskKey returns "****" for length <= 8
        every { apiKeyStore.getApiKey() } returns "12345678"

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        assertEquals("****", viewModel.uiState.value.keyMasked)
        assertTrue(viewModel.uiState.value.hasKey)
    }

    @Test
    fun `API key longer than 8 chars shows last 4 digits after asterisks`() = runTest {
        // 12-character key -- maskKey returns "****" + last 4 chars
        every { apiKeyStore.getApiKey() } returns "ABCDEFGH5678"

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        assertEquals("****5678", viewModel.uiState.value.keyMasked)
        assertTrue(viewModel.uiState.value.hasKey)
    }

    @Test
    fun `single character API key is fully masked as four asterisks`() = runTest {
        every { apiKeyStore.getApiKey() } returns "X"

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        assertEquals("****", viewModel.uiState.value.keyMasked)
        assertTrue(viewModel.uiState.value.hasKey)
    }

    // --- savePlan negative macro rejection ---

    /**
     * Spec (requirements.md): "Macronutrient targets (protein, carbohydrates, fat) must be
     * zero or greater." Negative values must be rejected.
     */
    @Test
    fun `savePlan rejects negative protein value`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.savePlan("2000", "-1", "250", "65")

        assertTrue(viewModel.uiState.value.planValidationErrors.containsKey("protein"))
        assertFalse(viewModel.uiState.value.planValidationErrors.containsKey("carbs"))
        assertFalse(viewModel.uiState.value.planValidationErrors.containsKey("fat"))
    }

    @Test
    fun `savePlan rejects negative carbohydrate value`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.savePlan("2000", "150", "-0.5", "65")

        assertTrue(viewModel.uiState.value.planValidationErrors.containsKey("carbs"))
        assertFalse(viewModel.uiState.value.planValidationErrors.containsKey("protein"))
    }

    @Test
    fun `savePlan rejects negative fat value`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.savePlan("2000", "150", "250", "-10")

        assertTrue(viewModel.uiState.value.planValidationErrors.containsKey("fat"))
    }

    @Test
    fun `savePlan accepts zero for all macros (zero-fat target is valid)`() = runTest {
        val viewModel = SettingsViewModel(apiKeyStore, planRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            viewModel.savePlan("1500", "0.0", "0.0", "0.0")
            testDispatcher.scheduler.advanceUntilIdle()

            // PlanSaved event must arrive -- zero macros are valid
            assertEquals(SettingsUiEvent.PlanSaved, awaitItem())
            assertTrue(viewModel.uiState.value.planValidationErrors.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- saveKey trims whitespace ---

    /**
     * Spec: API keys are sensitive values. The ViewModel already trims before saving
     * (key.trim()). This test verifies that behaviour is preserved.
     */
    @Test
    fun `saveKey trims surrounding whitespace before storing`() = runTest {
        every { apiKeyStore.getApiKey() } returns null andThen "trimmedkey"

        val viewModel = SettingsViewModel(apiKeyStore, planRepo)

        viewModel.events.test {
            viewModel.saveKey("  trimmedkey  ")
            val event = awaitItem()
            assertEquals(SettingsUiEvent.KeySaved, event)

            // Verify the trimmed value was passed to the store
            io.mockk.verify { apiKeyStore.saveApiKey("trimmedkey") }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
