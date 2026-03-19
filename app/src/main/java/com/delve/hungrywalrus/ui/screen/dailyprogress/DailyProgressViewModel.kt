package com.delve.hungrywalrus.ui.screen.dailyprogress

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

sealed interface DailyProgressUiState {
    data object Loading : DailyProgressUiState
    data class Content(
        val plan: NutritionPlan?,
        val entries: List<LogEntry>,
        val totalKcal: Double,
        val totalProteinG: Double,
        val totalCarbsG: Double,
        val totalFatG: Double,
        val displayDate: LocalDate,
    ) : DailyProgressUiState

    data class Error(val message: String) : DailyProgressUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyProgressViewModel @Inject constructor(
    private val planRepo: NutritionPlanRepository,
    private val logRepo: LogEntryRepository,
) : ViewModel() {

    /**
     * Provides the current date. Only overridden in tests to control the date
     * without mocking static methods. In production this always returns
     * [LocalDate.now].
     */
    @VisibleForTesting
    internal var todayProvider: () -> LocalDate = { LocalDate.now() }

    private val currentDate = MutableStateFlow(todayProvider())

    // Pair the date with its entries so that combine receives them atomically,
    // avoiding intermediate states where the date has changed but entries have not.
    private val datedEntries = currentDate.flatMapLatest { date ->
        logRepo.getEntriesForDate(date).map { entries -> date to entries }
    }

    val uiState: StateFlow<DailyProgressUiState> = combine(
        planRepo.getCurrentPlan(),
        datedEntries,
    ) { plan, (date, entries) ->
        val totalKcal = entries.sumOf { it.kcal }
        val totalProtein = entries.sumOf { it.proteinG }
        val totalCarbs = entries.sumOf { it.carbsG }
        val totalFat = entries.sumOf { it.fatG }
        DailyProgressUiState.Content(
            plan = plan,
            entries = entries.sortedByDescending { it.timestamp },
            totalKcal = totalKcal,
            totalProteinG = totalProtein,
            totalCarbsG = totalCarbs,
            totalFatG = totalFat,
            displayDate = date,
        ) as DailyProgressUiState
    }.retry(RETRY_COUNT) { e ->
        // Only retry I/O errors (e.g. Room database hiccup). Programming errors
        // such as IllegalArgumentException or NullPointerException should not be
        // retried. CancellationException is already rethrown by the retry operator
        // before reaching this predicate, but restricting to IOException makes the
        // intent explicit and avoids suppressing unexpected exception types.
        e is IOException
    }.catch { e ->
        emit(DailyProgressUiState.Error(
            e.message ?: "Could not load data. Please restart the app."
        ))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyProgressUiState.Loading,
    )

    fun refreshDate() {
        currentDate.value = todayProvider()
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            logRepo.deleteEntry(id)
        }
    }

    companion object {
        /** Number of times to retry upstream flow errors before emitting Error state. */
        @VisibleForTesting
        internal const val RETRY_COUNT = 2L
    }
}
