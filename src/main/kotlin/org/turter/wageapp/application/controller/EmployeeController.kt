package org.turter.wageapp.application.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.turter.wageapp.domain.employee.CompanyEmployeesResponse
import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.Employee
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import org.turter.wageapp.application.service.employee.EmployeeService
import java.security.Principal
import java.util.*

@RestController
@RequestMapping("/api/v1/employee")
class EmployeeController(
    private val service: EmployeeService
) {

    @GetMapping("/get/{id}")
    suspend fun get(@PathVariable id: UUID): Employee =
        service.getById(id)

    @GetMapping("/get/all")
    suspend fun getAll(): List<Employee> =
        service.getAll()

    @GetMapping("/get/by-companies")
    suspend fun getByCompanies(
        @RequestParam companyIds: List<UUID>
    ): List<CompanyEmployeesResponse> =
        service.getGroupedByCompanies(companyIds)

    @GetMapping("/get/coworkers")
    suspend fun getCoworkers(
        principal: Principal
    ): List<CompanyEmployeesResponse> =
        service.getCoworkersByUserId(principal.name)

    @PostMapping("/create")
    suspend fun create(
        @Valid @RequestBody payload: CreateEmployeePayload
    ): ResponseEntity<Employee> =
        ResponseEntity.status(201).body(service.create(payload))

    @PutMapping("/update/{id}")
    suspend fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody payload: UpdateEmployeePayload
    ): ResponseEntity<Unit> {
        service.update(id, payload)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/bind-user/{employeeId}")
    suspend fun bindUser(
        @PathVariable employeeId: UUID,
        @RequestParam userId: String
    ): ResponseEntity<Unit> {
        service.bindUser(employeeId, userId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/delete/{id}")
    suspend fun delete(@PathVariable id: UUID): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

}
