package com.delve.hungrywalrus.ui.screen.summaries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.RollingSummary
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class SummaryTab(val label: String, val days: Int) {
    Day7("7 Days", 7),
    Day28("28 Days", 28),
}

sealed interface SummariesUiState {
    data object Loading : SummariesUiState
    data class Content(
        val selectedTab: SummaryTab,
        val summary: RollingSummary,
    ) : SummariesUiState

    data class NoPlan(val selectedTab: SummaryTab, val summary: RollingSummary) : SummariesUiState
}

@HiltViewModel
class SummariesViewModel @Inject constructor(
    private val logRepo: LogEntryRepository,
    private val planRepo: NutritionPlanRepository,
    private val computeSummaryUseCase: ComputeRollingSummaryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SummariesUiState>(SummariesUiState.Loading)
    val uiState: StateFlow<SummariesUiState> = _uiState.asStateFlow()

    private var summaryJob: Job? = null
    private var currentTab = SummaryTab.Day7

    init {
        loadSummary(SummaryTab.Day7)
    }

    fun selectTab(tab: SummaryTab) {
        currentTab = tab
        loadSummary(tab)
    }

    /** Re-loads the current tab's summary. Call from the screen's LaunchedEffect on each visit
     *  so that plan target changes are reflected when the user returns to the Summaries tab. */
    fun reloadSummary() {
        loadSummary(currentTab)
    }

    private fun loadSummary(tab: SummaryTab) {
        summaryJob?.cancel()
        _uiState.value = SummariesUiState.Loading
        summaryJob = viewModelScope.launch {
            val today = LocalDate.now()
            val start = today.minusDays((tab.days - 1).toLong())
            val end = today

            // Build per-day plan map. Days with no plan fall back to the current plan so that
            // a user who set up their plan today still sees meaningful targets for the period.
            val dailyPlans = buildDailyPlans(start, end, today)

            // Collect entries reactively so the summary updates when new entries are logged.
            logRepo.getEntriesForRange(start, end).collect { entries ->
                val summary = computeSummaryUseCase(entries, dailyPlans, start, end)
                _uiState.value = if (summary.totalTarget == null) {
                    SummariesUiState.NoPlan(selectedTab = tab, summary = summary)
                } else {
                    SummariesUiState.Content(selectedTab = tab, summary = summary)
                }
            }
        }
    }

    private suspend fun buildDailyPlans(
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
    ): Map<LocalDate, NutritionPlan?> = coroutineScope {
        val dates = generateSequence(start) { d -> d.plusDays(1).takeUnless { it.isAfter(end) } }.toList()
        val planResults = dates.map { date -> date to async { planRepo.getPlanForDate(date) } }
        // Reuse today's deferred from the dates list as the fallback rather than launching a
        // second concurrent query for the same date. today == end so it is always in dates.
        val todayDeferred = planResults.find { (date, _) -> date == today }?.second
            ?: async { planRepo.getPlanForDate(today) }
        val resolved = todayDeferred.await()
        planResults.associate { (date, deferred) -> date to (deferred.await() ?: resolved) }
    }
}
