package com.delve.hungrywalrus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [OfflineException].
 *
 * Added in Pass 2 of the domain layer code review (alongside the move of
 * `OfflineException.kt` from `com.delve.hungrywalrus.domain` to
 * `com.delve.hungrywalrus.domain.model` per finding O07). The move itself
 * does not change behaviour, but the previously untested class now lives
 * in its prescribed location and has baseline tests for the public surface.
 */
class OfflineExceptionTest {

    @Test
    fun `default constructor sets the documented default message`() {
        val ex = OfflineException()
        assertEquals("No network connection available", ex.message)
    }

    @Test
    fun `custom message is preserved`() {
        val ex = OfflineException("Network error: unable to reach USDA service")
        assertEquals("Network error: unable to reach USDA service", ex.message)
    }

    @Test
    fun `is an Exception subclass so it can be wrapped in Result_failure`() {
        val ex: Exception = OfflineException()
        assertNotNull(ex)
        assertTrue(ex is Exception)
    }

    @Test
    fun `can be caught as Exception`() {
        var caught: Exception? = null
        try {
            throw OfflineException("offline")
        } catch (e: Exception) {
            caught = e
        }
        assertNotNull(caught)
        assertTrue(caught is OfflineException)
        assertEquals("offline", caught!!.message)
    }

    @Test
    fun `can be caught specifically as OfflineException`() {
        var caught: OfflineException? = null
        try {
            throw OfflineException()
        } catch (e: OfflineException) {
            caught = e
        }
        assertNotNull(caught)
    }

    @Test
    fun `Result_failure wrapping round-trips the exception instance`() {
        val ex = OfflineException("disconnected")
        val result: Result<Nothing> = Result.failure(ex)
        assertTrue(result.isFailure)
        val unwrapped = result.exceptionOrNull()
        assertTrue(unwrapped is OfflineException)
        assertEquals("disconnected", unwrapped!!.message)
    }
}
