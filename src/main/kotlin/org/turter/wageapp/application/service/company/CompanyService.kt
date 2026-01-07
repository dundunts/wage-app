package org.turter.wageapp.application.service.company

import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload
import java.util.*

interface CompanyService {

    suspend fun get(id: UUID): Company

    suspend fun create(payload: CompanyPayload): Company

    suspend fun update(id: UUID, payload: CompanyPayload): Company

    suspend fun delete(id: UUID)

    suspend fun getUserCompanies(userId: String): List<Company>

}
