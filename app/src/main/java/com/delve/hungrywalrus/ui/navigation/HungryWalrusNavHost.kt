package com.delve.hungrywalrus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

/**
 * Main NavHost with empty placeholder destinations for each screen.
 * Each destination will be replaced with actual screen composables in later sessions.
 */
@Composable
fun HungryWalrusNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DAILY_PROGRESS,
        modifier = modifier,
    ) {
        // Top-level destinations
        composable(Routes.DAILY_PROGRESS) {
            PlaceholderScreen("Daily Progress")
        }

        composable(Routes.PLAN) {
            PlaceholderScreen("Nutrition Plan")
        }

        composable(Routes.RECIPES) {
            PlaceholderScreen("Recipes")
        }

        composable(
            route = Routes.RECIPE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            PlaceholderScreen("Recipe Detail")
        }

        composable(Routes.RECIPE_CREATE) {
            PlaceholderScreen("Create Recipe")
        }

        composable(
            route = Routes.RECIPE_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            PlaceholderScreen("Edit Recipe")
        }

        composable(Routes.SUMMARIES) {
            PlaceholderScreen("Summaries")
        }

        composable(Routes.SETTINGS) {
            PlaceholderScreen("Settings")
        }

        // Meal logging nested navigation graph
        navigation(
            startDestination = Routes.LOG_METHOD,
            route = Routes.LOG_GRAPH,
        ) {
            composable(Routes.LOG_METHOD) {
                PlaceholderScreen("Log Method Selection")
            }

            composable(
                route = Routes.LOG_SEARCH,
                arguments = listOf(navArgument("source") { type = NavType.StringType }),
            ) {
                PlaceholderScreen("Food Search")
            }

            composable(Routes.LOG_BARCODE) {
                PlaceholderScreen("Barcode Scanner")
            }

            composable(Routes.LOG_MANUAL) {
                PlaceholderScreen("Manual Entry")
            }

            composable(Routes.LOG_RECIPE_SELECT) {
                PlaceholderScreen("Recipe Selection")
            }

            composable(Routes.LOG_WEIGHT_ENTRY) {
                PlaceholderScreen("Weight Entry")
            }

            composable(Routes.LOG_MISSING_VALUES) {
                PlaceholderScreen("Missing Values")
            }

            composable(Routes.LOG_CONFIRM) {
                PlaceholderScreen("Entry Confirmation")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "TODO: $name")
    }
}
