package com.delve.hungrywalrus.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.delve.hungrywalrus.data.local.HungryWalrusDatabase
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * In-memory Room database tests for [NutritionPlanDao].
 *
 * The architecture (Section 19, Revision 1 item 13) explicitly recommends these tests
 * for verifying the plan-for-date ordering query. The contract is that
 * [NutritionPlanDao.getPlanForDate] and [NutritionPlanDao.getCurrentPlan] return the
 * plan with the *latest* `effectiveFrom` that is `<=` the queried timestamp, so when
 * the user has changed their plan, historical summaries use the plan that was active
 * on the relevant day rather than the most-recent plan.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NutritionPlanDaoTest {

    private lateinit var database: HungryWalrusDatabase
    private lateinit var dao: NutritionPlanDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HungryWalrusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.nutritionPlanDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun plan(kcal: Int, effectiveFrom: Long) = NutritionPlanEntity(
        kcalTarget = kcal,
        proteinTargetG = 100.0,
        carbsTargetG = 200.0,
        fatTargetG = 50.0,
        effectiveFrom = effectiveFrom,
    )

    @Test
    fun `getCurrentPlan returns null when no plan exists`() = runTest {
        dao.getCurrentPlan(Long.MAX_VALUE).test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCurrentPlan returns the plan with latest effectiveFrom not in the future`() = runTest {
        dao.insert(plan(kcal = 1500, effectiveFrom = 1000L))
        dao.insert(plan(kcal = 2000, effectiveFrom = 2000L))
        dao.insert(plan(kcal = 2200, effectiveFrom = 3000L))

        dao.getCurrentPlan(2500L).test {
            val current = awaitItem()
            assertEquals(2000, current?.kcalTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCurrentPlan ignores plans whose effectiveFrom is in the future`() = runTest {
        dao.insert(plan(kcal = 1800, effectiveFrom = 1000L))
        dao.insert(plan(kcal = 9999, effectiveFrom = 5000L)) // "future" relative to now=2000

        dao.getCurrentPlan(2000L).test {
            val current = awaitItem()
            assertEquals(1800, current?.kcalTarget)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getPlanForDate returns null when date is before any plan`() = runTest {
        dao.insert(plan(kcal = 2000, effectiveFrom = 1000L))

        val result = dao.getPlanForDate(500L)
        assertNull(result)
    }

    @Test
    fun `getPlanForDate returns the plan active on that date when multiple plans exist`() = runTest {
        // Plan A: 1500 kcal, active from t=1000
        // Plan B: 2000 kcal, active from t=2000
        // Plan C: 2200 kcal, active from t=3000
        dao.insert(plan(kcal = 1500, effectiveFrom = 1000L))
        dao.insert(plan(kcal = 2000, effectiveFrom = 2000L))
        dao.insert(plan(kcal = 2200, effectiveFrom = 3000L))

        // Querying t=1500 -> Plan A active
        assertEquals(1500, dao.getPlanForDate(1500L)?.kcalTarget)
        // Querying t=2000 -> Plan B active (exactly at boundary)
        assertEquals(2000, dao.getPlanForDate(2000L)?.kcalTarget)
        // Querying t=2500 -> Plan B still active
        assertEquals(2000, dao.getPlanForDate(2500L)?.kcalTarget)
        // Querying t=3000 -> Plan C active
        assertEquals(2200, dao.getPlanForDate(3000L)?.kcalTarget)
        // Querying t=10000 -> Plan C still active
        assertEquals(2200, dao.getPlanForDate(10000L)?.kcalTarget)
    }

    @Test
    fun `insert adds a new row rather than updating in place`() = runTest {
        dao.insert(plan(kcal = 1500, effectiveFrom = 1000L))
        dao.insert(plan(kcal = 2000, effectiveFrom = 2000L))

        // Each insert produces a new row -- this is the architecture-mandated behaviour
        // (Section 5.2): plan history is preserved by inserting new rows, never by
        // updating an existing row. The most-recent plan is then surfaced by querying
        // ORDER BY effectiveFrom DESC LIMIT 1.
        dao.getCurrentPlan(Long.MAX_VALUE).test {
            val current = awaitItem()!!
            assertEquals(2000, current.kcalTarget)
            cancelAndIgnoreRemainingEvents()
        }

        // The older plan is still queryable via getPlanForDate
        assertEquals(1500, dao.getPlanForDate(1500L)?.kcalTarget)
    }
}
