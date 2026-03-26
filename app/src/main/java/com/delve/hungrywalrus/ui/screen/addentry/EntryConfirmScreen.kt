package com.delve.hungrywalrus.ui.screen.addentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.NutritionCard
import com.delve.hungrywalrus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryConfirmScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onGoBack: () -> Unit,
    onEntrySaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEntryUiEvent.EntrySaved -> onEntrySaved()
                else -> {}
            }
        }
    }

    val scaled = uiState.scaledNutrition

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Entry") },
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
                .padding(horizontal = Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Spacing.xl))

            // Food name
            Text(
                text = uiState.foodName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Nutrition card
            if (scaled != null) {
                NutritionCard(
                    kcal = scaled.kcal,
                    proteinG = scaled.proteinG,
                    carbsG = scaled.carbsG,
                    fatG = scaled.fatG,
                    prominent = true,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save Entry button
            Button(
                onClick = { viewModel.saveEntry() },
                enabled = !uiState.isSaving && scaled != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Save Entry", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Go Back button
            OutlinedButton(
                onClick = onGoBack,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Go Back", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(Spacing.xl))
        }
    }
}
