package org.turter.wageapp.domain.employee

import org.turter.wageapp.domain.employee.Employee.Position
import org.turter.wageapp.domain.shared.CompanyBindData
import java.util.*

// Data
data class Employee(
    val id: UUID,
    val companyIds: List<UUID>,
    val userId: String?,
    val firstName: String,
    val lastName: String,
    val patronymic: String,
    val simpleName: String?,
    val position: Position
) {
    enum class Position {
        MANAGER, WAITER_ACTIVE, WAITER_INACTIVE
    }
}

data class CompanyEmployeesResponse(
    override val companyId: UUID,
    override val data: List<CompanyEmployeeInfo>
) : CompanyBindData<List<CompanyEmployeeInfo>>

data class CompanyEmployeeInfo(
    val id: UUID,
    val userId: String?,
    val firstName: String,
    val lastName: String,
    val patronymic: String,
    val simpleName: String?,
    val position: Position
)

// Create
data class CreateEmployeePayload(
    val companyIds: List<UUID> = emptyList(),

    val firstName: String,

    val lastName: String,

    val patronymic: String,

    val simpleName: String?,

    val position: Position
)

// Update
data class UpdateEmployeePayload(
    val companyIds: List<UUID> = emptyList(),

    val userId: String?,

    val firstName: String,

    val lastName: String,

    val patronymic: String,

    val simpleName: String?,

    val position: Position
)
