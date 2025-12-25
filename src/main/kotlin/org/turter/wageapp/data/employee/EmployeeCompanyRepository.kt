package org.turter.wageapp.data.employee

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.turter.wageapp.data.employee.entity.EmployeeCompanyDbEntity
import reactor.core.publisher.Flux
import java.util.*

interface EmployeeCompanyRepository : R2dbcRepository<EmployeeCompanyDbEntity, UUID> {

    fun findAllByEmployeeId(employeeId: UUID): Flux<EmployeeCompanyDbEntity>

}