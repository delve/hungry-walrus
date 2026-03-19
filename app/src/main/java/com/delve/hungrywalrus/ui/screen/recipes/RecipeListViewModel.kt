package com.delve.hungrywalrus.ui.screen.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.Recipe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecipeListUiState {
    data object Loading : RecipeListUiState
    data class Content(val recipes: List<Recipe>) : RecipeListUiState
}

@HiltViewModel
class RecipeListViewModel @Inject constructor(
    private val recipeRepo: RecipeRepository,
) : ViewModel() {

    val uiState: StateFlow<RecipeListUiState> = recipeRepo.getAllRecipes()
        .map { recipes -> RecipeListUiState.Content(recipes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecipeListUiState.Loading,
        )

    fun deleteRecipe(id: Long) {
        viewModelScope.launch {
            recipeRepo.deleteRecipe(id)
        }
    }
}
