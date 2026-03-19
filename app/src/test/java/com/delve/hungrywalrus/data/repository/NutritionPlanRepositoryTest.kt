package com.delve.hungrywalrus.data.repository

import app.cash.turbine.test
import com.delve.hungrywalrus.data.local.dao.NutritionPlanDao
import com.delve.hungrywalrus.data.local.entity.NutritionPlanEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NutritionPlanRepositoryTest {

    private lateinit var dao: NutritionPlanDao
    private lateinit var repository: NutritionPlanRepositoryImpl

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = NutritionPlanRepositoryImpl(dao)
    }

    @Test
    fun `getCurrentPlan emits mapped domain model when entity exists`() = runTest {
        val entity = NutritionPlanEntity(
            id = 1,
            kcalTarget = 2000,
            proteinTargetG = 150.0,
            carbsTargetG = 250.0,
            fatTargetG = 70.0,
            effectiveFrom = 1000L,
        )
        every { dao.getCurrentPlan(any()) } returns flowOf(entity)

        repository.getCurrentPlan().test {
            val plan = awaitItem()!!
            assertEquals(1L, plan.id)
            assertEquals(2000, plan.kcalTarget)
            assertEquals(150.0, plan.proteinTargetG, 0.001)
            assertEquals(250.0, plan.carbsTargetG, 0.001)
            assertEquals(70.0, plan.fatTargetG, 0.001)
            assertEquals(1000L, plan.effectiveFrom)
            awaitComplete()
        }
    }

    @Test
    fun `getCurrentPlan emits null when no entity exists`() = runTest {
        every { dao.getCurrentPlan(any()) } returns flowOf(null)

        repository.getCurrentPlan().test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getPlanForDate converts date to epoch millis and returns mapped plan`() = runTest {
        val date = LocalDate.of(2026, 3, 15)
        val expectedMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val entity = NutritionPlanEntity(
            id = 2,
            kcalTarget = 1800,
            proteinTargetG = 130.0,
            carbsTargetG = 200.0,
            fatTargetG = 60.0,
            effectiveFrom = expectedMillis - 86400000L,
        )
        coEvery { dao.getPlanForDate(expectedMillis) } returns entity

        val plan = repository.getPlanForDate(date)

        assertEquals(2L, plan!!.id)
        assertEquals(1800, plan!!.kcalTarget)
        assertEquals(130.0, plan!!.proteinTargetG, 0.001)
        assertEquals(200.0, plan!!.carbsTargetG, 0.001)
        assertEquals(60.0, plan!!.fatTargetG, 0.001)
        assertEquals(expectedMillis - 86400000L, plan!!.effectiveFrom)
        coVerify { dao.getPlanForDate(expectedMillis) }
    }

    @Test
    fun `getPlanForDate returns null when no plan exists for date`() = runTest {
        val date = LocalDate.of(2026, 1, 1)
        val expectedMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        coEvery { dao.getPlanForDate(expectedMillis) } returns null

        val plan = repository.getPlanForDate(date)

        assertNull(plan)
    }

    @Test
    fun `savePlan inserts entity with correct values`() = runTest {
        val entitySlot = slot<NutritionPlanEntity>()
        coEvery { dao.insert(capture(entitySlot)) } returns Unit

        val beforeCall = System.currentTimeMillis()
        repository.savePlan(kcal = 2200, proteinG = 160.0, carbsG = 280.0, fatG = 75.0)
        val afterCall = System.currentTimeMillis()

        val captured = entitySlot.captured
        assertEquals(2200, captured.kcalTarget)
        assertEquals(160.0, captured.proteinTargetG, 0.001)
        assertEquals(280.0, captured.carbsTargetG, 0.001)
        assertEquals(75.0, captured.fatTargetG, 0.001)
        assert(captured.effectiveFrom in beforeCall..afterCall) {
            "effectiveFrom ${captured.effectiveFrom} was not in range [$beforeCall, $afterCall]"
        }
    }
}
