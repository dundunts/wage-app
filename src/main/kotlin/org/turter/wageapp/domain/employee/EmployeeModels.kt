package org.turter.wageapp.domain.employee

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
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

    @field:NotBlank(message = "First name is required")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    val lastName: String,

    @field:NotBlank(message = "Patronymic is required")
    val patronymic: String,

    val simpleName: String?,

    @field:NotNull(message = "Position is required")
    val position: Position
)

// Update
data class UpdateEmployeePayload(
    val companyIds: List<UUID> = emptyList(),

    val userId: String?,

    @field:NotBlank(message = "First name is required")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    val lastName: String,

    @field:NotBlank(message = "Patronymic is required")
    val patronymic: String,

    val simpleName: String?,

    @field:NotNull(message = "Position is required")
    val position: Position
)

