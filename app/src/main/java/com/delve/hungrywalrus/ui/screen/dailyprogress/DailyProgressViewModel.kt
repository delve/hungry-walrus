package com.delve.hungrywalrus.ui.screen.dailyprogress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    ) : DailyProgressUiState

    data class Error(val message: String) : DailyProgressUiState
}

@HiltViewModel
class DailyProgressViewModel @Inject constructor(
    private val planRepo: NutritionPlanRepository,
    private val logRepo: LogEntryRepository,
) : ViewModel() {

    val uiState: StateFlow<DailyProgressUiState> = combine(
        planRepo.getCurrentPlan(),
        logRepo.getEntriesForDate(LocalDate.now()),
    ) { plan, entries ->
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
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DailyProgressUiState.Loading,
    )

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            logRepo.deleteEntry(id)
        }
    }
}
