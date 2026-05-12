package com.delve.hungrywalrus.ui.screen.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.FoodLookupRepository
import com.delve.hungrywalrus.data.repository.LogEntryRepository
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.OfflineException
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
import kotlinx.coroutines.flow.map
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

sealed interface RecipesState {
    data object Loading : RecipesState
    data class Loaded(val recipes: List<Recipe>) : RecipesState
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

    val recipes: StateFlow<RecipesState> = recipeRepo.getAllRecipes()
        .map { RecipesState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipesState.Loading)

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
     *
     * Per architecture §6.2 item 2, an API-sourced food item is cached when the user
     * selects it and its per-100g data is resolved. We honour that contract here:
     * if the selected food is from USDA or Open Food Facts and has no missing fields,
     * its values are written to [FoodLookupRepository.cacheItem] so subsequent lookups
     * of the same `cacheKey` can be served locally for up to 30 days
     * (architecture §5.2). Items with missing fields are deferred until
     * [applyMissingValues] completes them, at which point that method performs the
     * cache write.
     *
     * Manual entries are not cached because their id ("manual_{timestamp}") is not a
     * stable cache key and manual entries have no API source to deduplicate against.
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
        val complete = validateUseCase.isComplete(result)
        if (complete && shouldCache(result)) {
            cacheSelectedFood(result)
        }
        return !complete
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

    /**
     * Sets direct consumption values entered by the user on the manual-entry screen.
     * For regular logging, per-100g fields store the consumed values and [weight] defaults to
     * "100" so that scaling maths produce the entered values unchanged (x * 100 / 100 = x).
     * For ingredient mode, the caller supplies the actual ingredient weight so that
     * [getIngredientData] reads the correct weight without a separate [setWeight] call.
     */
    fun setDirectEntry(
        name: String,
        kcal: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        weight: String = "100",
    ) {
        val result = FoodSearchResult(
            id = "manual_${System.currentTimeMillis()}",
            name = name,
            source = FoodSource.MANUAL,
            kcalPer100g = kcal,
            proteinPer100g = proteinG,
            carbsPer100g = carbsG,
            fatPer100g = fatG,
            missingFields = emptySet(),
        )
        val weightVal = weight.toDoubleOrNull() ?: 100.0
        _uiState.value = _uiState.value.copy(
            selectedFood = result,
            selectedRecipe = null,
            isRecipeSource = false,
            foodName = name,
            weightG = weight,
            scaledNutrition = NutritionValues(
                kcal = kcal * weightVal / 100.0,
                proteinG = proteinG * weightVal / 100.0,
                carbsG = carbsG * weightVal / 100.0,
                fatG = fatG * weightVal / 100.0,
            ),
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

    /**
     * Apply user-supplied overrides for any missing nutritional fields on the currently
     * selected food. If the result becomes complete and the food source is API-sourced
     * (USDA or Open Food Facts), the resolved item is cached via
     * [FoodLookupRepository.cacheItem] per architecture §6.2 item 2 (cache "when the
     * user selects a specific item and its per-100g data is resolved"). Manual-source
     * items are not cached for the same reasons described on [selectFood].
     */
    fun applyMissingValues(
        kcal: Double?,
        protein: Double?,
        carbs: Double?,
        fat: Double?,
    ) {
        val food = _uiState.value.selectedFood ?: return
        val updated = validateUseCase.applyOverrides(food, kcal, protein, carbs, fat)
        _uiState.value = _uiState.value.copy(selectedFood = updated)
        if (validateUseCase.isComplete(updated) && shouldCache(updated)) {
            cacheSelectedFood(updated)
        }
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

    /**
     * Only API-sourced results (USDA / Open Food Facts) are candidates for the food cache.
     * Manual entries have synthetic ids and no upstream source to deduplicate against.
     */
    private fun shouldCache(result: FoodSearchResult): Boolean =
        result.source == FoodSource.USDA || result.source == FoodSource.OPEN_FOOD_FACTS

    /**
     * Fire-and-forget cache write on [viewModelScope]. Cache writes are best-effort:
     * a Room failure here must not prevent the user from proceeding through the
     * meal-logging flow, so the exception (if any) is swallowed. The user's selection
     * remains correctly represented in [AddEntryUiState.selectedFood] either way.
     */
    private fun cacheSelectedFood(result: FoodSearchResult) {
        viewModelScope.launch {
            runCatching { foodLookupRepo.cacheItem(result) }
        }
    }
}
