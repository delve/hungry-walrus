package com.delve.hungrywalrus.ui.screen.createrecipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.NutritionValues
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeIngredient
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientDraft(
    val name: String,
    val weightG: Double,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
)

data class CreateRecipeUiState(
    val recipeName: String = "",
    val ingredients: List<IngredientDraft> = emptyList(),
    val liveTotals: NutritionValues = NutritionValues(0.0, 0.0, 0.0, 0.0),
    val totalWeightG: Double = 0.0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val originalCreatedAt: Long = 0L,
    /** True once the user has made at least one change. False until then, including after
     *  loading an existing recipe in edit mode, so the discard dialog is not shown on a
     *  clean open-and-close. */
    val isDirty: Boolean = false,
)

sealed interface CreateRecipeUiEvent {
    data object RecipeSaved : CreateRecipeUiEvent
}

@HiltViewModel
class CreateRecipeViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository,
    private val scaleUseCase: ScaleNutritionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val editId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        CreateRecipeUiState(isEditMode = editId != null),
    )
    val uiState: StateFlow<CreateRecipeUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateRecipeUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (editId != null) {
            loadExistingRecipe(editId)
        }
    }

    private fun loadExistingRecipe(id: Long) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val rwi = recipeRepo.getRecipeWithIngredients(id).first()
            if (rwi != null) {
                val drafts = rwi.ingredients.map { ing ->
                    IngredientDraft(
                        name = ing.foodName,
                        weightG = ing.weightG,
                        kcalPer100g = ing.kcalPer100g,
                        proteinPer100g = ing.proteinPer100g,
                        carbsPer100g = ing.carbsPer100g,
                        fatPer100g = ing.fatPer100g,
                    )
                }
                _uiState.value = _uiState.value.copy(
                    recipeName = rwi.recipe.name,
                    ingredients = drafts,
                    originalCreatedAt = rwi.recipe.createdAt,
                    isLoading = false,
                )
                recomputeTotals()
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun setRecipeName(name: String) {
        _uiState.value = _uiState.value.copy(recipeName = name, isDirty = true)
    }

    fun addIngredient(draft: IngredientDraft) {
        val updated = _uiState.value.ingredients + draft
        _uiState.value = _uiState.value.copy(ingredients = updated, isDirty = true)
        recomputeTotals()
    }

    fun removeIngredient(index: Int) {
        val updated = _uiState.value.ingredients.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
        _uiState.value = _uiState.value.copy(ingredients = updated, isDirty = true)
        recomputeTotals()
    }

    private fun recomputeTotals() {
        val ingredients = _uiState.value.ingredients
        var totalKcal = 0.0
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0
        var totalWeight = 0.0

        for (ing in ingredients) {
            val scaled = scaleUseCase(
                ing.kcalPer100g, ing.proteinPer100g, ing.carbsPer100g, ing.fatPer100g, ing.weightG,
            )
            totalKcal += scaled.kcal
            totalProtein += scaled.proteinG
            totalCarbs += scaled.carbsG
            totalFat += scaled.fatG
            totalWeight += ing.weightG
        }

        _uiState.value = _uiState.value.copy(
            liveTotals = NutritionValues(totalKcal, totalProtein, totalCarbs, totalFat),
            totalWeightG = totalWeight,
        )
    }

    fun saveRecipe() {
        val state = _uiState.value
        if (state.recipeName.isBlank() || state.ingredients.isEmpty()) return

        _uiState.value = state.copy(isSaving = true)
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val recipe = Recipe(
                id = editId ?: 0,
                name = state.recipeName.trim(),
                totalWeightG = state.totalWeightG,
                totalKcal = state.liveTotals.kcal,
                totalProteinG = state.liveTotals.proteinG,
                totalCarbsG = state.liveTotals.carbsG,
                totalFatG = state.liveTotals.fatG,
                createdAt = if (editId != null) state.originalCreatedAt else now,
                updatedAt = now,
            )

            val ingredients = state.ingredients.map { draft ->
                RecipeIngredient(
                    recipeId = editId ?: 0,
                    foodName = draft.name,
                    weightG = draft.weightG,
                    kcalPer100g = draft.kcalPer100g,
                    proteinPer100g = draft.proteinPer100g,
                    carbsPer100g = draft.carbsPer100g,
                    fatPer100g = draft.fatPer100g,
                )
            }

            if (editId != null) {
                recipeRepo.updateRecipe(recipe, ingredients)
            } else {
                recipeRepo.saveRecipe(recipe, ingredients)
            }

            _uiState.value = _uiState.value.copy(isSaving = false)
            _events.send(CreateRecipeUiEvent.RecipeSaved)
        }
    }
}
