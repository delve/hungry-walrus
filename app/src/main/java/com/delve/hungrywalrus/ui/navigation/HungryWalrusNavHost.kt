package com.delve.hungrywalrus.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.delve.hungrywalrus.ui.screen.addentry.AddEntryViewModel
import com.delve.hungrywalrus.ui.screen.addentry.EntryConfirmScreen
import com.delve.hungrywalrus.ui.screen.addentry.LogMethodScreen
import com.delve.hungrywalrus.ui.screen.addentry.MissingValuesScreen
import com.delve.hungrywalrus.ui.screen.addentry.RecipeSelectScreen
import com.delve.hungrywalrus.ui.screen.addentry.WeightEntryScreen
import com.delve.hungrywalrus.ui.screen.barcodescan.BarcodeScanScreen
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeScreen
import com.delve.hungrywalrus.ui.screen.createrecipe.CreateRecipeViewModel
import com.delve.hungrywalrus.ui.screen.createrecipe.IngredientDraft
import com.delve.hungrywalrus.ui.screen.dailyprogress.DailyProgressScreen
import com.delve.hungrywalrus.ui.screen.foodsearch.FoodSearchScreen
import com.delve.hungrywalrus.ui.screen.manualentry.ManualEntryScreen
import com.delve.hungrywalrus.ui.screen.recipes.RecipeDetailScreen
import com.delve.hungrywalrus.ui.screen.recipes.RecipeListScreen
import com.delve.hungrywalrus.ui.screen.settings.SettingsScreen
import com.delve.hungrywalrus.ui.screen.summaries.SummariesScreen

/** Returns the back-stack entry for [route], or null if it is not on the back stack. */
private fun NavController.findBackStackEntry(route: String): NavBackStackEntry? =
    try { getBackStackEntry(route) } catch (_: Exception) { null }

/** Returns the nearest recipe back-stack entry, or null if no recipe screen is on the stack. */
private fun NavController.recipeBackStackEntryOrNull(): NavBackStackEntry? =
    findBackStackEntry(Routes.RECIPE_CREATE) ?: findBackStackEntry(Routes.RECIPE_EDIT)

/**
 * Observes the "newIngredient" bundle written to [backStackEntry]'s SavedStateHandle by the
 * ingredient sub-flow and forwards it to [recipeViewModel] as an [IngredientDraft].
 * Extracted to eliminate identical blocks in RECIPE_CREATE and RECIPE_EDIT.
 */
@Composable
private fun ObserveNewIngredient(
    backStackEntry: NavBackStackEntry,
    recipeViewModel: CreateRecipeViewModel,
) {
    val newIngredientBundle by backStackEntry.savedStateHandle
        .getStateFlow<Bundle?>("newIngredient", null)
        .collectAsStateWithLifecycle()
    LaunchedEffect(newIngredientBundle) {
        val bundle = newIngredientBundle ?: return@LaunchedEffect
        val name = bundle.getString("name") ?: return@LaunchedEffect
        // Clear the key before adding so that any recomposition triggered by addIngredient
        // observes a null value and skips a second delivery.
        backStackEntry.savedStateHandle.remove<Bundle>("newIngredient")
        recipeViewModel.addIngredient(
            IngredientDraft(
                name = name,
                weightG = bundle.getDouble("weightG"),
                kcalPer100g = bundle.getDouble("kcalPer100g"),
                proteinPer100g = bundle.getDouble("proteinPer100g"),
                carbsPer100g = bundle.getDouble("carbsPer100g"),
                fatPer100g = bundle.getDouble("fatPer100g"),
            ),
        )
    }
}

/**
 * Main NavHost with all screen composables wired up.
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
            DailyProgressScreen(
                onNavigateToPlan = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToLogMethod = {
                    navController.navigate(Routes.LOG_GRAPH)
                },
            )
        }

        composable(Routes.RECIPES) {
            RecipeListScreen(
                onNavigateToDetail = { id ->
                    navController.navigate(Routes.recipeDetail(id))
                },
                onNavigateToCreate = {
                    navController.navigate(Routes.RECIPE_CREATE)
                },
            )
        }

        composable(
            route = Routes.RECIPE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            RecipeDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Routes.recipeEdit(id))
                },
            )
        }

        composable(Routes.RECIPE_CREATE) { backStackEntry ->
            val recipeViewModel: CreateRecipeViewModel = hiltViewModel()
            ObserveNewIngredient(backStackEntry, recipeViewModel)

            CreateRecipeScreen(
                viewModel = recipeViewModel,
                onClose = { navController.popBackStack() },
                onRecipeSaved = {
                    navController.popBackStack(Routes.RECIPES, inclusive = false)
                },
                onNavigateToIngredientSearch = { source ->
                    navController.navigate(Routes.logSearch(source))
                },
                onNavigateToIngredientBarcode = {
                    navController.navigate(Routes.LOG_BARCODE)
                },
            )
        }

        composable(
            route = Routes.RECIPE_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { backStackEntry ->
            val recipeViewModel: CreateRecipeViewModel = hiltViewModel()
            ObserveNewIngredient(backStackEntry, recipeViewModel)

            CreateRecipeScreen(
                viewModel = recipeViewModel,
                onClose = { navController.popBackStack() },
                onRecipeSaved = {
                    navController.popBackStack(Routes.RECIPES, inclusive = false)
                },
                onNavigateToIngredientSearch = { source ->
                    navController.navigate(Routes.logSearch(source))
                },
                onNavigateToIngredientBarcode = {
                    navController.navigate(Routes.LOG_BARCODE)
                },
            )
        }

        composable(Routes.SUMMARIES) {
            SummariesScreen()
        }

        composable(Routes.SETTINGS) {
            SettingsScreen()
        }

        // Food-input screens declared at the top level so that navigating here from
        // RECIPE_CREATE / RECIPE_EDIT does not implicitly push LOG_METHOD onto the back stack.
        composable(
            route = Routes.LOG_SEARCH,
            arguments = listOf(navArgument("source") { type = NavType.StringType }),
        ) { backStackEntry ->
            val logGraphEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.LOG_GRAPH)
            }
            val viewModel: AddEntryViewModel =
                if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()
            val source = backStackEntry.arguments?.getString("source") ?: "off"

            FoodSearchScreen(
                source = source,
                viewModel = viewModel,
                onClose = {
                    val recipeEntry = navController.recipeBackStackEntryOrNull()
                    if (recipeEntry != null) {
                        navController.popBackStack(recipeEntry.destination.route!!, inclusive = false)
                    } else {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    }
                },
                onNavigateToWeightEntry = {
                    navController.navigate(Routes.LOG_WEIGHT_ENTRY)
                },
                onNavigateToMissingValues = {
                    navController.navigate(Routes.LOG_MISSING_VALUES)
                },
                onNavigateToManual = {
                    navController.navigate(Routes.LOG_MANUAL)
                },
            )
        }

        composable(Routes.LOG_BARCODE) { backStackEntry ->
            val logGraphEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.LOG_GRAPH)
            }
            val viewModel: AddEntryViewModel =
                if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()

            BarcodeScanScreen(
                viewModel = viewModel,
                onClose = {
                    val recipeEntry = navController.recipeBackStackEntryOrNull()
                    if (recipeEntry != null) {
                        navController.popBackStack(recipeEntry.destination.route!!, inclusive = false)
                    } else {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    }
                },
                onNavigateToWeightEntry = {
                    navController.navigate(Routes.LOG_WEIGHT_ENTRY)
                },
                onNavigateToMissingValues = {
                    navController.navigate(Routes.LOG_MISSING_VALUES)
                },
                onNavigateToManual = {
                    navController.navigate(Routes.LOG_MANUAL)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.LOG_MANUAL) { backStackEntry ->
            val logGraphEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.LOG_GRAPH)
            }
            val viewModel: AddEntryViewModel =
                if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()

            ManualEntryScreen(
                viewModel = viewModel,
                onClose = {
                    val recipeEntry = navController.recipeBackStackEntryOrNull()
                    if (recipeEntry != null) {
                        navController.popBackStack(recipeEntry.destination.route!!, inclusive = false)
                    } else {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    }
                },
                onNavigateToWeightEntry = {
                    navController.navigate(Routes.LOG_WEIGHT_ENTRY)
                },
            )
        }

        // Downstream food-entry screens are declared at the top level (not inside LOG_GRAPH) so
        // that navigating here from the ingredient sub-flow (RECIPE_CREATE/RECIPE_EDIT → LOG_SEARCH
        // → LOG_WEIGHT_ENTRY) does not cause Navigation Compose to implicitly push LOG_METHOD as
        // the LOG_GRAPH start-destination onto the back stack. The conditional-scope pattern for
        // AddEntryViewModel is the same as LOG_SEARCH / LOG_BARCODE / LOG_MANUAL above.
        composable(Routes.LOG_WEIGHT_ENTRY) { backStackEntry ->
            val logGraphEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.LOG_GRAPH)
            }
            val viewModel: AddEntryViewModel =
                if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()

            val recipeBackStackEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.RECIPE_CREATE)
                    ?: navController.findBackStackEntry(Routes.RECIPE_EDIT)
            }
            SideEffect { viewModel.setIngredientMode(recipeBackStackEntry != null) }

            val onIngredientAdded = remember(recipeBackStackEntry) {
                recipeBackStackEntry?.let { recipeEntry ->
                    {
                        val data = viewModel.getIngredientData()
                        if (data != null) {
                            val bundle = Bundle().apply {
                                putString("name", data.name)
                                putDouble("weightG", data.weightG)
                                putDouble("kcalPer100g", data.kcalPer100g)
                                putDouble("proteinPer100g", data.proteinPer100g)
                                putDouble("carbsPer100g", data.carbsPer100g)
                                putDouble("fatPer100g", data.fatPer100g)
                            }
                            recipeEntry.savedStateHandle["newIngredient"] = bundle
                            navController.popBackStack(
                                recipeEntry.destination.route!!,
                                inclusive = false,
                            )
                        }
                    }
                }
            }

            WeightEntryScreen(
                viewModel = viewModel,
                onClose = {
                    val recipeEntry = navController.recipeBackStackEntryOrNull()
                    if (recipeEntry != null) {
                        navController.popBackStack(recipeEntry.destination.route!!, inclusive = false)
                    } else {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    }
                },
                onNavigateToConfirm = {
                    navController.navigate(Routes.LOG_CONFIRM)
                },
                onIngredientAdded = onIngredientAdded,
            )
        }

        composable(Routes.LOG_MISSING_VALUES) { backStackEntry ->
            val logGraphEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.LOG_GRAPH)
            }
            val viewModel: AddEntryViewModel =
                if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()

            MissingValuesScreen(
                viewModel = viewModel,
                onClose = {
                    val recipeEntry = navController.recipeBackStackEntryOrNull()
                    if (recipeEntry != null) {
                        navController.popBackStack(recipeEntry.destination.route!!, inclusive = false)
                    } else {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    }
                },
                onNavigateToWeightEntry = {
                    navController.navigate(Routes.LOG_WEIGHT_ENTRY)
                },
            )
        }

        composable(Routes.LOG_CONFIRM) { backStackEntry ->
            val logGraphEntry = remember(backStackEntry) {
                navController.findBackStackEntry(Routes.LOG_GRAPH)
            }
            val viewModel: AddEntryViewModel =
                if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()

            EntryConfirmScreen(
                viewModel = viewModel,
                onClose = {
                    navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                },
                onGoBack = {
                    navController.popBackStack()
                },
                onEntrySaved = {
                    navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                },
            )
        }

        // Meal logging nested navigation graph — contains only the entry point screens.
        // LOG_WEIGHT_ENTRY, LOG_MISSING_VALUES, and LOG_CONFIRM are declared at the top level
        // above so that they can be reached from the ingredient sub-flow without an implicit
        // LOG_METHOD push.
        navigation(
            startDestination = Routes.LOG_METHOD,
            route = Routes.LOG_GRAPH,
        ) {
            composable(Routes.LOG_METHOD) { backStackEntry ->
                val logGraphEntry = remember(backStackEntry) {
                    navController.findBackStackEntry(Routes.LOG_GRAPH)
                }
                val viewModel: AddEntryViewModel =
                    if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()
                LaunchedEffect(Unit) { viewModel.refreshUsdaKeyStatus() }

                LogMethodScreen(
                    viewModel = viewModel,
                    onClose = {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    },
                    onNavigateToUsdaSearch = {
                        navController.navigate(Routes.logSearch("usda"))
                    },
                    onNavigateToOffSearch = {
                        navController.navigate(Routes.logSearch("off"))
                    },
                    onNavigateToBarcode = {
                        navController.navigate(Routes.LOG_BARCODE)
                    },
                    onNavigateToManual = {
                        navController.navigate(Routes.LOG_MANUAL)
                    },
                    onNavigateToRecipeSelect = {
                        navController.navigate(Routes.LOG_RECIPE_SELECT)
                    },
                )
            }

            composable(Routes.LOG_RECIPE_SELECT) { backStackEntry ->
                val logGraphEntry = remember(backStackEntry) {
                    navController.findBackStackEntry(Routes.LOG_GRAPH)
                }
                val viewModel: AddEntryViewModel =
                    if (logGraphEntry != null) hiltViewModel(logGraphEntry) else hiltViewModel()

                RecipeSelectScreen(
                    viewModel = viewModel,
                    onClose = {
                        navController.popBackStack(Routes.DAILY_PROGRESS, inclusive = false)
                    },
                    onNavigateToWeightEntry = {
                        navController.navigate(Routes.LOG_WEIGHT_ENTRY)
                    },
                )
            }
        }
    }
}
