package com.delve.hungrywalrus.ui.screen.addentry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.NutritionCard
import com.delve.hungrywalrus.ui.component.QuickWeightSelector
import com.delve.hungrywalrus.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEntryScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToConfirm: () -> Unit,
    onIngredientAdded: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Parse weight as Double then round to Int for chip selection (O14) and +/- buttons (W07).
    // This ensures decimal values like "100.0" still highlight the 100g chip, and +/- buttons
    // correctly increment/decrement from a decimal base rather than falling back to 0.
    val weightVal = uiState.weightG.toDoubleOrNull()?.roundToInt()
    val isRecipeSource = uiState.isRecipeSource
    val selectedRecipe = uiState.selectedRecipe
    val isIngredientMode = uiState.ingredientMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amount") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Food name
            Text(
                text = uiState.foodName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Weight input with +/- buttons
            Text(
                text = "Weight consumed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = {
                        val current = uiState.weightG.toDoubleOrNull()?.roundToInt() ?: 0
                        if (current > 1) {
                            viewModel.setWeight((current - 1).toString())
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease weight")
                }

                OutlinedTextField(
                    value = uiState.weightG,
                    onValueChange = { newVal ->
                        // Filter out negative signs
                        val filtered = newVal.filter { it.isDigit() || it == '.' }
                        viewModel.setWeight(filtered)
                    },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.sm)
                        .focusRequester(focusRequester),
                    singleLine = true,
                )

                IconButton(
                    onClick = {
                        val current = uiState.weightG.toDoubleOrNull()?.roundToInt() ?: 0
                        viewModel.setWeight((current + 1).toString())
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase weight")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Quick select chips
            Text(
                text = "Quick select:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            QuickWeightSelector(
                selectedValue = weightVal,
                onSelect = { weight -> viewModel.setWeight(weight.toString()) },
                show100Percent = isRecipeSource && selectedRecipe != null,
                hundredPercentWeight = selectedRecipe?.totalWeightG,
                onSelect100Percent = {
                    selectedRecipe?.let { recipe ->
                        viewModel.setWeight(recipe.totalWeightG.toInt().toString())
                    }
                },
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Scaled nutrition preview
            Text(
                text = "Scaled nutrition preview:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            val scaled = uiState.scaledNutrition
            NutritionCard(
                kcal = scaled?.kcal ?: 0.0,
                proteinG = scaled?.proteinG ?: 0.0,
                carbsG = scaled?.carbsG ?: 0.0,
                fatG = scaled?.fatG ?: 0.0,
                prominent = false,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            val buttonText = if (isIngredientMode) "Add Ingredient" else "Confirm"

            Button(
                onClick = {
                    if (isIngredientMode && onIngredientAdded != null) {
                        onIngredientAdded()
                    } else {
                        onNavigateToConfirm()
                    }
                },
                enabled = scaled != null && (uiState.weightG.toDoubleOrNull() ?: 0.0) > 0.0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(buttonText)
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
