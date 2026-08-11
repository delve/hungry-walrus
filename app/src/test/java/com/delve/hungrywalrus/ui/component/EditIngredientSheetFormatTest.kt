package com.delve.hungrywalrus.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for [formatNumberForEdit] — the pre-population formatter used by
 * [EditIngredientSheet] to render stored `Double` field values into editable text
 * (architecture §7.8, design §3.13a).
 *
 * O10 (UI Pass 1): These tests guard against the silent-rounding regression that
 * would occur if the sheet were to use `Formatter.formatMacro()` (which rounds to
 * the nearest 0.5g) for the weight or per-100g fields. The display rounding in
 * §7.4 applies only to surfaces such as progress bars and list rows; in-place edit
 * inputs must preserve the stored precision so confirming the sheet unchanged is
 * a true no-op on the underlying value.
 */
class EditIngredientSheetFormatTest {

    @Test
    fun `whole-number value renders without trailing zero`() {
        assertEquals("100", formatNumberForEdit(100.0))
        assertEquals("0", formatNumberForEdit(0.0))
        assertEquals("1", formatNumberForEdit(1.0))
        assertEquals("250", formatNumberForEdit(250.0))
    }

    @Test
    fun `fractional non-half-gram value is preserved exactly`() {
        // The pre-existing implementation used Formatter.formatMacro here, which would
        // have produced "76.5" — silently rounding the stored 76.3 up by 0.2g.
        assertEquals("76.3", formatNumberForEdit(76.3))
        assertEquals("12.1", formatNumberForEdit(12.1))
        assertEquals("0.3", formatNumberForEdit(0.3))
    }

    @Test
    fun `half-gram fractional value is preserved exactly`() {
        // Even values that happen to be at the 0.5g granularity must not be rendered
        // as integers; the round-trip test below would fail if we did so.
        assertEquals("12.5", formatNumberForEdit(12.5))
        assertEquals("0.5", formatNumberForEdit(0.5))
        assertEquals("99.5", formatNumberForEdit(99.5))
    }

    @Test
    fun `large whole-number value renders without exponent`() {
        // Defensive: Double.toString() can use scientific notation for very large or
        // very small magnitudes. Recipe ingredient weights live well within Long range
        // and should always render as plain digits.
        assertEquals("100000", formatNumberForEdit(100_000.0))
        assertEquals("1000000", formatNumberForEdit(1_000_000.0))
    }

    @Test
    fun `negative whole-number value renders without trailing zero`() {
        // Negative values are rejected by the sheet's input validation before save,
        // but the formatter must still produce a string that round-trips so the
        // validation code can read it back.
        assertEquals("-5", formatNumberForEdit(-5.0))
    }

    @Test
    fun `every produced string round-trips through toDoubleOrNull`() {
        // Critical invariant: the sheet's validation reads each field via
        // toDoubleOrNull() and bails out on null. Pre-population must produce a
        // string that parses back into a numeric value or the Save button would be
        // disabled on a freshly-opened sheet.
        listOf(
            0.0, 0.3, 0.5, 1.0, 12.1, 12.5, 76.3, 99.5,
            100.0, 250.0, 250.5, 1_000.0, 100_000.0, -5.0,
        ).forEach { value ->
            val rendered = formatNumberForEdit(value)
            val parsed = rendered.toDoubleOrNull()
            assertNotNull("rendered '$rendered' did not parse", parsed)
            assertEquals(
                "rendered '$rendered' did not round-trip for input $value",
                value,
                parsed!!,
                0.0001,
            )
        }
    }

    @Test
    fun `precision-loss regression for stored 76 point 3 grams weight`() {
        // O10 regression: a recipe ingredient stored at 76.3g must surface in the edit
        // sheet as "76.3", not as the rounded "76.5" that the previous implementation
        // produced via Formatter.formatMacro. Confirming the sheet without changing
        // the weight would otherwise silently update the stored value from 76.3 to 76.5.
        val rendered = formatNumberForEdit(76.3)
        assertEquals("76.3", rendered)
        assertEquals(76.3, rendered.toDoubleOrNull()!!, 0.0001)
    }
}
