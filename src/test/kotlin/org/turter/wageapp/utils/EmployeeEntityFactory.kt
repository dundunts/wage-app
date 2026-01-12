package org.turter.wageapp.utils

import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.domain.employee.Employee
import java.util.UUID

object EmployeeEntityFactory {

    fun create(
        id: UUID? = null,
        userId: String? = null,
        firstName: String = "first_name",
        lastName: String = "last_name",
        patronymic: String = "first_name",
        simpleName: String = "first_name",
        position: Employee.Position = Employee.Position.WAITER_ACTIVE
    ): EmployeeDbEntity =
        EmployeeDbEntity().apply {
            this.id = id
            this.userId = userId
            this.firstName = firstName
            this.lastName = lastName
            this.patronymic = patronymic
            this.simpleName = simpleName
            this.position = position
        }

}