package com.delve.hungrywalrus.ui.screen.createrecipe

import androidx.lifecycle.SavedStateHandle
import com.delve.hungrywalrus.data.repository.RecipeRepository
import com.delve.hungrywalrus.domain.model.Recipe
import com.delve.hungrywalrus.domain.model.RecipeIngredient
import com.delve.hungrywalrus.domain.model.RecipeWithIngredients
import com.delve.hungrywalrus.domain.usecase.ScaleNutritionUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [CreateRecipeViewModel.editIngredient] (architecture §7.7).
 *
 * Verifies that the in-place ingredient editing flow:
 *  - Locates the draft by [id] and replaces its values atomically.
 *  - Recomputes the running totals from the updated ingredient list.
 *  - Preserves the order of ingredients across edits.
 *  - Marks the recipe as dirty so the discard confirmation appears on close.
 *  - Does not trigger any database I/O — persistence happens only on Save Recipe.
 *  - Is a no-op when the supplied id matches no ingredient.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRecipeViewModelEditIngredientTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var recipeRepo: RecipeRepository
    private val scaleUseCase = ScaleNutritionUseCase()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        recipeRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CreateRecipeViewModel(
        recipeRepo, scaleUseCase, SavedStateHandle(),
    )

    /**
     * `addIngredient` assigns a stable negative id when the draft is constructed
     * without an id, so subsequent `editIngredient` calls can locate the draft.
     */
    @Test
    fun `addIngredient assigns a unique id to drafts constructed without one`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("A", 100.0, 100.0, 10.0, 20.0, 5.0))
        viewModel.addIngredient(IngredientDraft("B", 200.0, 150.0, 12.0, 25.0, 7.0))

        val ids = viewModel.uiState.value.ingredients.map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
        // First id was set to nextDraftId = -1, then nextDraftId-- = -2 for the second.
        assertTrue(ids.all { it != 0L })
    }

    /**
     * `editIngredient(id, newValues)` replaces the targeted draft's name, weight,
     * and per-100g values with the supplied [IngredientEditValues], preserving
     * the draft's id and source.
     */
    @Test
    fun `editIngredient replaces the targeted ingredient by id`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("Chicken", 200.0, 165.0, 31.0, 0.0, 3.6))
        viewModel.addIngredient(IngredientDraft("Rice", 150.0, 130.0, 2.7, 28.0, 0.3))

        val ingredients = viewModel.uiState.value.ingredients
        val chickenId = ingredients[0].id

        viewModel.editIngredient(
            id = chickenId,
            newValues = IngredientEditValues(
                foodName = "Chicken thigh",
                weightG = 250.0,
                kcalPer100g = 209.0,
                proteinPer100g = 26.0,
                carbsPer100g = 0.0,
                fatPer100g = 11.0,
            ),
        )

        val updated = viewModel.uiState.value.ingredients
        // Order preserved.
        assertEquals(2, updated.size)
        assertEquals(chickenId, updated[0].id)
        assertEquals("Chicken thigh", updated[0].name)
        assertEquals(250.0, updated[0].weightG, 0.001)
        assertEquals(209.0, updated[0].kcalPer100g, 0.001)
        // Second ingredient unchanged.
        assertEquals("Rice", updated[1].name)
        assertEquals(150.0, updated[1].weightG, 0.001)
    }

    /**
     * Editing an ingredient recomputes the live totals immediately. Architecture §7.7.
     */
    @Test
    fun `editIngredient recomputes live totals immediately`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("Chicken", 100.0, 165.0, 31.0, 0.0, 3.6))
        // 100g * 165/100 = 165 kcal
        assertEquals(165.0, viewModel.uiState.value.liveTotals.kcal, 0.001)

        val id = viewModel.uiState.value.ingredients[0].id
        viewModel.editIngredient(
            id = id,
            newValues = IngredientEditValues(
                foodName = "Chicken",
                weightG = 200.0,
                kcalPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6,
            ),
        )

        // After edit: 200g * 165/100 = 330 kcal
        assertEquals(330.0, viewModel.uiState.value.liveTotals.kcal, 0.001)
        assertEquals(200.0, viewModel.uiState.value.totalWeightG, 0.001)
    }

    /**
     * Editing an ingredient marks the recipe as dirty so that closing the screen
     * triggers the discard confirmation. Architecture §7.7, design §3.13.
     */
    @Test
    fun `editIngredient sets isDirty to true`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Bowl", totalWeightG = 100.0,
            totalKcal = 165.0, totalProteinG = 31.0, totalCarbsG = 0.0, totalFatG = 3.6,
            createdAt = 0L, updatedAt = 0L,
        )
        val ingredient = RecipeIngredient(
            recipeId = 1, foodName = "Chicken", weightG = 100.0,
            kcalPer100g = 165.0, proteinPer100g = 31.0, carbsPer100g = 0.0, fatPer100g = 3.6,
        )
        every { recipeRepo.getRecipeWithIngredients(1) } returns flowOf(
            RecipeWithIngredients(recipe, listOf(ingredient)),
        )

        val viewModel = CreateRecipeViewModel(
            recipeRepo, scaleUseCase, SavedStateHandle(mapOf("id" to 1L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Loaded but no user mutation yet — isDirty must be false.
        assertFalse(viewModel.uiState.value.isDirty)

        val id = viewModel.uiState.value.ingredients[0].id
        viewModel.editIngredient(
            id = id,
            newValues = IngredientEditValues(
                foodName = "Chicken (corrected)",
                weightG = 100.0,
                kcalPer100g = 165.0,
                proteinPer100g = 31.0,
                carbsPer100g = 0.0,
                fatPer100g = 3.6,
            ),
        )

        assertTrue(viewModel.uiState.value.isDirty)
    }

    /**
     * If `editIngredient` is called with an id that matches no draft, the
     * ingredient list and dirty flag are unchanged.
     */
    @Test
    fun `editIngredient with unknown id is a no-op`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("Chicken", 100.0, 165.0, 31.0, 0.0, 3.6))

        // Snapshot state before the no-op.
        val beforeIngredients = viewModel.uiState.value.ingredients
        val beforeTotals = viewModel.uiState.value.liveTotals

        viewModel.editIngredient(
            id = 999_999L,
            newValues = IngredientEditValues(
                foodName = "Bogus",
                weightG = 9999.0,
                kcalPer100g = 9999.0,
                proteinPer100g = 9999.0,
                carbsPer100g = 9999.0,
                fatPer100g = 9999.0,
            ),
        )

        // State unchanged.
        assertEquals(beforeIngredients, viewModel.uiState.value.ingredients)
        assertEquals(beforeTotals.kcal, viewModel.uiState.value.liveTotals.kcal, 0.001)
    }

    /**
     * Loaded ingredients (from an existing recipe via `loadExistingRecipe`) receive
     * positive ids so they can be edited by id without colliding with the negative
     * sentinels assigned to drafts added during the session.
     */
    @Test
    fun `loaded ingredients have positive ids that can be edited`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Bowl", totalWeightG = 150.0,
            totalKcal = 200.0, totalProteinG = 20.0, totalCarbsG = 0.0, totalFatG = 5.0,
            createdAt = 0L, updatedAt = 0L,
        )
        val ingredients = listOf(
            RecipeIngredient(
                recipeId = 1, foodName = "Chicken", weightG = 100.0,
                kcalPer100g = 100.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 5.0,
            ),
            RecipeIngredient(
                recipeId = 1, foodName = "Broccoli", weightG = 50.0,
                kcalPer100g = 34.0, proteinPer100g = 2.8, carbsPer100g = 6.6, fatPer100g = 0.4,
            ),
        )
        every { recipeRepo.getRecipeWithIngredients(1) } returns flowOf(
            RecipeWithIngredients(recipe, ingredients),
        )

        val viewModel = CreateRecipeViewModel(
            recipeRepo, scaleUseCase, SavedStateHandle(mapOf("id" to 1L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val loaded = viewModel.uiState.value.ingredients
        assertEquals(2, loaded.size)
        // Both ids are positive and distinct.
        assertTrue(loaded.all { it.id > 0 })
        assertNotEquals(loaded[0].id, loaded[1].id)

        // Editing the first loaded ingredient works.
        viewModel.editIngredient(
            id = loaded[0].id,
            newValues = IngredientEditValues(
                foodName = "Chicken (corrected)",
                weightG = 200.0,
                kcalPer100g = 100.0,
                proteinPer100g = 20.0,
                carbsPer100g = 0.0,
                fatPer100g = 5.0,
            ),
        )
        val edited = viewModel.uiState.value.ingredients[0]
        assertEquals("Chicken (corrected)", edited.name)
        assertEquals(200.0, edited.weightG, 0.001)
        // id is preserved across the edit.
        assertEquals(loaded[0].id, edited.id)
    }

    /**
     * After adding a new ingredient to an edit-mode recipe and then editing the
     * loaded one, both ids are addressable and distinct. This regression test
     * guards against the negative-sentinel allocator colliding with loaded ids.
     */
    @Test
    fun `negative-sentinel ids do not collide with loaded ingredient ids`() = runTest {
        val recipe = Recipe(
            id = 1, name = "Bowl", totalWeightG = 100.0,
            totalKcal = 200.0, totalProteinG = 20.0, totalCarbsG = 0.0, totalFatG = 5.0,
            createdAt = 0L, updatedAt = 0L,
        )
        val ingredient = RecipeIngredient(
            recipeId = 1, foodName = "Chicken", weightG = 100.0,
            kcalPer100g = 200.0, proteinPer100g = 20.0, carbsPer100g = 0.0, fatPer100g = 5.0,
        )
        every { recipeRepo.getRecipeWithIngredients(1) } returns flowOf(
            RecipeWithIngredients(recipe, listOf(ingredient)),
        )

        val viewModel = CreateRecipeViewModel(
            recipeRepo, scaleUseCase, SavedStateHandle(mapOf("id" to 1L)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addIngredient(IngredientDraft("Rice", 100.0, 130.0, 2.7, 28.0, 0.3))

        val ids = viewModel.uiState.value.ingredients.map { it.id }
        assertEquals(2, ids.size)
        assertNotEquals(ids[0], ids[1])
        // Loaded id is positive, newly-added id is negative.
        assertTrue("loaded ingredient id should be positive", ids[0] > 0)
        assertTrue("newly added ingredient id should be negative", ids[1] < 0)
    }

    /**
     * Edits made via `editIngredient` are flushed to the database only on Save Recipe.
     * This guards the contract that confirming the edit dialog does not persist anything
     * to Room — persistence happens via the existing delete-and-reinsert strategy on
     * `Save Recipe` (architecture §7.7, §7.8).
     */
    @Test
    fun `editIngredient does not invoke repository`() = runTest {
        val viewModel = createViewModel()
        viewModel.addIngredient(IngredientDraft("A", 100.0, 100.0, 10.0, 20.0, 5.0))
        val id = viewModel.uiState.value.ingredients[0].id

        viewModel.editIngredient(
            id = id,
            newValues = IngredientEditValues(
                foodName = "A'",
                weightG = 150.0,
                kcalPer100g = 100.0,
                proteinPer100g = 10.0,
                carbsPer100g = 20.0,
                fatPer100g = 5.0,
            ),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        io.mockk.coVerify(exactly = 0) { recipeRepo.saveRecipe(any(), any()) }
        io.mockk.coVerify(exactly = 0) { recipeRepo.updateRecipe(any(), any()) }
        // Sanity: the in-memory edit took effect.
        assertNotNull(viewModel.uiState.value.ingredients[0])
        assertEquals("A'", viewModel.uiState.value.ingredients[0].name)
    }
}
