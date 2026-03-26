package com.delve.hungrywalrus.ui.screen.createrecipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.ConfirmationDialog
import com.delve.hungrywalrus.ui.component.NutritionCard
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    onClose: () -> Unit,
    onRecipeSaved: () -> Unit,
    onNavigateToIngredientSearch: (String) -> Unit,
    onNavigateToIngredientBarcode: () -> Unit,
    viewModel: CreateRecipeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showMethodSheet by rememberSaveable { mutableStateOf(false) }
    var showInlineAddDialog by rememberSaveable { mutableStateOf(false) }

    // Inline manual ingredient entry state
    var inlineName by rememberSaveable { mutableStateOf("") }
    var inlineWeight by rememberSaveable { mutableStateOf("") }
    var inlineKcal by rememberSaveable { mutableStateOf("") }
    var inlineProtein by rememberSaveable { mutableStateOf("") }
    var inlineCarbs by rememberSaveable { mutableStateOf("") }
    var inlineFat by rememberSaveable { mutableStateOf("") }

    val resetInlineFields = {
        inlineName = ""
        inlineWeight = ""
        inlineKcal = ""
        inlineProtein = ""
        inlineCarbs = ""
        inlineFat = ""
    }

    // Keyed on viewModel so the collector restarts if ViewModel identity changes (O13).
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CreateRecipeUiEvent.RecipeSaved -> {
                    snackbarHostState.showSnackbar("Recipe saved")
                    onRecipeSaved()
                }
            }
        }
    }

    if (showDiscardDialog) {
        ConfirmationDialog(
            title = "Discard changes?",
            body = "Your recipe changes will be lost.",
            confirmText = "Discard",
            onConfirm = {
                showDiscardDialog = false
                onClose()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }

    val title = if (uiState.isEditMode) "Edit Recipe" else "Create Recipe"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isDirty) {
                            showDiscardDialog = true
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.lg),
            ) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Recipe name
                    OutlinedTextField(
                        value = uiState.recipeName,
                        onValueChange = { viewModel.setRecipeName(it) },
                        label = { Text("Recipe name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Live totals card
                    Text(
                        text = "Live totals:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    NutritionCard(
                        kcal = uiState.liveTotals.kcal,
                        proteinG = uiState.liveTotals.proteinG,
                        carbsG = uiState.liveTotals.carbsG,
                        fatG = uiState.liveTotals.fatG,
                        prominent = false,
                    )
                    Text(
                        text = "${Formatter.formatMacro(uiState.totalWeightG)}g total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Ingredients header
                    Text(
                        text = "Ingredients:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }

                if (uiState.ingredients.isEmpty()) {
                    item {
                        Text(
                            text = "Add ingredients to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                    }
                }

                itemsIndexed(uiState.ingredients) { index, ingredient ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val scaledKcal = (ingredient.kcalPer100g / 100.0) * ingredient.weightG
                            Text(
                                text = "${Formatter.formatMacro(ingredient.weightG)}g | ${Formatter.formatKcal(scaledKcal)} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.removeIngredient(index) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove ${ingredient.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (index < uiState.ingredients.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.lg))

                    // Add Ingredient button
                    OutlinedButton(
                        onClick = { showMethodSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("+ Add Ingredient")
                    }

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    // Save button
                    Button(
                        onClick = { viewModel.saveRecipe() },
                        enabled = uiState.recipeName.isNotBlank() && uiState.ingredients.isNotEmpty() && !uiState.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Save Recipe")
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.xl))
                }
            }
        }
    }

    // Method selection sheet
    if (showMethodSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMethodSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.lg),
            ) {
                Text(
                    text = "Add Ingredient",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IngredientMethodRow(
                    label = "Search generic foods (USDA)",
                    onClick = { showMethodSheet = false; onNavigateToIngredientSearch("usda") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IngredientMethodRow(
                    label = "Search branded products (OFF)",
                    onClick = { showMethodSheet = false; onNavigateToIngredientSearch("off") },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IngredientMethodRow(
                    label = "Scan barcode",
                    onClick = { showMethodSheet = false; onNavigateToIngredientBarcode() },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                IngredientMethodRow(
                    label = "Enter manually",
                    onClick = { showMethodSheet = false; showInlineAddDialog = true },
                )
            }
        }
    }

    // Inline add ingredient dialog -- a simple bottom sheet with manual fields
    if (showInlineAddDialog) {
        ModalBottomSheet(
            onDismissRequest = {
                showInlineAddDialog = false
                resetInlineFields()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            ) {
                Text(
                    text = "Add Ingredient",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Spacing.md))

                OutlinedTextField(
                    value = inlineName,
                    onValueChange = { inlineName = it },
                    label = { Text("Ingredient name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = inlineWeight,
                    onValueChange = { inlineWeight = it },
                    label = { Text("Weight (g)") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = inlineKcal,
                    onValueChange = { inlineKcal = it },
                    label = { Text("Kcal (per 100g)") },
                    suffix = { Text("kcal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = inlineProtein,
                    onValueChange = { inlineProtein = it },
                    label = { Text("Protein (per 100g)") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = inlineCarbs,
                    onValueChange = { inlineCarbs = it },
                    label = { Text("Carbs (per 100g)") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedTextField(
                    value = inlineFat,
                    onValueChange = { inlineFat = it },
                    label = { Text("Fat (per 100g)") },
                    suffix = { Text("g") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                val weightValid = inlineWeight.toDoubleOrNull()?.let { it > 0 } == true
                val kcalValid = inlineKcal.toDoubleOrNull()?.let { it >= 0 } == true
                val proteinValid = inlineProtein.toDoubleOrNull()?.let { it >= 0 } == true
                val carbsValid = inlineCarbs.toDoubleOrNull()?.let { it >= 0 } == true
                val fatValid = inlineFat.toDoubleOrNull()?.let { it >= 0 } == true
                val allValid = inlineName.isNotBlank() && weightValid && kcalValid && proteinValid && carbsValid && fatValid

                Button(
                    onClick = {
                        viewModel.addIngredient(
                            IngredientDraft(
                                name = inlineName.trim(),
                                weightG = inlineWeight.toDouble(),
                                kcalPer100g = inlineKcal.toDouble(),
                                proteinPer100g = inlineProtein.toDouble(),
                                carbsPer100g = inlineCarbs.toDouble(),
                                fatPer100g = inlineFat.toDouble(),
                            ),
                        )
                        showInlineAddDialog = false
                        resetInlineFields()
                    },
                    enabled = allValid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Add")
                }
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
        }
    }
}

@Composable
private fun IngredientMethodRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
