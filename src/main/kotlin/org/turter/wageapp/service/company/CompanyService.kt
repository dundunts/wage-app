package org.turter.wageapp.service.company

import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload
import java.util.UUID

interface CompanyService {

    suspend fun get(id: UUID): Company

    suspend fun create(payload: CompanyPayload): Company

    suspend fun update(id: UUID, payload: CompanyPayload): Company

    suspend fun delete(id: UUID)

    suspend fun getUserCompanies(userId: UUID): List<Company>

    suspend fun bindUserToCompany(companyId: UUID, userId: UUID)
}
