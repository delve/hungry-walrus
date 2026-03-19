package com.delve.hungrywalrus.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items matching the design specification section 2.1.
 */
enum class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    TODAY(
        label = "Today",
        icon = Icons.Default.CalendarToday,
        route = Routes.DAILY_PROGRESS,
    ),
    RECIPES(
        label = "Recipes",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        route = Routes.RECIPES,
    ),
    SUMMARIES(
        label = "Summaries",
        icon = Icons.Default.BarChart,
        route = Routes.SUMMARIES,
    ),
    SETTINGS(
        label = "Settings",
        icon = Icons.Default.Settings,
        route = Routes.SETTINGS,
    ),
}
