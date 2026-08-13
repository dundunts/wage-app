package org.turter.wageapp.openapi

import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.controller.CalculationController
import org.turter.wageapp.application.controller.CheckpointController
import org.turter.wageapp.application.controller.CompanyController
import org.turter.wageapp.application.controller.EmployeeController
import org.turter.wageapp.application.controller.SalaryController
import org.turter.wageapp.application.controller.SessionController
import org.turter.wageapp.application.controller.ShiftResultController
import org.turter.wageapp.domain.company.CompanyPayload
import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import org.turter.wageapp.transport.api.CheckpointApi
import org.turter.wageapp.transport.api.CompanyApi
import org.turter.wageapp.transport.api.EmployeeApi
import org.turter.wageapp.transport.api.PayrollApi
import org.turter.wageapp.transport.api.ShiftResultApi
import org.turter.wageapp.transport.api.ShiftResultDraftApi
import org.turter.wageapp.transport.api.ShiftSessionApi
import org.turter.wageapp.transport.model.CreateEmployeeRequest
import org.turter.wageapp.transport.model.Position
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

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

    private val controllerByApi = mapOf(
        CheckpointApi::class.java to CheckpointController::class.java,
        CompanyApi::class.java to CompanyController::class.java,
        EmployeeApi::class.java to EmployeeController::class.java,
        PayrollApi::class.java to SalaryController::class.java,
        ShiftResultApi::class.java to ShiftResultController::class.java,
        ShiftResultDraftApi::class.java to CalculationController::class.java,
        ShiftSessionApi::class.java to SessionController::class.java,
    )

    @Test
    fun `generated transport boundary exposes every canonical API group`() {
        val operations = apiGroups.flatMap { it.declaredMethods.asList() }

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
        assertEquals(
            setOf(
                "bindEmployeeUser",
                "closeShiftSession",
                "confirmShiftResultDraft",
                "createCheckpoint",
                "createCompany",
                "createEmployee",
                "deleteCheckpoint",
                "deleteCompany",
                "deleteEmployee",
                "deleteShiftResult",
                "deleteShiftResultDraft",
                "exportStaffPayroll",
                "getAllEmployees",
                "getAvailableShiftSession",
                "getAvailableShiftSessions",
                "getCompaniesForUser",
                "getCompaniesPage",
                "getCompany",
                "getCoworkers",
                "getEmployee",
                "getEmployeesByCompanies",
                "getOpenedShiftSession",
                "getOrCalculateShiftResultDraft",
                "getOwnPayroll",
                "getShiftResult",
                "getShiftResultsPage",
                "getStaffPayroll",
                "openShiftSession",
                "openShiftSessionRecalculation",
                "saveManualOverrideShiftResult",
                "updateCheckpoint",
                "updateCompany",
                "updateEmployee",
                "updateShiftSessionStart",
            ),
            operations.mapTo(mutableSetOf()) { it.name },
        )
        assertEquals(34, operations.size)
    }

    @Test
    fun `generated operations require explicit implementations`() {
        assertTrue(apiGroups.all { it.isInterface })
        assertTrue(apiGroups.flatMap { it.declaredMethods.asList() }.all { Modifier.isAbstract(it.modifiers) })
    }

    @Test
    fun `every generated operation is implemented by its handwritten controller`() {
        assertEquals(apiGroups, controllerByApi.keys)

        controllerByApi.forEach { (api, controller) ->
            assertTrue(controller.isAnnotationPresent(RestController::class.java))
            assertTrue(api.isAssignableFrom(controller))
            assertEquals(
                api.declaredMethods.mapTo(mutableSetOf()) { it.name },
                controller.declaredMethods
                    .filterNot { it.isSynthetic }
                    .mapTo(mutableSetOf()) { it.name }
                    .intersect(api.declaredMethods.mapTo(mutableSetOf()) { it.name }),
            )
        }
    }

    @Test
    fun `HTTP operation signatures do not expose domain types`() {
        val exposedDomainTypes = controllerByApi.flatMap { (api, controller) ->
            val operationNames = api.declaredMethods.mapTo(mutableSetOf()) { it.name }
            (api.declaredMethods.asList() + controller.declaredMethods.filter { it.name in operationNames })
                .flatMap { method ->
                    (method.genericParameterTypes.asList() + method.genericReturnType)
                        .flatMap { it.referencedClasses() }
                }
                .filter { type -> type.packageName.startsWith("org.turter.wageapp.domain") }
        }

        assertTrue(exposedDomainTypes.isEmpty(), "Domain types exposed over HTTP: $exposedDomainTypes")
    }

    @Test
    fun `generated Employee request validation preserves non-blank name behavior`() {
        val validator = Validation.buildDefaultValidatorFactory().validator
        val request = CreateEmployeeRequest(
            firstName = "Ivan",
            lastName = "Ivanov",
            patronymic = "Ivanovich",
            position = Position.WAITER_ACTIVE,
        )

        assertTrue(validator.validate(request).isEmpty())
        assertEquals(
            setOf("firstName"),
            validator.validate(request.copy(firstName = "   ")).mapTo(mutableSetOf()) { it.propertyPath.toString() },
        )
    }

    @Test
    fun `domain command models do not retain HTTP validation annotations`() {
        val domainCommandTypes = setOf(
            CompanyPayload::class.java,
            CreateEmployeePayload::class.java,
            UpdateEmployeePayload::class.java,
        )

        domainCommandTypes.forEach { type ->
            assertFalse(
                type.declaredFields.flatMap { it.annotations.asList() }
                    .any { it.annotationClass.java.packageName.startsWith("jakarta.validation") },
                "$type still carries HTTP validation annotations",
            )
        }
    }

    private fun Type.referencedClasses(): List<Class<*>> = when (this) {
        is Class<*> -> listOf(this)
        is ParameterizedType -> actualTypeArguments.flatMap { it.referencedClasses() } + rawType.referencedClasses()
        else -> emptyList()
    }
}
