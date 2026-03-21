package com.delve.hungrywalrus.ui.screen.summaries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.domain.model.RollingSummary
import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private var currentTab: SummaryTab = SummaryTab.Day7

    init {
        loadSummary(SummaryTab.Day7)
    }

    fun selectTab(tab: SummaryTab) {
        currentTab = tab
        loadSummary(tab)
    }

    private fun loadSummary(tab: SummaryTab) {
        _uiState.value = SummariesUiState.Loading
        viewModelScope.launch {
            val today = LocalDate.now()
            val start = today.minusDays((tab.days - 1).toLong())
            val end = today

            // Load entries for the period
            val entries = logRepo.getEntriesForRange(start, end).first()

            // Load per-day plans
            val dailyPlans = mutableMapOf<LocalDate, NutritionPlan?>()
            var date = start
            while (!date.isAfter(end)) {
                dailyPlans[date] = planRepo.getPlanForDate(date)
                date = date.plusDays(1)
            }

            val summary = computeSummaryUseCase(entries, dailyPlans, start, end)

            _uiState.value = if (summary.totalTarget == null) {
                SummariesUiState.NoPlan(selectedTab = tab, summary = summary)
            } else {
                SummariesUiState.Content(selectedTab = tab, summary = summary)
            }
        }
    }
}
