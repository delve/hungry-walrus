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
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Local-time hour at and after which the rolling summary window includes today.
 * Architecture §7.6 — defined as a named constant rather than inlined so that any
 * future change happens in one place. The product owner has indicated this is a
 * fixed value in v1 and is not user-configurable.
 */
const val SUMMARY_CUTOFF_HOUR: Int = 20

enum class SummaryTab(val label: String, val days: Int) {
    Day7("7 Days", 7),
    Day28("28 Days", 28),
}

sealed interface SummariesUiState {
    data object Loading : SummariesUiState

    /**
     * Common fields exposed by states that carry a [RollingSummary]. Both [Content]
     * and [NoPlan] expose [includesToday] so the UI can render the rolling-window
     * hint consistently (design §3.14 element 4, component §5.8).
     */
    sealed interface WithSummary : SummariesUiState {
        val selectedTab: SummaryTab
        val summary: RollingSummary
        val includesToday: Boolean
    }

    data class Content(
        override val selectedTab: SummaryTab,
        override val summary: RollingSummary,
        override val includesToday: Boolean,
    ) : WithSummary

    data class NoPlan(
        override val selectedTab: SummaryTab,
        override val summary: RollingSummary,
        override val includesToday: Boolean,
    ) : WithSummary
}

/**
 * ViewModel for the Summaries screen.
 *
 * Rolling window (architecture §7.6, requirements rev 1):
 *   - Before [SUMMARY_CUTOFF_HOUR] (20:00) local time, today is excluded from the
 *     period; the window ends at end-of-yesterday.
 *   - From 20:00 onward, today is included; the window ends at end-of-today.
 *
 * Both window edges are recomputed on every visit via [reloadSummary] so that
 * crossing the cutoff or logging entries from another tab is reflected immediately.
 *
 * "Now" is read from the injected [Clock] so unit tests can deterministically verify
 * behaviour at 19:59 vs 20:00 vs 20:01. The clock's zone is implicitly honoured by
 * `LocalDate.now(clock)` and `LocalTime.now(clock)`.
 */
@HiltViewModel
class SummariesViewModel @Inject constructor(
    private val logRepo: LogEntryRepository,
    private val planRepo: NutritionPlanRepository,
    private val computeSummaryUseCase: ComputeRollingSummaryUseCase,
    private val clock: Clock,
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

    /**
     * Re-loads the current tab's summary. Call from the screen's LaunchedEffect on
     * each visit so that plan changes, new entries, and crossing the 20:00 cutoff
     * are all reflected (architecture §7.5, §7.6).
     */
    fun reloadSummary() {
        loadSummary(currentTab)
    }

    private fun loadSummary(tab: SummaryTab) {
        summaryJob?.cancel()
        _uiState.value = SummariesUiState.Loading
        summaryJob = viewModelScope.launch {
            // Compute window endpoints. endDate depends on the 20:00 cutoff and is
            // recomputed on every load to handle crossing the boundary mid-session.
            val today = LocalDate.now(clock)
            val nowTime = LocalTime.now(clock)
            val includesToday = nowTime.hour >= SUMMARY_CUTOFF_HOUR
            val end = if (includesToday) today else today.minusDays(1)
            val start = end.minusDays((tab.days - 1).toLong())

            // Today is only used as a fallback when the user has just set up a plan
            // and we are summarising historical days that pre-date its effectiveFrom.
            val fallbackAnchor = today

            val dailyPlans = buildDailyPlans(start, end, fallbackAnchor)

            // Collect entries reactively so the summary updates when new entries
            // are logged within the same screen visit.
            logRepo.getEntriesForRange(start, end).collect { entries ->
                val summary = computeSummaryUseCase(entries, dailyPlans, start, end)
                _uiState.value = if (summary.totalTarget == null) {
                    SummariesUiState.NoPlan(
                        selectedTab = tab,
                        summary = summary,
                        includesToday = includesToday,
                    )
                } else {
                    SummariesUiState.Content(
                        selectedTab = tab,
                        summary = summary,
                        includesToday = includesToday,
                    )
                }
            }
        }
    }

    /**
     * Builds a per-day plan map across [start]..[end] inclusive. Days where the
     * plan repository returns null fall back to the plan effective on
     * [fallbackAnchor] (today). This means a user who created their plan today
     * still sees meaningful targets for the period.
     */
    private suspend fun buildDailyPlans(
        start: LocalDate,
        end: LocalDate,
        fallbackAnchor: LocalDate,
    ): Map<LocalDate, NutritionPlan?> = coroutineScope {
        val dates = generateSequence(start) { d ->
            d.plusDays(1).takeUnless { it.isAfter(end) }
        }.toList()
        val planResults = dates.map { date -> date to async { planRepo.getPlanForDate(date) } }
        // Avoid a redundant query when fallbackAnchor is already in the dates list.
        val anchorDeferred = planResults.find { (date, _) -> date == fallbackAnchor }?.second
            ?: async { planRepo.getPlanForDate(fallbackAnchor) }
        val resolvedAnchor = anchorDeferred.await()
        planResults.associate { (date, deferred) -> date to (deferred.await() ?: resolvedAnchor) }
    }
}
