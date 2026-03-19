package com.delve.hungrywalrus.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun `logSearch builds correct route for usda source`() {
        assertEquals("log/search/usda", Routes.logSearch("usda"))
    }

    @Test
    fun `logSearch builds correct route for off source`() {
        assertEquals("log/search/off", Routes.logSearch("off"))
    }

    @Test
    fun `recipeDetail builds correct route with id`() {
        assertEquals("recipes/detail/42", Routes.recipeDetail(42L))
    }

    @Test
    fun `recipeEdit builds correct route with id`() {
        assertEquals("recipes/edit/7", Routes.recipeEdit(7L))
    }

    @Test
    fun `start destination is daily_progress`() {
        assertEquals("daily_progress", Routes.DAILY_PROGRESS)
    }
}
