package com.delve.hungrywalrus.ui.screen.addentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.domain.model.NutritionField
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingValuesScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToWeightEntry: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val food = uiState.selectedFood

    var kcalInput by rememberSaveable { mutableStateOf("") }
    var proteinInput by rememberSaveable { mutableStateOf("") }
    var carbsInput by rememberSaveable { mutableStateOf("") }
    var fatInput by rememberSaveable { mutableStateOf("") }

    val missingFields = food?.missingFields ?: emptySet()

    val kcalValid = if (NutritionField.KCAL in missingFields) {
        kcalInput.toDoubleOrNull()?.let { it >= 0 } == true
    } else true
    val proteinValid = if (NutritionField.PROTEIN in missingFields) {
        proteinInput.toDoubleOrNull()?.let { it >= 0 } == true
    } else true
    val carbsValid = if (NutritionField.CARBS in missingFields) {
        carbsInput.toDoubleOrNull()?.let { it >= 0 } == true
    } else true
    val fatValid = if (NutritionField.FAT in missingFields) {
        fatInput.toDoubleOrNull()?.let { it >= 0 } == true
    } else true
    val allValid = kcalValid && proteinValid && carbsValid && fatValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Nutrition Data") },
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

            Text(
                text = "\"${food?.name ?: ""}\" is missing some values. Please provide estimates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Missing value fields
            if (NutritionField.KCAL in missingFields) {
                OutlinedTextField(
                    value = kcalInput,
                    onValueChange = { kcalInput = it },
                    label = { Text("Kilocalories (per 100g)") },
                    suffix = { Text("kcal") },
                    isError = kcalInput.isNotEmpty() && !kcalValid,
                    supportingText = if (kcalInput.isNotEmpty() && !kcalValid) {
                        { Text("Enter a valid number") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }
            if (NutritionField.PROTEIN in missingFields) {
                OutlinedTextField(
                    value = proteinInput,
                    onValueChange = { proteinInput = it },
                    label = { Text("Protein (per 100g)") },
                    suffix = { Text("g") },
                    isError = proteinInput.isNotEmpty() && !proteinValid,
                    supportingText = if (proteinInput.isNotEmpty() && !proteinValid) {
                        { Text("Enter a valid number") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }
            if (NutritionField.CARBS in missingFields) {
                OutlinedTextField(
                    value = carbsInput,
                    onValueChange = { carbsInput = it },
                    label = { Text("Carbohydrates (per 100g)") },
                    suffix = { Text("g") },
                    isError = carbsInput.isNotEmpty() && !carbsValid,
                    supportingText = if (carbsInput.isNotEmpty() && !carbsValid) {
                        { Text("Enter a valid number") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }
            if (NutritionField.FAT in missingFields) {
                OutlinedTextField(
                    value = fatInput,
                    onValueChange = { fatInput = it },
                    label = { Text("Fat (per 100g)") },
                    suffix = { Text("g") },
                    isError = fatInput.isNotEmpty() && !fatValid,
                    supportingText = if (fatInput.isNotEmpty() && !fatValid) {
                        { Text("Enter a valid number") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            // Known values section
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Known values:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            if (food?.kcalPer100g != null && NutritionField.KCAL !in missingFields) {
                Text(
                    text = "Kilocalories: ${Formatter.formatKcal(food.kcalPer100g)} per 100g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (food?.proteinPer100g != null && NutritionField.PROTEIN !in missingFields) {
                Text(
                    text = "Protein: ${Formatter.formatMacro(food.proteinPer100g)}g per 100g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (food?.carbsPer100g != null && NutritionField.CARBS !in missingFields) {
                Text(
                    text = "Carbs: ${Formatter.formatMacro(food.carbsPer100g)}g per 100g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (food?.fatPer100g != null && NutritionField.FAT !in missingFields) {
                Text(
                    text = "Fat: ${Formatter.formatMacro(food.fatPer100g)}g per 100g",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Button(
                onClick = {
                    viewModel.applyMissingValues(
                        kcal = if (NutritionField.KCAL in missingFields) kcalInput.toDoubleOrNull() else null,
                        protein = if (NutritionField.PROTEIN in missingFields) proteinInput.toDoubleOrNull() else null,
                        carbs = if (NutritionField.CARBS in missingFields) carbsInput.toDoubleOrNull() else null,
                        fat = if (NutritionField.FAT in missingFields) fatInput.toDoubleOrNull() else null,
                    )
                    onNavigateToWeightEntry()
                },
                enabled = allValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Next")
            }
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
