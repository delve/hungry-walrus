package com.delve.hungrywalrus.ui.screen.addentry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delve.hungrywalrus.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMethodScreen(
    viewModel: AddEntryViewModel,
    onClose: () -> Unit,
    onNavigateToUsdaSearch: () -> Unit,
    onNavigateToOffSearch: () -> Unit,
    onNavigateToBarcode: () -> Unit,
    onNavigateToManual: () -> Unit,
    onNavigateToRecipeSelect: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Food") },
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
            MethodItem(
                icon = Icons.Default.Search,
                label = "Search generic foods (USDA)",
                subtitle = if (!uiState.hasUsdaKey) "API key required -- tap to configure" else null,
                onClick = {
                    if (uiState.hasUsdaKey) onNavigateToUsdaSearch()
                    else showApiKeyDialog = true
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MethodItem(
                icon = Icons.Default.Search,
                label = "Search branded products (OFF)",
                onClick = onNavigateToOffSearch,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MethodItem(
                icon = Icons.Default.QrCodeScanner,
                label = "Scan barcode",
                onClick = onNavigateToBarcode,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MethodItem(
                icon = Icons.Default.Edit,
                label = "Enter manually",
                onClick = onNavigateToManual,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MethodItem(
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "Log from recipe",
                onClick = onNavigateToRecipeSelect,
            )
        }
    }

    // In-flow API key dialog — keeps the user inside the log graph so the bottom nav
    // remains hidden, instead of navigating to the top-level Settings destination.
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false; apiKeyInput = "" },
            title = { Text("USDA API Key Required") },
            text = {
                Column {
                    Text("Enter your USDA FoodData Central API key to search generic foods.")
                    Spacer(modifier = Modifier.height(Spacing.md))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { value: String -> apiKeyInput = value },
                        label = { Text("API Key") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                        apiKeyInput = ""
                        onNavigateToUsdaSearch()
                    },
                    enabled = apiKeyInput.isNotBlank(),
                ) {
                    Text("Save & Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false; apiKeyInput = "" }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun MethodItem(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
