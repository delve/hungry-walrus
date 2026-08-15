package com.delve.hungrywalrus.ui.component

import com.delve.hungrywalrus.ui.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the chip model behind [QuickWeightSelector] (design spec sections 3.9
 * elements 5-6 and 5.5).
 *
 * The component renders a wrapping chip group whose ordering and selection rules are resolved
 * by [quickWeightChips]; these tests exercise that resolution directly.
 */
class QuickWeightSelectorTest {

    private val defaultOptions = listOf(25, 50, 100, 150, 200, 250)

    private fun chips(
        options: List<Int> = defaultOptions,
        selectedValue: Int? = null,
        show100Percent: Boolean = false,
        hundredPercentWeight: Double? = null,
    ) = quickWeightChips(options, selectedValue, show100Percent, hundredPercentWeight)

    @Test
    fun `100 percent chip is rendered first when a reference weight is available`() {
        val result = chips(show100Percent = true, hundredPercentWeight = 420.0)

        assertEquals(
            listOf("100%", "25g", "50g", "100g", "150g", "200g", "250g"),
            result.map { it.label },
        )
        assertTrue(result.first() is QuickWeightChip.HundredPercent)
    }

    @Test
    fun `100 percent chip is omitted and the group starts with 25g when not applicable`() {
        val result = chips(show100Percent = false, hundredPercentWeight = 420.0)

        assertEquals(
            listOf("25g", "50g", "100g", "150g", "200g", "250g"),
            result.map { it.label },
        )
    }

    @Test
    fun `100 percent chip is omitted when the flag is set but no reference weight is known`() {
        val result = chips(show100Percent = true, hundredPercentWeight = null)

        assertEquals(defaultOptions.size, result.size)
        assertTrue(result.none { it is QuickWeightChip.HundredPercent })
        assertEquals("25g", result.first().label)
    }

    @Test
    fun `dropping the 100 percent chip leaves no gap or placeholder`() {
        val withChip = chips(show100Percent = true, hundredPercentWeight = 420.0)
        val withoutChip = chips(show100Percent = false)

        assertEquals(withChip.size - 1, withoutChip.size)
        assertEquals(withChip.drop(1).map { it.label }, withoutChip.map { it.label })
    }

    @Test
    fun `gram chip order follows the order supplied by the caller`() {
        val result = chips(options = listOf(10, 30, 75))

        assertEquals(listOf("10g", "30g", "75g"), result.map { it.label })
    }

    @Test
    fun `empty options yield only the 100 percent chip`() {
        val result = chips(
            options = emptyList(),
            show100Percent = true,
            hundredPercentWeight = 320.0,
        )

        assertEquals(1, result.size)
        assertEquals("100%", result.single().label)
    }

    @Test
    fun `gram chip resolves to its own weight`() {
        val result = chips()

        assertEquals(
            defaultOptions,
            result.filterIsInstance<QuickWeightChip.Grams>().map { it.weightG },
        )
    }

    @Test
    fun `100 percent chip resolves to the reference total weight`() {
        val result = chips(show100Percent = true, hundredPercentWeight = 437.5)

        val hundred = result.filterIsInstance<QuickWeightChip.HundredPercent>().single()
        assertEquals(437.5, hundred.weightG, 0.0001)
    }

    @Test
    fun `gram chip matching the current weight renders selected`() {
        val result = chips(selectedValue = 150)

        assertEquals(listOf("150g"), result.filter { it.selected }.map { it.label })
    }

    @Test
    fun `no chip is selected for a weight that matches none of the presets`() {
        val result = chips(selectedValue = 137, show100Percent = true, hundredPercentWeight = 420.0)

        assertTrue(result.none { it.selected })
    }

    @Test
    fun `no chip is selected when the weight field is empty or invalid`() {
        val result = chips(selectedValue = null, show100Percent = true, hundredPercentWeight = 420.0)

        assertTrue(result.none { it.selected })
    }

    @Test
    fun `100 percent chip matching the current weight renders selected`() {
        val result = chips(selectedValue = 420, show100Percent = true, hundredPercentWeight = 420.0)

        assertEquals(listOf("100%"), result.filter { it.selected }.map { it.label })
    }

    @Test
    fun `a fractional reference weight never renders the 100 percent chip selected`() {
        // Tapping the chip writes a whole number of grams into the weight field, so a
        // fractional reference weight can never equal the field's value.
        listOf(437, 438).forEach { typedWeight ->
            val result = chips(
                selectedValue = typedWeight,
                show100Percent = true,
                hundredPercentWeight = 437.5,
            )
            assertFalse(
                "weight $typedWeight should not select the 100% chip",
                result.first().selected,
            )
        }
    }

    @Test
    fun `a reference weight equal to a gram preset selects both chips`() {
        // Both chips resolve to the same weight, so both correctly reflect the current value.
        val result = chips(selectedValue = 100, show100Percent = true, hundredPercentWeight = 100.0)

        assertEquals(listOf("100%", "100g"), result.filter { it.selected }.map { it.label })
    }

    @Test
    fun `chip spacing uses the sm token on both axes`() {
        assertEquals(Spacing.sm, QuickWeightChipSpacing)
    }
}
