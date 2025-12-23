package org.turter.wageapp.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.turter.wageapp.domain.company.BindUserToCompanyPayload
import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload
import org.turter.wageapp.domain.company.UserCompaniesResponse
import org.turter.wageapp.service.company.CompanyService
import java.security.Principal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/company")
class CompanyController(
    private val companyService: CompanyService
) {

    @GetMapping("/get/{id}")
    suspend fun get(@PathVariable id: UUID): ResponseEntity<Company> =
        ResponseEntity.ok(companyService.get(id))

    @GetMapping("/get/for-user")
    suspend fun getUserCompanies(principal: Principal): ResponseEntity<UserCompaniesResponse> {
        val userId = UUID.fromString(principal.name)
        return ResponseEntity.ok(UserCompaniesResponse(companyService.getUserCompanies(userId)))
    }

    @PostMapping("/create")
    suspend fun create(
        @RequestBody @Valid payload: CompanyPayload
    ): ResponseEntity<Company> =
        ResponseEntity.status(201).body(companyService.create(payload))

    @PutMapping("/update/{id}")
    suspend fun update(
        @PathVariable id: UUID,
        @RequestBody @Valid payload: CompanyPayload
    ): ResponseEntity<Unit> {
        companyService.update(id, payload)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/bind/{companyId}/user")
    suspend fun bindUser(
        @PathVariable companyId: UUID,
        @RequestBody @Valid payload: BindUserToCompanyPayload
    ): ResponseEntity<Unit> {
        companyService.bindUserToCompany(companyId, payload.userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/delete/{id}")
    suspend fun delete(@PathVariable id: UUID): ResponseEntity<Unit> {
        companyService.delete(id)
        return ResponseEntity.noContent().build()
    }


}
