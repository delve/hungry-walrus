package com.delve.hungrywalrus.ui.screen.summaries

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.domain.model.RollingSummary
import com.delve.hungrywalrus.ui.component.NutritionCard
import com.delve.hungrywalrus.ui.component.NutritionProgressBar
import com.delve.hungrywalrus.ui.theme.ProgressCarbs
import com.delve.hungrywalrus.ui.theme.ProgressFat
import com.delve.hungrywalrus.ui.theme.ProgressKcal
import com.delve.hungrywalrus.ui.theme.ProgressProtein
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummariesScreen(
    viewModel: SummariesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedTab = when (uiState) {
        is SummariesUiState.Loading -> SummaryTab.Day7
        is SummariesUiState.Content -> (uiState as SummariesUiState.Content).selectedTab
        is SummariesUiState.NoPlan -> (uiState as SummariesUiState.NoPlan).selectedTab
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Summaries") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = SummaryTab.entries.indexOf(selectedTab),
            ) {
                SummaryTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == selectedTab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }

            when (val state = uiState) {
                is SummariesUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is SummariesUiState.NoPlan -> {
                    SummaryContent(
                        summary = state.summary,
                        hasTarget = false,
                    )
                }

                is SummariesUiState.Content -> {
                    SummaryContent(
                        summary = state.summary,
                        hasTarget = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryContent(
    summary: RollingSummary,
    hasTarget: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg),
    ) {
        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = "${Formatter.formatDate(summary.startDate)} -- ${Formatter.formatDate(summary.endDate)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        if (!hasTarget) {
            Text(
                text = "Set up a nutrition plan to see targets.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        val target = summary.totalTarget
        val intake = summary.totalIntake

        // Kilocalories
        NutritionProgressBar(
            label = "Kilocalories",
            current = intake.kcal,
            target = target?.kcal ?: 0.0,
            unit = "kcal",
            colour = ProgressKcal,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Protein
        NutritionProgressBar(
            label = "Protein",
            current = intake.proteinG,
            target = target?.proteinG ?: 0.0,
            unit = "g",
            colour = ProgressProtein,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Carbohydrates
        NutritionProgressBar(
            label = "Carbohydrates",
            current = intake.carbsG,
            target = target?.carbsG ?: 0.0,
            unit = "g",
            colour = ProgressCarbs,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        // Fat
        NutritionProgressBar(
            label = "Fat",
            current = intake.fatG,
            target = target?.fatG ?: 0.0,
            unit = "g",
            colour = ProgressFat,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))

        // Daily average card
        Text(
            text = "Daily average",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        NutritionCard(
            kcal = summary.dailyAverage.kcal,
            proteinG = summary.dailyAverage.proteinG,
            carbsG = summary.dailyAverage.carbsG,
            fatG = summary.dailyAverage.fatG,
            prominent = false,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}
