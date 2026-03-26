package com.delve.hungrywalrus.ui.screen.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.ConfirmationDialog
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var keyInput by rememberSaveable { mutableStateOf("") }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    // Plan fields
    var kcalInput by rememberSaveable { mutableStateOf("") }
    var proteinInput by rememberSaveable { mutableStateOf("") }
    var carbsInput by rememberSaveable { mutableStateOf("") }
    var fatInput by rememberSaveable { mutableStateOf("") }
    var planFieldsInitialised by rememberSaveable { mutableStateOf(false) }

    // Pre-populate plan fields when the plan loads for the first time
    LaunchedEffect(uiState.planLoading, uiState.currentPlan) {
        if (!planFieldsInitialised && !uiState.planLoading) {
            val plan = uiState.currentPlan
            if (plan != null) {
                kcalInput = plan.kcalTarget.toString()
                proteinInput = plan.proteinTargetG.toString()
                carbsInput = plan.carbsTargetG.toString()
                fatInput = plan.fatTargetG.toString()
            }
            planFieldsInitialised = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsUiEvent.KeySaved -> {
                    keyInput = ""
                    snackbarHostState.showSnackbar("API key saved")
                }
                SettingsUiEvent.KeyCleared -> {
                    keyInput = ""
                    snackbarHostState.showSnackbar("API key cleared")
                }
                is SettingsUiEvent.ReadError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                SettingsUiEvent.PlanSaved -> {
                    snackbarHostState.showSnackbar("Plan updated")
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmationDialog(
            title = "Clear API key?",
            body = "Clear your USDA API key? USDA search will be disabled.",
            confirmText = "Clear",
            onConfirm = {
                viewModel.clearKey()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
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

            // API key section
            Text(
                text = "USDA API Key",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Status
            Text(
                text = if (uiState.hasKey) "Status: Configured" else "Status: Not set",
                style = MaterialTheme.typography.bodyMedium,
                color = if (uiState.hasKey) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("API key") },
                placeholder = {
                    if (uiState.hasKey) Text(uiState.keyMasked)
                    else Text("Enter your API key")
                },
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = "Enter your USDA FoodData Central API key.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Get a free key at fdc.nal.usda.gov",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fdc.nal.usda.gov/api-key-signup"))
                    context.startActivity(intent)
                },
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                FilledTonalButton(
                    onClick = { viewModel.saveKey(keyInput) },
                    enabled = keyInput.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save Key")
                }
                if (uiState.hasKey) {
                    OutlinedButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Clear Key")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Nutrition plan section
            Text(
                text = "Nutrition Plan",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            val effectiveDateText = if (!uiState.planLoading) {
                val plan = uiState.currentPlan
                if (plan != null) "Effective from: ${Formatter.formatDate(plan.effectiveFrom)}"
                else "No plan configured"
            } else ""
            Text(
                text = effectiveDateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            val planErrors = uiState.planValidationErrors

            OutlinedTextField(
                value = kcalInput,
                onValueChange = { kcalInput = it },
                label = { Text("Daily kilocalories") },
                suffix = { Text("kcal") },
                isError = planErrors.containsKey("kcal"),
                supportingText = planErrors["kcal"]?.let { { Text(it) } },
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
                isError = planErrors.containsKey("protein"),
                supportingText = planErrors["protein"]?.let { { Text(it) } },
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
                isError = planErrors.containsKey("carbs"),
                supportingText = planErrors["carbs"]?.let { { Text(it) } },
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
                isError = planErrors.containsKey("fat"),
                supportingText = planErrors["fat"]?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            val isPlanInputValid = kcalInput.isNotBlank() && proteinInput.isNotBlank() &&
                carbsInput.isNotBlank() && fatInput.isNotBlank()

            Button(
                onClick = {
                    viewModel.savePlan(kcalInput, proteinInput, carbsInput, fatInput)
                },
                enabled = isPlanInputValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save Plan")
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "Changes apply from today forward. Historical data is not affected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(Spacing.lg))

            // About section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Hungry Walrus v1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Data stored locally on this device only.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
