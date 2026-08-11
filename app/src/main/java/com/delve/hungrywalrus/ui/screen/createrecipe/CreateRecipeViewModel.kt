package com.delve.hungrywalrus.ui.screen.createrecipe

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.FoodSource
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

/**
 * In-memory representation of a recipe ingredient during creation/editing.
 * Mirrors `RecipeIngredient` but is decoupled from the persistence layer.
 *
 * Architecture §7.7 — the [id] is a stable identifier assigned by the ViewModel and
 * is used by [CreateRecipeViewModel.editIngredient] to locate the draft to update.
 * For ingredients not yet persisted the ViewModel assigns negative sentinel ids
 * (-1, -2, ...) on add. For ingredients loaded from an existing recipe the ViewModel
 * assigns positive ids derived from their position in the loaded list (1, 2, ...).
 * The id value is not propagated to the persisted rows — Room generates fresh
 * primary keys during the delete-and-reinsert save.
 *
 * [source] is retained internally so the source of each ingredient is preserved
 * across the editing session. It does not constrain edit behaviour — name and
 * per-100g values are editable for all sources per architecture §7.8.
 *
 * The non-id/source fields are listed first to preserve compatibility with
 * positional construction in existing call sites and tests; this allows new
 * ingredient drafts to be constructed without naming every field.
 */
data class IngredientDraft(
    val name: String,
    val weightG: Double,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val id: Long = 0L,
    val source: FoodSource = FoodSource.MANUAL,
)

/**
 * Values supplied by the ingredient edit dialog (design §3.13a, architecture §7.8).
 * Excludes the draft `id` and `source` which are preserved across the edit.
 */
data class IngredientEditValues(
    val foodName: String,
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
     *  clean open-and-close. In-place ingredient edits via [CreateRecipeViewModel.editIngredient]
     *  also set this to true. */
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

    /**
     * Source of unique negative ids for new ingredient drafts during this editing
     * session. Negative sentinels avoid collisions with the positive ids assigned to
     * drafts loaded from an existing recipe. The architecture only requires that the
     * value be stable within the session; it is not persisted.
     */
    private var nextDraftId: Long = -1L

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
                val drafts = rwi.ingredients.mapIndexed { index, ing ->
                    IngredientDraft(
                        id = (index + 1).toLong(),
                        name = ing.foodName,
                        weightG = ing.weightG,
                        kcalPer100g = ing.kcalPer100g,
                        proteinPer100g = ing.proteinPer100g,
                        carbsPer100g = ing.carbsPer100g,
                        fatPer100g = ing.fatPer100g,
                        // Source is unknown for loaded ingredients (the entity does
                        // not record it). MANUAL is a safe default and does not
                        // constrain edit behaviour per architecture §7.8.
                        source = FoodSource.MANUAL,
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
        // Assign a unique negative-sentinel id if the draft was constructed without one.
        // Drafts constructed with id != 0 retain their supplied id (used in some tests).
        val assigned = if (draft.id == 0L) draft.copy(id = nextDraftId--) else draft
        val updated = _uiState.value.ingredients + assigned
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

    /**
     * In-place edit of an existing ingredient (architecture §7.7).
     *
     * Locates the draft with the matching [id], replaces its name, weight, and per-100g
     * values with [newValues] while preserving the [id] and `source`, emits the updated
     * list to the StateFlow, and recomputes the live running totals.
     *
     * No database I/O occurs at this point — changes are persisted only on recipe save
     * via the existing delete-and-reinsert strategy.
     *
     * The order of ingredients is preserved. If no draft matches [id] the call is a no-op
     * and `isDirty` is not flipped.
     */
    fun editIngredient(id: Long, newValues: IngredientEditValues) {
        val current = _uiState.value.ingredients
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return

        val existing = current[index]
        val updated = current.toMutableList().apply {
            this[index] = existing.copy(
                name = newValues.foodName,
                weightG = newValues.weightG,
                kcalPer100g = newValues.kcalPer100g,
                proteinPer100g = newValues.proteinPer100g,
                carbsPer100g = newValues.carbsPer100g,
                fatPer100g = newValues.fatPer100g,
                // id and source intentionally preserved.
            )
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
