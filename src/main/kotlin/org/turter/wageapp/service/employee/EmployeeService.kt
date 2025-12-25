package org.turter.wageapp.service.employee

import org.turter.wageapp.domain.employee.CompanyEmployeesResponse
import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.Employee
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import java.util.*

interface EmployeeService {

    suspend fun getById(id: UUID): Employee

    suspend fun getAll(): List<Employee>

    suspend fun getGroupedByCompanies(companyIds: List<UUID>): List<CompanyEmployeesResponse>

    suspend fun getCoworkersByUserId(userId: String): List<CompanyEmployeesResponse>

    suspend fun create(payload: CreateEmployeePayload): Employee

    suspend fun update(id: UUID, payload: UpdateEmployeePayload): Employee

    suspend fun delete(id: UUID)

    suspend fun bindUser(employeeId: UUID, userId: String)
}
