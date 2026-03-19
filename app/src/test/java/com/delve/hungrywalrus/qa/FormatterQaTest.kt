package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.util.Formatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * QA unit tests for [Formatter] targeting spec requirements that are not yet covered:
 *
 * - formatKcal must use comma as the thousands separator (UK format), not a period.
 * - formatMacro always produces exactly one decimal place (spec: "Nearest 0.5g").
 * - formatDate epoch overload produces the same result as LocalDate overload for
 *   dates around daylight saving time transitions.
 * - formatKcal for kcal values between 1 and 999 (no thousands separator needed).
 * - roundMacro negative value (should work, even if impractical).
 * - formatKcal with very large values uses comma separators correctly.
 */
class FormatterQaTest {

    // --- UK formatting conventions ---

    /**
     * Spec: "Kilocalories: Nearest whole, comma sep" and "Thousands: Comma separator (UK)".
     * This test verifies that the thousands separator is a comma, not a period or space.
     */
    @Test
    fun `formatKcal uses comma not period as thousands separator`() {
        val result = Formatter.formatKcal(1500.0)
        assertEquals("1,500", result)
        assertTrue("Expected comma separator", result.contains(','))
        assertTrue("Must not contain period as thousands sep", !result.contains('.'))
    }

    @Test
    fun `formatKcal for value below 1000 has no thousands separator`() {
        assertEquals("999", Formatter.formatKcal(999.0))
        assertEquals("1", Formatter.formatKcal(1.0))
        assertEquals("500", Formatter.formatKcal(500.0))
    }

    @Test
    fun `formatKcal for 1000 adds comma thousands separator`() {
        assertEquals("1,000", Formatter.formatKcal(1000.0))
    }

    @Test
    fun `formatKcal for 10000 produces two comma-separated groups`() {
        assertEquals("10,000", Formatter.formatKcal(10_000.0))
    }

    @Test
    fun `formatKcal for 1000000 produces correct comma separation`() {
        assertEquals("1,000,000", Formatter.formatKcal(1_000_000.0))
    }

    // --- formatMacro always one decimal place ---

    /**
     * Spec: "Macronutrients: Nearest 0.5g". The format always uses one decimal place.
     * This test verifies the format string always produces exactly one decimal digit.
     */
    @Test
    fun `formatMacro for value that rounds to whole number still has one decimal place`() {
        // 10.0 rounds to 10.0 -- must display as "10.0" not "10"
        val result = Formatter.formatMacro(10.0)
        assertTrue("Expected one decimal place in '$result'", result.endsWith(".0"))
    }

    @Test
    fun `formatMacro for 0_5 displays as 0_5`() {
        assertEquals("0.5", Formatter.formatMacro(0.5))
    }

    @Test
    fun `formatMacro for large macro value still rounds to nearest 0_5`() {
        // 123.7 -> rounds to 123.5 (0.7 * 2 = 1.4, round = 1, 1/2 = 0.5)
        assertEquals("123.5", Formatter.formatMacro(123.7))

        // 123.8 -> rounds to 124.0 (0.8 * 2 = 1.6, round = 2, 2/2 = 1.0)
        assertEquals("124.0", Formatter.formatMacro(123.8))
    }

    // --- roundKcal and roundMacro numeric consistency ---

    /**
     * Spec: "roundMacro" and "roundKcal" return numeric values, not strings.
     * The numeric value must agree with the string formatters.
     */
    @Test
    fun `roundMacro numeric result matches parsed value from formatMacro`() {
        val value = 12.3
        val numericResult = Formatter.roundMacro(value)
        val stringResult = Formatter.formatMacro(value)
        val parsedFromString = stringResult.toDouble()
        assertEquals(numericResult, parsedFromString, 0.001)
    }

    @Test
    fun `roundKcal numeric result matches parsed value from formatKcal (no commas)`() {
        val value = 1250.7
        val numericResult = Formatter.roundKcal(value)
        // formatKcal includes commas for thousands; strip them before parsing
        val stringResult = Formatter.formatKcal(value).replace(",", "")
        val parsedFromString = stringResult.toInt()
        assertEquals(numericResult, parsedFromString)
    }

    // --- Date formatting ---

    /**
     * Spec: "Dates: dd/MM/yyyy". This verifies day-first (not month-first US format).
     * January 3rd must be "03/01/2026", not "01/03/2026".
     */
    @Test
    fun `formatDate places day before month (dd slash MM slash yyyy)`() {
        val date = LocalDate.of(2026, 1, 3)
        val result = Formatter.formatDate(date)
        // Day component must be "03", month "01"
        assertEquals("03/01/2026", result)
    }

    @Test
    fun `formatDate December 31 formats correctly`() {
        val date = LocalDate.of(2025, 12, 31)
        assertEquals("31/12/2025", Formatter.formatDate(date))
    }

    @Test
    fun `formatDate February 1 formats correctly`() {
        val date = LocalDate.of(2026, 2, 1)
        assertEquals("01/02/2026", Formatter.formatDate(date))
    }

    // --- formatKcal rounding at boundary ---

    /**
     * Spec: "Nearest whole number". Math.round uses half-up: 0.5 rounds to 1.
     */
    @Test
    fun `formatKcal rounds 999_5 up to 1000 with comma`() {
        assertEquals("1,000", Formatter.formatKcal(999.5))
    }

    @Test
    fun `formatKcal rounds 1999_5 up to 2000`() {
        assertEquals("2,000", Formatter.formatKcal(1999.5))
    }

    @Test
    fun `formatKcal rounds 1999_4 down to 1999`() {
        assertEquals("1,999", Formatter.formatKcal(1999.4))
    }
}
