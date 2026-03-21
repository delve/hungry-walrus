package com.delve.hungrywalrus.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.delve.hungrywalrus.util.Formatter

/**
 * Compact row showing macronutrient values: "P: Xg  C: Xg  F: Xg"
 */
@Composable
fun NutritionSummaryRow(
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "P: ${Formatter.formatMacro(proteinG)}g  C: ${Formatter.formatMacro(carbsG)}g  F: ${Formatter.formatMacro(fatG)}g",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
