package com.delve.hungrywalrus.ui.screen.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.RecipeWithIngredients
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipeDetailUiState {
    data object Loading : RecipeDetailUiState
    data class Content(val recipeWithIngredients: RecipeWithIngredients) : RecipeDetailUiState
    data object NotFound : RecipeDetailUiState
}

sealed interface RecipeDetailUiEvent {
    data object RecipeDeleted : RecipeDetailUiEvent
}

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val recipeId: Long = savedStateHandle["id"] ?: -1L

    private val _events = Channel<RecipeDetailUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<RecipeDetailUiState> = recipeRepo.getRecipeWithIngredients(recipeId)
        .map { recipeWithIngredients ->
            if (recipeWithIngredients != null) {
                RecipeDetailUiState.Content(recipeWithIngredients)
            } else {
                RecipeDetailUiState.NotFound
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeDetailUiState.Loading,
        )

    fun deleteRecipe() {
        viewModelScope.launch {
            recipeRepo.deleteRecipe(recipeId)
            _events.send(RecipeDetailUiEvent.RecipeDeleted)
        }
    }
}
