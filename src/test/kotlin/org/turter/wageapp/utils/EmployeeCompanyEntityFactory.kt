package org.turter.wageapp.utils

import org.turter.wageapp.application.data.employee.entity.EmployeeCompanyDbEntity
import java.util.UUID

object EmployeeCompanyEntityFactory {

    fun create(employeeId: UUID, companyId: UUID): EmployeeCompanyDbEntity =
        EmployeeCompanyDbEntity().apply {
            this.employeeId = employeeId
            this.companyId = companyId
        }

}