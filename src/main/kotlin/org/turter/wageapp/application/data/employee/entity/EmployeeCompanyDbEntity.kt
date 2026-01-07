package org.turter.wageapp.application.data.employee.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("wage_app.employees_companies")
class EmployeeCompanyDbEntity {

    @Id
    var id: UUID? = null

    @Column("employee_id")
    var employeeId: UUID? = null

    @Column("company_id")
    var companyId: UUID? = null

    constructor(employeeId: UUID, companyId: UUID) {
        this.employeeId = employeeId
        this.companyId = companyId
    }

    constructor()
}