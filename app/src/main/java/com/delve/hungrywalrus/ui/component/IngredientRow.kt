package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

/**
 * Tappable list item used in the Create/Edit Recipe screen ingredient list
 * (design §5.9, §3.13 element 4, architecture §7.8).
 *
 * The row content area (name, weight, kcal) is the primary tap target wired to
 * [onClick] — it opens the ingredient edit sheet. A separate trailing `IconButton`
 * with a `Close` icon is wired to [onRemove]; its click handler does not propagate
 * to the row to prevent edit-vs-remove confusion.
 *
 * The row content has a minimum height of 56dp to comfortably exceed Material's
 * 48dp touch-target requirement.
 */
@Composable
fun IngredientRow(
    name: String,
    weightG: Double,
    kcal: Double,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weightText = "${Formatter.formatMacro(weightG)}g"
    val kcalText = "${Formatter.formatKcal(kcal)} kcal"
    val rowDescription = "Edit $name, $weightText, $kcalText"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clickable(onClick = onClick)
                .semantics { contentDescription = rowDescription }
                .padding(vertical = Spacing.sm),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$weightText | $kcalText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove $name",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
