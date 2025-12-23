package org.turter.wageapp.data.company

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.turter.wageapp.domain.company.Company
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

interface CompanyRepository : R2dbcRepository<CompanyDbEntity, UUID>, CustomCompanyRepository {

    fun existsByTitle(title: String): Mono<Boolean>

}

interface CustomCompanyRepository {

    fun findAllForUserId(userId: UUID): Flux<Company>

}