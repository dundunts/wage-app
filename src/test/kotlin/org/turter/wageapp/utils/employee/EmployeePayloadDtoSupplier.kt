package org.turter.wageapp.utils.employee

import org.turter.wageapp.transport.model.CreateEmployeeRequest
import org.turter.wageapp.transport.model.Position
import org.turter.wageapp.transport.model.UpdateEmployeeRequest
import java.util.UUID

object EmployeePayloadDtoSupplier {

    fun validForCreate(
        companyIds: List<UUID> = emptyList(),
        firstName: String = "Ivan",
        lastName: String = "Ivanov",
        patronymic: String = "Ivanovich",
        simpleName: String? = null,
        position: Position = Position.WAITER_ACTIVE
    ): CreateEmployeeRequest =
        CreateEmployeeRequest(
            firstName = firstName,
            lastName = lastName,
            patronymic = patronymic,
            simpleName = simpleName,
            position = position,
            companyIds = companyIds,
        )

    fun withEmptyFirstName(): CreateEmployeeRequest =
        validForCreate(firstName = "")

    fun validForUpdate(
        companyIds: List<UUID> = emptyList(),
        userId: String? = null,
        firstName: String = "Ivan",
        lastName: String = "Ivanov",
        patronymic: String = "Ivanovich",
        simpleName: String? = null,
        position: Position = Position.WAITER_ACTIVE
    ): UpdateEmployeeRequest =
        UpdateEmployeeRequest(
            firstName = firstName,
            lastName = lastName,
            patronymic = patronymic,
            simpleName = simpleName,
            position = position,
            companyIds = companyIds,
            userId = userId,
        )

}
