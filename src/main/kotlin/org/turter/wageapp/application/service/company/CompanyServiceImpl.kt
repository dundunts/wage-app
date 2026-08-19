package org.turter.wageapp.application.service.company

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.turter.wageapp.application.data.company.CompanyRepository
import org.turter.wageapp.application.service.mapDuplicateKey
import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload
import org.turter.wageapp.domain.shared.EntityNotFoundException
import org.turter.wageapp.domain.shared.NotUniqueValueException
import org.turter.wageapp.application.mapper.CompanyMapper
import java.util.*

@Service
class CompanyServiceImpl(
    private val companyRepository: CompanyRepository,
    private val mapper: CompanyMapper
) : CompanyService {

    override suspend fun get(id: UUID) =
        companyRepository.findById(id).awaitSingleOrNull()
            ?.let(mapper::toCompanyDto)
            ?: throw EntityNotFoundException("Company $id not found")

    override suspend fun getUserCompanies(userId: String): List<Company> =
        companyRepository.findAllForUserId(userId).collectList().awaitSingle()

    override suspend fun getCompaniesPage(pageable: Pageable): Page<Company> {
        val data = companyRepository.findAllBy(pageable)
            .collectList()
            .awaitSingle()
            .mapNotNull { dbEntity -> mapper.toCompanyDto(dbEntity) }
        val totalCount = companyRepository.count().awaitSingle()

        return PageImpl(data, pageable, totalCount)
    }

    @Transactional
    override suspend fun create(payload: CompanyPayload): Company {
        val saved = mapDuplicateKey(
            exception = { e ->
                NotUniqueValueException("Company with title '${payload.title}' already exists", e)
            }
        ) {
            companyRepository.save(
                mapper.toNewCompanyDbEntity(payload)
            ).awaitSingle()
        }

        return mapper.toCompanyDto(saved)
    }

    @Transactional
    override suspend fun update(id: UUID, payload: CompanyPayload): Company {
        val saved = companyRepository.findById(id).awaitSingleOrNull()
            ?: throw EntityNotFoundException("Company $id not found")

        val updated = mapDuplicateKey(
            exception = { e ->
                NotUniqueValueException("Company with title '${payload.title}' already exists", e)
            }
        ) {
            companyRepository.save(
                mapper.mergeToCompanyDbEntity(payload, saved)
            ).awaitSingle()
        }

        return mapper.toCompanyDto(updated)
    }

    override suspend fun delete(id: UUID) {
        companyRepository.deleteById(id).awaitSingleOrNull()
    }

}
