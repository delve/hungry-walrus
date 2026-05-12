package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.delve.hungrywalrus.ui.screen.createrecipe.IngredientEditValues
import com.delve.hungrywalrus.ui.theme.Spacing

/**
 * Modal bottom sheet for in-place editing of a recipe ingredient.
 *
 * Design spec §3.13a / §5.10. Architecture §7.8.
 *
 * - Pre-populated with [initialValues].
 * - Edits are validated locally; Save is disabled while any field is invalid.
 * - Tapping Save invokes [onSave] with the current values. Cancel, the close icon,
 *   dragging the sheet down, and tapping the scrim all invoke [onCancel] and
 *   discard the change. None of these touches the database — persistence happens
 *   when the recipe is saved (architecture §7.7).
 *
 * Pre-population strategy:
 * All six fields (weight, name, and the four per-100g macros) pre-populate using a
 * precision-preserving representation of the stored value. The architecture's 0.5g
 * macronutrient display rounding (§7.4) applies to *display* surfaces such as
 * progress bars, summaries, and list rows; it must not be applied here, because the
 * user could otherwise confirm the sheet unchanged and silently round a precise
 * fractional weight (e.g. 76.3g -> 76.5g) on the next recipe save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIngredientSheet(
    initialValues: IngredientEditValues,
    onSave: (IngredientEditValues) -> Unit,
    onCancel: () -> Unit,
) {
    // rememberSaveable keys these to the initialValues' identity (e.g. the source
    // ingredient's id) so opening the sheet for a different ingredient resets the
    // edit state cleanly. We pre-populate from initialValues on first composition.
    var nameInput by rememberSaveable(initialValues) { mutableStateOf(initialValues.foodName) }
    var weightInput by rememberSaveable(initialValues) {
        // Preserve the stored weight precisely. See class-level kdoc — using
        // Formatter.formatMacro here would silently round e.g. 76.3 -> "76.5" and
        // cause precision loss on save.
        mutableStateOf(formatNumberForEdit(initialValues.weightG))
    }
    var kcalInput by rememberSaveable(initialValues) {
        mutableStateOf(formatNumberForEdit(initialValues.kcalPer100g))
    }
    var proteinInput by rememberSaveable(initialValues) {
        mutableStateOf(formatNumberForEdit(initialValues.proteinPer100g))
    }
    var carbsInput by rememberSaveable(initialValues) {
        mutableStateOf(formatNumberForEdit(initialValues.carbsPer100g))
    }
    var fatInput by rememberSaveable(initialValues) {
        mutableStateOf(formatNumberForEdit(initialValues.fatPer100g))
    }

    val nameValid = nameInput.trim().isNotEmpty()
    val weightVal = weightInput.toDoubleOrNull()
    val weightValid = weightVal != null && weightVal > 0.0
    val kcalVal = kcalInput.toDoubleOrNull()
    val kcalValid = kcalVal != null && kcalVal >= 0.0
    val proteinVal = proteinInput.toDoubleOrNull()
    val proteinValid = proteinVal != null && proteinVal >= 0.0
    val carbsVal = carbsInput.toDoubleOrNull()
    val carbsValid = carbsVal != null && carbsVal >= 0.0
    val fatVal = fatInput.toDoubleOrNull()
    val fatValid = fatVal != null && fatVal >= 0.0

    val allValid = nameValid && weightValid && kcalValid && proteinValid && carbsValid && fatValid

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Edit Ingredient",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            // Ingredient name
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Ingredient name") },
                isError = !nameValid,
                supportingText = if (!nameValid) {
                    { Text("Name is required") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            // Weight in recipe
            Text(
                text = "Weight in recipe",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            OutlinedTextField(
                value = weightInput,
                onValueChange = { newValue ->
                    // Reject minus signs on input.
                    weightInput = newValue.filter { it != '-' }
                },
                suffix = { Text("g") },
                isError = !weightValid,
                supportingText = if (!weightValid) {
                    { Text("Enter a valid weight") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Per-100g nutrition fields
            Text(
                text = "Nutrition per 100g:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            NutritionFieldRow(
                label = "Kilocalories",
                suffix = "kcal",
                value = kcalInput,
                onValueChange = { kcalInput = it },
                isError = !kcalValid,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            NutritionFieldRow(
                label = "Protein",
                suffix = "g",
                value = proteinInput,
                onValueChange = { proteinInput = it },
                isError = !proteinValid,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            NutritionFieldRow(
                label = "Carbohydrates",
                suffix = "g",
                value = carbsInput,
                onValueChange = { carbsInput = it },
                isError = !carbsValid,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            NutritionFieldRow(
                label = "Fat",
                suffix = "g",
                value = fatInput,
                onValueChange = { fatInput = it },
                isError = !fatValid,
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Scaled-for-this-ingredient preview
            Text(
                text = "Scaled for this ingredient",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            val previewKcal = scaleFor(kcalVal, weightVal)
            val previewProtein = scaleFor(proteinVal, weightVal)
            val previewCarbs = scaleFor(carbsVal, weightVal)
            val previewFat = scaleFor(fatVal, weightVal)
            if (previewKcal != null && previewProtein != null && previewCarbs != null && previewFat != null) {
                NutritionCard(
                    kcal = previewKcal,
                    proteinG = previewProtein,
                    carbsG = previewCarbs,
                    fatG = previewFat,
                    prominent = false,
                )
            } else {
                Text(
                    text = "--",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Action buttons: Cancel (leading) and Save (trailing)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (allValid) {
                            onSave(
                                IngredientEditValues(
                                    foodName = nameInput.trim(),
                                    weightG = weightVal ?: return@Button,
                                    kcalPer100g = kcalVal ?: return@Button,
                                    proteinPer100g = proteinVal ?: return@Button,
                                    carbsPer100g = carbsVal ?: return@Button,
                                    fatPer100g = fatVal ?: return@Button,
                                ),
                            )
                        }
                    },
                    enabled = allValid,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                ) {
                    Text("Save")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NutritionFieldRow(
    label: String,
    suffix: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { ch -> ch != '-' }) },
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = isError,
        supportingText = if (isError) {
            { Text("Enter a valid number") }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

/**
 * Returns `(per100g / 100) * weight` when both inputs are non-null and non-negative.
 * Returns null otherwise so the preview can display "--" while inputs are invalid.
 */
private fun scaleFor(per100g: Double?, weight: Double?): Double? {
    if (per100g == null || weight == null) return null
    if (per100g < 0.0 || weight <= 0.0) return null
    return (per100g / 100.0) * weight
}

/**
 * Formats a stored [Double] for editing. Whole-number values render without the
 * trailing ".0" so the user sees "100" rather than "100.0"; fractional values are
 * preserved exactly via [Double.toString] (e.g. 76.3 -> "76.3"). The output is
 * always parseable by [String.toDoubleOrNull], which is what the validation logic
 * relies on. This is internal to the edit sheet only and does not apply the 0.5g
 * display rounding used elsewhere (architecture §7.4).
 *
 * Visible for tests via Kotlin's package-private default.
 */
internal fun formatNumberForEdit(value: Double): String {
    // toLong() truncates; we only treat truly whole-number doubles (e.g. 100.0,
    // -0.0) as "whole" so the rendered string round-trips through toDouble().
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}
