package com.delve.hungrywalrus.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.NutritionPlanRepository
import com.delve.hungrywalrus.domain.model.NutritionPlan
import com.delve.hungrywalrus.util.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasKey: Boolean = false,
    val keyMasked: String = "",
    val planLoading: Boolean = true,
    val currentPlan: NutritionPlan? = null,
    val planValidationErrors: Map<String, String> = emptyMap(),
)

sealed interface SettingsUiEvent {
    data object KeySaved : SettingsUiEvent
    data object KeyCleared : SettingsUiEvent
    data class ReadError(val message: String) : SettingsUiEvent
    data object PlanSaved : SettingsUiEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val planRepo: NutritionPlanRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refreshKeyState()
        viewModelScope.launch {
            planRepo.getCurrentPlan().collect { plan ->
                _uiState.update { it.copy(planLoading = false, currentPlan = plan) }
            }
        }
    }

    private fun refreshKeyState() {
        val key = apiKeyStore.getApiKey()
        _uiState.update { it.copy(
            hasKey = key != null,
            keyMasked = if (key != null) maskKey(key) else "",
        ) }
    }

    fun saveKey(key: String) {
        if (key.isBlank()) return
        apiKeyStore.saveApiKey(key.trim())
        refreshKeyState()
        _events.trySend(SettingsUiEvent.KeySaved)
    }

    fun clearKey() {
        apiKeyStore.clearApiKey()
        refreshKeyState()
        _events.trySend(SettingsUiEvent.KeyCleared)
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
            _uiState.update { it.copy(planValidationErrors = errors) }
            return
        }

        _uiState.update { it.copy(planValidationErrors = emptyMap()) }
        viewModelScope.launch {
            planRepo.savePlan(kcal!!, protein!!, carbs!!, fat!!)
            _events.send(SettingsUiEvent.PlanSaved)
        }
    }

    private fun maskKey(key: String): String {
        return if (key.length > 8) {
            "****${key.takeLast(4)}"
        } else {
            "****"
        }
    }
}
