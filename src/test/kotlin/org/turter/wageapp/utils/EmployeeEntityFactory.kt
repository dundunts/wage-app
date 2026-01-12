package org.turter.wageapp.utils

import org.turter.wageapp.application.data.employee.entity.EmployeeDbEntity
import java.util.UUID

object EmployeeEntityFactory {

    fun getEmployee(
        id: UUID? = null,
        userId: UUID? = null,
        firstName: String = "first_name",
        lastName: String = "last_name",
        patronymic: String = "first_name",
        simpleName: String = "first_name",
    ): EmployeeDbEntity =
        EmployeeDbEntity().apply {

        }

}