package org.turter.wageapp.application.controller

import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.turter.wageapp.domain.company.Company
import org.turter.wageapp.domain.company.CompanyPayload
import org.turter.wageapp.domain.company.UserCompaniesResponse
import org.turter.wageapp.application.service.company.CompanyService
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/api/v1/company")
class CompanyController(
    private val companyService: CompanyService
) {

    @GetMapping("/get/{id}")
    suspend fun get(@PathVariable id: UUID): ResponseEntity<Company> =
        ResponseEntity.ok(companyService.get(id))

    @GetMapping("/get/for-user")
    suspend fun getUserCompanies(principal: Principal): ResponseEntity<UserCompaniesResponse> =
        ResponseEntity.ok(
            UserCompaniesResponse(companyService.getUserCompanies(principal.name))
        )

    @GetMapping("/get/page")
    suspend fun getCompaniesPage(pageable: Pageable): ResponseEntity<Page<Company>> =
        ResponseEntity.ok(companyService.getCompaniesPage(pageable))

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

    @DeleteMapping("/delete/{id}")
    suspend fun delete(@PathVariable id: UUID): ResponseEntity<Unit> {
        companyService.delete(id)
        return ResponseEntity.noContent().build()
    }

}
