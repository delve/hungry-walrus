package com.delve.hungrywalrus.ui.screen.foodsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.component.FoodSearchResultItem
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.ui.screen.addentry.SearchState
import com.delve.hungrywalrus.ui.theme.Spacing
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    source: String,
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToWeightEntry: () -> Unit,
    onNavigateToMissingValues: () -> Unit,
    onNavigateToManual: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    val title = if (source == "usda") "Search USDA" else "Search Products"
    val searchFn: (String) -> Unit = if (source == "usda") viewModel::searchUsda else viewModel::searchOff

    // Debounced search
    LaunchedEffect(uiState.searchQuery) {
        if (uiState.searchQuery.isNotBlank()) {
            delay(300)
            searchFn(uiState.searchQuery)
        }
    }

    // Auto-focus search field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                .padding(padding),
        ) {
            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Food name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                IconButton(
                    onClick = { searchFn(uiState.searchQuery) },
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Go")
                }
            }

            // Loading indicator
            if (uiState.searchState == SearchState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Results area
            when (uiState.searchState) {
                SearchState.Idle -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Search for a food to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SearchState.Loading -> {
                    // Loading indicator is shown above
                }

                SearchState.Results -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(uiState.searchResults) { result ->
                            FoodSearchResultItem(
                                name = result.name,
                                kcalPer100g = result.kcalPer100g,
                                proteinPer100g = result.proteinPer100g,
                                carbsPer100g = result.carbsPer100g,
                                fatPer100g = result.fatPer100g,
                                hasMissingValues = result.missingFields.isNotEmpty(),
                                onClick = {
                                    val needsMissing = viewModel.selectFood(result)
                                    if (needsMissing) {
                                        onNavigateToMissingValues()
                                    } else {
                                        onNavigateToWeightEntry()
                                    }
                                },
                            )
                        }
                    }
                }

                SearchState.NoResults -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No results found. Try a different search or enter manually.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            TextButton(onClick = onNavigateToManual) {
                                Text("Enter manually")
                            }
                        }
                    }
                }

                SearchState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (uiState.searchErrorMessage.contains("internet", ignoreCase = true)) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = Spacing.sm),
                                )
                            }
                            Text(
                                text = uiState.searchErrorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = Spacing.lg),
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            TextButton(onClick = onNavigateToManual) {
                                Text("Enter manually")
                            }
                        }
                    }
                }
            }
        }
    }
}
