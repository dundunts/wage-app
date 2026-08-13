package org.turter.wageapp.application.mapper

import org.turter.wageapp.domain.employee.CompanyEmployeeInfo
import org.turter.wageapp.domain.employee.CompanyEmployeesResponse
import org.turter.wageapp.domain.employee.CreateEmployeePayload
import org.turter.wageapp.domain.employee.Employee as DomainEmployee
import org.turter.wageapp.domain.employee.UpdateEmployeePayload
import org.turter.wageapp.transport.model.CompanyEmployee
import org.turter.wageapp.transport.model.CompanyEmployees
import org.turter.wageapp.transport.model.CreateEmployeeRequest
import org.turter.wageapp.transport.model.Employee as TransportEmployee
import org.turter.wageapp.transport.model.Position as TransportPosition
import org.turter.wageapp.transport.model.UpdateEmployeeRequest

fun DomainEmployee.toTransport() = TransportEmployee(
    id = id,
    companyIds = companyIds,
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    simpleName = simpleName,
    position = position.toTransport()
)

fun CompanyEmployeesResponse.toTransport() = CompanyEmployees(
    companyId = companyId,
    data = data.map { it.toTransport() }
)

fun CompanyEmployeeInfo.toTransport() = CompanyEmployee(
    id = id,
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    simpleName = simpleName,
    position = position.toTransport()
)

fun CreateEmployeeRequest.toDomain() = CreateEmployeePayload(
    companyIds = requireNotNull(companyIds) { "companyIds must not be null" },
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    simpleName = simpleName,
    position = position.toDomain()
)

fun UpdateEmployeeRequest.toDomain() = UpdateEmployeePayload(
    companyIds = requireNotNull(companyIds) { "companyIds must not be null" },
    userId = userId,
    firstName = firstName,
    lastName = lastName,
    patronymic = patronymic,
    simpleName = simpleName,
    position = position.toDomain()
)

private fun DomainEmployee.Position.toTransport() = TransportPosition.valueOf(name)

private fun TransportPosition.toDomain() = DomainEmployee.Position.valueOf(name)
