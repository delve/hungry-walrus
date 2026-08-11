package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.OfflineException
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.ui.screen.addentry.SearchState
import com.delve.hungrywalrus.util.ApiKeyStore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
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

/**
 * QA unit tests for [AddEntryViewModel] error-handling paths in [searchUsda] and [searchOff]
 * that are not covered by the existing developer test suite.
 *
 * Gaps filled:
 *
 * 1. [AddEntryViewModel.searchUsda] with [OfflineException] — the ViewModel uses a specific
 *    error message branch ("No internet connection. Search is unavailable.") for offline
 *    failures. The existing [AddEntryViewModelTest.`searchUsda failure sets error state`]
 *    test exercises only the generic `RuntimeException` branch, which produces the
 *    exception's `message` string directly. The offline branch is untested.
 *
 * 2. [AddEntryViewModel.searchOff] with [OfflineException] — same gap as #1.
 *
 * 3. [AddEntryViewModel.searchUsda] with empty results — the `SearchState.NoResults` path
 *    for USDA search is untested. (The NoResults path for OFF search is tested in
 *    [AddEntryViewModelTest] but not for USDA.)
 *
 * 4. [AddEntryViewModel.searchOff] with blank query — an early-return guard prevents
 *    a network call when the query string is blank/empty. The equivalent test for USDA
 *    ("`searchUsda with blank query is ignored`") exists in [AddEntryViewModelTest]; no
 *    analogous test exists for OFF search.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchErrorHandlingQaTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logRepo: LogEntryRepository
    private lateinit var foodLookupRepo: FoodLookupRepository
    private lateinit var recipeRepo: RecipeRepository
    private val scaleUseCase = ScaleNutritionUseCase()
    private val validateUseCase = ValidateFoodDataUseCase()
    private lateinit var apiKeyStore: ApiKeyStore

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logRepo = mockk(relaxed = true)
        foodLookupRepo = mockk()
        recipeRepo = mockk()
        apiKeyStore = mockk()
        every { apiKeyStore.hasApiKey() } returns false
        every { apiKeyStore.getApiKey() } returns null
        every { recipeRepo.getAllRecipes() } returns flowOf(emptyList())
        coEvery { foodLookupRepo.cacheItem(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AddEntryViewModel(
        logRepo, foodLookupRepo, recipeRepo, scaleUseCase, validateUseCase, apiKeyStore,
    )

    // ---- searchUsda with OfflineException ----

    /**
     * Spec: When the USDA search fails due to network unavailability ([OfflineException]),
     * the ViewModel must set [SearchState.Error] and provide the message
     * "No internet connection. Search is unavailable."
     *
     * The existing developer test exercises the generic error branch (any RuntimeException
     * message is forwarded directly). This test verifies the specific offline branch that
     * overrides the exception message with a user-friendly string.
     */
    @Test
    fun `searchUsda with OfflineException sets Error state with offline message`() = runTest {
        coEvery { foodLookupRepo.searchUsda("chicken") } returns
            Result.failure(OfflineException("Connection timed out"))

        val viewModel = createViewModel()
        viewModel.searchUsda("chicken")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Error, state.searchState)
        assertTrue(
            "Expected 'No internet connection' in the error message but got: '${state.searchErrorMessage}'",
            state.searchErrorMessage.contains("No internet connection", ignoreCase = true),
        )
        assertTrue(
            "Expected 'unavailable' in the error message but got: '${state.searchErrorMessage}'",
            state.searchErrorMessage.contains("unavailable", ignoreCase = true),
        )
    }

    // ---- searchOff with OfflineException ----

    /**
     * Spec: When the Open Food Facts search fails due to network unavailability
     * ([OfflineException]), the ViewModel must set [SearchState.Error] and provide the
     * message "No internet connection. Search is unavailable."
     *
     * Neither the developer tests nor the existing QA tests cover this path for [searchOff].
     */
    @Test
    fun `searchOff with OfflineException sets Error state with offline message`() = runTest {
        coEvery { foodLookupRepo.searchOpenFoodFacts("oats") } returns
            Result.failure(OfflineException("No network"))

        val viewModel = createViewModel()
        viewModel.searchOff("oats")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Error, state.searchState)
        assertTrue(
            "Expected 'No internet connection' in the error message but got: '${state.searchErrorMessage}'",
            state.searchErrorMessage.contains("No internet connection", ignoreCase = true),
        )
        assertTrue(
            "Expected 'unavailable' in the error message but got: '${state.searchErrorMessage}'",
            state.searchErrorMessage.contains("unavailable", ignoreCase = true),
        )
    }

    // ---- searchUsda with empty results ----

    /**
     * Spec: A USDA search that returns an empty result list should set [SearchState.NoResults]
     * so the UI can show "No results found."
     *
     * The equivalent test for OFF search (searchOff with no results) exists in the developer
     * test suite, but there is no corresponding test for the USDA path.
     */
    @Test
    fun `searchUsda with empty result list sets NoResults state`() = runTest {
        coEvery { foodLookupRepo.searchUsda("zzzyyyxxx") } returns
            Result.success(emptyList())

        val viewModel = createViewModel()
        viewModel.searchUsda("zzzyyyxxx")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(SearchState.NoResults, viewModel.uiState.value.searchState)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    // ---- searchOff blank-query guard ----

    /**
     * Spec: [AddEntryViewModel.searchOff] contains `if (query.isBlank()) return` as a guard
     * to prevent unnecessary network calls when the user has not typed a query. The ViewModel
     * must remain in [SearchState.Idle] and must not call [FoodLookupRepository.searchOpenFoodFacts].
     *
     * The analogous test for USDA search exists in the developer suite but there is no
     * equivalent for OFF search.
     */
    @Test
    fun `searchOff with blank query returns early without changing state`() = runTest {
        val viewModel = createViewModel()

        // Blank query — no network call should be issued
        viewModel.searchOff("")

        assertEquals(SearchState.Idle, viewModel.uiState.value.searchState)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    /**
     * Whitespace-only query is also blank and must be ignored.
     */
    @Test
    fun `searchOff with whitespace-only query returns early without changing state`() = runTest {
        val viewModel = createViewModel()
        viewModel.searchOff("   ")

        assertEquals(SearchState.Idle, viewModel.uiState.value.searchState)
    }

    /**
     * A generic (non-OfflineException) failure from USDA search propagates the exception
     * message directly. This verifies the fallback branch behaves correctly — distinct from
     * the OfflineException branch verified above.
     */
    @Test
    fun `searchUsda with generic RuntimeException uses exception message in error state`() = runTest {
        coEvery { foodLookupRepo.searchUsda("test") } returns
            Result.failure(RuntimeException("Invalid API key provided"))

        val viewModel = createViewModel()
        viewModel.searchUsda("test")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Error, state.searchState)
        assertEquals("Invalid API key provided", state.searchErrorMessage)
    }

    /**
     * A generic failure from OFF search propagates the exception message directly.
     */
    @Test
    fun `searchOff with generic RuntimeException uses exception message in error state`() = runTest {
        coEvery { foodLookupRepo.searchOpenFoodFacts("test") } returns
            Result.failure(RuntimeException("Service unavailable"))

        val viewModel = createViewModel()
        viewModel.searchOff("test")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchState.Error, state.searchState)
        assertEquals("Service unavailable", state.searchErrorMessage)
    }
}
