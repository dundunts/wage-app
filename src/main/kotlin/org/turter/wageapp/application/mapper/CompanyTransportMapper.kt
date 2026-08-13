package org.turter.wageapp.application.mapper

import org.springframework.data.domain.Page
import org.turter.wageapp.domain.company.Company as DomainCompany
import org.turter.wageapp.domain.company.CompanyPayload
import org.turter.wageapp.transport.model.Company as TransportCompany
import org.turter.wageapp.transport.model.CompanyCreateOrUpdateRequest
import org.turter.wageapp.transport.model.CompanyPage

fun DomainCompany.toTransport() = TransportCompany(
    id = id,
    title = title,
    employeeWageCoefficientFromRevenue = employeeWageCoefficientFromRevenue,
    defaultShiftStartTime = defaultShiftStartTime
)

fun CompanyCreateOrUpdateRequest.toDomain() = CompanyPayload(
    title = title,
    employeeWageCoefficientFromRevenue = employeeWageCoefficientFromRevenue,
    defaultShiftStartTime = defaultShiftStartTime
)

fun Page<DomainCompany>.toCompanyPage(): CompanyPage {
    val requestedSize = size
    val transportContent = content.map { it.toTransport() }
    return CompanyPage(
        content = transportContent,
        number = number,
        propertySize = requestedSize,
        totalElements = totalElements
    ).apply {
        put("content", transportContent)
        put("pageable", pageable)
        put("last", isLast)
        put("totalPages", totalPages)
        put("totalElements", totalElements)
        put("size", requestedSize)
        put("number", number)
        put("sort", sort)
        put("first", isFirst)
        put("numberOfElements", numberOfElements)
        put("empty", isEmpty())
    }
}
