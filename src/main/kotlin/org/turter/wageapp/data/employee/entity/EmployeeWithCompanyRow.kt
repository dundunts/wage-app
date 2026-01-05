package org.turter.wageapp.data.employee.entity

import org.turter.wageapp.domain.employee.Employee
import java.util.*

data class EmployeeWithCompanyRow(
    val employeeId: UUID,
    val companyId: UUID,
    val userId: String?,
    val firstName: String,
    val lastName: String,
    val patronymic: String,
    val simpleName: String?,
    val position: Employee.Position
)