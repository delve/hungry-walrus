package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.delve.hungrywalrus.ui.theme.CardCornerRadius
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

/**
 * Card for a food search result item.
 */
@Composable
fun FoodSearchResultItem(
    name: String,
    kcalPer100g: Double?,
    proteinPer100g: Double?,
    carbsPer100g: Double?,
    fatPer100g: Double?,
    hasMissingValues: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(CardCornerRadius),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Per 100g: ${formatNullableKcal(kcalPer100g)} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "P: ${formatNullableMacro(proteinPer100g)}g  C: ${formatNullableMacro(carbsPer100g)}g  F: ${formatNullableMacro(fatPer100g)}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasMissingValues) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Missing nutritional values",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

private fun formatNullableKcal(value: Double?): String {
    return if (value != null) Formatter.formatKcal(value) else "--"
}

private fun formatNullableMacro(value: Double?): String {
    return if (value != null) Formatter.formatMacro(value) else "--"
}
