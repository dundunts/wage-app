package org.turter.wageapp.service.company

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.data.company.CompanyRepository
import org.turter.wageapp.data.company.UserCompanyDbEntity
import org.turter.wageapp.data.company.UserCompanyRepository
import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shared.NotUniqueValue
import org.turter.wageapp.mapper.CompanyMapper
import java.util.UUID

@Service
class CompanyServiceImpl(
    private val companyRepository: CompanyRepository,
    private val userCompanyRepository: UserCompanyRepository,
    private val mapper: CompanyMapper
) : CompanyService {

    override suspend fun get(id: UUID) =
        companyRepository.findById(id).awaitSingleOrNull()
            ?.let(mapper::toCompanyDto)
            ?: throw EntityNotFoundException("Company $id not found")

    @Transactional
    override suspend fun create(payload: CompanyPayload): Company {
        if (companyRepository.existsByTitle(payload.title).awaitSingle()) {
            throw NotUniqueValue("Company with title '${payload.title}' already exists")
        }

        val saved = companyRepository.save(
            mapper.toNewCompanyDbEntity(payload)
        ).awaitSingle()

        return mapper.toCompanyDto(saved)
    }

    @Transactional
    override suspend fun update(id: UUID, payload: CompanyPayload): Company {
        val saved = companyRepository.findById(id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Company $id not found")

        val updated = companyRepository.save(
            mapper.mergeToCompanyDbEntity(payload, saved)
        ).awaitSingle()

        return mapper.toCompanyDto(updated)
    }

    override suspend fun delete(id: UUID) {
        companyRepository.deleteById(id).awaitSingleOrNull()
    }

    override suspend fun getUserCompanies(userId: UUID): List<Company> =
        companyRepository.findAllForUserId(userId).collectList().awaitSingle()

    override suspend fun bindUserToCompany(companyId: UUID, userId: UUID) {
        userCompanyRepository.save(UserCompanyDbEntity(userId = userId, companyId = companyId))
            .awaitSingleOrNull()
    }
}
