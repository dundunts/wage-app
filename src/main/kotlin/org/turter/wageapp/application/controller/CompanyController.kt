package org.turter.wageapp.application.controller

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.mapper.toCompanyPage
import org.turter.wageapp.application.mapper.toDomain
import org.turter.wageapp.application.mapper.toTransport
import org.turter.wageapp.application.service.company.CompanyService
import org.turter.wageapp.transport.api.CompanyApi
import org.turter.wageapp.transport.model.Company
import org.turter.wageapp.transport.model.CompanyCreateOrUpdateRequest
import org.turter.wageapp.transport.model.CompanyPage
import org.turter.wageapp.transport.model.UserCompaniesResponse
import java.util.UUID

@RestController
class CompanyController(
    private val companyService: CompanyService
) : CompanyApi {

    override suspend fun getCompany(id: UUID): ResponseEntity<Company> =
        ResponseEntity.ok(companyService.get(id).toTransport())

    override suspend fun getCompaniesForUser(): ResponseEntity<UserCompaniesResponse> =
        ResponseEntity.ok(
            UserCompaniesResponse(
                companies = companyService.getUserCompanies(currentUserId()).map { it.toTransport() }
            )
        )

    override suspend fun getCompaniesPage(
        page: String,
        size: String,
        sort: List<String>?
    ): ResponseEntity<CompanyPage> {
        val companies = companyService.getCompaniesPage(pageRequest(page, size, sort))
        return ResponseEntity.ok(companies.toCompanyPage())
    }

    override suspend fun createCompany(
        companyCreateOrUpdateRequest: CompanyCreateOrUpdateRequest
    ): ResponseEntity<Company> =
        ResponseEntity.status(201).body(
            companyService.create(companyCreateOrUpdateRequest.toDomain()).toTransport()
        )

    override suspend fun updateCompany(
        id: UUID,
        companyCreateOrUpdateRequest: CompanyCreateOrUpdateRequest
    ): ResponseEntity<Unit> {
        companyService.update(id, companyCreateOrUpdateRequest.toDomain())
        return ResponseEntity.noContent().build()
    }

    override suspend fun deleteCompany(id: UUID): ResponseEntity<Unit> {
        companyService.delete(id)
        return ResponseEntity.noContent().build()
    }

    private fun pageRequest(page: String, size: String, sort: List<String>?): PageRequest {
        val pageNumber = CompanyPagination.normalizePage(page)
        val pageSize = CompanyPagination.normalizeSize(size)
        return PageRequest.of(pageNumber, pageSize, parseSort(sort))
    }

    private fun parseSort(criteria: List<String>?): Sort {
        val tokens = criteria.orEmpty().flatMap { it.split(",") }.map(String::trim).filter(String::isNotEmpty)
        val orders = buildList {
            var index = 0
            while (index < tokens.size) {
                val property = tokens[index++]
                val direction = tokens.getOrNull(index)
                    ?.let(Sort.Direction::fromOptionalString)
                    ?.orElse(null)
                if (direction != null) index++
                add(Sort.Order(direction ?: Sort.Direction.ASC, property))
            }
        }
        return Sort.by(orders)
    }
}
