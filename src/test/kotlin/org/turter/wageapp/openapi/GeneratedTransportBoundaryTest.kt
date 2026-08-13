package org.turter.wageapp.openapi

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.turter.wageapp.transport.api.CheckpointApi
import org.turter.wageapp.transport.api.CompanyApi
import org.turter.wageapp.transport.api.EmployeeApi
import org.turter.wageapp.transport.api.PayrollApi
import org.turter.wageapp.transport.api.ShiftResultApi
import org.turter.wageapp.transport.api.ShiftResultDraftApi
import org.turter.wageapp.transport.api.ShiftSessionApi
import java.lang.reflect.Modifier

class GeneratedTransportBoundaryTest {

    private val apiGroups = setOf(
        CheckpointApi::class.java,
        CompanyApi::class.java,
        EmployeeApi::class.java,
        PayrollApi::class.java,
        ShiftResultApi::class.java,
        ShiftResultDraftApi::class.java,
        ShiftSessionApi::class.java,
    )

    @Test
    fun `generated transport boundary exposes every canonical API group`() {
        assertEquals(
            setOf(
                "CheckpointApi",
                "CompanyApi",
                "EmployeeApi",
                "PayrollApi",
                "ShiftResultApi",
                "ShiftResultDraftApi",
                "ShiftSessionApi",
            ),
            apiGroups.mapTo(mutableSetOf()) { it.simpleName },
        )
        assertEquals(34, apiGroups.sumOf { it.declaredMethods.size })
    }

    @Test
    fun `generated operations require explicit implementations`() {
        assertTrue(apiGroups.all { it.isInterface })
        assertTrue(apiGroups.flatMap { it.declaredMethods.asList() }.all { Modifier.isAbstract(it.modifiers) })
    }
}
