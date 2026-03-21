package com.delve.hungrywalrus.ui.screen.dailyprogress

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.ConfirmationDialog
import com.delve.hungrywalrus.ui.component.LogEntryItem
import com.delve.hungrywalrus.ui.component.NutritionProgressBar
import com.delve.hungrywalrus.ui.theme.CardCornerRadius
import com.delve.hungrywalrus.ui.theme.ProgressCarbs
import com.delve.hungrywalrus.ui.theme.ProgressFat
import com.delve.hungrywalrus.ui.theme.ProgressKcal
import com.delve.hungrywalrus.ui.theme.ProgressProtein
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyProgressScreen(
    onNavigateToPlan: () -> Unit,
    onNavigateToLogMethod: () -> Unit,
    viewModel: DailyProgressViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var entryToDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var entryToDeleteLabel by rememberSaveable { mutableStateOf<String?>(null) }

    if (entryToDeleteId != null && entryToDeleteLabel != null) {
        ConfirmationDialog(
            title = "Delete entry?",
            body = entryToDeleteLabel!!,
            confirmText = "Delete",
            onConfirm = {
                viewModel.deleteEntry(entryToDeleteId!!)
                entryToDeleteId = null
                entryToDeleteLabel = null
            },
            onDismiss = {
                entryToDeleteId = null
                entryToDeleteLabel = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Today")
                        Text(
                            text = Formatter.formatDate(LocalDate.now()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateToPlan) {
                        Text("Plan", color = MaterialTheme.colorScheme.primary)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLogMethod,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Log food",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        when (val state = uiState) {
            is DailyProgressUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is DailyProgressUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            is DailyProgressUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Progress section
                    if (state.plan == null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
                                .clickable(onClick = onNavigateToPlan),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            shape = RoundedCornerShape(CardCornerRadius),
                        ) {
                            Text(
                                text = "No nutrition plan set. Tap to configure.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(Spacing.lg),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = Spacing.lg,
                                vertical = Spacing.sm,
                            ),
                        ) {
                            NutritionProgressBar(
                                label = "Kcal",
                                current = state.totalKcal,
                                target = state.plan.kcalTarget.toDouble(),
                                unit = "kcal",
                                colour = ProgressKcal,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    NutritionProgressBar(
                                        label = "Protein",
                                        current = state.totalProteinG,
                                        target = state.plan.proteinTargetG,
                                        unit = "g",
                                        colour = ProgressProtein,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    NutritionProgressBar(
                                        label = "Carbs",
                                        current = state.totalCarbsG,
                                        target = state.plan.carbsTargetG,
                                        unit = "g",
                                        colour = ProgressCarbs,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    NutritionProgressBar(
                                        label = "Fat",
                                        current = state.totalFatG,
                                        target = state.plan.fatTargetG,
                                        unit = "g",
                                        colour = ProgressFat,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    if (state.entries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.lg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No entries today. Tap + to log food.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            items(
                                items = state.entries,
                                key = { it.id },
                            ) { entry ->
                                LogEntryItem(
                                    foodName = entry.foodName,
                                    kcal = entry.kcal,
                                    proteinG = entry.proteinG,
                                    carbsG = entry.carbsG,
                                    fatG = entry.fatG,
                                    timestamp = entry.timestamp,
                                    onDelete = {
                                        entryToDeleteId = entry.id
                                        entryToDeleteLabel = "${entry.foodName} -- ${Formatter.formatKcal(entry.kcal)} kcal"
                                    },
                                )
                            }
                            // Bottom spacer for FAB clearance
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}
