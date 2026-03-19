package com.delve.hungrywalrus.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Centralised formatting utilities for the app.
 * All formatting follows UK conventions as specified in the architecture document (section 18).
 */
object Formatter {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)

    /**
     * Format a [LocalDate] as dd/MM/yyyy.
     */
    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    /**
     * Format an epoch millis timestamp as dd/MM/yyyy.
     */
    fun formatDate(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return formatDate(date)
    }

    /**
     * Format an epoch millis timestamp as HH:mm.
     */
    fun formatTime(epochMillis: Long): String {
        val time = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return time.format(timeFormatter)
    }

    /**
     * Format kilocalories: rounded to nearest whole number with comma thousands separator.
     * Example: 1250.7 -> "1,250"
     */
    fun formatKcal(value: Double): String {
        val rounded = Math.round(value).toInt()
        return String.format(Locale.UK, "%,d", rounded)
    }

    /**
     * Format a macronutrient value: rounded to nearest 0.5g.
     * Example: 12.3 -> "12.5", 12.1 -> "12.0"
     */
    fun formatMacro(value: Double): String {
        val rounded = Math.round(value * 2.0) / 2.0
        return if (rounded == rounded.toLong().toDouble()) {
            String.format(Locale.UK, "%.1f", rounded)
        } else {
            String.format(Locale.UK, "%.1f", rounded)
        }
    }

    /**
     * Round a macronutrient value to nearest 0.5g (numeric, not string).
     */
    fun roundMacro(value: Double): Double {
        return Math.round(value * 2.0) / 2.0
    }

    /**
     * Round kilocalories to nearest whole number (numeric, not string).
     */
    fun roundKcal(value: Double): Int {
        return Math.round(value).toInt()
    }
}
