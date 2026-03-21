package com.delve.hungrywalrus.ui.screen.addentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.NutritionSummaryRow
import com.delve.hungrywalrus.ui.theme.CardCornerRadius
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSelectScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToWeightEntry: () -> Unit,
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Recipe") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        if (recipes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No recipes saved. Create one from the Recipes tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(
                    items = recipes,
                    key = { it.id },
                ) { recipe ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectRecipe(recipe)
                                onNavigateToWeightEntry()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        shape = RoundedCornerShape(CardCornerRadius),
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.md),
                        ) {
                            Text(
                                text = recipe.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${Formatter.formatMacro(recipe.totalWeightG)}g total | ${Formatter.formatKcal(recipe.totalKcal)} kcal total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            NutritionSummaryRow(
                                proteinG = recipe.totalProteinG,
                                carbsG = recipe.totalCarbsG,
                                fatG = recipe.totalFatG,
                            )
                        }
                    }
                }
            }
        }
    }
}
