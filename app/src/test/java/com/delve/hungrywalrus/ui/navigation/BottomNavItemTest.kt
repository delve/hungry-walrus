package com.delve.hungrywalrus.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavItemTest {

    @Test
    fun `bottom nav has exactly four items`() {
        assertEquals(4, BottomNavItem.entries.size)
    }

    @Test
    fun `bottom nav items have correct routes`() {
        assertEquals(Routes.DAILY_PROGRESS, BottomNavItem.TODAY.route)
        assertEquals(Routes.RECIPES, BottomNavItem.RECIPES.route)
        assertEquals(Routes.SUMMARIES, BottomNavItem.SUMMARIES.route)
        assertEquals(Routes.SETTINGS, BottomNavItem.SETTINGS.route)
    }

    @Test
    fun `bottom nav items have correct labels`() {
        assertEquals("Today", BottomNavItem.TODAY.label)
        assertEquals("Recipes", BottomNavItem.RECIPES.label)
        assertEquals("Summaries", BottomNavItem.SUMMARIES.label)
        assertEquals("Settings", BottomNavItem.SETTINGS.label)
    }
}
