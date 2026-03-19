package com.delve.hungrywalrus.ui.navigation

/**
 * All navigation routes for the app, matching the architecture document section 11.2.
 */
object Routes {
    const val DAILY_PROGRESS = "daily_progress"
    const val PLAN = "plan"

    // Meal logging flow (nested graph)
    const val LOG_GRAPH = "log"
    const val LOG_METHOD = "log/method"
    const val LOG_SEARCH = "log/search/{source}"
    const val LOG_BARCODE = "log/barcode"
    const val LOG_MANUAL = "log/manual"
    const val LOG_RECIPE_SELECT = "log/recipe_select"
    const val LOG_WEIGHT_ENTRY = "log/weight_entry"
    const val LOG_MISSING_VALUES = "log/missing_values"
    const val LOG_CONFIRM = "log/confirm"

    // Recipes
    const val RECIPES = "recipes"
    const val RECIPE_DETAIL = "recipes/detail/{id}"
    const val RECIPE_CREATE = "recipes/create"
    const val RECIPE_EDIT = "recipes/edit/{id}"

    // Summaries
    const val SUMMARIES = "summaries"

    // Settings
    const val SETTINGS = "settings"

    // Helper functions for parameterised routes
    fun logSearch(source: String): String = "log/search/$source"
    fun recipeDetail(id: Long): String = "recipes/detail/$id"
    fun recipeEdit(id: Long): String = "recipes/edit/$id"
}
