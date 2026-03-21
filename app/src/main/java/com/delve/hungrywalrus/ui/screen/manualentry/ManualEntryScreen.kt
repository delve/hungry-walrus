package com.delve.hungrywalrus.ui.screen.manualentry

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToWeightEntry: () -> Unit,
) {
    var foodName by rememberSaveable { mutableStateOf("") }
    var kcalInput by rememberSaveable { mutableStateOf("") }
    var proteinInput by rememberSaveable { mutableStateOf("") }
    var carbsInput by rememberSaveable { mutableStateOf("") }
    var fatInput by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val kcalValid = kcalInput.toDoubleOrNull()?.let { it >= 0 } == true
    val proteinValid = proteinInput.toDoubleOrNull()?.let { it >= 0 } == true
    val carbsValid = carbsInput.toDoubleOrNull()?.let { it >= 0 } == true
    val fatValid = fatInput.toDoubleOrNull()?.let { it >= 0 } == true
    val nameValid = foodName.isNotBlank()
    val allValid = nameValid && kcalValid && proteinValid && carbsValid && fatValid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Entry") },
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

            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text("Food name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

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
            Spacer(modifier = Modifier.height(Spacing.xl))

            Button(
                onClick = {
                    viewModel.setManualFood(
                        name = foodName.trim(),
                        kcalPer100g = kcalInput.toDouble(),
                        proteinPer100g = proteinInput.toDouble(),
                        carbsPer100g = carbsInput.toDouble(),
                        fatPer100g = fatInput.toDouble(),
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
