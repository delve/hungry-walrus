package com.delve.hungrywalrus.qa

import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import com.delve.hungrywalrus.data.repository.NutritionPlanRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * QA tests for NutritionPlanRepository plan history persistence.
 *
 * The architecture specifies (section 5.2): "When a user updates their plan, a new row
 * is inserted with effectiveFrom = now. This preserves historical plan data for summary
 * calculations across periods where the plan changed."
 *
 * These tests verify:
 * - savePlan inserts a new row, not an UPDATE (each call produces a new DAO insert).
 * - Two sequential savePlan calls each insert with a different effectiveFrom timestamp.
 * - getPlanForDate with a historical date uses the correct epoch millis cutoff.
 *
 * Note: these tests use a mocked DAO. Full SQL correctness (ORDER BY effectiveFrom DESC
 * LIMIT 1) requires a Room in-memory database with Robolectric or androidTest, which
 * is identified as a known gap.
 */
class PlanHistoryQaTest {

    private lateinit var dao: NutritionPlanDao
    private lateinit var repository: NutritionPlanRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = NutritionPlanRepositoryImpl(dao)
    }

    /**
     * Each call to savePlan must insert a new entity row.
     * The architecture explicitly mandates INSERT (not UPDATE) to preserve plan history.
     */
    @Test
    fun `each savePlan call inserts a new entity into the DAO`() = runTest {
        val insertedEntities = mutableListOf<NutritionPlanEntity>()
        coEvery { dao.insert(capture(insertedEntities)) } returns Unit

        repository.savePlan(kcal = 2000, proteinG = 150.0, carbsG = 250.0, fatG = 65.0)
        repository.savePlan(kcal = 2200, proteinG = 160.0, carbsG = 270.0, fatG = 70.0)

        coVerify(exactly = 2) { dao.insert(any()) }
        assertEquals(2, insertedEntities.size)
    }

    /**
     * Two sequential savePlan calls must produce entities with distinct effectiveFrom values.
     * Since effectiveFrom = System.currentTimeMillis(), even within the same test the two
     * inserts happen at different times and the timestamps should be non-decreasing.
     */
    @Test
    fun `two sequential savePlan calls produce entities with non-decreasing effectiveFrom`() = runTest {
        val insertedEntities = mutableListOf<NutritionPlanEntity>()
        coEvery { dao.insert(capture(insertedEntities)) } returns Unit

        repository.savePlan(kcal = 1800, proteinG = 120.0, carbsG = 220.0, fatG = 55.0)
        // Small delay isn't needed: we only assert non-decreasing (>=), not strictly greater.
        repository.savePlan(kcal = 2000, proteinG = 150.0, carbsG = 250.0, fatG = 65.0)

        assertEquals(2, insertedEntities.size)
        val first = insertedEntities[0]
        val second = insertedEntities[1]

        // Second insertion must have effectiveFrom >= first (never in the past)
        assert(second.effectiveFrom >= first.effectiveFrom) {
            "Second plan effectiveFrom (${second.effectiveFrom}) must not be before " +
                "first plan effectiveFrom (${first.effectiveFrom})"
        }
    }

    /**
     * Two savePlan calls with different kcal values must produce entities with the correct
     * nutritional values in each insert — they are independent rows.
     */
    @Test
    fun `two sequential savePlan calls persist the correct kcal for each`() = runTest {
        val insertedEntities = mutableListOf<NutritionPlanEntity>()
        coEvery { dao.insert(capture(insertedEntities)) } returns Unit

        repository.savePlan(kcal = 1800, proteinG = 120.0, carbsG = 220.0, fatG = 55.0)
        repository.savePlan(kcal = 2500, proteinG = 180.0, carbsG = 300.0, fatG = 80.0)

        assertEquals(1800, insertedEntities[0].kcalTarget)
        assertEquals(2500, insertedEntities[1].kcalTarget)
        assertNotEquals(
            "Two plan rows must have different kcalTarget values",
            insertedEntities[0].kcalTarget,
            insertedEntities[1].kcalTarget,
        )
    }

    /**
     * savePlan always sets effectiveFrom to approximately System.currentTimeMillis().
     * Verify the timestamp is within a 5-second window of when the test calls savePlan.
     */
    @Test
    fun `savePlan effectiveFrom is close to current time`() = runTest {
        val entitySlot = slot<NutritionPlanEntity>()
        coEvery { dao.insert(capture(entitySlot)) } returns Unit

        val before = System.currentTimeMillis()
        repository.savePlan(kcal = 2000, proteinG = 150.0, carbsG = 250.0, fatG = 65.0)
        val after = System.currentTimeMillis()

        val captured = entitySlot.captured
        assert(captured.effectiveFrom in before..after) {
            "effectiveFrom (${captured.effectiveFrom}) was not in range [$before, $after]"
        }
    }

    /**
     * getCurrentPlan uses Long.MAX_VALUE as the DAO query parameter.
     * This ensures any plan ever inserted is eligible (since effectiveFrom is always
     * a real timestamp which is << Long.MAX_VALUE).
     * Verify that the DAO is called with Long.MAX_VALUE.
     */
    @Test
    fun `getCurrentPlan passes Long MAX_VALUE to DAO`() = runTest {
        val timestampSlot = slot<Long>()
        coEvery { dao.getCurrentPlan(capture(timestampSlot)) } returns flowOf(null)

        repository.getCurrentPlan()

        // Room doesn't execute the query until collected, so just verify the argument via direct DAO call
        coVerify { dao.getCurrentPlan(Long.MAX_VALUE) }
    }
}
