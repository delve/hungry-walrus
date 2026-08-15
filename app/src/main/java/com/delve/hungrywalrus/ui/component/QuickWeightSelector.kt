package com.delve.hungrywalrus.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.delve.hungrywalrus.ui.theme.Spacing

/**
 * Gap between adjacent chips on a line and between wrapped lines (design spec section 5.5).
 */
internal val QuickWeightChipSpacing: Dp = Spacing.sm

/**
 * A single chip in the quick-select group, resolved from the component's props.
 *
 * Modelling the chips as data keeps the ordering and selection rules independent of
 * composition so they can be exercised directly by unit tests.
 */
internal sealed interface QuickWeightChip {

    /** Text rendered inside the chip. */
    val label: String

    /** Whether the chip renders in [FilterChip] selected state. */
    val selected: Boolean

    /** A fixed gram preset such as 25g. Tapping it sets the weight field to [weightG]. */
    data class Grams(
        val weightG: Int,
        override val selected: Boolean,
    ) : QuickWeightChip {
        override val label: String get() = "${weightG}g"
    }

    /**
     * The whole-portion preset. Tapping it sets the weight field to [weightG], which is the
     * recipe's total weight or the packaged item's serving size.
     */
    data class HundredPercent(
        val weightG: Double,
        override val selected: Boolean,
    ) : QuickWeightChip {
        override val label: String get() = "100%"
    }
}

/**
 * Builds the ordered chip list for [QuickWeightSelector].
 *
 * The "100%" chip leads the group when it applies, followed by the gram presets in the order
 * supplied by the caller. When no reference total weight is known the chip is dropped entirely
 * and the group simply begins with the first gram preset — no placeholder occupies the leading
 * position.
 *
 * A chip renders selected when the current weight equals its value. The "100%" chip therefore
 * never highlights for a fractional reference weight, because the weight field only ever holds a
 * whole number of grams when set from a chip. A reference weight that coincides with a gram
 * preset highlights both chips, which is correct: they resolve to the same weight.
 */
internal fun quickWeightChips(
    options: List<Int>,
    selectedValue: Int?,
    show100Percent: Boolean,
    hundredPercentWeight: Double?,
): List<QuickWeightChip> {
    val chips = mutableListOf<QuickWeightChip>()
    if (show100Percent && hundredPercentWeight != null) {
        chips += QuickWeightChip.HundredPercent(
            weightG = hundredPercentWeight,
            selected = selectedValue != null && selectedValue.toDouble() == hundredPercentWeight,
        )
    }
    options.mapTo(chips) { weight ->
        QuickWeightChip.Grams(weightG = weight, selected = selectedValue == weight)
    }
    return chips
}

/**
 * Wrapping group of weight quick-select chips.
 *
 * Every chip is composed at once and the group wraps onto as many lines as the available width
 * requires, so no preset is hidden behind a scroll gesture. The measured height therefore varies
 * with width and font scale: callers must not constrain this component to a fixed row height.
 *
 * [FlowRow] is opted into explicitly: the wrapping layout API is still marked experimental in the
 * Compose Foundation version resolved by the pinned BOM. Only the arrangement parameters used
 * here are relied upon, so a future signature change is contained to this file.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    val chips = quickWeightChips(
        options = options,
        selectedValue = selectedValue,
        show100Percent = show100Percent,
        hundredPercentWeight = hundredPercentWeight,
    )

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(QuickWeightChipSpacing),
        verticalArrangement = Arrangement.spacedBy(QuickWeightChipSpacing),
    ) {
        chips.forEach { chip ->
            FilterChip(
                selected = chip.selected,
                onClick = {
                    when (chip) {
                        is QuickWeightChip.Grams -> onSelect(chip.weightG)
                        is QuickWeightChip.HundredPercent -> onSelect100Percent()
                    }
                },
                label = { Text(chip.label) },
            )
        }
    }
}
