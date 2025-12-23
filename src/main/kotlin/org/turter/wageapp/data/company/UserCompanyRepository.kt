package org.turter.wageapp.data.company

import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Flux
import java.util.*

interface UserCompanyRepository : R2dbcRepository<UserCompanyDbEntity, UUID> {
}
