package com.delve.hungrywalrus.ui.screen.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.ConfirmationDialog
import com.delve.hungrywalrus.ui.component.NutritionCard
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: RecipeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                RecipeDetailUiEvent.RecipeDeleted -> {
                    snackbarHostState.showSnackbar("Recipe deleted")
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is RecipeDetailUiState.Content -> state.recipeWithIngredients.recipe.name
                        else -> "Recipe"
                    }
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is RecipeDetailUiState.Content) {
                        val recipeId = (uiState as RecipeDetailUiState.Content).recipeWithIngredients.recipe.id
                        IconButton(onClick = { onNavigateToEdit(recipeId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit recipe")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete recipe")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            is RecipeDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is RecipeDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Recipe not found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is RecipeDetailUiState.Content -> {
                val rwi = state.recipeWithIngredients
                val recipe = rwi.recipe

                if (showDeleteDialog) {
                    ConfirmationDialog(
                        title = "Delete recipe?",
                        body = "'${recipe.name}' will be permanently deleted. Previously logged entries are not affected.",
                        confirmText = "Delete",
                        onConfirm = {
                            viewModel.deleteRecipe()
                            showDeleteDialog = false
                        },
                        onDismiss = { showDeleteDialog = false },
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = Spacing.lg),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        NutritionCard(
                            kcal = recipe.totalKcal,
                            proteinG = recipe.totalProteinG,
                            carbsG = recipe.totalCarbsG,
                            fatG = recipe.totalFatG,
                            prominent = false,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "${Formatter.formatMacro(recipe.totalWeightG)}g total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            text = "Ingredients",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }

                    itemsIndexed(rwi.ingredients) { index, ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = ingredient.foodName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${Formatter.formatMacro(ingredient.weightG)}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val scaledKcal = (ingredient.kcalPer100g / 100.0) * ingredient.weightG
                            Text(
                                text = "${Formatter.formatKcal(scaledKcal)} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = Spacing.sm),
                            )
                        }
                        if (index < rwi.ingredients.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            text = "Created: ${Formatter.formatDate(recipe.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Last updated: ${Formatter.formatDate(recipe.updatedAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(Spacing.xl))
                    }
                }
            }
        }
    }
}
