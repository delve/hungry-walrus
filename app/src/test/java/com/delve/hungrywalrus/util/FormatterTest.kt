package com.delve.hungrywalrus.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FormatterTest {

    @Test
    fun `formatDate formats LocalDate as dd-MM-yyyy UK style`() {
        val date = LocalDate.of(2026, 3, 19)
        assertEquals("19/03/2026", Formatter.formatDate(date))
    }

    @Test
    fun `formatDate handles single digit day and month`() {
        val date = LocalDate.of(2026, 1, 5)
        assertEquals("05/01/2026", Formatter.formatDate(date))
    }

    @Test
    fun `formatKcal rounds to nearest whole number`() {
        assertEquals("1,250", Formatter.formatKcal(1250.4))
        assertEquals("1,251", Formatter.formatKcal(1250.5))
        assertEquals("0", Formatter.formatKcal(0.0))
    }

    @Test
    fun `formatKcal uses comma thousands separator`() {
        assertEquals("12,500", Formatter.formatKcal(12500.0))
        assertEquals("100", Formatter.formatKcal(100.0))
        assertEquals("1,000", Formatter.formatKcal(1000.0))
    }

    @Test
    fun `formatMacro rounds to nearest 0_5g`() {
        assertEquals("12.5", Formatter.formatMacro(12.3))
        assertEquals("12.5", Formatter.formatMacro(12.5))
        assertEquals("12.0", Formatter.formatMacro(12.1))
        assertEquals("13.0", Formatter.formatMacro(12.8))
        assertEquals("12.5", Formatter.formatMacro(12.74))
        assertEquals("13.0", Formatter.formatMacro(12.75))
    }

    @Test
    fun `formatMacro handles zero`() {
        assertEquals("0.0", Formatter.formatMacro(0.0))
    }

    @Test
    fun `formatMacro handles whole numbers`() {
        assertEquals("30.0", Formatter.formatMacro(30.0))
        assertEquals("30.0", Formatter.formatMacro(29.9))
        assertEquals("30.0", Formatter.formatMacro(30.1))
    }

    @Test
    fun `roundMacro returns numeric value rounded to nearest 0_5`() {
        assertEquals(12.5, Formatter.roundMacro(12.3), 0.001)
        assertEquals(12.0, Formatter.roundMacro(12.1), 0.001)
        assertEquals(13.0, Formatter.roundMacro(12.8), 0.001)
    }

    @Test
    fun `roundKcal returns whole number`() {
        assertEquals(1250, Formatter.roundKcal(1250.4))
        assertEquals(1251, Formatter.roundKcal(1250.5))
        assertEquals(0, Formatter.roundKcal(0.0))
    }
}
