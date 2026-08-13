package org.turter.wageapp.application.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.turter.wageapp.application.mapper.toDomain
import org.turter.wageapp.application.mapper.toTransport
import org.turter.wageapp.application.service.employee.EmployeeService
import org.turter.wageapp.transport.api.EmployeeApi
import org.turter.wageapp.transport.model.CompanyEmployees
import org.turter.wageapp.transport.model.CreateEmployeeRequest
import org.turter.wageapp.transport.model.Employee
import org.turter.wageapp.transport.model.UpdateEmployeeRequest
import java.util.UUID

@RestController
class EmployeeController(
    private val service: EmployeeService
) : EmployeeApi {

    override suspend fun getEmployee(id: UUID): ResponseEntity<Employee> =
        ResponseEntity.ok(service.getById(id).toTransport())

    override suspend fun getAllEmployees(): ResponseEntity<List<Employee>> =
        ResponseEntity.ok(service.getAll().map { it.toTransport() })

    override suspend fun getEmployeesByCompanies(
        companyIds: List<UUID>
    ): ResponseEntity<List<CompanyEmployees>> =
        ResponseEntity.ok(service.getGroupedByCompanies(companyIds).map { it.toTransport() })

    override suspend fun getCoworkers(): ResponseEntity<List<CompanyEmployees>> =
        ResponseEntity.ok(service.getCoworkersByUserId(currentUserId()).map { it.toTransport() })

    override suspend fun createEmployee(
        createEmployeeRequest: CreateEmployeeRequest
    ): ResponseEntity<Employee> =
        ResponseEntity.status(201).body(service.create(createEmployeeRequest.toDomain()).toTransport())

    override suspend fun updateEmployee(
        id: UUID,
        updateEmployeeRequest: UpdateEmployeeRequest
    ): ResponseEntity<Unit> {
        service.update(id, updateEmployeeRequest.toDomain())
        return ResponseEntity.noContent().build()
    }

    override suspend fun bindEmployeeUser(employeeId: UUID, userId: String): ResponseEntity<Unit> {
        service.bindUser(employeeId, userId)
        return ResponseEntity.noContent().build()
    }

    override suspend fun deleteEmployee(id: UUID): ResponseEntity<Unit> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }

}
