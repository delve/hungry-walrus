package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.delve.hungrywalrus.ui.theme.CardCornerRadius
import com.delve.hungrywalrus.ui.theme.Spacing
import com.delve.hungrywalrus.util.Formatter

/**
 * Card displaying nutritional values.
 * [prominent] = true: kcal in titleLarge centred, macros in three columns below.
 * [prominent] = false: four-column single row with label headers and values.
 */
@Composable
fun NutritionCard(
    kcal: Double,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    prominent: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(CardCornerRadius),
    ) {
        if (prominent) {
            ProminentLayout(kcal, proteinG, carbsG, fatG)
        } else {
            CompactLayout(kcal, proteinG, carbsG, fatG)
        }
    }
}

@Composable
private fun ProminentLayout(
    kcal: Double,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${Formatter.formatKcal(kcal)} kcal",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.md),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MacroColumn("Protein", proteinG)
            MacroColumn("Carbs", carbsG)
            MacroColumn("Fat", fatG)
        }
    }
}

@Composable
private fun MacroColumn(label: String, value: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${Formatter.formatMacro(value)}g",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CompactLayout(
    kcal: Double,
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        CompactColumn("Kcal", Formatter.formatKcal(kcal))
        CompactColumn("Protein", "${Formatter.formatMacro(proteinG)}g")
        CompactColumn("Carbs", "${Formatter.formatMacro(carbsG)}g")
        CompactColumn("Fat", "${Formatter.formatMacro(fatG)}g")
    }
}

@Composable
private fun CompactColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
