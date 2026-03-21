package com.delve.hungrywalrus.ui.screen.plan

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var kcalInput by rememberSaveable { mutableStateOf("") }
    var proteinInput by rememberSaveable { mutableStateOf("") }
    var carbsInput by rememberSaveable { mutableStateOf("") }
    var fatInput by rememberSaveable { mutableStateOf("") }
    var fieldsInitialised by rememberSaveable { mutableStateOf(false) }

    // Pre-populate fields when plan loads
    LaunchedEffect(uiState) {
        if (!fieldsInitialised && uiState is PlanUiState.Content) {
            val plan = (uiState as PlanUiState.Content).plan
            if (plan != null) {
                kcalInput = plan.kcalTarget.toString()
                proteinInput = plan.proteinTargetG.toString()
                carbsInput = plan.carbsTargetG.toString()
                fatInput = plan.fatTargetG.toString()
            }
            fieldsInitialised = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PlanUiEvent.PlanSaved -> {
                    snackbarHostState.showSnackbar("Plan updated")
                    onNavigateBack()
                }
            }
        }
    }

    val validationErrors = if (uiState is PlanUiState.ValidationError) {
        (uiState as PlanUiState.ValidationError).errors
    } else emptyMap()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Plan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Effective date line
            val effectiveDateText = when (uiState) {
                is PlanUiState.Content -> {
                    val plan = (uiState as PlanUiState.Content).plan
                    if (plan != null) "Effective from: ${Formatter.formatDate(plan.effectiveFrom)}"
                    else "No plan configured"
                }
                else -> ""
            }
            Text(
                text = effectiveDateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            OutlinedTextField(
                value = kcalInput,
                onValueChange = { kcalInput = it },
                label = { Text("Daily kilocalories") },
                suffix = { Text("kcal") },
                isError = validationErrors.containsKey("kcal"),
                supportingText = validationErrors["kcal"]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = proteinInput,
                onValueChange = { proteinInput = it },
                label = { Text("Protein") },
                suffix = { Text("g") },
                isError = validationErrors.containsKey("protein"),
                supportingText = validationErrors["protein"]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = carbsInput,
                onValueChange = { carbsInput = it },
                label = { Text("Carbohydrates") },
                suffix = { Text("g") },
                isError = validationErrors.containsKey("carbs"),
                supportingText = validationErrors["carbs"]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = fatInput,
                onValueChange = { fatInput = it },
                label = { Text("Fat") },
                suffix = { Text("g") },
                isError = validationErrors.containsKey("fat"),
                supportingText = validationErrors["fat"]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            val isValid = kcalInput.isNotBlank() && proteinInput.isNotBlank() &&
                carbsInput.isNotBlank() && fatInput.isNotBlank()

            Button(
                onClick = {
                    viewModel.savePlan(kcalInput, proteinInput, carbsInput, fatInput)
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Plan")
            }
            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = "Note: Changes apply from today forward. Historical data is not affected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
