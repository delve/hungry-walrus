package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.domain.usecase.ComputeRollingSummaryUseCase
import org.junit.Test
import java.time.LocalDate

/**
 * QA unit tests for [ComputeRollingSummaryUseCase] precondition guards.
 *
 * Gap filled:
 * - The use case contains `require(!start.isAfter(end)) { "start must not be after end" }`.
 *   No existing test verifies that this guard throws an [IllegalArgumentException] when
 *   invoked with an invalid date range. Verifying preconditions is important because
 *   the ViewModel is responsible for computing valid window boundaries — a regression
 *   that inverts start/end would otherwise produce a silent incorrect result.
 */
class ComputeRollingSummaryPreconditionTest {

    private val useCase = ComputeRollingSummaryUseCase()

    /**
     * start.isAfter(end) by one week must throw [IllegalArgumentException].
     * This verifies the precondition guard catches an obviously invalid range.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `start one week after end throws IllegalArgumentException`() {
        val end = LocalDate.of(2026, 3, 14)
        val start = LocalDate.of(2026, 3, 21) // start > end
        useCase(emptyList(), emptyMap(), start, end)
    }

    /**
     * start exactly one day after end must throw [IllegalArgumentException].
     * This tests the tight boundary — start must be <= end, so start = end+1 is invalid.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `start one day after end throws IllegalArgumentException`() {
        val end = LocalDate.of(2026, 5, 20)
        val start = end.plusDays(1) // start > end by exactly 1 day
        useCase(emptyList(), emptyMap(), start, end)
    }

    /**
     * start == end is valid (single-day period). Verify this does NOT throw.
     * This anchors the boundary — the guard only fires when start.isAfter(end),
     * not when start.isEqual(end).
     */
    @Test
    fun `start equal to end does not throw (single-day period)`() {
        val day = LocalDate.of(2026, 3, 20)
        // Must not throw
        val summary = useCase(emptyList(), emptyMap(), day, day)
        org.junit.Assert.assertEquals(1, summary.periodDays)
    }
}
