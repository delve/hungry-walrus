package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.delve.hungrywalrus.ui.theme.Overage
import com.delve.hungrywalrus.ui.theme.ProgressTrack
import com.delve.hungrywalrus.ui.theme.Spacing

/**
 * A horizontal progress bar with label row showing current/target and remaining/over amount.
 *
 * Two visual modes:
 * - [isKcalBar] = true (kcal row): combined "X / Y kcal" in `titleMedium` left-aligned,
 *   "Remaining: Z kcal" / "Over: Z kcal" in `bodyMedium` right-aligned.
 * - [isKcalBar] = false (macro rows): label in `labelSmall` / `onSurfaceVariant` left-aligned,
 *   value "X / Yg" in `bodyMedium` right-aligned, on the same line.
 *
 * [label] is always used in the accessibility semantics state description.
 */
@Composable
fun NutritionProgressBar(
    label: String,
    current: Double,
    target: Double,
    unit: String,
    colour: Color,
    modifier: Modifier = Modifier,
    isKcalBar: Boolean = false,
) {
    val isOver = current > target && target > 0.0
    val progress = if (target > 0.0) (current / target).toFloat().coerceIn(0f, 1f) else 0f
    val remaining = target - current
    val barColour = if (isOver) Overage else colour

    Column(modifier = modifier.fillMaxWidth()) {
        if (isKcalBar) {
            KcalLabelRow(
                current = current,
                target = target,
                unit = unit,
                isOver = isOver,
                remaining = remaining,
            )
        } else {
            MacroLabelRow(
                label = label,
                current = current,
                target = target,
                unit = unit,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .semantics {
                    stateDescription = if (isOver) {
                        "$label: ${formatProgressValue(current, unit)}, over target by ${formatProgressValue(-remaining, unit)}"
                    } else {
                        "$label: ${formatProgressValue(current, unit)} of ${formatProgressValue(target, unit)}, ${formatProgressValue(remaining, unit)} remaining"
                    }
                },
            color = barColour,
            trackColor = ProgressTrack,
            strokeCap = StrokeCap.Round,
        )
    }
}

/**
 * Label row for the kcal bar.
 * Left: "X / Y kcal" in titleMedium.
 * Right: "Remaining: Z kcal" or "Over: Z kcal" in bodyMedium.
 */
@Composable
private fun KcalLabelRow(
    current: Double,
    target: Double,
    unit: String,
    isOver: Boolean,
    remaining: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${formatProgressValue(current, unit)} / ${formatProgressValue(target, unit)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (target > 0.0) {
            if (isOver) {
                Text(
                    text = "Over: ${formatProgressValue(-remaining, unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Overage,
                )
            } else {
                Text(
                    text = "Remaining: ${formatProgressValue(remaining, unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Label row for a macro bar.
 * Left: label in labelSmall / onSurfaceVariant.
 * Right: "X / Yg" in bodyMedium.
 */
@Composable
private fun MacroLabelRow(
    label: String,
    current: Double,
    target: Double,
    unit: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${formatProgressValue(current, unit)} / ${formatProgressValue(target, unit)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatProgressValue(value: Double, unit: String): String {
    return if (unit == "kcal") {
        "${com.delve.hungrywalrus.util.Formatter.formatKcal(value)} $unit"
    } else {
        "${com.delve.hungrywalrus.util.Formatter.formatMacro(value)}$unit"
    }
}
