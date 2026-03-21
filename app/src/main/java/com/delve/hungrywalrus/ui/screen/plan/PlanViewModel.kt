package com.delve.hungrywalrus.ui.screen.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.NutritionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlanUiState {
    data object Loading : PlanUiState
    data class Content(val plan: NutritionPlan?) : PlanUiState
    data object Saved : PlanUiState
    data class ValidationError(val errors: Map<String, String>) : PlanUiState
}

sealed interface PlanUiEvent {
    data object PlanSaved : PlanUiEvent
}

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val planRepo: NutritionPlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlanUiState>(PlanUiState.Loading)
    val uiState: StateFlow<PlanUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlanUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            planRepo.getCurrentPlan().collect { plan ->
                val currentState = _uiState.value
                // Only update to Content if we haven't already saved or if we're still loading
                if (currentState is PlanUiState.Loading || currentState is PlanUiState.Content) {
                    _uiState.value = PlanUiState.Content(plan)
                }
            }
        }
    }

    fun savePlan(kcalStr: String, proteinStr: String, carbsStr: String, fatStr: String) {
        val errors = mutableMapOf<String, String>()

        val kcal = kcalStr.toIntOrNull()
        if (kcal == null || kcal <= 0) errors["kcal"] = "Enter a valid number"

        val protein = proteinStr.toDoubleOrNull()
        if (protein == null || protein < 0) errors["protein"] = "Enter a valid number"

        val carbs = carbsStr.toDoubleOrNull()
        if (carbs == null || carbs < 0) errors["carbs"] = "Enter a valid number"

        val fat = fatStr.toDoubleOrNull()
        if (fat == null || fat < 0) errors["fat"] = "Enter a valid number"

        if (errors.isNotEmpty()) {
            _uiState.value = PlanUiState.ValidationError(errors)
            return
        }

        viewModelScope.launch {
            planRepo.savePlan(kcal!!, protein!!, carbs!!, fat!!)
            _uiState.value = PlanUiState.Saved
            _events.send(PlanUiEvent.PlanSaved)
        }
    }
}
