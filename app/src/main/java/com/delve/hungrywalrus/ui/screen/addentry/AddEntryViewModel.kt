package com.delve.hungrywalrus.ui.screen.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.OfflineException
import com.delve.hungrywalrus.domain.model.FoodSearchResult
import com.delve.hungrywalrus.domain.model.FoodSource
import com.delve.hungrywalrus.domain.model.LogEntry
import com.delve.hungrywalrus.domain.model.NutritionValues
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import com.delve.hungrywalrus.domain.usecase.ValidateFoodDataUseCase
import com.delve.hungrywalrus.util.ApiKeyStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchState {
    Idle,
    Loading,
    Results,
    NoResults,
    Error,
}

data class AddEntryUiState(
    val searchQuery: String = "",
    val searchResults: List<FoodSearchResult> = emptyList(),
    val searchState: SearchState = SearchState.Idle,
    val searchErrorMessage: String = "",
    val selectedFood: FoodSearchResult? = null,
    val selectedRecipe: Recipe? = null,
    val weightG: String = "",
    val scaledNutrition: NutritionValues? = null,
    val isSaving: Boolean = false,
    val hasUsdaKey: Boolean = false,
    val isRecipeSource: Boolean = false,
    val foodName: String = "",
    val ingredientMode: Boolean = false,
)

sealed interface AddEntryUiEvent {
    data object NavigateToWeightEntry : AddEntryUiEvent
    data object NavigateToMissingValues : AddEntryUiEvent
    data object NavigateToConfirm : AddEntryUiEvent
    data object EntrySaved : AddEntryUiEvent
    data class IngredientReady(
        val name: String,
        val weightG: Double,
        val kcalPer100g: Double,
        val proteinPer100g: Double,
        val carbsPer100g: Double,
        val fatPer100g: Double,
    ) : AddEntryUiEvent
    data class BarcodeResult(val found: Boolean, val barcode: String, val isError: Boolean = false) : AddEntryUiEvent
}

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val logRepo: LogEntryRepository,
    private val foodLookupRepo: FoodLookupRepository,
    private val recipeRepo: RecipeRepository,
    private val scaleUseCase: ScaleNutritionUseCase,
    private val validateUseCase: ValidateFoodDataUseCase,
    private val apiKeyStore: ApiKeyStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()

    val recipes = recipeRepo.getAllRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = Channel<AddEntryUiEvent>(Channel.BUFFERED)
    val events: Flow<AddEntryUiEvent> = _events.receiveAsFlow()


    init {
        _uiState.value = _uiState.value.copy(hasUsdaKey = apiKeyStore.hasApiKey())
    }

    fun setIngredientMode(mode: Boolean) {
        _uiState.value = _uiState.value.copy(ingredientMode = mode)
    }

    fun refreshUsdaKeyStatus() {
        _uiState.value = _uiState.value.copy(hasUsdaKey = apiKeyStore.hasApiKey())
    }

    fun saveApiKey(key: String) {
        if (key.isNotBlank()) {
            apiKeyStore.saveApiKey(key)
            _uiState.value = _uiState.value.copy(hasUsdaKey = apiKeyStore.hasApiKey())
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun searchUsda(query: String) {
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(
            searchState = SearchState.Loading,
            searchQuery = query,
        )
        viewModelScope.launch {
            val result = foodLookupRepo.searchUsda(query)
            result.fold(
                onSuccess = { results ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = results,
                        searchState = if (results.isEmpty()) SearchState.NoResults else SearchState.Results,
                    )
                },
                onFailure = { error ->
                    val message = when (error) {
                        is OfflineException -> "No internet connection. Search is unavailable."
                        else -> error.message ?: "Search failed. Please try again."
                    }
                    _uiState.value = _uiState.value.copy(
                        searchState = SearchState.Error,
                        searchErrorMessage = message,
                    )
                },
            )
        }
    }

    fun searchOff(query: String) {
        if (query.isBlank()) return
        _uiState.value = _uiState.value.copy(
            searchState = SearchState.Loading,
            searchQuery = query,
        )
        viewModelScope.launch {
            val result = foodLookupRepo.searchOpenFoodFacts(query)
            result.fold(
                onSuccess = { results ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = results,
                        searchState = if (results.isEmpty()) SearchState.NoResults else SearchState.Results,
                    )
                },
                onFailure = { error ->
                    val message = when (error) {
                        is OfflineException -> "No internet connection. Search is unavailable."
                        else -> error.message ?: "Search failed. Please try again."
                    }
                    _uiState.value = _uiState.value.copy(
                        searchState = SearchState.Error,
                        searchErrorMessage = message,
                    )
                },
            )
        }
    }

    /**
     * Select a food from search results. Returns true if the food has missing values.
     */
    fun selectFood(result: FoodSearchResult): Boolean {
        _uiState.value = _uiState.value.copy(
            selectedFood = result,
            selectedRecipe = null,
            isRecipeSource = false,
            foodName = result.name,
            weightG = "",
            scaledNutrition = null,
        )
        return !validateUseCase.isComplete(result)
    }

    fun selectRecipe(recipe: Recipe) {
        _uiState.value = _uiState.value.copy(
            selectedFood = null,
            selectedRecipe = recipe,
            isRecipeSource = true,
            foodName = recipe.name,
            weightG = "",
            scaledNutrition = null,
        )
    }

    fun setManualFood(
        name: String,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
    ) {
        val result = FoodSearchResult(
            id = "manual_${System.currentTimeMillis()}",
            name = name,
            source = FoodSource.MANUAL,
            kcalPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g,
            missingFields = emptySet(),
        )
        _uiState.value = _uiState.value.copy(
            selectedFood = result,
            selectedRecipe = null,
            isRecipeSource = false,
            foodName = name,
            weightG = "",
            scaledNutrition = null,
        )
    }

    fun setWeight(weight: String) {
        val weightVal = weight.toDoubleOrNull()
        _uiState.value = _uiState.value.copy(weightG = weight)

        if (weightVal == null || weightVal <= 0) {
            _uiState.value = _uiState.value.copy(scaledNutrition = null)
            return
        }

        val state = _uiState.value
        val scaled = when {
            state.selectedRecipe != null -> {
                scaleUseCase.scaleRecipePortion(state.selectedRecipe, weightVal)
            }
            state.selectedFood != null -> {
                val food = state.selectedFood
                if (food.kcalPer100g != null && food.proteinPer100g != null &&
                    food.carbsPer100g != null && food.fatPer100g != null
                ) {
                    scaleUseCase(
                        food.kcalPer100g,
                        food.proteinPer100g,
                        food.carbsPer100g,
                        food.fatPer100g,
                        weightVal,
                    )
                } else null
            }
            else -> null
        }

        _uiState.value = _uiState.value.copy(scaledNutrition = scaled)
    }

    fun applyMissingValues(
        kcal: Double?,
        protein: Double?,
        carbs: Double?,
        fat: Double?,
    ) {
        val food = _uiState.value.selectedFood ?: return
        val updated = validateUseCase.applyOverrides(food, kcal, protein, carbs, fat)
        _uiState.value = _uiState.value.copy(selectedFood = updated)
    }

    fun lookupBarcode(barcode: String) {
        _uiState.value = _uiState.value.copy(searchState = SearchState.Loading)
        viewModelScope.launch {
            val result = foodLookupRepo.lookupBarcode(barcode)
            result.fold(
                onSuccess = { food ->
                    if (food != null) {
                        _uiState.value = _uiState.value.copy(
                            selectedFood = food,
                            selectedRecipe = null,
                            isRecipeSource = false,
                            foodName = food.name,
                            searchState = SearchState.Idle,
                        )
                        _events.send(AddEntryUiEvent.BarcodeResult(found = true, barcode = barcode))
                    } else {
                        _uiState.value = _uiState.value.copy(searchState = SearchState.Idle)
                        _events.send(AddEntryUiEvent.BarcodeResult(found = false, barcode = barcode))
                    }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is OfflineException -> "No internet connection. Cannot look up barcode."
                        else -> error.message ?: "Failed to look up barcode."
                    }
                    _uiState.value = _uiState.value.copy(
                        searchState = SearchState.Error,
                        searchErrorMessage = message,
                    )
                    _events.send(AddEntryUiEvent.BarcodeResult(found = false, barcode = barcode, isError = true))
                },
            )
        }
    }

    fun saveEntry() {
        val state = _uiState.value
        val scaled = state.scaledNutrition ?: return
        val name = state.foodName.ifBlank { "Unknown food" }

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val entry = LogEntry(
                foodName = name,
                kcal = scaled.kcal,
                proteinG = scaled.proteinG,
                carbsG = scaled.carbsG,
                fatG = scaled.fatG,
                timestamp = System.currentTimeMillis(),
            )
            logRepo.addEntry(entry)
            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.send(AddEntryUiEvent.EntrySaved)
        }
    }

    fun getIngredientData(): AddEntryUiEvent.IngredientReady? {
        val state = _uiState.value
        val weightVal = state.weightG.toDoubleOrNull() ?: return null

        return when {
            state.selectedRecipe != null -> {
                // For recipes, convert to per-100g equivalent
                val recipe = state.selectedRecipe
                if (recipe.totalWeightG > 0) {
                    AddEntryUiEvent.IngredientReady(
                        name = recipe.name,
                        weightG = weightVal,
                        kcalPer100g = (recipe.totalKcal / recipe.totalWeightG) * 100.0,
                        proteinPer100g = (recipe.totalProteinG / recipe.totalWeightG) * 100.0,
                        carbsPer100g = (recipe.totalCarbsG / recipe.totalWeightG) * 100.0,
                        fatPer100g = (recipe.totalFatG / recipe.totalWeightG) * 100.0,
                    )
                } else null
            }
            state.selectedFood != null -> {
                val food = state.selectedFood
                if (food.kcalPer100g != null && food.proteinPer100g != null &&
                    food.carbsPer100g != null && food.fatPer100g != null
                ) {
                    AddEntryUiEvent.IngredientReady(
                        name = food.name,
                        weightG = weightVal,
                        kcalPer100g = food.kcalPer100g,
                        proteinPer100g = food.proteinPer100g,
                        carbsPer100g = food.carbsPer100g,
                        fatPer100g = food.fatPer100g,
                    )
                } else null
            }
            else -> null
        }
    }

    fun resetState() {
        _uiState.value = AddEntryUiState(hasUsdaKey = apiKeyStore.hasApiKey())
    }
}
