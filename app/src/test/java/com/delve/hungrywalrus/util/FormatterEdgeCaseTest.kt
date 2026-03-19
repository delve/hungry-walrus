package com.delve.hungrywalrus.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Additional unit tests for [Formatter] covering edge cases not covered by FormatterTest.
 *
 * Note on rounding: [Formatter.roundMacro] uses Math.round which applies "half-up" rounding.
 * For the nearest-0.5g rounding, the formula is Math.round(value * 2.0) / 2.0.
 * This means 0.25 * 2 = 0.5, Math.round(0.5) = 1, result = 0.5 (rounds UP at the midpoint).
 */
class FormatterEdgeCaseTest {

    // --- formatKcal edge cases ---

    @Test
    fun `formatKcal rounds 0_5 up (half-up rounding)`() {
        // 0.5 rounds to 1 with Math.round (rounds half up)
        assertEquals("1", Formatter.formatKcal(0.5))
    }

    @Test
    fun `formatKcal handles large value with comma separator`() {
        assertEquals("10,000", Formatter.formatKcal(10000.0))
        assertEquals("100,000", Formatter.formatKcal(100000.0))
    }

    @Test
    fun `formatKcal handles negative value without throwing`() {
        // Negative calorie values should not appear in practice but the formatter should not crash.
        val result = Formatter.formatKcal(-5.0)
        // Simply verify it doesn't throw and produces some string
        assertTrue(result.isNotEmpty())
    }

    // --- formatMacro edge cases ---

    @Test
    fun `formatMacro rounds 0_25 up to 0_5 with half-up rounding`() {
        // 0.25 * 2 = 0.5, Math.round(0.5) = 1, result = 0.5 (half-up rounds the midpoint up)
        assertEquals("0.5", Formatter.formatMacro(0.25))
    }

    @Test
    fun `formatMacro rounds 0_24 down to 0_0`() {
        // 0.24 * 2 = 0.48, Math.round(0.48) = 0, result = 0.0
        assertEquals("0.0", Formatter.formatMacro(0.24))
    }

    @Test
    fun `formatMacro rounds 0_26 up to 0_5`() {
        // 0.26 * 2 = 0.52 -> round = 1 -> 0.5
        assertEquals("0.5", Formatter.formatMacro(0.26))
    }

    @Test
    fun `formatMacro handles large macro value`() {
        assertEquals("500.0", Formatter.formatMacro(500.0))
    }

    @Test
    fun `formatMacro preserves one decimal place for whole numbers`() {
        // The formatter always shows one decimal place per spec (e.g. "30.0" not "30")
        val result = Formatter.formatMacro(30.0)
        assertTrue("Expected one decimal place but got: $result", result.contains("."))
    }

    // --- roundMacro edge cases ---

    @Test
    fun `roundMacro rounds 0_25 up to 0_5 with half-up rounding`() {
        // 0.25 * 2 = 0.5, Math.round(0.5) = 1, 1 / 2.0 = 0.5 (half-up rounds the midpoint up)
        assertEquals(0.5, Formatter.roundMacro(0.25), 0.001)
    }

    @Test
    fun `roundMacro rounds 0_24 down to 0_0`() {
        assertEquals(0.0, Formatter.roundMacro(0.24), 0.001)
    }

    @Test
    fun `roundMacro rounds 0_75 up to 1_0`() {
        // 0.75 * 2 = 1.5, Math.round(1.5) = 2, 2 / 2.0 = 1.0
        assertEquals(1.0, Formatter.roundMacro(0.75), 0.001)
    }

    @Test
    fun `roundMacro is idempotent on already rounded values`() {
        assertEquals(12.5, Formatter.roundMacro(12.5), 0.001)
        assertEquals(12.0, Formatter.roundMacro(12.0), 0.001)
    }

    // --- roundKcal edge cases ---

    @Test
    fun `roundKcal rounds 0_5 up`() {
        assertEquals(1, Formatter.roundKcal(0.5))
    }

    @Test
    fun `roundKcal is unchanged for whole numbers`() {
        assertEquals(500, Formatter.roundKcal(500.0))
    }

    // --- formatDate from epoch millis ---

    @Test
    fun `formatDate epoch millis returns parseable date string`() {
        // We verify the returned string has the expected format (dd/MM/yyyy).
        val millis = java.time.LocalDate.of(2026, 3, 21)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val result = Formatter.formatDate(millis)
        // Format must match dd/MM/yyyy
        assertTrue(
            "Expected dd/MM/yyyy format but got: $result",
            result.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))
        )
    }

    @Test
    fun `formatDate LocalDate and epoch millis overloads produce same result for same date`() {
        val date = LocalDate.of(2026, 6, 15)
        val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(Formatter.formatDate(date), Formatter.formatDate(millis))
    }

    // --- formatTime ---

    @Test
    fun `formatTime returns HH_mm formatted string`() {
        val millis = java.time.LocalDateTime.of(2026, 3, 21, 14, 35)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val result = Formatter.formatTime(millis)
        assertTrue(
            "Expected HH:mm format but got: $result",
            result.matches(Regex("\\d{2}:\\d{2}"))
        )
    }

    @Test
    fun `formatTime pads single digit hour and minute with leading zero`() {
        val millis = java.time.LocalDateTime.of(2026, 3, 21, 1, 5)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val result = Formatter.formatTime(millis)
        assertEquals("01:05", result)
    }
}
