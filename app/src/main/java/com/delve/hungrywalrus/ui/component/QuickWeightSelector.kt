package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.delve.hungrywalrus.ui.theme.Spacing

/**
 * Horizontally scrollable row of weight quick-select chips.
 */
@Composable
fun QuickWeightSelector(
    options: List<Int> = listOf(25, 50, 100, 150, 200, 250),
    selectedValue: Int?,
    onSelect: (Int) -> Unit,
    show100Percent: Boolean = false,
    hundredPercentWeight: Double? = null,
    onSelect100Percent: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(options) { weight ->
            FilterChip(
                selected = selectedValue == weight,
                onClick = { onSelect(weight) },
                label = { Text("${weight}g") },
            )
        }
        if (show100Percent && hundredPercentWeight != null) {
            item {
                FilterChip(
                    selected = selectedValue != null && selectedValue.toDouble() == hundredPercentWeight,
                    onClick = onSelect100Percent,
                    label = { Text("100%") },
                )
            }
        }
    }
}
