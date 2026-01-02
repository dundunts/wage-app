package org.turter.wageapp.service.shift

import kotlinx.coroutines.reactor.awaitSingle
import org.turter.wageapp.data.company.CompanyRepository
import org.turter.wageapp.domain.shift.WrongCompanyIdException
import java.util.UUID

suspend fun validateUserCompanyBind(userId: String, companyId: UUID, repo: CompanyRepository) {
    val companies = repo.findAllForUserId(userId).collectList().awaitSingle()

    if (companies.none { c -> c.id == companyId })
        throw WrongCompanyIdException("Company ID {${companyId}} not included in user`s companies {$companies}.")
}