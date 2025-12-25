package org.turter.wageapp.data.employee

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.turter.wageapp.data.employee.entity.EmployeeDbEntity
import org.turter.wageapp.data.employee.entity.EmployeeWithCompanyRow
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

interface EmployeeRepository : R2dbcRepository<EmployeeDbEntity, UUID>, EmployeeCustomRepository {
    fun existsByUserId(userId: String): Mono<Boolean>

    fun findByUserId(userId: String): Mono<EmployeeDbEntity>
}

interface EmployeeCustomRepository {
    fun findEmployeesByCompanyIds(companyIds: List<UUID>): Flux<EmployeeWithCompanyRow>
    fun findCoworkersByUserId(userId: String): Flux<EmployeeWithCompanyRow>
}
