package com.delve.hungrywalrus.ui.screen.settings

import app.cash.turbine.test
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var apiKeyStore: ApiKeyStore

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        apiKeyStore = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects no key`() = runTest {
        every { apiKeyStore.getApiKey() } returns null

        val viewModel = SettingsViewModel(apiKeyStore)

        assertFalse(viewModel.uiState.value.hasKey)
        assertEquals("", viewModel.uiState.value.keyMasked)
    }

    @Test
    fun `initial state reflects stored key`() = runTest {
        every { apiKeyStore.getApiKey() } returns "my-secret-api-key-12345"

        val viewModel = SettingsViewModel(apiKeyStore)

        assertTrue(viewModel.uiState.value.hasKey)
        assertTrue(viewModel.uiState.value.keyMasked.contains("****"))
    }

    @Test
    fun `saveKey stores key and emits event`() = runTest {
        every { apiKeyStore.getApiKey() } returns null andThen "test-key-1234"

        val viewModel = SettingsViewModel(apiKeyStore)

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

        val viewModel = SettingsViewModel(apiKeyStore)
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

        val viewModel = SettingsViewModel(apiKeyStore)
        viewModel.saveKey("   ")

        verify(exactly = 0) { apiKeyStore.saveApiKey(any()) }
    }
}
