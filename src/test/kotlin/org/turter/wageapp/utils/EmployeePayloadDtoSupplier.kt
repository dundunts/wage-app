package org.turter.wageapp.utils

import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.Employee
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import java.util.UUID

object EmployeePayloadDtoSupplier {

    fun validForCreate(
        companyIds: List<UUID> = emptyList(),
        firstName: String = "Ivan",
        lastName: String = "Ivanov",
        patronymic: String = "Ivanovich",
        simpleName: String? = null,
        position: Employee.Position = Employee.Position.WAITER_ACTIVE
    ): CreateEmployeePayload =
        CreateEmployeePayload(
            companyIds = companyIds,
            firstName = firstName,
            lastName = lastName,
            patronymic = patronymic,
            simpleName = simpleName,
            position = position
        )

    fun withEmptyFirstName(): CreateEmployeePayload =
        validForCreate(firstName = "")

    fun validForUpdate(
        companyIds: List<UUID> = emptyList(),
        userId: String? = null,
        firstName: String = "Ivan",
        lastName: String = "Ivanov",
        patronymic: String = "Ivanovich",
        simpleName: String? = null,
        position: Employee.Position = Employee.Position.WAITER_ACTIVE
    ): UpdateEmployeePayload =
        UpdateEmployeePayload(
            companyIds = companyIds,
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            patronymic = patronymic,
            simpleName = simpleName,
            position = position
        )

}
